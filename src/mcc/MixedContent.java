package mcc;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MixedContent {
	// (@import) url("http..
	private static Pattern cssURLPattern 	= Pattern.compile("url(\\s)*\\((\\s)*(\"|\')(\\s)?http:");
	 // @import ("http...    --> omitting URL is valid to
	private static Pattern cssURL2Pattern 	= Pattern.compile("@import(\\s)*(\"|')(\\s)*http:");
	// (xhr.)open("POST","http.. 
	private static Pattern jsXHRPattern 	= Pattern.compile("open(\\s)?\\((\\s)?(\"|')[a-zA-Z]*(\"|')(\\s)?,(\\s)?(\"|')(\\s)?(http|HTTP):\\/\\/");
	
	public static void checkForMixedContent(String url, Document doc, String tag, String attribute, HashSet<String> visited) {	
		if(tag.equals("script")){
			checkForInlineScriptMixedContent(url, doc, tag);
		}
		if(tag.equals("style")){
			checkForInlineStyleMixedContent(url, doc, tag);
		}else{
			checkForLinkedResourceMixedContent(url, doc, tag, attribute, visited);
		}
	}
	
	private static void checkForMixedContentCSS(String url, String elementSrcAbs) {
		String css = Helper.getUrlAsString(url,elementSrcAbs);				
		Matcher m = cssURLPattern.matcher(css);
		while( m.find() ) {
			Helper.printFinding("Found insecure resource linked in CSS "+url+" -- "+m.group());
		}
		Matcher m2 = cssURL2Pattern.matcher(css);
		while( m2.find() ) {
			Helper.printFinding("Found insecure resource linked in CSS "+url+" -- "+m.group());
		}
	}
	
	private static void checkForMixedContentXHR(String url, String elementSrcAbs) {
		String js = Helper.getUrlAsString(url,elementSrcAbs);				
		Matcher m = jsXHRPattern.matcher(js);
		while( m.find() ) {
			Helper.printFinding("Found insecure XHR in JavaScript include "+url+" -- "+m.group());
		}		
	}
	
	private static void checkForInlineStyleMixedContent(String url, Document doc, String tag) {
		Elements allElements = doc.select(tag);
		for (Element element : allElements) {
			String css = element.toString();
			Matcher m = cssURLPattern .matcher(css);
			while( m.find() ) {
				Helper.printFinding("Found insecure resource linked in inline CSS in "+url+" -- "+m.group());
			}		
		}
	}

	private static void checkForLinkedResourceMixedContent(String url, Document doc, String tag, String attribute, HashSet<String> visited) {
		Elements elements = doc.select(tag+"["+attribute+"]");
		for (Element element : elements) {
			String elementSrc 		= element.attr(attribute);
			String elementSrcAbs 	= element.attr("abs:"+attribute);
			if(tag.equals("link")){
				if(!visited.contains(elementSrcAbs) && (element.attr("rel").equals("stylesheet") || element.attr("type").equals("text/css") || elementSrcAbs.endsWith(".css"))){
					visited.add(elementSrcAbs);				
					checkForMixedContentCSS(url,elementSrcAbs);						
				}
			}
			if(tag.equals("script")){
				if( !visited.contains(elementSrcAbs)){
					visited.add(elementSrcAbs);
					checkForMixedContentXHR(url,elementSrcAbs);						
				}
			}
			if(elementSrc.toLowerCase().startsWith("http://")){
				Helper.printFinding("Insecure <"+tag+" "+attribute+"=X >  on "+url+" - "+elementSrc);
			}	
		}
	}

	private static void checkForInlineScriptMixedContent(String url, Document doc, String tag) {
		Elements allElements = doc.select(tag);
		for (Element element : allElements) {
			if(element.attr("src")==""){
				String js = element.toString();
				Matcher m = jsXHRPattern.matcher(js);
				while( m.find() ) {
					Helper.printFinding("Found insecure XHR in inline JavaScript in "+url+" -- "+m.group());
				}		
			}
		}
	}
}
