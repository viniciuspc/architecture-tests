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
- docker-compose up (for setting up the dependencies). The docker-compose will start flink jobmanager and taskmanager instances.
- Start redis instances (enrichment-data, decision-rules, transactions) and assigne then to the flink-compose network:

```bash
docker run --network=flink-compose_default --name redis-enrichment-data -d redis
docker run --network=flink-compose_default --name redis-decision-rules -d redis
docker run --network=flink-compose_default --name redis-transactions -d redis
```
- Populate Mock Enrichment cache, by running the `GenerateEnrichdata.jar` at apache flink webapp using the following arguments:
```
--enrichment-host "172.18.0.4" --file-path "/opt/flink/transactions.csv" --localtion "dest"
```
- Which means:
    - It will read the file given at --file-path, and use the "nameDest" column as key and an random country name as value. 
	- To use the "nameOrig" column remove the --location "dest" argument.
	- The --enrichment-host specify the ip of the running redis instance for enrichment data.

- Populate Mock rules cache by runing `MSET` command at redis, specifing wich countrys will be considered fraud, example:

```
MSET  "Guinea" "Fraud" "Poland" "Fraud" "Guyana" "Fraud" "Sri Lanka" "Fraud" "Hungry" "Fraud" "Ireland" "Fraud" "United States Minor Outlying Islands" "Fraud" "Taiwan, Province of China" "Fraud" "St. Pierre and Miquelon" "Fraud" "Congo" "Fraud"
```

- Run the experiment by, sending the `TransactionProcesser.jar` to the apache flink webapp and using the fallowing arguments:

```
--transactions-host "172.18.0.6" --decisions-rules-host "172.18.0.5" --enrichment-host "172.18.0.4" --secret "h.uzZJ$j3S^jHV" --AES "128" --file-path "/opt/flink/files/200k.csv" --model-file-path "/opt/flink/files/randomForest.model"
```
- Arguments are the following:
	- `--transactions-host` IP of the running redis instance for transactions
	- `--decisions-rules-host` IP of the running redis instance for rules
	- `--enrichment-host` IP of the running redis instance for enrichment
	- `--secret` A secret phrase used at the encripton, can be removed if no encription is required
	- `--AES` The size of AES encription "128" or "256", requires the `--secret` argument. 
	- `--file-path` File path with the transactions to be processed.
	- `--model-file-path` path with a Weka model class serilized, used to classify the transactions.


### Workflow Description
1. Open stream with the file contents.
2. Convert text into a Java Object and enrich it with country information.
3. (Work in progress) Make an agregation in time window and sum the amount value.
4. Apply rules.
5. Apply machine learning model.
6. Convert the Java Object in json and encrypt it if necessary.
7. Sink the json to the redis instance of transactions.
