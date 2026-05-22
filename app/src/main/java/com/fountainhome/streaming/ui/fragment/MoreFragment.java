// FountainHome MoreFragment
package com.fountainhome.streaming.ui.fragment;
import com.fountainhome.streaming.ui.MainActivity;

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
        highlightTheme(t);

        binding.themeDark.setOnClickListener(v -> {
            AppPreferences.setTheme(requireContext(), AppPreferences.THEME_DARK);
            highlightTheme(AppPreferences.THEME_DARK);
            applyBackgroundFromTheme(AppPreferences.THEME_DARK);
            // Recreate parent activity to fully apply
            requireActivity().recreate();
        });

        binding.themeAmoled.setOnClickListener(v -> {
            AppPreferences.setTheme(requireContext(), AppPreferences.THEME_AMOLED);
            highlightTheme(AppPreferences.THEME_AMOLED);
            applyBackgroundFromTheme(AppPreferences.THEME_AMOLED);
            requireActivity().recreate();
        });

        binding.themeLight.setOnClickListener(v -> {
            AppPreferences.setTheme(requireContext(), AppPreferences.THEME_LIGHT);
            highlightTheme(AppPreferences.THEME_LIGHT);
            applyBackgroundFromTheme(AppPreferences.THEME_LIGHT);
            requireActivity().recreate();
        });
    }

    private void highlightTheme(String theme) {
        binding.themeDark.setAlpha(AppPreferences.THEME_DARK.equals(theme) ? 1f : 0.4f);
        binding.themeAmoled.setAlpha(AppPreferences.THEME_AMOLED.equals(theme) ? 1f : 0.4f);
        binding.themeLight.setAlpha(AppPreferences.THEME_LIGHT.equals(theme) ? 1f : 0.4f);

        // Show selected outline
        int accent = AppPreferences.getAccentColor(requireContext());
        binding.themeDark.setBackgroundColor(
            AppPreferences.THEME_DARK.equals(theme) ? darken(accent) : Color.parseColor("#1A1A1A"));
        binding.themeAmoled.setBackgroundColor(
            AppPreferences.THEME_AMOLED.equals(theme) ? darken(accent) : Color.BLACK);
        binding.themeLight.setBackgroundColor(
            AppPreferences.THEME_LIGHT.equals(theme) ? lighten(accent) : Color.parseColor("#F0F0F0"));
    }

    private void applyBackgroundFromTheme(String theme) {
        int bg = AppPreferences.getBackgroundColor(requireContext());
        if (binding.getRoot().getRootView() != null) {
            binding.getRoot().getRootView().setBackgroundColor(bg);
        }
    }

    private void setupAccent() {
        String[][] colors = {
            {AppPreferences.COLOR_GREEN,  "Green"},
            {AppPreferences.COLOR_PURPLE, "Purple"},
            {AppPreferences.COLOR_BLUE,   "Blue"},
            {AppPreferences.COLOR_RED,    "Red"},
            {AppPreferences.COLOR_ORANGE, "Orange"},
            {AppPreferences.COLOR_PINK,   "Pink"},
            {AppPreferences.COLOR_TEAL,   "Teal"},
        };
        View[] swatches = {
            binding.colorPurple,  // reused for green (first slot)
            binding.colorBlue,
            binding.colorRed,
            binding.colorGreen,
            binding.colorOrange,
            binding.colorPink,
            binding.colorTeal
        };

        String cur = AppPreferences.getAccent(requireContext());

        for (int i = 0; i < swatches.length && i < colors.length; i++) {
            final String hex = colors[i][0];
            final View swatch = swatches[i];
            swatch.setBackgroundColor(Color.parseColor(hex));

            // Show selected state with border effect
            boolean isSelected = hex.equalsIgnoreCase(cur);
            swatch.setAlpha(isSelected ? 1f : 0.45f);
            swatch.setScaleX(isSelected ? 1.15f : 1f);
            swatch.setScaleY(isSelected ? 1.15f : 1f);

            swatch.setOnClickListener(v -> {
                // Save
                AppPreferences.setAccent(requireContext(), hex);

                // Instantly update all swatches
                for (View sw : swatches) {
                    sw.setAlpha(0.45f);
                    sw.setScaleX(1f);
                    sw.setScaleY(1f);
                }
                v.setAlpha(1f);
                v.setScaleX(1.15f);
                v.setScaleY(1.15f);

                // Apply to bottom nav immediately
                if (getActivity() != null) {
                    View bnv = getActivity().findViewById(
                        com.fountainhome.streaming.R.id.bottom_nav);
                    if (bnv instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                        int newAccent = Color.parseColor(hex);
                        android.content.res.ColorStateList csl =
                            new android.content.res.ColorStateList(
                                new int[][]{
                                    new int[]{android.R.attr.state_checked},
                                    new int[]{}
                                },
                                new int[]{ newAccent, 0xFF666666 }
                            );
                        ((com.google.android.material.bottomnavigation.BottomNavigationView) bnv)
                            .setItemIconTintList(csl);
                        ((com.google.android.material.bottomnavigation.BottomNavigationView) bnv)
                            .setItemTextColor(csl);
                    }
                }

                // Update theme highlights with new accent
                highlightTheme(AppPreferences.getTheme(requireContext()));

                Toast.makeText(getContext(), "Accent color updated", Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).applyAccentToNav();
                }
            });
        }
    }

    private void setupSource() {
        String[] sources = {"VSEmbed", "VidSrc", "2Embed", "AutoEmbed", "MultiEmbed"};
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, sources);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sourceSpinner.setAdapter(a);

        String cur = AppPreferences.getSource(requireContext());
        for (int i = 0; i < sources.length; i++) {
            if (sources[i].equals(cur)) { binding.sourceSpinner.setSelection(i); break; }
        }

        binding.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                AppPreferences.setSource(requireContext(), sources[pos]);
            }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupSwitches() {
        // All switches apply instantly
        binding.autoplaySwitch.setChecked(AppPreferences.getAutoplay(requireContext()));
        binding.autoplaySwitch.setOnCheckedChangeListener((v, c) -> {
            AppPreferences.setAutoplay(requireContext(), c);
        });

        binding.hwAccelSwitch.setChecked(AppPreferences.getHwAccel(requireContext()));
        binding.hwAccelSwitch.setOnCheckedChangeListener((v, c) -> {
            AppPreferences.setHwAccel(requireContext(), c);
        });

        binding.wifiOnlySwitch.setChecked(AppPreferences.getWifiOnly(requireContext()));
        binding.wifiOnlySwitch.setOnCheckedChangeListener((v, c) -> {
            AppPreferences.setWifiOnly(requireContext(), c);
        });

        binding.showContinueSwitch.setChecked(AppPreferences.getShowContinue(requireContext()));
        binding.showContinueSwitch.setOnCheckedChangeListener((v, c) -> {
            AppPreferences.setShowContinue(requireContext(), c);
        });

        String[] langs = {"English","Spanish","French","German","Japanese","Korean","Arabic","Portuguese"};
        String[] codes = {"en","es","fr","de","ja","ko","ar","pt"};
        ArrayAdapter<String> la = new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_spinner_item, langs);
        la.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.subLangSpinner.setAdapter(la);

        String curL = AppPreferences.getSubLang(requireContext());
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(curL)) { binding.subLangSpinner.setSelection(i); break; }
        }
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
        for (int i = 0; i < quals.length; i++) {
            if (quals[i].equals(curQ)) { binding.qualitySpinner.setSelection(i); break; }
        }
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
            Toast.makeText(getContext(), "All data cleared. Restarting...", Toast.LENGTH_SHORT).show();
            requireActivity().recreate();
        });
    }

    private int darken(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.3f;
        return Color.HSVToColor(hsv);
    }

    private int lighten(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] *= 0.3f;
        hsv[2] = Math.min(1f, hsv[2] * 1.5f);
        return Color.HSVToColor(hsv);
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
