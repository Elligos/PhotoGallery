package com.example.dima.photogallery.Activities.Settings;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import com.example.dima.photogallery.R;
import com.example.dima.photogallery.Settings.PhotoGallerySettings;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsActivityFragment extends Fragment {

    String[] mPhotosPerPageAmountBuffer = {"10", "20", "30", "50", "100"};
    String[] mPhotosPerRowAmountBuffer = {"1", "2", "3", "4", "5"};
    PhotoGallerySettings mPhotoGallerySettings;

    public SettingsActivityFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhotoGallerySettings = PhotoGallerySettings.getPhotoGallerySettings(getContext());
//        setHasOptionsMenu(true);//зарегистрировать фрагмент для получения обратных вызовов меню
        //+ (фрагмент должен получить вызов onCreateOptionsMenu(…))
        Intent data = new Intent();
        ((Activity) (getContext())).setResult(Activity.RESULT_OK, data);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        createPhotosPerPageSpinnerView(v);
        createPhotosPerRowSpinnerView(v);
        createPollEnableCheckBoxView(v);
        return v;
    }

    private void createPhotosPerPageSpinnerView(View v){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                mPhotosPerPageAmountBuffer);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = (Spinner) v.findViewById(R.id.photos_per_page_spinner);
        spinner.setAdapter(adapter);
        // заголовок
        spinner.setPrompt("Photos on page");
        // выделяем элемент
        spinner.setSelection(getPhotosPerPageSettingPosition());
//        spinner.setSelection(4);
        // устанавливаем обработчик нажатия
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                int amountOfPhotosPerPage = Integer.valueOf(mPhotosPerPageAmountBuffer[position]);
                mPhotoGallerySettings.saveAmountOfPhotosInPage(amountOfPhotosPerPage);
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void createPhotosPerRowSpinnerView(View v){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                mPhotosPerRowAmountBuffer);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner spinner = (Spinner) v.findViewById(R.id.photos_per_row_spinner);
        spinner.setAdapter(adapter);
        // заголовок
        spinner.setPrompt("Photos in row");
        // выделяем элемент
        spinner.setSelection(getPhotosPerRowSettingPosition());
//        spinner.setSelection(2);
        // устанавливаем обработчик нажатия
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                int amountOfPhotosPerRow = Integer.valueOf(mPhotosPerRowAmountBuffer[position]);
                mPhotoGallerySettings.saveAmountOfPhotosInRow(amountOfPhotosPerRow);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void createPollEnableCheckBoxView(View v){
        CheckBox checkBox = (CheckBox) v.findViewById(R.id.polling_enabled_checkbox);
        checkBox.setChecked(getPolligEnabled());
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    mPhotoGallerySettings.savePollingEnabled(true);
                }
                else{
                    mPhotoGallerySettings.savePollingEnabled(false);
                }
            }
        });
    }
//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        super.onCreateOptionsMenu(menu, inflater);
//    }

    int getPhotosPerPageSettingPosition()
    {
        int photosPerPage = mPhotoGallerySettings.getAmountOfPhotosInPage();
        Integer position = findMatchInStringArray(String.valueOf(photosPerPage),
                                                  mPhotosPerPageAmountBuffer);
        if (position == null) {
            return 4;
        }
        return position;
    }

    int getPhotosPerRowSettingPosition()
    {
        int photosPerRow = mPhotoGallerySettings.getAmountOfPhotosInRow();
        Integer position = findMatchInStringArray(String.valueOf(photosPerRow),
                                                  mPhotosPerRowAmountBuffer);
        if (position == null) {
            return 2;
        }
        return position;
    }

    @Nullable
    Integer findMatchInStringArray(String template, String [] array){
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(template)) {
                return i;
            }
        }
        return null;
    }

    boolean getPolligEnabled(){
        return mPhotoGallerySettings.isPollingEnabled();
    }
}
