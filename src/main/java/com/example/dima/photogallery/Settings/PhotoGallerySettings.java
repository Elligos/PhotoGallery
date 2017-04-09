package com.example.dima.photogallery.Settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by Dima on 22.03.2017.
 */

public class PhotoGallerySettings {
    final String PHOTO_GALLERY_SHARED_PREFERENCES =
                                    "com.example.dima.photogallery";
    final String POLLING_ENABLED = "POLLING_ENABLED";
    final String PHOTOS_IN_PAGE = "PHOTOS_IN_PAGE";
    final String PHOTOS_IN_ROW = "PHOTOS_IN_ROW";
    private boolean mPollingEnabled;
    private int mAmountOfPhotosInPage;
    private int mAmountOfPhotosInRow;
    private SharedPreferences mSettings;
    static private PhotoGallerySettings mPhotoGallerySettings;
    private static final String TAG = "PhotoGallerySettings";

    static public PhotoGallerySettings getPhotoGallerySettings(Context context){
        Log.i(TAG, "PhotoGallerySettings object requested.");
        if (mPhotoGallerySettings == null) {
            synchronized (PhotoGallerySettings.class) {
                mPhotoGallerySettings = new PhotoGallerySettings(context);
                Log.i(TAG, "PhotoGallerySettings object created.");
            }
        }
        Log.i(TAG, "PhotoGallerySettings object:    "+
                            "mPollingEnabled="+mPhotoGallerySettings.mPollingEnabled+
                            "   mAmountOfPhotosInPage"+mPhotoGallerySettings.mAmountOfPhotosInPage+
                            "   mAmountOfPhotosInRow="+ mPhotoGallerySettings.mAmountOfPhotosInRow);
        return  mPhotoGallerySettings;
    }

    public PhotoGallerySettings(Context context) {
        mSettings = context.getSharedPreferences(PHOTO_GALLERY_SHARED_PREFERENCES, 0);
        mPollingEnabled = mSettings.getBoolean(POLLING_ENABLED, true);
        mAmountOfPhotosInPage = mSettings.getInt(PHOTOS_IN_PAGE, 100);
        mAmountOfPhotosInRow = mSettings.getInt(PHOTOS_IN_ROW, 3);
    }

    public void saveAmountOfPhotosInPage(int amountOfPhotosInPage){
        mAmountOfPhotosInPage = amountOfPhotosInPage;
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt(PHOTOS_IN_PAGE, mAmountOfPhotosInPage);
    }

    public void saveAmountOfPhotosInRow(int amountOfPhotosInRow){
        mAmountOfPhotosInRow= amountOfPhotosInRow;
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt(PHOTOS_IN_ROW, mAmountOfPhotosInRow);
    }
    public void savePollingEnabled(boolean pollingEnabled){
        mPollingEnabled = pollingEnabled;
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(POLLING_ENABLED, mPollingEnabled);
    }

    public int getAmountOfPhotosInPage() {
        return mAmountOfPhotosInPage;
    }

    public int getAmountOfPhotosInRow() {
        return mAmountOfPhotosInRow;
    }

    public boolean isPollingEnabled() {
        return mPollingEnabled;
    }
}
