package pt.archifeed.flink;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;
import org.json.JSONObject;

import pt.archifeed.encription.AES;
import pt.archifeed.encription.AES128;
import pt.archifeed.encription.AES256;
import pt.archifeed.flink.Metrics.CustomDropwizardMeterWrapper;
import pt.archifeed.flink.Metrics.LatencyMeter;
import pt.archifeed.flink.model.TransactionModel;
import redis.clients.jedis.Jedis;

public class TransactionProcesser {
	
	public static void main(String[] args) throws Exception {
		
		ParameterTool params = ParameterTool.fromArgs(args);
		
		//FlinkJedisPoolConfig conf = new FlinkJedisPoolConfig.Builder().setHost("172.17.0.4").setPort(6379).build();
		
		FlinkJedisPoolConfig conf = new FlinkJedisPoolConfig.Builder().setHost(params.get("transactions-host")).setPort(6379).build();
		String decisionsRulesHost = params.get("decisions-rules-host");
		String enrichmentHost = params.get("enrichment-host");
		
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		
		//env.setParallelism(1);
		//String secret = "h.uzZJ$j3S^jHV";
		String secret = params.get("secret");
		String aesType = params.get("AES");
		
		AES aes = null;
		if(secret != null && !secret.isEmpty() && aesType != null && !aesType.isEmpty()) {
			if(aesType.equals("128")) {
				aes = new AES128(secret);
			} else if(aesType.equals("256")) {
				aes = new AES256(secret);
			}
		}
		
		LocalTime initialTime = LocalTime.now();
		//DataStream<String> text = env.readTextFile("text-files/minitransaction.csv");
		DataStream<String> text = env.readTextFile(params.get("file-path"));
		
		SingleOutputStreamOperator<Tuple2<String,String>> tuples = text.map(new TransctionMapper(initialTime,aes, decisionsRulesHost, enrichmentHost));
		
		tuples.addSink(new RedisSink<Tuple2<String, String>>(conf, new RedisExampleMapper())).name("RedisSink");
		
		
		env.execute();
		
		LocalTime finalTime = LocalTime.now();
		Duration duration = Duration.between(initialTime, finalTime);
		
		int numberTransactions = TransctionMapper.getLastId()-1;
		System.out.println("Total time: "+duration.toMillis()+" Number of transactions: "+numberTransactions+" Avarage time per transaction: "+(duration.toMillis()/(numberTransactions*1.0)+" Transactions/second: "+numberTransactions/(duration.getSeconds()*1.0)));
		

	}
	
	public static class TransctionMapper extends RichMapFunction<String, Tuple2<String,String>>{
		/**
		 * Maps a line of the file to a id, and a json.
		 */
		private static final long serialVersionUID = -3656332727175530315L;
		
		private static AtomicInteger id = new AtomicInteger(1);
		private LocalTime initialTime;
		private AES aes;
		private String decisionsRulesHost;
		private String enrichmentHost;
		
		private transient Meter meter;
		private transient Meter latencyMeter;


		public TransctionMapper(LocalTime initialTime, AES aes, String decisionsRulesHost, String enrichmentHost) {
			// TODO Auto-generated constructor stub
			this.initialTime = initialTime;
			this.aes = aes;
			this.decisionsRulesHost = decisionsRulesHost;
			this.enrichmentHost = enrichmentHost;
		}
		
		@Override
		public void open(Configuration parameters) throws Exception {
			super.open(parameters);
			this.meter = getRuntimeContext()
					.getMetricGroup().addGroup("Metrics")
					.meter("Trhoughput", new CustomDropwizardMeterWrapper(new com.codahale.metrics.Meter()));
			this.latencyMeter = getRuntimeContext().
					getMetricGroup().addGroup("Latency")
					.meter("Latency", new LatencyMeter());
		}

		@Override
		public Tuple2<String,String> map(String value) throws Exception {
			this.meter.markEvent();
			this.latencyMeter.markEvent();
			
			Jedis decisionRules = new Jedis(this.decisionsRulesHost, 6379);
			Jedis enrichmentData = new Jedis(this.enrichmentHost, 6379);
			String[] fields = value.split(",");
			TransactionModel transaction = new TransactionModel(fields);
			
			transaction.setCountryOrig(enrichmentData.get(transaction.getNameOrig()));
			
			transaction.setCountryDest(enrichmentData.get(transaction.getNameDest()));
			
			//If the country of origin or destination is on decision rules then set trasanction as fraud
			if(decisionRules.exists(transaction.getCountryOrig(),transaction.getCountryDest()) > 0 ) {
				transaction.setFraud(true);
			}
			
			//Increase the counter
			String key = String.valueOf(id.getAndIncrement());
			
			if(id.get()%10000 == 0) {
				System.out.println(id.get()+": "+Duration.between(this.initialTime, LocalTime.now()).toMillis());
			}
			
			
			
			
			
			
			//Convert Transaction to json string
			String json = new JSONObject(transaction).toString();
			
			//If a secret is available we encrypt
			if(this.aes != null) {
				json = aes.encrypt(json);
			}
			
			decisionRules.close();
			enrichmentData.close();
			
			//Convert Transaction to json string
			Tuple2<String, String> tuple2 = new Tuple2<String, String>(key, json);
			
			return tuple2;
		}
		
		public static int getLastId() {
			return id.get();
		}
		
	}
	
	public static class RedisExampleMapper implements RedisMapper<Tuple2<String, String>>{
		
		
	    @Override
	    public RedisCommandDescription getCommandDescription() {
	        return new RedisCommandDescription(RedisCommand.SET, "TRANSACTIONS");
	    }

	    @Override
	    public String getKeyFromData(Tuple2<String, String> data) {
	        return data.f0;
	    }

	    @Override
	    public String getValueFromData(Tuple2<String, String> data) {
	        return data.f1;
	    }
	}
}


