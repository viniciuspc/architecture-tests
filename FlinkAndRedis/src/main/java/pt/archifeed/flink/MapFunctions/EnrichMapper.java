package pt.archifeed.flink.MapFunctions;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Meter;
import org.apache.flink.metrics.MetricGroup;

import pt.archifeed.flink.Metrics.CustomDropwizardMeterWrapper;
import pt.archifeed.flink.Metrics.LatencyMeter;
import pt.archifeed.flink.model.TransactionModel;
import redis.clients.jedis.Jedis;

/**
 * Mapper to convert, the string to a TransactionModel and enrich the model with the country information.
 * @author viniciuspc
 *
 */
public class EnrichMapper extends RichMapFunction<String, TransactionModel> {

	private static final long serialVersionUID = 2498193892368643718L;
	
	
	private String enrichmentHost;
	
	private transient Meter meter;
	private transient Meter latencyMeter;
	
	public EnrichMapper(String enrichmentHost) {
		this.enrichmentHost = enrichmentHost;
	}
	
	@Override
	public void open(Configuration parameters) throws Exception {
		super.open(parameters);
		MetricGroup group = getRuntimeContext().getMetricGroup().addGroup("Metrics");
		this.meter = group.meter("Throughput", new CustomDropwizardMeterWrapper(new com.codahale.metrics.Meter()));
		this.latencyMeter = group.meter("Latency", new LatencyMeter());
	}

	@Override
	public TransactionModel map(String value) throws Exception {
		this.meter.markEvent();
		this.latencyMeter.markEvent();
		
		Jedis enrichmentData = new Jedis(this.enrichmentHost, 6379);
		String[] fields = value.split(",");
		TransactionModel transaction = new TransactionModel(fields);
		
		if (transaction.getNameOrig() == null || transaction.getNameOrig().length() < 4) {
			transaction.setCountryOrig(enrichmentData.get(transaction.getNameOrig()));
		} else {
			transaction.setCountryOrig(enrichmentData.get(transaction.getNameOrig().substring(0, 4)));
		}

		if (transaction.getNameDest() == null || transaction.getNameDest().length() < 4) {
			transaction.setCountryDest(enrichmentData.get(transaction.getNameDest()));
		} else {
			transaction.setCountryDest(enrichmentData.get(transaction.getNameDest().substring(0, 4)));
		}
		
		
		enrichmentData.close();
		
		return transaction;
	}

}
