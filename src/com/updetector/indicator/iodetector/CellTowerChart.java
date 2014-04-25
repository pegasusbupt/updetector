package com.updetector.indicator.iodetector;

import java.util.ArrayList;
import java.util.List;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.SparseIntArray;

public class CellTowerChart {

	public enum PrevStatus{
		NO_INPUT(0),
		INDOOR(1),
		SEMI_OUTDOOR(2),
		OUTDOOR(3);

		private int value;

		PrevStatus(int value){
			this.setValue(value);
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}

	TelephonyManager telephonyManager;
	//GsmCellLocation cellLocation;
	CellLocation cellLocation;

	private DetectionProfile[] listProfile = new DetectionProfile[3];
	private DetectionProfile indoor, semi, outdoor;

	private int currentCID = -1;
	
	
	public int currentSignalStrength = -1;
	//TODO may have different ranges under different cellular networks 
	//ref http://forums.androidcentral.com/google-nexus-5/333298-what-your-signal-strength-your-cell-network-provider-your-n5-dbm-asu.html
	public double currentASU=0;
	
	private int time=0;

	private XYSeries connectedCellSeries;
	private ArrayList<XYSeries> neighboringCellDataSet = new ArrayList<XYSeries>();

	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();

	private XYSeriesRenderer connectedCellRenderer = new XYSeriesRenderer();
	private ArrayList<XYSeriesRenderer> neighboringCellRenderers = new ArrayList<XYSeriesRenderer>();

	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer(); // Holds a collection of XYSeriesRenderer and customizes the graph

	private static GraphicalView cellView;

	private SparseIntArray cellArray;

	private final int THRESHOLD = 13;

	private PrevStatus prevStatus = PrevStatus.NO_INPUT;

	public CellTowerChart(TelephonyManager tManager,Context context){
		this.telephonyManager = tManager;

		indoor = new DetectionProfile("indoor");
		semi = new DetectionProfile("semi-outdoor");
		outdoor = new DetectionProfile("outdoor");
		listProfile[0]=indoor;
		listProfile[1]=semi;
		listProfile[2]=outdoor;

		telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_SIGNAL_STRENGTH);
		cellLocation = telephonyManager.getCellLocation();
		int phoneType = telephonyManager.getPhoneType();
		if (phoneType == TelephonyManager.PHONE_TYPE_GSM)
			currentCID = ((GsmCellLocation)cellLocation).getCid();
		else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA)
			currentCID = ((CdmaCellLocation)cellLocation).getBaseStationId();

		connectedCellSeries = new XYSeries(Integer.toString(currentCID));

		mDataset.addSeries(connectedCellSeries);

		connectedCellRenderer.setColor(Color.GREEN);
		connectedCellRenderer.setPointStyle(PointStyle.CIRCLE);
		connectedCellRenderer.setFillPoints(true);

		mRenderer.setXTitle("Time (s)");
		mRenderer.setLabelsTextSize(10);
		mRenderer.setLabelsColor(Color.BLACK);
		mRenderer.setAntialiasing(true);
		mRenderer.setYTitle("RSSI (dBm)");
		mRenderer.setBackgroundColor(Color.BLACK);
		mRenderer.setYAxisMin(-120);
		mRenderer.setYAxisMax(-50);
		mRenderer.setPanEnabled(false,false);
		mRenderer.setMarginsColor(Color.WHITE);
		mRenderer.setAxesColor(Color.parseColor("#707070"));
		mRenderer.setShowGrid(true);
		mRenderer.setXLabels(5);
		mRenderer.addSeriesRenderer(connectedCellRenderer);

		cellView = ChartFactory.getLineChartView(context, mDataset, mRenderer);
	}

	public void setPrevStatus(int status){
		if(status == 0)
			prevStatus = PrevStatus.INDOOR;
		else if (status == 1) {
			prevStatus = PrevStatus.SEMI_OUTDOOR;
		}else {
			prevStatus = PrevStatus.OUTDOOR;
		}
	}

	public GraphicalView getView(){

		cellLocation = telephonyManager.getCellLocation();
		int phoneType = telephonyManager.getPhoneType();
		if (phoneType == TelephonyManager.PHONE_TYPE_GSM)
			currentCID = ((GsmCellLocation)cellLocation).getCid();
		else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA)
			currentCID = ((CdmaCellLocation)cellLocation).getBaseStationId();
		connectedCellSeries.setTitle(Integer.toString(currentCID));
		connectedCellSeries.add(time, currentSignalStrength);
		if(connectedCellSeries.getItemCount()>25)
			connectedCellSeries.remove(0);
		setNeighboringInfo();
		time++;
		cellView.repaint();
		return cellView;
	}


	public String getCellTowerInfo()
	{
		//Connected Cell Tower
		cellLocation = telephonyManager.getCellLocation();
		int phoneType = telephonyManager.getPhoneType();
		if (phoneType == TelephonyManager.PHONE_TYPE_GSM)
			currentCID = ((GsmCellLocation)cellLocation).getCid();
		else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA)
			currentCID = ((CdmaCellLocation)cellLocation).getBaseStationId();
		return "Connected id: " + currentCID + ", RSSI:" + currentSignalStrength + "dBm";
	}

	private void setNeighboringInfo(){
		//get all the available neighboring tower
		List<NeighboringCellInfo> NeighboringList = telephonyManager.getNeighboringCellInfo();

		//Check if each neighboring tower has a renderer available
		if(neighboringCellDataSet.size()!=NeighboringList.size()){
			//There is a tower that doesn't have renderer 
			if(NeighboringList.size()>neighboringCellDataSet.size()){
				//Create the renderer
				for(int i=neighboringCellDataSet.size();i<NeighboringList.size();i++){
					XYSeries tempXySeries = new XYSeries("");
					XYSeriesRenderer tempXyRenderer=new XYSeriesRenderer();
					neighboringCellDataSet.add(tempXySeries);
					mDataset.addSeries(tempXySeries);
					tempXyRenderer.setColor(getColor(neighboringCellRenderers.size()));
					tempXyRenderer.setPointStyle(PointStyle.TRIANGLE);
					tempXyRenderer.setFillPoints(true);
					neighboringCellRenderers.add(tempXyRenderer);
					mRenderer.addSeriesRenderer(tempXyRenderer);
				}
			}else{
				//If there is/are tower(s) that detected but now gone, remove the renderer
				for(int i=NeighboringList.size();i<neighboringCellDataSet.size();i++){
					mRenderer.removeSeriesRenderer(neighboringCellRenderers.get(i));
					mDataset.removeSeries(neighboringCellDataSet.get(i));
					neighboringCellDataSet.remove(i);
					neighboringCellRenderers.remove(i);
				}
			}
		}

		//get the rssi and cid info and add the data to the series
		for(int i=0;i<NeighboringList.size();i++){
			NeighboringCellInfo cellInfo = NeighboringList.get(i);
			XYSeries temp=neighboringCellDataSet.get(i);
			int rssi = cellInfo.getRssi();
			rssi = -113 + rssi *2;
			if(rssi == NeighboringCellInfo.UNKNOWN_RSSI || rssi == 85)
			{
				continue;
			}
			temp.setTitle(String.valueOf(cellInfo.getCid()));
			temp.add(time, rssi);
			if(temp.getItemCount()>25)
				temp.remove(0);
		}
	}
	
	private int getColor(int color){
		switch (color) {
		case 1:
			return Color.BLUE;
		case 2:
			return Color.CYAN;
		case 3:
			return Color.GRAY;
		case 4:
			return Color.RED;
		case 5:
			return Color.YELLOW;
		case 6: 
			return Color.DKGRAY;
		case 7:
			return Color.LTGRAY;
		default:
			return Color.MAGENTA;
		}
	}

	PhoneStateListener phoneStateListener = new PhoneStateListener() 
	{    	
		public void onSignalStrengthChanged(int asu) 
		{
			currentSignalStrength = -113 + 2*asu;//signalStrength.getGsmSignalStrength();
			currentASU=2*asu;
		}
	};
	//Get all the cell rssi at time = 0s
	public void updateProfile(){
		List<NeighboringCellInfo> NeighboringList = telephonyManager.getNeighboringCellInfo();

		cellArray = new SparseIntArray(NeighboringList.size() + 1);
		
		//Save the current connected cell
		cellArray.put(currentCID, currentSignalStrength);
		
		//Save the current detected neighbour
		for(int i=0 ; i< NeighboringList.size();i++){
			NeighboringCellInfo cellInfo = NeighboringList.get(i);
			int rssi = cellInfo.getRssi();
			rssi = -113 + rssi *2;
			if(rssi == NeighboringCellInfo.UNKNOWN_RSSI || rssi == 85)
			{
				continue;
			}
			cellArray.put(cellInfo.getCid(), rssi);
		}
	}
	
	public double[] calculateProfile(){
		int cellCount = 0, oldCellRssi, newCellRssi;
		double inToOut = 0, outToIn = 0;
	
		newCellRssi = currentSignalStrength;//New connected cell rssi
	
		if((oldCellRssi = cellArray.get(currentCID,0)) != 0){//Compare the variance with the old connected cell rssi
			if(newCellRssi - oldCellRssi >= THRESHOLD)
				inToOut++;
			else if (newCellRssi - oldCellRssi <= -THRESHOLD) {
				outToIn++;
			}else{
				if(prevStatus == PrevStatus.INDOOR){
					outToIn++;
				}else if(prevStatus == PrevStatus.OUTDOOR || prevStatus == PrevStatus.SEMI_OUTDOOR){
					inToOut++;
				}
			}
			cellCount++;
		}
		
		List<NeighboringCellInfo> NeighboringList = telephonyManager.getNeighboringCellInfo();
		
		for(int i=0 ; i< NeighboringList.size();i++){//Calculate the cell variance for all the detected neighbour rssi
			NeighboringCellInfo cellInfo = NeighboringList.get(i);
	
			if((oldCellRssi = cellArray.get(cellInfo.getCid(),0)) != 0){
	
				newCellRssi = cellInfo.getRssi();
	
				newCellRssi = -113 + newCellRssi *2;
				
				if(newCellRssi == NeighboringCellInfo.UNKNOWN_RSSI || newCellRssi == 85)
				{
					continue;
				}
				
				if(newCellRssi - oldCellRssi >= THRESHOLD)
					inToOut++;
				else if (newCellRssi - oldCellRssi <= -THRESHOLD) {
					outToIn++;
				}else{//if the changes is between 15dB
					if(prevStatus == PrevStatus.INDOOR){
						outToIn++;
					}else if(prevStatus == PrevStatus.OUTDOOR || prevStatus == PrevStatus.SEMI_OUTDOOR){
						inToOut++;
					}
				}
				cellCount++;
			}
		}
		
		return new double[]{outToIn, inToOut, cellCount};
	}
	
	//Check the cell variance after 10s
	//original getProfile()
	/*public DetectionProfile[] getProfile(){
		double[] values=calculateProfile();
		double outToIn=values[0], inToOut=values[1], cellCount=values[2];

		indoor.setConfidence(outToIn/cellCount);
		semi.setConfidence(inToOut/cellCount);
		outdoor.setConfidence(inToOut/cellCount);

		return listProfile;
	}*/
	
	public DetectionProfile[] getProfile(){
		double highThreshold=-66, lowThreshold=-80, interval=highThreshold-lowThreshold;
		
		if(currentSignalStrength>highThreshold){
			indoor.setConfidence(0);
			semi.setConfidence(1);
			outdoor.setConfidence(1);
		}else{
			if(currentSignalStrength<lowThreshold){
				indoor.setConfidence(1);
				semi.setConfidence(0);
				outdoor.setConfidence(0);
			}else{
				indoor.setConfidence((highThreshold-currentSignalStrength)/interval);
				semi.setConfidence((currentSignalStrength-lowThreshold)/interval);
				outdoor.setConfidence((currentSignalStrength-lowThreshold)/interval);
			}
		}		
		
		return listProfile;
	}
	
	public double getCellTowerDiff(){
		double[] values=calculateProfile(); //update the inToOut and outToIn first
		return values[1]-values[0];
	}

}
