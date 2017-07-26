package com.example.dima.photogallery.Activities.PhotoGallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
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
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

//import rx.Observable;
//import rx.Subscriber;
//import rx.schedulers.Schedulers;

public class PhotoGalleryActivity extends ViewPagerActivity /*SingleFragmentActivity*/
        implements PhotoGalleryFragment.Callbacks
{
    private static final String LAST_JSON_STRING = "LAST_JSON_STRING";
    private static final String CURRENT_PAGE = "CURRENT_PAGE";
    private static final String PAGES_AMOUNT = "PAGES_AMOUNT";
    private static final String RECREATE_APPLICATION = "RECREATE_APPLICATION";
    private static final String FRAGMENT_KEYS = "FRAGMENT_KEYS";
    private static final String TAG = "PhotoGallery";
    private static final String UNIQUE_POSTFIX = "com.example.dima.photogallery";
    private static final String NETWORK_REQUEST_ERRROR_MESSAGE =
                                                "Please, check your network connection and retry.";
    private int mCurrentPage = 1;//выбранная страница
    private int mPagesAmount = 1;//общее количество страниц
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;//загрузчик фотографий с сайта Flickr
    private List<GalleryItem> mItems;//список моделей, описывающих фотографии
    private List<FlickrSearchResult> mSearchResults;
    private PhotoGallerySettings mSettings;
    private boolean mRecreateApplication = false;
    AlertDialog mAlert;

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    ActionBarDrawerToggle mDrawerToggle;

    private RefWatcher mRefWatcher;

    public static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }


    //---------------------------------------LIFECYCLE--------------------------------------------//
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        startPhotoDownloaderThread();
        mRecreateApplication = false;
        mSettings = PhotoGallerySettings.getPhotoGallerySettings(this.getApplicationContext());
        PollService.setServiceAlarm(this, mSettings.isPollingEnabled());//запустить службу  проверки
                                            //+ наличия новых фотографий в хостинге Flickr
        if (savedInstanceState == null) {
            mCurrentPage = 1;
            myObservable
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe( result -> showPhotoPages(result),
                                v -> handleNetworkRequestError());
            super.onCreate(savedInstanceState);
            Log.i(TAG, "PhotoGalleryActivity created.");
        }
        else {
            super.onCreate(savedInstanceState);
            restoreApplicationState(savedInstanceState);
            Log.i(TAG, "PhotoGalleryActivity restored.");
        }
//        setupDrawer();
        mRefWatcher = LeakCanary.install(this.getApplication());
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
        outState.putBoolean(RECREATE_APPLICATION, mRecreateApplication);
        if(mRecreateApplication) return;
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
                outState.putShort(fragmentKey + UNIQUE_POSTFIX,
                                  (short) ((PhotoGalleryFragment) fragment).getCurrentPage());
                Log.i(TAG, fragmentKey + " saved in Bundle.");
            }
        }
        outState.putStringArrayList(FRAGMENT_KEYS, fragmentKeys);
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
                    storedFragment = (PhotoGalleryFragment) getSupportFragmentManager().
                                                    getFragment(savedInstanceState, fragmentKey);
                }
                catch(Exception e){
                    storedFragment = null;
                    Log.i(TAG, "Exception on restoring fragment (key " + fragmentKey + " )");
                }
                if(storedFragment != null){
                    fragmentPage = savedInstanceState.
                                    getShort(fragmentKey+ UNIQUE_POSTFIX, (short) 1);
                    storedFragment.setCurrentPage(fragmentPage);
                    storedFragment.setThumbnailDownloader(mThumbnailDownloader);
                    storedFragment.setQuery(query);
                    Log.i(TAG, "State of page " + fragmentPage +
                               " ( " + storedFragment.getTag() + " ) " + " restored.");
                }
            }
        }
        loadAdapter();
    }

    //настроить выдвижную панель
    protected void setupDrawer()
    {
        String [] titles = {"Search", "Cancel"};
        mDrawerList = (ListView)findViewById(R.id.drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(getSupportActionBar().getThemedContext(),
                android.R.layout.simple_list_item_activated_1, titles));
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this,
                                                  mDrawerLayout,
                                                  R.string.open_drawer,
                                                  R.string.close_drawer) {
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

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (mDrawerToggle.onOptionsItemSelected(item)) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }


    //-----------------------------------ViewPagerActivity----------------------------------------//

    //получить фрагмент, соответствующий выбранной позиции
    @Override
    protected Fragment getPageFragment(int position) {
        Log.i(TAG, "getPageFragment("+position+") called.");
        PhotoGalleryFragment fragment;
        int pageToSet = position+1;

        String query = QueryPreferences.getStoredQuery(this);//получить последний поисковый
        fragment = PhotoGalleryFragment.newInstance(mThumbnailDownloader,
                                                    mSettings,
                                                    pageToSet,
                                                    query);
        return fragment;
    }

    //вернуть общее количество страниц
    @Override
    protected int getPagesAmount() {
        return mPagesAmount==0 ? 1 : mPagesAmount;
    }

    //обработка события выбора новой страницы
    @Override
    protected void onPageSelectedAction(int position) {}

    //--------------------------------------CALLBACKS---------------------------------------------//

    @Override
    public void reloadPages() {
        mCurrentPage = 1;
        myObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( result -> showPhotoPages(result),
                        v -> handleNetworkRequestError());
    }


    //----------------------------------THREADS AND ASYNC-----------------------------------------//

    //старт потока, где будет идти загрузка фотографий
    protected void startPhotoDownloaderThread()
    {
        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder,
                                                      Bitmap bitmap) {
                        Drawable drawable = new BitmapDrawable(getResources(),
                                bitmap);
                        photoHolder.bindDrawable(drawable);
                    }

                    @Override
                    public void onThumbnailDownloadError(PhotoHolder target) {
                        handleNetworkRequestError();
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread " + mThumbnailDownloader.getId() + " created.");
    }

    Observable<FlickrSearchResult> myObservable = Observable.create(subscriber -> {
                FlickrSearchResult result = null;
                String query = QueryPreferences.getStoredQuery(this);
                if(query == null){
                    result = new FlickrFetchr().fetchRecentPhotosFromPage(mCurrentPage);
                } else{
                    result = new FlickrFetchr().searchPhotosInPage(query, mCurrentPage);
                }
                subscriber.onNext(result);
                Log.i(TAG, "subscriber.onNext(result) called in Thread:" +
                        Thread.currentThread().getId() +
                        "\nfor query: \"" + query + "\"");
    });

    void showPhotoPages(FlickrSearchResult result){
        mPagesAmount = result.getPagesAmount();
        mItems = result.getGalleryItems();
        loadAdapter();
        Log.i(TAG, "showPhotoPages(result) called in Thread:" + Thread.currentThread().getId());
    }

    //---------------------------------- ERRORS HANDLING -----------------------------------------//
    @Override
    public void handleError(int errorId) {
        switch(errorId){
            case PhotoGalleryFragment.NETWORK_ERROR_ID:
                handleNetworkRequestError();
                break;
            default:
                break;
        }
    }

    static int sDbguDialogCalledCounter = 0;
    void handleNetworkRequestError(){
        AlertDialog alert = getCreatedAlert();
        alert.show();
    }



    private AlertDialog getCreatedAlert(){
        if(mAlert == null){
            sDbguDialogCalledCounter++;
            AlertDialog.Builder builder = new AlertDialog.Builder(PhotoGalleryActivity.this);
            mRecreateApplication = true;
            builder.setTitle("NETWORK FAIL!")
                    .setMessage(NETWORK_REQUEST_ERRROR_MESSAGE +
                                " (activity) " +
                                sDbguDialogCalledCounter)
                    .setCancelable(false)
                    .setPositiveButton("RETRY",
                            (dialog, id) -> {
                                dialog.cancel();
                                finish();
                                startActivity(getIntent());

                            })
                    .setNegativeButton("QUITE",
                            (dialog, id) -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    finishAndRemoveTask();
                                }
                                else{
                                    finish();
                                }
                                dialog.cancel();
                            });
            mAlert = builder.create();
        }
        return mAlert;
    }
}
