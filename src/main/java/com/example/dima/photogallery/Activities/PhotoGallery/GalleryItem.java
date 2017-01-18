package com.example.dima.photogallery.Activities.PhotoGallery;

import android.net.Uri;

/**
 * Created by Dima on 06.11.2016.
 */
//класс модели, описывающей фотографию на фотохостинге Flickr
public class GalleryItem {
    private String mCaption;//заголовок фото
    private String mId;//идентификатор
    private String mUrl;//адрес фото для загрузки
    private String mOwner;//идентификатор пользователя, которому принадлежит фото

    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    //построить URL страницы фотографии
    public Uri getPhotoPageUri() {
        return Uri.parse("http://www.flickr.com/photos/")
                .buildUpon()
                .appendPath(mOwner)
                .appendPath(mId)
                .build();
    }

    @Override
    public String toString() {
        return getCaption();
    }
}
