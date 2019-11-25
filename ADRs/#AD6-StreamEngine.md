# AD6 - Stream Engine

## Status

Accepted

## Context

Due to the input nature of the data being processed, it is necessary to use some sort of realtime event processor. There are some tools on the market aiming event processing by batching and realtime, nevertheless the project requires realtime only.

## Decision

Usage of Apache Flink. 
The selected technology is High Throughput/ Low Latency optimized, achieving a median latency of 0 milliseconds (20 milliseconds on 99th percentile). The corresponding Throughput is 24.500events/second per core.

## Consequences

NA

## Alternatives

Apache Spark;