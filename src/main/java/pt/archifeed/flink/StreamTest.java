package pt.archifeed.flink;


import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;


public class StreamTest {
	public static void main(String[] args) throws Exception {
		
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		
		long now = System.currentTimeMillis();
		DataStream<String> text = env.readTextFile("text-files/transactions.csv");
		
		SingleOutputStreamOperator<String> upperCase = text.map(String::toUpperCase);
		
		upperCase.print();
		
		
		env.execute();
		long time = System.currentTimeMillis()-now;
		System.out.println("Total time: "+time+" Number of transactions: 24.000.000 Avarage time per transaction: "+(time/24000000.0));
	}
}
