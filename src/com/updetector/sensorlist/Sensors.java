package com.updetector.sensorlist;

import com.updetector.R;
import com.updetector.R.string;
import com.updetector.viewadapters.SensorListAdapter;

import android.R.integer;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class Sensors extends ListActivity
{
	public static final String LOG_TAG =Sensors.class.getCanonicalName();

	public static final String PREF_CAPTURE_STATE = "captureState";
    public static final String PREF_SAMPLING_SPEED = "samplingSpeed";
    public static final boolean DEBUG = true;
    public static final String PREF_FILE = "prefs";
    
    public static final int MENU_SETTINGS = 1;
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		if( savedInstanceState != null ) {
			samplingSensorPositions = savedInstanceState.getIntegerArrayList( SAMPLING_SERVICE_POSITION_KEY);
		}
        setContentView(R.layout.sensor_main);
        SensorManager sensorManager = 
                (SensorManager)getSystemService( SENSOR_SERVICE  );
        ArrayList<SensorItem> items = new ArrayList<SensorItem>();
        List<Sensor> sensors = sensorManager.getSensorList( Sensor.TYPE_ALL );
        for( int i = 0 ; i < sensors.size() ; ++i )
            items.add( new SensorItem( sensors.get( i ) ) );
    	for(int samplingService: samplingSensorPositions){
        	SensorItem item = items.get( samplingService );
        	item.setSampling( true );
        }
        ListView lv = getListView();
        listAdapter = new SensorListAdapter( this, items);
        lv.setAdapter(  listAdapter );
       
        // Set up the long click handler
		lv.setOnItemLongClickListener( 
			new AdapterView.OnItemLongClickListener() {
                public boolean onItemLongClick(
					AdapterView<?> av, 
					View v, 
					int pos, 
					long id) {
                        onLongListItemClick(v,pos,id);
                        return true;
        		}
			});
    }

// Save the information that 
	protected void onSaveInstanceState(Bundle outState) {
		outState.putIntegerArrayList( SAMPLING_SERVICE_POSITION_KEY, samplingSensorPositions ); 
	}

	protected void onDestroy() {
		super.onDestroy();
		if( Sensors.DEBUG )
			Log.d( LOG_TAG, "onDestroy" );
	}

	protected void onLongListItemClick( View v, int pos, long id) {
		if( Sensors.DEBUG )
			Log.d( LOG_TAG, "onLongListItemClick pos: "+pos+"; id: " + id );
// If the sensor is already sampling 
		if( samplingSensorPositions.contains(pos) )
			stopSamplingService(pos);
		else
			// If no sampling is running then just start the sampling on the sensor
			startSamplingService( pos );
	}


    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_SETTINGS, 1, R.string.sensor_settings );
        return result;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
        switch( id ) {
            case MENU_SETTINGS:
                Intent i = new Intent(Sensors.this, SensorSettings.class);
                startActivity( i );
                break;

        }
        return true;
    }

    protected void onListItemClick(
            ListView l,
            View v,
            int position,
            long id) {
        Sensor sensor = ((SensorItem)listAdapter.getItem( position )).getSensor();
        String sensorName = sensor.getName();
        Intent i = new Intent(Sensors.this, SensorMonitor.class);
        i.putExtra( "sensorname",sensorName );
        
        startActivity( i );
    }

	private void startSamplingService( int position ) {
		//stopSamplingService();
		SensorItem item = (SensorItem)listAdapter.getItem( position );
		item.setSampling( true );
		listAdapter.notifyDataSetChanged();
		
        Intent i = new Intent(Sensors.this, SamplingService.class);
        i.putExtra( "SENSOR_NAME",  item.getSensor().getName());
        i.putExtra("START", true);
        
        startService( i );
        samplingSensorPositions.add(position);
	}

	private void stopSamplingService(int position) {
		SensorItem item = (SensorItem)listAdapter.getItem( position );
		item.setSampling( false );
		listAdapter.notifyDataSetChanged();
		
		Intent i = new Intent(Sensors.this, SamplingService.class);
		i.putExtra( "SENSOR_NAME", item.getSensor().getName() );
		i.putExtra("START", false);
		
		startService(i);
		samplingSensorPositions.remove((Integer)position);
	}

    private SensorListAdapter listAdapter;
	private ArrayList<Integer> samplingSensorPositions = new ArrayList<Integer>();
	private static final String SAMPLING_SERVICE_POSITION_KEY = "samplingServicePositon";
}
