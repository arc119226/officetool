package com.arcare.document.reporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;

import com.arcare.document.reporter.wrap.WordDocUtil;
import com.arcare.document.reporter.wrap.WrapUtil;

/**
 * 
 * @author FUHSIANG_LIU
 *
 */
public class WordMultiHeaderBodyProvider {

	public static void main(String args[]) throws Exception{
		String rootOutput="./output";
		WrapUtil.cleanOldTemp(rootOutput);
		Map<String,String> multiHeaderV=WrapUtil.initDataBind("./datasource/MultiHeaderV");
		Map<String,String> multiBodyV=WrapUtil.initDataBind("./datasource/MultiBodyV");
		processAllHeaderBodyTableV("./resource/template_1.docx","./output", multiHeaderV, multiBodyV);
//		Map<String,String> multiHeaderV = WrapUtil.initDataBind("./datasource/MultiHeaderV");
//		Map<String,String> multiBodyV = WrapUtil.initDataBind("./datasource/MultiBodyV");
//
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
//		String currentDate=sdf.format(new Date());
//		WrapUtil.unzip("./resource/multiV.docx", rootOutput+File.separator+currentDate);
//		String inputStr = new String(Files.readAllBytes(Paths.get("./output/"+currentDate+"/word/document.xml")),"UTF-8");
//
//		String patternFormat = "<w:bookmarkStart w:id=\"(\\d+)\" w:name=\"%s\"\\s?\\/>([\\s\\S]*?)<w:bookmarkEnd w:id=\"\\1\"\\s?\\/>|<w:bookmarkStart w:name=\"%s\" w:id=\"(\\d+)\"\\s?\\/>([\\s\\S]*?)<w:bookmarkEnd w:id=\"\\1\"\\s?\\/>";
//		String result=WordMultiHeaderBodyProvider.insertMultiHeaderBodyVToDoc(inputStr,patternFormat,multiHeaderV,multiBodyV);
//		
//	    Files.write(Paths.get(rootOutput+File.separator+currentDate+"/word/document.xml"), result.getBytes());
//	    //3.zip to docx
//	    WrapUtil.zip(rootOutput+File.separator+currentDate,rootOutput+File.separator+currentDate+".docx");
	}

	public static String processAllHeaderBodyTableV(
			String templateFilePath,String outputDirPath,
			Map<String,String> header,
			Map<String,String> body){
		String outputFile=null;
		FileOutputStream out=null;
		try{
			XWPFDocument docx=WordDocUtil.readDocx(templateFilePath);
			List<String> headerBookMarkPrefix = header.keySet().stream()
					.map(k->k.substring(0,3)).distinct()
					.collect(Collectors.toList());
			
			List<String> bodyBookMarkPrefix = body.keySet().stream()
					.map(k->k.substring(0,3)).distinct()
					.collect(Collectors.toList());
			
			Map<String,Queue<String>> dataHeaderQueueMap=new HashMap<>();
			headerBookMarkPrefix.forEach(prefix->{
				dataHeaderQueueMap.put(prefix+"TestStage",
						new LinkedList<String>(Arrays.asList(header.get(prefix+"TestStage").split(","))));
			});
			Map<String,Queue<String>> dataBodyQueueMap=new HashMap<>();
			bodyBookMarkPrefix.forEach(prefix->{
				body.keySet().forEach(key->{
					if(key.startsWith(prefix)){
						dataBodyQueueMap.put(key, 
								new LinkedList<String>(Arrays.asList(body.get(key).split(","))));
					}
				});
			});
			
			
			List<XmlCursor> cursorList=new ArrayList<>();

			dataHeaderQueueMap.forEach((key,headerQueue)->{
				XWPFParagraph templateParagraph=findHeaderParagraphByBookMark(docx,key);
				XWPFTable templateTable=null;
				if(!bodyBookMarkPrefix.isEmpty()){
					 templateTable=findBodyTableByBookMark(docx,bodyBookMarkPrefix.get(0)+"_TABLE");
				}
				while(headerQueue.peek()!=null){
					String headerTitle=headerQueue.poll();
					//取得template

					if(templateParagraph != null && templateTable!=null){
							if(cursorList.size()==0){
								XmlCursor tmpTblCursor = templateTable.getCTTbl().newCursor();
								tmpTblCursor.toEndToken();
								cursorList.add(tmpTblCursor);
							}
							
							XWPFParagraph cp=copyParagraphToCurserAndUpdateText(
									templateParagraph,
									docx,
									cursorList.get(cursorList.size()-1),
									headerTitle);
							;
							XmlCursor cpCursor=cp.getCTP().newCursor();
							cpCursor.toEndToken();
							cursorList.add(cpCursor);
							XWPFTable cloneTable=copyTable(templateTable,docx,cursorList.get(cursorList.size()-1));
							System.out.println("-"+headerTitle);
							//process newTable..
							insertDataToTable(cloneTable,bodyBookMarkPrefix.get(0),headerTitle,dataBodyQueueMap);

					}
					
				}
				//remove template
				int position = docx.getPosOfTable( templateTable );
				docx.removeBodyElement( position );
				position=docx.getPosOfParagraph(templateParagraph);
				docx.removeBodyElement( position );
	
			});
			//remove first header and table.

			String uuid = UUID.randomUUID().toString(); 
			File outputDir=new File(outputDirPath);
			outputDir.mkdirs();
			outputFile=outputDirPath+File.separator+uuid+".docx";
			out = new FileOutputStream(outputFile);
			docx.write(out);
			docx.close();
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(out!=null){
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return outputFile;
	}
	/**
	 * process body queue map by header title
	 * @param currentTable
	 * @param headerTitle
	 * @param ddataBodyQueueMap
	 * @return
	 */
	public static XWPFTable insertDataToTable(XWPFTable currentTable,String prefix,String headerTitle,Map<String,Queue<String>> dataBodyQueueMap){
		System.out.println("insertDataToTable");
		List<XWPFTableRow> rows = currentTable.getRows();
		Map<Integer,String> indexMapping=new HashMap<>();
		XWPFTableRow row=rows.get(1);
		List<XWPFTableCell> cells=row.getTableCells();
		for(int i=0;i<cells.size();i++){
			XWPFTableCell cell=cells.get(i);
			indexMapping.put(i, cell.getText().trim());
		}

		Queue<String> refQueue=dataBodyQueueMap.get(prefix+"TestStage");
		
		if(!refQueue.peek().equals(headerTitle)) {
			System.out.println(headerTitle+" != "+refQueue.peek());
			return currentTable;
		}
		
		int i=1;
		int rowSize=rows.size();
		System.out.println("i="+i+",rowSize="+rowSize);
		while(refQueue.peek() != null && 
			refQueue.peek().equals(headerTitle)){//is reference
			System.out.println(headerTitle+" == "+refQueue.peek());
			String stageText=refQueue.poll();
//			System.out.println(headerTitle+" = "+stageText);
			//update'
			for(;i<rowSize;i++){
				Iterator<Integer> it=indexMapping.keySet().iterator();
				while(it.hasNext()){
					Integer index=it.next();
					String key=indexMapping.get(index);
					XWPFTableCell cell=rows.get(i).getTableCells().get(index);
					if(!cell.getParagraphs().isEmpty()) {
						cell.removeParagraph(0);
					}
					cell.setText(dataBodyQueueMap.get(key).poll());
				}
			}
			//insert
			if(i>=rowSize){
//				CTRow _ctrow=CTRow.Factory.newInstance();
//				_ctrow.set(row.getCtRow());
//				XWPFTableRow newRow = new XWPFTableRow(_ctrow, currentTable);
//				System.out.println(currentTable.addRow(newRow, rows.size()));
//				XWPFTableRow currentRow=rows.get(rows.size()-1);
				Iterator<Integer> it=indexMapping.keySet().iterator();
				while(it.hasNext()){
					Integer index=it.next();
					String key=indexMapping.get(index);
//					XWPFTableCell cell=currentRow.getTableCells().get(index);
//					if(!cell.getParagraphs().isEmpty()) {
//						cell.removeParagraph(0);
//					}
//					cell.setText(dataBodyQueueMap.get(key).poll());
//					dataBodyQueueMap.get(key).poll();
				}
			}
		}
		return currentTable;
	}
	
	static int w=2;
	
	/**
	 * 
	 * @param template
	 * @param docx
	 * @param cursor
	 */
	public static XWPFTable copyTable(XWPFTable template,XWPFDocument docx,XmlCursor cursor){
		cursor.toEndToken();
		while(cursor.toNextToken() != org.apache.xmlbeans.XmlCursor.TokenType.START);
		XWPFTable targetTable=docx.insertNewTbl(cursor);
		targetTable.setWidth(template.getWidth());
		copyTable(template,targetTable);
		return targetTable;
	}
	/**
	 * 
	 * @param source
	 * @param target
	 */
	private static void copyTable(XWPFTable source, XWPFTable target) {
		CTTblPr cttblpr=CTTblPr.Factory.newInstance();
		cttblpr.set(source.getCTTbl().getTblPr().copy());
		CTTblGrid cttblgrid=CTTblGrid.Factory.newInstance();
		cttblgrid.set(source.getCTTbl().getTblGrid().copy());
		
	    target.getCTTbl().setTblPr(cttblpr);
	    target.getCTTbl().setTblGrid(cttblgrid);
	    
	    for (int r = 0; r<source.getRows().size(); r++) {
	        XWPFTableRow targetRow = target.createRow();
	        XWPFTableRow sourceRow = source.getRows().get(r);
	        
	        CTTrPr _cttrPr=CTTrPr.Factory.newInstance();
	        _cttrPr.set(sourceRow.getCtRow().getTrPr().copy());
	        targetRow.getCtRow().setTrPr(_cttrPr);
	        
	        for (int c=0; c<sourceRow.getTableCells().size(); c++) {
	            //newly created row has 1 cell
	            XWPFTableCell targetCell = c==0 ? targetRow.getTableCells().get(0) : targetRow.createCell();
	            XWPFTableCell sourceCell = sourceRow.getTableCells().get(c);
	            CTTcPr _cttcpr=CTTcPr.Factory.newInstance();
	            _cttcpr.set(sourceCell.getCTTc().getTcPr().copy());
	            targetCell.getCTTc().setTcPr(_cttcpr);
	            
	            XmlCursor cursor = targetCell.getParagraphArray(0).getCTP().newCursor();
	            for (int p = 0; p < sourceCell.getBodyElements().size(); p++) {
	                IBodyElement elem = sourceCell.getBodyElements().get(p);
	                if (elem instanceof XWPFParagraph) {
	                    XWPFParagraph targetPar = targetCell.insertNewParagraph(cursor);
	                    cursor.toNextToken();
	                    XWPFParagraph par = (XWPFParagraph) elem;
	                    copyParagraph(par, targetPar);
	                } else if (elem instanceof XWPFTable) {
	                    XWPFTable targetTable = targetCell.insertNewTbl(cursor);
	                    XWPFTable table = (XWPFTable) elem;
	                    copyTable(table, targetTable);
	                    cursor.toNextToken();
	                }
	            }
	            //newly created cell has one default paragraph we need to remove
	            targetCell.removeParagraph(targetCell.getParagraphs().size()-1);
	            cursor.toEndToken();
	    		while(cursor.toNextToken() != org.apache.xmlbeans.XmlCursor.TokenType.START);
	        }
	    }
	    //newly created table has one row by default. we need to remove the default row.
	    target.removeRow(0);
	}
	/**
	 * 
	 * @param source
	 * @param target
	 */
	private static void copyParagraph(XWPFParagraph source, XWPFParagraph target) {
		CTPPr _ctppr=CTPPr.Factory.newInstance();
		_ctppr.set(source.getCTP().getPPr().copy());
	    target.getCTP().setPPr(_ctppr);
	    for (int i=0; i<source.getRuns().size(); i++ ) {
	        XWPFRun sourceRun = source.getRuns().get(i);
	        XWPFRun targetRun = target.createRun();
	        CTR _ctr=CTR.Factory.newInstance();
	        _ctr.set(sourceRun.getCTR().copy());
	        targetRun.getCTR().set(_ctr);
	    }
	}
	/**
	 * 
	 * @param docx
	 * @param tableBookmark
	 * @return
	 */
	public static XWPFTable findBodyTableByBookMark(XWPFDocument docx,String tableBookmark){
		XWPFTable templateTable=null;
		//找template table
		for(XWPFTable table:docx.getTables()){
			if(table.getCTTbl().toString().contains(tableBookmark)){//RV2_TABLE
				templateTable=table;
				break;
			}
		}
		return templateTable;
	}
	/**
	 * 
	 * @param docx
	 * @param headerBookmark
	 * @return
	 */
	public static XWPFParagraph findHeaderParagraphByBookMark(XWPFDocument docx,String headerBookmark){
		for(XWPFParagraph p:docx.getParagraphs()){
			for(XWPFRun r:p.getRuns()){
				if(r.getCTR().toString().contains(headerBookmark)){
					return p;
				}
			}
		}
		return null;
	}
	/**
	 * 複製段落 並更改標題
	 * @param p
	 * @param docx
	 * @param cursor
	 * @return
	 */
	public static XWPFParagraph copyParagraphToCurserAndUpdateText(XWPFParagraph p,XWPFDocument docx,XmlCursor cursor,String text){
		cursor.toEndToken();
		while(cursor.toNextToken() != org.apache.xmlbeans.XmlCursor.TokenType.START);
		XWPFParagraph newParagraph=docx.insertNewParagraph(cursor);
		copyParagraph(p,newParagraph);
		newParagraph.getRuns().get(0).setText(text, 0);
		return newParagraph;
	}
}
