package com.fountainhome.streaming.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fountainhome.streaming.ui.theme.ThemeManager;

/** Every activity extends this so it picks up the chosen palette before any view is created. */
public abstract class BaseActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeManager.apply(this);
        super.onCreate(savedInstanceState);
        ThemeManager.styleBars(this);
    }
}
