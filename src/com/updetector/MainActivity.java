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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.XPS;
import com.updetector.R;
import com.updetector.Constants.REQUEST_TYPE;
import com.updetector.classification.ClassificationManager;
import com.updetector.classification.WekaClassifier;
import com.updetector.fusion.ConditionalProbability;
import com.updetector.fusion.FusionManager;
import com.updetector.indicator.accelerometerbased.AccelerometerFeature;
import com.updetector.indicator.iodetector.CellTowerChart;
import com.updetector.indicator.iodetector.DetectionProfile;
import com.updetector.indicator.iodetector.LightChart;
import com.updetector.indicator.iodetector.MagnetChart;
import com.updetector.managers.AudioRecordManager;
import com.updetector.managers.EventDetectionNotificationManager;
import com.updetector.managers.LogManager;
import com.updetector.managers.WakeLockManager;
import com.updetector.sensorlist.BluetoothConnectionService;
import com.updetector.sensorlist.Sensors;
import com.updetector.viewadapters.MarkerInfoWindowAdapter;


/**
 * Sample application that demonstrates the use of
 * ActivityRecognitionClient}. It registers for activity detection updates
 * at a rate of 20 seconds, logs them to a file, and displays the detected
 * activities with their associated confidence levels.
 * <p>
 * An IntentService receives activity detection updates in the background
 * so that detection can continue even if the Activity is not visible.
 */
public class MainActivity extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener
 {
	public static final String LOG_TAG=MainActivity.class.getCanonicalName();
	
	private static final String LOCK_TAG="ACCELEROMETER_MONITOR";
	
	
	private static boolean geofencingOn=false;
	/**
	 * UI Widgets
	 */

    // Holds the text view  
    private TextView consoleTextView, environTextView, stateTextView, detectionTextView;
    public static final String ENVIRONMENT_PREFIX="Environment : ";
    public static final String STATE_PREFIX="State : ";
    public static final String DETECTION_PREFIX="Detection : ";
    public static final String INDICATOR_PREFIX="Indicator : "; 
    private GoogleMap mMap;

    
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
    private EventDetectionNotificationManager mEventDetectionNotificationManager;
    
    // The wake lock manager object
    private WakeLockManager mWakeLockManager;
    
    // The activity recognition update request object
    private DetectionRequester mDetectionRequester;

    // The activity recognition update removal object
    private DetectionRemover mDetectionRemover;
    
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
    private AudioRecordManager mAudioRecordManager;
    private FusionManager mFusionManager;
    
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
	private HashMap<Integer, ArrayList<Double>> lastVectors=new HashMap<Integer, ArrayList<Double>>();
   
	
	    
    // The classificatio manager object
    private ClassificationManager mClassificationManager;
	
    // Store the current request type (ADD or REMOVE)
    private REQUEST_TYPE mRequestType;
    
       
    public final static String BLUETOOTH_CONNECTION_ACK = "BLUETOOTH_CONNECTION_ACK";
	public final static String EXTRA_MESSAGE = "EVENT_CODE";
	
	
	public class LocationClientListener implements LocationListener{
		int eventCode;
		
		public LocationClientListener(int eventCode){
			this.eventCode=eventCode;			
		}

		@Override
		public void onLocationChanged(Location location) {
			(new GetAddressTask(eventCode)).execute(location);
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
		Location mLocation;
		
		public GetAddressTask(int eventCode) {
			super();
			this.eventCode=eventCode;			
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
        	actionsOnParkingLocation(eventCode, mLocation, address);
        }
	} 
		
	
	/** Callback when a message is sent from the Bluetooth connection service */
	private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	String action=intent.getAction();
	    	Log.e(LOG_TAG, action);
	    	if(action.equals(BLUETOOTH_CONNECTION_ACK)) {
	        	int eventCode = intent.getIntExtra(EXTRA_MESSAGE, Constants.OUTCOME_NONE);
	        	System.out.println(eventCode);
				
				Toast.makeText(getApplicationContext(), "\ndetected "+CommonUtils.eventCodeToString(eventCode)+" at time: "+CommonUtils.formatTimestamp( new Date(), "HH:mm:ss" ), Toast.LENGTH_LONG).show();

	        	
	        	// retrieve a location then
	        	/*mXPSHandler.getLocation(null
						 ,WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP
						 , new XPSLocationCallback(eventCode) );      */  
	        	
	        	mLocationClient.requestLocationUpdates( LocationRequest
	    				.create().setNumUpdates(1).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY), 
	    				new LocationClientListener(eventCode)); 
	        	
	        }
	    }
	};
	
	
	//accelerometer feature window and its neighboring windows
	private ArrayList<AccelerometerFeature> civVectorsWithinScope=new ArrayList<AccelerometerFeature>();
	
	
	
	  /**
	   * MST
	   */
    private ArrayList<String> pastMotionStates=new ArrayList<String>();
    private HashMap<String, Integer> pastMotionStatesCount=new HashMap<String, Integer>();
    
    private double[] lastMotionStateDistr=null;
    private double[] lastAccReading;
    
    
    private void updatePastMotionStates(String motion){
		if(pastMotionStates.size()==Constants.NO_OF_PAST_STATES_STORED){
			String removedMotion=pastMotionStates.remove(0);//remove the oldest state
			pastMotionStatesCount.put(removedMotion, pastMotionStatesCount.get(removedMotion)-1);
		}
		pastMotionStates.add(motion);
		if(!pastMotionStatesCount.containsKey(motion)) pastMotionStatesCount.put(motion, 0);
		pastMotionStatesCount.put(motion, pastMotionStatesCount.get(motion)+1);
    }
    
    private boolean motionStateBasedFilterPass(){
    	//return false if the filter fails
    	if(!pastMotionStatesCount.containsKey("Walking")||!pastMotionStatesCount.containsKey("Driving")) return false;
    	int walkingCnt=pastMotionStatesCount.get("Walking");
    	int drivingCnt=pastMotionStatesCount.get("Driving");
    	//Log.e(LOG_TAG,"#Walk="+walkingCnt+" #Drive="+drivingCnt);
    	if(walkingCnt<1||drivingCnt<1)	return false;    	
    	return true;
    }
    
    
    
    //TODO mSensnorEvent
    @SuppressLint("UseSparseArrays")
    public static long acceleometerSeq=0;
	private final SensorEventListener mSensorEventListener = new SensorEventListener() 
	  {
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
			
			/**
	  		 * Get the parameter values from the preference
	  		 */
			SharedPreferences mPrefs=getSharedPreferences(Constants.SHARED_PREFERENCES, 0);
	  		boolean classifierForCIVOn=mPrefs.getBoolean(Constants.CIV_CLASSIFIER_ON, false);
			boolean logOn=mPrefs.getBoolean(Constants.LOGGING_ON, false);
			boolean isOutdoor=mPrefs.getBoolean(Constants.IS_OUTDOOR, false);
			
			
			// log the raw readings
			String record=CommonUtils.buildALogRecordForNewAccelerometerReadings(event);
			if(record!=null) phoneNotStill=true;
			else phoneNotStill=false;
			
			boolean logRawOn=mPrefs.getBoolean(Constants.LOGGING_ACCL_RAW_SWITCH, false);
			if(logOn&&logRawOn){
				mLogManager.log(record, Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_ACCEL_RAW]);
			}			
			
			int outcome=Constants.OUTCOME_NONE;
			//conditions for early exit based on environment
			if( 
				(lastEnvironment==Constants.ENVIRON_INDOOR&&probabilityOfLastEnvironment>0.8)
				//|| !pastMotionStates.contains((Integer)Constants.STATE_DRIVING)
			){
				if(!isOutdoor)//not set to outdoor environment
					return;
			}
			
			
			AccelerometerFeature motionStateFeatures=mClassificationManager.mMSTFeatureExtraction.extractWindowFeature(event);
			if(motionStateFeatures!=null){
				String motionStateInstance=motionStateFeatures.asStringForMotationState();
				WekaClassifier motionStateClassifier=mClassificationManager.mClassfiers.get(Constants.ACCEL_MOTION_STATE); 
				double[] distr=motionStateClassifier.classify(motionStateInstance);
				Log.e(LOG_TAG, "motion state classifier output is : " +Arrays.toString(distr));
				
				/**
				 * Get the motion state with largest probability
				 */
				int predClassIdx=CommonUtils.idxOfMax(distr);
				if(predClassIdx!=-1){
					String predClass=Constants.CLASSIFIER_CLASS[1][predClassIdx];
					if(!phoneNotStill) predClass="Still";
					Log.e(LOG_TAG, "cur motion state="+predClass);
					stateTextView.setText(STATE_PREFIX+predClass);
					
					updatePastMotionStates(predClass);
				}
				
				//early exit based on state
				if(!motionStateBasedFilterPass()) return;
					
				if(lastMotionStateDistr!=null){
					//build the vector of the MST indicator
					ArrayList<Double> mstVector=new ArrayList<Double>();
					mstVector.add(lastMotionStateDistr[0] );
					mstVector.add(lastMotionStateDistr[1]);
					mstVector.add(distr[0]);
					mstVector.add(distr[1]);
					Log.e(LOG_TAG, acceleometerSeq+" new mst vector is :"+mstVector.toString());					
					HashMap<Integer, ArrayList<Double>> newPeriodicalVector=new HashMap<Integer, ArrayList<Double>>();
					newPeriodicalVector.put(Constants.INDICATOR_MST, mstVector);
					outcome=mFusionManager.fuse(lastVectors, newPeriodicalVector, System.currentTimeMillis(), Constants.HIGH_LEVEL_ACTIVITY_UPARKING, mLogManager);
				}
				lastMotionStateDistr=distr;
				//lastMotionStateDistr=new double[distr.length];
				//for(int ii=0;ii<distr.length;ii++) lastMotionStateDistr[ii]=distr[ii];
				
			}else{
				if(!motionStateBasedFilterPass()) return;
			}

			
			AccelerometerFeature civFeatures=mClassificationManager.mCIVFeatureExtraction.extractWindowFeature(event);
			if(civFeatures!=null){
				//get the vector of the Change-In-Variance features 
				String civVector=mClassificationManager.mCIVFeatureExtraction.extractCIVVector(civFeatures, civVectorsWithinScope);
				if( civVector!=null){
					Log.e(LOG_TAG, acceleometerSeq+" new civ vector is : "+civVector);
					
					boolean logAcclFeaturesOn=mPrefs.getBoolean(Constants.LOGGING_ACCL_FEATURES_SWITCH, false);
					if(logOn&&logAcclFeaturesOn){
						// log the Change-In-Variance Classifier predicated result
						mLogManager.log(civVector, Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_ACCEL_FEATURE]);
					}
						/**
						 * calculate the probability of the outcome
						 */
						if(!classifierForCIVOn){
							HashMap<Integer, ArrayList<Double>> newPeriodicalVector=new HashMap<Integer, ArrayList<Double>>();
							newPeriodicalVector.put(Constants.INDICATOR_CIV,CommonUtils.stringToDoubleListRemoved(civVector, ",", new int[]{0}) );
							outcome=mFusionManager.fuse(lastVectors, newPeriodicalVector, System.currentTimeMillis(),Constants.HIGH_LEVEL_ACTIVITY_UPARKING, mLogManager);
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
									 outcome=Constants.OUTCOME_PARKING;
								 }
								 else{
									 predClass=",u";
									 outcome=Constants.OUTCOME_UNPARKING;
								 }
							break;
							case Constants.STATE_STILL:
								// log the feature
								predClass=",t";
								//release the lock
								mWakeLockManager.unlock(LOCK_TAG);
								outcome=Constants.OUTCOME_NONE;
								break;
							default: 
								outcome=Constants.OUTCOME_NONE;
								break;
							}	
							System.out.println(predClass);
						}
				}
			}
			
			
			boolean logDetectionOn=mPrefs.getBoolean(Constants.LOGGING_DETECTION_SWITCH, false);
			switch(outcome){
			case Constants.OUTCOME_PARKING:
			case Constants.OUTCOME_UNPARKING:
				long notiInterval=mPrefs.getInt(Constants.DETECTION_INTERVAL, Constants.DETECTION_INTERVAL_IN_SECOND);
				long lastSameTypeDetectionTimestamp;
				if(outcome==Constants.OUTCOME_PARKING) lastSameTypeDetectionTimestamp=lastParkingTimestamp;
				else lastSameTypeDetectionTimestamp=lastUnparkingTimestamp;
				
				if(System.currentTimeMillis()-lastSameTypeDetectionTimestamp>notiInterval*1000){
					if(outcome==Constants.OUTCOME_PARKING) lastParkingTimestamp=System.currentTimeMillis();
					else lastUnparkingTimestamp=System.currentTimeMillis();
					/**
					 * log detection outcome and time
					 */
					Toast.makeText(getApplicationContext(), "\ndetected "+CommonUtils.eventCodeToString(outcome)+" at time: "+CommonUtils.formatTimestamp( new Date(), "HH:mm:ss" ), Toast.LENGTH_LONG).show();
					
					if(logOn){
						String logMsg="\ndetected "+CommonUtils.eventCodeToString(outcome)+" at time: "+CommonUtils.formatTimestamp( new Date(), "HH:mm:ss" )+"\n";
						
						if(mFusionManager.fusionProcessLog.toString().length()>0
							&&logDetectionOn){
							logMsg+=mFusionManager.fusionProcessLog.toString()+"\n";
						}
						mLogManager.log(logMsg, Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_DETECTION_REPORT]);
							
					}
					 /*mXPSHandler.getLocation(null
					 ,WPSStreetAddressLookup.WPS_FULL_STREET_ADDRESS_LOOKUP
					 , new XPSLocationCallback(eventCode) );*/
					mLocationClient.requestLocationUpdates( 
	    			LocationRequest.create()
	    			.setNumUpdates(1)
	    			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY), 
					new LocationClientListener(outcome));
				}
				break;
			case Constants.OUTCOME_NONE:
				if(logOn){
					if(logDetectionOn){
						mLogManager.log("outcome="+outcome+"\n"+mFusionManager.fusionProcessLog.toString()+"\n", Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_DETECTION_REPORT]);
					}
				}
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
			detectionTextView=(TextView) findViewById(R.id.detection);
//			detectionTextView.setText(DETECTION_PREFIX+"none");
			
			//indicatorTextView=(TextView) findViewById(R.id.indicator);
			//indicatorTextView.setText(INDICATOR_PREFIX);
			
			// set up the map view
			setupMapIfNeeded();
			//set up the location client
			setupLocationClientIfNeeded();
			
			
		

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
	        
	        mBroadcastFilter.addAction(BLUETOOTH_CONNECTION_ACK);
	        mBroadcastManager.registerReceiver(bluetoothReceiver, mBroadcastFilter);
	
	        // Get detection requester and remover objects
	        mDetectionRequester = new DetectionRequester(this);
	        mDetectionRemover = new DetectionRemover(this);
	
	        // Get the instance of the customized notification manager
	        mEventDetectionNotificationManager=EventDetectionNotificationManager.getInstance(this);
	        
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
	    * Initialize fields for IODetector
	    */
	    cellTowerChart = new CellTowerChart((TelephonyManager)getSystemService(TELEPHONY_SERVICE),this);
		magnetChart = new MagnetChart(mSensorManager,this);	
		lightChart = new LightChart(mSensorManager,this);   
		
		//new AggregatedIODetector().execute("");//Check the detection for the first time
		
		//This timer handle starts the aggregated calculation for the detection
 		//Interval 1 seconds.
		Timer uiTimer = new Timer();
		mIODectorHandler = new Handler();
		uiTimer.scheduleAtFixedRate(new TimerTask() {
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
		}, 0, 1000);

		
		
	   /**
        * Initialize fields other than managers
        */
	        lastAccReading=new double[3]; 
	        
	    /**
         * Startup routines
         */
	        // catch the force close error
	        Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MainActivity.this));

	        
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
    	Log.d(LOG_TAG, LOG_TAG+" activity has started");
    	super.onStart();
    }
    
    @Override
    protected void onPause() {
    	Log.d(LOG_TAG, LOG_TAG+" activity has paused");
        super.onPause();
    	
        // Stop listening to broadcasts when the Activity isn't visible.
        //mBroadcastManager.unregisterReceiver(updateListReceiver);
       
      /*  if (mLocationClient != null) {
            mLocationClient.disconnect();
        }*/
    }
  
    @Override
    protected void onStop(){
    	Log.d(LOG_TAG, LOG_TAG+" activity has stopped");
    	super.onStop();
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
		classifierForCIVOnButton.setChecked(mPrefs.getBoolean(Constants.CIV_CLASSIFIER_ON, false));
		
		final ToggleButton isOutdoorButton=(ToggleButton)dialog.findViewById(R.id.is_outdoor);
		isOutdoorButton.setChecked(mPrefs.getBoolean(Constants.IS_OUTDOOR, false));
		
		final EditText notificationTresholdText=(EditText)dialog.findViewById(R.id.notification_threshold);
		notificationTresholdText.setText(String.format("%.2f", mPrefs.getFloat(Constants.NOTIFICATION_THRESHOLD, (float)Constants.DEFAULT_DETECTION_THRESHOLD)) );
		
		final EditText detectionIntervalText=(EditText)dialog.findViewById(R.id.detection_interval);
		detectionIntervalText.setText(
				String.valueOf(mPrefs.getInt(Constants.DETECTION_INTERVAL, Constants.DETECTION_INTERVAL_IN_SECOND) ));
		
		
		final ToggleButton logAcclRawButton=(ToggleButton)dialog.findViewById(R.id.log_raw_switch);
		logAcclRawButton.setChecked(mPrefs.getBoolean(Constants.LOGGING_ACCL_RAW_SWITCH, false));
		
		final ToggleButton logAcclFeaturesButton=(ToggleButton)dialog.findViewById(R.id.log_accl_features_switch);
		logAcclFeaturesButton.setChecked(mPrefs.getBoolean(Constants.LOGGING_ACCL_FEATURES_SWITCH, false));
		
		final ToggleButton logDetectionButton=(ToggleButton)dialog.findViewById(R.id.log_report_switch);
		logDetectionButton.setChecked(mPrefs.getBoolean(Constants.LOGGING_DETECTION_SWITCH, false));
		
		final ToggleButton logErrorButton=(ToggleButton)dialog.findViewById(R.id.log_error_switch);
		logErrorButton.setChecked(mPrefs.getBoolean(Constants.LOGGING_ERROR_SWITCH, false));
		

		//final EditText deltaForConditionalProb=(EditText)dialog.findViewById(R.id.normal_dist_delta);
		//deltaForConditionalProb.setText(String.valueOf(mPrefs.getFloat(Constants.CIV_DELTA_CONDITIONAL_PROBABILITY, 2)) );		
		
		final Button applyButton = (Button) dialog.findViewById(R.id.performance_apply_button);
		final Button cancelButton = (Button) dialog.findViewById(R.id.peformance_cancel_button);
		applyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				if (classifierForCIVOnButton.isChecked())
					editor.putBoolean(Constants.CIV_CLASSIFIER_ON, true);
				else
					editor.putBoolean(Constants.CIV_CLASSIFIER_ON, false);

				if (isOutdoorButton.isChecked())
					editor.putBoolean(Constants.IS_OUTDOOR, true);
				else 
					editor.putBoolean(Constants.IS_OUTDOOR, false);

				if (logAcclRawButton.isChecked())
					editor.putBoolean(Constants.LOGGING_ACCL_RAW_SWITCH, true);
				else
					editor.putBoolean(Constants.LOGGING_ACCL_RAW_SWITCH, false);

				if (logAcclFeaturesButton.isChecked())
					editor.putBoolean(Constants.LOGGING_ACCL_FEATURES_SWITCH,
							true);
				else
					editor.putBoolean(Constants.LOGGING_ACCL_FEATURES_SWITCH,
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
				editor.putFloat(Constants.NOTIFICATION_THRESHOLD, notificationTreshold);
				
				
				int detectionInterval;
				try{
					detectionInterval=Integer.parseInt(
							detectionIntervalText.getText().toString());
				}catch(Exception ex){
					detectionInterval=Constants.DETECTION_INTERVAL_IN_SECOND;
				}
				editor.putInt(Constants.DETECTION_INTERVAL, detectionInterval);
				
				
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
					mDetectionRequester.requestUpdates();
					// If the request was to remove activity recognition updates
				} else if (Constants.REQUEST_TYPE.REMOVE == mRequestType) {
					/*
					 * Restart the removal of all activity recognition updates
					 * for the PendingIntent.
					 */
					mDetectionRemover.removeUpdates(mDetectionRequester
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
	private void actionsOnParkingLocation(int eventCode, Location location, String address){
		//latestLocation=getLatestLocationFromIndividualProvider(location);
		int resID;
		String prefix;
		float markerColor;
		if(eventCode==Constants.OUTCOME_PARKING){
			resID=R.raw.vehicle_parked;
			prefix=Constants.PARKING_NOTIFICATION;
			markerColor=BitmapDescriptorFactory.HUE_AZURE;
		}else{
			resID=R.raw.vehicle_deparked;
			prefix=Constants.UNPARKING_NOTIFICATION;
			markerColor=BitmapDescriptorFactory.HUE_RED;
		}
			
		//String curTimeString=CommonUtils.formatTimestamp(new Date(),formatTemplate);
		String curTimeString=CommonUtils.formatTimestamp( new Date(location.getTime()), "HH:mm:ss   " );
		Log.e(LOG_TAG, curTimeString+" \n"+location.toString() );	
		
		/*
		 * actions
		 */
		//1. send the text notification
		String notificationMsg=prefix+" "+curTimeString;
		if(address!=null) notificationMsg+=address;
		mEventDetectionNotificationManager.sendTextNotification(notificationMsg);
					
		//2. play the sound
		mEventDetectionNotificationManager.playVoiceNotification(resID);
		
		//3. log the address of event
		String logMsg=prefix+"\nlocatoin retrieval time:"+curTimeString+"\nlocation:"+location.toString()+"\n";
		if(address!=null){
			logMsg+=address+"\n";
			logMsg+=pastEnvironments.toString()+"\n"+pastMotionStates+"\n";
		}
		mLogManager.log(logMsg, Constants.LOG_FILE_TYPE[Constants.LOG_TYPE_DETECTION_REPORT]);
		
		//4. show on the map
		mMap.clear();
		mEventDetectionNotificationManager.addMarkersToMap(mMap, curTimeString, prefix
				, location.getLatitude(), location.getLongitude(), location.getAltitude(),	markerColor);
		//center and zoom in the map
		CameraPosition cameraPosition = new CameraPosition.Builder()
	    .target(new LatLng(location.getLatitude(), location.getLongitude()) )     // Sets the center of the map to Mountain View
	    .zoom(17)                   // Sets the zoom
	    //.bearing(90)                // Sets the orientation of the camera to east
	    //.tilt(30)                   // Sets the tilt of the camera to 30 degrees
	    .build();                   // Creates a CameraPosition from the builder
		mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));		
		//add a marker on the map				
		Log.e(LOG_TAG, "operations on map completed");
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
  private boolean serviceAvailable() {

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
    public void onStartUpdates(View view) {

        // Check for Google Play services
        if (!serviceAvailable()) {
            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = Constants.REQUEST_TYPE.ADD;

        // Pass the update request to the requester object
        mDetectionRequester.requestUpdates();
    }

    /**
     * Respond to "Stop" button by canceling updates.
     * @param view The view that triggered this method.
     */
    public void onStopUpdates(View view) {

        // Check for Google Play services
        if (!serviceAvailable()) {
            return;
        }

        /*
         * Set the request type. If a connection error occurs, and Google Play services can
         * handle it, then onActivityResult will use the request type to retry the request
         */
        mRequestType = Constants.REQUEST_TYPE.REMOVE;

        // Pass the remove request to the remover object
        mDetectionRemover.removeUpdates(mDetectionRequester.getRequestPendingIntent());

        /*
         * Cancel the PendingIntent. Even if the removal request fails, canceling the PendingIntent
         * will stop the updates.
         */
        PendingIntent pIntent=mDetectionRequester.getRequestPendingIntent();
        if(pIntent!=null) pIntent.cancel();
        
        //Stop listening to accelerometer readings
        mSensorManager.unregisterListener(mSensorEventListener);
        
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

}
