package pt.archifeed.flink.MapFunctions;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.json.JSONObject;

import pt.archifeed.encription.AES;
import pt.archifeed.flink.model.TransactionModel;

/**
 * Convert the TransactionMapper into a json and encrypt if nescessary
 * @author viniciuspc
 *
 */
public class JsonEncryptMapper extends RichMapFunction<TransactionModel, Tuple2<String,String>>{

	
	private static final long serialVersionUID = -8672691772406957924L;
	
	private static AtomicInteger id = new AtomicInteger(1);
	private LocalTime initialTime;
	private AES aes;
	
	public JsonEncryptMapper(LocalTime initialTime, AES aes) {
		this.initialTime = initialTime;
		this.aes = aes;
	}
	
	@Override
	public Tuple2<String, String> map(TransactionModel transaction) throws Exception {
		
		//Increase the counter
		String key = String.valueOf(id.getAndIncrement());
		
		if(id.get()%10000 == 0) {
			System.out.println(id.get()+": "+Duration.between(this.initialTime, LocalTime.now()).toMillis());
		}
		
		//Convert Transaction to json string
		String json = new JSONObject(transaction).toString();
		
		//If a aes class is available we encrypt
		if(this.aes != null) {
			json = aes.encrypt(json);
		}
		
		//Convert Transaction to json string
		Tuple2<String, String> tuple2 = new Tuple2<String, String>(key, json);
		
		return tuple2;
	}

}
