This is modified version of this [CMP Studio library](https://github.com/CMP-Studio/react-native-android-alarms). Tested on RN 0.57.8.

This React Native library will allow you to schedule and show alarms on Android (tested on >= API 21). To see a working example of this module (original one), see [Dawn Chorus](https://github.com/CMP-Studio/DawnChorus). The code for this module was modified from [Christoph Michel's App Launcher](https://github.com/MrToph/react-native-app-launcher).
Here is another [example](https://github.com/vasyl91/muse-sleep-tracker-and-arduino-light-therapy) of implemented module.

## Features
* Schedules alarms using AlarmManager
* Alarm reciever that will launch application at alarm time and run android alarm ringtone, even if the application is closed
* Minimize function that simulates home button and will programatically minimize your app
* Reschedules alarms after phone boots back up
* Notifies users of alarms they may have missed when their phone was off

## Installation
* Run 
    ```
    npm install --save git+https://github.com/vasyl91/react-native-android-alarms.git
    ```
    or 
    ```
    yarn add https://github.com/vasyl91/react-native-android-alarms.git
    ```

* Add the following to `android/settings.gradle`:
    ```
    include ':react-native-android-alarms'
    project(':react-native-android-alarms').projectDir = new File(settingsDir, '../node_modules/react-native-android-alarms/android')
    ```

* Add the following to `android/app/build.gradle`:
    ```xml
    ...

    dependencies {
        ...
        compile project(':react-native-android-alarms') 
    }
    ```
* Add the following to `android/app/src/main/AndroidManifest.xml`:
    ```xml
    <manifest 
        ...
    >   
        ...

        <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>

        ... 

        <application
            ...
        >
            <receiver android:name="com.dawnchorus.alarms.AlarmReceiver" />
            <receiver android:name="com.dawnchorus.alarms.RebootReceiver">
                <intent-filter>
                    <action android:name="android.intent.action.BOOT_COMPLETED" />
                    <action android:name="android.intent.action.QUICKBOOT_POWERON" />

                    <category android:name="android.intent.category.DEFAULT" />
                </intent-filter>
            </receiver>
            <service
              android:name="com.dawnchorus.alarms.RebootService"
              android:exported="false"/>

          ...

        </application>
    </manifest>
    ```
* Add the following to `android/app/src/main/java/**/MainApplication.java`:
    ```java
    import com.dawnchorus.alarms.AlarmPackage;  // add this for react-native-android-alarms

    public class MainApplication extends Application implements ReactApplication {

        @Override
        protected List<ReactPackage> getPackages() {
            return Arrays.<ReactPackage>asList(
                new AlarmPackage()     // add this for react-native-android-alarms
            );
        }
    }
    ```
    
    
* In `android/app/src/main/java/**/MainActivity.java`, 1) Add flags to Window that allow it to open over lockscreen and 2) Extend ReactActivityDelegate to pass data from the native module to your react native code as initial props.

  Copy all necessary stuff from `MainActivity.java` example:
    
    ```java
	package com.your_app_name;

	import android.app.Activity;
	import android.content.Intent;
	import android.os.Bundle;
	import android.view.Window;
	import android.view.WindowManager;

	import com.facebook.react.bridge.ReactApplicationContext;
	import com.facebook.react.bridge.ReactContext;
	import com.facebook.react.ReactActivity;
	import com.facebook.react.ReactActivityDelegate;

	import javax.annotation.Nullable;

	public class MainActivity extends ReactActivity {

	    @Override
	    protected String getMainComponentName() {
		return "YourAppName";
	    }

	    @Override
	    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Add
		final Window win = getWindow();
		win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
			WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
			WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
			WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
			WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
	    }

	    // Add
	    public class AlarmActivityDelegate extends ReactActivityDelegate {
		private final String ALARM_ID = "alarmID";
		private final String MISSED_ALARMS = "missedAlarms";
		private Bundle mInitialProps = null;
		private final @Nullable Activity mActivity; 

		public AlarmActivityDelegate(Activity activity, String mainComponentName) {
		    super(activity, mainComponentName);
		    this.mActivity = activity;
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {
		    mInitialProps = new Bundle();
		    final Bundle bundle = mActivity.getIntent().getExtras();
		    if (bundle != null && bundle.containsKey(ALARM_ID)) {
			mInitialProps.putString(ALARM_ID, bundle.getString(ALARM_ID));
		    }
		    if (bundle != null && bundle.containsKey(MISSED_ALARMS)) {
			mInitialProps.putString(MISSED_ALARMS, bundle.getString(MISSED_ALARMS));
		    }   
		    if (bundle != null && bundle.containsKey("launchAlarm")) {
			if (bundle.getString("launchAlarm").equals("ringtoneOn")) {
			    mInitialProps.putBoolean("alarmOn", true);
			}
		    }
		    super.onCreate(savedInstanceState);
		}

		@Override
		protected Bundle getLaunchOptions() {
		    return mInitialProps;
		}
	    };

	    @Override
	    protected ReactActivityDelegate createReactActivityDelegate() {
		return new AlarmActivityDelegate(this, getMainComponentName());
	    }
	}
    ```
    
 ## Usage
 
 ### Scheduling Alarms
 ```javascript
 import AndroidAlarms from 'react-native-android-alarms';
 import moment from 'moment';
 import { AsyncStorage } from "react-native";
 
 alarmID = ... // String or number, whatever you want. Necessary to identify your alarm. You can save it with AsyncStorage for further use (e.g. to load it and cancel an alarm in another scene or in case the app was restarted)
 alarmTime = Number(moment()) + 5000; // In this case alarm will be triggered in 5 seconds. You can also edit moment() itself to your correct time
 
 // Set the alarm and return the time 
 AndroidAlarms.setAlarm(alarmID, alarmTime.valueOf(), false);
 
 // Optionally save alarmID to AsyncStorage
 AsyncStorage.setItem('alarmID', JSON.stringify(alarmID));
 ```
 
 ### Clearing Alarms
 ```javascript
 AndroidAlarms.clearAlarm(alarmID); // Clears mounted alarmID
 ``` 
 or
 ```javascript 
 // If you saved alarmID to AsyncStorage simply load it and clear an alarm (useful in case the app had been closed)
 AsyncStorage.getItem('alarmID').then((value) => {
    AndroidAlarms.clearAlarm(JSON.parse(value)); 
 });
 ```

 ### Activating Alarm
 ```javascript
 AndroidAlarms.startAlarm(); // Turns ringtone on
 ``` 
 
 ### Dismissing Alarm
 ```javascript
 AndroidAlarms.stopAlarm(); // Turns ringtone off
 ```

 ### Minimizing app
 ```javascript
 /* 
 Imitates home button and programatically minimizes app. 
 Might be usefull because if app is in foreground FLAG_KEEP_SCREEN_ON prevents android from turning screen off. 
 If you assotiate this method with e.g. dismiss/snooze button you will minimize the app while tapping on it and android will turn the screen off shortly after. 
 */
 AndroidAlarms.minimizeApp(); 
 ```
 
 ### Reading data in React Native app
 
If you extended your ReactActivityDelegate as shown above, you can grab the initial data from this module by adding to your main app component (usually index.android.js):
 
 ```javascript
 import React, { Component, PropTypes } from "react";
 
 class YourAppName extends Component {
 
    static propTypes = {
       alarmID: PropTypes.string,
       missedAlarms: PropTypes.string,
       alarmOn: PropTypes.boolean
    }
 }
 ```
 or, if you use React version 15.5.0 or higher (check in package.json):
 ```javascript
 import React, { Component } from "react";
 import PropTypes from 'prop-types';
 
 class YourAppName extends Component {
 
    static propTypes = {
       alarmID: PropTypes.string,
       missedAlarms: PropTypes.string,
       alarmOn: PropTypes.bool
    }
 }
 ```
 And access those props elsewhere in the component with ```this.props.alarmID```, ```this.props.missedAlarms``` and ```this.props.alarmOn```
 
 ### Receiving An Alarm
 
If the app was launched by an alarm, the ```alarmID``` will hold the ID of the alarm that went off and ```alarmOn``` will return ```true```. If the app was not launched from an alarm, ```alarmID = undefined``` and ```alarmOn = undefined```.

 ```this.props.alarmOn``` can be used to run initial alarm scene with Dismiss and Snooze buttons.
 
 Add to your main app component:
 ```javascript
 componentDidMount() {
     if (this.props.alarmOn === true) {
         // your code
     }
 }
 ```
 
NOTE: In Android 8.0 and above, clicking the alarm icon in the Android notification drawer will launch the app and include the alarmID as an initial prop. To avoid this setting off the alarm, double check that it is the alarm time before sounding your alarm.
 
 ### Handling Missed Alarms
 
 If the user missed an alarm becuase their phone was off, when they turn their phone on, this module will present them with a notification telling them that they missed an alarm.
 Missed alarms is delivered as a String. For example, if you missed alarms with ids 3, 5, and 7 ```missedAlarms = "3,5,7,"```. If there are no missed alarms, ```missedAlarms = undefined```.
  

