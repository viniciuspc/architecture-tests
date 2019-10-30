package pt.archifeed.flink;

import java.util.List;

import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * Make a simple connection with apache flink and make operations on data.
 * @author viniciuspc
 *
 */
public class ConnectionTest {
	
	public static void main(String[] args) throws Exception {
		ExecutionEnvironment env = ExecutionEnvironment.createRemoteEnvironment("localhost", 8081);
		DataSet<Integer> amounts = env.fromElements(1, 29,40, 50);
		
		int threshold = 30;
		List<Integer> collect = amounts
				  .filter(a -> a > threshold)
				  .reduce((integer, t1) -> integer + t1)
				  .collect();
		
		System.out.println(collect);
		
	}

}
