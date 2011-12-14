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

import com.sun.xml.bind.v2.schemagen.xmlschema.ComplexContent;

public class MessageComplexityCalculator {

	public Hashtable<QName,Integer> analyzedTypes;	

	public int calculateFor(Element el) {
		analyzedTypes = new Hashtable<QName,Integer>();
		return analyzeElement(el, 0);
	}

	private int analyzeAttributeGroup(List attGroup, int acum){		
		Attribute		    attNext  = null;	// pr�ximo atributo a ser analizado en el recorrido de listas de atributos		
		Iterator<Attribute> iterAtt  = null;    // iterador de listas de atributos		
		org.ow2.easywsdl.schema.org.w3._2001.xmlschema.Attribute attAux = null; //variable auxiliar para almacenar el atributo bajo an�lisis

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
			acum = acum + 1;
		}		
		return acum;
	}

	private int analyzeElement(Element e, int acum){

		String 			  elemActual = null;	// Nombre del Elemento Actual como string
		QName  			  tableKey	 = null;	// Qname que utilizaremos como clave en la tabla de elementos ya analizados 	
		Iterator<Element> iter 		 = null;	// iterador auxiliar de estructuras complejas
		Iterator<Element> iter2 	 = null;	// iterador auxiliar de estructuras complejas
		Element 		  elemNext   = null;	// pr�ximo elemento a ser analizado en el recorrido de estructuras complejas

		try {
			elemActual = e.getQName().getLocalPart().trim();
		} catch (Exception e2) {
			elemActual = e.getQName().toString();			
		}

		Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Element under analysis: " + elemActual);

		Type tipo = e.getType(); //obtenemos el tipo del Elemento 							
		//este tipo puede ser simple o complejo. Intentamos castearlo a simple, cuyo an�lisis es m�s sencillo. 
		//Si dispara una excepci�n, lo casteamos a complejo y analizamos su estructura.

		//Si el tipo tiene nombre definido lo utilizamos como clave en la hashtable. Caso contrario, utilizamos el nombre del elemento.
		if (tipo.getQName() == null) {
			tableKey = e.getQName();
		} else {
			tableKey = tipo.getQName();
		}


		analyzedTypes.put(tableKey, -1);			
		try{				
			SimpleTypeImpl simple = (SimpleTypeImpl) tipo;				
			try{
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Simple Type: " + simple.getQName().getLocalPart().trim());
			}
			catch (Exception simpleName){
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Simple Type: " + simple.getQName());
			}

			// Ecuaci�n 12.1	
			// we = ws si el elemento es de tipo simple 

			//desglose de ws
			//Ecuaci�n 17.2 
			//ws = r si el tipo simple est� definido por restricci�n					
			int r = 0;									
			try {
				r = simple.getModel().getRestriction().getFacets().size();									
			} catch (Exception noRestriction) {

			}

			if (r != 0) {
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Restriction: " + simple.getModel().getRestriction().getBase().getLocalPart());
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Adds: " + r);
				acum = acum + r;
			}
			else{					
				try{						
					//Ecuaci�n 17.3
					//ws = u si el tipo simple est� definido por uni�n.						
					int u = simple.getModel().getUnion().getSimpleType().size();
					if (u != 0){
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Union: " + simple.getModel().getUnion().toString());
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Adds: " + u);
						acum = acum + u;						
					}					
				}
				catch (Exception noUnion){						
					try{
						//Ecuaci�n 17.4
						//ws = l si el tipo simple est� derivado por lista
						simple.getModel().getList();
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t List: " + simple.getModel().getList().getItemType().getLocalPart());
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Adds: 1");
						acum = acum + 1;	
					}
					catch (Exception noList){
						//Ecuaci�n 17.1
						//ws = 1 si el tipo simple es built-in		
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Built-in Simple Type adds: 1");							
						acum = acum + 1;
					}												
				}												
			}																																													
		}
		catch (Exception isComplexType){

			// Ecuaci�n 16 - El elemento es de tipo complejo
			// wc = wcBaseType + Elementos + Atributos + Grupos de Elementos + Grupos de Atributos				

			ComplexTypeImpl complejo = (ComplexTypeImpl) tipo;												

			// Ecuacion 16 - C�lculo de wcBaseType. MOVER DE AC�		

			try {
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Complex Type: " + complejo.getQName().getLocalPart().trim());
			}
			catch (Exception e2){
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Complex Type: " + complejo.getQName());	
			}


			// el complejo puede tener adentro varias cosas. Vamos chequeando qu� tiene y sumando si corresponde.

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
					acum += analyzeElement(elemNext, acum);
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
					acum += analyzeElement(elemNext, acum);
				}

			}		

			if (complejo.hasComplexContent()){
				// Seguimos con la Ecuacion 16 - sumando los Elementos.
				// Si el elemento complejo est� definido como "complexContent", la API provee m�todos limitados para el an�lisis. 
				// Podemos chequear si est� definido por extensi�n y si tiene una secuencia.

				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Has ComplexContent: " + complejo.getComplexContent().getExtension().getBase().getQName());			
				ExtensionImpl aux = (ExtensionImpl)complejo.getComplexContent().getExtension();

				//Ecuacion 16 - WcBase.
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Base Type of Complex Content adds: 1");
				acum = acum + 1;													

				// Ecuaci�n 13.2 - AnyAttribute dentro del ComplexContent										
				if (aux.getModel().getAnyAttribute() != null){						
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("AnyAttribute inside Complex Content: " + aux.getModel().getAnyAttribute().getId());
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Adds: 1");	
					acum = acum + 1;
				}										

				// Ecuaci�n 15.1 - Grupos de Atributos dentro del ComplexContent
				if (!aux.getModel().getAttributeOrAttributeGroup().isEmpty()) {									
					analyzeAttributeGroup(aux.getModel().getAttributeOrAttributeGroup(), acum);										
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
						acum += analyzeElement(elemNext, acum); //Llamada recursiva			
					}						
				}					
				else {
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Empty Sequence adds 1");
					acum = acum + 1;
				}					
			}								

			if (complejo.hasSimpleContent()){
				// Seguimos con la Ecuacion 16 - sumando los Elementos.
				// Si el elemento complejo est� definido como "simpleContent",
				// chequeamos si est� definido por extensi�n, con lo cual suma por el tipo base.
				// Adem�s, el SimpleContent puede tener atributos, que debemos recuperar y analizar
				// de acuerdo a sus respectivas ecuaciones.

				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Has SimpleContent: " + complejo.getSimpleContent().getExtension().getBase().getQName());

				//Ecuacion 16 - WcBase.
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Base Type of Complex Content adds: 1");
				acum = acum + 1;										


				// Ecuaci�n 13.2 - AnyAttribute dentro del SimpleContent  
				ExtensionImpl aux = (ExtensionImpl)complejo.getSimpleContent().getExtension();										
				if (aux.getModel().getAnyAttribute() != null){						
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t AnyAttribute: " + aux.getModel().getAnyAttribute().getId());
					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t Adds: 1");	
					acum = acum + 1;
				}

				// Ecuaci�n 15.1 - Grupos de Atributos dentro del SimpleContent
				if (!aux.getModel().getAttributeOrAttributeGroup().isEmpty()){			

					acum += analyzeAttributeGroup(aux.getModel().getAttributeOrAttributeGroup(), acum);

				}					

				try{
					// Ecuacion 16 - Analisis de Elementos/Grupos de elementos.
					iter  = complejo.getSimpleContent().getExtension().getSequence().getElements().iterator();
					iter2 = complejo.getSimpleContent().getExtension().getSequence().getElements().iterator();

					Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t SimpleContent Has Sequence: ");						
					if (complejo.getSimpleContent().getExtension().getSequence().getElements().isEmpty()){
						Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t Empty Sequence adds: 1");
						acum = acum + 1;
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
							acum += analyzeElement(elemNext, acum);
						}							
					}						
				}
				catch (Exception noSequence){

				}																															
			}

			// Ecuaci�n 16 - an�lisis de Atributos/Grupos de Atributos				
			// Ecuacion 15.1 Atributos o Grupos de Atributos definidos en el Tipo Complejo
			if (!complejo.getModel().getAttributeOrAttributeGroup().isEmpty()){

				acum += analyzeAttributeGroup(complejo.getModel().getAttributeOrAttributeGroup(), acum);					

			}										

			if (complejo.getModel().getAnyAttribute() != null){

				// Exuacion 13.2 AnyAttribute definido en el tipo complejo
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t AnyAttribute into Complex Type: " + complejo.getModel().getAnyAttribute().getId());
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t \t" );					
				acum = acum + 1;
				Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("\t adds: 1");
			}

			if (!complejo.getAttributes().isEmpty()){
				// Ecuaci�n 16 - an�lisis de ATRIBUTOS.					
				acum += analyzeAttributeGroup(complejo.getAttributes(), acum); 

			}								
		}

		return acum;
	}

	public int countArgumentsFor(Element e) {
		Type t = e.getType();
		Logger.getLogger(MessageComplexityCalculator.class.getName()).debug("Type: " + e.getQName() );
		if (t instanceof ComplexTypeImpl) {		
			ComplexTypeImpl c = (ComplexTypeImpl) t;
			int Nargs = 0;
			// TO DO: Revisar si hace falta contemplar otros casos 
			List<Element> elements = c.getSequence().getElements();
			for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
				Nargs += countArgumentsFor( ( Element) iterator.next() );				
			}
			return Nargs;
		} else {
			// Casos: simple,  
			return 1;			
		}
	}	
}