package com.example.dima.photogallery.Activities.PhotoGallery;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.example.dima.photogallery.R;
import com.example.dima.photogallery.Web.ThumbnailDownloader;
import android.support.v4.app.FragmentManager;

import java.util.List;

/**
 * Created by Dima on 06.11.2016.
 */

public class RecyclerViewPhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

    private static final String TAG = "PhotoAdapter";

    private List<GalleryItem> mGalleryItems;
    private Context mParentContext;
    ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int mImageHeight;

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
        Log.i(TAG, "Queue target in page " +
                    mGalleryItems.get(position).getPage() +
                    " for URL : " +
                    galleryItem.getUrl().replaceFirst("https://farm5.staticflickr.com/",""));
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

    //при нажатии на изображение, открыть страницу этого изображения
    @Override
    public void onClick(View v) {
//        Intent i = PhotoPageActivity.newIntent(mParentContext, mGalleryItem.getPhotoPageUri());
//        mParentContext.startActivity(i);

        FullscreenPhotoDialog fullscreenPhotoDialog = new FullscreenPhotoDialog();
//        fullscreenPhotoDialog.setTitle(mGalleryItem.getCaption());
//        fullscreenPhotoDialog.setAuthor(mGalleryItem.getOwner());
//        fullscreenPhotoDialog.setUrl(mGalleryItem.getUrl());
        fullscreenPhotoDialog.setGalleryItem(mGalleryItem);
        FragmentManager fragmentManager = ((FragmentActivity) mParentContext).getSupportFragmentManager();
        fullscreenPhotoDialog.show(fragmentManager, "fullscreenPhotoDialog");
    }
}