package mcc;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Stack;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.cli.CommandLine;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MCC {
	
	public static void main(String[] args){
        CommandLine cmd = Helper.parseCommandLineArgs(args);
        Helper.startCrawling(cmd);
	}

	public static void crawlForLinks(HashSet<String> visited, Stack<String> tovisit, HashSet<String> matches, boolean reportHTTPLinkalways,boolean quiet, boolean printCrawlList, boolean printBroken) {
		String format = "%-90s %5s %15s\n";

		HashSet<String> httpLinks = new HashSet<String>();
		long start = System.currentTimeMillis();
		while(!tovisit.isEmpty()){
			String url;
			// Getting rid of already visited URL's, this can happen when one page links to the same one multiple times. 
			// Otherwise a stack.contains would have to be performed which takes too long.
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
		        	boolean linkActIsSpecial 	= Helper.linkActIsSpecial(linkAct);
		        	boolean linkUsesHTTP 		= Helper.linkUsesHTTP(linkAbs);
		        	boolean circularLink 		= linkAbs.equals(url);
					if(linkUsesHTTP && !linkActIsSpecial && !linkAbs.equals("") && !circularLink){
						String linkAbsMatch = linkAbs.toLowerCase().replace("http://","").replace("https://", "");
						for (String match : matches) {
							if(linkAbsMatch.startsWith(match)){
								if(linkAbs.toLowerCase().startsWith("http://")){
									if(reportHTTPLinkalways || !httpLinks.contains(linkAbs)){
										httpLinks.add(linkAbs);
										Helper.printFinding("Found http link (automatically switching to https) on "+url+" -- "+linkAbs);
									}
									linkAbs=linkAbs.replace("http://","https://");
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
			} catch (HttpStatusException | UnknownHostException  e) {
				if(printBroken){
					Helper.printBroken("Broken link! "+e.getMessage()+" -- "+url);					
				}
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
		if(printCrawlList){
			System.out.println(visited.toString());
		}
	}
}
