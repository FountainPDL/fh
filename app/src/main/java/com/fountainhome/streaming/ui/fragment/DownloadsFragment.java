package com.fountainhome.streaming.ui.fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;
import com.fountainhome.streaming.databinding.FragmentDownloadsBinding;
import com.fountainhome.streaming.download.*;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.ui.WatchActivity;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
public class DownloadsFragment extends Fragment {
    private FragmentDownloadsBinding b;
    private List<DownloadItem> allItems = new ArrayList<>();
    private String currentTab = "offline";

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentDownloadsBinding.inflate(i, c, false); return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        b.backBtn.setOnClickListener(x -> requireActivity().onBackPressed());
        b.tabOffline.setOnClickListener(x -> { currentTab = "offline"; refreshList(); setTab(0); });
        b.tabDownloading.setOnClickListener(x -> { currentTab = "downloading"; refreshList(); setTab(1); });
        b.tabDownloaded.setOnClickListener(x -> { currentTab = "downloaded"; refreshList(); setTab(2); });
        setTab(0);
        AppDatabase.get(requireContext()).downloadDao().getAll()
            .observe(getViewLifecycleOwner(), items -> {
                allItems = items != null ? items : new ArrayList<>();
                refreshList();
            });
    }
    private void setTab(int a) {
        if (b == null) return;
        int ac = AppPreferences.getAccentColor(requireContext()), gr = 0xFF888888;
        b.tabOffline.setTextColor(a == 0 ? ac : gr);
        b.tabDownloading.setTextColor(a == 1 ? ac : gr);
        b.tabDownloaded.setTextColor(a == 2 ? ac : gr);
    }
    private void refreshList() {
        if (b == null) return;
        List<DownloadItem> filtered;
        switch (currentTab) {
            case "downloading":
                filtered = allItems.stream().filter(i -> "downloading".equals(i.status)).collect(Collectors.toList());
                break;
            case "downloaded":
                filtered = allItems.stream().filter(i -> "completed".equals(i.status) && i.localPath != null && new File(i.localPath).exists()).collect(Collectors.toList());
                break;
            default: // offline = saved metadata, not a video file
                filtered = allItems.stream().filter(i -> !("completed".equals(i.status) && i.localPath != null && new File(i.localPath).exists()) && !"downloading".equals(i.status)).collect(Collectors.toList());
                break;
        }
        b.emptyText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        switch (currentTab) {
            case "downloading": b.emptyText.setText("No active downloads."); break;
            case "downloaded":  b.emptyText.setText("No downloaded videos yet.\nUse the download button on any video."); break;
            default:            b.emptyText.setText("Nothing saved offline yet."); break;
        }
        b.downloadsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        b.downloadsRv.setAdapter(new Adapter(filtered));
    }
    class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        List<DownloadItem> items;
        Adapter(List<DownloadItem> i) { items = i; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            LinearLayout row = new LinearLayout(p.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(32, 24, 32, 24);
            row.setBackgroundColor(0xFF141414);
            TextView icon = new TextView(p.getContext());
            icon.setTextSize(20); icon.setPadding(0, 0, 16, 0);
            TextView tv = new TextView(p.getContext());
            tv.setTextColor(0xFFFFFFFF); tv.setTextSize(14);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            TextView del = new TextView(p.getContext());
            del.setText("\u2715"); del.setTextColor(0xFFCF6679); del.setTextSize(16); del.setPadding(16, 0, 0, 0);
            row.addView(icon); row.addView(tv); row.addView(del);
            return new VH(row, icon, tv, del);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            DownloadItem item = items.get(pos);
            String label = item.title + (item.season > 0 ? "  S" + item.season + "E" + item.episode : "");
            boolean playable = item.localPath != null && new File(item.localPath).exists();
            h.icon.setText(playable ? "\u25b6" : (item.status != null && item.status.equals("downloading") ? "\u23ec" : "\ud83d\udce5"));
            h.tv.setText(label);
            h.tv.setTextColor(playable ? 0xFFFFFFFF : 0xFFAAAAAA);
            h.tv.setOnClickListener(v -> {
                if (playable) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse("file://" + item.localPath), "video/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    } catch (Exception e) { Toast.makeText(getContext(), "No video player found", Toast.LENGTH_SHORT).show(); }
                } else {
                    Intent i = new Intent(getContext(), WatchActivity.class);
                    i.putExtra("type", item.type); i.putExtra("id", item.tmdbId); startActivity(i);
                }
            });
            h.del.setOnClickListener(v -> {
                DownloadManager2.deleteDownload(requireContext(), item);
                Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
            });
        }
        @Override public int getItemCount() { return items.size(); }
        class VH extends RecyclerView.ViewHolder { TextView icon, tv, del; VH(View r, TextView i, TextView t, TextView d) { super(r); icon=i; tv=t; del=d; } }
    }
    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
