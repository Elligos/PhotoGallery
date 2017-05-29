package com.example.dima.photogallery.Services;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.dima.photogallery.Activities.PhotoGallery.GalleryItem;
import com.example.dima.photogallery.Activities.PhotoGallery.PhotoGalleryActivity;
import com.example.dima.photogallery.Web.FlickrFetchr;
import com.example.dima.photogallery.R;
import com.example.dima.photogallery.Web.FlickrSearchResult;

import java.util.List;

/**
 * Created by Dima on 12.11.2016.
 */

public class PollService extends IntentService{
    private static final String TAG = "PollService";
    private static final int POLL_INTERVAL_mS = 1000 * 60;//интервал отправления сигналов
    public static final String ACTION_SHOW_NOTIFICATION =
                                                "com.example.dima.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE =
                                         "com.example.dima.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final int RECENT_PAGE = 1;

    public static Intent newIntent(Context context) {
        return new Intent(context, PollService.class);
    }

    //настроить автоматическую отправку сигналов
    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                                            SystemClock.elapsedRealtime(),
                                            POLL_INTERVAL_mS,
                                            pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }
        QueryPreferences.setAlarmOn(context, isOn);
    }

    public PollService() {
        super(TAG);
    }

    //обработчик сигналов
    @Override
    protected void onHandleIntent(Intent intent) {
        //выйти, если сеть недоступна
        if(!isNetworkAvailableAndConnected()){
            return;
        }
        Log.i(TAG, "Received an intent: " + intent);
        String query = QueryPreferences.getStoredQuery(this);//получить последний поисковый запрос
        String lastResultId = QueryPreferences.getLastResultId(this);//получить идентификатор результата
                                                                //+ последнего потскового запроса
        List<GalleryItem> items;//модели
        FlickrSearchResult result;//

        if (query == null) {
            result = new FlickrFetchr().fetchRecentPhotosFromPage(RECENT_PAGE);
        }
        else{
            result = new FlickrFetchr().searchPhotosInPage(query, RECENT_PAGE);
        }
        if(result == null){
            return;
        }
        items = result.getGalleryItems();
        if (items.size() == 0) {
            return;
        }
        //если идентификатор модели самой недавней фотографии отличается от того, который был в
        //+ предыдущем поисковом запросе, запустить процедуру уведомления пользователя
        String  resultId = items.get(0).getId();
        if(resultId.equals(lastResultId)){
            Log.i(TAG, "Got an old result: " + resultId);
        } else {
            Log.i(TAG, "Got a new result: " + resultId);
            notifyUser();
        }
        QueryPreferences.setLastResultId(this, resultId);
    }

    //запустить процедуру уведомления пользователя
    private void notifyUser(){
        Resources resources = getResources();
        Intent i = PhotoGalleryActivity.newIntent(this);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

        //сформировать уведомление
        Notification notification = new NotificationCompat.Builder(this)
                .setTicker(resources.getString(R.string.new_pictures_title))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(resources.getString(R.string.new_pictures_title))
                .setContentText(resources.getString(R.string.new_pictures_text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        showBackgroundNotification(0, notification);//отправить запрос на уведомление
    }

    //отправить запрос на уведомление
    private void showBackgroundNotification(int requestCode,
                                            Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);
        //отправить упорядоченный широковещательный интент
        sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
    }

    //проверить, доступна ли сеть
    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = cm.getActiveNetworkInfo().isConnected();

        return  (isNetworkAvailable && isNetworkConnected);
    }

    //проверить, включен ли сигнал
    public static boolean isServiceAlarmOn(Context context){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

}
