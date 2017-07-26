package com.example.dima.photogallery.Activities.PhotoGallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.example.dima.photogallery.Activities.Settings.SettingsActivity;
import com.example.dima.photogallery.Settings.PhotoGallerySettings;
import com.example.dima.photogallery.Web.FlickrFetchr;
import com.example.dima.photogallery.Services.QueryPreferences;
import com.example.dima.photogallery.R;
import com.example.dima.photogallery.Web.FlickrSearchResult;
import com.example.dima.photogallery.Web.ThumbnailDownloader;
import com.example.dima.photogallery.Activities.VisibleFragment;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Dima on 05.11.2016.
 */

public class PhotoGalleryFragment extends VisibleFragment
{
    private static final String TAG = "PhotoGalleryFragment";
    private static final int SETTINGS_ACTIVITY_REQUEST_CODE = 0x77;
    public static final int NETWORK_ERROR_ID = 1;
    private int mSpanCount = 2;//количество столбцов
    private int mImageHeight;
    private RecyclerView mPhotoRecyclerView;
    private RecyclerViewPhotoAdapter mAdapter;
    private List<GalleryItem> mItems;//список моделей, описывающих фотографии
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;//загрузчик фотографий с
                                                    //+ сайта Flickr
    private PhotoGallerySettings mSettings;//настройки приложения
    private int mCurrentPage = -1;//выбранная страница с фотографиями, которую отображает фрагмент
    private int mPagesAmount = 1;//общее количество страниц с фотографиями
    private Callbacks mCallbacks;//интерфейс обратного вызова
    private String mQuery;


    public interface Callbacks{
        void reloadPages();
        void handleError(int errorId);
    }

    //создать новый фрагмент
    public static PhotoGalleryFragment newInstance(ThumbnailDownloader<PhotoHolder> downloader,
                                                   PhotoGallerySettings settings,
                                                   int currentPage,
                                                   String query)
    {
        PhotoGalleryFragment pageFragment = new PhotoGalleryFragment();
        pageFragment.setCurrentPage(currentPage);
        pageFragment.setThumbnailDownloader(downloader);
        pageFragment.setSettings(settings);
        pageFragment.setQuery(query);
        Log.i(TAG, "Fragment(page: " + currentPage + ") instantiated.");
        return pageFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;//получить функции обратного вызова активности-хоста
        Log.i(TAG, "Photo gallery (page  " + mCurrentPage + ") attached.");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.i(TAG, "Photo gallery (page  " + mCurrentPage + ") detached.");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);//зарегистрировать фрагмент для получения обратных вызовов меню
                        //+ (фрагмент должен получить вызов onCreateOptionsMenu(…))
        Log.i(TAG, "Photo gallery fragment: " + this.getTag() +
                   " (page  " + mCurrentPage + ") created.");
        mSettings = PhotoGallerySettings.getPhotoGallerySettings(getContext());
        mSpanCount = mSettings.getAmountOfPhotosInRow();

        mImageHeight = calculateImageHeight();
    }

    //рассчитать размеры выводимого изображения, исходя из количества столбцов и размеров устройства
    private int calculateImageHeight(){
        int orientation;
        int screenWidth;
        int screenHeight;
        int imageWidth;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager  =  (WindowManager) getContext().
                getApplicationContext().
                getSystemService(Context.WINDOW_SERVICE);

        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        orientation = getContext().getResources().getConfiguration().orientation;
        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
            imageWidth = screenHeight/mSpanCount;
        }
        else{
            imageWidth = screenWidth/mSpanCount;
        }
        Log.i(TAG, "imageHeight=" + (imageWidth*3)/4 + "; imageWidth="+imageWidth+";");
        return  (imageWidth*3)/4;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Photo gallery fragment: " + this.getTag() +
                   " (page  " + mCurrentPage + ") destroyed.");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView =
                (RecyclerView) view.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), mSpanCount));
        myObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> startPhotoLoading(result), v -> handleNetworkRequestError());
        Log.i(TAG, "Photo gallery fragment view: " + this.getTag() +
                   " (page " + mCurrentPage + ") created.");
        return view;
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "Photo gallery view (page  " + mCurrentPage + ") destroyed.");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        super.onCreateOptionsMenu(menu, inflater);
        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);//получить ссылку на
                                        //+ созданный инструмент поиска
        final SearchView searchView = (SearchView) searchItem.getActionView();//получить интерфейс
                                        //+ для управления текстовым полем
        //назначить слушателя для поля поиска
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                //свернуть поле поиска
                searchView.setQuery("", false);
                searchView.setIconified(true);
                //загрузить изображения и обновить страницу
                mCallbacks.reloadPages();
                return true;
            }

            //обработка изменения текста в текстовом поле SearchView
            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "QueryTestChanged: " + newText);
                return false;
            }
        });
        //заполнить текстовое поле поиска сохраненным запросом, когда пользователь нажимает кнопку
        //+ поиска для открытия SearchView.
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });
        Log.i(TAG, "Photo gallery fragment OptionsMenu: " + this.getTag() +
                   " (page " + mCurrentPage + ") created.");
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //стереть сохраненный запрос
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                mCallbacks.reloadPages();
                return true;
            case R.id.menu_item_settings:
                Intent i = new Intent(getContext(), SettingsActivity.class);
                startActivityForResult(i, SETTINGS_ACTIVITY_REQUEST_CODE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case SETTINGS_ACTIVITY_REQUEST_CODE:
                Intent refresh = new Intent(getContext(), PhotoGalleryActivity.class);
                startActivity(refresh);
                ((Activity) getContext()).finish();
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    Observable<FlickrSearchResult> myObservable = Observable.create(subscriber -> {
        FlickrFetchr fetcher = new FlickrFetchr();
        FlickrSearchResult result = null;
        int photosPerPage = mSettings.getAmountOfPhotosInPage();
        fetcher.setPhotosPerPageAmount(photosPerPage);
        if(mQuery == null){
            result = fetcher.fetchRecentPhotosFromPage(mCurrentPage);//загрузить модели,
                                    //+ соответствующие последним добавленным фотографиям.
        } else{
            result = fetcher.searchPhotosInPage(mQuery, mCurrentPage);//загрузить модели,
                                    //+ соответствующие запросу.
        }
        subscriber.onNext(result);
        Log.i(TAG, "subscriber.onNext(result) called in page:" + mCurrentPage);
    });

    protected void startPhotoLoading(FlickrSearchResult result){
        mPagesAmount = result.getPagesAmount();
        mItems = result.getGalleryItems();
        mAdapter = new RecyclerViewPhotoAdapter(mItems, mThumbnailDownloader, mImageHeight);
        mPhotoRecyclerView.setAdapter(mAdapter);
        Log.i(TAG, "startPhotoLoading(result) called in page:" + mCurrentPage);
    }

    void handleNetworkRequestError(){
        mCallbacks.handleError(NETWORK_ERROR_ID);
    }


    //получить загрузчик фотографий
    public ThumbnailDownloader<PhotoHolder> getThumbnailDownloader() {
        return mThumbnailDownloader;
    }

    //задать загрузчик фотографий
    public void setThumbnailDownloader(ThumbnailDownloader<PhotoHolder> thumbnailDownloader) {
        mThumbnailDownloader = thumbnailDownloader;
    }

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public void setCurrentPage(int currentPage) {
        mCurrentPage = currentPage;
    }

    public int getPagesAmount() {
        return mPagesAmount;
    }

    public void setPagesAmount(int pagesAmount) {
        mPagesAmount = pagesAmount;
    }

    public List<GalleryItem> getItems() {
        return mItems;
    }

    public void setItems(List<GalleryItem> items) {
        mItems = items;
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    public void setSettings(PhotoGallerySettings settings) {
        mSettings = settings;
    }
}
