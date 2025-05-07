package com.example.ordermicroservice.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories(basePackages = ["com.example.ordermicroservice.es.repo"])
class ElasticSearchConfig: ElasticsearchConfiguration() {
    override fun clientConfiguration(): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo("http://elasticsearch-0.elasticsearch.default.svc.cluster.local:9200")
            .withBasicAuth("elastic", "my-elastic")
            .build()
    }
}