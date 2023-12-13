package com.pluralsight.reviewservice;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class SpringMongoConfig {
    private MongoProperties mongoProperties;
    private MongoMappingContext mongoMappingContext;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    public SpringMongoConfig(MongoMappingContext mongoMappingContext, MongoProperties mongoProperties) {
        this.mongoMappingContext = mongoMappingContext;
        this.mongoProperties = mongoProperties;
    }

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(this.mongoUri.replace("27027",
                this.mongoProperties.getPort() == null ? "27027" : this.mongoProperties.getPort().toString()));
    }
}
