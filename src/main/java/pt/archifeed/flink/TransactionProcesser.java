package pt.archifeed.flink;

import java.time.Duration;
import java.time.LocalTime;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;
import org.json.JSONObject;

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
		env.setParallelism(1);
		//String secret = "h.uzZJ$j3S^jHV";
		String secret = params.get("secret");
		
		LocalTime initialTime = LocalTime.now();
		//DataStream<String> text = env.readTextFile("text-files/minitransaction.csv");
		DataStream<String> text = env.readTextFile(params.get("file-path"));
		
		SingleOutputStreamOperator<Tuple2<String,String>> tuples = text.map(new TransctionMapper(initialTime,secret, decisionsRulesHost, enrichmentHost));
		
		tuples.addSink(new RedisSink<Tuple2<String, String>>(conf, new RedisExampleMapper()));
		
		
		env.execute();
		LocalTime finalTime = LocalTime.now();
		Duration duration = Duration.between(initialTime, finalTime);
		int numberTransactions = TransctionMapper.getLastId()-1;
		System.out.println("Total time: "+duration.toMillis()+" Number of transactions: "+numberTransactions+" Avarage time per transaction: "+(duration.toMillis()/(numberTransactions*1.0)+" Transactions/second: "+numberTransactions/(duration.getSeconds()*1.0)));
		

	}
	
	public static class TransctionMapper implements MapFunction<String, Tuple2<String,String>>{
		/**
		 * Maps a line of the file to a id, and a json.
		 */
		private static final long serialVersionUID = -3656332727175530315L;
		
//		private Jedis decisionRules;
//		private Jedis enrichmentData;
		//Automatic generate id;
		private static int id = 1;
		private LocalTime initialTime;
		private String secret;
		private String decisionsRulesHost;
		private String enrichmentHost;
		
//		public TransctionMapper(Jedis decisionRules, Jedis enrichmentData) {
//			super();
//			this.decisionRules = decisionRules;
//			this.enrichmentData = enrichmentData;
//		}


		public TransctionMapper(LocalTime initialTime, String secret, String decisionsRulesHost, String enrichmentHost) {
			// TODO Auto-generated constructor stub
			this.initialTime = initialTime;
			this.secret = secret;
			this.decisionsRulesHost = decisionsRulesHost;
			this.enrichmentHost = enrichmentHost;
		}

		@Override
		public Tuple2<String,String> map(String value) throws Exception {
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
			
			
			if(id%10000 == 0) {
				System.out.println(id+": "+Duration.between(this.initialTime, LocalTime.now()).toMillis());
			}
			
			
			String key = String.valueOf(id);
			//Increase the counter
			id++;
			
			//Convert Transaction to json string
			String json = new JSONObject(transaction).toString();
			
			//If a secret is available we encrypt
			if(this.secret != null) {
				json = AES.encrypt(json, this.secret);
			}
			
			decisionRules.close();
			enrichmentData.close();
			
			//Convert Transaction to json string
			return new Tuple2<String, String>(key, json);
		}
		
		public static int getLastId() {
			return id;
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


