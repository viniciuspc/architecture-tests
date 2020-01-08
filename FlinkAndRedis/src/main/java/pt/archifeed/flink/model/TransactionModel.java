package pt.archifeed.flink.model;

import java.io.Serializable;

/**
 * Model of a Transaction to serialize as json.
 * @author viniciuspc
 *
 */
public class TransactionModel implements Serializable {
	
	private static final long serialVersionUID = -1263457772420240789L;
	
	private int step;
	private String type;
	private double amount;
	private String nameOrig;
	private String countryOrig;
	private double oldbalanceOrg;
	private double newbalanceOrig;
	private String nameDest;
	private String countryDest;
	private double oldbalanceDest;
	private double newbalanceDest;
	private boolean fraud;
	
	//No argument constructor for the flink POJO convention
	public TransactionModel() {
		super();
	}
	
	public TransactionModel(String[] fields) {
		super();
		this.step = Integer.valueOf(fields[0]);
		this.type = fields[1];
		this.amount = Double.valueOf(fields[2]);
		this.nameOrig = fields[3];
		this.oldbalanceOrg = Double.valueOf(fields[4]);
		this.newbalanceOrig = Double.valueOf(fields[5]);
		this.nameDest = fields[6];
		this.oldbalanceDest = Double.valueOf(fields[7]);
		this.newbalanceDest = Double.valueOf(fields[8]);
	}
	
	public TransactionModel(int step, String type, double amount, String nameOrig, String countryOrig, double oldbalanceOrg,
			double newbalanceOrig, String nameDest, String countryDest, double oldbalanceDest, double newbalanceDest,
			boolean isFraud) {
		super();
		this.step = step;
		this.type = type;
		this.amount = amount;
		this.nameOrig = nameOrig;
		this.oldbalanceOrg = oldbalanceOrg;
		this.newbalanceOrig = newbalanceOrig;
		this.nameDest = nameDest;
		this.oldbalanceDest = oldbalanceDest;
		this.newbalanceDest = newbalanceDest;
	}



	public int getStep() {
		return step;
	}



	public void setStep(int step) {
		this.step = step;
	}



	public String getType() {
		return type;
	}



	public void setType(String type) {
		this.type = type;
	}



	public double getAmount() {
		return amount;
	}



	public void setAmount(double amount) {
		this.amount = amount;
	}



	public String getNameOrig() {
		return nameOrig;
	}



	public void setNameOrig(String nameOrig) {
		this.nameOrig = nameOrig;
	}



	public String getCountryOrig() {
		return countryOrig;
	}



	public void setCountryOrig(String countryOrig) {
		this.countryOrig = countryOrig;
	}



	public double getOldbalanceOrg() {
		return oldbalanceOrg;
	}



	public void setOldbalanceOrg(double oldbalanceOrg) {
		this.oldbalanceOrg = oldbalanceOrg;
	}



	public double getNewbalanceOrig() {
		return newbalanceOrig;
	}



	public void setNewbalanceOrig(double newbalanceOrig) {
		this.newbalanceOrig = newbalanceOrig;
	}



	public String getNameDest() {
		return nameDest;
	}



	public void setNameDest(String nameDest) {
		this.nameDest = nameDest;
	}



	public String getCountryDest() {
		return countryDest;
	}



	public void setCountryDest(String countryDest) {
		this.countryDest = countryDest;
	}



	public double getOldbalanceDest() {
		return oldbalanceDest;
	}



	public void setOldbalanceDest(double oldbalanceDest) {
		this.oldbalanceDest = oldbalanceDest;
	}



	public double getNewbalanceDest() {
		return newbalanceDest;
	}



	public void setNewbalanceDest(double newbalanceDest) {
		this.newbalanceDest = newbalanceDest;
	}



	public boolean isFraud() {
		return fraud;
	}



	public void setFraud(boolean isFraud) {
		this.fraud = isFraud;
	}
	
	
}
