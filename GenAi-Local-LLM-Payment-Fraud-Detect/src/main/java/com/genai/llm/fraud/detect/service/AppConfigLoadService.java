package com.genai.llm.fraud.detect.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class AppConfigLoadService 
{	
	@Autowired
	private VectorDataStoreService vectorDataSvc;
	
	/*
	 * On application startup - load default context to vector DB
	 */
	@PostConstruct
	public void loadOnStartup() 
	{	
		System.out.println("\n---- started initial context load on startup");
		
		//load app config data
		String currentDir = System.getProperty("user.dir");
		String appPropertiesPath = currentDir + "\\"+ "\\src\\main\\resources\\application.properties";		
		String vectorDbName = new FileUtilsService().extractFields("vector.db.name", appPropertiesPath);
		
		//load training/context data
		String trainingDataReload = new FileUtilsService().extractFields("training.data.reload", appPropertiesPath);
		String trainingDataCreditcardFraudDetect = new FileUtilsService().extractFields("training.data.creditcard.fraud.detect", appPropertiesPath);		
		String resoucePath = currentDir + "\\"+ trainingDataCreditcardFraudDetect;
		
		if("Y".equals(trainingDataReload))
		{
			vectorDataSvc.load(resoucePath, vectorDbName);
		}
		
		System.out.println("---- completed initial context load on startup");
	}		
}