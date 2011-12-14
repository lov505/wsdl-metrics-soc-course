package edu.isistan.easywsdl;

import java.net.MalformedURLException;
import java.net.URI;

import org.apache.log4j.Logger;

public class Main {

	/**
	 * Outputs to standard output a comma separated file according to URL,DW,DMR,ME,MRS
	 * @param args
	 */
	public static void main(String[] args) {	
		System.out.println("URL,DW,DMR,ME,MRS");
		for (int j = 0; j < args.length; j++) {
			String descUrl = args[j];			
			try {
				MetricsSuite ms = new MetricsSuite();				
				StringBuffer sb = new StringBuffer();
				sb.append(descUrl);
				sb.append(",");
				sb.append( ms.getDW( URI.create(descUrl).toURL()) );
				sb.append(",");
				sb.append( ms.getDMR( URI.create(descUrl).toURL()) );
				sb.append(",");
				sb.append( ms.getME( URI.create(descUrl).toURL()) );
				sb.append(",");
				sb.append( ms.getMRS( URI.create(descUrl).toURL()) );
				System.out.println(sb.toString());
			} catch (MalformedURLException e) {
				Logger.getLogger(MessageComplexityCalculator.class.getName()).error("Malformed: " + descUrl,e);
			}
		}
	}

}
