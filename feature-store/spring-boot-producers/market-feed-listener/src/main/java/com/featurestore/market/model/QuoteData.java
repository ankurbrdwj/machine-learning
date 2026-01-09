package com.featurestore.market.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a quote data received from the WebSocket feed.
 * Example: {"isin": "AAPL", "price": 178.45}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteData {
    @JsonProperty("isin")
    private String isin;

    @JsonProperty("price")
    private double price;
}
