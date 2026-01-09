# Feature Store - ML Infrastructure Platform

Production-style feature store demonstrating lambda architecture for ML applications with real-time and batch processing.

## 🏗️ Architecture

```
Spring Boot Services → Kafka → Spark → HDFS/Redis → Feature API → ML Models
```

### Components

- **Data Producers**: Spring Boot microservices generating events
- **Message Bus**: Apache Kafka for event streaming
- **Batch Processing**: Apache Spark for feature engineering
- **Storage**:
  - Offline: MinIO (S3-compatible) / HDFS
  - Online: Redis
- **Serving**: FastAPI for low-latency feature retrieval
- **Orchestration**: Apache Airflow
- **Monitoring**: Prometheus + Grafana

## 🚀 Quick Start

```bash
# 1. Setup (first time only)
make setup

# 2. Start all services
make start

# 3. Verify services are running
make health

# 4. Generate sample data
make generate-data

# 5. Run feature pipeline
make run-pipeline

# 6. Test the API
curl http://localhost:8000/api/v1/features/user/user_123
```

## 📊 Performance

- **Latency**: <5ms p99 for feature retrieval
- **Throughput**: 2,000+ req/sec (single instance)
- **Scale**: Designed for 100k+ req/sec with clustering

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Data Producer | Spring Boot 3.2 + Kafka |
| Message Bus | Apache Kafka |
| Batch Processing | Apache Spark 3.5 |
| Offline Storage | MinIO (S3-compatible) |
| Online Storage | Redis |
| API | FastAPI (Python) |
| Orchestration | Apache Airflow |
| Monitoring | Prometheus + Grafana |
| Containerization | Docker Compose |

## 📁 Project Structure

```
feature-store/
├── spring-boot-producers/    # Java microservices
│   ├── user-service/         # User event producer
│   └── shared/               # Common models
├── spark-jobs/               # Batch processing
├── feature-api/              # Online serving
├── ml-models/                # ML integration
├── airflow/                  # Orchestration
├── monitoring/               # Observability
└── docker/                   # Container configs
```

## 🌐 Access Points

| Service | URL | Purpose |
|---------|-----|---------|
| User Service | http://localhost:8081 | Event ingestion |
| Feature API | http://localhost:8000/docs | Feature retrieval |
| Kafka UI | http://localhost:8082 | Kafka monitoring |
| Spark UI | http://localhost:8080 | Spark jobs |
| MinIO Console | http://localhost:9001 | Object storage |
| Grafana | http://localhost:3000 | Dashboards |
| Prometheus | http://localhost:9090 | Metrics |

## 📚 Documentation

- [Architecture Design](docs/architecture.md)
- [API Specification](docs/api-spec.md)
- [Setup Guide](docs/setup-guide.md)
- [Design Decisions](docs/design-decisions.md)

## 🧪 Testing

```bash
# Run all tests
make test

# Test API endpoints
make test-api

# Performance benchmarks
make benchmark

# Integration tests
make test-integration
```

## 🔄 Data Flow

1. **Event Production**: Spring Boot services produce events to Kafka
2. **Stream Ingestion**: Spark Streaming reads from Kafka → writes to HDFS
3. **Batch Processing**: Spark jobs engineer features from raw events
4. **Materialization**: Features copied from HDFS to Redis
5. **Online Serving**: FastAPI serves features from Redis (<5ms)
6. **ML Consumption**: ML models/LLMs retrieve features via API

## 🎯 Use Cases

- **LLM Context Enrichment**: Real-time user/product features for prompts
- **Real-time Recommendations**: Low-latency feature access
- **A/B Testing**: Consistent features across experiments
- **Fraud Detection**: Real-time user behavior patterns

## 🔧 Development

```bash
# Start development environment
make dev

# View logs
make logs-api
make logs-spark
make logs-kafka

# Shell into containers
make shell-api
make shell-spark
```

## 📈 Monitoring

Access Grafana dashboards:
- Feature Store Performance
- Kafka Metrics
- Spark Job Metrics
- API Latency & Throughput

## 🚦 Status

- [x] Core infrastructure setup
- [x] Spring Boot event producers
- [x] Kafka event streaming
- [x] Spark batch processing
- [x] Redis online serving
- [x] FastAPI feature API
- [x] Monitoring stack
- [ ] ML model integration
- [ ] Advanced features (streaming aggregations)

## 📝 License

MIT

## 👤 Author

Ankur Bardwaj