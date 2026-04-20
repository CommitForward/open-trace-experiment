# open-trace-experiment 

## Architecture 1
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
### Pros
Offloaded Complexity: The Java app doesn't care about Kafka's availability. It hands off data to the agent in microseconds and returns to business logic.

Automatic Metadata: The agent uses the resourcedetection processor to automatically tag every span with VM Hostname, Cloud Instance ID, and OS details.

Protocol Flexibility: You can change your backend (e.g., move from Kafka to a direct SaaS) by changing one YAML file in the agent, without touching a single line of Java code.

Safety Valve: The agent has a memory_limiter. If data spikes, the agent drops spans to save itself, ensuring your main application never runs out of memory (OOM).

### Cons
Resource Overhead: Every VM needs an additional ~50MB–100MB of RAM to run the otelcol binary.

Management: You have to manage the lifecycle of a second process (e.g., via systemd or Ansible) on every VM.


## Architecture 2
```

[ VM - SERVICE 1 ]      [ VM - SERVICE 2 ]      [ VM - SERVICE N ]
  +--------------+        +--------------+        +--------------+
  |  Java App    |        |  Java App    |        |  Java App    |
  |              |        |              |        |              |
  | (OTel SDK)   |        | (OTel SDK)   |        | (OTel SDK)   |
  | (Kafka Client)|        | (Kafka Client)|        | (Kafka Client)|
  |              |        |              |        |              |
  +------|-------+        +------|-------+        +------|-------+
         |                       |                       |
         | Kafka Protocol        | Kafka Protocol        | Kafka Protocol
         | (Port 9092)           | (Port 9092)           | (Port 9092)
         |                       |                       |
         +-----------+           |           +-----------+
                     |           |           |
                     v           v           v
              +---------------------------------+
              |          APACHE KAFKA           |
              |     (Topic: otel_traces_all)    |
              |                                 |
              | - Acts as the primary buffer    |
              | - Decouples Apps from DB        |
              +----------------|----------------+
                               |
                               v
              +---------------------------------+
              |     otelcol-contrib             |
              |     (Gateway Cluster)           |
              |                                 |
              | - Consumes from Kafka           |
              | - Batching & Transformation     |
              | - PII Masking / Sampling        |
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
### Pros
Fewer Moving Parts: No need to install or manage the OTel Collector binary on your application VMs.

Reduced Infrastructure Latency: Removes one "hop" (the agent) from the data path, which can slightly reduce the time it takes for a span to reach the Kafka cluster.

## Cons
Heavier Application: Your Java app now must pull in Kafka client libraries, increasing your JAR/WAR size and dependency management complexity.

Risk to Stability: If Kafka becomes slow, the "backpressure" is felt inside your Java app. Your application's memory (Heap) will be used to buffer spans, which can lead to GC pressure or OOM.

Manual Metadata: You must write code to discover the VM's hostname/ID and manually inject it into the Resource attributes of your OTel SDK configuration.

Tight Coupling: If you want to change how data is sent (e.g., add a different header or change the topic), you must re-compile and re-deploy your entire application.

# Architecture 3
```  
[ VM INSTANCE (1 to N) ]
+------------------------------------+
|                                    |
|   +----------------------------+   |
|   |      Java Application      |   |
|   |   (OTel File Exporter)     |   |
|   +--------------|-------------+   |
|                  | Disk Write (Fastest)
|   +--------------v-------------+   |
|   |    Local Disk / Pipe       |   |
|   |   (Buffer / .json log)     |   |
|   +--------------|-------------+   |
|                  | File Tail/Read  |
|   +--------------v-------------+   |
|   |    Lightweight Shipper     |   |
|   |  (Vector / Fluent-Bit)     |   |
|   +--------------|-------------+   |
|                  |                 |
+------------------|-----------------+
                   |
                   | Network (Asynchronous)
                   v
    +------------------------------+
    |         APACHE KAFKA         |
    |    (Distributed Log Hub)     |
    +--------------|--------------+
                   |
                   v
    +------------------------------+
    |    OTel Gateway Cluster      |
    |    (Final Processing Pool)   |
    +--------------|--------------+
                   |
                   v
    +------------------------------+
    |         CLICKHOUSE           |
    |      (Columnar Storage)      |
    +--------------|--------------+
                   |
                   v
    +------------------------------+
    |      GRAFANA / JAEGER        |
    |    (Visual Analytics)        |
    +------------------------------+
```  
### Pros 
Absolute Decoupling: Java app writes to a local file/pipe; network health has zero impact on app performance.

Durability: If the network goes down, data sits safely on the disk until the shipper resumes.

### Cons
Disk I/O: Can wear out SSDs or slow down apps if the disk sub-system is already at capacity.

Complexity: Requires managing a log shipper (Vector/Fluent-Bit) and handling file rotation.


### Telemetry Shipper Comparison

| Feature | **Vector** | **Fluent Bit** | **OTel Collector** | **Filebeat** |
| :--- | :--- | :--- | :--- | :--- |
| **Language** | Rust | C | Go | Go |
| **Primary Goal** | High-perf Observability | Resource-limited VMs | Unified Telemetry | Elastic Stack Logs |
| **RAM Footprint** | ~15-30 MB | **< 10 MB** | ~50-100 MB+ | ~30-50 MB |
| **Performance** | **Ultra-High** | High | Medium/High | Medium |
| **License** | MPL-2.0 (Open) | Apache 2.0 (Open) | Apache 2.0 (Open) | Elastic (Restrictive) |
| **Backpressure** | Excellent (Disk/Mem) | Good | Moderate | Moderate |


