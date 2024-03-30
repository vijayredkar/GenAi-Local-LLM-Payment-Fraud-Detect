package com.genai.llm.fraud.detect.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import org.springframework.stereotype.Service;

@Service
public class FileUtilsService 
{			
	/*
	 * general file operations
	 */
	public List<String> readFile(String fileNameWithFullPath) 
	{
		List<String> lines = new ArrayList<String>();
		Path path = Paths.get(fileNameWithFullPath);
		
		try 
		{
			 lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return lines;
	}
	
	/*
	 * extract text from PDF
	 */	
	public List<String> readPdf(String fileNameWithFullPath) 
	{	
		List<String> contextBldr = new ArrayList<String>();
		List<String> lines = new ArrayList<String>();
		Path path = Paths.get(fileNameWithFullPath);
		try 
		{
			 //lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			String pdfFilePath = fileNameWithFullPath;
			
		    PdfReader pdfReader = new PdfReader(pdfFilePath);
		    int numPages = pdfReader.getNumberOfPages();
		    
		    for (int i = 1; i <= numPages; i++) 
		    {
		    	String alllPdfText = PdfTextExtractor.getTextFromPage(pdfReader, i);
		    	String[] pdfLines = alllPdfText.split("\n");
		    	
		    	Arrays.asList(pdfLines)
		    		  .stream()
		    		  .filter(line -> !"".equals(line.trim()))
		    		  .map(line -> lines.add(line))
		    		  .toList();		    	
//		    	lines.add(PdfTextExtractor.getTextFromPage(pdfReader, i));
		    } 
		    
		    refinePdfExtract(lines, contextBldr);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		return contextBldr;
	}
	
	private List<String> refinePdfExtract(List<String> lines, List<String> contextBldr) 
	{	
		lines.remove(0); //BankNext Credit Card Statement
		lines.remove(4); // Date  Transaction Details                Currency   Debit  
		lines.remove(4); // ----------------------------------------------------------
		lines.remove(lines.size() -1); // ----------------------------------- End of statement ---------------------------------------------- 
		
		String customerName = lines.get(0).trim().replaceAll("\\.", "");// John Mayo.
		lines.remove(0);
		String address = lines.get(0).trim().replaceAll("\\.", "");// 5, Saltwell Street.
		lines.remove(0);
		String city = lines.get(0).trim().replaceAll("\\.", "");// London.
		lines.remove(0);
		String country = lines.get(0).trim().replaceAll("\\.", "");// United Kingdom.
		lines.remove(0);
				
		//only txn line items remain in the list now on 
		for(String line : lines)
		{	
		 String[] tokens = line.split("\\.");// 1-Jan-2024  RayBan eye glasses, London, United Kingdom     USD   250,00
		 String dateOfTxn = tokens[0].trim(); // 1-Jan-2024
		 
		 String itemPlaceCountry = tokens[1].trim(); // RayBan eye glasses, London, United Kingdom		 
		 String[] txnDetails = itemPlaceCountry.split(","); //RayBan eye glasses  London  United Kingdom
		 String item = txnDetails[0].trim(); //RayBan eye glasses
		 String placeOfTxn = txnDetails[1].trim(); //  London  
		 String countryOfTxn = txnDetails[2].trim(); //United Kingdom
		 
		 String currencyOfTxn = tokens[2].trim(); //USD
		 String amount = tokens[3].trim(); //250,00
		 amount = amount.replace(",", "."); //250.00
		 
		 // John Mayo recent transaction was for an amount USD 250 in London, United Kingdom at 10 AM CET. Item purchased is Rayban eye glasses.
		 contextBldr.add(customerName + " recent transaction was for an amount "+ currencyOfTxn + " "+ amount + " in "+ city + ", " + country + " on "+ dateOfTxn + ". Item purchased is "+ item + ". ");
		}
		
		return contextBldr;
	}

	/*
	 * expects fieldNamePair to be in the format key=value
	 * llm.response.temperature=0.8
	 */
	
	public String extractFields(String fieldNamePair, String resourcePath) 
	{	
		List<String> lines = readFile(resourcePath);
		
		String keyValuePair = lines.stream()	
									 .filter(line -> !"".equals(line.trim()))
									 .filter(line -> !line.startsWith("#"))
									 .filter(line -> line.contains(fieldNamePair))	//llm.response.temperature=0.8
									 .findFirst()
									 .orElse("");
		
		String[] tokens = keyValuePair.split("=");   // llm.response.temperature     0.8
		return tokens[1]; // 0.8
	}

	public Map<String, String> loadConfig()
	{
		Map<String, String> config = new HashMap<String, String>(); 
		
		String currentDir = System.getProperty("user.dir");
		String resourcePath = currentDir + "\\"+ "\\src\\main\\resources\\application.properties";
		
		String vectorDbName = new FileUtilsService().extractFields("vector.db.name", resourcePath);
		config.put("vectorDbName", vectorDbName);
		
		//-- chroma
		String vectorDbUrlChroma        = new FileUtilsService().extractFields("vector.db.url.chroma", resourcePath);		
		String vectorDbCollectionChroma = new FileUtilsService().extractFields("vector.db.collection.chroma", resourcePath);
		config.put("vectorDbUrlChroma", vectorDbUrlChroma);
		config.put("vectorDbCollectionChroma", vectorDbCollectionChroma);
		
		
		//-- elasticsearch
		String vectorDbUrlElasticSearch = new FileUtilsService().extractFields("vector.db.url.elasticsearch", resourcePath);
		config.put("vectorDbUrlElasticSearch", vectorDbUrlElasticSearch);
		
		//-- qdrant
		String vectorDbHostQdrant       = new FileUtilsService().extractFields("vector.db.host.qdrant", resourcePath);
		String vectorDbPortQdrant       = new FileUtilsService().extractFields("vector.db.port.qdrant", resourcePath);
		String vectorDbCollectionQdrant = new FileUtilsService().extractFields("vector.db.collection.qdrant", resourcePath);
		config.put("vectorDbHostQdrant", vectorDbHostQdrant);
		config.put("vectorDbPortQdrant", vectorDbPortQdrant);
		config.put("vectorDbCollectionQdrant", vectorDbCollectionQdrant);
		
		
		//training data load
		String trainingDataReload = new FileUtilsService().extractFields("training.data.reload", resourcePath);
		String trainingDataCreditcardFraudDetect = new FileUtilsService().extractFields("training.data.creditcard.fraud.detect", resourcePath);
		config.put("trainingDataReload", trainingDataReload);
		config.put("trainingDataCreditcardFraudDetect", trainingDataCreditcardFraudDetect);
		
		return config;
	}
	
}