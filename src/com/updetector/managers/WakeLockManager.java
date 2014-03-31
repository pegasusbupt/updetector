package com.updetector.managers;

import java.util.HashMap;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class WakeLockManager {
	private PowerManager mPowerManager;
	
	private HashMap<String, WakeLock> mWakeLocks;
	
	private static WakeLockManager mWakeLockManager;
	private Context mContext;
	
	private WakeLockManager(Context ctxt){
		mContext=ctxt;
		mPowerManager=(PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
		mWakeLocks=new HashMap<String,WakeLock>();
	}
	
	public static WakeLockManager getInstance(Context ctxt){
		if(mWakeLockManager==null){
			mWakeLockManager=new WakeLockManager(ctxt);
		}
		return mWakeLockManager;
	}
	
	public void lock(String lockTag){
		if(!mWakeLocks.containsKey(lockTag)){
			WakeLock wakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, lockTag);
			wakeLock.acquire();
			mWakeLocks.put(lockTag, wakeLock);
		}
	}
	
	public void unlock(String lockTag){
		if(mWakeLocks.containsKey(lockTag)){
			WakeLock wakeLock=mWakeLocks.get(lockTag);
			wakeLock.release();
			wakeLock=null;
			mWakeLocks.remove(lockTag);
		}
	}

}
