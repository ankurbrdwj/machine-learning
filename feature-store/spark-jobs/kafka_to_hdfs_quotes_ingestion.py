"""
Spark Structured Streaming job to ingest market quotes from Kafka to HDFS.

Reads from: market-quotes Kafka topic
Writes to: hdfs://namenode:9000/feature-store/raw/quotes/{date}/

Data format: Parquet, partitioned by date
Schema: symbol, price, timestamp, eventType, source
"""

from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, col, to_date, from_unixtime
from pyspark.sql.types import StructType, StructField, StringType, DoubleType, LongType

# Define schema for QuoteEvent
quote_schema = StructType([
    StructField("symbol", StringType(), False),
    StructField("price", DoubleType(), False),
    StructField("timestamp", LongType(), False),
    StructField("eventType", StringType(), False),
    StructField("source", StringType(), False)
])

def create_spark_session():
    """Create Spark session with Kafka support."""
    return SparkSession.builder \
        .appName("MarketQuotesKafkaToHDFS") \
        .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0") \
        .config("spark.sql.streaming.checkpointLocation", "/tmp/checkpoint/quotes") \
        .getOrCreate()

def ingest_quotes_to_hdfs(spark, kafka_bootstrap_servers, hdfs_output_path):
    """
    Read from Kafka market-quotes topic and write to HDFS as Parquet.

    Args:
        spark: SparkSession
        kafka_bootstrap_servers: Kafka bootstrap servers (e.g., "localhost:9092")
        hdfs_output_path: HDFS path (e.g., "hdfs://namenode:9000/feature-store/raw/quotes/")
    """

    # Read from Kafka
    kafka_df = spark \
        .readStream \
        .format("kafka") \
        .option("kafka.bootstrap.servers", kafka_bootstrap_servers) \
        .option("subscribe", "market-quotes") \
        .option("startingOffsets", "latest") \
        .option("failOnDataLoss", "false") \
        .load()

    # Parse JSON from Kafka value
    quotes_df = kafka_df.select(
        from_json(col("value").cast("string"), quote_schema).alias("data")
    ).select("data.*")

    # Add date partition column from timestamp
    quotes_with_date = quotes_df.withColumn(
        "date",
        to_date(from_unixtime(col("timestamp") / 1000))
    )

    # Write to HDFS as Parquet, partitioned by date
    query = quotes_with_date \
        .writeStream \
        .format("parquet") \
        .option("path", hdfs_output_path) \
        .option("checkpointLocation", "/tmp/checkpoint/quotes") \
        .partitionBy("date") \
        .outputMode("append") \
        .trigger(processingTime="10 seconds") \
        .start()

    return query

if __name__ == "__main__":
    # Configuration
    KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
    HDFS_OUTPUT_PATH = "hdfs://namenode:9000/feature-store/raw/quotes/"

    # Create Spark session
    spark = create_spark_session()
    spark.sparkContext.setLogLevel("WARN")

    print("=" * 80)
    print("Starting Market Quotes Kafka to HDFS Ingestion")
    print(f"Kafka: {KAFKA_BOOTSTRAP_SERVERS}")
    print(f"HDFS: {HDFS_OUTPUT_PATH}")
    print("=" * 80)

    # Start ingestion
    query = ingest_quotes_to_hdfs(spark, KAFKA_BOOTSTRAP_SERVERS, HDFS_OUTPUT_PATH)

    # Wait for termination
    query.awaitTermination()