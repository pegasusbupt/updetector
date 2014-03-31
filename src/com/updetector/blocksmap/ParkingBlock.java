package com.updetector.blocksmap;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

public class ParkingBlock {
	public LatLng meterLocation;
	public LatLng startLocation;
	public LatLng endLocation;
	public String meterAddress;
	public double availability = 0.5;
	public Polyline display;
	
	public ParkingBlock(String meterAddress, LatLng meterLocation, LatLng startLocation, LatLng endLocation) {
		this.meterAddress = meterAddress;
		this.meterLocation = meterLocation;
		this.startLocation = startLocation;
		this.endLocation = endLocation;
	}
	
	public void changeAvailability(double availability) {
		this.availability = availability;
		display.setColor(getColorByAvailability());
	}
	
	public int getColorByAvailability() {
		int color;
		if (availability < 0.3) {
			color = Color.RED;
		} else if (availability < 0.6) {
			color = Color.YELLOW;
		} else {
			color = Color.GREEN;
		}
		return color;
	}
	
    public double distanceOfTwoPoints(LatLng pp1, LatLng pp2)
    {
        double result;
        result = Math.sqrt(Math.pow((pp2.longitude-pp1.longitude),2.0)+ Math.pow((pp2.latitude-pp1.latitude),2.0));
        return result;
    }

    public double distanceToPoint(LatLng p)
    {
        double distance;
        distance = Math.pow((endLocation.longitude - startLocation.longitude),2.0) + Math.pow((endLocation.latitude - startLocation.latitude),2.0);
        if (distance == 0)
        {
            return distanceOfTwoPoints(p, startLocation);
        }
        else
        {
            double tx = (((p.longitude - startLocation.longitude)*(endLocation.longitude-startLocation.longitude)+(p.latitude-startLocation.latitude)*(endLocation.latitude-startLocation.latitude))/distance);
            if (tx < 0)
                return distanceOfTwoPoints(startLocation, p);
            else if (tx > 1)
                return distanceOfTwoPoints(endLocation, p);
            else
            {
                double lat = startLocation.latitude + tx * (endLocation.latitude - startLocation.latitude);
                double lng = startLocation.longitude + tx * (endLocation.longitude - startLocation.longitude);
                LatLng pp = new LatLng(lat, lng);
                return distanceOfTwoPoints(pp, p);
            }
        }
    }
}
