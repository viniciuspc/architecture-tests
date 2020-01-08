package pt.archifeed.flink.MapFunctions;

import org.apache.flink.api.common.functions.RichMapFunction;

import pt.archifeed.flink.model.TransactionModel;
import redis.clients.jedis.Jedis;

/**
 * Will apply the bussiness rules.
 * @author viniciuspc
 *
 */
public class RulesMapper extends RichMapFunction<TransactionModel, TransactionModel>{

	private static final long serialVersionUID = 5347713277141066016L;
	
	private String decisionsRulesHost;
	
	public RulesMapper(String decisionsRulesHost) {
		this.decisionsRulesHost = decisionsRulesHost;
	}

	@Override
	public TransactionModel map(TransactionModel transaction) throws Exception {
		
		Jedis decisionRules = new Jedis(this.decisionsRulesHost, 6379);
		
		//If the country of origin or destination is on decision rules then set trasanction as fraud
		if(decisionRules.exists(transaction.getCountryOrig(),transaction.getCountryDest()) > 0 ) {
			transaction.setFraud(true);
		}
		
		decisionRules.close();
		
		return transaction;
	}

}
