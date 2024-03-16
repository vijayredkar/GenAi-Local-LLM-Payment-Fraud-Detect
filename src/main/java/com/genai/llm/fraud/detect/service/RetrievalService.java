package com.genai.llm.fraud.detect.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class RetrievalService 
{
	@Value("${vector.context.type:fraud}")
	private String contextType;
	
	@Value("${llm.system.message:You are a helpful assistant}")
	private String systemMessage;
	
	
	@Value("${llm.system.response.instruction.1:Test transaction to classify}")
	private String llmResponseInstruction1;
	
	@Value("${llm.system.response.instruction.2:Be concise in your response. Only state if the given test transaction is fraud or not alongwith a brief reason.}")
	private String llmResponseInstruction2;
	
	@Autowired
	private VectorDataStoreService vectorDataSvc;
	
	@Autowired
	private LargeLanguageModelService largeLangModelSvc;
	
	/*
	 * LLM - RAG orchestration operations
	 */
	public String orchestrate(String text, Integer maxResultsToRetrieveDynamic, Double minScoreRelevanceScoreDynamic) 
	{	
		System.out.println("\n---- started LLM - RAG orchestrations");
		String userPrompt = text;
		
		//--step -1  : enhance the user prompt with the context information from the DB 
		String contextFromVectorDb = null;
		if(maxResultsToRetrieveDynamic == -1 && minScoreRelevanceScoreDynamic == -1)
		{	
			//app startup default values	
			contextFromVectorDb = vectorDataSvc.retrieve(contextType);
		}
		else
		{	//user supplied dynamic values
			contextFromVectorDb = vectorDataSvc.retrieveDynamic(contextType, maxResultsToRetrieveDynamic, minScoreRelevanceScoreDynamic);
		}
		
		
		String promptWithFullContext = systemMessage + " " + contextFromVectorDb + "  "+ llmResponseInstruction1  +  " \"" + userPrompt + "\""  + llmResponseInstruction2;
		
		/*
		promptWithFullContext = "You are a helpful assistant.  You will classify the given transaction as potentially fraud or not based on the below patterns "
								+ "Fraud pattern 1 is transaction carried out at a location outside the card owner's country. "
								+ "Fraud pattern 2 is transaction carried out at a time that is way beyond regular business hours. "
								+ "Test transaction to classify is Transaction on Heather Bank's creditcard was made in Tokyo. Heather lives in Monaco. "
								+ "Be concise in your response. Only state if the given test transaction is fraud or not alongwith a brief reason.";
		*/
		
		//-- testing only
		System.out.println("---- constructed RAG promptWithFullContext \n"+promptWithFullContext);
		
		
		//--step -2 : invoke the LLM inferencing engine with the fully constructed prompt
		String response = largeLangModelSvc.generate(promptWithFullContext);
		
		System.out.println("---- completed LLM - RAG orchestrations with response : \n"+ response);
		return response;
	}
	
	
}