package pt.archifeed.redis;

import redis.clients.jedis.Jedis;

public class Test {
	
	public static void main(String[] args) {
		
		Jedis jedis = new Jedis("172.17.0.3", 6379);
		jedis.set("events/city/rome", "32,15,223,828");
		String cachedResponse = jedis.get("events/city/rome");
		
		System.out.println("Hello world: "+cachedResponse);
		
		jedis.close();
	}

}
