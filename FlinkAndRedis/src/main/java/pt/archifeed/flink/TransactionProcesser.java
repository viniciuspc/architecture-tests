package pt.archifeed.flink;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;
import org.json.JSONObject;

import pt.archifeed.encription.AES;
import pt.archifeed.encription.AES128;
import pt.archifeed.encription.AES256;
import pt.archifeed.flink.MapFunctions.EnrichMapper;
import pt.archifeed.flink.MapFunctions.JsonEncryptMapper;
import pt.archifeed.flink.MapFunctions.RulesMapper;
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
		env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
		
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
		
		//Steps:
		//1 - Read file stream. Output text
		DataStream<String> text = env.readTextFile(params.get("file-path"));
		
		//2 - Convert text into Object while enrich it with the country information. Output TransactionModel
		SingleOutputStreamOperator<TransactionModel> transactionModels = text.map(new EnrichMapper(enrichmentHost));
		//3 - Make a agregation in a time window and sum the amount. Otput ...
		//transactionModels.keyBy((transaction) -> transaction.getNameOrig()).timeWindow(Time.hours(2)).reduce((a,b) -> a.getAmount()+b.getAmount());
		
		//4 - apply rules - country and sum of the amunt Output TransactionModel
		SingleOutputStreamOperator<TransactionModel> transactionModelsWithRules = transactionModels.map(new RulesMapper(decisionsRulesHost));
		//5 - Convert to json and encrypt if is nescessary. Output Tuple2<String,String>
		SingleOutputStreamOperator<Tuple2<String,String>> tuples = transactionModelsWithRules.map(new JsonEncryptMapper(initialTime, aes));
		//6 - sink this to redis.
		tuples.addSink(new RedisSink<Tuple2<String, String>>(conf, new RedisExampleMapper())).name("RedisSink");
		
		
		env.execute();

	}
	
	public static class RedisExampleMapper implements RedisMapper<Tuple2<String, String>>{
		
		private static final long serialVersionUID = -3300467364501490398L;

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


