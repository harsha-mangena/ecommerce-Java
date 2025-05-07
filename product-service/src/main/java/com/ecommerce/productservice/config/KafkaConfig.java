package com.ecommerce.productservice.config;

import com.ecommerce.productservice.event.InventoryEvent;
import com.ecommerce.productservice.event.ProductEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {
    
    @Value("${kafka.topics.product-created}")
    private String productCreatedTopic;
    
    @Value("${kafka.topics.product-updated}")
    private String productUpdatedTopic;
    
    @Value("${kafka.topics.product-deleted}")
    private String productDeletedTopic;
    
    @Value("${kafka.topics.inventory-update}")
    private String inventoryUpdateTopic;
    
    // Create topics
    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name(productCreatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic productUpdatedTopic() {
        return TopicBuilder.name(productUpdatedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name(productDeletedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic inventoryUpdateTopic() {
        return TopicBuilder.name(inventoryUpdateTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
    
    // Create Kafka templates
    @Bean
    public KafkaTemplate<String, ProductEvent> productKafkaTemplate(
            ProducerFactory<String, ProductEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
    
    @Bean
    public KafkaTemplate<String, InventoryEvent> inventoryKafkaTemplate(
            ProducerFactory<String, InventoryEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
