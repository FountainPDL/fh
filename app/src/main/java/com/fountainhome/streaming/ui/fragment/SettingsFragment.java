package com.fountainhome.streaming.ui.fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainhome.streaming.databinding.FragmentSettingsBinding;
import com.fountainhome.streaming.service.*;
import com.fountainhome.streaming.ui.MainActivity;
import java.io.File;
public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding b;
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentSettingsBinding.inflate(i, c, false); return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        b.backBtn.setOnClickListener(x -> requireActivity().onBackPressed());
        setupAccent(); setupSource(); setupPlayer(); setupGestures(); setupSubtitles();
        setupAnime(); setupDownloads(); setupUi(); setupStorage();
    }
    private void setupAccent() {
        String[][] cols = {{AppPreferences.COLOR_PURPLE},{AppPreferences.COLOR_BLUE},{AppPreferences.COLOR_RED},{AppPreferences.COLOR_GREEN},{AppPreferences.COLOR_ORANGE},{AppPreferences.COLOR_PINK},{AppPreferences.COLOR_TEAL}};
        View[] sw = {b.colorPurple, b.colorBlue, b.colorRed, b.colorGreen, b.colorOrange, b.colorPink, b.colorTeal};
        String cur = AppPreferences.getAccent(requireContext());
        for (int i = 0; i < sw.length; i++) {
            final String hex = cols[i][0];
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(hex));
            sw[i].setBackground(gd);
            boolean sel = hex.equalsIgnoreCase(cur);
            sw[i].setAlpha(sel ? 1f : 0.4f); sw[i].setScaleX(sel ? 1.25f : 1f); sw[i].setScaleY(sel ? 1.25f : 1f);
            sw[i].setOnClickListener(vv -> {
                AppPreferences.setAccent(requireContext(), hex);
                for (View s2 : sw) { s2.setAlpha(0.4f); s2.setScaleX(1f); s2.setScaleY(1f); }
                vv.setAlpha(1f); vv.setScaleX(1.25f); vv.setScaleY(1.25f);
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).applyAccent();
                Toast.makeText(getContext(), "Accent updated", Toast.LENGTH_SHORT).show();
            });
        }
    }
    private void setupSource() {
        String[] srcs = {"VidSrc", "2Embed", "AutoEmbed", "SuperEmbed VIP"};
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, srcs);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.sourceSpinner.setAdapter(a);
        String cur = AppPreferences.getSource(requireContext());
        for (int i = 0; i < srcs.length; i++) if (srcs[i].equals(cur)) { b.sourceSpinner.setSelection(i); break; }
        b.sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setSource(requireContext(), srcs[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupPlayer() {
        b.autoplaySwitch.setChecked(AppPreferences.getAutoplay(requireContext()));
        b.autoplaySwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setAutoplay(requireContext(), c));
        b.pipSwitch.setChecked(AppPreferences.getPiP(requireContext()));
        b.pipSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setPiP(requireContext(), c));
        b.hwAccelSwitch.setChecked(AppPreferences.getHwAccel(requireContext()));
        b.hwAccelSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setHwAccel(requireContext(), c));
        b.keepScreenSwitch.setChecked(AppPreferences.getKeepScreenOn(requireContext()));
        b.keepScreenSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setKeepScreenOn(requireContext(), c));
        String[] speeds = {"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x"};
        float[] speedVals = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        ArrayAdapter<String> sp = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, speeds);
        sp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.speedSpinner.setAdapter(sp);
        float cur = AppPreferences.getPlaybackSpeed(requireContext());
        for (int i = 0; i < speedVals.length; i++) if (speedVals[i] == cur) { b.speedSpinner.setSelection(i); break; }
        b.speedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setPlaybackSpeed(requireContext(), speedVals[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupGestures() {
        b.gestureSwitch.setChecked(AppPreferences.getGestureControls(requireContext()));
        b.gestureSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setGestureControls(requireContext(), c));
        String[] dt = {"5s", "10s", "15s", "30s"};
        int[] dtVals = {5, 10, 15, 30};
        ArrayAdapter<String> dta = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, dt);
        dta.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.dtSeekSpinner.setAdapter(dta);
        int curDt = AppPreferences.getDoubleTapSeek(requireContext());
        for (int i = 0; i < dtVals.length; i++) if (dtVals[i] == curDt) { b.dtSeekSpinner.setSelection(i); break; }
        b.dtSeekSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setDoubleTapSeek(requireContext(), dtVals[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        b.showSkipIntroSwitch.setChecked(AppPreferences.getShowSkipIntro(requireContext()));
        b.showSkipIntroSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setShowSkipIntro(requireContext(), c));
        b.autoSkipIntroSwitch.setChecked(AppPreferences.getAutoSkipIntro(requireContext()));
        b.autoSkipIntroSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setAutoSkipIntro(requireContext(), c));
        b.bgPlaybackSwitch.setChecked(AppPreferences.getBackgroundPlayback(requireContext()));
        b.bgPlaybackSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setBackgroundPlayback(requireContext(), c));
    }
    private void setupSubtitles() {
        b.subEnabledSwitch.setChecked(AppPreferences.getSubEnabled(requireContext()));
        b.subEnabledSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setSubEnabled(requireContext(), c));
        String[] langs = {"English", "Spanish", "French", "German", "Japanese", "Korean", "Arabic"};
        String[] codes = {"en", "es", "fr", "de", "ja", "ko", "ar"};
        ArrayAdapter<String> la = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, langs);
        la.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.subLangSpinner.setAdapter(la);
        String cl = AppPreferences.getSubLang(requireContext());
        for (int i = 0; i < codes.length; i++) if (codes[i].equals(cl)) { b.subLangSpinner.setSelection(i); break; }
        b.subLangSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setSubLang(requireContext(), codes[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupAnime() {
        String[] ds = {"Sub (Subbed)", "Dub (Dubbed)"};
        ArrayAdapter<String> da = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, ds);
        da.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.dubSubSpinner.setAdapter(da);
        b.dubSubSpinner.setSelection("dub".equals(AppPreferences.getAnimeDubSub(requireContext())) ? 1 : 0);
        b.dubSubSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setAnimeDubSub(requireContext(), pos == 1 ? "dub" : "sub"); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupDownloads() {
        b.wifiOnlySwitch.setChecked(AppPreferences.getWifiOnly(requireContext()));
        b.wifiOnlySwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setWifiOnly(requireContext(), c));
        b.dlSubSwitch.setChecked(AppPreferences.getSubWithDownload(requireContext()));
        b.dlSubSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setSubWithDownload(requireContext(), c));
        String[] qs = {"360p", "480p", "720p", "1080p"};
        ArrayAdapter<String> qa = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, qs);
        qa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.qualitySpinner.setAdapter(qa);
        String cq = AppPreferences.getDlQuality(requireContext());
        for (int i = 0; i < qs.length; i++) if (qs[i].equals(cq)) { b.qualitySpinner.setSelection(i); break; }
        b.qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { AppPreferences.setDlQuality(requireContext(), qs[pos]); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
    }
    private void setupUi() {
        b.showRatingSwitch.setChecked(AppPreferences.getShowRating(requireContext()));
        b.showRatingSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setShowRating(requireContext(), c));
        b.showContinueSwitch.setChecked(AppPreferences.getShowContinue(requireContext()));
        b.showContinueSwitch.setOnCheckedChangeListener((v, c) -> AppPreferences.setShowContinue(requireContext(), c));
    }
    private void setupStorage() {
        long sz = ds(requireContext().getCacheDir());
        b.storageText.setText("Cache: " + fmt(sz));
        b.clearCacheBtn.setOnClickListener(v -> { cd(requireContext().getCacheDir()); b.storageText.setText("Cache: 0 B"); Toast.makeText(getContext(), "Cache cleared", Toast.LENGTH_SHORT).show(); });
        b.clearHistoryBtn.setOnClickListener(v -> { LibraryManager.clearList(requireContext(), LibraryManager.CONTINUE); Toast.makeText(getContext(), "History cleared", Toast.LENGTH_SHORT).show(); });
        b.clearAllBtn.setOnClickListener(v -> { LibraryManager.clearList(requireContext(), LibraryManager.FAVORITES); LibraryManager.clearList(requireContext(), LibraryManager.WATCHLIST); LibraryManager.clearList(requireContext(), LibraryManager.CONTINUE); AppPreferences.clearAll(requireContext()); Toast.makeText(getContext(), "All cleared", Toast.LENGTH_SHORT).show(); });
    }
    private void cd(File d) { if (d!=null&&d.isDirectory()){File[]fs=d.listFiles();if(fs!=null)for(File f:fs)cd(f);}if(d!=null)d.delete(); }
    private long ds(File d) { long s=0;if(d!=null&&d.isDirectory()){File[]fs=d.listFiles();if(fs!=null)for(File f:fs)s+=f.length();}return s; }
    private String fmt(long bv) { if(bv<1024)return bv+" B";if(bv<1048576)return(bv/1024)+" KB";return(bv/1048576)+" MB"; }
    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
