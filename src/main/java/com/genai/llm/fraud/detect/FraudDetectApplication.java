package com.genai.llm.fraud.detect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FraudDetectApplication 
{	
	public static void main(String[] args) 
	{
		System.out.println("\n******************** Please ensure that your Vector DB instance is running and reachable. Load the necessary context data before invoking the LLM ********************");
				
		System.out.println("Chroma launch  		  : docker run -p 8000:8000 chromadb/chroma");
		System.out.println("Elasticsearch launch  : docker run -d -p 9200:9200 -p 9300:9300 -e discovery.type=single-node -e xpack.security.enabled=false docker.elastic.co/elasticsearch/elasticsearch:8.9.0");
		System.out.println("Qdrant launch         : docker run -p 6333:6333 qdrant/qdrant");
		
		System.out.println("******************** Please ensure that your Vector DB instance is running and reachable. Load the necessary context data before invoking the LLM  ********************");
		
		
		SpringApplication.run(FraudDetectApplication.class, args);		
	}
}
