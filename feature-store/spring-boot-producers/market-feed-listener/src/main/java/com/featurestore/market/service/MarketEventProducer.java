package com.featurestore.market.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.featurestore.market.model.InstrumentEvent;
import com.featurestore.market.model.QuoteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes market events to Kafka topics.
 * - Quotes → market-quotes topic
 * - Instruments → market-instruments topic
 */
@Service
public class MarketEventProducer {

    private static final Logger log = LoggerFactory.getLogger(MarketEventProducer.class);

    @Value("${kafka.topic.quotes:market-quotes}")
    private String quotesTopic;

    @Value("${kafka.topic.instruments:market-instruments}")
    private String instrumentsTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public MarketEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Publish quote event to Kafka.
     * Key: stock symbol (for proper partitioning)
     * Value: JSON serialized QuoteEvent
     */
    public void publishQuote(QuoteEvent quoteEvent) {
        try {
            String key = quoteEvent.getSymbol();
            String value = objectMapper.writeValueAsString(quoteEvent);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(quotesTopic, key, value);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Published quote for {} at price {} to partition {}",
                            quoteEvent.getSymbol(),
                            quoteEvent.getPrice(),
                            result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish quote for {}: {}",
                            quoteEvent.getSymbol(), ex.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing quote event: {}", e.getMessage());
        }
    }

    /**
     * Publish instrument event (ADD/DELETE) to Kafka.
     * Key: stock symbol
     * Value: JSON serialized InstrumentEvent
     */
    public void publishInstrument(InstrumentEvent instrumentEvent) {
        try {
            String key = instrumentEvent.getSymbol();
            String value = objectMapper.writeValueAsString(instrumentEvent);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(instrumentsTopic, key, value);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published instrument {} action {} to partition {}",
                            instrumentEvent.getSymbol(),
                            instrumentEvent.getAction(),
                            result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish instrument {}: {}",
                            instrumentEvent.getSymbol(), ex.getMessage());
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Error serializing instrument event: {}", e.getMessage());
        }
    }
}
