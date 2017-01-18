package com.example.dima.photogallery.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.dima.photogallery.Services.PollService;
import com.example.dima.photogallery.Services.QueryPreferences;

/**
 * Created by Dima on 17.11.2016.
 */
//широковещательный приемник, запускающий опрос хостинга на наличие новых фотографий, если включена
// эта опция
public class StartupReceiver  extends BroadcastReceiver{
    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: " + intent.getAction());
        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);
    }
}
