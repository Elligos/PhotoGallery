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
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.dima.photogallery.Activities.ViewPagerActivity;
import com.example.dima.photogallery.R;
import com.example.dima.photogallery.Services.PollService;
import com.example.dima.photogallery.Services.QueryPreferences;
import com.example.dima.photogallery.Settings.PhotoGallerySettings;
import com.example.dima.photogallery.Web.FlickrFetchr;
import com.example.dima.photogallery.Web.FlickrSearchResult;
import com.example.dima.photogallery.Web.ThumbnailDownloader;

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
    private PhotoGallerySettings mSettings;

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;

    public static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        startPhotoDownloaderThread();
        mSettings = PhotoGallerySettings.getPhotoGallerySettings(this.getApplicationContext());
        PollService.setServiceAlarm(this, mSettings.isPollingEnabled());//запустить службу на проверку наличия
        //+ новых фотографий в хостинге Flickr
        if (savedInstanceState == null) {
            mCurrentPage = 1;
            String query = QueryPreferences.getStoredQuery(this);//получить последний поисковый запрос
            new FetchItemTask(query).execute();
            super.onCreate(savedInstanceState);
            Log.i(TAG, "PhotoGalleryActivity created.");
        }
        else {
            super.onCreate(savedInstanceState);
            restoreApplicationState(savedInstanceState);
            Log.i(TAG, "PhotoGalleryActivity restored.");
        }
        setupDrawer();
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

    protected void setupDrawer()
    {
        String [] titles = {"Search", "Cancel"};
        mDrawerList = (ListView)findViewById(R.id.drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(getSupportActionBar().getThemedContext(),
                android.R.layout.simple_list_item_activated_1/*R.layout.drawer_list_item*/, titles));
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,/*toolbar,*/
                R.string.open_drawer, R.string.close_drawer) {
            // Вызывается при переходе выдвижной панели в полностью закрытое состояние.
            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);//Код, выполняемый при закрытии выдвижной панели
            }
            //Вызывается при переходе выдвижной панели в полностью открытое состояние.
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);//Код, выполняемый при открытии выдвижной панели
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    //восстановить состояние активности
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
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread " + mThumbnailDownloader.getId() + " destroyed");
    }

    @Override
    protected void onPause() {
        mThumbnailDownloader.clearQueue();
        Log.i(TAG, "Photo gallery activity paused");
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    //сохранить состояние активности
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
        fragment = PhotoGalleryFragment.newInstance(mThumbnailDownloader, mSettings, pageToSet, query);
        return fragment;
    }

    //вернуть общее количество страниц
    @Override
    protected int getPagesAmount() {
        return mPagesAmount;
    }

    //---------------------------------CALLBACKS--------------------------------------------------//
    @Override
    public void reloadPages() {
        mCurrentPage = 1;
        String query = QueryPreferences.getStoredQuery(this);//получить последний поисковый
        new FetchItemTask(query).execute();//обновить
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
}
