package edu.isistan.easywsdl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.log4j.Logger;

public class Main {

	/**
	 * Outputs to standard output a comma separated file according to URL,DW,DMR,ME,MRS
	 * @param args
	 */
	public static void main(String[] args) {	
		System.out.println("URL,DW,DMC,DMR,ME,MRS");
		for (int j = 0; j < args.length; j++) {
			String descUrl = args[j];
			URL curr;
			try {
				curr = URI.create(descUrl).toURL();			
				MetricsSuite ms = new MetricsSuite();				
				StringBuffer sb = new StringBuffer();
				sb.append(descUrl.split("etc/")[1]);
				sb.append(",");
				sb.append( ms.getDW(curr) );
				sb.append(",");
				sb.append( ms.getDMC(curr) );
				sb.append(",");
				sb.append( ms.getDMR(curr) );
				sb.append(",");
				sb.append( ms.getMRS(curr) );
				sb.append(",");
				sb.append( ms.getME(curr) );				
				System.out.println(sb.toString());
			} catch (MalformedURLException e) {
				Logger.getLogger(MessageComplexityCalculator.class.getName()).error("Malformed: " + descUrl,e);
			}
		}
	}

}
