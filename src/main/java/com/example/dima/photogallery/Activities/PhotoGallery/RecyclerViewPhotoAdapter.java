package com.example.dima.photogallery.Activities.PhotoGallery;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.dima.photogallery.Activities.PhotoPage.PhotoPageActivity;
import com.example.dima.photogallery.R;
import com.example.dima.photogallery.Web.ThumbnailDownloader;

import java.util.List;

/**
 * Created by Dima on 06.11.2016.
 */

public class RecyclerViewPhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

    private List<GalleryItem> mGalleryItems;
    private Context mParentContext;
    ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int mImageHeight;

    public RecyclerViewPhotoAdapter(List<GalleryItem> galleryItems,
                                    ThumbnailDownloader<PhotoHolder> downloader){
        mGalleryItems = galleryItems;//передать модели, по которым будет проводиться загрузка фото
        mThumbnailDownloader = downloader;//передать загрузчик
    }

    public RecyclerViewPhotoAdapter(List<GalleryItem> galleryItems,
                                    ThumbnailDownloader<PhotoHolder> downloader,
                                    int imageHeight){
        mGalleryItems = galleryItems;//передать модели, по которым будет проводиться загрузка фото
        mThumbnailDownloader = downloader;//передать загрузчик
        mImageHeight = imageHeight;
    }

    @Override
    public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mParentContext = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(mParentContext);
        View view = inflater.inflate(R.layout.gallery_item, parent, false);
        return new PhotoHolder(view);
    }

    @Override
    public void onBindViewHolder(PhotoHolder holder, int position) {
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        params.height = mImageHeight;
        holder.itemView.setLayoutParams(params);

        Drawable placeHolder = mParentContext.getResources().getDrawable(R.drawable.temp_image);
        holder.bindDrawable(placeHolder);
        holder.bindParentContext(mParentContext);
        if(mGalleryItems == null){
            return;
        }
        GalleryItem galleryItem = mGalleryItems.get(position);
        if (galleryItem == null) {
            return;
        }
        holder.bindGalleryItem(galleryItem);
        mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());//запустить загрузку
        //+ фотографии
    }

    @Override
    public int getItemCount() {
        return mGalleryItems.size();
    }
}

class PhotoHolder extends RecyclerView.ViewHolder
                    implements View.OnClickListener
{
    private ImageView mItemImageView;
    private GalleryItem mGalleryItem;
    private Context mParentContext;
    private int mImageHeight;

    public PhotoHolder(View itemView){
        super(itemView);
        mItemImageView = (ImageView) itemView;
        mItemImageView.setOnClickListener(this);
    }

    public void bindDrawable(Drawable drawable){
        mItemImageView.setImageDrawable(drawable);
    }

    public void bindGalleryItem(GalleryItem galleryItem) {
        mGalleryItem = galleryItem;
    }

    public void bindParentContext(Context parentContext) {
        mParentContext = parentContext;
    }

    public void bindImageHeight(int imageHeight) {
        mImageHeight = imageHeight;
    }

    //при нажатии на изображение, открыть страницу этого изображения
    @Override
    public void onClick(View v) {
//        Intent i = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
        Intent i = PhotoPageActivity.newIntent(mParentContext, mGalleryItem.getPhotoPageUri());
        mParentContext.startActivity(i);
    }
}