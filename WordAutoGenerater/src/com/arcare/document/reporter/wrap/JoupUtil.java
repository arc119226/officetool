package com.arcare.document.reporter.wrap;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * extends jsoup function
 * @author FUHSIANG_LIU
 *
 */
public class JoupUtil {
	/**
	 * find doc by tagName ignore namespace
	 * @param doc
	 * @param tagName
	 * @return
	 */
	public static Elements ignoreNameSpaceSelect(Document doc, String tagName){
		Elements withTypes = new Elements();
		for( Element element : doc.select("*") ){
		    final String s[] = element.tagName().split(":");
		    if( s.length > 1 && s[1].equals(tagName) == true ){
		        withTypes.add(element);
		    }
		}
		return withTypes;
	}
	/**
	 * find sub element by tagName ignore namespace
	 * @param doc
	 * @param tagName
	 * @return
	 */
	public static Elements ignoreNameSpaceSelect(Element doc, String tagName){
		Elements withTypes = new Elements();
		for( Element element : doc.select("*") ){
		    final String s[] = element.tagName().split(":"); 
		    if( s.length > 1 && s[1].equals(tagName) == true ){
		        withTypes.add(element);
		    }
		}
		return withTypes;
	}
}
