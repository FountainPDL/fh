package com.fountainhome.streaming.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.fountainhome.streaming.databinding.ActivityLibraryBinding;

public class LibraryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLibraryBinding binding = ActivityLibraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.backBtn.setOnClickListener(v -> finish());
    }
}
