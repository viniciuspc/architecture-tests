package pt.archifeed.ml;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToBinary;
import weka.filters.unsupervised.instance.RemovePercentage;

public class Test {

	public static void main(String[] args) throws Exception {
		Instances data = prepareDataset();
		trainModel(data);
		
		
		
	}
	
	public static Instances prepareDataset() throws Exception {
		//Load a file with the transactions to train the model.
		DataSource source = new DataSource("datasources/paysim500k.csv");
		Instances data = source.getDataSet();
		//Remove the isFlaged as Instance
		data.deleteAttributeAt(10);
		
		//Options to convert the isFraud colunm from numeric to binary
		String[] options = new String[2];
		options[0] = "-R";
		options[1] = "10";
		
		//Convert the isFraud colunm to binary
		NumericToBinary numToB = new NumericToBinary();
		numToB.setOptions(options);
		numToB.setInputFormat(data);
		Instances newData = Filter.useFilter(data, numToB);
		
		newData.setClassIndex(9);
		
		return newData;
	}
	
	public static void trainModel(Instances data) throws Exception {
		RemovePercentage rp = new RemovePercentage();
		rp.setInputFormat(data);
		//Prepare TrainingData - Remove 20% of the data, so the trainingData will keep 80% of data
		rp.setPercentage(20.0);
		Instances trainingData = Filter.useFilter(data, rp);
		System.out.println("Training Size: "+trainingData.size());
		
		//Prepare TestData - Use the 20% remove previously.
		rp.setInputFormat(data);
		rp.setInvertSelection(true);
		Instances testData = Filter.useFilter(data, rp);
		System.out.println("Test Size: "+testData.size());
		
		//Classifier cs = new NaiveBayes();
		Classifier cs = new RandomForest();
		
		cs.buildClassifier(trainingData);
		
		System.out.println("Training Finished...");
		
		Evaluation eval = new Evaluation(trainingData);
		eval.evaluateModel(cs, testData);
		System.out.println(eval.toSummaryString("\nResults\n======\n", false));
		double[][] confusionMatrix = eval.confusionMatrix();
		
		System.out.println(eval.toMatrixString());
		
		
	}

}
