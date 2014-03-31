package com.updetector.indicator.acoustic;

import jAudioFeatureExtractor.CommandLineThread;
import jAudioFeatureExtractor.DataModel;
import jAudioFeatureExtractor.ACE.DataTypes.Batch;
import jAudioFeatureExtractor.ACE.XMLParsers.XMLDocumentParser;
import jAudioFeatureExtractor.DataTypes.RecordingInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;

import com.updetector.R;

import android.content.Context;
import android.util.Log;


public class AudioFeatureExtraction {
	public static final String LOG_TAG=AudioFeatureExtraction.class.getCanonicalName();
	
	
	public static String extractFeatures(Context ctxt, String inputAudioFileName){
		
		//read and parse the setting
		DataModel dm=null;
				
		try {
			InputStream ins=ctxt.getResources().openRawResource(R.raw.audio_features);
			dm=new DataModel(ins,null);
			
		} catch (Exception e) {
			System.out.println("Error encountered parsing the features or settings xml file");
			e.printStackTrace();
		}

		
		int windowLength = 512;
		double offset = 0.0;
		double samplingRate;
		boolean saveWindows;
		boolean saveOverall;
		boolean normalise;
		int outputType;
		
		//hard-code all the setting parameters 
		try {
			windowLength = 512;
		} catch (NumberFormatException e) {
			System.out.println("Error in settings file");
			System.out.println("Window length of settings must be an integer");
			System.exit(4);
		}
		try {
			offset = 0.0;
		} catch (NumberFormatException e) {
			System.out.println("Error in settings file");
			System.out
					.println("Window offset of settings must be an double between 0 and 1");
			System.exit(4);
		}
		
		samplingRate = 16000;
		normalise = false;
		saveWindows = false;
		saveOverall = true;
		String outputFormat = "ARFF";
		outputType = 1; //"ARFF"
	
		OutputStream destinationFK = null;
		OutputStream destinationFV = null;
		try{		
			destinationFK = ctxt.openFileOutput("audioFK", Context.MODE_PRIVATE);  //definitions
			destinationFV = ctxt.openFileOutput("audioFV", Context.MODE_PRIVATE);  //values
		}catch(Exception ex){
			ex.printStackTrace();
		}

		HashMap<String, Boolean> active =new HashMap<String, Boolean>();
		active.put("Derivative of Running Mean of Spectral Rolloff Point",false);
		active.put("Strongest Beat",true);
		active.put("Derivative of Standard Deviation of Spectral Flux",false);
		active.put("Spectral Centroid",true);
		active.put("Running Mean of Fraction Of Low Energy Windows",false);
		active.put("Derivative of Standard Deviation of Spectral Rolloff Point",false);
		active.put("Derivative of Strongest Beat",false);
		active.put("Derivative of Running Mean of Strongest Frequency Via FFT Maximum",false);
		active.put("Derivative of Standard Deviation of Method of Moments",false);
		active.put("Running Mean of Zero Crossings",false);
		active.put("Derivative of Running Mean of LPC",false);
		active.put("LPC",true);
		active.put("Derivative of Standard Deviation of Fraction Of Low Energy Windows",false);
		active.put("Running Mean of MFCC",false);
		active.put("Derivative of Standard Deviation of Strongest Frequency Via FFT Maximum",false);
		active.put("Derivative of Running Mean of Method of Moments",false);
		active.put("Derivative of Running Mean of Strongest Frequency Via Zero Crossings",false);
		active.put("Standard Deviation of Area Method of Moments",false);
		active.put("Derivative of Relative Difference Function",false);
		active.put("Derivative of Compactness",false);
		active.put("Derivative of Fraction Of Low Energy Windows",false);
		active.put("Derivative of Standard Deviation of Spectral Variability",false);
		active.put("Running Mean of Spectral Flux",false);
		active.put("Compactness",true);
		active.put("Derivative of Standard Deviation of Spectral Centroid",false);
		active.put("Derivative of Method of Moments",false);
		active.put("Beat Histogram",false);
		active.put("Derivative of Running Mean of Compactness",false);
		active.put("Standard Deviation of Spectral Flux",false);
		active.put("Running Mean of LPC",false);
		active.put("Standard Deviation of Beat Sum",false);
		active.put("Standard Deviation of Zero Crossings",false);
		active.put("Standard Deviation of Root Mean Square",false);
		active.put("MFCC",true);
		active.put("Zero Crossings",true);
		active.put("Fraction Of Low Energy Windows",true);
		active.put("Derivative of Spectral Centroid",false);
		active.put("Derivative of Spectral Flux",false);
		active.put("Derivative of Strength Of Strongest Beat",false);
		active.put("Running Mean of Relative Difference Function",false);
		active.put("Standard Deviation of MFCC",false);
		active.put("Running Mean of Area Method of Moments",false);
		active.put("Standard Deviation of Strongest Frequency Via Zero Crossings",false);
		active.put("Standard Deviation of Partial Based Spectral Centroid",false);
		active.put("Derivative of Running Mean of Fraction Of Low Energy Windows",false);
		active.put("Derivative of Root Mean Square",false);
		active.put("FFT Bin Frequency Labels",false);
		active.put("Derivative of Strongest Frequency Via Zero Crossings",false);
		active.put("Derivative of Standard Deviation of Strength Of Strongest Beat",false);
		active.put("Derivative of Running Mean of Relative Difference Function",false);
		active.put("Derivative of Standard Deviation of Relative Difference Function",false);
		active.put("Standard Deviation of Strength Of Strongest Beat",false);
		active.put("Derivative of Partial Based Spectral Flux",false);
		active.put("Strongest Frequency Via Spectral Centroid",false);
		active.put("Derivative of Area Method of Moments",false);
		active.put("Standard Deviation of Spectral Variability",false);
		active.put("Running Mean of Strongest Frequency Via FFT Maximum",false);
		active.put("Running Mean of Strength Of Strongest Beat",false);
		active.put("Running Mean of Beat Sum",false);
		active.put("Derivative of Spectral Variability",false);
		active.put("Derivative of Running Mean of Zero Crossings",false);
		active.put("Derivative of Running Mean of Spectral Flux",false);
		active.put("Strength Of Strongest Beat",true);
		active.put("Derivative of Standard Deviation of LPC",false);
		active.put("Derivative of LPC",false);
		active.put("Derivative of Standard Deviation of Compactness",false);
		active.put("Derivative of Zero Crossings",false);
		active.put("Standard Deviation of Strongest Frequency Via Spectral Centroid",false);
		active.put("Partial Based Spectral Centroid",false);
		active.put("Running Mean of Compactness",false);
		active.put("Running Mean of Strongest Beat",false);
		active.put("Derivative of Standard Deviation of Strongest Frequency Via Zero Crossings",false);
		active.put("Standard Deviation of Spectral Rolloff Point",false);
		active.put("Spectral Variability",true);
		active.put("Peak Based Spectral Smoothness",false);
		active.put("Running Mean of Method of Moments",false);
		active.put("Strongest Frequency Via Zero Crossings",false);
		active.put("Beat Histogram Bin Labels",false);
		active.put("Method of Moments",true);
		active.put("Standard Deviation of Fraction Of Low Energy Windows",false);
		active.put("Magnitude Spectrum",false);
		active.put("Derivative of Standard Deviation of Beat Sum",false);
		active.put("Derivative of Standard Deviation of Area Method of Moments",false);
		active.put("Running Mean of Peak Based Spectral Smoothness",false);
		active.put("Relative Difference Function",false);
		active.put("Derivative of Standard Deviation of Strongest Beat",false);
		active.put("Running Mean of Root Mean Square",false);
		active.put("Running Mean of Partial Based Spectral Centroid",false);
		active.put("Derivative of Running Mean of MFCC",false);
		active.put("Derivative of Running Mean of Strength Of Strongest Beat",false);
		active.put("Standard Deviation of Peak Based Spectral Smoothness",false);
		active.put("Spectral Flux",true);
		active.put("Running Mean of Partial Based Spectral Flux",false);
		active.put("Derivative of Standard Deviation of Peak Based Spectral Smoothness",false);
		active.put("Derivative of Running Mean of Spectral Variability",false);
		active.put("Derivative of Standard Deviation of Zero Crossings",false);
		active.put("Derivative of Running Mean of Beat Sum",false);
		active.put("Standard Deviation of Relative Difference Function",false);
		active.put("Derivative of Running Mean of Peak Based Spectral Smoothness",false);
		active.put("Beat Sum",true);
		active.put("Derivative of Running Mean of Spectral Centroid",false);
		active.put("Standard Deviation of Strongest Beat",false);
		active.put("Derivative of Running Mean of Partial Based Spectral Centroid",false);
		active.put("Standard Deviation of Partial Based Spectral Flux",false);
		active.put("Derivative of Running Mean of Root Mean Square",false);
		active.put("Running Mean of Spectral Centroid",false);
		active.put("Derivative of Standard Deviation of Partial Based Spectral Flux",false);
		active.put("Derivative of Running Mean of Strongest Beat",false);
		active.put("Derivative of Spectral Rolloff Point",false);
		active.put("Derivative of Running Mean of Area Method of Moments",false);
		active.put("Standard Deviation of Strongest Frequency Via FFT Maximum",false);
		active.put("Derivative of Standard Deviation of Root Mean Square",false);
		active.put("Standard Deviation of Spectral Centroid",false);
		active.put("Derivative of Strongest Frequency Via FFT Maximum",false);
		active.put("Peak Detection",false);
		active.put("Running Mean of Spectral Variability",false);
		active.put("Derivative of Peak Based Spectral Smoothness",false);
		active.put("Derivative of Strongest Frequency Via Spectral Centroid",false);
		active.put("Derivative of Partial Based Spectral Centroid",false);
		active.put("Strongest Frequency Via FFT Maximum",false);
		active.put("Derivative of Standard Deviation of MFCC",false);
		active.put("Running Mean of Spectral Rolloff Point",false);
		active.put("Standard Deviation of Method of Moments",false);
		active.put("Spectral Rolloff Point",true);
		active.put("Derivative of MFCC",false);
		active.put("Standard Deviation of Compactness",false);
		active.put("Derivative of Standard Deviation of Partial Based Spectral Centroid",false);
		active.put("Running Mean of Strongest Frequency Via Spectral Centroid",false);
		active.put("Root Mean Square",true);
		active.put("Derivative of Running Mean of Strongest Frequency Via Spectral Centroid",false);
		active.put("Area Method of Moments",false);
		active.put("Partial Based Spectral Flux",false);
		active.put("Running Mean of Strongest Frequency Via Zero Crossings",false);
		active.put("Derivative of Standard Deviation of Strongest Frequency Via Spectral Centroid",false);
		active.put("Derivative of Running Mean of Partial Based Spectral Flux",false);
		active.put("Standard Deviation of LPC",false);
		active.put("Derivative of Beat Sum",false);
		active.put("Power Spectrum",false);
		
		HashMap<String, String[]> attribute =new HashMap<String, String[]>();
		String[] attributeVales= new String[]{"0.85","100"};
		attribute.put("Derivative of Running Mean of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{"0.85","100"};
		attribute.put("Derivative of Standard Deviation of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Zero Crossings",attributeVales);
		attributeVales= new String[]{"0.0","10","100"};
		attribute.put("Derivative of Running Mean of LPC",attributeVales);
		attributeVales= new String[]{"0.0","10"};
		attribute.put("LPC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{"13","100"};
		attribute.put("Running Mean of MFCC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"10","100"};
		attribute.put("Standard Deviation of Area Method of Moments",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Relative Difference Function",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Compactness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Spectral Variability",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Compactness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Method of Moments",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Beat Histogram",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Compactness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Spectral Flux",attributeVales);
		attributeVales= new String[]{"0.0","10","100"};
		attribute.put("Running Mean of LPC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Beat Sum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Root Mean Square",attributeVales);
		attributeVales= new String[]{"13"};
		attribute.put("MFCC",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Zero Crossings",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Relative Difference Function",attributeVales);
		attributeVales= new String[]{"13","100"};
		attribute.put("Standard Deviation of MFCC",attributeVales);
		attributeVales= new String[]{"10","100"};
		attribute.put("Running Mean of Area Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Root Mean Square",attributeVales);
		attributeVales= new String[]{};
		attribute.put("FFT Bin Frequency Labels",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Relative Difference Function",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Relative Difference Function",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{"10"};
		attribute.put("Derivative of Area Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Spectral Variability",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Beat Sum",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Spectral Variability",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Spectral Flux",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"0.0","10","100"};
		attribute.put("Derivative of Standard Deviation of LPC",attributeVales);
		attributeVales= new String[]{"0.0","10"};
		attribute.put("Derivative of LPC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Compactness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Compactness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"0.85","100"};
		attribute.put("Standard Deviation of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Spectral Variability",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Method of Moments",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Beat Histogram Bin Labels",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Fraction Of Low Energy Windows",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Magnitude Spectrum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Beat Sum",attributeVales);
		attributeVales= new String[]{"10","100"};
		attribute.put("Derivative of Standard Deviation of Area Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Relative Difference Function",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Root Mean Square",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"13","100"};
		attribute.put("Derivative of Running Mean of MFCC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strength Of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Spectral Variability",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Beat Sum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Relative Difference Function",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Beat Sum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strongest Beat",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Root Mean Square",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strongest Beat",attributeVales);
		attributeVales= new String[]{"0.85"};
		attribute.put("Derivative of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{"10","100"};
		attribute.put("Derivative of Running Mean of Area Method of Moments",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Root Mean Square",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"10"};
		attribute.put("Peak Detection",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Spectral Variability",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Peak Based Spectral Smoothness",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Strongest Frequency Via FFT Maximum",attributeVales);
		attributeVales= new String[]{"13","100"};
		attribute.put("Derivative of Standard Deviation of MFCC",attributeVales);
		attributeVales= new String[]{"0.85","100"};
		attribute.put("Running Mean of Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Method of Moments",attributeVales);
		attributeVales= new String[]{"0.85"};
		attribute.put("Spectral Rolloff Point",attributeVales);
		attributeVales= new String[]{"13"};
		attribute.put("Derivative of MFCC",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Standard Deviation of Compactness",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Partial Based Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Root Mean Square",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{"10"};
		attribute.put("Area Method of Moments",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Running Mean of Strongest Frequency Via Zero Crossings",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Standard Deviation of Strongest Frequency Via Spectral Centroid",attributeVales);
		attributeVales= new String[]{"100"};
		attribute.put("Derivative of Running Mean of Partial Based Spectral Flux",attributeVales);
		attributeVales= new String[]{"0.0","10","100"};
		attribute.put("Standard Deviation of LPC",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Derivative of Beat Sum",attributeVales);
		attributeVales= new String[]{};
		attribute.put("Power Spectrum",attributeVales);
		
		// now process the aggregators
		String[] aggNames = {"Standard Deviation", "Mean"};
		String[][] aggFeatures ={{},{}};
		String[][] aggParameters ={{},{}};

		// now process the files
		File[] names = new File[1];
		for (int i = 0; i < names.length; ++i) {
			names[i] = new File(inputAudioFileName+".wav");
		}
		
		// Go through the files one by one
		RecordingInfo[] recording_info = new RecordingInfo[1];
		for (int i = 0; i < names.length; i++) {
			// Assume file is invalid as first guess
			recording_info[i] = new RecordingInfo(names[i].getName(), names[i]
					.getPath(), null, false);
		}// for i in names

		try {
			dm.featureKey = destinationFK;
			dm.featureValue = destinationFV;
			Batch b = new Batch();
			b.setDataModel(dm);
			b.setWindowSize(windowLength);
			b.setWindowOverlap(offset);
			b.setSamplingRate(samplingRate);
			b.setNormalise(normalise);
			b.setPerWindow(saveWindows);
			b.setOverall(saveOverall);
			b.setRecording(recording_info);
			b.setOutputType(outputType);
			b.setFeatures(active,attribute);
			b.setAggregators(aggNames,aggFeatures,aggParameters);

			CommandLineThread clt = new CommandLineThread(b);
			clt.start();
			while(clt.isAlive()){
				if(System.in.available()>0){
					clt.cancel();
				}
				clt.join(1000);
			}
			
			destinationFK.close();
			destinationFV.close();
			Log.d(LOG_TAG, "Features extracted and save to audioFV");
			
			//read the outputFile and convert it to String
			FileInputStream fis=ctxt.openFileInput("audioFV");
			int bite;
			StringBuilder features=new StringBuilder();
			while ((bite = fis.read()) != -1) {
				// convert to char and display it
				features.append((char) bite);
			}
			fis.close();
			Log.d(LOG_TAG, "Features readout and returned");
			return features.toString();
			
		} catch (Exception e) {
			System.out.println("Error extracting features - aborting");
			System.out.println(e.getMessage());
			System.exit(5);
		}
		return null;
		
	}
	
	
	
	
	
	
	

	

}
