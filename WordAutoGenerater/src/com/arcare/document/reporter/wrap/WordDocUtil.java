package com.arcare.document.reporter.wrap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
/**
 * 
 * @author FUHSIANG_LIU
 *
 */
public class WordDocUtil {

	/**
	 * poi open docx document
	 * @param filePath
	 * @return
	 */
	public static XWPFDocument readDocx(String filePath){
		try {
			Path path = Paths.get(filePath);
			byte[] byteData = Files.readAllBytes(path);
			XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(byteData));
			return doc;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * copy paragraph
	 * @param clone
	 * @param source
	 */
	public static void cloneParagraph(XWPFParagraph clone, XWPFParagraph source) {
		CTP ctp=CTP.Factory.newInstance();
		ctp.set(source.getCTP());
		clone.getCTP().set(ctp);
	}
}
