package edu.isistan.easywsdl;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ow2.easywsdl.wsdl.WSDLFactory;
import org.ow2.easywsdl.wsdl.api.Description;
import org.ow2.easywsdl.wsdl.api.InterfaceType;
import org.ow2.easywsdl.wsdl.api.Operation;
import org.ow2.easywsdl.wsdl.api.Part;
import org.ow2.easywsdl.wsdl.api.WSDLException;
import org.ow2.easywsdl.wsdl.api.WSDLReader;
import org.xml.sax.InputSource;

public class MetricsSuite {

	protected Description read(URL url) {
		WSDLReader reader = null;
		try {
			reader = WSDLFactory.newInstance().newWSDLReader();
		} catch (WSDLException e) {
			Logger.getLogger(MetricsSuite.class.getName()).error(e);
		}
		Description desc;
		URLConnection con = null;
		InputStream in = null;
		try {
			con = url.openConnection();
			con.connect();
			in = con.getInputStream();
			desc = reader.read(new InputSource(in));
			return desc;
		} catch (Exception e) {
			Logger.getLogger(MetricsSuite.class.getName()).error(e);
			return null;
		} 
	}
	
	/**
	 * @param url Pointer to WSDL document
	 * @return The Data Weight (DW) of a WSDL (sec. 4.1 of 10.1049/iet-sen.2010.0089)
	 * The DW of a given WSDL can be defined as the sum of the
	 * data complexities of each input and output messages
	 */
	public int getDW(URL url) {
		int DW = 0;
		MessageComplexityCalculator c = new MessageComplexityCalculator();
		Description desc = read(url);		
		Iterator<InterfaceType> iter = desc.getInterfaces().iterator();
		while (iter.hasNext()) {			
			InterfaceType portType = iter.next();				
			Iterator<Operation> opers = portType.getOperations().iterator();
			while (opers.hasNext()) {
				Operation op = (Operation) opers.next();
				Logger.getLogger(MetricsSuite.class.getName()).debug("Analizando operacion: " + op.getQName().getLocalPart());
				Iterator<Part> parts = op.getInput().getParts().iterator();
				while (parts.hasNext()) {					
					Part part = (Part) parts.next();					
					DW += c.calculateFor(part.getElement());					
				}
				parts = op.getOutput().getParts().iterator();
				while (parts.hasNext()) {
					Part part = (Part) parts.next();
					DW += c.calculateFor(part.getElement());
				}
			}
		}
		return DW;
	}
	
	/**
	 * @param url Pointer to WSDL document
	 * @return The Distinct Message Ratio (DMR) of a WSDL (sec. 4.2 of 10.1049/iet-sen.2010.0089)
	 * The DMR is derived from distinct message count (DMC) metric
	 * and the total number of messages Nm.
	 * 
	 * For DMC see {@link MetricsSuite#getDMC(URL)}
	 */
	public double getDMR(URL url) {
		int Nm = getMessageCount(url);
		return getDMC(url) / Nm;
	}
	
	/**
	 * @param  url Pointer to WSDL document 
	 * @return The Distinct Message Count (DMC) of a WSDL (sec. 4.2 of 10.1049/iet-sen.2010.0089)
	 * The DMC is defined as the number of distinct structured messages 
	 * represented by [C(M),arg] pair reflecting the message’s complexity 
	 * value C(M) and total number of arguments arg that the message contains.
	 * 
	 * For [C(M),arg] see {@link Pair}
	 */
	public int getDMC(URL url) {
		List<Pair> pairs = getMessagePairs(url);		
		Set<Pair> noDups = new LinkedHashSet<Pair>(pairs);
		Logger.getLogger(MetricsSuite.class.getName()).debug("Pares [C(M),Nargs] distintos: " + noDups.size());		
		return noDups.size();
	}
	
	/**
	 * @param  url Pointer to WSDL document 
	 * @return The ME of a WSDL document (sec. 4.3 of 10.1049/iet-sen.2010.0089)
	 * The ME metric is intended to measure the complexity caused by 
	 * occurrences of similar-structured messages.
 	 * This metric multiplies the probability of a message represented by [C(M ),arg] 
 	 * by its log_2. The probability of a pair [C(M ),arg] is its occurrences divided the total
 	 * number of messages.
 	 *  
	 * 
	 */
	public double getME(URL url) {
		double ME = 0.0;
		List<Pair> pairs = getMessagePairs(url);			
		double[] NOMs = getMessageOccurrences(pairs);
		int Nm = getMessageCount(url);
		for (int i = 0; i < NOMs.length; i++) {
			Logger.getLogger(MetricsSuite.class.getName()).debug("Par: "+ i + " NOM: " + NOMs[i] + " Nm:" + Nm + " PM:" + NOMs[i]/Nm);
			double log2 = Math.log(NOMs[i]/Nm)/Math.log(2);
			ME += NOMs[i]/Nm * log2;
		}
		return  (-1)*ME;
	}	
	
	/**
	 * @param  url Pointer to WSDL document 
	 * @return The MRS of a WSDL document (sec. 4.4 of 10.1049/iet-sen.2010.0089)
	 * MRS metric is intended to analyze variety in structures of WSDL documents.
	 * 
	 */
	public double getMRS(URL url) {
		double MRS = 0.0;
		List<Pair> pairs = getMessagePairs(url);
		double[] NOMs = getMessageOccurrences(pairs);
		int Nm = getMessageCount(url);
		for (int i = 0; i < NOMs.length; i++) {
			Logger.getLogger(MetricsSuite.class.getName()).debug("Par: "+ i + " NOM: " + NOMs[i] + " Nm:" + Nm + " PM:" + NOMs[i]/Nm);
			double square = Math.pow(NOMs[i],2);
			MRS += square;
		}
		return  MRS/Nm;
	}
	
	
	protected int getMessageCount(URL url) {
		Description desc = read(url);
		int Nm = 0;
		Iterator<InterfaceType> iter = desc.getInterfaces().iterator();
		while (iter.hasNext()) {			
			InterfaceType portType = iter.next();
			Iterator<Operation> opers = portType.getOperations().iterator();
			while (opers.hasNext()) {
				Operation op = (Operation) opers.next();
				Logger.getLogger(MetricsSuite.class.getName()).debug("Analizando operacion: " + op.getQName().getLocalPart());
				if (op.getInput()!=null) {
					Nm++;
				}
				if (op.getOutput()!=null) {					
					Nm++;
				}
			}
		}
		return Nm;
	}	
	
	protected List<Pair> getMessagePairs(URL url) {
		Description desc = read(url);
		MessageComplexityCalculator c = new MessageComplexityCalculator();
		Iterator<InterfaceType> iter = desc.getInterfaces().iterator();
		List<Pair> pairs = new Vector<Pair>();
		while (iter.hasNext()) {			
			InterfaceType portType = iter.next();
			Iterator<Operation> opers = portType.getOperations().iterator();
			while (opers.hasNext()) {
				Operation op = (Operation) opers.next();
				Logger.getLogger(MetricsSuite.class.getName()).debug("Analizando operacion: " + op.getQName().getLocalPart());
				if (op.getInput()!=null) {
					Iterator<Part> parts = op.getInput().getParts().iterator();
					int CM = 0;
					int Nargs = 0;
					while (parts.hasNext()) {
						Part part = (Part) parts.next();					
						CM += c.calculateFor(part.getElement());
						Nargs += c.countArgumentsFor(part.getElement());
					}
					pairs.add(new Pair(CM, Nargs, op.getInput().getMessageName().getLocalPart()));
				}
				if (op.getOutput()!=null) {					
					Iterator<Part> parts = op.getOutput().getParts().iterator();
					int CM = 0;
					int Nargs = 0;
					while (parts.hasNext()) {
						Part part = (Part) parts.next();
						CM += c.calculateFor(part.getElement());
						Nargs += c.countArgumentsFor(part.getElement());
					}
					pairs.add(new Pair(CM, Nargs, op.getOutput().getMessageName().getLocalPart()));
				}
			}
		}
		Logger.getLogger(MetricsSuite.class.getName()).debug("Pares [C(M),Nargs] recolectados: " + pairs);
		return pairs;
	}	
	
	protected double[] getMessageOccurrences(List<Pair> pairs) {
		Set<Pair> noDups = new LinkedHashSet<Pair>(pairs);	
		double[] NOMs = new double[noDups.size()];				
		int i=0;
		Iterator<Pair> iNoDups = noDups.iterator();
		while (iNoDups.hasNext()) {
			Pair current = iNoDups.next();
			int NOM = 0;
			for (Iterator<Pair> iterator = pairs.iterator(); iterator.hasNext();) {
				Pair pair = iterator.next();
				if (current.equals(pair)) {
					NOM++;
				}
			}
			NOMs[i++] = NOM;
		}
		return NOMs;
	}
	
	/**
	 * Auxiliary class for modeling structured messages represented by
	 * [C(M),arg] pair reflecting the message’s complexity value
     * C(M) and total number of arguments arg that the message contains.
     * 
     * For C(M) see {@link MessageComplexityCalculator#calculateFor(org.ow2.easywsdl.schema.api.Element)}
     * For arg see {@link MessageComplexityCalculator#countArgumentsFor(org.ow2.easywsdl.schema.api.Element)}
     *  
	 * @author mcrasso
	 *
	 */
	class Pair {
		int CM;
		int Nargs;
		String mName;
		
		public Pair(int CM, int Nargs, String mName) {
			this.CM=CM;
			this.Nargs=Nargs;
			this.mName = mName;
		}
		
		public int getCM() {
			return CM;
		}
		
		public int getNargs() {
			return Nargs;
		}
		
		@Override
		public boolean equals(Object obj) {
			Pair p = (Pair) obj;	
			return this.CM == p.getCM() && this.Nargs == p.getNargs();
		}
		
		@Override
		public String toString() {
			return this.mName + "[CM="+this.CM+",Nargs="+this.Nargs+"]";
		}
	}
}