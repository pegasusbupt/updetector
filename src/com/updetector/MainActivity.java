/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.updetector;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.commons.math3.analysis.function.Constant;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.internal.br;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.XPS;
import com.updetector.MotionState.Type;
import com.updetector.R;
import com.updetector.Constants.REQUEST_TYPE;
import com.updetector.MotionState.Source;
import com.updetector.classification.ClassificationManager;
import com.updetector.classification.WekaClassifier;
import com.updetector.fusion.ConditionalProbability;
import com.updetector.fusion.FusionManager;
import com.updetector.google.activityrecognition.GoogleActivityRecognitionClientRemover;
import com.updetector.google.activityrecognition.GoogleActivityRecognitionClientRequester;
import com.updetector.indicator.accelerometerbased.AccelerometerFeature;
import com.updetector.indicator.iodetector.CellTowerChart;
import com.updetector.indicator.iodetector.DetectionProfile;
import com.updetector.indicator.iodetector.LightChart;
import com.updetector.indicator.iodetector.MagnetChart;
import com.updetector.managers.AudioRecordManager;
import com.updetector.managers.DetectionNotificationManager;
import com.updetector.managers.LogManager;
import com.updetector.managers.WakeLockManager;
import com.updetector.sensorlist.Sensors;
import com.updetector.viewadapters.MarkerInfoWindowAdapter;
import com.updetector.blocksmap.ParkingBlock;
import com.updetector.blocksmap.ParkingBlocks;
import com.updetector.bluetooth.BluetoothConnectionService;

/**
 * Sample application that demonstrates the use of
 * ActivityRecognitionClient}. It registers for activity detection updates
 * at a rate of 20 seconds, logs them to a file, and displays the detected
 * activities with their associated confidence levels.
 * <p>
 * An IntentService receives activity detection updates in the background
 * so that detection can continue even if the Activity is not visible.
 */
public class MainActivity extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener, OnInitListener
 {
	public static final String LOG_TAG=MainActivity.class.getCanonicalName();
	
	private static final String LOCK_TAG="ACCELEROMETER_MONITOR";
	
	
	private static boolean geofencingOn=false;
	/**
	 * UI Widgets
	 */

    // Holds the text view  
    private TextView consoleTextView, environTextView, stateTextView, googleStateTextView;
    public static final String ENVIRONMENT_PREFIX="Environment : ";
    public static final String STATE_PREFIX="Motion State Classified : ";
    public static final String GOOGLE_MOBILITY_STATE_PREFIX="Motion State Google : ";
    public static final String INDICATOR_PREFIX="Indicator : "; 
   
    private GoogleMap mMap;
    Vector<ParkingBlock> parkingBlocks = null;
    
    
    private TextToSpeech mTextToSpeech;
    
    
	/**
     * Holds activity recognition data, in the form of
     * strings that can contain markup
     */
    //private ArrayAdapter<Spanned> mStatusAdapter;
    
    //Instance of a Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;

    /**
     *  Intent filter for incoming broadcasts from the
     *  IntentService.
     */
    IntentFilter mBroadcastFilter;

    // Instance of a local broadcast manager
    private LocalBroadcastManager mBroadcastManager;

    //Instance of a customized location manager
    private LocationManager mLocationManager;
    private LocationClient mLocationClient;
    private XPS mXPSHandler;
    
    // The logger object
    private LogManager mLogManager;

    // Instance of customized notification manager
    private DetectionNotificationManager mDetectionNotificationManager;
    
    // The wake lock manager object
    private WakeLockManager mWakeLockManager;
    
    /**
     * Google Activity Update Fields
     */
	    private GoogleActivityRecognitionClientRequester mGoogleActivityDetectionRequester;
	    private GoogleActivityRecognitionClientRemover mGoogleActivityDetectionRemover;
	    private double[] probOfOnFootAndInVehicleOfLastUpdate=new double[2];
    
    /**
     * MST
     */
  	private PastMotionStates mPastGoogleActivities=new PastMotionStates(Source.Google, Constants.GOOGLE_ACTIVITY_LAST_STATE_NO);
  	private PastMotionStates mPastClassifiedMotionStates=new PastMotionStates(Source.Classifier, Constants.NO_OF_PAST_STATES_STORED);
  	private CachedDetectionList mCachedUnparkingDetectionList=new CachedDetectionList(CachedDetection.Type.Unparking);
  	private CachedDetectionList mCachedParkingDetectionList=new CachedDetectionList(CachedDetection.Type.Parking);
  	
    private double[] lastClassifiedMotionStateDistr=null;
    private double[] lastAccReading;
	    
	    
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
    private AudioRecordManager mAudioRecordManager;
    private FusionManager mFusionManager;
    
    /**
     * Detection Interval Fields
     */
    private long lastParkingTimestamp=-1;
    private long lastUnparkingTimestamp=-1;
    
    /**
     * IODetector fields
     */
	    private CellTowerChart cellTowerChart;
	    private LightChart lightChart;
	    private MagnetChart magnetChart;
	    private Handler mIODectorHandler;
		private boolean aggregationFinish = true;
		private boolean phoneNotStill = false;
	    private int lastEnvironment=Constants.ENVIRON_UNKNOWN;
	    private double probabilityOfLastEnvironment;
	    private ArrayList<Integer> pastEnvironments=new ArrayList<Integer>();
	    

	      
	
	    
	/**
	 * Indicator Fusion
	 */
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer, ArrayList<Double>> lastVectors=new HashMap<Integer, ArrayList<Double>>();
   
	
	    
    // The classification manager object
    private ClassificationManager mClassificationManager;
	
    // Store the current request type (ADD or REMOVE)
    private REQUEST_TYPE mRequestType;
    
	
	public class LocationClientListener implements LocationListener{
		int eventCode;
		double detectionConfidence;
		long requestTimestamp;
		
		public LocationClientListener(int eventCode, double confidence, long timestamp){
			this.eventCode=eventCode;	
			this.detectionConfidence=confidence;
			this.requestTimestamp=timestamp;
		}
	

		@Override
		public void onLocationChanged(Location location) {
//			new GetAddressTask(eventCode, requestTimestamp,detectionConfidence).execute(location);
			onLocationRetrieved(eventCode, detectionConfidence, requestTimestamp, location, null);
		}
	}
	
	/**
	 * A subclass of AsyncTask that calls getFromLocation() in the background.
	 * The class definition has these generic types: Location - A Location
	 * object containing the current location. Void - indicates that progress
	 * units are not used String - An address passed to onPostExecute()
	 */
	private class GetAddressTask extends AsyncTask<Location, Void, String> {
		int eventCode;
		long locationRequestTimestamp;
		double detectionConfidence;
		long locationRetrievedTimestamp;//addressRequestTimestamp;
		Location mLocation;
		
		
		public GetAddressTask(int eventCode, long locationRequestTimestamp, double confidence) {
			super();
			this.eventCode=eventCode;	
			this.detectionConfidence=confidence;
			this.locationRequestTimestamp=locationRequestTimestamp;
			this.locationRetrievedTimestamp=System.currentTimeMillis();
		}

		/**
		 * Get a Geocoder instance, get the latitude and longitude look up the
		 * address, and return it
		 * 
		 * @params params One or more Location objects
		 * @return A string containing the address of the current location, or
		 *         an empty string if no address can be found, or an error
		 *         message
		 */
		@Override
		protected String doInBackground(Location... params) {
			Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
			// Get the current location from the input parameter list
			Location loc = params[0];
			mLocation=loc;
			
			// Create a list to contain the result address
			List<Address> addresses = null;
			try {
				/*
				 * Return 1 address.
				 */
				addresses = geocoder.getFromLocation(loc.getLatitude(),
						loc.getLongitude(), 1);
			} catch (IOException e1) {
				Log.e("LocationSampleActivity",
						"IO Exception in getFromLocation()");
				e1.printStackTrace();
				return ("IO Exception trying to get address");
			} catch (IllegalArgumentException e2) {
				// Error message to post in the log
				String errorString = "Illegal arguments "
						+ Double.toString(loc.getLatitude()) + " , "
						+ Double.toString(loc.getLongitude())
						+ " passed to address service";
				Log.e("LocationSampleActivity", errorString);
				e2.printStackTrace();
				return errorString;
			}
			// If the reverse geocode returned an address
			if (addresses != null && addresses.size() > 0) {
				// Get the first address
				Address address = addresses.get(0);
				/*
				 * Format the first line of address (if available), city, and
				 * country name.
				 */
				String addressText = String.format(
						"%s, %s, %s",
						// If there's a street address, add it
						address.getMaxAddressLineIndex() > 0 ? address
								.getAddressLine(0) : "",
						// Locality is usually a city
						address.getLocality(),
						// The country of the address
						address.getCountryName());
				// Return the text
				return addressText;
			} else {
				return "No address found";
			}
		}
		
		/**
         * A method that's called once doInBackground() completes. Turn
         * off the indeterminate activity indicator and set
         * the text of the UI element that shows the address. If the
         * lookup failed, display the error message.
         */
        @Override
        protected void onPostExecute(String address) {
        	// Display the results of the lookup.
        	onLocationRetrieved(eventCode, detectionConfidence,locationRequestTimestamp, mLocation, address);
        }
	} 
		
	
	/** Callback when a message is sent from some service */
	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
	    @SuppressLint("UseSparseArrays")
		@Override
	    public void onReceive(Context context, Intent intent) {
	    	String action=intent.getAction();
	    	Log.e(LOG_TAG, action);
	    	if(action.equals(Constants.BLUETOOTH_CONNECTION_UPDATE)) {
	        	int eventCode = intent.getIntExtra(Constants.BLUETOOTH_CON_UPDATE_EVENT_CODE, Constants.OUTCOME_NONE);
	        	System.out.println(eventCode);
				
				//Toast.makeText(getApplicationContext(), "\ndetected "+CommonUtils.eventCodeToString(eventCode)+" at time: "+CommonUtils.formatTimestamp( new Date(), "HH:mm:ss" ), Toast.LENGTH_LONG).show();

	        	
	        	// retrieve a location then
	        	/*mXPSHandler.getLocation(null
						 ,WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP
						 , new XPSLocationCallback(eventCode) );      */  
	        	
	        	mLocationClient.requestLocationUpdates( LocationRequest
	    				.create().setNumUpdates(1).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY), 
	    				new LocationClientListener(eventCode, 1.0, System.currentTimeMillis())); 
	        	
	        }else{
	        	//TODO return from Google activity update
	        	if(action.equals(Constants.GOOGLE_ACTIVITY_RECOGNITION_UPDATE)){
	        		String mostLikelyActivity=intent.getStringExtra(Constants.GOOGLE_ACT_UPDATE_MOST_LIKELY_ACTIVITY_TYPE);
	        		float mostLikelyActivityConfidence=intent.getFloatExtra(Constants.GOOGLE_ACT_UPDATE_MOST_LIKELY_ACTIVITY_CONFIDENCE, 0);
	        		float onFootConfidence=intent.getFloatExtra(Constants.GOOGLE_ACT_UPDATE_ON_FOOT_ACTIVITY_CONFIDENCE, 0);
	        		float inVehicleConfidence=intent.getFloatExtra(Constants.GOOGLE_ACT_UPDATE_IN_VEHICLE_ACTIVITY_CONFIDENCE, 0);
	        		
	        		//little correction to the UNKNOWN state
	        		int mostLikelyActivityType=intent.getIntExtra(Constants.GOOGLE_ACT_UPDATE_MOST_LIKELY_ACTIVITY_TYPE_INT, DetectedActivity.UNKNOWN);
	        		if(mostLikelyActivityType==DetectedActivity.UNKNOWN){
	        				if(inVehicleConfidence>100-inVehicleConfidence-mostLikelyActivityConfidence) 
	        					mostLikelyActivityType=DetectedActivity.IN_VEHICLE;
	        				else{
	        					if(onFootConfidence>100-onFootConfidence-mostLikelyActivityConfidence)
	        						mostLikelyActivityType=DetectedActivity.ON_FOOT;
	        				}
	        		}
	        		MotionState.Type activityType=MotionState.translate(mostLikelyActivityType);
	        		mPastGoogleActivities.add(activityType);
	        			
	        		if(activityType==MotionState.Type.IN_VEHICLE
	        	||activityType==MotionState.Type.ON_FOOT){
	        			int outcome;
	        			CachedDetection NotExpiredCachedDetectionWithLargestConfidence=null;
	        			if(activityType==MotionState.Type.IN_VEHICLE){
	        				outcome=Constants.OUTCOME_UNPARKING;
	        				NotExpiredCachedDetectionWithLargestConfidence=mCachedUnparkingDetectionList.getWithLargestProbability();
	        			}else{
	        				outcome=Constants.OUTCOME_PARKING;
	        				NotExpiredCachedDetectionWithLargestConfidence=mCachedParkingDetectionList.getWithLargestProbability();
	        			}
		        
		        		if(mPastGoogleActivities.isTransitionTo(activityType)){
		        				if(NotExpiredCachedDetectionWithLargestConfidence!=null){
		        						onDetectionConfirmed(outcome,
			        					 NotExpiredCachedDetectionWithLargestConfidence.timestamp,
			        					 NotExpiredCachedDetectionWithLargestConfidence.location,
			        					 NotExpiredCachedDetectionWithLargestConfidence.address);
		        				}else{//no earlier cached result, request a new location
		        					mLocationClient.requestLocationUpdates( 
		        			    			LocationRequest.create()
		        			    			.setNumUpdates(1)
		        			    			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY), 
		        							new LocationClientListener(outcome, 1.0, System.currentTimeMillis()));
		        				}
		        		}

	        		}
	        		
	        		
	        		//update the textview
	        		googleStateTextView.setText(GOOGLE_MOBILITY_STATE_PREFIX+mostLikelyActivity+" conf:"+mostLikelyActivityConfidence
	        				+ "  f:"+onFootConfidence+",v:"+inVehicleConfidence);
	        		
	        		//log the update
	        		SharedPreferences mPrefs=getSharedPreferences(Constants.SHARED_PREFERENCES, 0);
	    			boolean logOn=mPrefs.getBoolean(Constants.LOGGING_ON, false);
	        		boolean logGoogActUpdatesOn=mPrefs.getBoolean(Constants.LOGGING_GOOGLE_ACTIVITY_UPDATE, false);
					if(logOn&&logGoogActUpdatesOn){
						// log the Change-In-Variance Classifier predicated result
						mLogManager.log(
								new SimpleDateFormat("yyyy-MM-dd kk:mm:ss:SSS", Locale.US).format(Calendar.getInstance().getTime())+" "+activityType.toString(),
								Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_GOOGLE_ACT_UPDATE]);
					}
	        		
	        		
	        		
	        		//build the new MST vector
	        		/*double[] probsOfNewUpdate=null;
	        		if(probOfOnFootAndInVehicleOfLastUpdate!=null){
	        			probsOfNewUpdate=new double[]{onFootConfidence/100, inVehicleConfidence/100};
	        			ArrayList<Double> features=new ArrayList<Double>();
	        			features.add(probOfOnFootAndInVehicleOfLastUpdate[0]);
	        			features.add(probOfOnFootAndInVehicleOfLastUpdate[1]);
	        			features.add(probsOfNewUpdate[0]);
	        			features.add(probsOfNewUpdate[0]);
	        			
	        			HashMap<Integer, ArrayList<Double>> mstVector=new HashMap<Integer, ArrayList<Double>>();
	        			mstVector.put(Constants.INDICATOR_MST, features);
	        			Log.d(LOG_TAG, "Google MST Vector: "+features.toString());
	        		}
	        		probOfOnFootAndInVehicleOfLastUpdate=probsOfNewUpdate;*/
	        	}
	        }
	    }
	};
	
	//accelerometer feature window and its neighboring windows
	private ArrayList<AccelerometerFeature> civVectorsWithinScope=new ArrayList<AccelerometerFeature>();
	 
    
    //TODO mSensnorEvent
    @SuppressLint("UseSparseArrays")
    public static long acceleometerSeq=0;
	private final SensorEventListener mSensorEventListener = new SensorEventListener() 
	  {
		@SuppressLint("UseSparseArrays")
		public void onSensorChanged(SensorEvent event) 
		{
			// check if the accelerometer readings have changed since last sample
			boolean readingChanged=false;
			for(int i=0;i<event.values.length;i++){
				if(event.values[i]!=lastAccReading[i]){
					readingChanged=true; 
					lastAccReading[i]=event.values[i];
				}
			}
			if(!readingChanged) return;						
			acceleometerSeq=(acceleometerSeq+1)%Integer.MAX_VALUE;
	        
			
			// requires a wake lock 
	        mWakeLockManager.lock(LOCK_TAG);
				
			//initialization 
			SharedPreferences mPrefs=getSharedPreferences(Constants.SHARED_PREFERENCES, 0);
			int mostLikelyOutcome=Constants.OUTCOME_NONE;
			double mostLikelyOutcomeProb=1;
			
			
			/**
			 * Log  raw reading option
			 */
			boolean logOn=mPrefs.getBoolean(Constants.LOGGING_ON, false);
			boolean logRawOn=mPrefs.getBoolean(Constants.LOGGING_ACCL_RAW_SWITCH, false);
			if(logOn&&logRawOn){
				String record=CommonUtils.buildALogRecordForNewAccelerometerReadings(event);
				if(record!=null) phoneNotStill=true;
				else phoneNotStill=false;
				mLogManager.log(record, Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_ACCEL_RAW]);
			}			
			
			
			//stop analyzing accelerometer based on environment conditions
			boolean manuallySetToOutdoor=mPrefs.getBoolean(Constants.PREFERENCE_KEY_IS_OUTDOOR, false);
			if( !manuallySetToOutdoor&&
				(lastEnvironment==Constants.ENVIRON_INDOOR&&probabilityOfLastEnvironment>0.8)
				//|| !pastMotionStates.contains((Integer)Constants.STATE_DRIVING)
			){
					return;
			}
			
				
			//boolean useGoogleActivityInFusion=mPrefs.getBoolean(Constants.PREFERENCE_KEY_USE_GOOGLE_ACTIVITY_IN_FUSION, false);

			/**
			 * MST Classifier And Fusion
			 */
			/*AccelerometerFeature motionStateFeatures=mClassificationManager.mMSTFeatureExtraction.extractWindowFeature(event);
			if(motionStateFeatures!=null){
				String motionStateInstance=motionStateFeatures.asStringForMotationState();
				WekaClassifier motionStateClassifier=mClassificationManager.mClassfiers.get(Constants.ACCEL_MOTION_STATE); 
				double[] distr=motionStateClassifier.classify(motionStateInstance);
				Log.e(LOG_TAG, "motion state classifier output is : " +Arrays.toString(distr));
				
				*//**
				 * Get the motion state with largest probability
				 *//*
				int predClassIdx=CommonUtils.idxOfMax(distr);
				if(predClassIdx!=-1){
					String predClass=Constants.CLASSIFIER_CLASS[1][predClassIdx];
					if(!phoneNotStill) predClass="Still";
					Log.e(LOG_TAG, "cur motion state="+predClass);
					stateTextView.setText(STATE_PREFIX+predClass);
					mPastClassifiedMotionStates.add(MotionState.translate(predClass));
				}
				
				//early exit based on state
				if(//!mPastGoogleActivities.containsAtLeastOneWalkingAndOneParking()
						!mPastClassifiedMotionStates.containsAtLeastMOnFootAndAtLeastNInVehicleStates(1,1)
						) return;
					
				if(lastClassifiedMotionStateDistr!=null){
					//build the vector of the MST indicator
					ArrayList<Double> mstVector=new ArrayList<Double>();
					mstVector.add(lastClassifiedMotionStateDistr[0] );
					mstVector.add(lastClassifiedMotionStateDistr[1]);
					mstVector.add(distr[0]);
					mstVector.add(distr[1]);
					Log.e(LOG_TAG, acceleometerSeq+" new mst vector is :"+mstVector.toString());					
					HashMap<Integer, ArrayList<Double>> newPeriodicalVector=new HashMap<Integer, ArrayList<Double>>();
					newPeriodicalVector.put(Constants.INDICATOR_MST, mstVector);
					outcome=mFusionManager.fuse(lastVectors, newPeriodicalVector, System.currentTimeMillis(), Constants.HIGH_LEVEL_ACTIVITY_UPARKING, mLogManager);
				}
				lastClassifiedMotionStateDistr=distr;
				//lastMotionStateDistr=new double[distr.length];
				//for(int ii=0;ii<distr.length;ii++) lastMotionStateDistr[ii]=distr[ii];
				
			}
			else
				if(//!mPastGoogleActivities.containsAtLeastOneWalkingAndOneParking()
						!mPastClassifiedMotionStates.containsAtLeastMOnFootAndAtLeastNInVehicleStates(1,1)
						) 
					return;*/
			
			
			AccelerometerFeature civFeatures=mClassificationManager.mCIVFeatureExtraction.extractWindowFeature(event);
			if(civFeatures!=null){
				//get the vector of the Change-In-Variance features 
				String civVector=mClassificationManager.mCIVFeatureExtraction.extractCIVVector(civFeatures, civVectorsWithinScope);
				if( civVector!=null){
					Log.e(LOG_TAG, "accelerometerSeq="+acceleometerSeq+" new civ vector is : "+civVector);
					
					
						/**
						 * calculate the probability of the outcome
						 */
					boolean classifierForCIVOn=mPrefs.getBoolean(Constants.PREFERENCE_KEY_CIV_CLASSIFIER_ON, false);	
					if(!classifierForCIVOn){
							HashMap<Integer, ArrayList<Double>> newPeriodicalVector=new HashMap<Integer, ArrayList<Double>>();
							newPeriodicalVector.put(Constants.INDICATOR_CIV,CommonUtils.stringToDoubleListRemoved(civVector, ",", new int[]{0}) );
							double[] res=mFusionManager.fuse(lastVectors, newPeriodicalVector, System.currentTimeMillis(),Constants.HIGH_LEVEL_ACTIVITY_UPARKING, mLogManager);
							mostLikelyOutcome=(int)res[0];
							mostLikelyOutcomeProb=res[1];
						}
						/**
						 * classify the vector of the Change-In-Variance vectors
						 */
						else{
							WekaClassifier changeInVarianceClassifier=mClassificationManager.mClassfiers.get(Constants.ACCEL_CHANGE_IN_VAR);
							double[] distr=changeInVarianceClassifier.classify(civVector);
							
							int predClassInt=CommonUtils.idxOfMax(distr);
							String predClass=",n";
							
							switch(predClassInt){
							case Constants.CIV_SIGNI_INCREASE:
							case Constants.CIV_SIGNI_DECREASE:						
								 //log the feature
								 if(predClassInt==Constants.CIV_SIGNI_INCREASE){
									 predClass=",p";
									 mostLikelyOutcome=Constants.OUTCOME_PARKING;
								 }
								 else{
									 predClass=",u";
									 mostLikelyOutcome=Constants.OUTCOME_UNPARKING;
								 }
							break;
							case Constants.STATE_STILL:
								// log the feature
								predClass=",t";
								//release the lock
								mWakeLockManager.unlock(LOCK_TAG);
								mostLikelyOutcome=Constants.OUTCOME_NONE;
								break;
							default: 
								mostLikelyOutcome=Constants.OUTCOME_NONE;
								break;
							}	
							Log.e(LOG_TAG, "predClass="+predClass);
							
						}
				}
			}
			
			
			//boolean logDetectionOn=mPrefs.getBoolean(Constants.LOGGING_DETECTION_SWITCH, false);
			
			switch(mostLikelyOutcome){
			case Constants.OUTCOME_PARKING:
			case Constants.OUTCOME_UNPARKING:
				//long notificationInterval=mPrefs.getInt(Constants.PREFERENCE_KEY_DETECTION_INTERVAL, Constants.DETECTION_INTERVAL_DEFAULT_VALUE);
				//long lastSameTypeDetectionTimestamp;
				//if(outcome==Constants.OUTCOME_PARKING) lastSameTypeDetectionTimestamp=lastParkingTimestamp;
				//else lastSameTypeDetectionTimestamp=lastUnparkingTimestamp;
				
				//if(System.currentTimeMillis()-lastSameTypeDetectionTimestamp>notificationInterval*Constants.ONE_SECOND_IN_MILLISECOND){
					//if(outcome==Constants.OUTCOME_PARKING) lastParkingTimestamp=System.currentTimeMillis();
					//else lastUnparkingTimestamp=System.currentTimeMillis();
					
					 /*mXPSHandler.getLocation(null
					 ,WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP
					 , new XPSLocationCallback(eventCode) );*/
					mLocationClient.requestLocationUpdates( 
	    			LocationRequest.create()
	    			.setNumUpdates(1)
	    			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY), 
					new LocationClientListener(mostLikelyOutcome, mostLikelyOutcomeProb, System.currentTimeMillis())
	    			);
				//}
				break;
			case Constants.OUTCOME_NONE:
				/*if(logOn){
					if(logDetectionOn){
						mLogManager.log("outcome="+outcome+"\n"+mFusionManager.fusionProcessLog.toString()+"\n", Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_DETECTION_REPORT]);
					}
				}*/
				break;
			default:
				break;
			}			
			
	    }
	      public void onAccuracyChanged(Sensor sensor, int accuracy) { }
	  };
    
	/**
     * A single callback class that will be used to handle
     * all location notifications sent by WPS.
     */
    private class XPSLocationCallback implements WPSLocationCallback
    {
    	private int eventCode;    	
    	public XPSLocationCallback(int eventCode) {
    		this.eventCode=eventCode;
    	}    	
        public void done(){           
        }
        public WPSContinuation handleError(final WPSReturnCode error)
        {
        	// To retry the location call on error use WPS_CONTINUE,
    		// otherwise return WPS_STOP
        	Log.e(LOG_TAG, "WPS API return error "+error.toString());
            //return WPSContinuation.WPS_CONTINUE;
        	return WPSContinuation.WPS_STOP;
        }

		@Override
		public void handleWPSLocation(WPSLocation location) {
			//actionsOnParkingLocation(eventCode, (Location) location);
		}
    } 
    
    /*
     * Set main UI layout, get a handle to the ListView for logs, and create the broadcast
     * receiver.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Set the views
         */
	        // Set the main layout
	        setContentView(R.layout.activity_main);
	        
	        // get a handle to the console textview
			consoleTextView = (TextView) findViewById(R.id.console_text_id);
			consoleTextView.setMovementMethod(new ScrollingMovementMethod());
			
			//setup monitoring fields
			environTextView=(TextView) findViewById(R.id.environment);
			environTextView.setText(ENVIRONMENT_PREFIX+CommonUtils.eventCodeToString(lastEnvironment));
			stateTextView=(TextView) findViewById(R.id.state);
			stateTextView.setText(STATE_PREFIX+"unknown");
			googleStateTextView=(TextView) findViewById(R.id.google_state);
			googleStateTextView.setText(GOOGLE_MOBILITY_STATE_PREFIX+"unknown");
			
			//indicatorTextView=(TextView) findViewById(R.id.indicator);
			//indicatorTextView.setText(INDICATOR_PREFIX);
			
			// set up the map view
			setupMapIfNeeded();
			//set up the location client
			setupLocationClientIfNeeded();
			
			/**
			 * set up color coded map
			 */
			if (parkingBlocks == null) {
				parkingBlocks = ParkingBlocks.GetParkingBlocks();
				showAvailabilityMap();
			}
			
		
		mTextToSpeech = new TextToSpeech(this, this);
			

        /*
         * Initialize managers
         */
	        // Instantiate an adapter to store update data from the log
/*	        mStatusAdapter = new ArrayAdapter<Spanned>(
	                this,
	                R.layout.item_layout,
	                R.id.log_text
	        );*/
	
	        // Set the broadcast receiver intent filer
	        mBroadcastManager = LocalBroadcastManager.getInstance(this);
	
	        // Create a new Intent filter for the broadcast receiver
	        mBroadcastFilter = new IntentFilter(Constants.ACTION_REFRESH_STATUS_LIST);
	        mBroadcastFilter.addCategory(Constants.CATEGORY_LOCATION_SERVICES);
	        mBroadcastFilter.addAction(Constants.BLUETOOTH_CONNECTION_UPDATE);
	        mBroadcastFilter.addAction(Constants.GOOGLE_ACTIVITY_RECOGNITION_UPDATE);
	        mBroadcastManager.registerReceiver(mBroadcastReceiver, mBroadcastFilter);
	
	       
	        // Get the instance of the customized notification manager
	        mDetectionNotificationManager=DetectionNotificationManager.getInstance(this);
	        
	        //Get the FusionManager object
	        mFusionManager=new FusionManager(this);
	        
	        // Get the ClassificationManager object
	        mClassificationManager=ClassificationManager.getInstance(this);
	        
	        // train the classification models
	        if(Constants.IS_TRAINING_MODE){
	        	//TODO  train classifiers if necessary
	        	int[] classifiersToBeTrained={Constants.ACCEL_MOTION_STATE};	
	        	for(int classifier: classifiersToBeTrained){
	        		mClassificationManager.mClassfiers.get(classifier).train();
	        	}
	        }
	        
	        
	        //get the sensor service
	        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
	        //get the accelerometer sensor
	        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);	        
	        mSensorManager.registerListener(mSensorEventListener,mAccelerometer, 
	        		SensorManager.SENSOR_DELAY_NORMAL);
	        
	        mAudioRecordManager=AudioRecordManager.getInstance();
	        
	        // Get the WakeLockManager object
	        mWakeLockManager=WakeLockManager.getInstance(this);
	        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    	
	    	
	        // Get the LogManager object
	        mLogManager = LogManager.getInstance(this);
	    	mLocationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
	    	mXPSHandler=new XPS(this);
	    	mXPSHandler.setRegistrationUser(new WPSAuthentication("dbmc", "uic"));
	    	//mXPSHandler.setTiling("", 0, 0,  null);

	   
	   /**
	    * Initialize IODetector
	    */
	    cellTowerChart = new CellTowerChart((TelephonyManager)getSystemService(TELEPHONY_SERVICE),this);
		magnetChart = new MagnetChart(mSensorManager,this);	
		lightChart = new LightChart(mSensorManager,this);   
		
		//new AggregatedIODetector().execute("");//Check the detection for the first time
		
		//This timer handle starts the aggregated calculation for the detection
 		//Interval 1 seconds.
		Timer uiTimer = new Timer();
		mIODectorHandler = new Handler();
		
		/*uiTimer.scheduleAtFixedRate(new TimerTask() {
			private int walked = 0;
			@Override
			public void run() {
				mIODectorHandler.post(new Runnable() {
					@Override
					public void run() {
						if(phoneNotStill){//Check if the user is walking
							walked++;
						}
						else{
							walked = 0;
						}
						if(aggregationFinish && walked > 3){//Check if the user has walked for at least 3 second, and the previous calculation has been finish
							aggregationFinish = false;
							walked = 0;
							new AggregatedIODetector().execute("");
						}

					}
				});
			}
		}, 0, 1000);*/
		
		
	   /**
        * Initialize fields other than managers
        */
	        lastAccReading=new double[3]; 
	        
	    /**
         * Startup routines
         */
		    // catch the force close error
		    Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MainActivity.this));
	       
	    
		    /**
		     * Start Google Activity Recognition
		     */
		    mGoogleActivityDetectionRequester = new GoogleActivityRecognitionClientRequester(this);
		    mGoogleActivityDetectionRemover = new GoogleActivityRecognitionClientRemover(this);
		    startGoogleActivityRecognitionUpdates(null);
        
	    
		    checkGPSEnabled();
	    
	    //TODO 	test record sample
	    //mAudioRecordManager.recordAudioSample("/sdcard/audio.wav");
	    
	    //Test extract features from audio files
        //String features=AudioFeatureExtraction.extractFeatures(this, "/sdcard/bus6.wav");
	    //mClassificationManager.mClassfiers.get(Constants.SENSOR_MICROPHONE).classify(features);
	    
    }
    
    
    
    
  /**
   * TODO
   * This class is to handle the Aggregated detection
   */
    private class AggregatedIODetector extends AsyncTask<String, Void, String>{

  		private DetectionProfile lightProfile[];
  		private DetectionProfile cellProfile[];
  		private DetectionProfile magnetProfile[];

  		private double[] normalizedProbablities;
  		private double[] featureValues;
  		
  		@SuppressLint({ "UseSparseArrays", "SimpleDateFormat" })
		@Override
  		protected String doInBackground(String... param) {
  			cellTowerChart.updateProfile();//get the cell info at time = 0
  			for(int i=0;i<10;i++){//get the value for the magnet at the interval of 1s for 10s
  				try {
  					magnetChart.updateProfile();
  					Thread.sleep(1000);
  				} catch (Exception e) {
  				}
  			}
  			//time = 10s
  			lightProfile = lightChart.getProfile();//get the result from the light sensor
  			magnetProfile = magnetChart.getProfile();//get the result from the magnet
  			cellProfile = cellTowerChart.getProfile();//get the result from the cell tower
  			
  			/**
  			 * Weighted Average to combine different indicators
  			 */
  			/*normalizedProbablities=new double[3];//indoor, semi, outdoor
  			Log.i("profile", "light indoor " + lightProfile[0].getConfidence() + " semi " + lightProfile[1].getConfidence() + " outdoor " + lightProfile[2].getConfidence());
  			Log.i("profile","magnet indoor " + magnetProfile[0].getConfidence() + " semi " + magnetProfile[1].getConfidence() + " outdoor " + magnetProfile[2].getConfidence());
  			Log.i("profile","cell indoor " + cellProfile[0].getConfidence() + " semi " + cellProfile[1].getConfidence() + " outdoor " + cellProfile[2].getConfidence());
  			
  			for(int i=0;i<normalizedProbablities.length;i++){
  				//Aggregate the result
  	  			normalizedProbablities[i] = lightProfile[i].getConfidence()*Constants.IODETECTOR_WEIGHT_LIGHT
  	  					+ magnetProfile[i].getConfidence()*Constants.IODETECTOR_WEIGHT_MAGNET
  	  					+ cellProfile[i].getConfidence()*Constants.IODETECTOR_WEIGHT_CELLULAR;
  			}
  			double sum=0;
  			for(int i=0;i<normalizedProbablities.length;i++) sum+=normalizedProbablities[i];
  			for(int i=0;i<normalizedProbablities.length;i++) normalizedProbablities[i]/=sum;*/
  			
  			/**
  			 * Bayesian Data Fusion
  			 */
  			int[] outcomes={Constants.ENVIRON_INDOOR, Constants.ENVIRON_OUTDOOR};
  			HashMap<Integer, ArrayList<Double>> vectorsToBeFused=new HashMap<Integer, ArrayList<Double>>();
  			ArrayList<Double> lightVector=new ArrayList<Double>();
  			ArrayList<Double> RSSVector=new ArrayList<Double>();
  			ArrayList<Double> magneticVector=new ArrayList<Double>();
  			Calendar calendar = Calendar.getInstance();

  			featureValues=new double[3];
  			if(lightChart.getLigthValue()>0){//not blocked
	  			int hour = calendar.get(Calendar.HOUR_OF_DAY);
				if(hour>=8 && hour<=17)	vectorsToBeFused.put(Constants.INDICATOR_LIGHT_DAY, lightVector); 
				else vectorsToBeFused.put(Constants.INDICATOR_LIGHT_NIGHT, lightVector);
				lightVector.add((double)lightChart.getLigthValue());
				featureValues[0]=lightVector.get(0);
			}
  			vectorsToBeFused.put(Constants.INDICATOR_RSS, RSSVector); 
  			RSSVector.add(cellTowerChart.currentASU);
  			featureValues[1]=RSSVector.get(0);  			
  			vectorsToBeFused.put(Constants.INDICATOR_MAGNETIC, magneticVector); 
  			magneticVector.add(magnetChart.magnetVariation);
  			featureValues[2]=magneticVector.get(0);
  			
  			normalizedProbablities=mFusionManager.BayesianFusion(outcomes, vectorsToBeFused,Constants.HIGH_LEVEL_ACTIVITY_IODOOR, mLogManager);
  			Log.d(LOG_TAG, "Baysian fusion Environment: "+Arrays.toString(normalizedProbablities));
  			  			  			
  			
  			//For logging purposes only  		
  			SharedPreferences sp=getSharedPreferences(Constants.SHARED_PREFERENCES, 0);
  			boolean logEnvironOn=sp.getBoolean(Constants.LOGGING_ENVIRON_SWITCH, false);
  			boolean logOn=sp.getBoolean(Constants.LOGGING_ON, false);
  			if(logOn&&logEnvironOn){
  				mLogManager.log(
  						new SimpleDateFormat("HH:mm:ss").format(new Date(System.currentTimeMillis()))+","+
  						lightChart.getLigthValue()+","+magnetChart.magnetVariation+","+cellTowerChart.currentASU
  			, Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_ENVIRONMENT]);
  			}
  			
  			
  			return null;
  		}
  		//After calculation has been done, post the result to the user
  		@Override
  		protected void onPostExecute(String result2) { 
  			if(normalizedProbablities[0] > normalizedProbablities[1]
  				//	&& normalizedProbablities[0] >= normalizedProbablities[1]
  				){//Indoor
  				lastEnvironment =Constants.ENVIRON_INDOOR;//updating the condition for the comparison graph
  				probabilityOfLastEnvironment=normalizedProbablities[0];
  				//notifyUser(view ,"You are in indoor",R.drawable.indoor_icon, 1);//triggering the notification
  				cellTowerChart.setPrevStatus(0);//set the status for the cell tower, to be used for checking previous status when unchanged.
  			}else{ 
  				/*if (normalizedProbablities[1] >normalizedProbablities[0] && normalizedProbablities[1] > normalizedProbablities[2]){//Semi outdoor
	  				lastEnvironment =Constants.ENVIRON_SEMI_OUTDOOR;
	  				probabilityOfLastEnvironment=normalizedProbablities[1];
	  				cellTowerChart.setPrevStatus(1);
		  		}else{//Outdoor
*/	  				lastEnvironment = Constants.ENVIRON_OUTDOOR;
	  				probabilityOfLastEnvironment=normalizedProbablities[1];
	  				cellTowerChart.setPrevStatus(2);
	  			//}
  			}
  			
  			if(pastEnvironments.size()==Constants.NO_OF_PAST_STATES_STORED){
  				pastEnvironments.remove(0);
  			}
  			pastEnvironments.add(lastEnvironment);
  			String environText=ENVIRONMENT_PREFIX+CommonUtils.eventCodeToString(lastEnvironment);
  			if(Constants.IS_DEBUG){
  				for(int i=0;i<normalizedProbablities.length;i++){
  					environText+=" "+String.format("%.2f", normalizedProbablities[i]);
  				}
  			}
  			
  			environTextView.setText(environText+"  "
   					+"light:"+String.format("%.1f", featureValues[0])
   					+", RSS:"+String.format("%.1f", featureValues[1]));
  			aggregationFinish = true;//calculation finish
  		}
  	}
    
  	
  	
  	
    @Override
    protected void onResume() {
    	Log.d(LOG_TAG, LOG_TAG+" activity has resumed");
    	super.onResume();
        
        // restore the mapview        
        setupMapIfNeeded();
    	
        // Register the broadcast receiver
        //mBroadcastManager.registerReceiver(updateListReceiver, mBroadcastFilter);
        
        // Register the sensor event listencer
        //mSensorManager.registerListener(mSensorEventListener,mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        // restore the GPS and bluetooth setting
        //checkGPSEnabled();       
    }
    
    @Override
    protected void onStart(){
    	Log.e(LOG_TAG, LOG_TAG+" activity has started");
    	super.onStart();
    }
    
    @Override
    protected void onPause() {
    	Log.e(LOG_TAG, LOG_TAG+" activity has paused");
        super.onPause();
    	
        // Stop listening to broadcasts when the Activity isn't visible.
        //mBroadcastManager.unregisterReceiver(updateListReceiver);
       
      /*  if (mLocationClient != null) {
            mLocationClient.disconnect();
        }*/
    }
  
    @Override
    protected void onStop(){
    	Log.e(LOG_TAG, LOG_TAG+" activity has stopped");
    	super.onStop();
    }
    
    
    @Override
    protected void onDestroy() {
/*        if (mTextToSpeech != null) {
        	mTextToSpeech.stop();
        	mTextToSpeech.shutdown();
        }*/
        super.onDestroy();
     }

    /*
     * Create the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    /**
	 * Handle Performance Tuning Click
	 */
    private void handleAdvancedSetting(){
    	final Dialog dialog = new Dialog(this);
		dialog.setTitle(R.string.menu_item_advanced_settings);
		dialog.setContentView(R.layout.advanced_setting);
		
		final SharedPreferences mPrefs = getSharedPreferences(Constants.SHARED_PREFERENCES,  Context.MODE_PRIVATE);
		final Editor editor=mPrefs.edit();
		
		final ToggleButton classifierForCIVOnButton=(ToggleButton)dialog.findViewById(R.id.civ_classifier_on);
		classifierForCIVOnButton.setChecked(mPrefs.getBoolean(Constants.PREFERENCE_KEY_CIV_CLASSIFIER_ON, false));
		
		final ToggleButton isOutdoorButton=(ToggleButton)dialog.findViewById(R.id.is_outdoor);
		isOutdoorButton.setChecked(mPrefs.getBoolean(Constants.PREFERENCE_KEY_IS_OUTDOOR, false));
		
		final EditText notificationTresholdText=(EditText)dialog.findViewById(R.id.notification_threshold);
		notificationTresholdText.setText(String.format("%.2f", mPrefs.getFloat(Constants.PREFERENCE_KEY_NOTIFICATION_THRESHOLD, (float)Constants.DEFAULT_DETECTION_THRESHOLD)) );
		
		//final EditText detectionIntervalText=(EditText)dialog.findViewById(R.id.detection_interval);
		//detectionIntervalText.setText(String.valueOf(mPrefs.getInt(Constants.PREFERENCE_KEY_DETECTION_INTERVAL, Constants.DETECTION_INTERVAL_DEFAULT_VALUE) ));
		
		final EditText googleActivityUpdateIntervalText=(EditText)dialog.findViewById(R.id.google_activity_update_interval);
		googleActivityUpdateIntervalText.setText(
				String.valueOf(mPrefs.getInt(Constants.PREFERENCE_KEY_GOOGLE_ACTIVITY_UPDATE_INTERVAL, Constants.GOOGLE_ACTIVITY_UPDATE_INTERVAL_DEFAULT_VALUE))
				);
		
		//final ToggleButton useGoogleActivityInFusion=(ToggleButton)dialog.findViewById(R.id.use_google_for_motion_state_in_fusion);
		//useGoogleActivityInFusion.setChecked(mPrefs.getBoolean(Constants.PREFERENCE_KEY_USE_GOOGLE_ACTIVITY_IN_FUSION, false));
		
		final ToggleButton logAcclRawButton=(ToggleButton)dialog.findViewById(R.id.log_raw_switch);
		logAcclRawButton.setChecked(mPrefs.getBoolean(Constants.LOGGING_ACCL_RAW_SWITCH, false));
		
		final ToggleButton logGoogleActUpdatesButton=(ToggleButton)dialog.findViewById(R.id.log_google_updates_switch);
		logGoogleActUpdatesButton.setChecked(mPrefs.getBoolean(Constants.LOGGING_GOOGLE_ACTIVITY_UPDATE, false));
		
		final ToggleButton logDetectionButton=(ToggleButton)dialog.findViewById(R.id.log_report_switch);
		logDetectionButton.setChecked(mPrefs.getBoolean(Constants.LOGGING_DETECTION_SWITCH, false));
		
		final ToggleButton logErrorButton=(ToggleButton)dialog.findViewById(R.id.log_error_switch);
		logErrorButton.setChecked(mPrefs.getBoolean(Constants.LOGGING_ERROR_SWITCH, true));
		

		//final EditText deltaForConditionalProb=(EditText)dialog.findViewById(R.id.normal_dist_delta);
		//deltaForConditionalProb.setText(String.valueOf(mPrefs.getFloat(Constants.CIV_DELTA_CONDITIONAL_PROBABILITY, 2)) );		
		
		final Button applyButton = (Button) dialog.findViewById(R.id.performance_apply_button);
		final Button cancelButton = (Button) dialog.findViewById(R.id.peformance_cancel_button);
		applyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (classifierForCIVOnButton.isChecked())
					editor.putBoolean(Constants.PREFERENCE_KEY_CIV_CLASSIFIER_ON, true);
				else
					editor.putBoolean(Constants.PREFERENCE_KEY_CIV_CLASSIFIER_ON, false);

				if (isOutdoorButton.isChecked())
					editor.putBoolean(Constants.PREFERENCE_KEY_IS_OUTDOOR, true);
				else 
					editor.putBoolean(Constants.PREFERENCE_KEY_IS_OUTDOOR, false);

				if (logAcclRawButton.isChecked())
					editor.putBoolean(Constants.LOGGING_ACCL_RAW_SWITCH, true);
				else
					editor.putBoolean(Constants.LOGGING_ACCL_RAW_SWITCH, false);

				if (logGoogleActUpdatesButton.isChecked())
					editor.putBoolean(Constants.LOGGING_GOOGLE_ACTIVITY_UPDATE,
							true);
				else
					editor.putBoolean(Constants.LOGGING_GOOGLE_ACTIVITY_UPDATE,
							false);

				if (logDetectionButton.isChecked())
					editor.putBoolean(Constants.LOGGING_DETECTION_SWITCH, true);
				else
					editor.putBoolean(Constants.LOGGING_DETECTION_SWITCH, false);

				if (logErrorButton.isChecked())
					editor.putBoolean(Constants.LOGGING_ERROR_SWITCH, true);
				else
					editor.putBoolean(Constants.LOGGING_ERROR_SWITCH, false);
				
				
				float notificationTreshold;
				try{
					notificationTreshold=Float.parseFloat(
							notificationTresholdText.getText().toString());
				}catch(Exception ex){
					notificationTreshold=(float)Constants.DEFAULT_DETECTION_THRESHOLD;
				}
				editor.putFloat(Constants.PREFERENCE_KEY_NOTIFICATION_THRESHOLD, notificationTreshold);
				
				
				/*int detectionInterval;
				try{
					detectionInterval=Integer.parseInt(
							detectionIntervalText.getText().toString());
				}catch(Exception ex){
					detectionInterval=Constants.DETECTION_INTERVAL_DEFAULT_VALUE;
				}
				editor.putInt(Constants.PREFERENCE_KEY_DETECTION_INTERVAL, detectionInterval);*/
				
				
				/*if (useGoogleActivityInFusion.isChecked())
					editor.putBoolean(Constants.PREFERENCE_KEY_USE_GOOGLE_ACTIVITY_IN_FUSION, true);
				else
					editor.putBoolean(Constants.PREFERENCE_KEY_USE_GOOGLE_ACTIVITY_IN_FUSION, false);*/
				
				int googleActivityUpdateInterval;
				try{
					googleActivityUpdateInterval=Integer.parseInt(
							googleActivityUpdateIntervalText.getText().toString());
				}catch(Exception ex){
					googleActivityUpdateInterval=Constants.GOOGLE_ACTIVITY_UPDATE_INTERVAL_DEFAULT_VALUE;
				}
				editor.putInt(Constants.PREFERENCE_KEY_GOOGLE_ACTIVITY_UPDATE_INTERVAL, googleActivityUpdateInterval);
				
				
				/*try{
					Float delta=Float.parseFloat(deltaForConditionalProb.getText().toString());
					editor.putFloat(Constants.CIV_DELTA_CONDITIONAL_PROBABILITY, delta);
				}catch(Exception ex){
					Toast.makeText(getApplicationContext(), "Input must be a float number", Toast.LENGTH_SHORT).show();
				}*/
				
				editor.commit();
				dialog.cancel();
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
			}
		});
		dialog.show();
    }
    
    
    /**
	 * Handle Setting click
	 */
	private void handleSettings() {
		final Dialog dialog = new Dialog(this);
		dialog.setTitle(R.string.menu_item_settings);
		dialog.setContentView(R.layout.settings);
		
		final SharedPreferences mPrefs = getSharedPreferences(Constants.SHARED_PREFERENCES,  Context.MODE_PRIVATE);
		final Editor editor=mPrefs.edit();
		
		final ToggleButton logOnButton=(ToggleButton)dialog.findViewById(R.id.log_on);
		logOnButton.setChecked(mPrefs.getBoolean(Constants.LOGGING_ON, false));

		final Button btDeviceSelectButton=(Button)dialog.findViewById(R.id.bt_device_button);
		btDeviceSelectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if(mBluetoothAdapter.isEnabled()){
					selectBluetoothDevice();
				}else{
					Toast.makeText(getApplicationContext(), "Please enable your Bluetooth first.", Toast.LENGTH_SHORT).show();
				}
				
			}
		});
		
		
		final Button applyButton = (Button) dialog.findViewById(R.id.apply_button);
		final Button cancelButton = (Button) dialog.findViewById(R.id.cancel_button);
		applyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (logOnButton.isChecked())
					editor.putBoolean(Constants.LOGGING_ON, true);
				else
					editor.putBoolean(Constants.LOGGING_ON, false);
				editor.commit();
				dialog.cancel();
			}
		});

		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				dialog.cancel();
			}
		});
		dialog.show();
	}

    /*
     * Handle selections from the menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
/*            // Clear the log display and remove the log files
            case R.id.menu_item_clearlog:
                return true;

            // Display the update log
            case R.id.menu_item_showlog:                
            	// Continue by passing true to the menu handler
                return true;*/
        	case R.id.menu_item_settings:
        		handleSettings();
        		return true;
                
            case R.id.menu_item_showSensors:
            	Intent i= new Intent(MainActivity.this, Sensors.class);
            	startActivity( i );            	
            	return true;
            
            case R.id.menu_item_advanced_settings:
            	handleAdvancedSetting();
            	return true;
            

            // For any other choice, pass it to the super()
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
	/** Make sure that GPS is enabled */
	public void checkGPSEnabled()
	{	
		if ( !mLocationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
			Log.e(LOG_TAG, "GPS not enabled yet");
			/** Ask user to enable GPS */
			final AlertDialog enableGPS = new AlertDialog.Builder(this)
				.setTitle(Constants.APP_NAME+ " needs access to GPS. Please enable GPS.")
		         .setPositiveButton("Press here to enable GPS", new DialogInterface.OnClickListener() {
		        	   public void onClick(final DialogInterface dialog, final int id) {
		        		   startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), Constants.SENSOR_GPS);
		        	   }		        	  
		           })
		           .setCancelable(false)
		           .create();
		           /*.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
		        	   public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
		        	   }
		           })*/
		          
		    enableGPS.show();
	    }else{
	    	Log.e(LOG_TAG, "GPS already enabled");
	    	//GPS already enabled
	    	checkBluetoothEnabled();
	    }
	}
	
	/** Make sure that Bluetooth is enabled */
	public void checkBluetoothEnabled()
	{		
		if (mBluetoothAdapter == null)
		{
			// Device does not support Bluetooth
			AlertDialog noBluetoothAlert  = new AlertDialog.Builder(this)
			.setTitle("Bluetooth not supported.")
			.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
	        	   public void onClick(final DialogInterface dialog, final int id) {
	        	   }
	           })
			.setCancelable(true).create();
			noBluetoothAlert.show();
			writeToConsole("This phone does not have Bluetooth capability. Bluetooth connection method will not work.");
		}		
		if (!mBluetoothAdapter.isEnabled())
		{
			Log.e(LOG_TAG, "bluetooth not enabled yet");
			/** Ask user to enable Bluetooth */
			AlertDialog enableBluetoothDialog = new AlertDialog.Builder(this)
					.setTitle("Please enable Bluetooth on your phone.")
					.setCancelable(false)
					.setPositiveButton("Enable Bluetooth",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog, final int id) {
									startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),	Constants.SENSOR_BLUETOOTH);
								}
							})
					.setNegativeButton("Skip",
							new DialogInterface.OnClickListener() {
								public void onClick(
										final DialogInterface dialog,final int id) {}
							}).create();
			enableBluetoothDialog.show();
		} else {
			//bluetooth is enabled (directed from onActivityResult())			
			SharedPreferences mPrefs = getSharedPreferences(Constants.SHARED_PREFERENCES,
	                Context.MODE_PRIVATE);			
			String carBluetoothDeviceName=null;
			if(mPrefs.contains(Constants.BLUETOOTH_CAR_DEVICE_NAME)){
				carBluetoothDeviceName=mPrefs.getString(Constants.BLUETOOTH_CAR_DEVICE_NAME, null);
			}			
			Log.e(LOG_TAG, "bluetooth enabled "+ carBluetoothDeviceName);
			
			if(carBluetoothDeviceName!=null){
				Intent intent = new Intent(MainActivity.this, BluetoothConnectionService.class);
				startService(intent);		
			}else{//ask the user to select a car bluetooth device
				selectBluetoothDevice();
			}
		}
	}
	
	/*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * DetectionRemover and DetectionRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
    	Log.e(LOG_TAG, requestCode+"  "+requestCode);
    	switch (requestCode) {
        case Constants.SENSOR_GPS:
        	checkBluetoothEnabled();
        	break;
        case Constants.SENSOR_BLUETOOTH:
        	if(mBluetoothAdapter.isEnabled()){//only if the user enables the bluetooth
        		checkBluetoothEnabled();
        	}
        	break;
       // If the request code matches the code sent in onConnectionFailed
		case Constants.CONNECTION_FAILURE_RESOLUTION_REQUEST:
			switch (resultCode) {
			// If Google Play services resolved the problem
			case Activity.RESULT_OK:
				// If the request was to start activity recognition updates
				if (Constants.REQUEST_TYPE.ADD == mRequestType) {
					// Restart the process of requesting activity recognition
					// updates
					mGoogleActivityDetectionRequester.requestUpdates();
					// If the request was to remove activity recognition updates
				} else if (Constants.REQUEST_TYPE.REMOVE == mRequestType) {
					/*
					 * Restart the removal of all activity recognition updates
					 * for the PendingIntent.
					 */
					mGoogleActivityDetectionRemover.removeUpdates(mGoogleActivityDetectionRequester
							.getRequestPendingIntent());

				}
				break;

			// If any other result was returned by Google Play services
			default:
				// Report that Google Play services was unable to resolve the
				// problem.
				Log.d(Constants.APP_NAME, getString(R.string.no_resolution));
			}
			// If any other request code was received
		default:
			// Report that this Activity received an unknown requestCode
			Log.d(Constants.APP_NAME,
					getString(R.string.unknown_activity_request_code,
							requestCode));
			break;
        }
    }

		
	private String selectedBloothDeviceName=null;
	public void selectBluetoothDevice()
	{	
		Set<BluetoothDevice> bluetoothDevices=mBluetoothAdapter.getBondedDevices();
		final CharSequence[] listItems = new CharSequence[bluetoothDevices.size()];
		int i=0;
		for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
			String device_name = device.getName();
			listItems[i++]=device_name;
		}
		
		AlertDialog select=new AlertDialog.Builder(this)
        .setTitle(R.string.set_bluetooth_message)
        .setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	Log.e(LOG_TAG, "id="+whichButton);
        		if(whichButton>=0) selectedBloothDeviceName=listItems[whichButton].toString();
            }
        })
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	Log.e(LOG_TAG, selectedBloothDeviceName);
            	Toast.makeText(getApplicationContext(), getString(R.string.bluetooth_device_selected, selectedBloothDeviceName) , Toast.LENGTH_SHORT).show();
				
				final SharedPreferences mPrefs = getSharedPreferences(Constants.SHARED_PREFERENCES, Context.MODE_PRIVATE);
				Editor editor=mPrefs.edit();
				editor.putString(Constants.BLUETOOTH_CAR_DEVICE_NAME, selectedBloothDeviceName);
				editor.commit();
             }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        })
       .create();
		select.show();
	}


	
	
		/** Write a string to output console */
	public void writeToConsole(String str)
	{
		consoleTextView.append(str);
        final Layout layout = consoleTextView.getLayout();
        if(layout != null){
            int scrollDelta = layout.getLineBottom(consoleTextView.getLineCount() - 1) 
                - consoleTextView.getScrollY() - consoleTextView.getHeight();
            if(scrollDelta > 0)
            	consoleTextView.scrollBy(0, scrollDelta);
        }
	}
	
    private void setupMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
          // Try to obtain the map from the SupportMapFragment.
          mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                 .getMap();
          // Check if we were successful in obtaining the map.
          if (mMap != null) {
            mMap.setMyLocationEnabled(true);
	        
            // Setting an info window adapter allows us to change the both the contents and look of the
	        // info window.
	        mMap.setInfoWindowAdapter(new MarkerInfoWindowAdapter(getLayoutInflater()));
          }
        }
      }

      private void setupLocationClientIfNeeded() {
        if (mLocationClient == null) {
          mLocationClient = new LocationClient(
              getApplicationContext(),
              this,  // ConnectionCallbacks
              this); // OnConnectionFailedListener
          if(mLocationClient!=null){
        	  mLocationClient.connect();
        	  
          }
        }
      }
    
    
      
      
    // actions taken when a parking/unparking event is detected and the location of the event is retrieved
	private void onLocationRetrieved(int eventCode, double detectionConfidence,
			long locationRequestTimestamp, Location location, String address){
		boolean logDetection=getSharedPreferences(Constants.SHARED_PREFERENCES, 0).getBoolean(Constants.LOGGING_DETECTION_SWITCH, false);
		if(logDetection){
			String logMsg=
					(eventCode==Constants.OUTCOME_PARKING?Constants.PARKING_NOTIFICATION:Constants.UNPARKING_NOTIFICATION)+CommonUtils.formatTimestamp( new Date(locationRequestTimestamp), "HH:mm:ss   " )
					+"\nlocatoin retrieval time:"+CommonUtils.formatTimestamp( new Date(location.getTime()), "HH:mm:ss  " )+"\nlocation:"+location.toString()+"\n";
				
				if(address!=null){
					logMsg+="address retrieved at "+CommonUtils.formatTimestamp( new Date(System.currentTimeMillis()), "HH:mm:ss " )+address+"\n";
				}
				else logMsg+="address is not available. \n";

				logMsg+=pastEnvironments.toString()+"\n"
					+mPastClassifiedMotionStates.toString()+"\n"
					+mPastGoogleActivities.toString()+"\n";
			mLogManager.log(logMsg, Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_DETECTION_REPORT]);
		}

		if(eventCode==Constants.OUTCOME_PARKING){//parking
			if(detectionConfidence==1.0||mPastGoogleActivities.isTransitionTo(MotionState.Type.ON_FOOT)){
				onDetectionConfirmed(eventCode, locationRequestTimestamp, location, address);
			}else{
				CachedDetection cd=new CachedDetection(CachedDetection.Type.Parking, location,locationRequestTimestamp, address, detectionConfidence);
				mCachedParkingDetectionList.add(cd);
			}
		}else{//unparking
			if(detectionConfidence==1.0||mPastGoogleActivities.isTransitionTo(MotionState.Type.IN_VEHICLE)){
				onDetectionConfirmed(eventCode, locationRequestTimestamp, location, address);
			}else{
				CachedDetection cd=new CachedDetection(CachedDetection.Type.Unparking, location,locationRequestTimestamp, address, detectionConfidence);
				mCachedUnparkingDetectionList.add(cd);
			}
		}
	}
	
	public void onDetectionConfirmed(int eventCode,long eventTimestamp, Location eventlocation, String eventAddress){
    	int resID;
		String prefix;
		float markerColor;
		String voiceNotificationMsg;
		if(eventCode==Constants.OUTCOME_PARKING){
			resID=R.raw.vehicle_parked;
			prefix=Constants.PARKING_NOTIFICATION;
			markerColor=BitmapDescriptorFactory.HUE_AZURE;
			voiceNotificationMsg="Vehicle Parked ";
		}else{//unparking
			resID=R.raw.vehicle_deparked;
			prefix=Constants.UNPARKING_NOTIFICATION;
			markerColor=BitmapDescriptorFactory.HUE_RED;
			voiceNotificationMsg="Vehicle Departured ";
		}

		//String curTimeString=CommonUtils.formatTimestamp(new Date(),formatTemplate);
		long curTimestamp=System.currentTimeMillis();
		String curTimeString=CommonUtils.formatTimestamp( new Date(curTimestamp), "HH:mm:ss   " );
		Log.e(LOG_TAG, curTimeString+" \n"+eventlocation.toString() );	
		
		/*
		 * actions
		 */
		//1. send the text notification
		String notificationMsg=prefix+" "+curTimeString;
		if(eventAddress!=null) notificationMsg+=eventAddress;
		mDetectionNotificationManager.sendTextNotification(notificationMsg);
					
		//2. play the sound
		//2.1 announce the type
		//mDetectionNotificationManager.playVoiceNotification(resID);
		//2.2 announce the time
		long diffInMillisecs=curTimestamp-eventTimestamp;
		long pastMinutes=diffInMillisecs/Constants.ONE_MINUTE;
		long pastSeconds=(diffInMillisecs-pastMinutes*Constants.ONE_MINUTE)/Constants.ONE_SECOND_IN_MILLISECOND;
		if(pastMinutes>0) voiceNotificationMsg+=pastMinutes+" minutes ";
		voiceNotificationMsg+=pastSeconds+" seconds ago";
		mTextToSpeech.speak(voiceNotificationMsg, TextToSpeech.QUEUE_FLUSH, null);
		
		//3. log the address of event
		String logMsg=prefix+CommonUtils.formatTimestamp(new Date(eventTimestamp), "HH:mm:ss ")
				+"\nNotification generation time:"+curTimeString
				+"\nCached Detection timestamp: "+CommonUtils.formatTimestamp(new Date(eventTimestamp), "HH:mm:ss ")
				+"\nlocation:"+eventlocation.toString()+"\n";
		if(eventAddress!=null) logMsg+=eventAddress+"\n";
		else logMsg+="address is not available. \n";
		logMsg+=pastEnvironments.toString()+"\n"
				+mPastClassifiedMotionStates.toString()+"\n"
				+mPastGoogleActivities.toString()+"\n";
		boolean logDetection=getSharedPreferences(Constants.SHARED_PREFERENCES, 0).getBoolean(Constants.LOGGING_DETECTION_SWITCH, false);
		if(logDetection)
			mLogManager.log(logMsg, Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_DETECTION_REPORT]);
		
		//4. show on the map
		mMap.clear();
		mDetectionNotificationManager.addMarkersToMap(mMap, curTimeString, prefix
				, eventlocation.getLatitude(), eventlocation.getLongitude(), eventlocation.getAltitude(),	markerColor);
		//center and zoom in the map
		CameraPosition cameraPosition = new CameraPosition.Builder()
	    .target(new LatLng(eventlocation.getLatitude(), eventlocation.getLongitude()) )     // Sets the center of the map to Mountain View
	    .zoom(17)                   // Sets the zoom
	    //.bearing(90)                // Sets the orientation of the camera to east
	    //.tilt(30)                   // Sets the tilt of the camera to 30 degrees
	    .build();                   // Creates a CameraPosition from the builder
		mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));		
		//add a marker on the map				
		Log.e(LOG_TAG, "operations on map completed");
		
		
		//5. update availability display
		//updateAvailabilityDisplay(eventCode, location);
		//add a marker on the map				
		Log.e(LOG_TAG, "operations on map completed");
		updateAvailabilityDisplay(eventCode, eventlocation);
    }
	
	
	public void updateAvailabilityDisplay(int eventCode, Location location) {
		//mMap.clear();
		
//        for (int i = 0; i < parkingBlocks.size(); i++) {
//        	ParkingBlock parkingBlock = parkingBlocks.elementAt(i);
//        	PolylineOptions line =  
//        			new PolylineOptions().add(parkingBlock.startLocation, parkingBlock.endLocation)
//        			.width(20).color(Color.RED);
//        	Polyline polyline = mMap.addPolyline(line);
//        	parkingBlock.display = polyline;
//        }


//        for (int i = 0; i < parkingBlocks.size(); i++) {
//        	ParkingBlock parkingBlock = parkingBlocks.elementAt(i);
//        	parkingBlock.display.setColor(Color.RED);;
//        }
		
		
		
		// find closest street block within 30 meters
        LatLng point = new LatLng(location.getLatitude(),location.getLongitude());
        double minDist = Double.MAX_VALUE;
        ParkingBlock matchedBlock = null;
        for (int i = 0; i < parkingBlocks.size(); i++) {
        	ParkingBlock parkingBlock = parkingBlocks.elementAt(i);
        	double dist = parkingBlock.distanceToPoint(point);
        	if (dist < minDist) {
        		minDist = dist;
        		matchedBlock = parkingBlock;
        	}
        }
        if (matchedBlock != null) {
//Toast.makeText(getApplicationContext(), "a block matched", 2).show();        	
            if(eventCode==Constants.OUTCOME_PARKING) {
            	matchedBlock.availability = 0;
            } else {
            	matchedBlock.availability = 1;
            }
        }
        showAvailabilityMap();
//        for (int i = 0; i < parkingBlocks.size(); i++) {
//      	    ParkingBlock parkingBlock = parkingBlocks.elementAt(i);
//    	    PolylineOptions line =  
//    			new PolylineOptions().add(parkingBlock.startLocation, parkingBlock.endLocation)
//    			.width(20).color(parkingBlock.getColorByAvailability());
//    	    Polyline polyline = mMap.addPolyline(line);
//    	    parkingBlock.display = polyline;
//        }        
	}
	
	public void showAvailabilityMap() {
    	//MapFragment mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        //GoogleMap map = mapFragment.getMap();
    	//GoogleMap map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
        //.getMap();
//        VisibleRegion bounds = map.getProjection().getVisibleRegion();
//        LatLngBounds xyBounds = bounds.latLngBounds;
//        String msg = "northest = " + xyBounds.northeast;
//        CameraUpdate center=
//            CameraUpdateFactory.newLatLng(new LatLng(41.909348,
//                		-87.777664));
//            CameraUpdate zoom=CameraUpdateFactory.zoomTo(15);
//
//            mMap.moveCamera(center);
//            mMap.animateCamera(zoom);        

            
            for (int i = 0; i < parkingBlocks.size(); i++) {
            	ParkingBlock parkingBlock = parkingBlocks.elementAt(i);
            	PolylineOptions line = new PolylineOptions().add(parkingBlock.startLocation, parkingBlock.endLocation)
            			.width(20).color(parkingBlock.getColorByAvailability());
            	Polyline polyline = mMap.addPolyline(line);
            	parkingBlock.display = polyline;
            	//map.addMarker(new MarkerOptions().position(parkingBlock.meterLocation)
            		//.title(parkingBlock.meterAddress));
            }
    }
	
	
	

	@Override
	public void onConnected(Bundle arg0) {
		 /***
			 * Use periodical GPS to test energy consumption
			 */
			/*mLocationClient.requestLocationUpdates( 
      			LocationRequest.create()
      			.setInterval(1000*60)
      			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY), 
  				new LocationListener() {
						@Override
						public void onLocationChanged(Location arg0) {
						}
					});*/
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
	}

	@Override
	public void onDisconnected() {
	}
	
	/**
	 * Legacy codes
	 */
	
  	//private double calibration = 0.0;
    private double currentAcceleration;
    private double appliedAcceleration = 0;
    private Date lastUpdate;  
  @SuppressWarnings("unused")
  private double calVelocityIncrease() 
  {
	  // Calculate how long this acceleration has been applied.
      Date timeNow = new Date(System.currentTimeMillis());
      double timeDelta = timeNow.getTime()-lastUpdate.getTime();
      lastUpdate.setTime(timeNow.getTime());

      // Calculate the change in velocity
      // current acceleration since the last update. 
      double deltaVelocity = appliedAcceleration * (timeDelta/1000);
      appliedAcceleration = currentAcceleration;

      // Add the velocity change to the current velocity.
      return deltaVelocity;
  }
  
  /**
   * Verify that Google Play services is available before making a request.
   *
   * @return true if Google Play services is available, otherwise false
   */
  private boolean isGooglePlayServiceAvailable() {

      // Check that Google Play services is available
      int resultCode =
              GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

      // If Google Play services is available
      if (ConnectionResult.SUCCESS == resultCode) {

          // In debug mode, log the status
          Log.d(Constants.APP_NAME, getString(R.string.play_services_available));

          // Continue
          return true;

      // Google Play services was not available for some reason
      } else {

          // Display an error dialog
          GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0).show();
          return false;
      }
  }
	
    /**
     * Respond to "Start" button by requesting activity recognition
     * updates.
     * @param view The view that triggered this method.
     */
    public void startGoogleActivityRecognitionUpdates(View view) {

        // Check for Google Play services
        if (!isGooglePlayServiceAvailable()) {
            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = Constants.REQUEST_TYPE.ADD;

        // Pass the update request to the requester object
        mGoogleActivityDetectionRequester.requestUpdates();
    }

    /**
     * Respond to "Stop" button by canceling updates.
     * @param view The view that triggered this method.
     */
    public void stopGoogleActivityRecognitionUpdates(View view) {

        // Check for Google Play services
        if (!isGooglePlayServiceAvailable()) {
            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = Constants.REQUEST_TYPE.REMOVE;

        // Pass the remove request to the remover object
        mGoogleActivityDetectionRemover.removeUpdates(mGoogleActivityDetectionRequester.getRequestPendingIntent());

        /*
         * Cancel the PendingIntent. Even if the removal request fails, canceling the PendingIntent
         * will stop the updates.
         */
        PendingIntent pIntent=mGoogleActivityDetectionRequester.getRequestPendingIntent();
        if(pIntent!=null) pIntent.cancel();   
    }

    /**
     * Display the activity detection history stored in the
     * log file
     */
    /*private void updateActivityHistory() {
        // Try to load data from the history file
        try {
            // Load log file records into the List
            List<Spanned> activityDetectionHistory =
                    mLogManager.loadLogFile();

            // Clear the adapter of existing data
            mStatusAdapter.clear();

            // Add each element of the history to the adapter
            for (Spanned activity : activityDetectionHistory) {
                mStatusAdapter.add(activity);
            }

            // If the number of loaded records is greater than the max log size
            if (mStatusAdapter.getCount() > Constants.MAX_LOG_SIZE) {
            	
            	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	    builder.setMessage("File is too large to be shown.")
        	           .setCancelable(true);
        	    final AlertDialog alert = builder.create();
        	    alert.show();
            	
                // Delete the old log file
                if (!mLogFile.removeLogFiles()) {

                    // Log an error if unable to delete the log file
                    Log.e(Constants.APPTAG, getString(R.string.log_file_deletion_error));
                }
            }

            // Trigger the adapter to update the display
            mStatusAdapter.notifyDataSetChanged();

        // If an error occurs while reading the history file
        } catch (IOException e) {
            Log.e(Constants.APP_NAME, e.getMessage(), e);
        }
    }*/

    /**
     * Broadcast receiver that receives activity update intents
     * It checks to see if the ListView contains items. If it
     * doesn't, it pulls in history.
     * This receiver is local only. It can't read broadcast Intents from other apps.
     */
    BroadcastReceiver updateListReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {            /*
             * When an Intent is received from the update listener IntentService, update
             * the displayed log.
             */
        	//do not execute an update to avoid freezing the app
            //updateActivityHistory();
        }
    };
    
    
	@Override
	//of TextToSpeech
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			// set Language
			int result = mTextToSpeech.setLanguage(Locale.US);
			// tts.setPitch(5); // set pitch level
			// tts.setSpeechRate(2); // set speech speed rate
			if (result == TextToSpeech.LANG_MISSING_DATA
				|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
			}{
				Log.e(LOG_TAG, "Text-To-Speech Language cannot be setted US. Error code=" + result);
			}
			//mTextToSpeech.speak("test", TextToSpeech.QUEUE_FLUSH, null);
		} else {
			Log.e(LOG_TAG, "Text-To-Speech Initilization Failed");
		}
	}

}



class PastMotionStates{
	public int capacity;
	public Source source;
	public HashMap<MotionState.Type, Integer> map;
	public ArrayList<MotionState.Type> list;
	
	public long timestampOfLastInVehicleState;
	public long timestampOfLastOnFootState;
	public static final long EXPIRATION_TIME_IN_MILLISEC=Constants.ONE_MINUTE+Constants.ONE_MINUTE/2;

	public PastMotionStates(Source source, int capacity) {
		this.source = source;
		this.capacity = capacity;
		map = new HashMap<MotionState.Type, Integer>();
		list = new ArrayList<MotionState.Type>();
	}

	public void clear(){
		map.clear();
		list.clear();
	}
	
	public void add(MotionState.Type state) {
		if (list.size() == capacity) {
			MotionState.Type removedMotionType = list.remove(0);// remove the oldest state
			map.put(removedMotionType, map.get(removedMotionType) - 1);
		}
		list.add(state);
		if (!map.containsKey(state))
			map.put(state, 0);
		map.put(state, map.get(state) + 1);
	}
	
	public void removeAll(MotionState.Type state) {
		while(list.remove(state));
		map.remove(state);
	}
	
	public boolean isTransitionTo(MotionState.Type toState){
		if(toState!=MotionState.Type.IN_VEHICLE&&toState!=MotionState.Type.ON_FOOT) return false;
		boolean isTransiting=containsAtLeastMOnFootAndAtLeastNInVehicleStates(1, 1)&&containsOnlyOneAndIsLaterThanFromState(toState);
		
		if(isTransiting){
    		//remove all previous occurrences of fromState
			if(toState==MotionState.Type.IN_VEHICLE) removeAll(MotionState.Type.ON_FOOT);
			else removeAll(MotionState.Type.IN_VEHICLE);
		}
		return isTransiting;
	}
	
	public boolean containsAtLeastMOnFootAndAtLeastNInVehicleStates(int mOnFoot, int nInVehicle) {
		// return false if the filter fails
		if (!map.containsKey(MotionState.Type.ON_FOOT)
				|| !map.containsKey(MotionState.Type.IN_VEHICLE))
			return false;
		int walkingCnt = map.get(MotionState.Type.ON_FOOT);
		int drivingCnt = map.get(MotionState.Type.IN_VEHICLE);
		// Log.e(LOG_TAG,"#Walk="+walkingCnt+" #Drive="+drivingCnt);
		if (walkingCnt < mOnFoot  || drivingCnt < nInVehicle)
			return false;
		return true;
	}
	
	//Type equals to either On_foot or In_vehicle
	public boolean containsOnlyOneAndIsLaterThanFromState(MotionState.Type type) {
		if (!map.containsKey(type)||map.get(type)!=1) return false;

		for(int i=list.size()-1;i>=0;i--){
			MotionState.Type curType=list.get(i);
			if(curType!=MotionState.Type.ON_FOOT&&curType!=MotionState.Type.IN_VEHICLE) continue;
			if(curType==type) return true;
			else return false;
		}
		return false;		
	}
	
	public String toString() {
		String ret = list.toString() + "\n";
		for (Type type : map.keySet())
			ret += type.toString() + ":" + map.get(type) + "  ";
		return ret;
	}
}

class MotionState {
	public enum Source {
		Google, Classifier;
	}

	public enum Type {
		ON_FOOT("On_Foot"), IN_VEHICLE("In_Vehicle"), STILL("Still"), UNKNOWN(
				"Unknown"), ON_BIKE("On_Bike"), OTHER("Other");

		private String typeString;

		private Type(String type) {
			this.typeString = type;
		}

		public String toString() {
			return typeString;
		}
	}

	public Source source;
	public Type type;
	public int secondOfDay;
	
	public static MotionState.Type translate(String predClass) {
		MotionState.Type ret;
		if ("Walking".equals(predClass)) {
			ret=MotionState.Type.ON_FOOT;
		} else {
			if ("Driving".equals(predClass))
				ret=MotionState.Type.IN_VEHICLE;
			else {
				if ("Still".equals(predClass))
					ret=MotionState.Type.STILL;
				else
					ret=MotionState.Type.OTHER;
			}
		}
		return ret;
	}

	public static MotionState.Type translate(int activityTypeDefinedByGoogle) {
		MotionState.Type ret;
		switch (activityTypeDefinedByGoogle) {
		case DetectedActivity.ON_FOOT:
			ret=MotionState.Type.ON_FOOT;
			break;
		case DetectedActivity.IN_VEHICLE:
			ret=MotionState.Type.IN_VEHICLE;
			break;
		case DetectedActivity.STILL:
			ret=MotionState.Type.STILL;
			break;
		case DetectedActivity.ON_BICYCLE:
			ret=MotionState.Type.ON_BIKE;
		default:
			ret=MotionState.Type.UNKNOWN;
			break;		
		}
		return ret;
	}
}

class CachedDetection{
	public enum Type{
		Parking, Unparking
	}
	public long timestamp;
	public Location location;
	public String address;
	public Type type;
	public double confidence;
	
	public static final long EXPIRATION_TIME=Constants.ONE_MINUTE;
	
	public CachedDetection(Type type, Location loc,
			long locationRequestTime, String address
			,double confidence){
		timestamp=locationRequestTime;
		location=loc;
		this.type=type;
		this.address=address;
		this.confidence=confidence;
	}
	
	
}

class CachedDetectionList{
	CachedDetection.Type type;
	ArrayList<CachedDetection> list;
	public CachedDetectionList(CachedDetection.Type type) {
		this.type=type;
		list=new ArrayList<CachedDetection>();
	}
	
	public void removeExpiredCachedDetection(){
		//remove expired cached detections
		long curtime=System.currentTimeMillis();
		int i;
		ArrayList<CachedDetection> newList=new ArrayList<CachedDetection>();
		for(i=0;i<list.size();i++){
			if(curtime-list.get(i).timestamp<=CachedDetection.EXPIRATION_TIME){
				newList.add(list.get(i));
			}
		}
		list=newList;
	}
	
	public void add(CachedDetection cd){
		removeExpiredCachedDetection();
		//add the new one
		list.add(cd);
	}
	
	public CachedDetection get(int index){
		removeExpiredCachedDetection();
		if(index<0||index>=list.size()) return null;
		return list.get(index);
	}
	
	public CachedDetection getWithLargestProbability(){
		removeExpiredCachedDetection();
		
		if(list.size()<=0) return null;
		if(list.size()==1) return list.get(0);
		
		//sort 
		ArrayList<CachedDetection> sortedList=new ArrayList<CachedDetection>();
		sortedList.addAll(list);
		Collections.sort(sortedList, new Comparator<CachedDetection>() {
			@Override
			public int compare(CachedDetection lhs, CachedDetection rhs) {
				double diff=lhs.confidence-rhs.confidence;
				if(diff>0) return 1;
				else if(diff<0) return -1;
				else return 0;
			}
			
		});
		return sortedList.get(sortedList.size()-1);
	}
	
}

