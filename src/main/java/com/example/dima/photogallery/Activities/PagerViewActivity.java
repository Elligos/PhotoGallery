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

import java.util.UUID;

/**
 * Created by Dima on 02.12.2016.
 */
//абстрактный класс для активностей с PagerView
public abstract class PagerViewActivity extends AppCompatActivity{

    private ViewPager mViewPager;

    protected abstract Fragment getPageFragment(int position);//получить фрагмент для соответствующей позиции
    protected abstract int getPagesAmount();//получить общее количество страниц
    protected abstract void onPageSelectedAction(int position);//обработка выбора новой страницы

    protected int getLayoutResId(){
        return R.layout.view_pager_activity;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResId());

        mViewPager = (ViewPager) findViewById(R.id.view_pager_activity);
        FragmentManager fragmentManager = getSupportFragmentManager();
        mViewPager.addOnPageChangeListener(pageChangeListener);
        mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                return getPageFragment(position);
            }

            @Override
            public int getCount() {
                return getPagesAmount();
            }

        });
        if(getPagesAmount()==0) {
            mViewPager.setCurrentItem(1);
        }
    }

    //слушатель событий изменения страниц в PageeView
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

    //перезагрузить адаптер
    protected void reloadAdapter(){
        FragmentManager fragmentManager = getSupportFragmentManager();
        mViewPager.clearOnPageChangeListeners();
        mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {
            @Override
            public Fragment getItem(int position) {
                return getPageFragment(position);
            }

            @Override
            public int getCount() {
                return getPagesAmount();
            }

        });
        mViewPager.getAdapter().notifyDataSetChanged();
        mViewPager.addOnPageChangeListener(pageChangeListener);
        if(getPagesAmount()==0) {
            mViewPager.setCurrentItem(1);
        }
    }
}
