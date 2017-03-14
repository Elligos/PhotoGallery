package com.example.dima.photogallery.Activities.PhotoGallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
    private List<GalleryItem> mItems;// = new ArrayList<>();//список моделей, описывающих фотографии
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;//загрузчик фотографий с сайта Flickr

    private int mCurrentPage = -1;//выбранная страница с фотографиями, которую отображает фрагмент
    private int mPagesAmount = 1;//общее количество страниц с фотографиями
    private Callbacks mCallbacks;//интерфейс обратного вызова

    public interface Callbacks{
        void setCurrentPhotoPage(int currentPage);
        void setPhotoPagesAmount(int pagesAmount);
        void reloadPages();
        void onSaveFragment(Bundle outState, Fragment fragment);
        List<GalleryItem> getItems();
        List<GalleryItem> getItemsForPage(int page);
        ThumbnailDownloader<PhotoHolder> getDownloader();
        int getCurrentPage();
    }

    //создать новый фрагмент
    public static PhotoGalleryFragment newInstance(List<GalleryItem> items,
                                                   ThumbnailDownloader<PhotoHolder> downloader,
                                                   int currentPage)
    {
        PhotoGalleryFragment pageFragment = new PhotoGalleryFragment();
        pageFragment.setItems(items);
        pageFragment.setCurrentPage(currentPage);
        pageFragment.setThumbnailDownloader(downloader);
        Log.i(TAG, "Fragment(page: " + currentPage + ") instantiated.");
        return pageFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = (Callbacks) context;//получить функции обратного вызова активности-хоста
//        mItems = mCallbacks.getItems();
//        mCurrentPage = mCallbacks.getCurrentPage();
//        mThumbnailDownloader = mCallbacks.getDownloader();
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
//        setRetainInstance(true);//сохранять текущий фрагмент между изменениями конфигурации
        setHasOptionsMenu(true);//зарегистрировать фрагмент для получения обратных вызовов меню
                        //+ (фрагмент должен получить вызов onCreateOptionsMenu(…))
        PollService.setServiceAlarm(getActivity(), true);//запустить службу на проверку наличия
                                        //+ новых фотографий в хостинге Flickr
        Log.i(TAG, "Photo gallery fragment: " + this.getTag() + " (page  " + mCurrentPage + ") created.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Photo gallery fragment: " + this.getTag() + " (page  " + mCurrentPage + ") destroyed.");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), SPAN_COUNT));
        updateItems();//обновить модели
        Log.i(TAG, "Photo gallery fragment view: " + this.getTag() + " (page " + mCurrentPage + ") created.");
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "Photo gallery view (page  " + mCurrentPage + ") destroyed.");
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
                //updateItems();
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
    public void updateItems(){
//        String query = QueryPreferences.getStoredQuery(getActivity());//получить последний поисковый
//                                                //+ запрос, если имеется, либо null
        //new FetchItemTask(query).execute();//обновить
        Log.i(TAG, "updateItems() in page " + mCurrentPage + ") called.");
        setupAdapter();
    }


    //настроить адаптер для RecyclerView
    private void setupAdapter() {
        Log.i(TAG, "setupAdapter() in page " + mCurrentPage + ") called.");
        //если фрагмент подключен к активности, настроить адаптер для набора данных mPhotoRecyclerView
        if (isAdded()) {
            if((mItems == null) || (mThumbnailDownloader == null)){
                if(mItems == null){
                    Log.i(TAG, "mItems == NULL in page " + mCurrentPage + ") called.");
                }
                if(mThumbnailDownloader == null){
                    Log.i(TAG, "mThumbnailDownloader == NULL in page " + mCurrentPage + ") called.");
                }
                mPhotoRecyclerView.setAdapter(new RecyclerViewDumbAdapter());
            }
            else {
                mPhotoRecyclerView.setAdapter(new RecyclerViewPhotoAdapter(mItems, mThumbnailDownloader));
            }
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

    public List<GalleryItem> getItems() {
        return mItems;
    }

    public void setItems(List<GalleryItem> items) {
        mItems = items;
    }
}
