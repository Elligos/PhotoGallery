package com.example.dima.photogallery.Receivers;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.example.dima.photogallery.Services.PollService;

/**
 * Created by Dima on 17.11.2016.
 */
//получатель уведомления о том, что на фотохостинг были добавлены новые фотографии
public class NotificationReceiver extends BroadcastReceiver{
    private static final String TAG = "NotificationReceiver";

    //если уведомление не было отменено предыдущим приемником, отправить оповещение пользователю
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "received result: " + getResultCode());
        if (getResultCode() != Activity.RESULT_OK) {
            return;
        }
        int requestCode = intent.getIntExtra(PollService.REQUEST_CODE, 0);
        Notification notification = intent.getParcelableExtra(PollService.NOTIFICATION);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(requestCode, notification);
    }
}
