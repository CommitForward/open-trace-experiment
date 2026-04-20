# open-trace-experimen text

graph TD
    subgraph VM_Instance ["VM Instance (1 to N)"]
        App[Java Application / OTel SDK] -->|OTLP/gRPC| Agent[OTel Host Agent]
    end

    Agent -->|otlp_proto| Kafka[(Apache Kafka)]
    
    Kafka -->|Consume| Gateway[OTel Gateway Cluster]
    
    subgraph Processing ["Processing Layer"]
        Gateway -->|Transform/Sample| CH[(ClickHouse DB)]
    end
    
    CH --> UI[Grafana / Jaeger UI]

    style VM_Instance fill:#f9f9f9,stroke:#333,stroke-dasharray: 5 5
    style Kafka fill:#e1f5fe,stroke:#01579b
    style CH fill:#e8f5e9,stroke:#2e7d32