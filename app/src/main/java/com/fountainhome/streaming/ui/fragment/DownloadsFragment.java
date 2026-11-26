package com.fountainhome.streaming.ui.fragment;

import com.fountainhome.streaming.ui.theme.ThemeManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.FragmentDownloadsBinding;
import com.fountainhome.streaming.databinding.ItemDownloadBinding;
import com.fountainhome.streaming.download.AppDatabase;
import com.fountainhome.streaming.download.DownloadItem;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.service.DownloadService;
import com.fountainhome.streaming.service.OfflineStore;

import java.util.ArrayList;
import java.util.List;

/** The download queue: what is downloading now, what is waiting, and what failed. */
public class DownloadsFragment extends Fragment {
    private FragmentDownloadsBinding b;
    private final Adapter adapter = new Adapter();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentDownloadsBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        b.backBtn.setOnClickListener(x -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        b.downloadsRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.downloadsRv.setAdapter(adapter);
        AppDatabase.get(requireContext()).downloadDao().getUnfinished()
            .observe(getViewLifecycleOwner(), items -> {
                List<DownloadItem> list = items != null ? items : new ArrayList<>();
                adapter.set(list);
                b.emptyText.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            });
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }

    private void cancelAndDelete(DownloadItem d) {
        Context c = requireContext().getApplicationContext();
        if (d.isActive()) {
            Intent i = new Intent(c, DownloadService.class);
            i.setAction(DownloadService.ACTION_CANCEL);
            i.putExtra(DownloadService.EXTRA_ID, d.id);
            c.startService(i);
        }
        DownloadManager2.delete(c, d);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private List<DownloadItem> items = new ArrayList<>();
        void set(List<DownloadItem> l) { items = l; notifyDataSetChanged(); }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(ItemDownloadBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            DownloadItem d = items.get(pos);
            ItemDownloadBinding x = h.b;
            x.title.setText("movie".equals(d.type) ? d.title : d.title + "  ·  S" + d.season + " E" + d.episode);
            Glide.with(x.thumb).load(OfflineStore.image(x.thumb.getContext(),
                d.backdropPath != null ? d.backdropPath : d.posterPath, "w300")).into(x.thumb);

            boolean failed = DownloadItem.FAILED.equals(d.status);
            String sub;
            if (failed) sub = "Failed" + (d.error != null && !d.error.isEmpty() ? ": " + d.error : "");
            else if (DownloadItem.PAUSED.equals(d.status)) sub = "Paused" + (d.progress > 0 ? " at " + d.progress + "%" : "")
                + (d.error != null && !d.error.isEmpty() ? " \u2014 " + d.error : "");
            else if (DownloadItem.QUEUED.equals(d.status)) sub = "Waiting…";
            else if (DownloadItem.RESOLVING.equals(d.status)) sub = "Finding a source…";
            else sub = "Downloading  " + d.progress + "%";
            x.subtitle.setText(sub);
            x.subtitle.setTextColor(failed ? ThemeManager.color(x.subtitle.getContext(), R.attr.fhDanger) : ThemeManager.color(x.subtitle.getContext(), R.attr.fhTextDim));

            boolean showBar = DownloadItem.DOWNLOADING.equals(d.status) || DownloadItem.PAUSED.equals(d.status);
            x.progress.setVisibility(showBar ? View.VISIBLE : View.GONE);
            x.progress.setProgress(d.progress);

            if (failed) {
                x.action1.setVisibility(View.VISIBLE);
                x.action1.setImageResource(R.drawable.ic_refresh);
                x.action1.setOnClickListener(v -> {
                    DownloadManager2.retry(v.getContext().getApplicationContext(), d);
                    Toast.makeText(v.getContext(), "Retrying…", Toast.LENGTH_SHORT).show();
                });
            } else if (DownloadItem.PAUSED.equals(d.status)) {
                x.action1.setVisibility(View.VISIBLE);
                x.action1.setImageResource(R.drawable.ic_play);
                x.action1.setContentDescription("Resume");
                x.action1.setOnClickListener(v -> DownloadManager2.resume(v.getContext().getApplicationContext(), d));
            } else {
                x.action1.setVisibility(View.VISIBLE);
                x.action1.setImageResource(R.drawable.ic_pause);
                x.action1.setContentDescription("Pause");
                x.action1.setOnClickListener(v -> DownloadManager2.pause(v.getContext().getApplicationContext(), d));
            }
            x.action2.setOnClickListener(v -> cancelAndDelete(d));
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            final ItemDownloadBinding b;
            VH(ItemDownloadBinding b) { super(b.getRoot()); this.b = b; }
        }
    }
}
