package com.example.dima.photogallery.Web;

import com.example.dima.photogallery.Activities.PhotoGallery.GalleryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dima on 04.12.2016.
 */
//результат поискового запроса
public class FlickrSearchResult {
    List<GalleryItem> mItems = new ArrayList<>();
    int page = 0;
    int pagesAmount = 0;

    void setGalleryItems(List<GalleryItem> items)
    {
        mItems.clear();
        mItems.addAll(items);
    }

    public List<GalleryItem> getGalleryItems()
    {
        return mItems;
    }

    GalleryItem getGalleryItem(int position) {
        return mItems.get(position);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPagesAmount() {
        return pagesAmount;
    }

    public void setPagesAmount(int pagesAmount) {
        this.pagesAmount = pagesAmount;
    }
}
