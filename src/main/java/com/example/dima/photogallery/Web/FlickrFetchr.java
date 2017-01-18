package com.example.dima.photogallery.Web;

import android.net.Uri;
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

    //общая часть url-запроса
    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
                                            .buildUpon()
                                            .appendQueryParameter("method", "flickr.photos.getRecent")
                                            .appendQueryParameter("api_key", API_KEY)
                                            .appendQueryParameter("format", "json")
                                            .appendQueryParameter("nojsoncallback", "1")
                                            .appendQueryParameter("extras", "url_s")
                                            .build();

    //получить низкоуровневые данные по URL и вернуть их в виде массива байтов
    public byte[] getUrlBytes(String urlSpec) throws IOException{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();//подключиться к
                                                    // ресурсу, указанному в URL
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte [] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        }
        finally {
            connection.disconnect();
        }
    }

    //получить результат url-запроса в виде строки
    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    //загрузить модели последних фотографий
    public List<GalleryItem> fetchRecentPhotos(){
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    //построить  url, соответствующий выбранному методу
    private String buildUrl(String method, String query){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);
        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    //загрузить модели фотографий, соответствующих поисковому запросу
    public List<GalleryItem> searchPhotos(String query){
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }

    //загрузить модели фотографий по url
    public List<GalleryItem> downloadGalleryItems(String url){
        List<GalleryItem> items = new ArrayList<>();

        try{

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch(JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        } catch(IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        }

        return items;
    }


    //бесжалостно расчленить Джейсона, чтобы получить модели для фотографий
    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
                            throws IOException, JSONException
    {
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
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(result, jsonBody);
        } catch(JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        } catch(IOException ioe){
            Log.e(TAG, "Failed to fetch items", ioe);
        }
        return result;
    }

    //снова расчленить Джейсона, чтобы получить нужный результат
    private void parseItems(FlickrSearchResult result, JSONObject jsonBody)
            throws IOException, JSONException
    {
        List<GalleryItem> items = new ArrayList<>();
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
}
