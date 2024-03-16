package com.genai.llm.fraud.detect.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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