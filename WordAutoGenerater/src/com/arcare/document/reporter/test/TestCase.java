package com.arcare.document.reporter.test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.arcare.document.reporter.WordImageProvider;
import com.arcare.document.reporter.WordReplaceProvider;
import com.arcare.document.reporter.wrap.WrapUtil;

public class TestCase {

	private static void reAggregationTest() throws UnsupportedEncodingException, IOException{
		String rootOutput="./output";
		WrapUtil.cleanOldTemp(rootOutput);
		//prepare datasource map
		//AF1
		Map<String,String> singleRow=WrapUtil.initDataBind("./datasource/SingleRow");
		Map<String,String> multiRow=WrapUtil.initDataBind("./datasource/MultiRow");
		Map<String,String> multiColumn=WrapUtil.initDataBind("./datasource/MultiColumn");
		Map<String,String> multiHeaderV=WrapUtil.initDataBind("./datasource/MultiHeaderV");
		Map<String,String> multiBodyV=WrapUtil.initDataBind("./datasource/MultiBodyV");
		Map<String,String> multiHeaderH=WrapUtil.initDataBind("./datasource/MultiHeaderH");
		Map<String,String> multiBodyH=WrapUtil.initDataBind("./datasource/MultiBodyH");

		//1.process insert image to table grid
		String outputTempFile = WordImageProvider.processAllImgTable("./resource/template_1.docx","./output",multiColumn);
		System.out.println(outputTempFile);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String currentDate=sdf.format(new Date());
		WrapUtil.unzip(outputTempFile, rootOutput+File.separator+currentDate);
		
		String inputStr = new String(Files.readAllBytes(Paths.get("./output/"+currentDate+"/word/document.xml")),"UTF-8");
		String patternFormat = "<w:bookmarkStart w:id=\"\\d+\" w:name=\"%s\"\\s?\\/>([\\s\\S]*?)<w:bookmarkEnd w:id=\"\\d+\"\\s?\\/>";
		String result = "";
		//2. process single val
		result = WordReplaceProvider.replaceSingleVariableDefineInBookmark(inputStr, patternFormat, singleRow);
	    //3. process multiRow table val
		result = WordReplaceProvider.insertMultiRowToTABLE(result,patternFormat,multiRow);
	    
	    Files.write(Paths.get(rootOutput+File.separator+currentDate+"/word/document.xml"), result.getBytes());
	    //3.zip to docx
	    WrapUtil.zip(rootOutput+File.separator+currentDate,rootOutput+File.separator+currentDate+".docx");
	}
	

	/**
	 * test case suit
	 * @param args
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		reAggregationTest();
	}
	


}
