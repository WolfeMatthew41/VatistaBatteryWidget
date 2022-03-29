package com.battery.vatista;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

public class ScreenMonitorService extends JobIntentService {
	private static BroadcastReceiver screenOffReceiver;
	private static BroadcastReceiver screenOnReceiver;
	private static BroadcastReceiver userPresentReceiver;

	static final int JOB_ID= 4115;

	static void enqueueWork(Context context, Intent work){
		enqueueWork(context, ScreenMonitorService.class, JOB_ID, work);
	}

	@Override
	protected void onHandleWork(@NonNull Intent intent){
		SideEffectManager.effect("onHandleWork()");

	}

/*
	@Override
	public IBinder onBind(@NonNull Intent arg0) {
		return null;
	}
*/
	@Override
	public void onCreate() {
		super.onCreate();

		registerScreenOffReceiver();
		registerScreenOnReceiver();
		registerUserPresentReceiver();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(screenOffReceiver);
		unregisterReceiver(screenOnReceiver);
		unregisterReceiver(userPresentReceiver);
	}
	
	private void registerScreenOffReceiver() {
		screenOffReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				SideEffectManager.effect(intent.getAction());
				VBWidget.changeAlarm(context, false);
			}
			
		};
		
		registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
	}

	private void registerScreenOnReceiver() {
		screenOnReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				SideEffectManager.effect(intent.getAction());
				
				KeyguardManager keyguardManager = (KeyguardManager)
						context.getSystemService(Context.KEYGUARD_SERVICE);
				if (!keyguardManager.inKeyguardRestrictedInputMode())
					VBWidget.changeAlarm(context, true);
			}
			
		};

		registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
	}
	
	private void registerUserPresentReceiver() {
		userPresentReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				SideEffectManager.effect(intent.getAction());
				
				VBWidget.changeAlarm(context, true);
			}
			
		};

		registerReceiver(userPresentReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
	}
	
}
