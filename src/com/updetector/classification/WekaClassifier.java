package com.updetector.classification;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.misc.SerializedClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.SerializationHelper;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AddClassification;
import android.R.integer;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.updetector.CommonUtils;
import com.updetector.Constants;
import com.updetector.R;

/**
 * 
 * @author Shuo
 * A wrap class for Weka classifiers 
 *
 */
public class WekaClassifier {
	public static final String LOG_TAG=WekaClassifier.class.getCanonicalName();
	
	private Context mContext;
	
	private int mClassifierName; 
	
	private Classifier mClassifier;
	private Filter  mFilter;
	private Instances mInstances;
	private Attribute mClassAttribute;
	
	//a hashmap that translates the output predicated class to an event
	public HashMap<String, Integer> classierfierClsToEvents;
	
	
	public WekaClassifier(Context ctxt,
			int classifierName,
			String filterName,
			String filterOption,
			HashMap<String, Integer> classierfierClsToEvents){
		//initialize classifier
		try {
			mContext=ctxt;
			mClassifierName=classifierName;
			
			int headFileResourceID=0, classifierModelFileResourceID=0;
			switch(mClassifierName){
			case Constants.ACCEL_CHANGE_IN_VAR:
				headFileResourceID=R.raw.accel_arff_header;
				classifierModelFileResourceID=R.raw.accel_random_forest;			
				break;
			case Constants.ACCEL_MOTION_STATE:
				headFileResourceID=R.raw.state_arff_header;
				classifierModelFileResourceID=R.raw.state_random_forest;
				break;
			case Constants.SENSOR_MICROPHONE:
				headFileResourceID=R.raw.audio_arff_header;
				classifierModelFileResourceID=R.raw.audio_random_forest;
				break;
			default:
				break;
			}

			mInstances=new Instances(new BufferedReader(new InputStreamReader(
					mContext.getResources().openRawResource(headFileResourceID)	)));
			mInstances.setClassIndex(mInstances.numAttributes()-1);
			mClassAttribute=mInstances.classAttribute();
			
			if(!Constants.IS_TRAINING_MODE){
				mClassifier = (RandomForest) SerializationHelper.read(
					mContext.getResources().openRawResource(classifierModelFileResourceID) );			
			}
			
			if(filterName.length()>0){//if filter is not empty
				mFilter = (Filter) Class.forName(filterName).newInstance();
				if (mFilter instanceof OptionHandler)
			      ((OptionHandler) mFilter).setOptions(filterOption.split(" "));
				//mFilter.setInputFormat(mInstances);			
			}
			
			if(Constants.IS_DEBUG){
				Log.d(LOG_TAG, mClassifierName+" # of attrs="+mInstances.numAttributes());
				Log.d(LOG_TAG, mClassAttribute.toString());
			}
			

			
			this.classierfierClsToEvents=classierfierClsToEvents;
			
			if(Constants.IS_DEBUG){				
				Log.d(LOG_TAG, mClassifier.toString());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			Log.e(LOG_TAG, ex.toString());
			//System.exit(0);//comment this line when need to train a model
		}
	}
	
	public String toString(){
		return mClassifierName+"	"+mClassAttribute;
	}
	
	
	/**
	 * 
	 * @param feautres: an instance to be classified
	 * @return a code that represents an event
	 */
	public double[] classify(String features) {
		double[] distr=null;
		
		String[] fields=features.split(",");
		int n=fields.length;		
		
		if(Constants.IS_DEBUG){
			Log.e(LOG_TAG, "n="+n+"  "+features);
		}
		
		Instance in = new Instance(n+1);//class 
		in.setDataset(mInstances); //necessary under android; removable under Windows
		// Set instance's values
		for(int i=0;i<n;i++){
			//Attribute attr=mInstances.attribute(i);
			in.setValue(i, Double.parseDouble(fields[i].trim()) );
			//System.out.println(Double.parseDouble(fields[i])+" "+inst.value(i));
		}		
		
		Instances ins;
		ins=new Instances(mInstances);
		ins.add(in); //add the new instance
		
		
		/*mInstances.delete();// remove the last instance
		ins=mInstances;
		ins.add(in);
		*/
		
		if(Constants.IS_DEBUG){
			Log.e(LOG_TAG, "before fileter:  # of attributes: " + mInstances.numAttributes()+"  cls attr idx=" + mInstances.classIndex());
			if(mFilter!=null) Log.e(LOG_TAG, mFilter.toString());
		}
		
		try {
			Instances filterInstances;
			Instance filteredInst;
			if(mFilter!=null){
				mFilter.setInputFormat(mInstances);	//necessary under android; removable under Windows
				filterInstances = Filter.useFilter(ins, mFilter);
				filteredInst=filterInstances.instance(0);
			}else {
				filterInstances=ins;
				filteredInst=in;
				if(Constants.IS_DEBUG) Log.e(LOG_TAG, "mFileter is null.");
			}
			
			if(Constants.IS_DEBUG){
				Log.e(LOG_TAG, "after fileter:  # of attributes: " + filterInstances.numAttributes()+"  cls attr idx=" + filterInstances.classIndex());
				Log.e(LOG_TAG, "after fileter classification inst= "+ filteredInst.toString()); 
			}
			
			int predClass=(int)mClassifier.classifyInstance(filteredInst);
			distr=mClassifier.distributionForInstance(filteredInst);
			
			//Log.e(LOG_TAG, "MST dist= "+Arrays.toString(distr));
			//Log.e(LOG_TAG,  "MST predClass= "+predClass+"  ==> "+mClassAttribute.value(predClass));
			
			
			//return classierfierClsToEvents.get(mClassAttribute.value(predClass));
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LOG_TAG, e.toString()+"\n something wrong with the "+mClassifierName+" classification");
		}
		return distr;
		//return Constants.OUTCOME_NONE;
	}
	
	
	/**
	 * write a serizable model using the android os
	 * after training needs manaually copy the model file to the res folder
	 * Problem:
	 * http://weka.8497.n7.nabble.com/Problems-Deserializing-RandomForest-model-td26858.html
	 * @param ctxt
	 * @param indexInSenorClassifierArray
	 */
	public void train(){
		try {
			Log.e(LOG_TAG, "ready to train the model");

			//R.raw.audio_combined; 
			int resID=-1;
			
			
			String prefix="";
			switch (mClassifierName) {
			case Constants.ACCEL_CHANGE_IN_VAR:
				resID=R.raw.accel_combined;
				prefix="accel_";
				break;
			case Constants.ACCEL_MOTION_STATE:
				resID=R.raw.state_combined;
				prefix="state_";
				break;
			case Constants.SENSOR_MICROPHONE:
				resID=R.raw.audio_combined;
				prefix="audio_";
				break;
			default:
				break;
			}
			if(resID!=-1){//resID initialized
				Instances instances= new Instances( new BufferedReader(new InputStreamReader(
						mContext.getResources().openRawResource(resID)) ));				
				instances.setClassIndex(instances.numAttributes()-1);//set the class
				Instances filterInstances;
				
				if(mFilter!=null){
					mFilter.setInputFormat(mInstances);	
					filterInstances=Filter.useFilter(instances, mFilter);
				}else {
					filterInstances=instances;
					
				}
				
				//build		
				Classifier classifier=Classifier.forName("weka.classifiers.trees.RandomForest", null);
				Log.e(LOG_TAG, "Right before training the classifier");
				classifier.buildClassifier(filterInstances);		
				
				//output the classifier model
				SerializationHelper.write("/sdcard/"+prefix+"random_forest.model", classifier);
				
				if(Constants.IS_DEBUG){
					Log.d(LOG_TAG, instances.classAttribute().toString());
					Log.e(LOG_TAG, "successfully write to the file");
				}
				
			}
			
		} catch (Exception ex) {
			Log.e(LOG_TAG, ex.toString()+"\n Something wrong when training the model");
		}
		
	    
	}
}
