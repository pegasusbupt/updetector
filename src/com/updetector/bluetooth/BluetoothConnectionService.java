package com.updetector.bluetooth;

import com.updetector.Constants;
import com.updetector.MainActivity;
import com.updetector.R;
import com.updetector.R.raw;
import com.updetector.managers.EventDetectionNotificationManager;

import android.R.integer;
import android.R.string;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class BluetoothConnectionService extends Service {
	public static final String LOG_TAG=BluetoothConnectionService.class.getCanonicalName();
	
	private String targetDeviceName;
	private int parkingStatus = Constants.OUTCOME_NONE;
	private long lastStatusChangeTime = 0;
	private boolean toReport = false;

	
	
	private Location parkingPlace = null;
	private EventDetectionNotificationManager mNotificationManagerWrapper;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(LOG_TAG, "Service created");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(LOG_TAG, "Service destroyed");	
		unregisterReceiver(mReceiver);
	}
	
	/** Callback when Bluetooth is connected or disconnected */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			
			if (!device.getName().equals(targetDeviceName))//the connected bt device needs to be the car
				return;
			
			//TODO show something
			String debugInfo="device " + device.getName() + ", " + device.getAddress() + " connected";
			Toast.makeText(getApplicationContext(), debugInfo, Toast.LENGTH_LONG).show();
			Log.i(LOG_TAG, debugInfo);
	        
			if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
	           onBluetoothConnectionChange(true);
	        }
	        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
	        	onBluetoothConnectionChange(false);
	        }
		}
	};
	
	
	/**
	 * 
	 * @param connected: true if connected false in disconnected
	 */
	private void onBluetoothConnectionChange(boolean connected){
		if(connected) Log.e(LOG_TAG, "status: connected");
		else Log.e(LOG_TAG, "status: disconnected");
		
        long curTime = System.currentTimeMillis() / 1000;
        if (curTime - lastStatusChangeTime >Constants.STATUS_CHANGE_INTERVAL_THRESHOLD) {
     	   
        	if(connected) parkingStatus = Constants.OUTCOME_UNPARKING;
        	else parkingStatus=Constants.OUTCOME_PARKING;
        	
        	toReport = true;
        	lastStatusChangeTime = curTime;
        	
        	//locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        	sendConnectionChangeNotificationToMainActivity(parkingStatus);
        }
	}

	

	/** Called when the service is started */
	public int onStartCommand(Intent intent, int flags, int startId) {
		// setup a broadcast receiver that monitors the bluetooth connection
		IntentFilter filter_connected = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
		IntentFilter filter_disconnected = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		registerReceiver(mReceiver, filter_connected);
		registerReceiver(mReceiver, filter_disconnected);
		
		mNotificationManagerWrapper=EventDetectionNotificationManager.getInstance(this);
		
		Log.e(LOG_TAG, "Service started for device. ");
		//writeToMainActivity("Bluetooth connection service started.\n\n");
		
		if(intent!=null){
			SharedPreferences sharedPreferences=getSharedPreferences(Constants.SHARED_PREFERENCES,
		                Context.MODE_PRIVATE);			
			targetDeviceName = sharedPreferences.getString(Constants.BLUETOOTH_CAR_DEVICE_NAME, null);
			
			/*if(targetDeviceName!=null){
				sendConnectionChangeNotificationToMainActivity(Constants.OUTCOME_UNPARKING);
			}*/
		}else{
			Log.e(LOG_TAG, "intent is null");
		}
		
		//createLocationListener();		
		return START_STICKY;
	}
	
	private void sendConnectionChangeNotificationToMainActivity(int eventCode){
		Log.e(LOG_TAG, "Send out the connection change notice ");
		Intent ackIntent = new Intent(Constants.BLUETOOTH_CONNECTION_UPDATE);
		ackIntent.putExtra(Constants.BLUETOOTH_CON_UPDATE_EVENT_CODE, eventCode);
		LocalBroadcastManager.getInstance(this).sendBroadcast(ackIntent);
	}
	
	
	private LocationManager locationManager;
	private LocationListener locationListener;
	public void createLocationListener()
	{
		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		        // Called when a new location is found by the network location provider.
			    parkingPlace = location;
			    locationManager.removeUpdates(this);
			    if (toReport) {
			    	toReport = false;			    	 
			    	String msg = "---------------------------------------\n";
			    	if (parkingStatus == Constants.OUTCOME_UNPARKING){
			    		msg += "Unparked at: ";
			    		mNotificationManagerWrapper.playVoiceNotification(R.raw.vehicle_deparked);
			    	}
			    	else{
			    		msg += "Parked at: ";
			    		mNotificationManagerWrapper.playVoiceNotification(R.raw.vehicle_parked);
			    	}
			    	msg+=parkingPlace.toString() + ".\nDetected by Bluetooth Connection Service.\n";
		    		msg += "---------------------------------------\n\n";
		    		writeToMainActivity(msg);
		    		
		    		
			    } else {
			        String msg = "Location recorded: " + location.toString() + " by Bluetooth Connection Service.\n\n";
			        writeToMainActivity(msg);
			    }
		    }
	
		    public void onStatusChanged(String provider, int status, Bundle extras) {}
	
		    public void onProviderEnabled(String provider) {}
	
		    public void onProviderDisabled(String provider) {}
	
		};
	}
	
		
	
	public void writeToMainActivity(String str) {
		Intent ackIntent = new Intent(Constants.BLUETOOTH_CONNECTION_UPDATE);
		ackIntent.putExtra(Constants.BLUETOOTH_CON_UPDATE_EVENT_CODE, str);
		LocalBroadcastManager.getInstance(this).sendBroadcast(ackIntent);
	}
}
