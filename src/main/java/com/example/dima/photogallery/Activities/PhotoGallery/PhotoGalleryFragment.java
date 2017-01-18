package com.example.dima.photogallery.Activities.PhotoGallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.dima.photogallery.Web.FlickrFetchr;
import com.example.dima.photogallery.Services.PollService;
import com.example.dima.photogallery.Services.QueryPreferences;
import com.example.dima.photogallery.R;
import com.example.dima.photogallery.Web.FlickrSearchResult;
import com.example.dima.photogallery.Web.ThumbnailDownloader;
import com.example.dima.photogallery.Activities.VisibleFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dima on 05.11.2016.
 */

public class PhotoGalleryFragment extends VisibleFragment
{
    private static final String TAG = "PhotoGalleryFragment";

    private int SPAN_COUNT = 3;//количество столбцов
    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();//список моделей, описывающих фотографии
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;//загрузчик фотографий с сайта Flickr

    private int mCurrentPage = 1;//выбранная страница с фотографиями, которую отображает фрагмент
    private int mPagesAmount = 1;//общее количество страниц с фотографиями
    private Callbacks mCallbacks;//интерфейс обратного вызова

    public interface Callbacks{
        void setCurrentPhotoPage(int currentPage);
        void setPhotoPagesAmount(int pagesAmount);
        void reloadPages();
    }

//    public static PhotoGalleryFragment newInstance() {
//        return new PhotoGalleryFragment();
//    }
//
//    public static PhotoGalleryFragment newInstance(int currentPage) {
//        PhotoGalleryFragment pageFragment = new PhotoGalleryFragment();
//        pageFragment.setCurrentPage(currentPage);
//        return pageFragment;
//    }

    //создать новый фрагмент
    public static PhotoGalleryFragment newInstance(int currentPage,
                                                   ThumbnailDownloader<PhotoHolder> downloader)
    {
        Log.i(TAG, "Background thread " + downloader.getId() + " queue cleared.");
        PhotoGalleryFragment pageFragment = new PhotoGalleryFragment();
        pageFragment.setCurrentPage(currentPage);
        pageFragment.setThumbnailDownloader(downloader);
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
        setRetainInstance(true);//сохранять текущий фрагмент между изменениями конфигурации
        setHasOptionsMenu(true);//зарегистрировать фрагмент для получения обратных вызовов меню
                        //+ (фрагмент должен получить вызов onCreateOptionsMenu(…))
        updateItems();//обновить модели

        PollService.setServiceAlarm(getActivity(), true);//запустить службу на проверку наличия
                                        //+ новых фотографий в хостинге Flickr
        Log.i(TAG, "Photo gallery (page  " + mCurrentPage + ") created.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Photo gallery (page  " + mCurrentPage + ") destroyed.");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), SPAN_COUNT));
        setupAdapter();
        Log.i(TAG, "Photo gallery view (page  " + mCurrentPage + ") created.");
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "Photo gallery view (page  " + mCurrentPage + ") destroyed.");
//        mThumbnailDownloader.clearQueue();
//        Log.i(TAG, "Background thread " + mThumbnailDownloader.getId() + " queue cleared");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);//получить ссылку на
                                        //+ созданный инструмент поиска
        final SearchView searchView = (SearchView) searchItem.getActionView();//получить интерфейс
                                        //+ для управления текстовым полем

        //вывести на панель, включена ли служба опроса
        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
        //назначить слушателя для поля поиска
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                updateItems();
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

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //стереть сохраненный запрос
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            //вкл/выкл службу опроса
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    //обновить модели
    private void updateItems(){
        String query = QueryPreferences.getStoredQuery(getActivity());//получить последний поисковый
                                                //+ запрос, если имеется, либо null
        new FetchItemTask(query).execute();//обновить
    }

    //    private class FetchItemTask extends AsyncTask<Void, Void, List<GalleryItem>> {
//        private static final String TAG = "PhotoGalleryFragment";
//        private String mQuery;
//
//        public FetchItemTask(String query){
//            mQuery = query;
//        }
//
//        @Override
//        protected List<GalleryItem> doInBackground(Void... params) {
//            if(mQuery == null){
//                return new FlickrFetchr().fetchRecentPhotos();
//            } else{
//                return new FlickrFetchr().searchPhotos(mQuery);
//            }
//        }
//
//        @Override
//        protected void onPostExecute(List<GalleryItem> galleryItems) {
//            mItems  = galleryItems;
//            setupAdapter();
//        }
//    }

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
            mItems  = result.getGalleryItems();
            mCallbacks.setPhotoPagesAmount(result.getPagesAmount());
            setupAdapter();
        }
    }



    //настроить адаптер для RecyclerView
    private void setupAdapter() {
        //если фрагмент подключен к активности, настроить адаптер для набора данных mPhotoRecyclerView
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new RecyclerViewPhotoAdapter(mItems, mThumbnailDownloader));
        }
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


}
