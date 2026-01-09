# Spark Jobs for Market Data Ingestion

This directory contains Spark Structured Streaming jobs that ingest market data from Kafka and write to HDFS as Parquet files.

## Jobs

### 1. kafka_to_hdfs_quotes_ingestion.py
Ingests real-time stock quotes from Kafka to HDFS.

- **Source**: `market-quotes` Kafka topic
- **Destination**: `hdfs://namenode:9000/feature-store/raw/quotes/{date}/`
- **Format**: Parquet, partitioned by date
- **Trigger**: 10-second micro-batches

### 2. kafka_to_hdfs_instruments_ingestion.py
Ingests instrument lifecycle events (ADD/DELETE) from Kafka to HDFS.

- **Source**: `market-instruments` Kafka topic
- **Destination**: `hdfs://namenode:9000/feature-store/raw/instruments/{date}/`
- **Format**: Parquet, partitioned by date
- **Trigger**: 10-second micro-batches

## Prerequisites

1. Docker and Docker Compose installed
2. Kafka and HDFS running (via docker-compose)
3. PySpark installed with Kafka support

## Running the Jobs

### Submit to local Spark

```bash
# Submit quotes ingestion job
spark-submit \
  --master local[*] \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0 \
  kafka_to_hdfs_quotes_ingestion.py

# Submit instruments ingestion job
spark-submit \
  --master local[*] \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0 \
  kafka_to_hdfs_instruments_ingestion.py
```

### Submit to Spark cluster (Docker)

```bash
# Copy job to Spark master container
docker cp kafka_to_hdfs_quotes_ingestion.py spark-master:/opt/bitnami/spark/

# Submit to cluster
docker exec spark-master spark-submit \
  --master spark://spark-master:7077 \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0 \
  /opt/bitnami/spark/kafka_to_hdfs_quotes_ingestion.py
```

## Verifying Output

### Check HDFS contents

```bash
# List files in HDFS
docker exec namenode hdfs dfs -ls /feature-store/raw/quotes/

# Read Parquet files (example)
docker exec namenode hdfs dfs -cat /feature-store/raw/quotes/date=2025-01-09/*.parquet
```

### Check Kafka topics

```bash
# List topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Consume from topic
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic market-quotes \
  --from-beginning \
  --max-messages 10
```

## Monitoring

- **Kafka UI**: http://localhost:8080
- **HDFS NameNode**: http://localhost:9870
- **Spark Master**: http://localhost:8090

## Data Schema

### Quote Event (market-quotes)
```json
{
  "symbol": "AAPL",
  "price": 178.45,
  "timestamp": 1704828000000,
  "eventType": "QUOTE",
  "source": "market-feed-listener"
}
```

### Instrument Event (market-instruments)
```json
{
  "symbol": "AAPL",
  "description": "Apple Inc.",
  "action": "ADD",
  "timestamp": 1704828000000,
  "source": "market-feed-listener"
}
```

## Troubleshooting

### Job fails with "NoClassDefFoundError"
Ensure the `--packages` flag includes the Kafka connector:
```bash
--packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0
```

### Cannot connect to Kafka
- Verify Kafka is running: `docker ps | grep kafka`
- Check Kafka health: `docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092`

### Cannot write to HDFS
- Verify HDFS is running: `docker ps | grep namenode`
- Check HDFS health: `curl http://localhost:9870`
- Ensure HDFS directory exists:
  ```bash
  docker exec namenode hdfs dfs -mkdir -p /feature-store/raw/quotes
  ```