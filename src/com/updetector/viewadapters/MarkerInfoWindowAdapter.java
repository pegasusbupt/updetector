package com.updetector.viewadapters;

import android.app.Activity;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.updetector.R;

public class MarkerInfoWindowAdapter implements InfoWindowAdapter{
	 private LayoutInflater mLayoutInflater;

     public MarkerInfoWindowAdapter(LayoutInflater inflater) {
    	 mLayoutInflater=inflater;     
     }



     @Override
     public View getInfoContents(Marker marker) {
    	 View view = mLayoutInflater.inflate(R.layout.marker_custom_info_contents, null);
    	 
    	 String title = marker.getTitle();
         TextView titleUi = ((TextView) view.findViewById(R.id.title));
         if (title != null) {
             // Spannable string allows us to edit the formatting of the text.
             SpannableString titleText = new SpannableString(title);
             titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
             titleUi.setText(titleText);
         } else {
             titleUi.setText("");
         }

         String snippet = marker.getSnippet();
         TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
         if (snippet != null ) {
             /*SpannableString snippetText = new SpannableString(snippet);
             snippetText.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, 10, 0);
             snippetText.setSpan(new ForegroundColorSpan(Color.BLUE), 12, snippet.length(), 0);
             */
        	 snippetUi.setText(snippet);
         } else {
             snippetUi.setText("");
         }
    	 
         return view;
     }


	@Override
	public View getInfoWindow(Marker arg0) {
	    //This means that getInfoContents will be called.
		return null;
	}
}
