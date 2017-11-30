package mcc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Helper {
	
	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static boolean colorActive=false;
	
	public static void printBroken(String msg){
		if(colorActive){
			System.out.println(ANSI_YELLOW+msg+ANSI_RESET);			
		}else{
			System.out.println(msg);
		}
	}
	
	public static void printFinding(String msg){
		if(colorActive){
			System.out.println(ANSI_RED+msg+ANSI_RESET);
		}else{
			System.out.println(msg);
		}
	}
	
	public static String trimString(String s, int maxLength){
		if(s.length()>maxLength){
			return s.substring(0, Math.min(s.length(), maxLength-2))+"..";	
		}
		return s;
	}
	
	public static CommandLine parseCommandLineArgs(String[] args) {
		Options options = new Options();
        
		Option visitOption = new Option("v", "visit", true, "Comma seperated URL(s) to start crawling from.");
        visitOption.setRequired(true);
        options.addOption(visitOption);

        Option mactchesOption = new Option("m", "matches", true, "Comma seperated string(s) that need to be included. Example visiting www.example.com you can set matches to example.com, so no other domains will be crawled which might be linked on example.com");
        mactchesOption.setRequired(false);
        options.addOption(mactchesOption);
        
        Option reportHTTPLinksOption = new Option("a", "alwaysreport", false, "If flag is set, http links that match will always be reported. Otherwise every http link will be reported only once. Default is false");
        reportHTTPLinksOption.setRequired(false);
        options.addOption(reportHTTPLinksOption);
        
        Option colorOption = new Option("c", "color", false, "If flag is set, the findings and warnings will be colorized (does not work on Windows). Default is false");
        colorOption.setRequired(false);
        options.addOption(colorOption);

        Option quietOption = new Option("q", "quiet", false, "If flag is set, the current progress will not be displayed. Default is false");
        quietOption.setRequired(false);
        options.addOption(quietOption);
        
        Option unsafeOption = new Option("u", "unsafe", false, "If flag is set, all certificates will be trusted. Default is false");
        unsafeOption.setRequired(false);
        options.addOption(unsafeOption);
        
        Option printCrawlsOption = new Option("p", "printcrawls", false, "If flag is set, a list of all crawled URLs will be printed. Default is false");
        printCrawlsOption.setRequired(false);
        options.addOption(printCrawlsOption);
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar MixedContentChecker.jar ", options);
            System.out.println("");
            System.out.println("Example: -v example.com,b.example.com -m example.com,example.org -c");
            System.out.println("This will crawl example.com and b.example.com if the outgoing links contain example.com or example.org");
            System.exit(1);
        }
		return cmd;
	}

	public static String getUrlAsString(String url, String elementSrcAbs) {
		try {
			URL urlObj = new URL(elementSrcAbs);
			URLConnection con = urlObj.openConnection();
			con.setDoOutput(true);
			con.connect();
			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream())
			);
			StringBuilder response = new StringBuilder();
			String inputLine;
			String newLine = System.getProperty("line.separator");
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine + newLine);
			}
			in.close();
			return response.toString();
		} catch (FileNotFoundException | UnknownHostException e) {
			printBroken("Found broken link on "+url+" -- "+elementSrcAbs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	static boolean linkUsesHTTP(String linkAbs) {
		return (linkAbs.toLowerCase().startsWith("http://") ||linkAbs.toLowerCase().startsWith("https://"));
	}

	static boolean linkActIsSpecial(String linkAct) {
		return (
				linkAct.startsWith("javascript:") 
				|| linkAct.startsWith("#") 
				|| linkAct.startsWith("mailto:") 
				|| linkAct.startsWith("tel:") 
				|| linkAct.startsWith("skype:")
		);
	}
	
	static void startCrawling(CommandLine cmd) {
		String tovisitArg = cmd.getOptionValue("visit");

		String matchesArg ="";
		if(cmd.hasOption("matches")){
			matchesArg = cmd.getOptionValue("matches");
		}
		if(cmd.hasOption("unsafe")){
	        try {
	        	SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, Helper.disableSSLCertCheck(), new java.security.SecureRandom());
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			} catch (KeyManagementException | NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
	        HostnameVerifier allHostsValid = new HostnameVerifier() {
				@Override
				public boolean verify(String arg0, SSLSession arg1) {
					return true;
				}
	        };
	        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		}
		
        boolean reportHTTPLinkAlways = cmd.hasOption("alwaysreport");
        boolean quiet= cmd.hasOption("quiet");
        Helper.colorActive = cmd.hasOption("color");
        
		String[] tovisitArr = tovisitArg.split(",");
		String[] matchesArr = matchesArg.split(",");
		
		HashSet<String> visited = new HashSet<String>();
        Stack<String> tovisit = new Stack<String>();
        for (String tovisitE : tovisitArr) {
        	if(!tovisitE.startsWith("https://")){
        		tovisitE="https://"+tovisitE;
        	}
			tovisit.add(tovisitE);
		}
        HashSet<String> matches = new HashSet<String>();
        for (String match : matchesArr) {
			matches.add(match);
		}
        
        boolean printCrawlList = cmd.hasOption("printcrawls");
        
		System.out.println("Initial crawl list: "+(tovisit));
		System.out.println("Must match         :"+Arrays.toString(matchesArr));
		System.out.println("Trusting all certificates:"+cmd.hasOption("unsafe"));
		System.out.println("---");
		System.out.println();
        
		MCC.crawlForLinks(visited, tovisit, matches ,reportHTTPLinkAlways,quiet,printCrawlList);
	}

	
	public static TrustManager[] disableSSLCertCheck() throws NoSuchAlgorithmException, KeyManagementException {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			@Override
			public void checkClientTrusted(
				java.security.cert.X509Certificate[] arg0,
				String arg1) throws CertificateException {
			}
			@Override
			public void checkServerTrusted(
				java.security.cert.X509Certificate[] arg0, String arg1) throws CertificateException {	 } 
		}};
		return trustAllCerts;
	}
}
