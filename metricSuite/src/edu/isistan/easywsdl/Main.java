package edu.isistan.easywsdl;

import java.net.MalformedURLException;
import java.net.URI;

import org.apache.log4j.Logger;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		for (int j = 0; j < args.length; j++) {
			String descUrl = args[j];			
			try {
				MetricsSuite ms = new MetricsSuite();
				System.out.println( ms.getDW( URI.create(descUrl).toURL()) );
				System.out.println( ms.getDMR( URI.create(descUrl).toURL()) );
				System.out.println( ms.getME( URI.create(descUrl).toURL()) );
				System.out.println( ms.getMRS( URI.create(descUrl).toURL()) );
			} catch (MalformedURLException e) {
				Logger.getLogger(MessageComplexityCalculator.class.getName()).error("Malformed: " + descUrl,e);
			}
		}
	}

}
