package com.featurestore.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka event for instrument lifecycle (ADD/DELETE).
 * Published to 'market-instruments' topic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentEvent {
    private String symbol;          // Stock symbol
    private String description;     // Full company name
    private String action;          // ADD or DELETE
    private long timestamp;         // Event timestamp (epoch millis)
    private String source;          // market-feed-listener

    public static InstrumentEvent from(InstrumentData instrumentData, String action) {
        return InstrumentEvent.builder()
                .symbol(instrumentData.getIsin())
                .description(instrumentData.getDescription())
                .action(action)
                .timestamp(Instant.now().toEpochMilli())
                .source("market-feed-listener")
                .build();
    }
}
