package com.featurestore.market;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Market Feed Listener Application.
 *
 * Connects to stock market simulator WebSocket feeds and publishes events to Kafka:
 * - /quotes → market-quotes topic
 * - /instruments → market-instruments topic
 *
 * These events are consumed by Spark jobs and written to HDFS Parquet lake.
 */
@SpringBootApplication
public class MarketFeedListenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketFeedListenerApplication.class, args);
    }
}
