/*
 * This class is for the profile of each sensor. 
 * It will saved the environment(indoor,semi,outdoor) and the confidence value;
 */

package com.updetector.indicator.iodetector;

public class DetectionProfile {

	private String environment;
	private double confidence;
	
	public DetectionProfile(String env){
		this.environment = env;
		this.confidence = 0;
	}

	public String getEnvironment() {
		return environment;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}
	
	

}
