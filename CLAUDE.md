# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a real-time feature store implementation that ingests events from multiple microservices, processes them through Kafka Streams and Spark, and serves features to ML models.

**Data Flow:**
1. Spring Boot microservices (user-service, product-service, order-service) → Kafka topics
2. Kafka Streams processors aggregate real-time features
3. Spark jobs ingest from Kafka → HDFS and perform batch feature engineering
4. Feature API serves both real-time and batch features to ML models

## Build Commands

### Spring Boot Services

Each service in `spring-boot-producers/` is built independently with Maven:

```bash
# Build a specific service
cd feature-store/spring-boot-producers/user-service
mvn clean package

# Run a service locally
mvn spring-boot:run

# Build all services (from spring-boot-producers/)
for service in user-service product-service order-service; do
  cd $service && mvn clean package && cd ..
done
```

### Kafka Streams Applications

Located in `kafka-streams/`:

```bash
cd feature-store/kafka-streams/user-aggregator
mvn clean package
mvn exec:java -Dexec.mainClass="com.featurestore.streams.UserAggregatorApp"
```

### Spark Jobs

Located in `spark-jobs/`:

```bash
# Submit Kafka ingestion job
spark-submit \
  --master local[*] \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0 \
  feature-store/spark-jobs/kafka_to_hdfs_ingestion.py

# Submit feature engineering job
spark-submit \
  --master local[*] \
  feature-store/spark-jobs/feature_engineering.py

# Submit materialization job
spark-submit \
  --master local[*] \
  feature-store/spark-jobs/materialization.py
```

### Docker Infrastructure

Start Kafka, Zookeeper, and other dependencies:

```bash
cd feature-store
docker-compose up -d

# View logs
docker-compose logs -f kafka

# Stop all services
docker-compose down
```

## Testing

### Unit Tests

```bash
# Spring Boot service tests
cd feature-store/spring-boot-producers/user-service
mvn test

# Run specific test class
mvn test -Dtest=UserEventControllerTest
```

### Integration Tests

```bash
# Run integration tests (requires Docker)
mvn verify -P integration-tests
```

## Development Workflow

### Local Development Setup

1. Start infrastructure: `cd feature-store && docker-compose up -d`
2. Verify Kafka is running: `docker-compose ps`
3. Start services in order:
   - Spring Boot producers (to generate events)
   - Kafka Streams processors (for real-time aggregation)
   - Spark jobs (for batch processing)
   - Feature API (to serve features)

### Event Generator

The `event-generator` service simulates realistic traffic:

```bash
cd feature-store/spring-boot-producers/event-generator
mvn spring-boot:run
```

This generates user interactions, product views, and orders to test the pipeline end-to-end.

### Monitoring Kafka Topics

```bash
# List all topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Consume from a topic
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning
```

## Architecture Notes

### Shared Models

The `shared/common-models` module contains POJOs used across services. When modifying shared models:

1. Update the model in `shared/common-models/src/main/java/com/featurestore/common/`
2. Build: `cd shared/common-models && mvn clean install`
3. Update version in dependent service POMs if needed
4. Rebuild affected services

### Kafka Configuration

- **Bootstrap servers**: Configured in each service's `application.yml`
- **Topic naming**: Convention is `{domain}-events` (e.g., `user-events`, `product-events`)
- **Partitioning**: User events partitioned by `userId`, product events by `productId`

### Feature Store Architecture

**Real-time features** (Kafka Streams):
- User: session duration, click rate, recent interactions
- Product: view count, conversion rate
- Served with <10ms latency

**Batch features** (Spark):
- User: 30-day purchase history, category preferences
- Product: popularity scores, co-purchase patterns
- Updated hourly/daily depending on the job

### HDFS Integration

Spark jobs read from Kafka and write to HDFS in Parquet format:
- Raw events: `/feature-store/raw/{domain}/{date}/`
- Engineered features: `/feature-store/features/{feature-group}/{date}/`

## Common Issues

### Kafka Connection Failures

If Spring Boot services can't connect to Kafka:
1. Check `docker-compose ps` - ensure Kafka is healthy
2. Verify `KAFKA_ADVERTISED_LISTENERS` in docker-compose.yml matches your setup
3. Check application.yml bootstrap-servers configuration

### Spark Job Failures

If Spark jobs fail with "NoClassDefFoundError":
- Ensure `--packages` flag includes all required dependencies
- For Kafka integration: `org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0`

### Maven Dependency Resolution

If shared models aren't found:
```bash
cd feature-store/spring-boot-producers/shared/common-models
mvn clean install
```

This installs the shared JAR to your local Maven repository (~/.m2).