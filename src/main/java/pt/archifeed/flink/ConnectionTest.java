package pt.archifeed.flink;

import java.util.List;

import org.apache.flink.api.java.CollectionEnvironment;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.utils.ParameterTool;

/**
 * Make a simple connection with apache flink and make operations on data.
 * @author viniciuspc
 *
 */
public class ConnectionTest {
	
	public static void main(String[] args) throws Exception {
		final ParameterTool params = ParameterTool.fromArgs(args);
		
		//ExecutionEnvironment env = CollectionEnvironment.createRemoteEnvironment("localhost", 8081);
		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		env.getConfig().setGlobalJobParameters(params);
		
		DataSet<Integer> amounts = env.fromElements(1, 29,40, 50);
		
		int threshold = 30;
		List<Integer> collect = amounts
				 .filter(a -> a > threshold)
				 .reduce((integer, t1) -> integer + t1)
				
				.collect();
		
		System.out.println(collect);
	}

}
