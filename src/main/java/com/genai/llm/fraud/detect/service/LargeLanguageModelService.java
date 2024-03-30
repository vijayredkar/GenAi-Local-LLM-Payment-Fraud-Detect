package com.genai.llm.fraud.detect.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.awaitility.Durations;
import org.testcontainers.shaded.org.bouncycastle.oer.its.ieee1609dot2.basetypes.Duration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

@Service
public class LargeLanguageModelService 
{
	@Autowired
	private FileUtilsService fileUtilsSvc;	
	
	/*
	 * Local LLM server : Ollama operations	
	 */
	public String generate(String text) 
	{
		
		Map<String, String> severConfigMap  = gatherConfig();		
		String modelName       = severConfigMap.get("modelName");		
		Integer llmServerPort  = Integer.parseInt(severConfigMap.get("llmServerPort"));
		Double llmResponseTemp = Double.parseDouble(severConfigMap.get("llmResponseTemp"));
				
	    GenericContainer<?> ollama = startLLMServer(modelName, llmServerPort);
	    
	    ChatLanguageModel model = buildLLMResponseModel(ollama, modelName, llmResponseTemp);
	    String llmResponse = model.generate(text);	    
	    System.out.println("\n---- Got local LLM response : "+ llmResponse);
	    	    
	    stopLLMServer(ollama);
	    return llmResponse;
	 }

	/*
	 * get server config
	 */
	private Map<String, String> gatherConfig() 
	{
		Map<String, String> llmServerConfig = new HashMap<String, String>();
		
		String currentDir = System.getProperty("user.dir");
		String resoucePath = currentDir + "\\"+ "\\src\\main\\resources\\application.properties";
		
		String modelName       = fileUtilsSvc.extractFields("llm.model.name", resoucePath);		
		String llmServerPort   = fileUtilsSvc.extractFields("llm.server.port", resoucePath);
		String llmResponseTemp = fileUtilsSvc.extractFields("llm.response.temperature", resoucePath);		
		
		llmServerConfig.put("modelName", modelName);
		llmServerConfig.put("llmServerPort", llmServerPort);
		llmServerConfig.put("llmResponseTemp", llmResponseTemp);
		
		return llmServerConfig;
	}

	/*
	 * create and start Ollama server on the fly
	 */
	private GenericContainer<?> startLLMServer(String modelName, Integer llmServerPort) 
	{
		System.out.println("\n---- starting LLM server with : "+ "langchain4j/ollama-" + modelName + ":latest");
		
		//-- be patient - this docker pull model will require time to complete on its 1st run
		System.out.println("**********  LLM server launch in progress. This docker pull may take time. Please be patient **********");
	    GenericContainer<?> ollama = new GenericContainer<>("langchain4j/ollama-" + modelName + ":latest") 
	            							.withExposedPorts(llmServerPort);
	    ollama.start();
	    System.out.println("---- started LLM server");
		return ollama;
	}
	
	/*
	 * stop Ollama server
	 */
	private void stopLLMServer(GenericContainer<?> ollama) 
	{
		System.out.println("---- stopping LLM server");
		ollama.stop();
		System.out.println("---- stopped LLM server");
	}

	/*
	 * build Ollama server host URL
	 */
	 private String baseUrl(GenericContainer<?> ollama) 
	 {
	    return String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort());
	 }
	 
	 /*
	  * build LLM response model 
	  */
	 private ChatLanguageModel buildLLMResponseModel(GenericContainer<?> ollama, String modelName, double llmResponseTemp) 
	 { 
			ChatLanguageModel model = OllamaChatModel.builder()
								        			   .baseUrl(baseUrl(ollama))
								        			   .modelName(modelName)
								        			   .temperature(llmResponseTemp)
								        			   //.timeout(Durations.TEN_MINUTES) //best is to NOT change this
								        			   .timeout(java.time.Duration.ofSeconds(1200))
								        			   .build();
			return model;
		}
}