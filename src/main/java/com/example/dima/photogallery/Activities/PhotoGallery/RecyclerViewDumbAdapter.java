package com.example.dima.photogallery.Activities.PhotoGallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.dima.photogallery.Activities.PhotoPage.PhotoPageActivity;
import com.example.dima.photogallery.R;
import com.example.dima.photogallery.Web.ThumbnailDownloader;

import java.util.List;

/**
 * Created by Dima on 08.02.2017.
 */

public class RecyclerViewDumbAdapter extends RecyclerView.Adapter<DumbHolder>{

    private Context mParentContext;

    @Override
    public DumbHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mParentContext = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(mParentContext);
        View view = inflater.inflate(R.layout.gallery_item, parent, false);
        return new DumbHolder(view);
    }

    @Override
    public void onBindViewHolder(DumbHolder holder, int position) {
        Drawable placeHolder = mParentContext.getResources().getDrawable(R.drawable.temp_image);
        holder.bindDrawable(placeHolder);
        holder.bindParentContext(mParentContext);
    }

    @Override
    public int getItemCount() {
        return 10;
    }
}

class DumbHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener
{
    private ImageView mItemImageView;
    private GalleryItem mGalleryItem;
    private Context mParentContext;

    public DumbHolder(View itemView){
        super(itemView);
        mItemImageView = (ImageView) itemView;
        mItemImageView.setOnClickListener(this);
    }

    public void bindDrawable(Drawable drawable){
        mItemImageView.setImageDrawable(drawable);
    }

    public void bindParentContext(Context parentContext) {
        mParentContext = parentContext;
    }

    //при нажатии на изображение, открыть страницу этого изображения
    @Override
    public void onClick(View v) {
    }
}
