package mcc;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Stack;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MCC {
	
	public static void main(String[] args){

        Options options = new Options();
        
		Option visitOption = new Option("v", "visit", true, "Comma seperated URL(s) to start crawling from.");
        visitOption.setRequired(true);
        options.addOption(visitOption);

        Option mactchesOption = new Option("m", "matches", true, "Comma seperated string(s) that need to be included. Example visiting www.example.com you can set matches to example.com, so no other domains will be crawler which might be linked on example.com");
        mactchesOption.setRequired(false);
        options.addOption(mactchesOption);
        
        Option reportBrokenLinksOption = new Option("a", "allwaysreport", false, "If flag is set, broken links will always be reported. Otherwise every broken link will be reported only once. Default is false");
        reportBrokenLinksOption.setRequired(false);
        options.addOption(reportBrokenLinksOption);
        
        Option colorOption = new Option("c", "color", false, "If flag set, the findings and warnings will be colorized (does not work on Windows). Default is false");
        colorOption.setRequired(false);
        options.addOption(colorOption);

        Option quietOption = new Option("q", "quiet", false, "If flag set, the current progress will not be displayed. Default is false");
        quietOption.setRequired(false);
        options.addOption(quietOption);
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("java -jar MixedContentChecker.jar ", options);
            System.out.println("");
            System.out.println("Example: -v example.com,b.example.com -m example.com,example.org -c");
            System.out.println("This will crawl example.com and b.example.com if the outgoing links contain example.com or example.org");
            System.exit(1);
            return;
        }

        String tovisitArg = cmd.getOptionValue("visit");
        String matchesArg = cmd.getOptionValue("matches");
        boolean reportBrokenLinkAllways = cmd.hasOption("allwaysreport");
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
        
		System.out.println("Initial crawl list: "+(tovisit));
		System.out.println("Must match         :"+Arrays.toString(matchesArr));
		System.out.println("---");
		System.out.println();
        
		crawlForLinks(visited, tovisit, matches ,reportBrokenLinkAllways,quiet);
	}

	private static void crawlForLinks(HashSet<String> visited, Stack<String> tovisit, HashSet<String> matches, boolean reportBrokenLinkAllways,boolean quiet) {
		String format = "%-90s %5s %15s\n";

		HashSet<String> httpLinks = new HashSet<String>();
		long start = System.currentTimeMillis();
		while(!tovisit.isEmpty()){
			String url;
			do{
				url = tovisit.pop();
			}while(visited.contains(url) && !tovisit.isEmpty());
			visited.add(url);
			try {
				Document doc = Jsoup.connect(url).get();
		        Elements links = doc.select("a[href]");
		        for (Element element : links) {
		        	String linkAbs = element.attr("abs:href");
		        	String linkAct = element.attr("href");
		        	boolean linkActIsSpecial = (linkAct.startsWith("javascript:") || linkAct.startsWith("#") || linkAct.startsWith("mailto:") || linkAct.startsWith("tel:") || linkAct.startsWith("skype:"));
		        	boolean linkUsesHTTP = (linkAbs.toLowerCase().startsWith("http://")||linkAbs.toLowerCase().startsWith("https://"));
		        	boolean circularLink = linkAbs.equals(url);
					if(linkUsesHTTP && !linkActIsSpecial && !linkAbs.equals("") && !circularLink){
						for (String match : matches) {
							if(linkAbs.contains(match)){
								if(linkAbs.toLowerCase().startsWith("http://")){
									if(reportBrokenLinkAllways || !httpLinks.contains(linkAbs)){
										httpLinks.add(linkAbs);
										Helper.printFinding("Found http link on "+url+" -- "+linkAbs);
										linkAbs=linkAbs.replace("http://","https://");
									}
								}
								if(!visited.contains(linkAbs)){
									tovisit.push(linkAbs);
								}
							}
						}
					}
				}
		        
		        MixedContent.checkForMixedContent(url, doc, "style"		,"none"	,visited);
		        MixedContent.checkForMixedContent(url, doc, "script"	,"src"	,visited);
		        MixedContent.checkForMixedContent(url, doc, "link"		,"href"	,visited);
		        MixedContent.checkForMixedContent(url, doc, "iframe"	,"src"	,visited);
		        MixedContent.checkForMixedContent(url, doc, "object"	,"data"	,visited);
		        MixedContent.checkForMixedContent(url, doc, "audio"		,"src"	,visited);
		        MixedContent.checkForMixedContent(url, doc, "img"		,"src"	,visited);
		        MixedContent.checkForMixedContent(url, doc, "video"		,"src"	,visited);
		        if(!quiet){
		        	System.out.format(format, "Checked URL "+Helper.trimString(url, 78), tovisit.size()+" queued", visited.size()+" checked");		        	
		        }
		        
			} catch (UnsupportedMimeTypeException e) {
				// OK, is a file like pdf or docx etc.
			} catch (HttpStatusException e) {
				Helper.printBroken("Broken link! "+e.getMessage()+" -- "+url);
			} catch (UnknownHostException e) {
				Helper.printBroken("Broken link! "+e.getMessage()+" -- "+url);
			} catch (SSLHandshakeException e){
				Helper.printBroken("SSL broken!  "+e.getMessage()+" -- "+url);
			} catch (IllegalArgumentException e){
				if(e.getMessage().contains("Malformed URL")){
					System.err.println("Malformed URL provided.");
				}else{
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		long end = System.currentTimeMillis();
		System.out.println(visited.size()+" elements in "+(end-start)+" ms");
	}
}
