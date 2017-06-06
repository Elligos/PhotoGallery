package com.example.dima.photogallery.Web;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dima on 30.05.2017.
 */

public class Photos {
    public int page;
    public int pages;
    public int perpage;
    public int total;
    List<Photo> photo = new ArrayList<>();

    public class Photo{
        public String id;
        public String owner;
        public String secret;
        public String server;
        public int farm;
        public String title;
        public int ispublic;
        public int isfriend;
        public int isfamily;
        public String url_s;
        public String height_s;
        public String width_s;
    }
}
