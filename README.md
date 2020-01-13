# archicteture-test
This repo servers as a PoC for `archifeed` architectural project. Design decisions are exerciced on the repo aiming to achieve Performance goals:

<b>Throughput<b>: 2000 tr/s
<b>Latency<b>: 200ms (99 percentile)
  
This PoC is intended to test the behaviour of the tecnologies used in the transaction scoring workflow, which may directly impact the performance of the whole process.
Additional architectural components were not considered in these testing, since they operate in an un-bounded context which does not influence the normal workflow processing.


### Main technologies used: 
- Jedis (Redis connection for java).
- Apache Flink (Stream process engine).
- Weka (ML Training)


### Setup
- docker-compose up (for setting up the dependencies)
- Populate Mock rules cache ...
- Populate Mock Enrichment cache ...
- Setup Flink ...
- Deploy wokflow ...
- Run Experiment


### Workflow Description
