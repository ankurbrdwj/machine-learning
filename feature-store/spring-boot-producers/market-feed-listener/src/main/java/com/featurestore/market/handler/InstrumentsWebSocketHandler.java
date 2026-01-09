package com.featurestore.market.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.featurestore.market.model.InstrumentData;
import com.featurestore.market.model.InstrumentEvent;
import com.featurestore.market.model.WebSocketMessage;
import com.featurestore.market.service.MarketEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Handles WebSocket messages from /instruments endpoint.
 * Receives instrument ADD/DELETE events and publishes to Kafka.
 */
@Component
public class InstrumentsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(InstrumentsWebSocketHandler.class);

    private final MarketEventProducer marketEventProducer;
    private final ObjectMapper objectMapper;

    public InstrumentsWebSocketHandler(MarketEventProducer marketEventProducer) {
        this.marketEventProducer = marketEventProducer;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connected to instruments feed: {}", session.getUri());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);

            String messageType = wsMessage.getType();
            if (("ADD".equals(messageType) || "DELETE".equals(messageType)) && wsMessage.getData() != null) {
                // Parse the data as InstrumentData
                InstrumentData instrumentData = objectMapper.convertValue(
                        wsMessage.getData(), InstrumentData.class);

                // Convert to InstrumentEvent and publish to Kafka
                InstrumentEvent instrumentEvent = InstrumentEvent.from(instrumentData, messageType);
                marketEventProducer.publishInstrument(instrumentEvent);

                log.info("Processed instrument {} action: {}", instrumentData.getIsin(), messageType);
            }
        } catch (Exception e) {
            log.error("Error processing instrument message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error on instruments feed: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Disconnected from instruments feed. Status: {}", status);
    }
}