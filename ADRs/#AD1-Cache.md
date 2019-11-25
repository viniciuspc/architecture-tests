# AD1 - Caching tecnologies

## Status

Accepted

## Context

Data required to evaluate transactions needs to be fast accessed during a transaction evaluation process. The best possible manner to ensure the desired performance in data access is to keep it in memory, more preciselly in a memory cache system.

This raises the need of including a KVS cache system maintained in memory.

## Decision

Use Redis for kvs caching.
Redis is a distributed, high availability KVS caching system that maintains data in memory.

## Consequences

NA

## Alternatives

Cassandra; RocksDb;