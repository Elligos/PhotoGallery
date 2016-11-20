package com.example.dima.photogallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

/**
 * Created by Dima on 06.11.2016.
 */

public class RecyclerViewPhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

    private List<GalleryItem> mGalleryItems;
    private Context mParentContext;
    ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public RecyclerViewPhotoAdapter(List<GalleryItem> galleryItems,
                                    ThumbnailDownloader<PhotoHolder> downloader){
        mGalleryItems = galleryItems;
        mThumbnailDownloader = downloader;
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
        GalleryItem galleryItem = mGalleryItems.get(position);
        Drawable placeHolder = mParentContext.getResources().getDrawable(R.drawable.temp_image);
        holder.bindDrawable(placeHolder);
        holder.bindGalleryItem(galleryItem);
        holder.bindParentContext(mParentContext);
        mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());
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

    @Override
    public void onClick(View v) {
        Intent i = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
        mParentContext.startActivity(i);
    }
}