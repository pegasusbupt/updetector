package com.updetector.sensorlist;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.PowerManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.hardware.SensorManager;
import android.util.Config;
import android.util.Log;
import android.util.LogPrinter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Locale;


import weka.classifiers.Classifier;

import com.updetector.CommonUtils;
import com.updetector.Constants;
import com.updetector.R;
import com.updetector.classification.ClassificationManager;
import com.updetector.classification.WekaClassifier;
import com.updetector.managers.LogManager;
import com.updetector.managers.WakeLockManager;

@SuppressLint("CommitPrefEdits")
public class SamplingService extends Service implements SensorEventListener {
	public static final String LOG_TAG = SamplingService.class.getCanonicalName();
	
	private static final String LOCK_TAG="SamplingInProgress";
	private static final boolean KEEPAWAKE_HACK = false;
	private static final boolean MINIMAL_ENERGY = false;
	private static final long MINIMAL_ENERGY_LOG_PERIOD = 15000L;
	
	
    // Instantiates a log file utility object, used to log status updates
    private LogManager mLogFile;
    private WakeLockManager mWakeLockManager;
    private SensorManager sensorManager;
    private HashMap<String, PrintWriter> logFiles;

    private ScreenOffBroadcastReceiver screenOffBroadcastReceiver = null;
	private GenerateUserActivityThread generateUserActivityThread = null;
	
	private long logCounter = 0;
	private Date samplingStartedTimeStamp;
	
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand( intent, flags, startId );
		
		if(mLogFile==null) mLogFile=LogManager.getInstance(getApplicationContext());
		if(mWakeLockManager==null) mWakeLockManager=WakeLockManager.getInstance(getApplicationContext());
		if(sensorManager==null) sensorManager = (SensorManager)getSystemService( SENSOR_SERVICE  );
		
		if(logFiles==null) logFiles=new HashMap<String, PrintWriter>();
		//stopSampling();		// just in case the activity-level service management fails
      	
		String sensorName = intent.getStringExtra( "SENSOR_NAME" );
		boolean startSampling=intent.getBooleanExtra("START", false);
       
		if( Sensors.DEBUG )	Log.d( LOG_TAG,"sensorName: "+sensorName+ "  start="+startSampling );

        	
		screenOffBroadcastReceiver = new ScreenOffBroadcastReceiver();
		IntentFilter screenOffFilter = new IntentFilter();
		screenOffFilter.addAction( Intent.ACTION_SCREEN_OFF );
		if( KEEPAWAKE_HACK ) registerReceiver( screenOffBroadcastReceiver, screenOffFilter );
		
		if(startSampling) startSamplingSensor(sensorName);
		else stopSamplingSensor(sensorName);
		
		return START_NOT_STICKY;
	}

	public void onDestroy() {
		super.onDestroy();
		if( Sensors.DEBUG ) Log.d( LOG_TAG, "onDestroy" );
		stopSamplingService();
		if( KEEPAWAKE_HACK ) unregisterReceiver( screenOffBroadcastReceiver );
	}

	public IBinder onBind(Intent intent) {
		return null;	// cannot bind
	}

	// SensorEventListener
    public void onAccuracyChanged (Sensor sensor, int accuracy) {
    }
    

    public void onSensorChanged(SensorEvent sensorEvent) {
		++logCounter;
		String sensorName=sensorEvent.sensor.getName();
		//if the phone's energy is above the minimum
    	if( !MINIMAL_ENERGY ) {
    		StringBuilder b = new StringBuilder();
    		//print the sensor reading to the log console
    		if( Sensors.DEBUG ){
    			b.append(CommonUtils.formatTimestamp(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
    			for( int i = 0 ; i < sensorEvent.values.length ; ++i ) {
   					b.append( " " );
    				b.append(String.format("%.3f", sensorEvent.values[i] ) );
    			}
    			Log.d( LOG_TAG, sensorName+ " onSensorChanged: ["+b+"]" );
    		}
    		
    		
    		//save the sensor reading to the log file
    		if( logFiles.containsKey(sensorName)  ) {    			
    			logFiles.get(sensorName).println(b);
    		} 
    	} else {
    		++logCounter;
    		if( ( logCounter % MINIMAL_ENERGY_LOG_PERIOD ) == 0L )
    			Log.d( LOG_TAG, "logCounter: "+logCounter+" at "+new Date().toString());
    	}
    }

    
	private void stopSamplingService() {
		if( generateUserActivityThread != null ) {
			generateUserActivityThread.stopThread();
			generateUserActivityThread = null;
		}
		if( sensorManager != null ) sensorManager.unregisterListener( this );
		mWakeLockManager.unlock(LOCK_TAG);
	}
	
	
	private Sensor getSensor(String sensorName){
		List<Sensor> sensors = sensorManager.getSensorList( Sensor.TYPE_ALL );
		Sensor selectedSensor = null;;
        for( int i = 0 ; i < sensors.size() ; ++i ){
            if( sensorName.equals( sensors.get( i ).getName() ) ) {
                selectedSensor = sensors.get( i );
                break;
            }
        }
        return selectedSensor;
	}
	
	private void stopSamplingSensor(String sensorName){
	 if( logFiles.containsKey(sensorName)) {
            logFiles.get(sensorName).close();
			logFiles.remove(sensorName);
			
			sensorManager.unregisterListener(this, getSensor(sensorName));
			
			/*Date samplingStoppedTimeStamp = new Date();
			long secondsEllapsed = 
				( samplingStoppedTimeStamp.getTime() -
				  samplingStartedTimeStamp.getTime() ) / 1000L;
			Log.d(LOG_TAG, "Sampling started: "+
					samplingStartedTimeStamp.toString()+
					"; Sampling stopped: "+
					samplingStoppedTimeStamp.toString()+
					" ("+secondsEllapsed+" seconds) "+
					"; samples collected: "+logCounter );*/
	 }
	 //stop Sampling Service
	 if(logFiles.size()==0) stopSamplingService();
	}
	
	private void startSamplingSensor(String sensorName) {
        if( sensorName != null ) {
            Sensor selectedSensor = getSensor(sensorName);
            if( selectedSensor != null ) {
            	if( Sensors.DEBUG ) Log.d( LOG_TAG, "registerListener/SamplingService" );
           		sensorManager.registerListener(this, selectedSensor, SensorManager.SENSOR_DELAY_UI);
           		
           		mWakeLockManager.lock(LOCK_TAG);
    			
                Context mContext=getApplicationContext();

    			
    			int mFileNumber;
    			// Open the shared preferences repository
    	        SharedPreferences   mPrefs = mContext.getSharedPreferences(Constants.SHARED_PREFERENCES,
    	                Context.MODE_PRIVATE);
    	        // If it doesn't contain a file number, set the file number to 1
    	        if (!mPrefs.contains(Constants.LOG_FILE_TYPE[1]+"_"+sensorName)) {
    	            mFileNumber = 1;
    	        // Otherwise, get the last-used file number and increment it.
    	        } else {
    	            mFileNumber = mPrefs.getInt(sensorName+"_"+Constants.PREFERENCE_KEY_TYPE[0], 0) + 1;
    	        }
    	        Editor editor=mPrefs.edit();
    	        editor.putInt(sensorName+"_"+Constants.PREFERENCE_KEY_TYPE[0], mFileNumber);

    	       	// log file name for the sensor 
    			String mFileName = new SimpleDateFormat("yyyy_MM_dd", Locale.US).format(new Date())
    	                +mFileNumber
    	                +"_"+sensorName.replaceAll(" ", "_")
    	                +".log";
    			File logDirectory = new File(mContext.getExternalFilesDir(null)+File.separator);
    			/*if(!logDirectory.exists())//create the directory for the selected sensor if the sensor does not exists
    				logDirectory.mkdirs();*/
    			Log.e(LOG_TAG, logDirectory.getAbsolutePath());
    			File captureFileName = new File(logDirectory, mFileName );

    			//initialize the log file for the sensor
    			try {
                    logFiles.put(sensorName, new PrintWriter( new FileWriter( captureFileName, false)) );
                } catch( IOException ex ) {
                    Log.e( LOG_TAG, ex.getMessage(), ex );
                }
                
    			if( Sensors.DEBUG ) Log.d( LOG_TAG, "Capture file created" );
			}

            
        }
	}


    

	class ScreenOffBroadcastReceiver extends BroadcastReceiver {
		private static final String LOG_TAG = "ScreenOffBroadcastReceiver";

		public void onReceive(Context context, Intent intent) {
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "onReceive: "+intent );
			if( sensorManager != null ) {
				if( generateUserActivityThread != null ) {
					generateUserActivityThread.stopThread();
					generateUserActivityThread = null;
				}
				generateUserActivityThread = new GenerateUserActivityThread();
				generateUserActivityThread.start();
			}
		}
	}

	class GenerateUserActivityThread extends Thread {
		public void run() {
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "Waiting 2 sec for switching back the screen ..." );
			try {
				Thread.sleep( 2000L );
			} catch( InterruptedException ex ) {}
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "User activity generation thread started" );

			PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
			userActivityWakeLock = 
				pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, 
						"GenerateUserActivity");
			userActivityWakeLock.acquire();
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "User activity generation thread exiting" );
		}

		public void stopThread() {
			if( Sensors.DEBUG )
				Log.d( LOG_TAG, "User activity wake lock released" );
			userActivityWakeLock.release();
			userActivityWakeLock = null;
		}

		PowerManager.WakeLock userActivityWakeLock;
	}
}

