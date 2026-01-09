package com.featurestore.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka event for stock quotes.
 * Published to 'market-quotes' topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuoteEvent {
    private String symbol;          // Stock symbol (AAPL, GOOGL, TCS, etc.)
    private double price;           // Current price
    private long timestamp;         // Event timestamp (epoch millis)
    private String eventType;       // QUOTE
    private String source;          // market-feed-listener

    public static QuoteEvent from(QuoteData quoteData) {
        return QuoteEvent.builder()
                .symbol(quoteData.getIsin())
                .price(quoteData.getPrice())
                .timestamp(Instant.now().toEpochMilli())
                .eventType("QUOTE")
                .source("market-feed-listener")
                .build();
    }
}
