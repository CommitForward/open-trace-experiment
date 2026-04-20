# open-trace-experiment

[ VM INSTANCE (1 to N) ]
+------------------------------------+
|                                    |
|   +----------------------------+   |
|   |      Java Application      |   |
|   |  (Maven / OTel Java SDK)   |   |
|   +--------------|-------------+   |
|                  | OTLP/gRPC       |
|   +--------------v-------------+   |
|   |    OTel Host Agent         |   |
|   |   (Binary / Systemd)       |   |
|   |                            |   |
|   |  - Resource Detection      |   |
|   |  - Batching                |   |
|   |  - Kafka Exporter          |   |
|   +--------------|-------------+   |
|                  |                 |
+------------------|-----------------+
                   |
                   v
    +------------------------------+
    |         APACHE KAFKA         |
    |  (Topic: otel_traces_proto)  |
    +--------------|--------------+
                   |
                   v
    +------------------------------+
    |    OTel Gateway Cluster      |
    |    (Dedicated VM Pool)       |
    |                              |
    |  - Kafka Receiver            |
    |  - Tail-Based Sampling       |
    |  - ClickHouse Exporter       |
    +--------------|--------------+
                   |
                   v
    +------------------------------+
    |         CLICKHOUSE           |
    |      (Columnar DB)           |
    |                              |
    |  - Trace & Span Tables       |
    |  - 30 Day TTL / Retention    |
    +--------------|--------------+
                   |
                   v
    +------------------------------+
    |      GRAFANA / JAEGER        |
    |    (Visualization UI)        |
    +------------------------------+