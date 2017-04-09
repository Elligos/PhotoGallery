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
import android.util.Log;

import com.example.dima.photogallery.Activities.ViewPagerActivity;
import com.example.dima.photogallery.Services.QueryPreferences;
import com.example.dima.photogallery.Web.FlickrFetchr;
import com.example.dima.photogallery.Web.FlickrSearchResult;
import com.example.dima.photogallery.Web.ThumbnailDownloader;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryActivity extends ViewPagerActivity /*SingleFragmentActivity*/
        implements PhotoGalleryFragment.Callbacks
{
    private static final String LAST_JSON_STRING = "LAST_JSON_STRING";
    private static final String CURRENT_PAGE = "CURRENT_PAGE";
    private static final String PAGES_AMOUNT = "PAGES_AMOUNT";
    private static final String FRAGMENT_KEYS = "FRAGMENT_KEYS";
    private static final String TAG = "PhotoGallery";
    private static final String UNIQUE_POSTFIX = "com.example.dima.photogallery";
    private int mCurrentPage = 1;//выбранная страница
    private int mPagesAmount = 1;//общее количество страниц
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;//загрузчик фотографий с сайта Flickr
    private List<GalleryItem> mItems;//список моделей, описывающих фотографии
    private List<FlickrSearchResult> mSearchResults;

    public static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        startPhotoDownloaderThread();

        if (savedInstanceState == null) {
            mCurrentPage = 1;
            String query = QueryPreferences.getStoredQuery(this);//получить последний поисковый запрос
            new FetchItemTask(query).execute();
            super.onCreate(savedInstanceState);
        }
        else {
            super.onCreate(savedInstanceState);
            restoreApplicationState(savedInstanceState);
        }
        Log.i(TAG, "PhotoGalleryActivity created");
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

    protected void restoreApplicationState(@Nullable Bundle savedInstanceState)
    {
        ArrayList<String> storedFragmentsKeys;

        mCurrentPage = savedInstanceState.getInt(CURRENT_PAGE, 1);
        mPagesAmount = savedInstanceState.getInt(PAGES_AMOUNT, 1);
        storedFragmentsKeys = savedInstanceState.getStringArrayList(FRAGMENT_KEYS);
        String query = QueryPreferences.getStoredQuery(this);//получить последний поисковый запрос
        mSearchResults = new ArrayList<>();

        if (storedFragmentsKeys != null) {
            PhotoGalleryFragment storedFragment;
            int fragmentPage;
            for (String fragmentKey : storedFragmentsKeys) {
                try {
                    storedFragment = (PhotoGalleryFragment) getSupportFragmentManager().getFragment(savedInstanceState, fragmentKey);
                }
                catch(Exception e){
                    storedFragment = null;
                    Log.i(TAG, "Exception on restoring fragment (key " + fragmentKey + " )");
                }
                if(storedFragment != null){
                    fragmentPage = savedInstanceState.getShort(fragmentKey+ UNIQUE_POSTFIX, (short) 1);
                    storedFragment.setCurrentPage(fragmentPage);
                    storedFragment.setThumbnailDownloader(mThumbnailDownloader);
                    storedFragment.setQuery(query);
                    Log.i(TAG, "State of page " + fragmentPage + " ( " + storedFragment.getTag() + " ) " + " restored.");
                }
            }
        }
        loadAdapter();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread " + mThumbnailDownloader.getId() + "destroyed");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String jsonString = FlickrFetchr.getLastSearchedJsonString();
        outState.putString(LAST_JSON_STRING, jsonString);
        outState.putInt(CURRENT_PAGE, mCurrentPage);
        outState.putInt(PAGES_AMOUNT, mPagesAmount);

        ArrayList<String> fragmentKeys = new ArrayList<>();
        String fragmentKey;
        ArrayList<Fragment> fragmentsToSave = getActiveFragments();
        if(fragmentsToSave == null){
            Log.i(TAG, "No fragments to save in Bundle.");
            throw new RuntimeException();
        }
        else{
            Log.i(TAG, fragmentsToSave.size() + " fragments to save in Bundle.");
        }
        for (Fragment fragment : fragmentsToSave) {
            if(fragment != null) {
                fragmentKey = "PAGE" + ((PhotoGalleryFragment) fragment).getCurrentPage();
                fragmentKeys.add(fragmentKey);
                getSupportFragmentManager().putFragment(outState, fragmentKey, fragment);
                outState.putShort(fragmentKey + UNIQUE_POSTFIX, (short) ((PhotoGalleryFragment) fragment).getCurrentPage());
                Log.i(TAG, fragmentKey + " saved in Bundle.");
            }
        }
        outState.putStringArrayList(FRAGMENT_KEYS, fragmentKeys);
    }

    //получить фрагмент, соответствующий выбранной позиции
    @Override
    protected Fragment getPageFragment(int position) {
        PhotoGalleryFragment fragment;
        int pageToSet = position+1;

        String query = QueryPreferences.getStoredQuery(this);//получить последний поисковый
        fragment = PhotoGalleryFragment.newInstance(mThumbnailDownloader, pageToSet, query );
        fragment.updateItems();
        return fragment;
    }

    //вернуть общее количество страниц
    @Override
    protected int getPagesAmount() {
        return mPagesAmount;
    }

    @Override
    public void reloadPages() {
        mCurrentPage = 1;
        String query = QueryPreferences.getStoredQuery(this);//получить последний поисковый
//        new PhotoGalleryActivity.FetchItemsTask(query).execute();//обновить
        new FetchItemTask(query).execute();//обновить
    }

    @Override
    public void onSaveFragment(Bundle outState, Fragment fragment) {
        ArrayList<String> fragmentKeys;
        String fragmentKey;



        if(outState.get(FRAGMENT_KEYS) == null) {
            fragmentKeys = new ArrayList<>();
        }
        else{
            fragmentKeys = outState.getStringArrayList(FRAGMENT_KEYS);
        }
        fragmentKey = "PAGE"+((PhotoGalleryFragment)fragment).getCurrentPage();
        fragmentKeys.add(fragmentKey);
        outState.putStringArrayList(FRAGMENT_KEYS, fragmentKeys);
        outState.putInt(fragmentKey, ((PhotoGalleryFragment)fragment).getCurrentPage());
        getSupportFragmentManager().putFragment(outState, fragmentKey, fragment);
    }

    //задать текущую страницу
    @Override
    public void setCurrentPhotoPage(int page) {
        mCurrentPage = page;
    }

    //задать общее количество страниц
    @Override
    public void setPhotoPagesAmount(int amount) {
        mPagesAmount = amount;

    }

    @Override
    public List<GalleryItem> getItems() {
        return mSearchResults.get(mCurrentPage-1).getGalleryItems();
    }

    @Override
    public List<GalleryItem> getItemsForPage(int page) {
        return mSearchResults.get(page-1).getGalleryItems();
    }

    @Override
    public ThumbnailDownloader<PhotoHolder> getDownloader() {
        return mThumbnailDownloader;
    }

    @Override
    public int getCurrentPage() {
        return mCurrentPage;
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
            mItems = result.getGalleryItems();
            loadAdapter();
        }
    }

    //запустить задачу по загрузке и обновлению моделей
    //+ Если поисковый запрос задан, будут загружены модели, соответствующие запросу.
    //+ Если поисковый запрос не задан, будут загружены модели, соответствующие последним добавленным
    //+ фотографиям.
    private class FetchItemsTask extends AsyncTask<Void, Void, List<FlickrSearchResult>> {
        private static final String TAG = "PhotoGalleryFragment";
        private String mQuery;

        public FetchItemsTask(String query){
            mQuery = query;
        }

        @Override
        protected List<FlickrSearchResult> doInBackground(Void... params) {
            if(mQuery == null){
                return new FlickrFetchr().fetchRecentPhotos();
            } else{
                return new FlickrFetchr().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<FlickrSearchResult> result) {
            mSearchResults = result;
            if(mSearchResults == null){
                return;
            }
            if((mCurrentPage == 0) || (mCurrentPage > mSearchResults.size())){
                mCurrentPage = 1;//TODO: throw exception? or remain fail-safe style?
            }
            mPagesAmount = mSearchResults.size();
            loadAdapter();
        }
    }
}
