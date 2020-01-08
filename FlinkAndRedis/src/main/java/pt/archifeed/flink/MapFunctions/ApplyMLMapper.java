package pt.archifeed.flink.MapFunctions;

import java.util.Vector;

import org.apache.flink.api.common.functions.RichMapFunction;

import pt.archifeed.flink.model.TransactionModel;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

/**
 * Mapper to classify a TransactionModel on the givem Machine Learning model
 * @author viniciuspc
 *
 */
public class ApplyMLMapper extends RichMapFunction<TransactionModel, TransactionModel>{
	
	/**
	 * Vector contaning the machine learning model and the header with the attributes.
	 */
	Vector v ;
	
	
	public ApplyMLMapper(Vector v) {
		this.v = v;
	}
	
	@Override
	public TransactionModel map(TransactionModel value) throws Exception {
		
		//Apply the machine learning model only if the transaction model is not marked as fraud (by the rules) already .
		if(!value.isFraud()) {
			Classifier c = (Classifier) this.v.get(0);
			Instances header = (Instances) this.v.get(1);
			
			Instance inst = new DenseInstance(header.numAttributes());
			inst.setDataset(header);
			inst.setValue(header.attribute("step"), value.getStep());
			inst.setValue(header.attribute("type"), value.getType());
			inst.setValue(header.attribute("amount"), value.getAmount());
			inst.setValue(header.attribute("nameOrig"), value.getNameOrig());
			inst.setValue(header.attribute("oldbalanceOrg"), value.getOldbalanceOrg());
			inst.setValue(header.attribute("newbalanceOrig"), value.getNewbalanceOrig());
			inst.setValue(header.attribute("nameDest"), value.getNameDest());
			inst.setValue(header.attribute("oldbalanceDest"), value.getOldbalanceDest());
			inst.setValue(header.attribute("newbalanceDest"), value.getNewbalanceDest());
			
			double classifyInstance = c.classifyInstance(inst);
			
			if(classifyInstance == 1) {
				value.setFraud(true);
			}
			inst = null;
		}
		
		return value;
	}

}
