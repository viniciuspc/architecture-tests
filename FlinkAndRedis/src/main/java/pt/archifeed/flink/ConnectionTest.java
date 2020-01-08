package pt.archifeed.flink;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import pt.archifeed.flink.TransactionProcesser.HourlyExtractor;
import pt.archifeed.flink.TransactionProcesser.SumAggregate;
import pt.archifeed.flink.MapFunctions.EnrichMapper;
import pt.archifeed.flink.model.TransactionModel;

/**
 * Make a simple connection with apache flink and make operations on data.
 * @author viniciuspc
 *
 */
public class ConnectionTest {
	
	public static void main(String[] args) throws Exception {
		final ParameterTool params = ParameterTool.fromArgs(args);
		
		//ExecutionEnvironment env = CollectionEnvironment.createRemoteEnvironment("localhost", 8081);
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
		
//		DataSet<Integer> amounts = env.fromElements(1, 29,40, 50);
//		
//		int threshold = 30;
//		List<Integer> collect = amounts
//				 .filter(a -> a > threshold)
//				 .reduce((integer, t1) -> integer + t1)
//				
//				.collect();
//		
//		System.out.println(collect);
		
		DataStream<String> text = env.readTextFile("text-files/timewindowtest.csv");
		
		SingleOutputStreamOperator<TransactionModel> transactionModels = text
																																				.map(new MapFunction<String, TransactionModel>() {

																																					@Override
																																					public TransactionModel map(String value) throws Exception {
																																						String[] fields = value.split(",");
																																						return new TransactionModel(fields);
																																						
																																					}
																																				})
																																				.assignTimestampsAndWatermarks(new HourlyExtractor(Time.hours(1)));
		//transactionModels.print();
		
		SingleOutputStreamOperator<Tuple2<String, Double>> aggregate = transactionModels
				.keyBy((transaction) -> transaction.getNameOrig())
				.window(SlidingEventTimeWindows.of(Time.hours(2),Time.hours(1)))
				.aggregate(new SumAggregate());
		
		aggregate.print();
		
		
		
		env.execute();
		
		
		
	}

}
