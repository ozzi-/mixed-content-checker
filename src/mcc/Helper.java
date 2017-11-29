package mcc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

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
		} catch (FileNotFoundException e) {
			printBroken("Found broken link on "+url+" -- "+elementSrcAbs);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}
