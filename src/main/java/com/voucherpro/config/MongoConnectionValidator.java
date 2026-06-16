package com.voucherpro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoConnectionValidator {

    private static final Logger log = LoggerFactory.getLogger(MongoConnectionValidator.class);

    private final MongoTemplate mongoTemplate;

    public MongoConnectionValidator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateConnection() {
        try {
            mongoTemplate.getDb().getName();
            log.info("MongoDB connected successfully. Database: {}", mongoTemplate.getDb().getName());
        } catch (Exception ex) {
            log.error("MongoDB connection failed: {}", ex.getMessage());
            log.error("Update backend/.env with the correct Atlas password from Database Access in MongoDB Atlas.");
        }
    }
}
