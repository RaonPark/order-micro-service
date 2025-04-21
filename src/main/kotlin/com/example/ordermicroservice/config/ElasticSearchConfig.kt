package com.example.ordermicroservice.config

import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@EnableElasticsearchRepositories(basePackages = ["com.example.ordermicroservice.es.repo"])
class ElasticSearchConfig: ElasticsearchConfiguration() {
    override fun clientConfiguration(): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo("elasticsearch:9200")
            .build()
    }
}