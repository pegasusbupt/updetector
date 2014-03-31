package com.updetector.sensorlist;

import android.hardware.Sensor;

public class SensorItem {
    SensorItem( Sensor sensor ) {
        this.sensor = sensor;
        this.sampling = false;
    }

    public String getSensorName() {
        return sensor.getName();
    }

    Sensor getSensor() {
        return sensor;
    }

    void setSampling( boolean sampling ) {
    	this.sampling = sampling;
    }
    
    public boolean getSampling() {
    	return sampling;
    }
    
    private Sensor sensor;
    private boolean sampling;
}
