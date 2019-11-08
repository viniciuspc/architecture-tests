package pt.archifeed.flink;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
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
		
		
		FlinkJedisPoolConfig conf = new FlinkJedisPoolConfig.Builder().setHost("172.17.0.4").setPort(6379).build();
		
//		Jedis decisionRules = new Jedis("172.17.0.2", 6379);
//		Jedis enrichmentData = new Jedis("172.17.0.3", 6379);
		
		try {
		
			StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
			
			
			
			long now = System.currentTimeMillis();
			DataStream<String> text = env.readTextFile("text-files/minitransaction.csv");
			
			SingleOutputStreamOperator<Tuple2<String,String>> tuples = text.map(new TransctionMapper(now));
			
			tuples.addSink(new RedisSink<Tuple2<String, String>>(conf, new RedisExampleMapper()));
			
			
			env.execute();
			long time = System.currentTimeMillis()-now;
			System.out.println("Total time: "+time+" Number of transactions: "+(TransctionMapper.getLastId()-1)+" Avarage time per transaction: "+(time/(TransctionMapper.getLastId()-1.0)));
			
		} finally {
//			decisionRules.close();
//			enrichmentData.close();
		}
	}
	
	public static class TransctionMapper implements MapFunction<String, Tuple2<String,String>>{
		/**
		 * 
		 */
		private static final long serialVersionUID = -3656332727175530315L;
		
//		private Jedis decisionRules;
//		private Jedis enrichmentData;
		//Automatic generate id;
		private static int id = 1;
		private long now;
		
//		public TransctionMapper(Jedis decisionRules, Jedis enrichmentData) {
//			super();
//			this.decisionRules = decisionRules;
//			this.enrichmentData = enrichmentData;
//		}


		public TransctionMapper(long now) {
			// TODO Auto-generated constructor stub
			this.now = now;
		}

		@Override
		public Tuple2<String,String> map(String value) throws Exception {
			Jedis decisionRules = new Jedis("172.17.0.2", 6379);
			Jedis enrichmentData = new Jedis("172.17.0.3", 6379);
			String[] fields = value.split(",");
			TransactionModel transaction = new TransactionModel(fields);
			
			transaction.setCountryOrig(enrichmentData.get(transaction.getNameOrig()));
			
			transaction.setCountryDest(enrichmentData.get(transaction.getNameDest()));
			
			//If the country of origin or destination is on decision rules then set trasanction as fraud
			if(decisionRules.exists(transaction.getCountryOrig(),transaction.getCountryDest()) > 0 ) {
				transaction.setFraud(true);
			}
			
			
			if(id%10000 == 0) {
				System.out.println(id+": "+(System.currentTimeMillis()-this.now));
			}
			
			
			String key = String.valueOf(id);
			//Increase the counter
			id++;
			
			//Convert Transaction to json string
			String json = new JSONObject(transaction).toString();
			
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


