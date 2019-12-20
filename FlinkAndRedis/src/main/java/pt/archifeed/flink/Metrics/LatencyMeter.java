package pt.archifeed.flink.Metrics;

import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.flink.metrics.Meter;

public class LatencyMeter implements Meter{
	
	AtomicLong count = new AtomicLong(0);
	double slowestTime = 0;
	LocalTime initialTime;
	LocalTime finalTime;
	
	public LatencyMeter() {
		this.initialTime = LocalTime.now();
	}
	
	@Override
	public void markEvent() {
		this.markEvent(1);
	}

	@Override
	public void markEvent(long n) {
		this.finalTime = LocalTime.now();
		long millis = Duration.between(this.initialTime, this.finalTime).toMillis()/n;
		if(millis>this.slowestTime) {
			this.slowestTime = millis;
		}
		count.addAndGet(n);
		//Restart count to for next transaction
		this.initialTime = LocalTime.now();
	}

	@Override
	public double getRate() {
		return slowestTime;
	}

	@Override
	public long getCount() {
		return count.get();
	}

}
