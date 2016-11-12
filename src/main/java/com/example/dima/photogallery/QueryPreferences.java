package com.example.dima.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Dima on 12.11.2016.
 */

public class QueryPreferences {
    private static final String PREF_SEARCH_QUERY = "SearchQuery";

    public static String getStoredQuery(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY, null);
    }

    public static void setStoredQuery(Context context, String query){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }

}
