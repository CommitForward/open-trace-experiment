# open-trace-experiment 

## Typical Architecture
```
[ VM - SERVICE 1 ]      [ VM - SERVICE 2 ]      [ VM - SERVICE N ]
  +--------------+        +--------------+        +--------------+
  |  Java App    |        |  Java App    |        |  Java App    |
  +------|-------+        +------|-------+        +------|-------+
         | OTLP/gRPC (Port 4317) |                       | 
  +------v-------+        +------v-------+        +------v-------+
  |  otelcol     |        |  otelcol     |        |  otelcol     |
  | (Host Agent) |        | (Host Agent) |        | (Host Agent) |
  +------|-------+        +------|-------+        +------|-------+
         |                       |                       |
         +-----------+           |           +-----------+
                     |           |           |
                     v           v           v
              +---------------------------------+
              |          APACHE KAFKA           |
              |     (Port 9092 | Proto)         |
              +----------------|----------------+
                               |
                               v
              +---------------------------------+
              |     otelcol-contrib             |
              |     (Gateway Cluster)           |
              |                                 |
              | - Consumes from Kafka           |
              | - Batching (Port 4317/4318)     |
              +----------------|----------------+
                               |
                               v
              +---------------------------------+
              |          CLICKHOUSE DB          |
              |        (Port 9000 | Native)     |
              +----------------|----------------+
                               |
                               v
              +---------------------------------+
              |        GRAFANA / JAEGER         |
              |       (Visual Analytics)        |
              +---------------------------------+


```