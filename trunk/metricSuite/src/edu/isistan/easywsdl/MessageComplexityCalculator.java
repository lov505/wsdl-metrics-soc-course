package edu.isistan.easywsdl;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.ow2.easywsdl.schema.api.Attribute;
import org.ow2.easywsdl.schema.api.Element;
import org.ow2.easywsdl.schema.api.Sequence;
import org.ow2.easywsdl.schema.api.Type;
import org.ow2.easywsdl.schema.impl.ComplexTypeImpl;
import org.ow2.easywsdl.schema.impl.ExtensionImpl;
import org.ow2.easywsdl.schema.impl.SimpleTypeImpl;

public class MessageComplexityCalculator {

	public Hashtable<QName,Integer> analyzedTypes;		
	public Hashtable<QName,Integer> analyzedArguments;
	public Hashtable<QName,Integer> arguments;
	private boolean flag; 

	public int calculateFor(Element el) {
		analyzedTypes = new Hashtable<QName,Integer>();
		return analyzeElement(el, 0);		
	}

	private int analyzeAttributeGroup(List attGroup, int auxWeight){		
		Attribute		    attNext  = null;	// proximo atributo a ser analizado en el recorrido de listas de atributos		
		Iterator<Attribute> iterAtt  = null;    // iterador de listas de atributos		
		org.ow2.easywsdl.schema.org.w3._2001.xmlschema.Attribute attAux = null; //variable auxiliar para almacenar el atributo bajo analisis

		auxWeight = 0;
		
		Iterator iterAux = attGroup.iterator();
		Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Has Attribute Group: ");
		while (iterAux.hasNext()){
			attAux = (org.ow2.easywsdl.schema.org.w3._2001.xmlschema.Attribute) iterAux.next();																																										
			if (attAux.getType() != null){
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t" + attAux.getType().toString());	
			} else {
				if (attAux.getId()!= null) {
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t" + attAux.getId().toString());
				} else {									
					if (attAux.getRef() != null) {
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t" + attAux.getRef().toString());								
					}										
				}								
			}				
			Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Att/AttGroup Adds: 1");			
			auxWeight = auxWeight + 1;
		}		
		return auxWeight;
	}

	private int analyzeElement(Element e, int acumWeight){

		String 			  elemActual = null;	// Nombre del Elemento Actual como string
		QName  			  tableKey	 = null;	// Qname que utilizaremos como clave en la tabla de elementos ya analizados 	
		Iterator<Element> iter 		 = null;	// iterador auxiliar de estructuras complejas
		Iterator<Element> iter2 	 = null;	// iterador auxiliar de estructuras complejas
		Element 		  elemNext   = null;	// proximo elemento a ser analizado en el recorrido de estructuras complejas

		try {
			elemActual = e.getQName().getLocalPart().trim();
		} catch (Exception e2) {
				
			try{
				elemActual = e.getQName().toString();	
			}
			catch (Exception mayBeNull){
				if (e == null){
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Null element. Skipping...");
					return 0;
				}
			}
			
						
		}

		Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Element under analysis: " + elemActual);

		Type tipo = e.getType(); 
		 //obtenemos el tipo del Elemento 							
		 //este tipo puede ser simple o complejo. Intentamos castearlo a simple, cuyo analisis es mas sencillo. 
		 //Si dispara una excepcion, lo casteamos a complejo y analizamos su estructura.

		//Si el tipo tiene nombre definido lo utilizamos como clave en la hashtable. Caso contrario, utilizamos el nombre del elemento.
		if (tipo.getQName() == null) {
			tableKey = e.getQName();
		} else {
			tableKey = tipo.getQName();
		}
		
		acumWeight = 0;
		
		//Chequeamos que el elemento actual no haya sido analizado previamente.
		if (analyzedTypes.containsKey(tableKey)){
			//Avisamos y devolvemos el valor analizado previamente.
			int aux = analyzedTypes.get(tableKey); 
			Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Element already analyzed: " + tableKey);
			Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Adds: " + aux);
			if (aux != 1){
				flag = true;
			}
			else {
				flag = false;
			}
			return analyzedTypes.get(tableKey);
		} 
		
		// Agregamos el elemento a la tabla y lo analizamos
		analyzedTypes.put(tableKey, 0);			
		try{				
			SimpleTypeImpl simple = (SimpleTypeImpl) tipo;				
			try{
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Simple Type: " + simple.getQName().getLocalPart().trim());
			}
			catch (Exception simpleName){
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Simple Type: " + simple.getQName());
			}
			

			// Ecuacion 12.1	
			// we = ws si el elemento es de tipo simple 
			
			//desglose de ws
			//Ecuacion 17.2 
			//ws = r si el tipo simple esta definido por restriccion					
			int r = 0;									
			try {
				r = simple.getRestriction().getEnumerations().size();									
			} catch (Exception noRestriction) {

			}
			
			if (r != 0) {

				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Restriction: " + simple.getModel().getRestriction().getBase().getLocalPart());
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Adds: " + r);
				acumWeight = acumWeight + r;
//				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Adds: " + 1);
//				acumWeight = acumWeight + 1;
			}
			else{					
				try{						
					//Ecuacion 17.3
					//ws = u si el tipo simple esta definido por union.					
					int u = simple.getModel().getUnion().getSimpleType().size();
					if (u != 0){
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Union: " + simple.getModel().getUnion().toString());
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Adds: " + u);
						acumWeight = acumWeight + u;						
					}					
				}
				catch (Exception noUnion){						
					try{
						//Ecuacion 17.4
						//ws = l si el tipo simple esta derivado por lista
						simple.getModel().getList();
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t List: " + simple.getModel().getList().getItemType().getLocalPart());
					
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Adds: 1");
						acumWeight = acumWeight + 1;	
					}
					catch (Exception noList){
						//Ecuacion 17.1
						//ws = 1 si el tipo simple es built-in		
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Built-in Simple Type adds: 1");							
						acumWeight = acumWeight + 1;
					}												
				}												
			}							
			analyzedTypes.put(tableKey, acumWeight);						
			flag = false;			
			return acumWeight;
		}
		catch (Exception isComplexType){

			// Ecuacion 16 - El elemento es de tipo complejo
			// wc = wcBaseType + Elementos + Atributos + Grupos de Elementos + Grupos de Atributos				

			ComplexTypeImpl complejo = (ComplexTypeImpl) tipo;												

			// Ecuacion 16 - El elemento es de tipo complejo
			// wc = wcBaseType + Elementos + Atributos + Grupos de Elementos + Grupos de Atributos
			try {
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Complex Type: " + complejo.getQName().getLocalPart().trim());
			}
			catch (Exception simpleName){
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Complex Type: " + complejo.getQName());	
			}
			
			//Ecuacion 16 - WcBase.
			Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Base Type of Complex Content adds: 1");
			acumWeight = acumWeight + 1;	

			// el complejo puede tener adentro varias cosas. Vamos chequeando que tiene y sumando si corresponde.
			if (complejo.hasAll()){
				// Ecuacion 16 - sumando los Elementos/Grupos de Elementos

				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Has All: " + complejo.getAll().toString());

				iter2 = complejo.getAll().getElements().iterator();										
				iter  = complejo.getAll().getElements().iterator();

				while (iter2.hasNext()){
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t" + iter2.next().getQName().getLocalPart().toString());
				}

				elemNext = null;
				while (iter.hasNext()){
					elemNext = iter.next();
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t next: " + elemNext.getQName().getLocalPart().trim());
					acumWeight += analyzeElement(elemNext, acumWeight);
				}										

			}

			if (complejo.hasChoice()){
				//Seguimos con la Ecuacion 16 - sumando los Elementos/Grupos de Elementos

				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Has Choice: " + complejo.getChoice().toString());

				iter  = complejo.getChoice().getElements().iterator();
				iter2 = complejo.getChoice().getElements().iterator();

				while (iter2.hasNext()){
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t" + iter2.next().getQName().getLocalPart().toString());
				}

				elemNext = null;
				while (iter.hasNext()){
					elemNext = iter.next();
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t next: " + elemNext.getQName().getLocalPart().trim());
					acumWeight += analyzeElement(elemNext, acumWeight);
				}

			}		

			if (complejo.hasComplexContent()){
				// Seguimos con la Ecuacion 16 - sumando los Elementos.
				// Si el elemento complejo esta definido como "complexContent", la API provee metodos limitados para el analisis. 
				// Podemos chequear si esta definido por extension y si tiene una secuencia.
				
				try{
					
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Has ComplexContent: " + complejo.getComplexContent().getExtension().getBase().getQName());					
				}
				catch (Exception noBaseQName){
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Has ComplexContent (undef Name)");					
				}
								
					ExtensionImpl aux = (ExtensionImpl)complejo.getComplexContent().getExtension();

//					//Ecuacion 16 - WcBase.
//					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Base Type of Complex Content adds: 1");
//					acumWeight = acumWeight + 1;													

					// Ecuacion 13.2 - AnyAttribute dentro del ComplexContent										
					if (aux.getModel().getAnyAttribute() != null){						
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("AnyAttribute inside Complex Content: " + aux.getModel().getAnyAttribute().getId());
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Adds: 1");	
						acumWeight = acumWeight + 1;
					}										

					// Ecuacion 15.1 - Grupos de Atributos dentro del ComplexContent
					if (!aux.getModel().getAttributeOrAttributeGroup().isEmpty()) {		

						acumWeight += analyzeAttributeGroup(aux.getModel().getAttributeOrAttributeGroup(), acumWeight);
					}	
			}

			if (complejo.hasSequence()) {
				// Seguimos con la Ecuacion 16 - sumando los Elementos.
				//Si el complejo tiene una secuencia de elementos, llamamos recursivamente con esos elementos para analizarlos.									

				Sequence seq = complejo.getSequence();				
				iter = seq.getElements().listIterator();						
				iter2 = seq.getElements().iterator();							

				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Has Sequence: ");
				if (!seq.getElements().isEmpty()){
					while (iter2.hasNext()){
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t" + iter2.next().getQName().getLocalPart().toString());
					}								

					elemNext = null;
					while (iter.hasNext()) {
						elemNext = iter.next();
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t next: " + elemNext.getQName().getLocalPart().trim());						
						acumWeight += analyzeElement(elemNext, acumWeight); //Llamada recursiva						
						if (flag == true) {							
							acumWeight += 1;
							Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Complex Element in Sequence: " + elemNext.getQName().getLocalPart().trim() + ". Adds: 1");							
							flag = false;
						}
						
					}						
				}					
				else {
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Empty Sequence Adds: 1");
					acumWeight = acumWeight + 1;
				}					
			}								

			if (complejo.hasSimpleContent()){
				// Seguimos con la Ecuacion 16 - sumando los Elementos.
				// Si el elemento complejo esta definido como "simpleContent",
				// chequeamos si esta definido por extension, con lo cual suma por el tipo base.
				// Ademas, el SimpleContent puede tener atributos, que debemos recuperar y analizar
				// de acuerdo a sus respectivas ecuaciones.

				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Has SimpleContent: " + complejo.getSimpleContent().getExtension().getBase().getQName());

				//Ecuacion 16 - WcBase.
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Base Type of Complex Content adds: 1");
				acumWeight = acumWeight + 1;										


				// Ecuacion 13.2 - AnyAttribute dentro del SimpleContent  
				ExtensionImpl aux = (ExtensionImpl)complejo.getSimpleContent().getExtension();										
				if (aux.getModel().getAnyAttribute() != null){						
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t AnyAttribute: " + aux.getModel().getAnyAttribute().getId());
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Adds: 1");	
					acumWeight = acumWeight + 1;
				}

				// Ecuacion 15.1 - Grupos de Atributos dentro del SimpleContent
				if (!aux.getModel().getAttributeOrAttributeGroup().isEmpty()){			

					acumWeight += analyzeAttributeGroup(aux.getModel().getAttributeOrAttributeGroup(), acumWeight);

				}					

				try{
					// Ecuacion 16 - Analisis de Elementos/Grupos de elementos.
					iter  = complejo.getSimpleContent().getExtension().getSequence().getElements().iterator();
					iter2 = complejo.getSimpleContent().getExtension().getSequence().getElements().iterator();

					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t SimpleContent Has Sequence: ");						
					if (complejo.getSimpleContent().getExtension().getSequence().getElements().isEmpty()){
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Empty Sequence adds: 1");
						acumWeight = acumWeight + 1;
					}
					else {
						while (iter2.hasNext()){							
							Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t" + iter2.next().getQName().getLocalPart().toString());
						}						
						elemNext = null;

						while (iter.hasNext()){
							elemNext = iter.next();
							Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("");
							Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t next: " + elemNext.getQName().getLocalPart().trim());
							acumWeight += analyzeElement(elemNext, acumWeight);
						}							
					}						
				}
				catch (Exception noSequence){

				}																															
			}

			// Ecuacion 16 - analisis de Atributos/Grupos de Atributos				
			// Ecuacion 15.1 Atributos o Grupos de Atributos definidos en el Tipo Complejo
			if (!complejo.getModel().getAttributeOrAttributeGroup().isEmpty()){

				acumWeight += analyzeAttributeGroup(complejo.getModel().getAttributeOrAttributeGroup(), acumWeight);					

			}										

			if (complejo.getModel().getAnyAttribute() != null){

				// Ecuacion 13.2 AnyAttribute definido en el tipo complejo
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t AnyAttribute into Complex Type: " + complejo.getModel().getAnyAttribute().getId());
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t" );
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t adds: 1");
				acumWeight = acumWeight + 1;
				
			}

			if (!complejo.getAttributes().isEmpty()){
				// Ecuacion 16 - analisis de Atributos.					
				acumWeight += analyzeAttributeGroup(complejo.getAttributes(), acumWeight); 

			}			
			analyzedTypes.put(tableKey, acumWeight);			
			if (acumWeight != 1){
				flag = true;
			}
			else{
				flag = false;
			}
			return acumWeight;
		}
		
	}

	public int countArgumentsFor(Element e) {
		analyzedArguments = new Hashtable<QName,Integer>();
		arguments = new Hashtable<QName,Integer>();
		int c = countArgs(e);
		Iterator<QName> recursiveRefs = analyzedArguments.keySet().iterator();
		Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Buscando referencias recursivas en: " + e.getQName() );	
		while (recursiveRefs.hasNext()) {
			QName qName = (QName) recursiveRefs.next();
			int refs = analyzedArguments.get(qName);
			int q = arguments.get(qName);
			Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Hay " + refs + " refs. recursivas c/u con peso " + q + " a: " + qName );
			c += q*refs;
		}
		return c;
	}
	
	public int countArgs(Element e) {	
		if (e==null) 
			return 0;	
		int Nargs = 0;
		Type t = e.getType();		
		if (t instanceof ComplexTypeImpl) {			
			if (t.getQName()!=null) {
				if (analyzedArguments.containsKey(t.getQName())) {
					int recursiveRefs = analyzedArguments.get(t.getQName());
					recursiveRefs++;
					analyzedArguments.put(t.getQName(), recursiveRefs);
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Argumento recursivo: " + t.getQName() );
					return 0;
				} else {
					analyzedArguments.put(t.getQName(), 0);
				}
			}
			ComplexTypeImpl c = (ComplexTypeImpl) t;
			if (c.hasSequence()) {
				List<Element> elements = c.getSequence().getElements();
				for (int i=0; i<elements.size(); i++) {					
					Nargs += countArgs( ( Element) elements.get(i) );				
				}
			}			
			if (c.getAttributes()!=null) {
				Nargs += c.getAttributes().size();
			}				
			Nargs = (Nargs==0) ? 1 : Nargs;	
			if (t.getQName()!=null)
				arguments.put(t.getQName(), Nargs);
		} else {			  
			Nargs = 1;
		}					
		return Nargs;
	}	
}