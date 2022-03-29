/*
* Author@ Matthew Wolfe
*
* Description: This is a widget using Vatista from under-night in-birth. The widget's main purpose
* is to display the battery level to the user on the screen of the users choice.
*
* */


package com.battery.vatista;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.SystemClock;
//import android.view.MotionEvent;
//import android.view.View;
import android.widget.RemoteViews;


public class VBWidget extends AppWidgetProvider {
	private static final String ACTION_BATTERY_UPDATE = "com.battery.vatista.action.UPDATE";
	private int batteryLevel = 0;

	@Override
	public void onEnabled(Context context) { //This is to start the screen monitor service
		super.onEnabled(context);

		SideEffectManager.effect("onEnabled()");

		changeAlarm(context, true);
		//context.startService(new Intent(context, ScreenMonitorService.class));
		ScreenMonitorService.enqueueWork(context, new Intent(context, ScreenMonitorService.class));
	}

	public static void changeAlarm(Context context, boolean turnOn) {
		//Possibly change from AlarmManager to Handler
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		//Intent intent = new Intent(ACTION_BATTERY_UPDATE);
		Intent intent = new Intent(context, VBWidget.class);
		//intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		intent.setAction(ACTION_BATTERY_UPDATE);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, getWidgetId(intent));
		PendingIntent pendingIntent =
				PendingIntent.getBroadcast(context, 0, intent, 0);

		if (turnOn) {// Add extra 1 sec because sometimes ACTION_BATTERY_CHANGED
			// is called after the first alarm
			alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
					SystemClock.elapsedRealtime() + 1000,
					75 * 1000, pendingIntent); //Make sure is 300 * 1000 when finished
			SideEffectManager.effect("Alarm set");
		} else {
			alarmManager.cancel(pendingIntent);
			SideEffectManager.effect("Alarm disabled");
		}
	}

	public static int getWidgetId(Intent intent) {
		Bundle extras = intent.getExtras();
		int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
		if (extras != null) {
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
		return appWidgetId;
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		SideEffectManager.effect("onUpdate()");

		// Sometimes when the phone is booting, onUpdate method gets called before onEnabled()
		int currentLevel = calculateBatteryLevel(context);
		if (batteryChanged(currentLevel)) {
			batteryLevel = currentLevel;
			//batteryLevel = 5; //debugging test
			SideEffectManager.effect("Battery changed");
		}
		updateViews(context);
		//changeAlarm(context, true);
	}

	private boolean batteryChanged(int currentLevelLeft) {

		return (batteryLevel != currentLevelLeft);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);

		SideEffectManager.effect("onReceive() " + intent.getAction());

		//changeAlarm(context, true);
		ScreenMonitorService.enqueueWork(context, new Intent(context, ScreenMonitorService.class));
		changeAlarm(context, true);

		//if (Intent.ACTION_GET_CONTENT.equals(intent.getAction())) //idk why this don't work
		if (intent.getAction().equals(ACTION_BATTERY_UPDATE)) {
			int currentLevel = calculateBatteryLevel(context);
			if (batteryChanged(currentLevel)) {
				SideEffectManager.effect("Battery changed");
				batteryLevel = currentLevel;
				//batteryLevel = 15; //debugging test
				updateViews(context);
			}
		}
	}

	@Override
	public void onDisabled(Context context) {
		super.onDisabled(context);

		SideEffectManager.effect("onDisabled()");

		changeAlarm(context, false);
		context.stopService(new Intent(context, ScreenMonitorService.class));
	}

	private int calculateBatteryLevel(Context context) {
		SideEffectManager.effect("calculateBatteryLevel()");

		Intent batteryIntent = context.getApplicationContext().registerReceiver(null,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		assert batteryIntent != null;
		int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
		int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
		return level * 100 / scale;
	}

	/* Implementing the sound effects

	imageButton.setOnTouchListener(new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event){
			if(event.getAction() == MotionEvent.Action_UP){
				return true;
			}
		}
		return false;
	});
	*/


	private void updateViews(Context context) { //This is the block of code to edit the view
		SideEffectManager.effect("updateViews()");

		//views is needed to make changes to the layout
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		//this is no longer necessary now that we will be working with images
		//views.setTextViewText(R.id.batteryText, batteryLevel + "%");
		int bar = getBar();
		int vatista = getVatista(context);
		int ten = getTen();
		int one = getOne();


		//Battery Bar Placement
		views.setImageViewResource(R.id.bar, bar);


		//Vatista Placement
		views.setImageViewResource(R.id.vatista, vatista);


		//Number Display Placement
		//Hundred Position
		if (getHundred())
			views.setViewVisibility(R.id.hundred, 0); //Oddly, visible is 0 and 100 isn't
		else
			views.setViewVisibility(R.id.hundred, 100);

		//Ten Position
		if (ten != -1) {
			views.setViewVisibility(R.id.ten, 0);
			views.setImageViewResource(R.id.ten, ten);
		} else
			views.setViewVisibility(R.id.ten, 100);

		//One Position
		views.setImageViewResource(R.id.one, one);


		ComponentName componentName = new ComponentName(context, VBWidget.class);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		appWidgetManager.updateAppWidget(componentName, views);
	}


	public boolean isCharging(Context context) {

		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent batteryStatus = context.registerReceiver(null, filter);

		assert batteryStatus != null;
		int charging = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

		return charging == BatteryManager.BATTERY_STATUS_CHARGING;
	}

	private int getVatista(Context context) {

		if (isCharging(context)) //For when charging
			return R.drawable.vatista_ch;

		if (batteryLevel < 100 && batteryLevel > 75) //For less than 100
			return R.drawable.vatista_90;

		if (batteryLevel <= 75 && batteryLevel > 50) //For 75%
			return R.drawable.vatista_75;

		if (batteryLevel <= 50 && batteryLevel > 40) //For 50%
			return R.drawable.vatista_50;

		if (batteryLevel <= 40 && batteryLevel > 25) //For 40%
			return R.drawable.vatista_40;

		if (batteryLevel <= 25) //For 25%
			return R.drawable.vatista_25;


		return R.drawable.vatista_100;

	}

	private boolean getHundred() {
		return batteryLevel == 100;
	}

	private int getTen() {
		if (batteryLevel == 100) //sets to 0
			return R.drawable.n_battery_00;
		else if (batteryLevel > 89) //sets to 9
			return R.drawable.n_battery_90;
		else if (batteryLevel > 79) //sets to 8
			return R.drawable.n_battery_80;
		else if (batteryLevel > 69) //sets to 7
			return R.drawable.n_battery_70;
		else if (batteryLevel > 59) //sets to 6
			return R.drawable.n_battery_60;
		else if (batteryLevel > 49) //sets to 5
			return R.drawable.n_battery_50;
		else if (batteryLevel > 39) //sets to 4
			return R.drawable.n_battery_40;
		else if (batteryLevel > 29) //sets to 3
			return R.drawable.n_battery_30;
		else if (batteryLevel > 19) //sets to 2
			return R.drawable.n_battery_20;
		else if (batteryLevel > 9) //sets to 1
			return R.drawable.n_battery_10;
		else
			return -1; //sets to blank

	}

	private int getOne() {

		int oneSpot = batteryLevel % 10;

		switch (oneSpot) {
			case 9: //sets to 9
				return R.drawable.n_battery_9;
			case 8: //sets to 8
				return R.drawable.n_battery_8;
			case 7: //sets to 7
				return R.drawable.n_battery_7;
			case 6: //sets to 6
				return R.drawable.n_battery_6;
			case 5: //sets to 5
				return R.drawable.n_battery_5;
			case 4: //sets to 4
				return R.drawable.n_battery_4;
			case 3: //sets to 3
				return R.drawable.n_battery_3;
			case 2: //sets to 2
				return R.drawable.n_battery_2;
			case 1: //sets to 1
				return R.drawable.n_battery_1;
			default:
				return R.drawable.n_battery_0; //sets to 0
		}

	}

	private int getBar() {
		
		switch (batteryLevel){
			case 100:
				return R.drawable.col_battery_100;


			//=================================================================================99-90
			case 99:
				return R.drawable.col_battery_99;
			case 98:
				return R.drawable.col_battery_98;
			case 97:
				return R.drawable.col_battery_97;
			case 96:
				return R.drawable.col_battery_96;
			case 95:
				return R.drawable.col_battery_95;
			case 94:
				return R.drawable.col_battery_94;
			case 93:
				return R.drawable.col_battery_93;
			case 92:
				return R.drawable.col_battery_92;
			case 91:
				return R.drawable.col_battery_91;
			case 90:
				return R.drawable.col_battery_90;


			//=================================================================================89-80
			case 89:
				return R.drawable.col_battery_89;
			case 88:
				return R.drawable.col_battery_88;
			case 87:
				return R.drawable.col_battery_87;
			case 86:
				return R.drawable.col_battery_86;
			case 85:
				return R.drawable.col_battery_85;
			case 84:
				return R.drawable.col_battery_84;
			case 83:
				return R.drawable.col_battery_83;
			case 82:
				return R.drawable.col_battery_82;
			case 81:
				return R.drawable.col_battery_81;
			case 80:
				return R.drawable.col_battery_80;


			//=================================================================================79-70
			case 79:
				return R.drawable.col_battery_79;
			case 78:
				return R.drawable.col_battery_78;
			case 77:
				return R.drawable.col_battery_77;
			case 76:
				return R.drawable.col_battery_76;
			case 75:
				return R.drawable.col_battery_75;
			case 74:
				return R.drawable.col_battery_74;
			case 73:
				return R.drawable.col_battery_73;
			case 72:
				return R.drawable.col_battery_72;
			case 71:
				return R.drawable.col_battery_71;
			case 70:
				return R.drawable.col_battery_70;


			//=================================================================================69-60
			case 69:
				return R.drawable.col_battery_69;
			case 68:
				return R.drawable.col_battery_68;
			case 67:
				return R.drawable.col_battery_67;
			case 66:
				return R.drawable.col_battery_66;
			case 65:
				return R.drawable.col_battery_65;
			case 64:
				return R.drawable.col_battery_64;
			case 63:
				return R.drawable.col_battery_63;
			case 62:
				return R.drawable.col_battery_62;
			case 61:
				return R.drawable.col_battery_61;
			case 60:
				return R.drawable.col_battery_60;


			//=================================================================================59-50
			case 59:
				return R.drawable.col_battery_59;
			case 58:
				return R.drawable.col_battery_58;
			case 57:
				return R.drawable.col_battery_57;
			case 56:
				return R.drawable.col_battery_56;
			case 55:
				return R.drawable.col_battery_55;
			case 54:
				return R.drawable.col_battery_54;
			case 53:
				return R.drawable.col_battery_53;
			case 52:
				return R.drawable.col_battery_52;
			case 51:
				return R.drawable.col_battery_51;
			case 50:
				return R.drawable.col_battery_50;


			//=================================================================================49-40
			case 49:
				return R.drawable.col_battery_49;
			case 48:
				return R.drawable.col_battery_48;
			case 47:
				return R.drawable.col_battery_47;
			case 46:
				return R.drawable.col_battery_46;
			case 45:
				return R.drawable.col_battery_45;
			case 44:
				return R.drawable.col_battery_44;
			case 43:
				return R.drawable.col_battery_43;
			case 42:
				return R.drawable.col_battery_42;
			case 41:
				return R.drawable.col_battery_41;
			case 40:
				return R.drawable.col_battery_40;


			//=================================================================================39-30
			case 39:
				return R.drawable.col_battery_39;
			case 38:
				return R.drawable.col_battery_38;
			case 37:
				return R.drawable.col_battery_37;
			case 36:
				return R.drawable.col_battery_36;
			case 35:
				return R.drawable.col_battery_35;
			case 34:
				return R.drawable.col_battery_34;
			case 33:
				return R.drawable.col_battery_33;
			case 32:
				return R.drawable.col_battery_32;
			case 31:
				return R.drawable.col_battery_31;
			case 30:
				return R.drawable.col_battery_30;


			//=================================================================================29-20
			case 29:
				return R.drawable.col_battery_29;
			case 28:
				return R.drawable.col_battery_28;
			case 27:
				return R.drawable.col_battery_27;
			case 26:
				return R.drawable.col_battery_26;
			case 25:
				return R.drawable.col_battery_25;
			case 24:
				return R.drawable.col_battery_24;
			case 23:
				return R.drawable.col_battery_23;
			case 22:
				return R.drawable.col_battery_22;
			case 21:
				return R.drawable.col_battery_21;
			case 20:
				return R.drawable.col_battery_20;


			//=================================================================================19-10
			case 19:
				return R.drawable.col_battery_19;
			case 18:
				return R.drawable.col_battery_18;
			case 17:
				return R.drawable.col_battery_17;
			case 16:
				return R.drawable.col_battery_16;
			case 15:
				return R.drawable.col_battery_15;
			case 14:
				return R.drawable.col_battery_14;
			case 13:
				return R.drawable.col_battery_13;
			case 12:
				return R.drawable.col_battery_12;
			case 11:
				return R.drawable.col_battery_11;
			case 10:
				return R.drawable.col_battery_10;


			//===================================================================================9-1
			case 9:
				return R.drawable.col_battery_9;
			case 8:
				return R.drawable.col_battery_8;
			case 7:
				return R.drawable.col_battery_7;
			case 6:
				return R.drawable.col_battery_6;
			case 5:
				return R.drawable.col_battery_5;
			case 4:
				return R.drawable.col_battery_4;
			case 3:
				return R.drawable.col_battery_3;
			case 2:
				return R.drawable.col_battery_2;
			default:
				return R.drawable.col_battery_1;
		}
	}
	
	
	
	

} //Closing Bracket for the WHOLE CLASS