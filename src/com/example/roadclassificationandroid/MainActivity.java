/*
 *
 * @author Scott Weaver
 */
package com.example.roadclassificationandroid;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.Spinner;

public class MainActivity extends Activity {

	String condition = "";
	String trainingFileName = "";
	boolean stopped = false;
	long timeInterval = (long) 0.5;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final DataCollection collection = new DataCollection(this);
		
		String trainingDataDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/Classification/";
		final HashMap<String, String> fileNameMap = new HashMap<String, String>();
		fileNameMap.put("paved_vs_unpaved", trainingDataDirPath + "training_data_30.csv");
		fileNameMap.put("30mph_vs_60mph", trainingDataDirPath + "training_data_paved.csv");

		//CONDITION DROP DOWN
		ArrayList<String> conditions = new ArrayList<String>();
		conditions.add("paved_vs_unpaved");
		conditions.add("30mph_vs_60mph");
		ArrayAdapter<String> conditionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, conditions);
		conditionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner conditionSpinner = (Spinner) findViewById(R.id.conditionSpinner);
		conditionSpinner.setAdapter(conditionAdapter);
		conditionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				condition = parent.getItemAtPosition(pos).toString();
				trainingFileName = fileNameMap.get(condition);
			}
			
			public void onNothingSelected(AdapterView<?> parent) {
				condition = "";
			}
		});
		
		//STOP BUTTON
		((Button) findViewById(R.id.stopButton)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				collection.StopCollection();
				stopped = true;
				((Chronometer) findViewById(R.id.chronometer)).stop();
			}
		});
		
		//START BUTTON
		((Button) findViewById(R.id.startButton)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View paramAnonymousView) {
				
				ArrayList<double[]> trainingData = new ArrayList<double[]>();
				ArrayList<String> trainingClassification = new ArrayList<String>();
				ArrayList<String> trainingColumnHeaders = new ArrayList<String>();
				
				FileUtilities.readFeatureFile(trainingFileName, trainingData, trainingClassification, trainingColumnHeaders);
				collection.setTrainingData(trainingData, trainingClassification, trainingColumnHeaders);
				
				long startTime = SystemClock.elapsedRealtime();
				
				if (collection.StartCollection()) {
					((Chronometer) findViewById(R.id.chronometer)).setBase(startTime);
					((Chronometer) findViewById(R.id.chronometer)).start();
				}
				
				while (!stopped) {
					if (SystemClock.elapsedRealtime() > (startTime + timeInterval)) {
						if (!collection.StopCollection()) {
							stopped = true;
						}
						
						String predictedClassification = collection.getClassification();
						System.out.println("Predicted Classification: " + predictedClassification);
						((EditText) findViewById(R.id.classificationText)).setText(predictedClassification);
						
						startTime = SystemClock.elapsedRealtime();
						if (!stopped) {
							if (!collection.StartCollection()) {
								stopped = true;
							}
						}
					}
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
