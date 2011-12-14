package edu.isistan.easywsdl;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.ow2.easywsdl.schema.SchemaFactory;
import org.ow2.easywsdl.schema.api.Element;
import org.ow2.easywsdl.schema.api.Schema;
import org.ow2.easywsdl.schema.api.SchemaException;
import org.ow2.easywsdl.schema.api.SchemaReader;
import org.ow2.easywsdl.schema.impl.ElementImpl;
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
	
	protected List<Element> getAllElements(Description desc) {
		List<Element> r = new Vector<Element>();
		Iterator<Schema> schemas = desc.getTypes().getSchemas().iterator();
		while (schemas.hasNext()) {
			Schema schema = (Schema) schemas.next();
			r.addAll(schema.getElements());			
		}		
		return r;
	}
	
	
	public int getDataWeight(URL url) {
		int DW = 0;		
		Description desc = read(url);
		
		MessageComplexityCalculator c = new MessageComplexityCalculator();		
		c.initialize();
				
//		// Prueba para utilizar como Input WSDLBaseFaults que no tiene interfaces definidas
//		Iterator<Element> iterElem = getAllElements(desc).iterator(); 		
//		while (iterElem.hasNext()){
//			DW += c.getWeightOf(iterElem.next());
//		}		
		
//		e = new ElementImpl(null, null);
		Iterator<InterfaceType> iter = desc.getInterfaces().iterator();
		
		if (iter.hasNext()) {			
			InterfaceType portType = iter.next();				
			Iterator<Operation> opers = portType.getOperations().iterator();
			
			while (opers.hasNext()) {
				Operation op = (Operation) opers.next();
				Iterator<Part> parts = op.getInput().getParts().iterator();
				while (parts.hasNext()) {
					Part part = (Part) parts.next();					
					try{

						DW += c.getWeightOf(part.getElement());
					}
					catch (Exception noElement){
						
					}				
				}
				parts = op.getOutput().getParts().iterator();
				while (parts.hasNext()) {
					Part part = (Part) parts.next();
					try{

						DW += c.getWeightOf(part.getElement());
					}
					catch (Exception noElement){
						
					}
				}
			}
		}				
		else {
			Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("No Interfaces in WSDL input");
		}
		return DW;
	}

}
