package com.example.roadclassificationandroid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class KnnUtilities {
	
	//Input:	fullData is a list of training data. Each array in the list contains all the features; for n features, the data is in n dimensions.
	//Input:	testData is a single data point to classify agains the training data. The features should match up with fullData.
	//Output:	Returns training data sorted by distance from test data. 
	public static ArrayList<DistObj> performKNN(ArrayList<double[]> fullData, double[] testData) {
        int fullDataSize = fullData.size();

        ArrayList<DistObj> distObjects = new ArrayList<DistObj>();

        for (int i = 0; i < fullDataSize; i++) {
            double distance = calculateDistance(testData, fullData.get(i));
            DistObj dobj = new DistObj();
            dobj.index = i;
            dobj.distance = distance;
            distObjects.add(dobj);
        }

        sortDistObjs(distObjects);
        return distObjects;
	}
	
	//Calculates Euclidean distance in n-dimensional space (n is the size of the arrays).
    public static double calculateDistance(double[] array1, double[] array2) {
        double Sum = 0.0;
        for (int i = 0; i < array1.length; i++) {
            Sum = Sum + Math.pow((array1[i] - array2[i]), 2.0);
        }
        return Math.sqrt(Sum);
    }
    
    //Returns training data sorted by distance from test data. 
    //Smaller distances correspond to a more similar classification.
    private static void sortDistObjs(ArrayList<DistObj> distObjects) {
        Collections.sort(distObjects, new Comparator<DistObj>() {
            @Override
            public int compare(DistObj do1, DistObj do2) {
                return Double.compare(do1.distance, do2.distance);
            }
        });
    }
    
	//Return the results of KNN (predicted classification) using k. Print out the percentage of training data that the test data matched up with
	public static String getPredictedClassification(ArrayList<DistObj> distanceObjects, ArrayList<String> trainingClassification, int k) {
		
		HashMap<String,Integer> numOccurances = new HashMap<String,Integer>();
		for (int i = 0; i < k; i++) {
			int index = distanceObjects.get(i).index;
			String classification = trainingClassification.get(index);
			
			Integer count = numOccurances.get(classification);
			numOccurances.put(classification, count==null?1:count+1);
		}
		
		String classification = "";
		int max = 0;
		
		for (String key : numOccurances.keySet()) {
			int val = numOccurances.get(key);
			
			if (val > max) {
    			max = val;
    			classification = key;
    		}
		}
		
		System.out.println(classification + ":\t\t" + max + "/" + k);
		
		return classification;
		
	}
}
