package com.featurestore.market.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an instrument (stock) received from the WebSocket feed.
 * Example: {"isin": "AAPL", "description": "Apple Inc."}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentData {
    @JsonProperty("isin")
    private String isin;

    @JsonProperty("description")
    private String description;
}
