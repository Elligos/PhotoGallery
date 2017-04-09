package com.example.dima.photogallery.Web;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.dima.photogallery.Activities.PhotoGallery.GalleryItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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



    @Nullable
    public List<FlickrSearchResult> fetchRecentPhotos(){
        List<FlickrSearchResult> fetchResults = new ArrayList<>();
        FlickrSearchResult result = fetchRecentPhotosFromPage(1);
        if (result == null) {
            return null;
        }
        fetchResults.add(result);
        int pagesAmount = result.getPagesAmount();
        if(pagesAmount>10){
            pagesAmount=10;
        }
        for(int page = 2; page <= pagesAmount; page++) {
            result = fetchRecentPhotosFromPage(page);
            fetchResults.add(result);
        }
        return fetchResults;
    }

    @Nullable
    public List<FlickrSearchResult> searchPhotos(String query){
        List<FlickrSearchResult> fetchResults = new ArrayList<>();
        FlickrSearchResult result = searchPhotosInPage(query, 1);
        if (result == null) {
            return null;
        }
        fetchResults.add(result);
        int pagesAmount = result.getPagesAmount();
        if(pagesAmount>10){
            pagesAmount=10;
        }
        for(int page = 2; page <= pagesAmount; page++) {
            result = searchPhotosInPage(query, page);
            fetchResults.add(result);
        }
        return fetchResults;
    }




    //вернуть резудьтат поиска недавних фотографий с соответствующей страницы
    public FlickrSearchResult fetchRecentPhotosFromPage(int page){
        String url = buildUrl(FETCH_RECENTS_METHOD, null, page);
        return executeSearch(url);
    }

    //построить  url, соответствующий выбранному методу, возвращающий данные с соответствующей
    // страницы
    private String buildUrl(String method, String query, int page){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);
        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }
        uriBuilder.appendQueryParameter("page", String.valueOf(page));
        return uriBuilder.build().toString();
    }

    //вернуть резудьтат поискового запроса с соответствующей страницы
    public FlickrSearchResult searchPhotosInPage(String query, int page){
        String url = buildUrl(SEARCH_METHOD, query, page);
        return executeSearch(url);
    }

    //вернуть резудьтат url-запроса
    public FlickrSearchResult executeSearch(String url){
        FlickrSearchResult result = new FlickrSearchResult();
        try{
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            parseItems(result, jsonString);
        } catch(JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        } catch(IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        }
        return result;
    }

    //бесжалостно расчленить Джейсона, чтобы достать модели для фотографий и сопутствующую информацию
    static public void parseItems(FlickrSearchResult result, String jsonString)
            throws IOException, JSONException
    {
        List<GalleryItem> items = new ArrayList<>();

        result.setOriginalJsonString(jsonString);
        sLastSearchedJsonString = jsonString;
        Log.i(TAG, "Parsed JSON: " + jsonString);
        JSONObject jsonBody = new JSONObject(jsonString);

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for(int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            if(!photoJsonObject.has("url_s")){
                continue;
            }
            item.setUrl(photoJsonObject.getString("url_s"));
            item.setOwner(photoJsonObject.getString("owner"));
            items.add(item);
        }
        result.setGalleryItems(items);
        String pagesField = photosJsonObject.getString("pages");
        String pageField = photosJsonObject.getString("page");
        Integer pages = Integer.valueOf(pagesField);
        Integer page = Integer.valueOf(pageField);
        if(pages!=null) {
            result.setPagesAmount(pages);
        }
        else{
            throw new IOException();
        }
        if(page!=null) {
            result.setPage(page);
        }
        else{
            throw new IOException();
        }
    }

    //получить результат url-запроса в виде строки
    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    //получить низкоуровневые данные по URL и вернуть их в виде массива байтов
    public byte[] getUrlBytes(String urlSpec) throws IOException{
        final int BUFFER_SIZE = 1024;
        URL url = new URL(urlSpec);
//        Log.i(TAG, "openConnection(): start");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();//подключиться к
        // ресурсу, указанному в URL
//        Log.i(TAG, "openConnection(): stop");
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
//            Log.i(TAG, "getInputStream(): start");
            InputStream in = connection.getInputStream();
//            Log.i(TAG, "getInputStream(): stop");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte [] buffer = new byte[BUFFER_SIZE];
//            Log.i(TAG, "reading bytes: start");
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
//            Log.i(TAG, "reading bytes: stop");
            out.flush();
            out.close();
            return out.toByteArray();
        }
        finally {
//            Log.i(TAG, "disconnect(): start");
            connection.disconnect();
//            Log.i(TAG, "disconnect(): stop");
        }
    }

    @Nullable
    public static String getLastSearchedJsonString() {
        return sLastSearchedJsonString;
    }



}
