package com.featurestore.market.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.featurestore.market.model.QuoteData;
import com.featurestore.market.model.QuoteEvent;
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
 * Handles WebSocket messages from /quotes endpoint.
 * Receives real-time stock price updates and publishes to Kafka.
 */
@Component
public class QuotesWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(QuotesWebSocketHandler.class);

    private final MarketEventProducer marketEventProducer;
    private final ObjectMapper objectMapper;

    public QuotesWebSocketHandler(MarketEventProducer marketEventProducer) {
        this.marketEventProducer = marketEventProducer;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connected to quotes feed: {}", session.getUri());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);

            if ("QUOTE".equals(wsMessage.getType()) && wsMessage.getData() != null) {
                // Parse the data as QuoteData
                QuoteData quoteData = objectMapper.convertValue(wsMessage.getData(), QuoteData.class);

                // Convert to QuoteEvent and publish to Kafka
                QuoteEvent quoteEvent = QuoteEvent.from(quoteData);
                marketEventProducer.publishQuote(quoteEvent);

                log.debug("Processed quote: {} @ {}", quoteData.getIsin(), quoteData.getPrice());
            }
        } catch (Exception e) {
            log.error("Error processing quote message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error on quotes feed: {}", exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Disconnected from quotes feed. Status: {}", status);
    }
}
