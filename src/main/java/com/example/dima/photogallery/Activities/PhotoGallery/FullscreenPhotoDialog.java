package com.example.dima.photogallery.Activities.PhotoGallery;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.dima.photogallery.Activities.PhotoPage.PhotoPageActivity;
import com.example.dima.photogallery.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Dima on 12.06.2017.
 */

public class FullscreenPhotoDialog extends DialogFragment {
    private static final String TAG = "FullscreenPhotoDialog";
    private String mTitle;
    private String mAuthor;
    private String mPhotoUrl;
    private Uri mPhotoSourceUri;
    private GalleryItem mGalleryItem;
    private Context mParentContext;
    private Bitmap mImage;
    private String mSavedImagePath = "";
    private Target mMyTarget = null;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        Dialog dialog = super.onCreateDialog(savedInstanceState);

//        Dialog dialog = new Dialog(getActivity(),android.R.style.Theme_Translucent_NoTitleBar);
        Dialog dialog = new Dialog(getActivity(),android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        final View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_fullscreen_photo, null);
        final Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(200);
        dialog.getWindow().setBackgroundDrawable(d);
        dialog.getWindow().setContentView(view);
        final WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.CENTER;
        dialog.setCanceledOnTouchOutside(true);

//        int orientation;
//        int screenWidth;
//        int screenHeight;
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        WindowManager windowManager  =  (WindowManager) getContext().
//                getApplicationContext().
//                getSystemService(Context.WINDOW_SERVICE);
//        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
//        screenWidth = displayMetrics.widthPixels;
//        screenHeight = displayMetrics.heightPixels;
//        orientation = getContext().getResources().getConfiguration().orientation;
//        if(orientation == Configuration.ORIENTATION_LANDSCAPE){
//            params.width = screenHeight*75/100;
//            params.height = screenWidth*75/100;
//        }
//        else{
//            params.width = screenWidth*75/100;
//            params.height = screenHeight*75/100;
//        }

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {

        View view = inflater.inflate(R.layout.dialog_fullscreen_photo, null);

        mParentContext = inflater.getContext();

        ImageButton btnSavePhoto = (ImageButton) view.findViewById(R.id.btn_save_photo);
        ImageButton btnToPhotoWebSource = (ImageButton) view.findViewById(R.id.btn_go_to_photo_web_source);
        ImageButton btnCloseDialog = (ImageButton) view.findViewById(R.id.btn_close_fullscreen_photo_dialog);

//        btnCloseDialog.setOnClickListener(v -> dismiss());
//        btnToPhotoWebSource.setOnClickListener(v -> goToWebSource());
//        btnSavePhoto.setOnClickListener(v -> savePhoto());

        btnCloseDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        btnToPhotoWebSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToWebSource();
            }
        });
        btnSavePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePhoto();
            }
        });


        if(savedInstanceState == null){
            mTitle = mGalleryItem.getCaption();
            mAuthor = mGalleryItem.getOwner();
            mPhotoUrl = mGalleryItem.getUrl();
            mPhotoSourceUri = mGalleryItem.getPhotoPageUri();
        }
        else{
            mTitle = savedInstanceState.getParcelable("FullscreenPhotoDialog_Title");
            mAuthor = savedInstanceState.getString("FullscreenPhotoDialog_Author");
            mPhotoUrl = savedInstanceState.getString("FullscreenPhotoDialog_Url");
            mPhotoSourceUri = savedInstanceState.getParcelable("FullscreenPhotoDialog_Uri");
        }

        EditText etTitle = (EditText) view.findViewById(R.id.et_photo_title);
        etTitle.setText("TITLE: "+mTitle);
        EditText etAuthor = (EditText) view.findViewById(R.id.et_photo_author);
        etAuthor.setText("AUTHOR: "+mAuthor);
        ImageView photoImageView = (ImageView) view.findViewById(R.id.image_view_fullscreen_photo);
        photoImageView.setDrawingCacheEnabled(true);
        final Target myTarget = new Target() {
            @Override
            public void onBitmapLoaded (final Bitmap bitmap, Picasso.LoadedFrom from){
                //Set it in the ImageView
                photoImageView.setImageBitmap(bitmap);
                mImage = bitmap;
                view.invalidate();
                mMyTarget = this;
                Log.i(TAG, "onBitmapLoaded()");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.i(TAG, "onBitmapFailed()");
            }
        };
        if(savedInstanceState == null){
            Picasso.with(inflater.getContext()).setLoggingEnabled(true);
            Picasso.with(inflater.getContext()).
                    load(mPhotoUrl).
                    into(myTarget);
            photoImageView.setTag(myTarget);//to prevent be garbage collected
        }
        else{
            mImage = savedInstanceState.getParcelable("FullscreenPhotoDialog_Image");
            photoImageView.setImageBitmap(mImage);
            Log.i(TAG, "Image restored");
        }
        Log.i(TAG, "FullscreenPhotoDialog view created");
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("FullscreenPhotoDialog_Image", mImage);
        outState.putString("FullscreenPhotoDialog_Title", mTitle);
        outState.putString("FullscreenPhotoDialog_Author", mAuthor);
        outState.putParcelable("FullscreenPhotoDialog_Uri", mPhotoSourceUri);
        outState.putString("FullscreenPhotoDialog_Url", mPhotoUrl);
        super.onSaveInstanceState(outState);
    }

    public void setTitle(String title){
        mTitle = title;
    }

    public void setAuthor(String author){
        mAuthor = author;
    }

    public void setUrl(String url){
        mPhotoUrl = url;
    }

    public void setGalleryItem(GalleryItem galleryItem) {
        mGalleryItem = galleryItem;
    }

    private void goToWebSource(){
        Intent i = PhotoPageActivity.newIntent(mParentContext, mPhotoSourceUri);
        mParentContext.startActivity(i);
        dismiss();
    }

    private void savePhoto(){
//        Toast.makeText(mParentContext, "MAYBE LATER...", Toast.LENGTH_SHORT).show();
        if(mSavedImagePath == "") {
//            saveToInternalStorage();
            saveToGallery();
            Toast.makeText(mParentContext, "SAVED IN GALLERY", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(mParentContext, "ALREADY SAVED", Toast.LENGTH_LONG).show();
        }
//        Toast.makeText(mParentContext, "Saved in "+mSavedImagePath, Toast.LENGTH_LONG).show();
    }

    private void saveToInternalStorage(){
        ContextWrapper cw = new ContextWrapper(mParentContext.getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("savedPhotos", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,"profile.jpg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            mImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSavedImagePath = directory.getAbsolutePath();
//            return directory.getAbsolutePath();
        }
    }



    private void saveToGallery(){
        ContentResolver resolver = mParentContext.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
//        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        MediaStore.Images.Media.insertImage(resolver, mImage, mTitle , "Author: " + mAuthor);
    }

    private void saveToExternalStorage(){

    }
}