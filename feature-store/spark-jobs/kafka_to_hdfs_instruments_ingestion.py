"""
Spark Structured Streaming job to ingest market instruments from Kafka to HDFS.

Reads from: market-instruments Kafka topic
Writes to: hdfs://namenode:9000/feature-store/raw/instruments/{date}/

Data format: Parquet, partitioned by date
Schema: symbol, description, action, timestamp, source
"""

from pyspark.sql import SparkSession
from pyspark.sql.functions import from_json, col, to_date, from_unixtime
from pyspark.sql.types import StructType, StructField, StringType, LongType

# Define schema for InstrumentEvent
instrument_schema = StructType([
    StructField("symbol", StringType(), False),
    StructField("description", StringType(), False),
    StructField("action", StringType(), False),
    StructField("timestamp", LongType(), False),
    StructField("source", StringType(), False)
])

def create_spark_session():
    """Create Spark session with Kafka support."""
    return SparkSession.builder \
        .appName("MarketInstrumentsKafkaToHDFS") \
        .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0") \
        .config("spark.sql.streaming.checkpointLocation", "/tmp/checkpoint/instruments") \
        .getOrCreate()

def ingest_instruments_to_hdfs(spark, kafka_bootstrap_servers, hdfs_output_path):
    """
    Read from Kafka market-instruments topic and write to HDFS as Parquet.

    Args:
        spark: SparkSession
        kafka_bootstrap_servers: Kafka bootstrap servers
        hdfs_output_path: HDFS path
    """

    # Read from Kafka
    kafka_df = spark \
        .readStream \
        .format("kafka") \
        .option("kafka.bootstrap.servers", kafka_bootstrap_servers) \
        .option("subscribe", "market-instruments") \
        .option("startingOffsets", "latest") \
        .option("failOnDataLoss", "false") \
        .load()

    # Parse JSON from Kafka value
    instruments_df = kafka_df.select(
        from_json(col("value").cast("string"), instrument_schema).alias("data")
    ).select("data.*")

    # Add date partition column from timestamp
    instruments_with_date = instruments_df.withColumn(
        "date",
        to_date(from_unixtime(col("timestamp") / 1000))
    )

    # Write to HDFS as Parquet, partitioned by date
    query = instruments_with_date \
        .writeStream \
        .format("parquet") \
        .option("path", hdfs_output_path) \
        .option("checkpointLocation", "/tmp/checkpoint/instruments") \
        .partitionBy("date") \
        .outputMode("append") \
        .trigger(processingTime="10 seconds") \
        .start()

    return query

if __name__ == "__main__":
    # Configuration
    KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
    HDFS_OUTPUT_PATH = "hdfs://namenode:9000/feature-store/raw/instruments/"

    # Create Spark session
    spark = create_spark_session()
    spark.sparkContext.setLogLevel("WARN")

    print("=" * 80)
    print("Starting Market Instruments Kafka to HDFS Ingestion")
    print(f"Kafka: {KAFKA_BOOTSTRAP_SERVERS}")
    print(f"HDFS: {HDFS_OUTPUT_PATH}")
    print("=" * 80)

    # Start ingestion
    query = ingest_instruments_to_hdfs(spark, KAFKA_BOOTSTRAP_SERVERS, HDFS_OUTPUT_PATH)

    # Wait for termination
    query.awaitTermination()