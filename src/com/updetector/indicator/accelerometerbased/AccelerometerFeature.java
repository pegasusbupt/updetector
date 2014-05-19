package com.updetector.indicator.accelerometerbased;

import java.util.ArrayList;

import com.updetector.*;
import com.updetector.indicator.accelerometerbased.AccelerometerFeatureExtraction.Config;

public class AccelerometerFeature {

	public int[] temporalWindow;//the start and end time index of the window
	public int timeIndex; //the middle time index of the window
	//each axis has two values: variance of the values in the first and second half window, respectively
	public ArrayList<ArrayList<Double>> varianceSeries; 
	//each axis has two values: average of the values in the first and second half window, respectively
	public ArrayList<ArrayList<Double>> averageSeries;
	
	
	/**
	 * additional fields for classifying motion states
	 * reference: http://www.cis.fordham.edu/wisdm/public_files/sensorKDD-2010.pdf
	 * */
	// For X, Y, Z and aggregate axis
	public ArrayList<ArrayList<Double>> timeIntervalBtwPeaks;
	public ArrayList<ArrayList<Double>> binPercents;
	
	public AccelerometerFeature(){
		temporalWindow=new int[2];
		varianceSeries=new ArrayList<ArrayList<Double>>(Constants.AXIS_NUMBER); 
		for(int i=0;i<Constants.AXIS_NUMBER;i++) varianceSeries.add(new ArrayList<Double>());
		
		averageSeries=new ArrayList<ArrayList<Double>>(Constants.AXIS_NUMBER); 
		for(int i=0;i<Constants.AXIS_NUMBER;i++) averageSeries.add(new ArrayList<Double>());
		
		binPercents=new ArrayList<ArrayList<Double>>(Constants.AXIS_NUMBER); 
		for(int i=0;i<Constants.AXIS_NUMBER;i++) binPercents.add(new ArrayList<Double>());
	}
	
	public AccelerometerFeature(int[] window){
		this();
		for(int i=0;i<window.length; i++) temporalWindow[i]=window[i];
		this.timeIndex=temporalWindow[0]+(temporalWindow[1]-temporalWindow[0])/2;
	}
	
	/**
	 * @return a string feature for motion state classification
	 */
	public String asStringForMotationState(){
		StringBuilder sb=new StringBuilder();
		//output the time 
		//sb.append(timeIndex+",");
		
		//format needs to be compatible WISDM_Act_v1.1 dataset
		String dFormat="%.2f";
		for(int axisIdx=0;axisIdx<3;axisIdx++){
			for(int i=0;i<Config.NO_OF_BINS;i++){
				sb.append(String.format(dFormat, binPercents.get(axisIdx).get(i))+",");
			}
		}
		for(int axisIdx=0;axisIdx<3;axisIdx++){
			sb.append(String.format(dFormat, averageSeries.get(axisIdx).get(2))+",");	
		}
		for(int axisIdx=0;axisIdx<3;axisIdx++){
			//note it is standard deviation not variance
			//in order to be compatible WISDM_Act_v1.1 dataset
			sb.append(String.format(dFormat, Math.sqrt(varianceSeries.get(axisIdx).get(2)) )+",");
		}
		sb.append(String.format(dFormat,averageSeries.get(3).get(2))) ;//append the resultant
		
		return sb.toString();
	}
	
	
	public String toArffString(){
		String[] fields=toString().trim().split(" ");
		StringBuilder sb=new StringBuilder();
		//output the time 
		//not convient to handld string in weka, so use integer
		sb.append(CommonUtils.HMSToSecs(fields[0])+",");
		
		//output the variance difference of x axis
		double varianceDiff=Double.parseDouble(fields[2])-Double.parseDouble(fields[1]);
		sb.append(String.format("%.3f", varianceDiff)+",");
		//output the avearge of x axis
		sb.append(fields[3]+",");
		sb.append(fields[4]+",");
		
		// output the variance difference of y axis
		varianceDiff=Double.parseDouble(fields[6])-Double.parseDouble(fields[5]);
		sb.append(String.format("%.3f", varianceDiff)+",");
		//output the avearge of z axis
		sb.append(fields[7]+",");
		sb.append(fields[8]+",");
		
		
		// output the variance difference of z axis
		varianceDiff=Double.parseDouble(fields[10])-Double.parseDouble(fields[9]);
		sb.append(String.format("%.3f", varianceDiff)+",");
		//output the avearge of z axis
		sb.append(fields[11]+",");
		sb.append(fields[12]+",");
		
		
		// output the variance difference of the aggregate acceleration
		varianceDiff=Double.parseDouble(fields[14])-Double.parseDouble(fields[13]);
		sb.append(String.format("%.3f", varianceDiff)+",");
		//output the avearge of the aggregate acceleration
		sb.append(fields[15]+",");
		sb.append(fields[16]+",");
		
		return sb.toString();
	}
	
	public String toString(){
		if(this==null) return null;
		
		StringBuilder sb=new StringBuilder();
		sb.append(CommonUtils.secsToHMS(timeIndex)+" ");
		
		//determining which axises to output
		int[] outputAxis={0, 1, 2, 3};
		int axisIdx;
		int precision=3;
		
		for(int j=0;j<outputAxis.length;j++){
			axisIdx=outputAxis[j];
			for(int i=axisIdx;i<Math.min(axisIdx+1, Constants.AXIS_NUMBER);i++){
				ArrayList<Double> values=varianceSeries.get(i);
				//output the variance
				sb.append(CommonUtils.roundFraction(values.get(0),precision)+" "+CommonUtils.roundFraction(values.get(1),precision));
				//output average
				values=averageSeries.get(i);
				sb.append(" "+CommonUtils.roundFraction(values.get(0),precision)+" "+CommonUtils.roundFraction(values.get(1),precision)+" ");
			}
		}
		
		//sb.append("\n");
		return sb.toString();
	
	}
	
	public boolean equals(Object other){
		AccelerometerFeature feature=(AccelerometerFeature)other;
		return toString().equals(feature.toString());
	
	}

}
