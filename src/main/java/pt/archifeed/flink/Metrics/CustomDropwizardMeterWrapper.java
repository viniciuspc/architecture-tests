package pt.archifeed.flink.Metrics;

import org.apache.flink.dropwizard.metrics.DropwizardMeterWrapper;

import com.codahale.metrics.Meter;

public class CustomDropwizardMeterWrapper extends DropwizardMeterWrapper {

	private final com.codahale.metrics.Meter meter;
	
	public CustomDropwizardMeterWrapper(Meter meter) {
		super(meter);
		this.meter = meter;
	}
	
	@Override
	public double getRate() {
		return this.meter.getMeanRate();
	}

}
