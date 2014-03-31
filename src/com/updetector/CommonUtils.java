package com.updetector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.R.integer;
import android.hardware.SensorEvent;
import android.util.Log;

public class CommonUtils {
	
	/*************************************
	 * string format
	 **************************************/
	public static String formatTimestamp(Date date, String formatTemplate){		
        try {
        	SimpleDateFormat mDateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
            // Format the timestamp according to the pattern, then localize the pattern
            mDateFormat.applyPattern(formatTemplate);
            mDateFormat.applyLocalizedPattern(mDateFormat.toLocalizedPattern());
            String timeStamp = mDateFormat.format(date);
            return timeStamp;
        } catch (Exception e) {
            Log.e(Constants.APP_NAME, "error in dateformat initialization");
        }
        return null;
	}
	
	public static String getFileName(String path){
		int idx = path.lastIndexOf("/");
		return idx >= 0 ? path.substring(idx + 1) : path;
	}
	
	public static String getDirectory(String path){
		int idx = path.lastIndexOf("/");
		return idx >= 0 ? path.substring(0,idx + 1) : path;
	}
	
	/*************************************
	 * data type conversion
	 **************************************/
	public static int idxOfMax(double[] values){
		int ret=-1;
		double max=Double.MIN_VALUE;
		for(int i=0;i<values.length;i++){
			if(values[i]>max){
				max=values[i];
				ret=i;
			}
		}
		return ret;
	}
	
	
	
	public static double[] intArrayToDoubleArray(int[] arr){
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}
	
	public static float[] doubleArrayToFloatArray(double[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (float)arr[i];
		}
		return ret;
	}

	public static double[] floatArrayToDoubleArray(float[] arr) {
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++) {
			ret[i] = (double)arr[i];
		}
		return ret;
	}
	
	public static double[] doubleListToDoubleArray(List<Double> firstHalfOfWindow){
		if(firstHalfOfWindow==null) return null;
		double[] ret=new double[firstHalfOfWindow.size()];
		for(int i=0; i<ret.length;i++) ret[i]=firstHalfOfWindow.get(i);
		return ret;
	}
	
	/**
	 * 
	 * @param s
	 * @param delimeter
	 * @param fieldsToBeKept: idxes of the fields to be kept
	 * @return
	 */
	public static ArrayList<Double> stringToDoubleList(String s, String delimeter, int[] fieldsToBeKept){
		ArrayList<Double> ret=new ArrayList<Double>();
		ArrayList<Double> allFields=new ArrayList<Double>();
		for(int i=0;i<fieldsToBeKept.length;i++){
			ret.add(allFields.get(fieldsToBeKept[i]) );
		}
		return ret;
	}
	
	public static ArrayList<Double> stringToDoubleListRemoved(String s, String delimeter, int[] fieldsToBeRemoved){
		ArrayList<Double> ret=new ArrayList<Double>();
		
		ArrayList<Double> allFields=stringToDoubleList(s, delimeter);
		
		HashSet<Integer> toBeRemoved=new HashSet<Integer>();
		for(int i:fieldsToBeRemoved) toBeRemoved.add(i);
				
		for(int i=0;i<allFields.size();i++){
			if(toBeRemoved.contains(i)) continue;
			ret.add(allFields.get(i) );
		}
		return ret;
	}
	
	public static ArrayList<Double> stringToDoubleList(String s, String delimeter){
		String[] fields=s.split(delimeter);
		ArrayList<Double> ret=new ArrayList<Double>();
		for(int i=0;i<fields.length;i++){
			ret.add(Double.parseDouble(fields[i].trim()) );
		}
		return ret;
	}
	
	public static int stringTimeToInt(String time){
		String[] fields=time.split(":");
		int secs=Integer.parseInt(fields[0])*3600+Integer.parseInt(fields[1])*60;
		if (fields.length>2) secs+=Integer.parseInt(fields[2]);
		return secs;
	}
	
	public static String intTimeToString(int secs){
		StringBuilder  sb=new StringBuilder();
		int[] hourMinSec=new int[3];
		hourMinSec[0]=secs/3600;
		hourMinSec[1]=(secs-hourMinSec[0]*3600)/60;
		hourMinSec[2]=secs-hourMinSec[0]*3600-hourMinSec[1]*60;
		for(int i=0;i<hourMinSec.length;i++){			
			if(i==hourMinSec.length-1&&hourMinSec[i]==0) continue;
			if(i>0) sb.append(":");
			sb.append(hourMinSec[i]>=10?hourMinSec[i]:("0"+hourMinSec[i]));

		}
		return sb.toString();
	}
	
	
	/**
	 *   
	 * @start: index of the starting field (inclusive)
	 * @end: index of the ending field (exclusive)
	 * @return a substring consisting of fields of the given string at the given indices
	 */
	public static String cutString(String s, String delimeter, int start, int end, String connectingDelimeter){
		try{
			String[] fields=s.split(delimeter);
			StringBuilder sb=new StringBuilder();
			for(int i=start;i<end;i++){
				if(i!=start) sb.append(connectingDelimeter);
				sb.append(fields[i]);
			}
			return sb.toString();
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}
	}
	
	public static String cutString(String s, String delimeter, int start){
		return cutString(s, delimeter, start, s.split(delimeter).length, " ");
	}
	
	
	/**************************************
	 * app specific functions
	 *************************************/
	public static String eventCodeToString(int event){
		switch (event) {
		case Constants.ENVIRON_UNKNOWN:
			return "unkown";
		case Constants.ENVIRON_INDOOR:
			return "indoor";
		case Constants.ENVIRON_OUTDOOR:
			return "outdoor";
		case Constants.ENVIRON_SEMI_OUTDOOR:
			return "semi-outdoor";
		case Constants.OUTCOME_PARKING:
			return "parking";
		case Constants.OUTCOME_UNPARKING:
			return "unparking";
		default:
			break;
		}
		
		for(int i=0;i<Constants.CLASSIFIER_NAME.length;i++){
			for(int j=0;j<Constants.CLASSIFIER_EVENT[i].length;j++)
				if(Constants.CLASSIFIER_EVENT[i][j]==event)
					return Constants.CLASSIFIER_CLASS[i][j];
		}
		
		return "";
	}
	
	/**
	 * 
	 * @param sensorEvent: accelerometer readings update event 
	 * @return a log record 
	 */
	public static String buildALogRecordForNewAccelerometerReadings(SensorEvent event){
		double [] gravity=new double[3];
		//low-pass filter
		gravity[0] = Constants.ALPHA * gravity[0] + (1 - Constants.ALPHA) * event.values[0];
		gravity[1] = Constants.ALPHA * gravity[1] + (1 - Constants.ALPHA) * event.values[1];
		gravity[2] = Constants.ALPHA * gravity[2] + (1 - Constants.ALPHA) * event.values[2];
		
		//linear acceleration along three axies
        double x = event.values[0]-gravity[0];
        double y = event.values[1]-gravity[1];
        double z = event.values[2]-gravity[2];
        	
  
	    double acceleration = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)); 
	    if(acceleration<Constants.ACCL_THRESHOLD_FOR_STILL) //threshold for still
	    	return null;
	    
	    String timeStamp = formatTimestamp(new Date(),"yyyy-MM-dd HH:mm:ss:SSS");
        // Log the acceleration data only if the readings changed        
        String record=timeStamp;
        for(double d: event.values) record+=" " + String.format("%.3f", d);
        record+=" "+String.format("%.3f", acceleration);
        
        return record;
        
	}
	
	/**************************************
	 * Math relevant
	 *************************************/
	
	public static double calculatePDFOfNormalDistribution(double mean, double std, double value){
		double prob=Math.pow(Math.E, -Math.pow(value-mean, 2)/2/Math.pow(std, 2))/(Math.sqrt(Math.PI*2)*std);
		if(prob>1) System.out.println(mean+" "+std+" "+value);
		return prob;
	}
	
	public static double roundFraction(Double value, int precision){
		int scale=(int)Math.pow(10, precision);
		return (double)Math.round(value * scale) / scale;
	}
	
	
	
	public static double calculateMean(double[] list){
		ArrayList<Double> dList=new ArrayList<Double>(list.length);
		for(double d: list) dList.add(d);
		return calculateMean(dList);
	}
	
	
	public static double calculateMean(List<Double> list){
		if(list==null||list.size()==0) return Double.MAX_VALUE;
		double sum=0;		
		for(double num: list) sum+=num;
		return sum/list.size();		
	}
	
	public static double calculateVariance(double[] list, double mean){
		ArrayList<Double> dList=new ArrayList<Double>(list.length);
		for(double d: list) dList.add(d);
		return calculateVariance(dList, mean);
	}
	
	public static double calculateVariance(List<Double> list, double mean){
		if(mean==Double.MAX_VALUE) return mean;
		if(list.size()==1) return 0;
		double sum=0;
		for(double num: list){
			sum+=Math.pow(num-mean, 2);
		}
		return sum/(list.size()-1);
	}
	
	public static double calculatePearsonCorrelation(List<Double> list1, List<Double> list2){
		if(list1.size()!=list2.size()){
			System.err.println("Two lists must have the same dimensionality.");
			return 0;
		}
		double mean1=calculateMean(list1);
		double mean2=calculateMean(list2);
		
		double std1=Math.sqrt(calculateVariance(list1, mean1));
		double std2=Math.sqrt(calculateVariance(list2, mean2));
		
		double dividend=0;
		for(int i=0;i<list1.size();i++){
			dividend+=(list1.get(i)-mean1)*(list2.get(i)-mean2);
		}
		dividend/=list1.size()-1;
			
		//System.out.println(mean1+" "+std1+" "+mean2+" "+std2+" "+dividend);
		return dividend/(std1*std2);
	}
	
	public static double calculateCosineSimilarity(List<Double> list1, List<Double> list2){
		if(list1.size()!=list2.size()){
			System.err.println("Two lists must have the same dimensionality.");
			return 0;
		}
		double dividend=0, divisor1=0, divisor2=0;
	
		for(int i=0;i<list1.size();i++){
			dividend+=list1.get(i)*list2.get(i);
			divisor1+=Math.pow(list1.get(i),2);
			divisor2+=Math.pow(list2.get(i),2);
		}
		return dividend/(Math.sqrt(divisor1)*Math.sqrt(divisor2));		
	}
	

}
