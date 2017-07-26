package com.example.dima.photogallery.Web;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.dima.photogallery.Activities.PhotoGallery.GalleryItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dima on 06.11.2016.
 */

public class FlickrFetchr {
    private static final String TAG = "FlickFetchr";
    private static final String API_KEY = "4fbe78b172a473f95e19018b057fd305";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static String sLastSearchedJsonString = null;

    //общая часть url-запроса
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
                                            .buildUpon()
                                            .appendQueryParameter("method", "flickr.photos.getRecent")
                                            .appendQueryParameter("api_key", API_KEY)
                                            .appendQueryParameter("format", "json")
                                            .appendQueryParameter("nojsoncallback", "1")
                                            .appendQueryParameter("extras", "url_s")
                                            .build();

    private int mPhotosPerPage = 20;


    public void setPhotosPerPageAmount(int photosPerPage) {
        mPhotosPerPage = photosPerPage;
    }


    //вернуть резудьтат поиска недавних фотографий с соответствующей страницы
    public FlickrSearchResult fetchRecentPhotosFromPage(int page)
                            throws IOException,
                                   JSONException {
        String url = buildUrl(FETCH_RECENTS_METHOD, null, page);
        return executeSearch(url);
    }

    //построить  url, соответствующий выбранному методу, возвращающий данные с соответствующей
    // страницы
    private String buildUrl(String method, String query, int page){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("per_page", String.valueOf(mPhotosPerPage))
                .appendQueryParameter("method", method);

        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }
        uriBuilder.appendQueryParameter("page", String.valueOf(page));
        return uriBuilder.build().toString();
    }

    //вернуть резудьтат поискового запроса с соответствующей страницы
    public FlickrSearchResult searchPhotosInPage(String query, int page)
                       throws IOException,
                              JSONException {
        String url = buildUrl(SEARCH_METHOD, query, page);
        return executeSearch(url);
    }

    //вернуть резудьтат url-запроса
    public FlickrSearchResult executeSearch(String url) throws IOException, JSONException {
        FlickrSearchResult result = new FlickrSearchResult();
        try{
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            parseItems(result, jsonString);
        } catch(JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
            throw je;
        } catch(IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
            throw ioe;
        }
        return result;
    }



    //бесжалостно расчленить Джейсона, чтобы достать модели для фотографий и сопутствующую информацию
    static public void parseItems(FlickrSearchResult result, String jsonString)
            throws JSONException
    {
        List<GalleryItem> items = new ArrayList<>();
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JsonPhotos photos;
        photos = gson.fromJson(jsonString, JsonPhotos.class);
        result.setOriginalJsonString(jsonString);
        sLastSearchedJsonString = jsonString;
        Log.i(TAG, "Parsed JSON: " + jsonString);
        ArrayList<Photos.Photo> photoArray = (ArrayList< Photos.Photo>) photos.photos.photo;
        for(int i = 0; i < photoArray.size(); i++) {
            Photos.Photo photo = photoArray.get(i);
            GalleryItem item = new GalleryItem();
            item.setId(photo.id);
            item.setCaption(photo.title);
            if(photo.url_s == null){
                continue;
            }
            item.setUrl(photo.url_s);
            item.setOwner(photo.owner);
            item.setPage(photos.photos.page);
            items.add(item);
        }
        result.setGalleryItems(items);
        Integer pages = photos.photos.pages;
        Integer page = photos.photos.page;
        result.setPage(page);
        result.setPagesAmount(pages);
    }

    //получить результат url-запроса в виде строки
    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    //получить низкоуровневые данные по URL и вернуть их в виде массива байтов
    public byte[] getUrlBytes(String urlSpec) throws IOException{
        final int BUFFER_SIZE = 1024;
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();//подключиться к
        //+ ресурсу, указанному в URL
        try{
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }
            InputStream in = connection.getInputStream();
            if(in == null){
                throw new IOException(connection.getResponseMessage() +
                                        ": getInputStream() for " + urlSpec);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int bytesRead = 0;
            byte [] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            out.close();
            return out.toByteArray();
        }
        finally {
            if(connection != null){
                connection.disconnect();
            }
        }
    }

    @Nullable
    public static String getLastSearchedJsonString() {
        return sLastSearchedJsonString;
    }



}
