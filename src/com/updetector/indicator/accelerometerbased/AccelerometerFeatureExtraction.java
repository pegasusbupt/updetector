package com.updetector.indicator.accelerometerbased;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.R.integer;
import android.hardware.SensorEvent;
import android.util.Log;

import com.updetector.CommonUtils;
import com.updetector.Constants;


public class AccelerometerFeatureExtraction {
	private static String LOG_TAG=AccelerometerFeatureExtraction.class.getCanonicalName();
	
	public static class Config {		
		/*
		 * Parameters for feature extraction
		 */	
			//length of window in secs
			public int windowSize;  
			public int slidingStep; 
			public int scope;
			
			public static int NO_OF_BINS=5;
			public int noOfBins;
			
			//if the no. of accelerometer readings in any half is below the threshold, the window is not output for a feature
			public int minNoOfSamplesInHalfWindow;
			
			//if the # of readings in the window below the threshold, the window is not output for a feature
			public int minNoOfSamplesInWholeWindow;
			public boolean motionStateFeature; //true if extracting features for motion state classifying 
			
		
		public Config(int windowSize, int slidingStep){
			//windowSize=10;		
			//slidingStep=9;
			this.windowSize=windowSize;
			this.slidingStep=slidingStep;
			
			this.scope=6; //6 windows
			minNoOfSamplesInHalfWindow=1;
			noOfBins=NO_OF_BINS;
			minNoOfSamplesInWholeWindow=minNoOfSamplesInHalfWindow;
		}
			
	}

	public class Window{
		//temporal boundary of the window
		int[] bounds;
		//recording the content in the window
		ArrayList<ArrayList<Double>> acclReadings;
		//recording the no of samples for each time index in the window
		ArrayList<ArrayList<Integer>> noOfSamples; 
		
		public Window(){
			bounds=new int[]{-1, -1};
			acclReadings=new ArrayList<ArrayList<Double>>(Constants.AXIS_NUMBER);
			for(int i=0;i<Constants.AXIS_NUMBER;i++) acclReadings.add(new ArrayList<Double>());
			noOfSamples=new ArrayList<ArrayList<Integer>> ();
		}
		
		public void setBounds(int[] bounds){
			this.bounds[0]=bounds[0];
			this.bounds[1]=bounds[1];			
		}
	}
	
	public Config conf;

	//store labeld lines in the input minimally-labeld accelerometer file
	public HashMap<Integer, String> eventsTimestamps;
	public Window window;
	public ArrayList<AccelerometerFeature> features;
	
	
	public AccelerometerFeatureExtraction(Config conf){
		this.conf=conf;
		eventsTimestamps=new HashMap<Integer, String>();
		window=new Window();	
		features=new ArrayList<AccelerometerFeature>();
	}	
	
	/**
	 * 
	 * @param sensorEvent: an accelerometer sensor reading update 
	 * @return the feature for the current window
	 */
	public AccelerometerFeature extractWindowFeature(SensorEvent sensorEvent){		
		String record=CommonUtils.buildALogRecordForNewAccelerometerReadings(sensorEvent);
		AccelerometerFeature newFeature=null;
		if(record!=null){
			String[] fields=record.split(" ");
			int curTime=CommonUtils.HMSToSecs(fields[1]);	
			newFeature=slideWindow(window, curTime, features, fields);			
		}
		
		return  newFeature;
	}
	
	
	public String extractCIVVector(AccelerometerFeature curWindow, ArrayList<AccelerometerFeature> previousWindows){
		String line=null;
		/*try {
			
		}catch (Exception ex) {
			Log.e(LOG_TAG, "something wrong within creating an instance");
			ex.printStackTrace();
			return null;
		}*/
		int K=conf.scope; // K/2 preceding windows and K/2 trailing windows
		
		
		if(curWindow==null) return line;
		//add curwindow
		previousWindows.add(curWindow);
		
		
		if(previousWindows.size()==K+1){
			line=String.valueOf(previousWindows.get(K/2).timeIndex);
			int[] precedingOrTrailing={0, K/2+1};
			for(int j=0;j<precedingOrTrailing.length;j++){
				double[] variDiff=new double[K/2];
				double[] avgAccel=new double[K/2];
				//add the mean and variance of the values of the neighboring K/2 windows
				for(int i=precedingOrTrailing[j];i<precedingOrTrailing[j]+K/2;i++){
					//the variance diff. between two halves of the window
					AccelerometerFeature window=previousWindows.get(i);
					ArrayList<Double> acclList=window.varianceSeries.get(Constants.AXIS_AGG);
					variDiff[i-precedingOrTrailing[j]]=acclList.get(1)-acclList.get(0);
					avgAccel[i-precedingOrTrailing[j]]=acclList.get(2);
				}
				double avg=CommonUtils.calculateMean(variDiff);
				line+=","+String.format("%7.3f", avg); //add the avg. of the variDiff
				line+=","+String.format("%7.3f", CommonUtils.calculateVariance(variDiff, avg)); // add the var. of the variDiff
				
				avg=CommonUtils.calculateMean(avgAccel);
				line+=","+String.format("%7.3f", avg); //add the avg. of the avg
				line+=","+String.format("%7.3f", CommonUtils.calculateVariance(avgAccel, avg)); // add the var. of the avg
				
				if(j==0){//add the value of the center window
					ArrayList<Double> acclList=previousWindows.get(K/2).varianceSeries.get(Constants.AXIS_AGG);
					line+=","+String.format("%7.3f", acclList.get(1)-acclList.get(0));
				}
			}
			previousWindows.remove(0); // remove the first window
		} 
		return line;
	}
	
	
	
	
	
	
	/**
	 * @param window: 
	 * @param curTime: time in seconds
	 * @param features: save the new feature to the list 
	 * @param fields: the readings of the three axes
	 * @return the extracted feature for the current window
	 */	
	private AccelerometerFeature slideWindow(Window window, int curTime, ArrayList<AccelerometerFeature> features, String[] fields){
		AccelerometerFeature newFeature=null;
		//if the window slides
		//System.out.println(window.bounds[0]);
		if(curTime-window.bounds[0]>conf.windowSize){
		
			//extract the feature for the current window
			newFeature=extractFeatureForCurrentWindow(window);
			int noOfFeatures=features.size();
			if(newFeature!=null&& (noOfFeatures==0||!features.get(noOfFeatures-1).equals(newFeature)) ){
				features.add(newFeature);
			}
			
			//update the bounds
			if(curTime-window.bounds[1]>conf.slidingStep) window.setBounds(new int[]{curTime-conf.windowSize, curTime});
			else  window.setBounds(new int[]{window.bounds[0]+conf.slidingStep, window.bounds[1]+conf.slidingStep});
			
			
			//remove expired readings
			int noOfRemoveTimestamps=0;
			for(ArrayList<Integer> count: window.noOfSamples){
				int timeIdx=count.get(0);
				int cnt=count.get(1);
				if(timeIdx>=window.bounds[0]) break;
				
				noOfRemoveTimestamps+=1;
				for(int axisIdx=0;axisIdx<Constants.AXIS_NUMBER; axisIdx++){
					for(int j=0;j<cnt;j++){
						window.acclReadings.get(axisIdx).remove(0);
					}
				}
			}
			for (int i = 0; i < noOfRemoveTimestamps; i++) {
				window.noOfSamples.remove(0);
			}
		}
		
		//add the new accelerometer readings
		for(int axisIdx=0;axisIdx<Constants.AXIS_NUMBER;axisIdx++){
			window.acclReadings.get(axisIdx).add(Double.parseDouble(fields[2+axisIdx]));
		}		
		
		//count the no of samples for this second
		int idxOfLastTimestamp=window.noOfSamples.size()-1;
		//if a new second
		if(idxOfLastTimestamp==-1||window.noOfSamples.get(idxOfLastTimestamp).get(0)!=curTime){
			window.noOfSamples.add(new ArrayList<Integer>()); 
			idxOfLastTimestamp=window.noOfSamples.size()-1; 
			window.noOfSamples.get(idxOfLastTimestamp).add(curTime); //add timestamp
			window.noOfSamples.get(idxOfLastTimestamp).add(1); //only 1 sample in this second
		}else{ // a old second
			int cnt=window.noOfSamples.get(idxOfLastTimestamp).get(1);
			//update the count
			window.noOfSamples.get(idxOfLastTimestamp).remove(1);
			window.noOfSamples.get(idxOfLastTimestamp).add(cnt+1);
		}
		return newFeature;		
	}
	
	private  AccelerometerFeature extractFeatureForCurrentWindow(Window window){
		//initialize a feature
		AccelerometerFeature feature=new AccelerometerFeature(window.bounds);
		//add the content to the feature 
		for(int axisIdx=0;axisIdx<Constants.AXIS_NUMBER; axisIdx++){
			ArrayList<Double> axisAccl=window.acclReadings.get(axisIdx);
			
			int centralTimeIdxOftheWindow=feature.timeIndex;
			int noOfSamplesInFirstHalf=0;
			for(ArrayList<Integer> cnt: window.noOfSamples){
				if(cnt.get(0)>centralTimeIdxOftheWindow) break;
				noOfSamplesInFirstHalf+=cnt.get(1);
			}
			
			//add the first half average
			List<Double> firstHalfOfWindow=axisAccl.subList(0, noOfSamplesInFirstHalf); 
			
			//if the first half of window is empty; then discard this window
			if(firstHalfOfWindow.size()<conf.minNoOfSamplesInHalfWindow) return null; 
			
			
			double avgValue=CommonUtils.calculateMean(firstHalfOfWindow);
			feature.averageSeries.get(axisIdx).add(avgValue);
			//add the first half variance
			double varValue=CommonUtils.calculateVariance(firstHalfOfWindow, avgValue);
			feature.varianceSeries.get(axisIdx).add(varValue);
			
			//add the second half average
			List<Double> secondHalfOfWindow=axisAccl.subList(noOfSamplesInFirstHalf, axisAccl.size());
			
			//if the second half of window is empty; then discard this window
			if(secondHalfOfWindow.size()<conf.minNoOfSamplesInHalfWindow) return null;
			
			avgValue=CommonUtils.calculateMean(secondHalfOfWindow);
			feature.averageSeries.get(axisIdx).add(avgValue);
			//add the second half variance
			varValue=CommonUtils.calculateVariance(secondHalfOfWindow, avgValue);
			feature.varianceSeries.get(axisIdx).add(varValue);
			
			
			//add the whole window
			avgValue=CommonUtils.calculateMean(axisAccl);
			feature.averageSeries.get(axisIdx).add(avgValue);
			varValue=CommonUtils.calculateVariance(axisAccl, avgValue);
			feature.varianceSeries.get(axisIdx).add(varValue);
			
			/**
			 * calculate additional fields of feature for classifying motion states
			 */
			//binPecents
			double max=Integer.MIN_VALUE, min=Integer.MAX_VALUE;
			for(Double d: axisAccl){
				max=Math.max(max, d);
				min=Math.min(min, d);
			}
			double[] lowerBoundOfBins=new double[conf.noOfBins];
			double step=(max-min)/lowerBoundOfBins.length;
			for(int i=0;i<lowerBoundOfBins.length;i++){
				lowerBoundOfBins[i]=min+step*i;
			}
			double[] cntOfBins=new double[lowerBoundOfBins.length];
			for(Double d: axisAccl){
				//find which bin this value belongs to
				int l=-1, r=lowerBoundOfBins.length, m;
				while(l+1!=r){
					m=l+(r-l)/2;
					if(d<lowerBoundOfBins[m]) r=m;
					else l=m;
				}
				if(l>=0) cntOfBins[l]+=1;
			}
			for(int i=0;i<cntOfBins.length;i++){
				feature.binPercents.get(axisIdx).add(cntOfBins[i]/axisAccl.size());
			}
			
		}
		return feature;
	}
	
		
	

}
