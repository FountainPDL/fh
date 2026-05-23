package com.fountainhome.streaming.ui.fragment;

import android.content.res.ColorStateList;
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

import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.FragmentMoreBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.service.LibraryManager;
import com.fountainhome.streaming.ui.MainActivity;

public class MoreFragment extends Fragment {

    private FragmentMoreBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyThemeToRoot();
        setupTheme();
        setupAccent();
        setupSource();
        setupAnimeSettings();
        setupSwitches();
        setupDownloads();
        setupPrivacy();
    }

    private void applyThemeToRoot() {
        if (binding == null || getContext() == null) return;
        int bg = AppPreferences.getBackgroundColor(requireContext());
        int surf = AppPreferences.getSurfaceColor(requireContext());
        binding.getRoot().setBackgroundColor(bg);
    }

    private void setupTheme() {
        String t = AppPreferences.getTheme(requireContext());
        updateThemeBtns(t);

        binding.themeDark.setOnClickListener(v -> {
            AppPreferences.setTheme(requireContext(), AppPreferences.THEME_DARK);
            updateThemeBtns(AppPreferences.THEME_DARK);
            requireActivity().recreate();
        });
        binding.themeLight.setOnClickListener(v -> {
            AppPreferences.setTheme(requireContext(), AppPreferences.THEME_LIGHT);
            updateThemeBtns(AppPreferences.THEME_LIGHT);
            requireActivity().recreate();
        });
    }

    private void updateThemeBtns(String theme) {
        int accent = AppPreferences.getAccentColor(requireContext());
        boolean isDark = AppPreferences.THEME_DARK.equals(theme);
        binding.themeDark.setAlpha(isDark ? 1f : 0.5f);
        binding.themeLight.setAlpha(!isDark ? 1f : 0.5f);
        if (isDark) binding.themeDark.setBackgroundColor(darken(accent, 0.3f));
        else binding.themeDark.setBackgroundColor(Color.parseColor("#1F1F1F"));
        if (!isDark) binding.themeLight.setBackgroundColor(lighten(accent, 0.7f));
        else binding.themeLight.setBackgroundColor(Color.parseColor("#EEEEEE"));
    }

    private void setupAccent() {
        String[][] colors = {
            {AppPreferences.COLOR_PURPLE, "purple"},
            {AppPreferences.COLOR_BLUE,   "blue"},
            {AppPreferences.COLOR_RED,    "red"},
            {AppPreferences.COLOR_GREEN,  "green"},
            {AppPreferences.COLOR_ORANGE, "orange"},
            {AppPreferences.COLOR_PINK,   "pink"},
            {AppPreferences.COLOR_TEAL,   "teal"},
        };
        View[] swatches = {
            binding.colorPurple, binding.colorBlue, binding.colorRed,
            binding.colorGreen, binding.colorOrange, binding.colorPink, binding.colorTeal
        };
        String cur = AppPreferences.getAccent(requireContext());
        for (int i = 0; i < swatches.length; i++) {
            final String hex = colors[i][0];
            int c = Color.parseColor(hex);
            swatches[i].setBackgroundColor(c);
            // Rounded corners
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(c);
            gd.setCornerRadius(8);
            swatches[i].setBackground(gd);
            swatches[i].setAlpha(hex.equalsIgnoreCase(cur) ? 1f : 0.45f);
            swatches[i].setScaleX(hex.equalsIgnoreCase(cur) ? 1.2f : 1f);
            swatches[i].setScaleY(hex.equalsIgnoreCase(cur) ? 1.2f : 1f);

            swatches[i].setOnClickListener(v -> {
                AppPreferences.setAccent(requireContext(), hex);
                // Update colors.xml accent at runtime via tinting
                updateAllAccentViews(hex);
                for (View sw : swatches) { sw.setAlpha(0.45f); sw.setScaleX(1f); sw.setScaleY(1f); }
                v.setAlpha(1f); v.setScaleX(1.2f); v.setScaleY(1.2f);
                if (getActivity() instanceof MainActivity)
                    ((MainActivity) getActivity()).applyAccentToNav();
                Toast.makeText(getContext(), "Accent updated", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateAllAccentViews(String hex) {
        if (binding == null) return;
        try {
            int color = Color.parseColor(hex);
            ColorStateList csl = ColorStateList.valueOf(color);
            binding.autoplaySwitch.setTrackTintList(csl);
            binding.pipSwitch.setTrackTintList(csl);
            binding.hwAccelSwitch.setTrackTintList(csl);
            binding.wifiOnlySwitch.setTrackTintList(csl);
            binding.showContinueSwitch.setTrackTintList(csl);
        } catch (Exception ignored) {}
    }

    private void setupSource() {
        String[] sources = {"VidSrc","2Embed","AutoEmbed","SuperEmbed VIP"};
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

    private void setupAnimeSettings() {
        String[] animeSources = {"Gogoanime","AniAPI","Crunchyroll Embed"};
        ArrayAdapter<String> aa = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, animeSources);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.animeSourceSpinner.setAdapter(aa);
        String curA = AppPreferences.getAnimeSource(requireContext());
        for (int i = 0; i < animeSources.length; i++)
            if (animeSources[i].equals(curA)) { binding.animeSourceSpinner.setSelection(i); break; }
        binding.animeSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                AppPreferences.setAnimeSource(requireContext(), animeSources[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });

        String[] dubSub = {"Sub (default)","Dub"};
        ArrayAdapter<String> da = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, dubSub);
        da.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.dubSubSpinner.setAdapter(da);
        String curDS = AppPreferences.getAnimeDubSub(requireContext());
        binding.dubSubSpinner.setSelection("dub".equals(curDS) ? 1 : 0);
        binding.dubSubSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                AppPreferences.setAnimeDubSub(requireContext(), pos == 1 ? "dub" : "sub");
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupSwitches() {
        binding.autoplaySwitch.setChecked(AppPreferences.getAutoplay(requireContext()));
        binding.autoplaySwitch.setOnCheckedChangeListener((v, c) ->
            AppPreferences.setAutoplay(requireContext(), c));

        binding.pipSwitch.setChecked(AppPreferences.getPiP(requireContext()));
        binding.pipSwitch.setOnCheckedChangeListener((v, c) ->
            AppPreferences.setPiP(requireContext(), c));

        binding.hwAccelSwitch.setChecked(AppPreferences.getHwAccel(requireContext()));
        binding.hwAccelSwitch.setOnCheckedChangeListener((v, c) ->
            AppPreferences.setHwAccel(requireContext(), c));

        binding.showContinueSwitch.setChecked(AppPreferences.getShowContinue(requireContext()));
        binding.showContinueSwitch.setOnCheckedChangeListener((v, c) ->
            AppPreferences.setShowContinue(requireContext(), c));

        String[] langs = {"English","Spanish","French","German","Japanese","Korean","Arabic"};
        String[] codes = {"en","es","fr","de","ja","ko","ar"};
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
        binding.wifiOnlySwitch.setChecked(AppPreferences.getWifiOnly(requireContext()));
        binding.wifiOnlySwitch.setOnCheckedChangeListener((v, c) ->
            AppPreferences.setWifiOnly(requireContext(), c));

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
            Toast.makeText(getContext(), "Cleared. Restarting...", Toast.LENGTH_SHORT).show();
            requireActivity().recreate();
        });
    }

    private int darken(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor;
        return Color.HSVToColor(hsv);
    }
    private int lighten(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] *= factor;
        hsv[2] = Math.min(1f, hsv[2] * 1.4f);
        return Color.HSVToColor(hsv);
    }
    private void clearDir(java.io.File d) {
        if (d != null && d.isDirectory()) { java.io.File[] fs = d.listFiles();
            if (fs != null) for (java.io.File f : fs) clearDir(f); }
        if (d != null) d.delete();
    }
    private long getDirSize(java.io.File d) {
        long s = 0;
        if (d != null && d.isDirectory()) { java.io.File[] fs = d.listFiles();
            if (fs != null) for (java.io.File f : fs) s += f.length(); }
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
