package com.updetector.indicator.iodetector;


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

public class MagnetChart implements SensorEventListener{

	private double x,y,z;
	private SensorManager sensorManager;
	private Sensor magnetSensor;
	
	private DetectionProfile[] listProfile = new DetectionProfile[3];
	private DetectionProfile indoor, semi, outdoor;

	private XYSeries xySeries = new XYSeries("Magnetic Intensity");

	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYSeriesRenderer renderer = new XYSeriesRenderer(); 

	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(); // Holds a collection of XYSeriesRenderer and customizes the graph

	private int time=0, timer=0;
	private static GraphicalView magnetView;
	private double magnetValue, indoorVar, outdoorVar;
	
	public double magnetVariation;
	private double[] magnetism = new double[10];
	
	private final int THRESHOLD = 18;
	
	public MagnetChart() {
		// TODO Auto-generated constructor stub
	}

	public MagnetChart(SensorManager sManager,Context context){
		sensorManager = sManager;
		magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_UI);
		
		indoor = new DetectionProfile("indoor");
		semi = new DetectionProfile("semi-outdoor");
		outdoor = new DetectionProfile("outdoor");
		listProfile[0]=indoor;
		listProfile[1]=semi;
		listProfile[2]=outdoor;

		mDataset.addSeries(xySeries);

		renderer.setColor(Color.BLACK);
		renderer.setPointStyle(PointStyle.CIRCLE);
		renderer.setFillPoints(true);
		
		mRenderer.setXTitle("Time (s)");
		mRenderer.setLabelsTextSize(10);
		mRenderer.setLabelsColor(Color.BLACK);
		mRenderer.setAntialiasing(true);
		mRenderer.setYTitle("Values (uT)");
		mRenderer.setYAxisMin(0);
		mRenderer.setYAxisMax(40);
		mRenderer.setPanEnabled(false,false);
		mRenderer.setMarginsColor(Color.WHITE);
		mRenderer.setAxesColor(Color.parseColor("#707070"));
		mRenderer.setShowGrid(true);
		mRenderer.setXLabels(3);
		mRenderer.addSeriesRenderer(renderer);
		
		magnetView = ChartFactory.getLineChartView(context, mDataset, mRenderer);
		
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		x = event.values[0];
		y = event.values[1];
		z = event.values[2];
	}
	

	public GraphicalView getView()
	{
		magnetValue = Math.sqrt(x*x+y*y+z*z);
		
		/*
		magnetism[time%10] = magnetValue;
		if(time > 9)
		{
			magnetVariation[time%10] = getVariation(magnetism);
		}
		*/
		
		xySeries.add(time, magnetValue);
		time++;
		
		if(xySeries.getItemCount() > 15){
			xySeries.remove(0);
		}
		
		mRenderer.setYAxisMax(xySeries.getMaxY()+15);//dynamically change the top value
		
		magnetView.repaint();
		return magnetView;

	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public double getMagnetIntensity(){
		return magnetValue;
	}
	
	public double getVariation(double[] values)
	{
		double ave = 0;
		
		for(int i=0; i< values.length; i++)
			ave += values[i];
		ave /= values.length;
		
		double variation = 0;
		for (int i=0; i<values.length; i++)
			variation = (values[i] - ave) * (values[i] - ave);
		variation /= values.length;		
		
		return variation;
	}
	
	public void updateProfile()
	{
		
		magnetism[timer%10] = magnetValue;
		timer++;
		
		magnetVariation = getVariation(magnetism);
		
		//Compare the value with the threshold (18)
		if(magnetVariation > THRESHOLD)
		{
			indoorVar = 0.7;
			outdoorVar = 0.3;
		}else{
			outdoorVar = 0.7;
			indoorVar = 0.3;
		}
	}
	
	//get the diff. 
	public double getVariDiff(){
		getProfile();//update the var first
		return outdoorVar-indoorVar;
	}
	
	public DetectionProfile[] getProfile()
	{
		indoor.setConfidence(indoorVar/10);
		semi.setConfidence(indoorVar/10);
		outdoor.setConfidence(outdoorVar/10);
		indoorVar = 0;
		outdoorVar = 0;
		timer = 0;
		
		return listProfile;
	}
	

}
