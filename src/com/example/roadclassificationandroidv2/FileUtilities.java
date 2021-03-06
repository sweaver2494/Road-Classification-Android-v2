/* A new feature file is created using files in RawData folder.
 *
 * @author Scott Weaver
 */
package com.example.roadclassificationandroidv2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class FileUtilities {
	public static void readFeatureFile(String featureFilePath, ArrayList<double[]> featureData, ArrayList<String> featureClassification, ArrayList<String> columnHeaders) {
		try {
	        BufferedReader bufferedReader = new BufferedReader(new FileReader(featureFilePath));
	        
	        String line = bufferedReader.readLine();
	        String columnHeadersArray[] = line.substring(line.indexOf(",") + 1).split(",");
	        for (String header : columnHeadersArray) {
	        	columnHeaders.add(header);
	        }
	        
	        line = bufferedReader.readLine();
	        int dataSize = line.length() - line.replace(",", "").length();
	        while (line != null) {
	        	String classification = line.substring(0, line.indexOf(","));
	            String dataCompsStr[] = line.substring(line.indexOf(",") + 1).split(",");
	
	            double dataComps[] = new double[dataSize];
	
	            for (int i = 0; i < dataSize; i++) {
	                dataComps[i] = Double.parseDouble(dataCompsStr[i]);
	            }
	
	            featureData.add(dataComps);
	            featureClassification.add(classification);
	            line = bufferedReader.readLine();
	        }
	        bufferedReader.close();
	        
        } catch(IOException e) {
        	System.err.println("Cannot read feature file.");
        }
	}

	public static void createTestData(String testFilePath, double[] featureData, ArrayList<String> featureHeaders) {
		//values contains the values of each sensor type
		HashMap<String, ArrayList<Double>> values = new HashMap<String, ArrayList<Double>>();
		//rms is the root-means-squared of all values of each sensor type
		//HashMap<String, Double> rms = new HashMap<String, Double>();
		//avg is the average of all values of each sensor type
		HashMap<String, Double> avg = new HashMap<String, Double>();
		//sdv is the standard deviation of all values of each sensor type
		HashMap<String, Double> sdv = new HashMap<String, Double>();

		HashMap<String, Double> maxValues = new HashMap<String, Double>();
		HashMap<String, Double> minValues = new HashMap<String, Double>();

		//Calculate sensor average & variance for each feature, then write to features file		
		File testFile = new File(testFilePath);
		//read sensor data file and list all values for each feature
		readRawData(testFile, values, minValues, maxValues);

		//calculate the average, root-means-squared, and standard deviation of all values for each feature
		calculateStatistics(values, minValues, maxValues, avg, /*rms,*/ sdv);

		//write feature column headers only once
		getFeatureHeaders(featureHeaders, avg, /*rms,*/ sdv);

		//write single row of feature data, one column per feature
		getFeatureData(featureData, avg, /*rms,*/ sdv);
	}

	//Read rawData files and record all values for each feature type
	public static void readRawData(File rawDataFile, HashMap<String, ArrayList<Double>> values, HashMap<String, Double> minValues, HashMap<String, Double> maxValues) {
		try {
			BufferedReader rawDataBuffer = new BufferedReader(new FileReader(rawDataFile));
			String line = rawDataBuffer.readLine();
			
			//read all data from file
			while (line != null) {
				String dataCompsStr[] = line.split(",");
				String key = dataCompsStr[0];
				double val = Double.parseDouble(dataCompsStr[2]);

				if (values.containsKey(key)) {
					ArrayList<Double> temp = values.get(key);
					temp.add(val);

					if (val < minValues.get(key)) {
						minValues.put(key, val);
					}
					if (val > maxValues.get(key)) {
						maxValues.put(key, val);
					}
				} else {
					ArrayList<Double> temp = new ArrayList<Double>();
					temp.add(val);
					values.put(key, temp);

					minValues.put(key, val);
					maxValues.put(key, val);
				}

				line = rawDataBuffer.readLine();
			}
			rawDataBuffer.close();
		} catch (IOException e) {
			System.err.println("Cannot read raw data file: " + rawDataFile.getAbsolutePath());
		}
	}

	//normalize value from 0 to 1
	private static double normalizeValue(double value, double min, double max) {
		double normalized = (value - min) / (max - min);

		return normalized;
	}

	public static void calculateStatistics(HashMap<String, ArrayList<Double>> values, HashMap<String, Double> minValues, HashMap<String, Double> maxValues, HashMap<String, Double> avg, /*HashMap<String, Double> rms,*/ HashMap<String, Double> sdv) {
		for (String key : values.keySet()) {
			//calculate average for each reading (value) for each sensor type (key)
			for (double val : values.get(key)) {

				//normalize val from 0 to 1
				val = normalizeValue(val, minValues.get(key), maxValues.get(key));

				if (avg.containsKey(key)) {
					avg.put(key, avg.get(key) + val);
				} else {
					avg.put(key, val);
				}
			}
			double keyAvg = avg.get(key) / values.get(key).size();
			avg.put(key, keyAvg);

			//calculate root-means-squared for each reading (value) for each sensor type (key)
			//root-means-squared is square root of the average of squared values
			/*for (double val : values.get(key)) {

				//normalize val from 0 to 1
				val = normalizeValue(val, minValues.get(key), maxValues.get(key));

				if (rms.containsKey(key)) {
					rms.put(key, rms.get(key) + Math.pow(val, 2));
				} else {
					rms.put(key, Math.pow(keyAvg, 2));
				}
			}
			double keyRms = rms.get(key) / values.get(key).size();
			keyRms = Math.sqrt(keyRms);
			rms.put(key, keyRms);*/

			//calculate standard deviation for each reading (value) for each sensor type (key)
			//standard deviation is the square root of the average of the squared differences from the mean
			for (double val : values.get(key)) {

				//normalize val from 0 to 1
				val = normalizeValue(val, minValues.get(key), maxValues.get(key));

				if (sdv.containsKey(key)) {
					sdv.put(key, sdv.get(key) + Math.pow(val - keyAvg, 2));
				} else {
					sdv.put(key, Math.pow(val - keyAvg, 2));
				}
			}
			double keySdv = sdv.get(key) / values.get(key).size();
			keySdv = Math.sqrt(keySdv);
			sdv.put(key, keySdv);
		}
	}

	//assumes values exist for sum, avg, and var
	public static void getFeatureHeaders(ArrayList<String> featureHeaders, HashMap<String, Double> avg, HashMap<String, Double> sdv) {
		//Add column headers.
		for (String key : avg.keySet()) {
			String header = key + "_avg";
			featureHeaders.add(header);
		}

		for (String key : sdv.keySet()) {
			String header = key + "_sdv";
			featureHeaders.add(header);
		}
	}

	public static void getFeatureData(double[] featureData, HashMap<String, Double> avg, HashMap<String, Double> sdv) {
		//Add all values for single row.
		int count = 0;
		for (String key : avg.keySet()) {
			double val = avg.get(key);
			featureData[count] = val;
			count++;
		}
		for (String key : sdv.keySet()) {
			double val = sdv.get(key);
			featureData[count] = val;
			count++;
		}
	}
}
