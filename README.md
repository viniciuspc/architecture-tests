# archicteture-test
This repo servers as a PoC for `archifeed` architectural project. Design decisions are exerciced on the repo aiming to achieve Performance goals:

<b>Throughput</b>: 2000 tr/s

<b>Latency</b>: 200ms (99 percentile)
  
This PoC is intended to test the behaviour of the tecnologies used in the transaction scoring workflow, which may directly impact the performance of the whole process.
Additional architectural components were not considered in these testing, since they operate in an un-bounded context which does not influence the normal workflow processing.


### Main technologies used: 
- Jedis (Redis connection for java).
- Apache Flink (Stream process engine).
- Weka (ML Training)


### Setup
1. docker-compose up (for setting up the dependencies). The docker-compose command execution will bootstrap both flink jobmanager and taskmanager instances.
2. Start redis instances (enrichment-data, decision-rules, transactions) and assign them to the same network created by flink-compose:

```bash
docker run --network=flink-compose_default --name redis-enrichment-data -d redis
docker run --network=flink-compose_default --name redis-decision-rules -d redis
docker run --network=flink-compose_default --name redis-transactions -d redis
```
3. Populate Mock Enrichment cache, by running the `GenerateEnrichdata.jar` in apache flink webapp using the following arguments:
```
--enrichment-host "172.18.0.4" --file-path "/opt/flink/transactions.csv" --location "dest"
```
- Which means:
    - It will read the file given at --file-path, and use the "nameDest" column as key and an random country name as value. 
	- To use the "nameOrig" column remove the --location "dest" argument.
	- The --enrichment-host specifies the ip of the running redis instance for enrichment data.

4. Populate Mock rules cache by runing `MSET` command against the redis server, specifing the origin countries from which transaction will be considered fraud, example:

```
MSET  "Guinea" "Fraud" "Poland" "Fraud" "Guyana" "Fraud" "Sri Lanka" "Fraud" "Hungry" "Fraud" "Ireland" "Fraud" "United States Minor Outlying Islands" "Fraud" "Taiwan, Province of China" "Fraud" "St. Pierre and Miquelon" "Fraud" "Congo" "Fraud"
```

5. Run the experiment by, sending the `TransactionProcesser.jar` to the apache flink webapp and using the following arguments:

```
--transactions-host "172.18.0.6" --decisions-rules-host "172.18.0.5" --enrichment-host "172.18.0.4" --secret "h.uzZJ$j3S^jHV" --AES "128" --file-path "/opt/flink/files/200k.csv" --model-file-path "/opt/flink/files/randomForest.model"
```
- Arguments:
	- `--transactions-host` IP of the running redis instance holding transaction's data.
	- `--decisions-rules-host` IP of the running redis instance holding decison rules' data.
	- `--enrichment-host` IP of the running redis instance for holding enrichment data.
	- `--secret` A secret phrase used for encripton (can be removed if no encription is required)
	- `--AES` The size of AES encription "128" or "256", requires the `--secret` argument. 
	- `--file-path` File path for source transactions.
	- `--model-file-path` path with a Weka model class serilized, used to classify the transactions.


### Workflow Description
1. Open a stream with the file content.
2. Convert text into a Java Object and enrich it with countries' information.
3. Single value aggregation. (WIP)
4. Apply rules.
5. Apply machine learning model.
6. Convert the Java Object into a json response and encrypt it if needed.
7. Sink json result to the processed transactions' redis instance.
