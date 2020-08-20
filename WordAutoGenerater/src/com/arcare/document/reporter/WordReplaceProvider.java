package com.arcare.document.reporter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.arcare.document.reporter.wrap.JoupUtil;
/**
 * 
 * @author FUHSIANG_LIU
 *
 */
public class WordReplaceProvider {

	/**
	 * replace document.xml keyword singleVal
	 * @param inputStr --> source xml
	 * @param patternFormat --> regx pattern
	 * @param dataSource --> mapping real data
	 * @param prifix --> bookmark prifix
	 * @return
	 */
	public static String replaceSingleVariableDefineInBookmark(String inputStr,String patternFormat,Map<String,String> dataSource){
		String result=inputStr;
		Set<String> keys=dataSource.keySet();
		for(String k:keys){
			String v=dataSource.get(k);
			Pattern pattern = Pattern.compile(String.format(patternFormat, k));
			Matcher matcher = pattern.matcher(result);
			boolean matchFound = matcher.find();
			if(matchFound){
				String groupStr = matcher.group(0);
				groupStr=groupStr.replaceAll("<w:t>[\\w\\s]+<\\s?\\/w:t>", "<w:t> </w:t>");
		        groupStr=groupStr.replaceFirst("<w:t> <\\s?\\/w:t>",
	            			String.format("<w:t>%s</w:t>", v));
		        result=matcher.replaceAll(groupStr);
			}
		}
	    return result;
	}

	/**
	 * 
	 * @param inputStr
	 * @param patternFormat
	 * @param dataSource
	 * @return
	 */
	public static String insertMultiRowToTABLE(String inputStr,String patternFormat,Map<String,String> dataSource){
		final String processPostfix="_TABLE";
		final Document doc=Jsoup.parse(inputStr, "", Parser.xmlParser());
		final Elements tabls=JoupUtil.ignoreNameSpaceSelect(doc,"tbl");//get aall tbl block
		final List<String> tableBookmarks=dataSource.keySet().stream().map(k->k.substring(0,3)+"_TABLE").distinct().collect(Collectors.toList());
		final String splitStr=",";
		tabls.forEach(tbl->{
			tableBookmarks.stream()
				.filter(tableBookmark->tbl.toString().matches(String.format("[.\\S\\s]*<w:bookmarkStart w:id=\"\\d+\" w:name=\"%s\"\\s?\\/>[.\\S\\s]*", tableBookmark)))
				.forEach(tableBookmark->{
					
					String tablePrifix = tableBookmark.split("_")[0];//table prifix
					List<String> tableCellKeyBookmarks = dataSource.keySet().stream()
							.filter(tableValueBookmarkKey->(tableValueBookmarkKey.startsWith(tablePrifix) && !tableValueBookmarkKey.endsWith(processPostfix)))
								.collect(Collectors.toList());

					String firstValue=dataSource.get(tableCellKeyBookmarks.get(0));
					int count=firstValue.split(splitStr).length;
					Elements trs=JoupUtil.ignoreNameSpaceSelect(tbl,"tr");
					Element dataRowTemplate=null;//=trs.get(1);
					for(Element _tpl:trs){
						if(_tpl.toString().contains(tablePrifix)){
							dataRowTemplate=_tpl;
						}
					}
					String insertRows="";
					for(int valueIndex=0;valueIndex<count;valueIndex++){
						Map<String,String> tableValueMap=new HashMap<String,String>();
						for(int keyIndex=0;keyIndex < tableCellKeyBookmarks.size();keyIndex++){
							String _key=tableCellKeyBookmarks.get(keyIndex);
							tableValueMap.put(_key, dataSource.get(_key).split(splitStr)[valueIndex]);
						}
						insertRows+=WordReplaceProvider.replaceSingleVariableDefineInBookmark(dataRowTemplate.toString(),patternFormat,tableValueMap);
					}
					dataRowTemplate.after(insertRows);
					dataRowTemplate.remove();
					
				});
		});
		//formatter and replace data
		return doc.toString().replaceAll("\r|\n", "").replaceAll("> +", "> ").replaceAll(" +<"," <");
	}

}
