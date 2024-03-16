package com.genai.llm.fraud.detect.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;

@Service
public class ContextLoadService 
{		
	private String vectorDbName = null;
	private Map<String, String> configMap = null;
	
	private EmbeddingStore<TextSegment> embeddingStoreChroma = null;
	private EmbeddingStore<TextSegment> embeddingStoreElasticSearch = null;
	private EmbeddingStore<TextSegment> embeddingStoreQdrant = null;
	private EmbeddingStore<TextSegment> embeddingStore = getEmbeddingStore();
	
	public EmbeddingStore<TextSegment> getEmbeddingStore()
	{	
		configMap = new FileUtilsService().loadConfig();
		vectorDbName = configMap.get("vectorDbName");
				
		if("chroma".equals(configMap.get(vectorDbName)))
		{
			embeddingStore = getEmbeddingStoreChroma();
		}
		else if("elasticsearch".equals(vectorDbName))
		{
			embeddingStore = getEmbeddingStoreElasticSearch();
		}
		else if("qdrant".equals(vectorDbName))
		{
			embeddingStore = getEmbeddingStoreQdrant();
		}
		else //default
		{
			vectorDbName = "chroma";
			System.out.println("---- vectorDb not pecified: defaulting to : "+ vectorDbName);
			embeddingStore = getEmbeddingStoreChroma();
		}
		
		System.out.println("---- completed connect to "+ vectorDbName +". Got embeddingStore "+embeddingStore);		
		return embeddingStore;
	}
	
	public EmbeddingStore<TextSegment> getEmbeddingStoreChroma() 
	{
		System.out.println("---- started connect to "+vectorDbName);
	
		if(embeddingStoreChroma == null)
		{
			embeddingStore = ChromaEmbeddingStore.builder()
											.baseUrl(configMap.get("vectorDbUrlChroma"))
											.collectionName(configMap.get("vectorDbCollectionChroma"))
											.build();		
		}
		return embeddingStore;
	}

	public EmbeddingStore<TextSegment> getEmbeddingStoreElasticSearch() 
	{
		System.out.println("---- started connect to "+vectorDbName);
		
		if(embeddingStoreElasticSearch == null)
		{
		embeddingStore = ElasticsearchEmbeddingStore.builder()
													.serverUrl(configMap.get("vectorDbUrlElasticSearch"))
									                //.dimension(1)  //v0.27.1
									                //.indexName("collection-fraud-detect-1") //v0.27.1
									                .build();			
		}
		return embeddingStore;
	}
	
	
	public EmbeddingStore<TextSegment> getEmbeddingStoreQdrant() 
	{
		System.out.println("---- started connect to "+vectorDbName);
		
		if(embeddingStoreQdrant == null)
		{
			embeddingStoreQdrant = QdrantEmbeddingStore.builder()
											.collectionName(configMap.get("vectorDbCollectionQdrant"))
								            .host(configMap.get("vectorDbHostQdrant"))
								            .port(Integer.parseInt(configMap.get("vectorDbPortQdrant")))
									        .build();
		}
		return embeddingStoreQdrant;
	}

	public Map<String, String> getConfigMap() 
	{
		return configMap;
	}

	public void setConfigMap(Map<String, String> configMap) 
	{
		this.configMap = configMap;
	}
}