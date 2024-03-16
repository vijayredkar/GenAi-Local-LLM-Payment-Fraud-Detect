package com.genai.llm.fraud.detect.service;

import org.springframework.stereotype.Service;

import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

@Service
public class ModelService 
{
	private AllMiniLmL6V2EmbeddingModel embeddingModel;
	
	public EmbeddingModel getEmbeddingModel() 
	{
		if (embeddingModel == null) {
			embeddingModel = new AllMiniLmL6V2EmbeddingModel();
		}
		return embeddingModel;
	}
}