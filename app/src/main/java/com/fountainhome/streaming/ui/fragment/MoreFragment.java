package com.fountainhome.streaming.ui.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fountainhome.streaming.databinding.FragmentMoreBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;

public class MoreFragment extends Fragment {

    private FragmentMoreBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupTheme();
        setupAccent();
        setupSource();
        setupSwitches();
        setupDownloads();
        setupPrivacy();
    }

    private void setupTheme() {
        String t = AppPreferences.getTheme(requireContext());
        float dA = AppPreferences.THEME_DARK.equals(t) ? 1f : 0.4f;
        float aA = AppPreferences.THEME_AMOLED.equals(t) ? 1f : 0.4f;
        float lA = AppPreferences.THEME_LIGHT.equals(t) ? 1f : 0.4f;
        binding.themeDark.setAlpha(dA);
        binding.themeAmoled.setAlpha(aA);
        binding.themeLight.setAlpha(lA);

        binding.themeDark.setOnClickListener(v -> applyTheme(AppPreferences.THEME_DARK));
        binding.themeAmoled.setOnClickListener(v -> applyTheme(AppPreferences.THEME_AMOLED));
        binding.themeLight.setOnClickListener(v -> applyTheme(AppPreferences.THEME_LIGHT));
    }

    private void applyTheme(String theme) {
        AppPreferences.setTheme(requireContext(), theme);
        Toast.makeText(getContext(), "Theme set. Restart to fully apply.", Toast.LENGTH_SHORT).show();
        binding.themeDark.setAlpha(AppPreferences.THEME_DARK.equals(theme) ? 1f : 0.4f);
        binding.themeAmoled.setAlpha(AppPreferences.THEME_AMOLED.equals(theme) ? 1f : 0.4f);
        binding.themeLight.setAlpha(AppPreferences.THEME_LIGHT.equals(theme) ? 1f : 0.4f);
    }

    private void setupAccent() {
        String[][] colors = {
            {AppPreferences.COLOR_PURPLE,"#BB86FC"},
            {AppPreferences.COLOR_BLUE,"#2196F3"},
            {AppPreferences.COLOR_RED,"#CF6679"},
            {AppPreferences.COLOR_GREEN,"#4CAF50"},
            {AppPreferences.COLOR_ORANGE,"#FF9800"},
            {AppPreferences.COLOR_PINK,"#E91E8C"},
            {AppPreferences.COLOR_TEAL,"#03DAC6"}
        };
        View[] swatches = {
            binding.colorPurple, binding.colorBlue, binding.colorRed,
            binding.colorGreen, binding.colorOrange, binding.colorPink, binding.colorTeal
        };
        String cur = AppPreferences.getAccent(requireContext());
        for (int i = 0; i < swatches.length; i++) {
            final String hex = colors[i][0];
            swatches[i].setBackgroundColor(Color.parseColor(hex));
            swatches[i].setAlpha(hex.equalsIgnoreCase(cur) ? 1f : 0.45f);
            swatches[i].setOnClickListener(v -> {
                AppPreferences.setAccent(requireContext(), hex);
                for (View sw : swatches) sw.setAlpha(0.45f);
                v.setAlpha(1f);
                Toast.makeText(getContext(), "Accent color updated", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupSource() {
        String[] sources = {"VidSrc", "2Embed", "AutoEmbed", "MultiEmbed"};
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, sources);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sourceSpinner.setAdapter(a);
        String cur = AppPreferences.getSource(requireContext());
        for (int i = 0; i < sources.length; i++)
            if (sources[i].equals(cur)) { binding.sourceSpinner.setSelection(i); break; }
        binding.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                AppPreferences.setSource(requireContext(), sources[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupSwitches() {
        binding.autoplaySwitch.setChecked(AppPreferences.getAutoplay(requireContext()));
        binding.autoplaySwitch.setOnCheckedChangeListener((v, c) ->
            AppPreferences.setAutoplay(requireContext(), c));

        binding.hwAccelSwitch.setChecked(AppPreferences.getHwAccel(requireContext()));
        binding.hwAccelSwitch.setOnCheckedChangeListener((v, c) ->
            AppPreferences.setHwAccel(requireContext(), c));

        binding.wifiOnlySwitch.setChecked(AppPreferences.getWifiOnly(requireContext()));
        binding.wifiOnlySwitch.setOnCheckedChangeListener((v, c) ->
            AppPreferences.setWifiOnly(requireContext(), c));

        binding.showContinueSwitch.setChecked(AppPreferences.getShowContinue(requireContext()));
        binding.showContinueSwitch.setOnCheckedChangeListener((v, c) ->
            AppPreferences.setShowContinue(requireContext(), c));

        String[] langs = {"English","Spanish","French","German","Japanese","Korean"};
        String[] codes = {"en","es","fr","de","ja","ko"};
        ArrayAdapter<String> la = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, langs);
        la.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.subLangSpinner.setAdapter(la);
        String curL = AppPreferences.getSubLang(requireContext());
        for (int i = 0; i < codes.length; i++)
            if (codes[i].equals(curL)) { binding.subLangSpinner.setSelection(i); break; }
        binding.subLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                AppPreferences.setSubLang(requireContext(), codes[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupDownloads() {
        String[] quals = {"480p","720p","1080p"};
        ArrayAdapter<String> qa = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, quals);
        qa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.qualitySpinner.setAdapter(qa);
        String curQ = AppPreferences.getDlQuality(requireContext());
        for (int i = 0; i < quals.length; i++)
            if (quals[i].equals(curQ)) { binding.qualitySpinner.setSelection(i); break; }
        binding.qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                AppPreferences.setDlQuality(requireContext(), quals[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupPrivacy() {
        long size = getDirSize(requireContext().getCacheDir());
        binding.storageText.setText("Cache: " + formatSize(size));

        binding.clearCacheBtn.setOnClickListener(v -> {
            clearDir(requireContext().getCacheDir());
            binding.storageText.setText("Cache: 0 B");
            Toast.makeText(getContext(), "Cache cleared", Toast.LENGTH_SHORT).show();
        });
        binding.clearHistoryBtn.setOnClickListener(v -> {
            LibraryManager.clearList(requireContext(), LibraryManager.CONTINUE);
            Toast.makeText(getContext(), "Watch history cleared", Toast.LENGTH_SHORT).show();
        });
        binding.clearAllBtn.setOnClickListener(v -> {
            LibraryManager.clearList(requireContext(), LibraryManager.FAVORITES);
            LibraryManager.clearList(requireContext(), LibraryManager.WATCHLIST);
            LibraryManager.clearList(requireContext(), LibraryManager.CONTINUE);
            AppPreferences.clearAll(requireContext());
            Toast.makeText(getContext(), "All data cleared", Toast.LENGTH_SHORT).show();
            setupTheme(); setupAccent(); setupSource(); setupSwitches();
        });
    }

    private void clearDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) for (java.io.File f : files) clearDir(f);
        }
        if (dir != null) dir.delete();
    }
    private long getDirSize(java.io.File dir) {
        long s = 0;
        if (dir != null && dir.isDirectory()) {
            java.io.File[] fs = dir.listFiles();
            if (fs != null) for (java.io.File f : fs) s += f.length();
        }
        return s;
    }
    private String formatSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1048576) return (b/1024) + " KB";
        return (b/1048576) + " MB";
    }

    @Override
    public void onDestroyView() { super.onDestroyView(); binding = null; }
}
