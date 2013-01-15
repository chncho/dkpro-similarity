package de.tudarmstadt.ukp.similarity.experiments.rte.util;

import static de.tudarmstadt.ukp.similarity.experiments.rte.Pipeline.DATASET_DIR;
import static de.tudarmstadt.ukp.similarity.experiments.rte.Pipeline.MODELS_DIR;
import static de.tudarmstadt.ukp.similarity.experiments.rte.Pipeline.OUTPUT_DIR;
import static de.tudarmstadt.ukp.similarity.experiments.rte.Pipeline.GOLD_DIR;
import static de.tudarmstadt.ukp.similarity.experiments.rte.Pipeline.EvaluationMetric.*;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitive;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;
import static org.uimafit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.pipeline.SimplePipeline;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.output.prediction.AbstractOutput;
import weka.classifiers.evaluation.output.prediction.PlainText;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AddClassification;
import weka.filters.unsupervised.attribute.AddID;
import weka.filters.unsupervised.attribute.Remove;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Document;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.similarity.algorithms.ml.ClassifierSimilarityMeasure;
import de.tudarmstadt.ukp.similarity.algorithms.ml.ClassifierSimilarityMeasure.WekaClassifier;
import de.tudarmstadt.ukp.similarity.dkpro.annotator.SimilarityScorer;
import de.tudarmstadt.ukp.similarity.dkpro.io.CombinationReader;
import de.tudarmstadt.ukp.similarity.dkpro.io.CombinationReader.CombinationStrategy;
import de.tudarmstadt.ukp.similarity.dkpro.io.RTECorpusReader;
import de.tudarmstadt.ukp.similarity.dkpro.io.SemEvalCorpusReader;
import de.tudarmstadt.ukp.similarity.dkpro.resource.ml.ClassifierResource;
import de.tudarmstadt.ukp.similarity.dkpro.resource.ml.LinearRegressionResource;
import de.tudarmstadt.ukp.similarity.ml.filters.LogFilter;
import de.tudarmstadt.ukp.similarity.ml.io.SimilarityScoreWriter;
import de.tudarmstadt.ukp.similarity.experiments.rte.Pipeline.Dataset;
//import de.tudarmstadt.ukp.similarity.experiments.rte.Pipeline.EvaluationMetric;
//import de.tudarmstadt.ukp.similarity.experiments.rte.Pipeline.Mode;
//import de.tudarmstadt.ukp.similarity.experiments.rte.filter.LogFilter;
import de.tudarmstadt.ukp.similarity.experiments.rte.Pipeline.EvaluationMetric;


public class Evaluator
{
	public static final String LF = System.getProperty("line.separator");
	
	private static final WekaClassifier wekaClassifier = WekaClassifier.SMO;
	
//	public static void runClassifier(Dataset train, Dataset test)
//		throws UIMAException, IOException
//	{
//		CollectionReader reader = createCollectionReader(
//				RTECorpusReader.class,
//				RTECorpusReader.PARAM_INPUT_FILE, RteUtil.getInputFilePathForDataset(DATASET_DIR, test),
//				RTECorpusReader.PARAM_COMBINATION_STRATEGY, CombinationStrategy.SAME_ROW_ONLY.toString());
//		
//		AnalysisEngineDescription seg = createPrimitiveDescription(
//				BreakIteratorSegmenter.class);
//		
//		AggregateBuilder builder = new AggregateBuilder();
//		builder.add(seg, CombinationReader.INITIAL_VIEW, CombinationReader.VIEW_1);
//		builder.add(seg, CombinationReader.INITIAL_VIEW, CombinationReader.VIEW_2);
//		AnalysisEngine aggr_seg = builder.createAggregate();
//
//		AnalysisEngine scorer = createPrimitive(
//				SimilarityScorer.class,
//			    SimilarityScorer.PARAM_NAME_VIEW_1, CombinationReader.VIEW_1,
//			    SimilarityScorer.PARAM_NAME_VIEW_2, CombinationReader.VIEW_2,
//			    SimilarityScorer.PARAM_SEGMENT_FEATURE_PATH, Document.class.getName(),
//			    SimilarityScorer.PARAM_TEXT_SIMILARITY_RESOURCE, createExternalResourceDescription(
//			    	ClassifierResource.class,
//			    	ClassifierResource.PARAM_CLASSIFIER, wekaClassifier.toString(),
//			    	ClassifierResource.PARAM_TRAIN_ARFF, MODELS_DIR + "/" + train.toString() + ".arff",
//			    	ClassifierResource.PARAM_TEST_ARFF, MODELS_DIR + "/" + test.toString() + ".arff")
//			    );
//		
//		AnalysisEngine writer = createPrimitive(
//				SimilarityScoreWriter.class,
//				SimilarityScoreWriter.PARAM_OUTPUT_FILE, OUTPUT_DIR + "/" + test.toString() + ".csv",
//				SimilarityScoreWriter.PARAM_OUTPUT_SCORES_ONLY, true,
//				SimilarityScoreWriter.PARAM_OUTPUT_GOLD_SCORES, false);
//
//		SimplePipeline.runPipeline(reader, aggr_seg, scorer, writer);
//	}
	
	public static void runClassifier(Dataset trainDataset, Dataset testDataset)
			throws Exception
	{
		Classifier baseClassifier = ClassifierSimilarityMeasure.getClassifier(wekaClassifier);
		
		// Set up the random number generator
    	long seed = new Date().getTime();			
		Random random = new Random(seed);	
				
		// Add IDs to the train instances and get the instances
		AddID.main(new String[] {"-i", MODELS_DIR + "/" + trainDataset.toString() + ".arff",
							 	 "-o", MODELS_DIR + "/" + trainDataset.toString() + "-plusIDs.arff" });
		Instances train = DataSource.read(MODELS_DIR + "/" + trainDataset.toString() + "-plusIDs.arff");
		train.setClassIndex(train.numAttributes() - 1);	
		
		// Add IDs to the test instances and get the instances
		AddID.main(new String[] {"-i", MODELS_DIR + "/" + testDataset.toString() + ".arff",
							 	 "-o", MODELS_DIR + "/" + testDataset.toString() + "-plusIDs.arff" });
		Instances test = DataSource.read(MODELS_DIR + "/" + testDataset.toString() + "-plusIDs.arff");
		test.setClassIndex(test.numAttributes() - 1);		
		
		// Instantiate the Remove filter
        Remove removeIDFilter = new Remove();
    	removeIDFilter.setAttributeIndices("first");
				
		// Randomize the data
		test.randomize(random);
		
		// Apply log filter
//	    Filter logFilter = new LogFilter();
//	    logFilter.setInputFormat(train);
//	    train = Filter.useFilter(train, logFilter);        
//	    logFilter.setInputFormat(test);
//	    test = Filter.useFilter(test, logFilter);
        
        // Copy the classifier
        Classifier classifier = AbstractClassifier.makeCopy(baseClassifier);
        	
        // Instantiate the FilteredClassifier
        FilteredClassifier filteredClassifier = new FilteredClassifier();
        filteredClassifier.setFilter(removeIDFilter);
        filteredClassifier.setClassifier(classifier);
        	 
        // Build the classifier
        filteredClassifier.buildClassifier(train);
		
        // Prepare the output buffer 
        AbstractOutput output = new PlainText();
        output.setBuffer(new StringBuffer());
        output.setHeader(test);
        output.setAttributes("first");
        
		Evaluation eval = new Evaluation(train);
        eval.evaluateModel(filteredClassifier, test, output);
        
        // Convert predictions to CSV
        // Format: inst#, actual, predicted, error, probability, (ID)
        String[] scores = new String[new Double(eval.numInstances()).intValue()];
        double[] probabilities = new double[new Double(eval.numInstances()).intValue()];
        for (String line : output.getBuffer().toString().split("\n"))
        {
        	String[] linesplit = line.split("\\s+");

        	// If there's been an error, the length of linesplit is 6, otherwise 5,
        	// due to the error flag "+"
        	
        	int id;
        	String expectedValue, classification;
        	double probability;
        	
        	if (line.contains("+"))
        	{
        	   	id = Integer.parseInt(linesplit[6].substring(1, linesplit[6].length() - 1));
	        	expectedValue = linesplit[2].substring(2);
	        	classification = linesplit[3].substring(2);
	        	probability = Double.parseDouble(linesplit[5]);
        	} else {
        		id = Integer.parseInt(linesplit[5].substring(1, linesplit[5].length() - 1));
	        	expectedValue = linesplit[2].substring(2);
	        	classification = linesplit[3].substring(2);
	        	probability = Double.parseDouble(linesplit[4]);
        	}
        	
        	scores[id - 1] = classification;
        	probabilities[id - 1] = probability;
        }
                
        System.out.println(eval.toSummaryString());
	    System.out.println(eval.toMatrixString());
	    
	    // Output classifications
	    StringBuilder sb = new StringBuilder();
	    for (String score : scores)
	    	sb.append(score.toString() + LF);
	    
	    FileUtils.writeStringToFile(
	    	new File(OUTPUT_DIR + "/" + testDataset.toString() + ".csv"),
	    	sb.toString());
	    
	    // Output probabilities
	    sb = new StringBuilder();
	    for (Double probability : probabilities)
	    	sb.append(probability.toString() + LF);
	    
	    FileUtils.writeStringToFile(
	    	new File(OUTPUT_DIR + "/" + testDataset.toString() + ".probabilities.csv"),
	    	sb.toString());
	    
	    // Output predictions
	    FileUtils.writeStringToFile(
	    	new File(OUTPUT_DIR + "/" + testDataset.toString() + ".predictions.txt"),
	    	output.getBuffer().toString());
	    
	    // Output meta information
	    sb = new StringBuilder();
	    sb.append(classifier.toString() + LF);
	    sb.append(eval.toSummaryString() + LF);
	    sb.append(eval.toMatrixString() + LF);
	    
	    FileUtils.writeStringToFile(
	    	new File(OUTPUT_DIR + "/" + testDataset.toString() + ".meta.txt"),
	    	sb.toString());
	}
	
	public static void runClassifierCV(Dataset dataset)
		throws Exception
	{
		// Set parameters
		int folds = 10;
		Classifier baseClassifier = ClassifierSimilarityMeasure.getClassifier(wekaClassifier);
		
		// Set up the random number generator
    	long seed = new Date().getTime();			
		Random random = new Random(seed);	
    	
		// Add IDs to the instances
		AddID.main(new String[] {"-i", MODELS_DIR + "/" + dataset.toString() + ".arff",
							 	 "-o", MODELS_DIR + "/" + dataset.toString() + "-plusIDs.arff" });
		Instances data = DataSource.read(MODELS_DIR + "/" + dataset.toString() + "-plusIDs.arff");
		data.setClassIndex(data.numAttributes() - 1);				
		
        // Instantiate the Remove filter
        Remove removeIDFilter = new Remove();
    	removeIDFilter.setAttributeIndices("first");
				
		// Randomize the data
		data.randomize(random);
	
		// Perform cross-validation
	    Instances predictedData = null;
	    Evaluation eval = new Evaluation(data);
	    
	    for (int n = 0; n < folds; n++)
	    {
	    	Instances train = data.trainCV(folds, n, random);
	        Instances test = data.testCV(folds, n);
	        
	        // Apply log filter
//		    Filter logFilter = new LogFilter();
//	        logFilter.setInputFormat(train);
//	        train = Filter.useFilter(train, logFilter);        
//	        logFilter.setInputFormat(test);
//	        test = Filter.useFilter(test, logFilter);
	        
	        // Copy the classifier
	        Classifier classifier = AbstractClassifier.makeCopy(baseClassifier);
	        	         		        
	        // Instantiate the FilteredClassifier
	        FilteredClassifier filteredClassifier = new FilteredClassifier();
	        filteredClassifier.setFilter(removeIDFilter);
	        filteredClassifier.setClassifier(classifier);
	        	 
	        // Build the classifier
	        filteredClassifier.buildClassifier(train);
	        
	        // Evaluate
	        eval.evaluateModel(filteredClassifier, test);
	        
	        // Add predictions
	        AddClassification filter = new AddClassification();
	        filter.setClassifier(classifier);
	        filter.setOutputClassification(true);
	        filter.setOutputDistribution(false);
	        filter.setOutputErrorFlag(true);
	        filter.setInputFormat(train);
	        Filter.useFilter(train, filter);  // trains the classifier
	        
	        Instances pred = Filter.useFilter(test, filter);  // performs predictions on test set
	        if (predictedData == null)
	        	predictedData = new Instances(pred, 0);
	        for (int j = 0; j < pred.numInstances(); j++)
	        	predictedData.add(pred.instance(j));		        
	    }
	    
	    System.out.println(eval.toSummaryString());
	    System.out.println(eval.toMatrixString());
	    
	    // Prepare output scores
	    String[] scores = new String[predictedData.numInstances()];
	    
	    for (Instance predInst : predictedData)
	    {
	    	int id = new Double(predInst.value(predInst.attribute(0))).intValue() - 1;
	    	
	    	int valueIdx = predictedData.numAttributes() - 2;
	    	
	    	String value = predInst.stringValue(predInst.attribute(valueIdx));
	    	
	    	scores[id] = value;
	    }
	    
	    // Output classifications
	    StringBuilder sb = new StringBuilder();
	    for (String score : scores)
	    	sb.append(score.toString() + LF);
	    
	    FileUtils.writeStringToFile(
	    	new File(OUTPUT_DIR + "/" + dataset.toString() + ".csv"),
	    	sb.toString());
	    
	    // Output prediction arff
	    DataSink.write(
	    	OUTPUT_DIR + "/" + dataset.toString() + ".predicted.arff",
	    	predictedData);
	    
	    // Output meta information
	    sb = new StringBuilder();
	    sb.append(baseClassifier.toString() + LF);
	    sb.append(eval.toSummaryString() + LF);
	    sb.append(eval.toMatrixString() + LF);
	    
	    FileUtils.writeStringToFile(
	    	new File(OUTPUT_DIR + "/" + dataset.toString() + ".meta.txt"),
	    	sb.toString());
	}

	@SuppressWarnings("unchecked")
	public static void runEvaluationMetric(EvaluationMetric metric, Dataset dataset)
		throws IOException
	{
		StringBuilder sb = new StringBuilder();
			
		if (metric == Accuracy)
		{
			// Read gold scores
			List<String> goldScores = FileUtils.readLines(new File(GOLD_DIR + "/" + dataset.toString() + ".txt"));
						
			// Read the experimental scores
			List<String> expScores = FileUtils.readLines(new File(OUTPUT_DIR + "/" + dataset.toString() + ".csv"));
						
			// Compute the accuracy
			double acc = 0.0;
			for (int i = 0; i < goldScores.size(); i++)
			{
				if (goldScores.get(i).equals(expScores.get(i)))
					acc++;
			}
			acc = acc / goldScores.size();
			
			sb.append(acc);
		}
		if (metric == CWS)
		{
			// Read gold scores
			List<String> goldScores = FileUtils.readLines(new File(GOLD_DIR + "/" + dataset.toString() + ".txt"));
						
			// Read the experimental scores
			List<String> expScores = FileUtils.readLines(new File(OUTPUT_DIR + "/" + dataset.toString() + ".csv"));
			
			// Read the confidence scores
			List<String> probabilities = FileUtils.readLines(new File(OUTPUT_DIR + "/" + dataset.toString() + ".probabilities.csv"));
			
			// Combine the data
			List<CwsData> data = new ArrayList<CwsData>();
			
			for (int i = 0; i < goldScores.size(); i++)
			{
				CwsData cws = (new Evaluator()).new CwsData(
						Double.parseDouble(probabilities.get(i)),
						goldScores.get(i),
						expScores.get(i));
				data.add(cws);
			}
			
			// Sort in descending order
			Collections.sort(data, Collections.reverseOrder());
			
			// Compute the CWS score
			double cwsScore = 0.0;
			for (int i = 0; i < data.size(); i++)
			{
				double cws_sub = 0.0;
				for (int j = 0; j <= i; j++)
				{
					if (data.get(j).isCorrect())
						cws_sub++;
				}
				cws_sub /= (i+1);
				
				cwsScore += cws_sub;
			}
			cwsScore /= data.size();
						
			sb.append(cwsScore);
		}

		FileUtils.writeStringToFile(new File(OUTPUT_DIR + "/" + dataset.toString() + "_" + metric.toString() + ".txt"), sb.toString());
		
		System.out.println(metric.toString() + ": " + sb.toString());
	}
	
	private class CwsData
		implements Comparable
	{
		private double confidence;
		private String goldScore;
		private String expScore;
		
		public CwsData(double confidence, String goldScore, String expScore)
		{
			this.confidence = confidence;
			this.goldScore = goldScore;
			this.expScore = expScore;
		}
		
		public boolean isCorrect()
		{
			return goldScore.equals(expScore);
		}

		public int compareTo(Object other)
		{
			CwsData otherObj = (CwsData)other;
			
			if (this.getConfidence() == otherObj.getConfidence()) {
				return 0;
			} else if (this.getConfidence() > otherObj.getConfidence()) {
				return 1;
			} else {
				return -1;
			}
		}

		public double getConfidence()
		{
			return confidence;
		}

		public String getGoldScore()
		{
			return goldScore;
		}

		public String getExpScore()
		{
			return expScore;
		}
	}
	
//	
//	@SuppressWarnings("unchecked")
//	private static void computePearsonCorrelation(Mode mode, Dataset dataset)
//		throws IOException
//	{
//		File expScoresFile = new File(OUTPUT_DIR + "/" + mode.toString().toLowerCase() + "/" + dataset.toString() + ".csv");
//		
//		String gsScoresFilePath = GOLDSTANDARD_DIR + "/" + mode.toString().toLowerCase() + "/" + 
//				"STS.gs." + dataset.toString() + ".txt";
//		
//		PathMatchingResourcePatternResolver r = new PathMatchingResourcePatternResolver();
//        Resource res = r.getResource(gsScoresFilePath);				
//		File gsScoresFile = res.getFile();
//		
//		List<Double> expScores = new ArrayList<Double>();
//		List<Double> gsScores = new ArrayList<Double>();
//		
//		List<String> expLines = FileUtils.readLines(expScoresFile);
//		List<String> gsLines = FileUtils.readLines(gsScoresFile);
//		
//		for (int i = 0; i < expLines.size(); i++)
//		{
//			expScores.add(Double.parseDouble(expLines.get(i)));
//			gsScores.add(Double.parseDouble(gsLines.get(i)));
//		}
//		
//		double[] expArray = ArrayUtils.toPrimitive(expScores.toArray(new Double[expScores.size()])); 
//		double[] gsArray = ArrayUtils.toPrimitive(gsScores.toArray(new Double[gsScores.size()]));
//
//		PearsonsCorrelation pearson = new PearsonsCorrelation();
//		Double correl = pearson.correlation(expArray, gsArray);
//		
//		FileUtils.writeStringToFile(
//				new File(OUTPUT_DIR + "/" + mode.toString().toLowerCase() + "/" + dataset.toString() + ".txt"),
//				correl.toString());
//	}
}
