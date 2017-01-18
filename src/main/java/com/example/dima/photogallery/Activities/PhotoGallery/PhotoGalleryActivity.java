package com.example.dima.photogallery.Activities.PhotoGallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.example.dima.photogallery.Activities.PagerViewActivity;
import com.example.dima.photogallery.Activities.SingleFragmentActivity;
import com.example.dima.photogallery.Services.QueryPreferences;
import com.example.dima.photogallery.Web.FlickrFetchr;
import com.example.dima.photogallery.Web.FlickrSearchResult;
import com.example.dima.photogallery.Web.ThumbnailDownloader;

public class PhotoGalleryActivity extends PagerViewActivity /*SingleFragmentActivity*/
        implements PhotoGalleryFragment.Callbacks
{
    private static final String TAG = "PhotoGalleryActivity";
    private int mCurrentPage = 1;//выбранная страница
    private int mPagesAmount = 1;//общее количество страниц
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;//загрузчик фотографий с сайта Flickr

    public static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        String query = QueryPreferences.getStoredQuery(this);//получить последний поисковый
        AsyncTask task = new PhotoGalleryActivity.FetchItemTask(query).execute();//обновить
//        while(task.getStatus() != AsyncTask.Status.FINISHED);
        startPhotoDownloaderThread();
        super.onCreate(savedInstanceState);
    }

    //старт потока, где будет идти загрузка фотографий
    protected void startPhotoDownloaderThread()
    {
        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>(){
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder holder, Bitmap thumbnail) {
                        Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                        holder.bindDrawable(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread " + mThumbnailDownloader.getId() + " created.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread " + mThumbnailDownloader.getId() + "destroyed");
    }

    //получить фрагмент, соответствующий выбранной позиции
    @Override
    protected Fragment getPageFragment(int position) {
        int pageToSet = position+1;
        setCurrentPhotoPage(pageToSet);
        //return PhotoGalleryFragment.newInstance(pageToSet);
        return PhotoGalleryFragment.newInstance(pageToSet, mThumbnailDownloader);
    }

    //вернуть общее количество страниц
    @Override
    protected int getPagesAmount() {
        //return mPagesAmount;
        return 10;
    }

    @Override
    public void reloadPages() {
        reloadAdapter();
    }

    //задать текущую страницу
    @Override
    public void setCurrentPhotoPage(int page) {
        mCurrentPage = page;
    }

    //задать общее количество страниц
    @Override
    public void setPhotoPagesAmount(int amount) {
        if(mPagesAmount>10){
            mPagesAmount = 10;
        }
        else {
            mPagesAmount = amount;
        }

    }

    //обработка события выбора новой страницы
    @Override
    protected void onPageSelectedAction(int position) {
        mThumbnailDownloader.clearQueue();
        Log.i(TAG, "Background thread " + mThumbnailDownloader.getId() + " queue cleared");
    }

    //запустить задачу по загрузке и обновлению моделей
    //+ Если поисковый запрос задан, будут загружены модели, соответствующие запросу.
    //+ Если поисковый запрос не задан, будут загружены модели, соответствующие последним добавленным
    //+ фотографиям.
    private class FetchItemTask extends AsyncTask<Void, Void, FlickrSearchResult> {
        private static final String TAG = "PhotoGalleryFragment";
        private String mQuery;

        public FetchItemTask(String query){
            mQuery = query;
        }

        @Override
        protected FlickrSearchResult doInBackground(Void... params) {
            if(mQuery == null){
                return new FlickrFetchr().fetchRecentPhotosFromPage(mCurrentPage);
            } else{
                return new FlickrFetchr().searchPhotosInPage(mQuery, mCurrentPage);
            }
        }

        @Override
        protected void onPostExecute(FlickrSearchResult result) {
            mPagesAmount = result.getPagesAmount();
            reloadAdapter();
        }
    }
}
