package com.featurestore.market.service;

import com.featurestore.market.handler.InstrumentsWebSocketHandler;
import com.featurestore.market.handler.QuotesWebSocketHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages WebSocket connections to the stock market feed.
 * Connects to both /quotes and /instruments endpoints with auto-reconnect.
 */
@Service
public class MarketFeedService {

    private static final Logger log = LoggerFactory.getLogger(MarketFeedService.class);

    @Value("${market.feed.websocket.url:ws://localhost:9090}")
    private String feedBaseUrl;

    @Value("${market.feed.enabled:true}")
    private boolean feedEnabled;

    @Value("${market.feed.reconnect.delay:5000}")
    private long reconnectDelayMs;

    private final QuotesWebSocketHandler quotesHandler;
    private final InstrumentsWebSocketHandler instrumentsHandler;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private WebSocketSession quotesSession;
    private WebSocketSession instrumentsSession;
    private volatile boolean running = false;

    public MarketFeedService(QuotesWebSocketHandler quotesHandler,
                             InstrumentsWebSocketHandler instrumentsHandler) {
        this.quotesHandler = quotesHandler;
        this.instrumentsHandler = instrumentsHandler;
    }

    @PostConstruct
    public void init() {
        if (feedEnabled) {
            log.info("Market feed is enabled. Connecting to {}", feedBaseUrl);
            connectToQuotesFeed();
            connectToInstrumentsFeed();
        } else {
            log.info("Market feed is disabled. Set market.feed.enabled=true to enable.");
        }
    }

    /**
     * Connect to the /quotes WebSocket endpoint
     */
    private void connectToQuotesFeed() {
        if (quotesSession != null && quotesSession.isOpen()) {
            log.debug("Already connected to quotes feed");
            return;
        }

        try {
            String url = feedBaseUrl + "/quotes";
            log.info("Connecting to quotes feed: {}", url);

            StandardWebSocketClient client = new StandardWebSocketClient();
            quotesSession = client.execute(quotesHandler, new WebSocketHttpHeaders(), URI.create(url)).get();
            running = true;

            log.info("Successfully connected to quotes feed");
        } catch (Exception e) {
            log.error("Failed to connect to quotes feed: {}", e.getMessage());
            scheduleReconnect(this::connectToQuotesFeed);
        }
    }

    /**
     * Connect to the /instruments WebSocket endpoint
     */
    private void connectToInstrumentsFeed() {
        if (instrumentsSession != null && instrumentsSession.isOpen()) {
            log.debug("Already connected to instruments feed");
            return;
        }

        try {
            String url = feedBaseUrl + "/instruments";
            log.info("Connecting to instruments feed: {}", url);

            StandardWebSocketClient client = new StandardWebSocketClient();
            instrumentsSession = client.execute(instrumentsHandler, new WebSocketHttpHeaders(), URI.create(url)).get();
            running = true;

            log.info("Successfully connected to instruments feed");
        } catch (Exception e) {
            log.error("Failed to connect to instruments feed: {}", e.getMessage());
            scheduleReconnect(this::connectToInstrumentsFeed);
        }
    }

    /**
     * Schedule reconnection after delay
     */
    private void scheduleReconnect(Runnable reconnectTask) {
        if (feedEnabled) {
            log.info("Scheduling reconnect in {}ms...", reconnectDelayMs);
            scheduler.schedule(reconnectTask, reconnectDelayMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Disconnect from both feeds
     */
    public void disconnect() {
        running = false;

        if (quotesSession != null && quotesSession.isOpen()) {
            try {
                quotesSession.close();
                log.info("Disconnected from quotes feed");
            } catch (Exception e) {
                log.error("Error closing quotes WebSocket session: {}", e.getMessage());
            }
        }

        if (instrumentsSession != null && instrumentsSession.isOpen()) {
            try {
                instrumentsSession.close();
                log.info("Disconnected from instruments feed");
            } catch (Exception e) {
                log.error("Error closing instruments WebSocket session: {}", e.getMessage());
            }
        }
    }

    /**
     * Check connection status
     */
    public boolean isConnected() {
        boolean quotesConnected = quotesSession != null && quotesSession.isOpen();
        boolean instrumentsConnected = instrumentsSession != null && instrumentsSession.isOpen();
        return quotesConnected && instrumentsConnected;
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down market feed service...");
        disconnect();
        scheduler.shutdown();
    }
}