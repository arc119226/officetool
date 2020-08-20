package com.arcare.document.reporter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import com.arcare.document.reporter.wrap.WordDocUtil;
/**
 * 
 * @author FUHSIANG_LIU
 *
 */
public class WordImageProvider {
	
	/**
	 * 1.scan dataSource map filter後贅字為_IMGTABLE collect其前贅字
	 * 2.處理插入圖片
	 * 3.輸出暫存word檔案路徑
	 * @param templateFilePath
	 * @param outputDirPath
	 * @param dataSource
	 * @return
	 * @throws IOException
	 */
	public static String processAllImgTable(String templateFilePath,String outputDirPath,Map<String,String> dataSource){
		String outputFile=null;
		FileOutputStream out=null;
		try{
			XWPFDocument docx=WordDocUtil.readDocx(templateFilePath);
			List<String> imgTableBookmarkPrefix = dataSource.keySet().stream()
					.map(k->k.substring(0,3)).distinct()
					.collect(Collectors.toList());
			System.out.println(imgTableBookmarkPrefix);
			imgTableBookmarkPrefix.forEach(prefix->{
				try {
					Map<String,Queue<String>> dataQueueMap=new HashMap<>();
					dataQueueMap.put(prefix+"PhotoTag",new LinkedList<String>(Arrays.asList(dataSource.get(prefix+"PhotoTag").split(","))));
					dataQueueMap.put(prefix+"PhotoData", new LinkedList<String>(Arrays.asList(dataSource.get(prefix+"PhotoData").split(","))));
					Map<Integer,String> indexKeyMap=prepareImgTableMapping(docx,prefix+"_IMGTABLE",dataQueueMap);
					processImgTableUpdateAndInsert(docx,prefix+"_IMGTABLE",dataQueueMap,indexKeyMap);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
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
	 * 
	 * @param docx
	 * @param tableBookmark
	 * @param dataQueueMap
	 * @param keyIndexMap
	 */
	private static void processImgTableUpdateAndInsert(XWPFDocument docx,String tableBookmark,Map<String,Queue<String>> dataQueueMap,Map<Integer,String> keyIndexMap){
		List<XWPFTable> tables = docx.getTables();
		tables.stream().filter(table->table.getCTTbl().toString().matches(String.format("[.\\S\\s]*<w:bookmarkStart w:id=\"\\d+\" w:name=\"%s\"\\s?\\/>[.\\S\\s]*", tableBookmark)))
        .forEach(table->{
        	List<XWPFTableRow> tableRows = table.getRows();
        	int defaultHeight = tableRows.get(0).getHeight();
        	int increaseStep=keyIndexMap.size();
        	//一次取得N列
        	int cellSize=0;
        	int rowIndex=0;
        	//update to cell
	    	for(;rowIndex < tableRows.size(); rowIndex += increaseStep){
	    		for(int stepKeyIndex = rowIndex,step=0;step < increaseStep; stepKeyIndex++,step++){
	    			String rowDataKey=keyIndexMap.get(stepKeyIndex%increaseStep);
	    			XWPFTableRow row=tableRows.get(stepKeyIndex);
	    			row.setHeight(defaultHeight);
	    			List<XWPFTableCell> celllist=row.getTableCells();
	    			cellSize=celllist.size();
	    			celllist.forEach(it->{
	    				Queue<String> dataQueue=dataQueueMap.get(rowDataKey);
	    				String obj=dataQueue.poll();
	    				if(obj!=null){
	    					try{
	    						if(it.getParagraphs().size()>0){
	    							it.removeParagraph(0);
	    						}
	    					}catch(Exception e){
	    						e.printStackTrace();
	    					}
	    					if(rowDataKey.contains("PhotoTag")) {
	    						it.setText(obj);
	    					}else if(rowDataKey.contains("PhotoData")) {
    							try {
    								File image = new File(obj);
    	    	        			BufferedImage bimg = null;
    								bimg = ImageIO.read(image);
    			        			int width = bimg.getWidth();
    			        			int height = bimg.getHeight();
    			        			int imgFormat = getImageFormat(image.getName());
    			        			
    			        			it.addParagraph().createRun().addPicture(new FileInputStream(image),
    										imgFormat,
    										image.getName(),
    										Units.toEMU(width),
    										Units.toEMU(height));
    							} catch (Exception e1) {
    								e1.printStackTrace();
    							}
	    					}
	    				}
	    			});
	    		}
	    	}

	    	double generateRow=Math.ceil((double)dataQueueMap.get(keyIndexMap.get(0)).size()/cellSize);
	    	for(int t=0;t<keyIndexMap.size()*generateRow;t++) {
	    		table.createRow().getTableCells();
	    	}

	    	//insert data
	    	for(;rowIndex < tableRows.size(); rowIndex += increaseStep){
	    		for(int stepKeyIndex = rowIndex,step=0;step < increaseStep; stepKeyIndex++,step++){
	    			String rowDataKey=keyIndexMap.get(stepKeyIndex%increaseStep);
	    			XWPFTableRow row=tableRows.get(stepKeyIndex);//
	    			List<XWPFTableCell> celllist=row.getTableCells();
	    			cellSize=celllist.size();
	    			celllist.forEach(it->{
	    				Queue<String> dataQueue=dataQueueMap.get(rowDataKey);
	    				String obj=dataQueue.poll();
	    				if(obj!=null){
	    					try{
	    						if(it.getParagraphs().size()>0){
	    							it.removeParagraph(0);
	    						}
	    					}catch(Exception e){
	    						e.printStackTrace();
	    					}
	    					if(rowDataKey.contains("PhotoTag")) {
	    						it.setText(obj);
	    					}else if(rowDataKey.contains("PhotoData")) {
	    						try {
    								File image = new File(obj);
    	    	        			BufferedImage bimg = null;
    								bimg = ImageIO.read(image);
    			        			int width = bimg.getWidth();
    			        			int height = bimg.getHeight();
    			        			int imgFormat = getImageFormat(image.getName());
    			        			
    			        			it.addParagraph().createRun().addPicture(new FileInputStream(image),
    										imgFormat,
    										image.getName(),
    										Units.toEMU(width),
    										Units.toEMU(height));
    							} catch (Exception e1) {
    								e1.printStackTrace();
    							}
	    					}
	    				}
	    			});
	    		}
	    	}
        });
	}
	/**
	 * 
	 * @param docx
	 * @param tableBookmark
	 * @param dataMap
	 * @return
	 * @throws IOException
	 */
	 private static Map<Integer,String> prepareImgTableMapping(XWPFDocument docx,String tableBookmark, Map<String,Queue<String>> dataMap){ 
		//find image table cell with bookmarks
		//one time n step
		final Map<Integer,String> tableIndexDataMapiing=new HashMap<>();
		docx.getTables().stream().filter(table->table.getCTTbl().toString().matches(String.format("[.\\S\\s]*<w:bookmarkStart w:id=\"\\d+\" w:name=\"%s\"\\s?\\/>[.\\S\\s]*", tableBookmark)))
		.forEach(table->{
			Set<String> keys=dataMap.keySet();
			List<XWPFTableRow> tableRows = table.getRows();
			for(int i=0;i<tableRows.size();i++){
				XWPFTableRow row=tableRows.get(i);
				for(String key:keys){
					if(row.getCtRow().toString().matches(String.format("[.\\S\\s]*<w:bookmarkStart w:id=\"\\d+\" w:name=\"%s\"\\s?\\/>[.\\S\\s]*", key))){
		    			tableIndexDataMapiing.put(i,key);
		    		}
				}
			}
		});
		return tableIndexDataMapiing;      
	} 

	private static int getImageFormat(String imgFileName) {
		int format;
		if (imgFileName.toLowerCase().endsWith(".emf"))
			format = XWPFDocument.PICTURE_TYPE_EMF;
		else if (imgFileName.toLowerCase().endsWith(".wmf"))
			format = XWPFDocument.PICTURE_TYPE_WMF;
		else if (imgFileName.toLowerCase().endsWith(".pict"))
			format = XWPFDocument.PICTURE_TYPE_PICT;
		else if (imgFileName.toLowerCase().endsWith(".jpeg") || imgFileName.toLowerCase().endsWith(".jpg"))
			format = XWPFDocument.PICTURE_TYPE_JPEG;
		else if (imgFileName.toLowerCase().endsWith(".png"))
			format = XWPFDocument.PICTURE_TYPE_PNG;
		else if (imgFileName.toLowerCase().endsWith(".dib"))
			format = XWPFDocument.PICTURE_TYPE_DIB;
		else if (imgFileName.toLowerCase().endsWith(".gif"))
			format = XWPFDocument.PICTURE_TYPE_GIF;
		else if (imgFileName.toLowerCase().endsWith(".tiff"))
			format = XWPFDocument.PICTURE_TYPE_TIFF;
		else if (imgFileName.toLowerCase().endsWith(".eps"))
			format = XWPFDocument.PICTURE_TYPE_EPS;
		else if (imgFileName.toLowerCase().endsWith(".bmp"))
			format = XWPFDocument.PICTURE_TYPE_BMP;
		else if (imgFileName.toLowerCase().endsWith(".wpg"))
			format = XWPFDocument.PICTURE_TYPE_WPG;
		else {
			return 0;
		}
		return format;
	}
}
