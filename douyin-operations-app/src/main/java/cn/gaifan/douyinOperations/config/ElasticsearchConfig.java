package cn.gaifan.douyinOperations.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${app.elasticsearch.uris}")
    private String uris;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        String[] uriArray = uris.split(",");
        HttpHost[] hosts = new HttpHost[uriArray.length];

        for (int i = 0; i < uriArray.length; i++) {
            String uri = uriArray[i].trim();
            hosts[i] = HttpHost.create(uri);
        }

        RestClient restClient = RestClient.builder(hosts).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        return new ElasticsearchClient(transport);
    }
}
