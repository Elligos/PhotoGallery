package com.example.dima.photogallery.Activities;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.dima.photogallery.R;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Dima on 02.12.2016.
 */
//абстрактный класс для активностей с PagerView
public abstract class ViewPagerActivity extends AppCompatActivity{
    private static final String TAG = "PhotoGalleryFragment";
    private ViewPager mViewPager;

    protected abstract Fragment getPageFragment(int position);//получить фрагмент для соответствующей позиции
    protected abstract int getPagesAmount();//получить общее количество страниц
    protected abstract void onPageSelectedAction(int position);//обработка выбора новой страницы

    protected int getLayoutResId(){
        return R.layout.view_pager_activity;
    }

    TaggedPagerAdapter mViewPagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());

        mViewPager = (ViewPager) findViewById(R.id.view_pager_activity);
        Log.i(TAG, "ViewPagerActivity created");
    }

    //слушатель событий изменения страниц в PagerView
    ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrollStateChanged(int arg0) { }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) { }

        @Override
        public void onPageSelected(int position) {
            onPageSelectedAction(position);
        }

    };

    protected void loadAdapter(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        mViewPager.addOnPageChangeListener(pageChangeListener);
        mViewPagerAdapter = new TaggedPagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                Log.i(TAG, "Get item with position:" + position);
                return getPageFragment(position);
            }

            @Override
            public int getCount() {
                return getPagesAmount();
            }

        };
        mViewPager.setAdapter(mViewPagerAdapter);
        if(getPagesAmount()==0) {
            mViewPager.setCurrentItem(1);
        }
    }

    @Nullable
    protected ArrayList<Fragment> getActiveFragments(){
        if(mViewPagerAdapter == null) return null;
        return mViewPagerAdapter.getActiveFragments();
    }
}
