package com.genai.llm.fraud.detect.service;

import java.util.Arrays;
import java.util.List;

import javax.swing.filechooser.FileNameExtensionFilter;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;

@Service
public class VectorDataStoreService 
{			
	@Value("${retrieval.max.limit:1}")
	int maxResultsToRetrieve; 
	
	@Value("${embeddings.min.score:0.5}")
	double minScoreRelevanceScore;	 
	
	@Autowired
	private ModelService modelSvc;
	
	@Autowired
	private ContextLoadService contextLoadSvc;
	
	@Autowired
	private FileUtilsService fileUtilsSvc;
	
	StringBuilder contentBldr = new StringBuilder();
	String content = "";
	
	int hdrIdx = 0;
	
	//String trainingDataHeaders = "transaction_timestamp,creditcard_number,merchant,category,amount,first_name,last_name,gender,street,city,state,zip,latitude,longitude,point_of_presence,job,date_of_birth,transaction_number,unix_time,merchant_latitude,merchant_longitude,is_fraud";
	//String trainingDataHeaders = "This creditcard transaction occurred on timestamp,with the creditcard number,at the merchant,under the category,for an amount,. The customer's first name is, and last name is, and the customer's gender is, The customer's address is street,city,state,zip,latitude,longitude,point of presence,.The job performed is,. This merchant with date of birth,created this transaction number,at unix time,. This merchant's latitude is, . This merchant's longitude is,. This transaction is genuine.";
	//String trainingDataHeaders = "This creditcard transaction occurred on timestamp,with the creditcard number,at the merchant,under the category,for an amount,. The customer's first name is, and last name is, and the customer's gender is, The customer's address is street,city,state,zip,latitude,longitude,point of presence,.The job performed is,. This merchant with date of birth,created this transaction number,at unix time,. This merchant's latitude is, . This merchant's longitude is,0";
	String trainingDataHeaders = "This transaction is,This creditcard transaction occurred on timestamp,with the creditcard number,at the merchant,under the category,for an amount,. The customer's first name is, and last name is, and the customer's gender is, The customer's address is street,city,state,zip,latitude,longitude,point of presence,.The job performed is,. This merchant with date of birth,created this transaction number,at unix time,. This merchant's latitude is, . This merchant's longitude is";
	String txnStatusGenuine = "genuine.";
	String txnStatusFraud = "fraud.";	
	List<String>trainingDataHeadersList = null;
	List<String> trainingDataList = null;
	
	
	/*
	 * vectorDB operations to fetch records
	 */
	public String retrieve(String text) 
	{	
		System.out.println("\n--- started vectorDB operations");
		System.out.println("---- embeddingModel : "+ modelSvc.getEmbeddingModel() +" \n "+"text : "+ text +" \n "+"minScore : "+ minScoreRelevanceScore +" \n "+"maxResults : "+ maxResultsToRetrieve);
		
		List<EmbeddingMatch<TextSegment>> result = fetchRecords(text);	
		
		StringBuilder responseBldr = new StringBuilder();
		StringBuilder tempResponseBldr = new StringBuilder();
		
		for(EmbeddingMatch<TextSegment> segment : result)
		{
			responseBldr.append(segment.embedded().text());
			
			tempResponseBldr.append(segment.embedded().text());
			tempResponseBldr.append("- with embedding score : ");
			tempResponseBldr.append(segment.score());
			tempResponseBldr.append("\n");
		}
		
		System.out.println("--- Got most relevant record from vectorDB : \n"+tempResponseBldr.toString());
		System.out.println("--- completed vectorDB operations");
		return responseBldr.toString();
	}
	
	/*
	 * vectorDB operations to fetch records
	 */
	public String retrieveDynamic(String text, int maxResultsToRetrieve, double minScoreRelevanceScore) 
	{	
		System.out.println("\n--- started vectorDB operations");
		System.out.println("---- embeddingModel : "+ modelSvc.getEmbeddingModel() +" \n "+"text : "+ text +" \n "+"minScore : "+ minScoreRelevanceScore +" \n "+"maxResults : "+ maxResultsToRetrieve);
		
		List<EmbeddingMatch<TextSegment>> result = fetchRecordsDynamic(text, maxResultsToRetrieve, minScoreRelevanceScore);	
		
		StringBuilder responseBldr = new StringBuilder();
		StringBuilder tempResponseBldr = new StringBuilder();
		
		for(EmbeddingMatch<TextSegment> segment : result)
		{
			responseBldr.append(segment.embedded().text());
			
			tempResponseBldr.append(segment.embedded().text());
			tempResponseBldr.append("- with embedding score : ");
			tempResponseBldr.append(segment.score());
			tempResponseBldr.append("\n");
		}
		
		System.out.println("--- Got most relevant record from vectorDB : \n"+tempResponseBldr.toString());
		System.out.println("--- completed vectorDB operations");
		return responseBldr.toString();
	}
	
	/*
	 * fetches records from vectorDB based on semantic similarities
	 */
	public List<EmbeddingMatch<TextSegment>> fetchRecords(String query) 
	{
		EmbeddingModel embdgModel= modelSvc.getEmbeddingModel();		
		EmbeddingStore<TextSegment> embdgStore = contextLoadSvc.getEmbeddingStore();  
		
        Embedding queryEmbedding = embdgModel.embed(query).content();
        return  embdgStore.findRelevant(queryEmbedding, maxResultsToRetrieve, minScoreRelevanceScore);
    }
	
	/*
	 * fetches records from vectorDB based on semantic similarities
	 */
	public List<EmbeddingMatch<TextSegment>> fetchRecordsDynamic(String query, int maxResultsToRetrieve, double minScoreRelevanceScore) 
	{
		EmbeddingModel embdgModel= modelSvc.getEmbeddingModel();		
		EmbeddingStore<TextSegment> embdgStore = contextLoadSvc.getEmbeddingStore();  
		
        Embedding queryEmbedding = embdgModel.embed(query).content();
        return  embdgStore.findRelevant(queryEmbedding, maxResultsToRetrieve, minScoreRelevanceScore);
    }
	
	/*
	 * loads context to vectorDB
	 */
	public void load(String fileNameWithFullPath, String vectorDbName) 
	{
		System.out.println("\n---- started loading context to vectorDB "+ vectorDbName);
		trainingDataList =  new ArrayList<String>();
		prepareSimpleTrainingData(fileNameWithFullPath);
		//prepareTrainingData(fileNameWithFullPath);   //populate trainingDataList
		
		insertVectorData(modelSvc.getEmbeddingModel(), trainingDataList, vectorDbName);
		System.out.println("---- completed loading context to vectorDB " + vectorDbName);
    }
	
	
	/*
	 * loads context with genuine txn patterns to vectorDB
	 */
	public void loadWithGenuineTxns(String fileNameWithFullPath, String vectorDbName) 
	{
		System.out.println("\n---- started loading context to vectorDB "+ vectorDbName);
		System.out.println("---- source of context : "+ fileNameWithFullPath);
		trainingDataList =  new ArrayList<String>();
		 
		prepareGenuineTxnsTrainingData(fileNameWithFullPath);
		
		insertVectorData(modelSvc.getEmbeddingModel(), trainingDataList, vectorDbName);
		System.out.println("---- completed loading context to vectorDB " + vectorDbName);
    }
	
	private void prepareSimpleTrainingData(String fileNameWithFullPath) 
	{
		List<String> lines = fileUtilsSvc.readFile(fileNameWithFullPath);
				
		lines.stream()
			.map(line -> trainingDataList.add(line))
			//.count()
			.toList();
	}
		
	private void prepareGenuineTxnsTrainingData(String fileNameWithFullPath) 
	{
		List<String> lines = fileUtilsSvc.readPdf(fileNameWithFullPath);
				
		lines.stream()
			.map(line -> trainingDataList.add(line))
			//.count()
			.toList();
	}
	
	private List<String> prepareTrainingData(String fileNameWithFullPath) 
	{
		List<String> lines = fileUtilsSvc.readFile(fileNameWithFullPath);
				
		lines.stream()
										 //.forEach(line -> line.split(",")) // 1/1/2019 0:00,2703190000000000,"fraud_Rippin, Kub and Mann",misc_net,4.97,Jennifer,Banks,F,561 Perry Cove,Moravian Falls,NC,28654,36.0788,-81.1781,3495,"Psychologist, counselling",3/9/1988,0b242abb623afc578575680df30655b9,1325376018,36.011293,-82.048315,0
										 //.flatMap(line -> line.split(","))
											.map(line -> Arrays.asList(line.split(",")))
											//.collect(Collectors.toList())
											.forEach(tokens -> collectTokens(tokens))
											;
		
		//content = contentBldr.toString();
		//System.out.println("---- content \n "+content);
		
		return trainingDataList;
	}
	
	private void  collectTokens(List<String> tokens) 
	{
	 
	 if(trainingDataHeadersList == null)
	 {
		 trainingDataHeadersList =   Arrays.asList(trainingDataHeaders.split(","));		 
	 }
	 
	  tokens.stream()
	  	    .forEach(token -> {	  	    					
	  	    					if(hdrIdx == 0)
	  	    					{
	  	    						String txnStatus = "0".equals(token) ? txnStatusGenuine : txnStatusFraud;       //decipher genuine/fraud txn
	  	    						contentBldr.append(trainingDataHeadersList.get(hdrIdx) + " "  + txnStatus + " ");	    						
	  	    					}
	  	    					else
	  	    					{
	  	    						contentBldr.append(trainingDataHeadersList.get(hdrIdx) + " "  +token + " ");
	  	    					}
	  	    					hdrIdx = hdrIdx + 1;
	  	    				  }
	  	            );
	  
	  contentBldr.append("\n");
	  trainingDataList.add(contentBldr.toString());
	  hdrIdx = 0;
	  contentBldr = new StringBuilder();
	}	
	
	/*
	 * inserts to vectorDB
	 */
	private void insertVectorData(EmbeddingModel embeddingModel, List<String> lines, String vectorDbName) 
	{
		for(String text : lines)
		{
			//System.out.println("---- line : \n" +text);
			
			TextSegment segment1 = TextSegment.from(text, new Metadata());
	        Embedding embedding1 = embeddingModel.embed(segment1).content();
	        
	        contextLoadSvc.getEmbeddingStore().add(embedding1, segment1);
	        System.out.println("---- loading to vectorDB : " +text);
		}
	}
}