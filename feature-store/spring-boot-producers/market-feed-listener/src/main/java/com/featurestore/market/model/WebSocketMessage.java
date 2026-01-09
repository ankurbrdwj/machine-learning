package com.featurestore.market.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic WebSocket message wrapper from the stock market feed.
 * Can contain QUOTE, ADD, or DELETE message types.
 * Example: {"type": "QUOTE", "data": {"isin": "AAPL", "price": 178.45}}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    @JsonProperty("type")
    private String type;  // QUOTE, ADD, DELETE

    @JsonProperty("data")
    private Object data;  // Can be QuoteData or InstrumentData
}
