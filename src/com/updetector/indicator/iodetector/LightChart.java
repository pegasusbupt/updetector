package com.updetector.indicator.iodetector;

import java.util.Calendar;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class LightChart implements SensorEventListener{
	
	private static GraphicalView lightGView;

	private SensorManager sensorManager;
	private Sensor lightSensor;
	private Sensor proxSensor;

	private XYSeries xySeries = new XYSeries("Light Sensor"); 
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYSeriesRenderer renderer = new XYSeriesRenderer(); // This will be used to customize line 1
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(); // Holds a collection of XYSeriesRenderer and customizes the graph

	private float lightIntensity;
	private boolean lightBlocked = false;
	private int time=0;
	
	private DetectionProfile[] listProfile = new DetectionProfile[3];
	private DetectionProfile indoor, semi, outdoor;

	final int HIGH_THRESHOLD = 1000; //original 2000
	final int LOW_THRESHOLD = 10; //original 50
	
	public LightChart(){}

	public LightChart(SensorManager sManager,Context context ){
		sensorManager = sManager;

		lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);

		proxSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		sensorManager.registerListener(this, proxSensor, SensorManager.SENSOR_DELAY_UI);
		
		indoor = new DetectionProfile("indoor");
		semi = new DetectionProfile("semi-outdoor");
		outdoor = new DetectionProfile("outdoor");
		listProfile[0]=indoor;
		listProfile[1]=semi;
		listProfile[2]=outdoor;
		
		// Add single dataset to multiple dataset
		mDataset.addSeries(xySeries);

		renderer.setColor(Color.BLACK);
		renderer.setPointStyle(PointStyle.SQUARE);
		renderer.setFillPoints(true);
		renderer.setChartValuesTextSize(15);

		mRenderer.setXTitle("Time (s)");
		mRenderer.setLabelsTextSize(10);
		mRenderer.setAntialiasing(true);
		mRenderer.setLabelsColor(Color.BLACK);
		mRenderer.setYTitle("Light Intensity");
		mRenderer.setMarginsColor(Color.WHITE);
		mRenderer.setAxesColor(Color.parseColor("#707070"));
		mRenderer.setYAxisMin(0);
		mRenderer.setYAxisMax(320);
		mRenderer.setShowGrid(true);
		mRenderer.setPanEnabled(false,false);
		mRenderer.setXLabels(3);
		
		// Add single renderer to multiple renderer
		mRenderer.addSeriesRenderer(renderer);	
		lightGView = ChartFactory.getLineChartView(context, mDataset, mRenderer);
	}

	public GraphicalView getView(){
		//add the data to the series
		xySeries.add(time++, lightIntensity);
		
		//remove the first data
		if(xySeries.getItemCount() > 15)
			xySeries.remove(0);
		
		mRenderer.setYAxisMax(xySeries.getMaxY()+20);
		lightGView.repaint();
		return lightGView;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_PROXIMITY){
			if(event.values[0] == event.sensor.getMaximumRange()){
				lightBlocked = false;
			}else {
				lightBlocked = true;
			}
		}else {
			lightIntensity = event.values[0];
		}
		
	}
	public float getLigthValue(){
		return lightIntensity;
	}
	
	public DetectionProfile[] getProfile(){
		indoor.setConfidence(0);
		semi.setConfidence(0);
		outdoor.setConfidence(0);
		if(!lightBlocked){//Light sensor is not blocked
			if(lightIntensity > HIGH_THRESHOLD){
				semi.setConfidence(1);
				outdoor.setConfidence(1);
				indoor.setConfidence(0);
			}else{
				double confidence;
				if(lightIntensity<=LOW_THRESHOLD){
					confidence = (LOW_THRESHOLD-lightIntensity)/LOW_THRESHOLD;
					semi.setConfidence(confidence);
					outdoor.setConfidence(confidence);
					indoor.setConfidence(1-confidence);
				}else{//between low and high
					if(HIGH_THRESHOLD-lightIntensity<=lightIntensity-LOW_THRESHOLD){//close to high
						confidence = (HIGH_THRESHOLD-lightIntensity)/HIGH_THRESHOLD;
						indoor.setConfidence(confidence);
						semi.setConfidence(1-confidence);
						outdoor.setConfidence(1-confidence);
					}else{//close to low
						confidence = (lightIntensity-LOW_THRESHOLD)/lightIntensity;
						semi.setConfidence(confidence);
						outdoor.setConfidence(confidence);
						indoor.setConfidence(1-confidence);
					}
				}
					
				/*
				Calendar calendar = Calendar.getInstance();
				int hour = calendar.get(Calendar.HOUR_OF_DAY);
				if(hour>=8 && hour<=17){//Check if the current time is daytime
					indoor.setConfidence(0.7);
					outdoor.setConfidence(0.3);
					semi.setConfidence(0);
				}else{
					if(lightIntensity<=LOW_THRESHOLD){
						double confidence = (LOW_THRESHOLD-lightIntensity)/LOW_THRESHOLD;
						semi.setConfidence(confidence);
						outdoor.setConfidence(confidence);
						indoor.setConfidence(1-confidence);
					}else{
						double confidence = (HIGH_THRESHOLD-lightIntensity)/HIGH_THRESHOLD;
						indoor.setConfidence(confidence);
						outdoor.setConfidence(1-confidence);
						semi.setConfidence(1-confidence);
					}
				}*/
			}
		}
		return listProfile;
	}



}
