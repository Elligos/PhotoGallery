package com.example.dima.photogallery.Web;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.example.dima.photogallery.Web.FlickrFetchr;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Dima on 06.11.2016.
 */

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private Handler mRequestHandler;//обработчик текущего потока ThumbnailDownloader
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();//хранит объекты,
                                                                //+ связанные с запросами

    private Handler mResponseHandler;//обработчик потока, создавшего ThumbnailDownloader
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    //интерфейс для обработки загруженного изображения после завершения загрузки
    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
        void onThumbnailDownloadError(T target);
    }
    //назначить слушателя, ответственного за загрузку и обработку изображения
    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }


    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;
    }

    //реализация обработчика запросов
    // (метод вызывается до того, как Looper впервые проверит очередь)
    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Thread "+ getId() + " got a request for URL: " + mRequestMap.get(target).replaceFirst("https://farm5.staticflickr.com/",""));
                    handleRequest(target);
                }
            }
        };
    }

    //обработать запрос из очереди
    private void handleRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                Log.i(TAG, "Null url detected");
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");
            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mRequestMap.get(target) != url) {
                        Log.i(TAG, "Url not coincide");
                        return;
                    }

                    mRequestMap.remove(target);
                    Log.i(TAG, "thumbnail downloaded for URL: " + url.replaceFirst("https://farm5.staticflickr.com/",""));
                    mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
            mThumbnailDownloadListener.onThumbnailDownloadError(target);
        }
    }

    //добавить в очередь запрос на загрузку по url-адресу для объекта Т
    public void queueThumbnail(T target, String url) {
        if(url == null){
            mRequestMap.remove(target);
            Log.e(TAG, "Queue target failed for the reason : url == null");
        } else{
                mRequestMap.put(target, url);
                mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                        .sendToTarget();
        }
    }

    //очистить очередь загрузки
    public void clearQueue(){
        mResponseHandler.removeMessages(MESSAGE_DOWNLOAD);
    }
}



