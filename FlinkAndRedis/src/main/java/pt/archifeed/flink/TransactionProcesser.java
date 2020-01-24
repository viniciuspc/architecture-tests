package pt.archifeed.flink;

import java.time.LocalTime;
import java.util.Vector;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

import pt.archifeed.encription.AES;
import pt.archifeed.encription.AES128;
import pt.archifeed.encription.AES256;
import pt.archifeed.flink.MapFunctions.ApplyMLMapper;
import pt.archifeed.flink.MapFunctions.EnrichMapper;
import pt.archifeed.flink.MapFunctions.JsonEncryptMapper;
import pt.archifeed.flink.MapFunctions.RulesMapper;
import pt.archifeed.flink.model.TransactionModel;
import weka.core.SerializationHelper;

public class TransactionProcesser {
	
	private static Vector v = null;
	
	public static void main(String[] args) throws Exception {
		
		ParameterTool params = ParameterTool.fromArgs(args);
		
		FlinkJedisPoolConfig conf = new FlinkJedisPoolConfig.Builder().setHost(params.get("transactions-host")).setPort(6379).build();
		String decisionsRulesHost = params.get("decisions-rules-host");
		String enrichmentHost = params.get("enrichment-host");
		String secret = params.get("secret");
		String aesType = params.get("AES");
		if(params.has("model-file-path")) {
		//Load the vector only if it is not yet loaded (by other thread) yet.
			if(v == null) {
				v = (Vector) SerializationHelper.read(params.get("model-file-path"));
			}
		}
		
		//StreamExecutionEnvironment env =StreamExecutionEnvironment.createRemoteEnvironment("localhost", 8081);
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
		
		
		
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
		SingleOutputStreamOperator<Tuple2<String, Double>> aggregate = transactionModels
										.assignTimestampsAndWatermarks(new HourlyExtractor(Time.hours(1)))
										.keyBy((transaction) -> transaction.getNameOrig())
										.window(TumblingEventTimeWindows.of(Time.hours(2)))
										.aggregate(new SumAggregate());
		
		//4 - apply rules - country and sum of the amunt Output TransactionModel
		SingleOutputStreamOperator<TransactionModel> transactionModelsWithRules = transactionModels.map(new RulesMapper(decisionsRulesHost));
		//5 - apply machine learning model
		SingleOutputStreamOperator<TransactionModel> transactionModelsWithMlApplied = transactionModelsWithRules;
		if(v != null) {
			//Only apply ml if a machine lerning model was provide (v contains that model)
			transactionModelsWithMlApplied = transactionModelsWithRules.map(new ApplyMLMapper(v));
		}
		//6 - Convert to json and encrypt if is nescessary. Output Tuple2<String,String>
		SingleOutputStreamOperator<Tuple2<String,String>> tuples = transactionModelsWithMlApplied.map(new JsonEncryptMapper(initialTime, aes));
		//7 - sink this to redis.
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
	
	public static class HourlyExtractor extends BoundedOutOfOrdernessTimestampExtractor<TransactionModel> {

		public HourlyExtractor(Time maxOutOfOrderness) {
			super(maxOutOfOrderness);
			
		}

		@Override
		public long extractTimestamp(TransactionModel element) {
			return Time.hours(element.getStep()-1).toMilliseconds();
		}
		
	}
	
	/**
	 * Sum the amount for a key and a time window
	 * @author viniciuspc
	 *
	 */
	public static class SumAggregate implements AggregateFunction<TransactionModel, Tuple2<String,Double>, Tuple2<String,Double>> {

		@Override
		public Tuple2<String, Double> createAccumulator() {
			return new Tuple2<String, Double>("", 0.0);
		}

		@Override
		public Tuple2<String, Double> add(TransactionModel value, Tuple2<String, Double> accumulator) {
			double sum = accumulator.f1;
			if(accumulator.f1 > 0.0 && value.getAmount() > accumulator.f1) {
				//is fraud will mark transaction as a fraud;
				value.setFraud(true);
			} else {
				sum+=value.getAmount();
			}
			return new Tuple2<String, Double>(value.getNameOrig(), sum);
		}

		@Override
		public Tuple2<String, Double> getResult(Tuple2<String, Double> accumulator) {
			return accumulator;
		}

		@Override
		public Tuple2<String, Double> merge(Tuple2<String, Double> a, Tuple2<String, Double> b) {
			return new Tuple2<String, Double>(a.f0, a.f1+b.f1);
		}

		
		
	}
}


