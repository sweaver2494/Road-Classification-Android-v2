/*
 *
 * @author Scott Weaver
 */
package com.example.roadclassificationandroid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

public class DataCollection implements SensorEventListener {

	// Audio Constants
	private static int[] mSampleRates = { 44100, 22050, 11025, 8000 };
	private int AUDIO_FORMAT = 0;
	private int AUDIO_SOURCE = 0;
	private int BUFFER_SIZE = 0;
	private int CHANNEL_IN_CONFIG = 0;
	private int SAMPLING_RATE = 0;

	// Miscellaneous Audio Variables
	private String audioFilePath = "";
	private Thread audioThread = null;
	private AudioRecord audiorec = null;
	private boolean isRecording = false;

	// Sensor variables
	private final SensorManager sensorManager;
	private Sensor gyroscope = null;
	private Sensor accelerom = null;

	// Timing Variables
	private boolean isStarted = false;
	private long startTime = 0L;

	// File Handling
	private BufferedWriter sensorbw = null;
	private BufferedWriter audiobw = null;
	private BufferedWriter testDataBw = null;

	private File dcimDir = null;
	private String classification = "";
	private String testDataDir = "";
	private String sensorFilePath = "";
	private String testDataPath = "";
	private String featureDataPath = "";
	
	private ArrayList<double[]> trainingData;
	private ArrayList<String> trainingClassification;
	private ArrayList<String> trainingColumnHeaders;
	
	int k_value = 2;

	private Activity currentActivity;

	public DataCollection(Activity paramActivity) {
		currentActivity = paramActivity;
		sensorManager = (SensorManager) currentActivity.getSystemService(Context.SENSOR_SERVICE);
		gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		accelerom = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
	}
	
	/*public AudioRecord findAudioRecord() {
		AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
	    for (int rate : mSampleRates) {
	        for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
	            for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
	                try {
	                    System.out.println("Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
	                    int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

	                    if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
	                        // check if we can instantiate and have a success
	                        AudioRecord recorder = new AudioRecord(AUDIO_SOURCE, rate, channelConfig, audioFormat, bufferSize);

	                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
	                        	SAMPLING_RATE = rate;
	                        	CHANNEL_IN_CONFIG = channelConfig;
	                        	AUDIO_FORMAT = audioFormat;
	                        	BUFFER_SIZE = bufferSize;
	                            return recorder;
	                        }
	                    }
	                } catch (Exception e) {
	                    System.out.println("Uninitialized, trying again...");
	                }
	            }
	        }
	    }
	    return null;
	}*/

	private AudioRecord getAudioRecord() {
		try {
			AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT; // 0
			SAMPLING_RATE = mSampleRates[0]; // 44100
			CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO; // 16
			AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // 2
			BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
			AudioRecord audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
			
			if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
				System.out.println("Audio Record Initialized");
				return audioRecord;
			}
		} catch (Exception e) {
			System.err.println("Could not get audio recorder. " + e.toString());
		}
		return null;
	}

	private void sampleAudioData() {
		int numShorts = BUFFER_SIZE / 2;
		short[] audioArray = new short[numShorts];
		try {
			while (isRecording) {
				long endTime = System.nanoTime();
				long tempStartTime = startTime;
				audiorec.read(audioArray, 0, numShorts);
	
				int audioArrayLength = audioArray.length;
				for (int i = 0; i < audioArrayLength; i++) {
					int audioReading = audioArray[i];
					audiobw.write("audio,"
							+ String.valueOf(endTime - tempStartTime) + ","
							+ String.valueOf(audioReading));
					audiobw.newLine();
				}
				audiobw.flush();
			}
		} catch (IOException e) {
			System.err.println("Cannot write audio data file. " + e.toString());
		}
	}

	public boolean StartCollection() {
		boolean success = true;

		if (!isStarted) {
			testDataDir = (dcimDir.getAbsolutePath() + "/Classification/");
			(new File(testDataDir)).mkdirs();

			sensorFilePath = (testDataDir + "test_sensor.csv");
			audioFilePath = (testDataDir + "test_audio.csv");
			testDataPath = (testDataDir + "test.csv");
			featureDataPath = (testDataDir + "test_features.csv");

			System.out.println("Sensor Path " + sensorFilePath);
			System.out.println("Audio Path " + audioFilePath);
			System.out.println("Test Data Path " + testDataPath);
			System.out.println("Test Feature Path " + featureDataPath);

			if (success) {
				try {
					sensorbw = new BufferedWriter(new FileWriter(sensorFilePath, false));
					audiobw = new BufferedWriter(new FileWriter(audioFilePath, false));
					testDataBw = new BufferedWriter(new FileWriter(testDataPath, false));
				} catch (IOException e) {
					System.err.println("Could not open data file. " + e.toString());
					success = false;
				}
			}

			startTime = System.nanoTime();

			if (success) {
				success = sensorManager.registerListener(this, gyroscope,SensorManager.SENSOR_DELAY_FASTEST);
			}

			if (success) {
				success = sensorManager.registerListener(this, accelerom,SensorManager.SENSOR_DELAY_FASTEST);
			}

			if (success) {
				audiorec = getAudioRecord();
				if (audiorec != null) {
					audiorec.startRecording();
					isRecording = true;
					audioThread = new Thread(new Runnable() {
						public void run() {
							sampleAudioData();
						}
					}, "AudioRecorder Thread");
					audioThread.start();
					isStarted = true;
				} else {
					System.out.println("Could not find audio recorder.");
					success = false;
				}
			}
		}
		return success;
	}

	public boolean StopCollection() {
		boolean success = false;
		if (isStarted) {
			if (gyroscope != null) {
				sensorManager.unregisterListener(this, gyroscope);
			}
			if (accelerom != null) {
				sensorManager.unregisterListener(this, accelerom);
			}
			if (audiorec != null) {
				isRecording = false;
				audiorec.stop();
				audiorec.release();
			}
			
			try {
				audioThread.join();
				audioThread = null;
				
				BufferedReader sensorbr = new BufferedReader(new InputStreamReader(new FileInputStream(sensorFilePath)));
				BufferedReader audiobr = new BufferedReader(new InputStreamReader(new FileInputStream(audioFilePath)));
				testDataBw.write("unkown");
				testDataBw.newLine();
				testDataBw.flush();

				String line = sensorbr.readLine();
				while (line != null) {
					testDataBw.write(line);
					testDataBw.newLine();

					line = sensorbr.readLine();
				}
				testDataBw.flush();

				line = audiobr.readLine();
				while (line != null) {
					testDataBw.write(line);
					testDataBw.newLine();

					line = audiobr.readLine();
				}
				testDataBw.flush();

				sensorbr.close();
				audiobr.close();
				if ((new File(testDataPath)).isFile()) {
					(new File(sensorFilePath)).delete();
					(new File(audioFilePath)).delete();
				}

				isStarted = false;

				predictClassification();
			} catch (IOException e) {
				System.err.println("Cannot write data file. " + e.toString());
			} catch (InterruptedException e) {
				System.err.println("Could not join audio thread. " + e.toString());
			} finally {
				try {
					sensorbw.close();
					audiobw.close();
					testDataBw.close();
				} catch (IOException e2) {
					System.err.println("Cannot close data file. " + e2.toString());
				}
			}
		}

		return success;
	}

	public String getClassification() {
		return classification;
	}
	
	public void setTrainingData(ArrayList<double[]> data, ArrayList<String> classif, ArrayList<String> colHeaders) {
		trainingData = data;
		trainingClassification = classif;
		trainingColumnHeaders = colHeaders;
	}
	
	private String predictClassification() {
		
		ArrayList<double[]> testData = new ArrayList<double[]>(1);
		ArrayList<String> testClassification = new ArrayList<String>();
		ArrayList<String> testColumnHeaders = new ArrayList<String>();
		
		//Convert raw data to feature file
		FileUtilities.createTestData(testDataPath, featureDataPath);
		//Extract features from feature file
		FileUtilities.readFeatureFile(featureDataPath, testData, testClassification, testColumnHeaders);
		
		//double[] orderedTestDataArr = new double[testData.get(0).length];
		ArrayList<double[]> orderedTestData = new ArrayList<double[]>(1);
		
		for (String header : trainingColumnHeaders) {
			for (int i = 0; i < testColumnHeaders.size(); i++) {
				if (testColumnHeaders.get(i).equals(header)) {
					orderedTestData.add(testData.get(i));
				}
			}
		}
		
		ArrayList<DistObj> distObjects = KnnUtilities.performKNN(trainingData, orderedTestData.get(0));
		classification = KnnUtilities.getPredictedClassification(distObjects, trainingClassification, k_value);
		
		return classification;
	}

	public void onAccuracyChanged(Sensor paramSensor, int paramInt) {
	}

	public void onSensorChanged(SensorEvent sensorEvent) {
		String sensor = sensorEvent.sensor.getStringType();
		long elapsedTime = System.nanoTime() - startTime;
		try {
			sensorbw.write(sensor.substring(sensor.lastIndexOf(".") + 1)
					+ "_x," + String.valueOf(elapsedTime) + ","
					+ String.valueOf(sensorEvent.values[0]));
			sensorbw.newLine();
			sensorbw.write(sensor.substring(sensor.lastIndexOf(".") + 1)
					+ "_y," + String.valueOf(elapsedTime) + ","
					+ String.valueOf(sensorEvent.values[1]));
			sensorbw.newLine();
			sensorbw.write(sensor.substring(sensor.lastIndexOf(".") + 1)
					+ "_z," + String.valueOf(elapsedTime) + ","
					+ String.valueOf(sensorEvent.values[2]));
			sensorbw.newLine();
			sensorbw.flush();
			return;
		} catch (IOException e) {
			System.err.println("Cannot write sensor data file. " + e.toString());
		}
	}
}
