# open-trace-experiment

DATA CENTER / CLOUD VPC (VM BASED)
+-------------------------------------------------------------------------------------------------------+
|                                                                                                       |
|   [ VM INSTANCE 1..N ]            [ BUFFER LAYER ]                [ INGESTION LAYER ]                 |
|                                                                     (Collector Pool)                  |
|   +-------------------+                                                                               |
|   |   Java Service    |                                           +-----------------------+           |
|   | (OTel SDK/Agent)  |                                           |  OTel Gateway Cluster |           |
|   +---------|---------+       +-------------------------------+   |  (On Dedicated VMs)   |           |
|             | OTLP/gRPC       |         APACHE KAFKA          |   |  - Kafka Receiver     |           |
|   +---------v---------+       |  Topic: otel_traces_protobuf  |   |  - ClickHouse Exporter|           |
|   | OTel Host Agent   | ----> |  (Clustered Brokers)          |   +-----------|-----------+           |
|   | (Binary/Systemd)  |       +-------------------------------+               |                       |
|   +-------------------+                       ^                               |                       |
|                                               |                               |                       |
|   [ VM INSTANCE 2 ]                           |                               |                       |
|   +-------------------+                       |                               |                       |
|   |   Java Service    | ----------------------+                               |                       |
|   +-------------------+                                                       |                       |
|                                                                               v                       |
+-------------------------------------------------------------------------------|-----------------------+
                                                                                |
                                                                                v
+-------------------------------------------------------------------------------------------------------+
|                                                                                                       |
|   [ STORAGE LAYER ]               [ VISUALIZATION ]               [ OBSERVABILITY ]                   |
|                                                                                                       |
|   +-----------------------+       +-------------------------------+       +-----------------------+   |
|   |      CLICKHOUSE       |       |       GRAFANA / JAEGER        |       |    PROMETHEUS / ELK   |   |
|   |      (Columnar)       | <---- |      (Data Source)            |       |    (Optional Extras)  |   |
|   | - otel_traces table   |       | - Trace Analytics             |       +-----------------------+   |
|   | - otel_tags table     |       | - Service Maps                |                                   |
|   +-----------------------+       +-------------------------------+                                   |
|                                                                                                       |
+-------------------------------------------------------------------------------------------------------+