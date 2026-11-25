package com.fountainhome.streaming.ui.fragment;

import com.fountainhome.streaming.ui.theme.ThemeManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.FragmentOfflineBinding;
import com.fountainhome.streaming.databinding.ItemDownloadBinding;
import com.fountainhome.streaming.download.AppDatabase;
import com.fountainhome.streaming.download.DownloadItem;
import com.fountainhome.streaming.download.DownloadManager2;
import com.fountainhome.streaming.player.OfflinePlayerActivity;
import com.fountainhome.streaming.service.OfflineStore;
import com.fountainhome.streaming.ui.WatchActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** More > Offline: downloaded videos (play them) and saved details pages (open them without internet). */
public class OfflineFragment extends Fragment {
    private FragmentOfflineBinding b;
    private boolean showVideos = true;
    private List<DownloadItem> videos = new ArrayList<>();
    private List<OfflineStore.Saved> pages = new ArrayList<>();
    private final Adapter adapter = new Adapter();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentOfflineBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        b.backBtn.setOnClickListener(x -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        b.queueLink.setOnClickListener(x -> requireActivity().getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new DownloadsFragment()).addToBackStack(null).commit());
        b.tabVideos.setOnClickListener(x -> { showVideos = true; render(); });
        b.tabPages.setOnClickListener(x -> { showVideos = false; render(); });
        b.offlineRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.offlineRv.setAdapter(adapter);

        AppDatabase.get(requireContext()).downloadDao().getCompleted()
            .observe(getViewLifecycleOwner(), items -> {
                List<DownloadItem> ok = new ArrayList<>();
                if (items != null) for (DownloadItem d : items)
                    if (d.localPath != null && new File(d.localPath).isFile()) ok.add(d);
                videos = ok;
                render();
            });
    }

    @Override public void onResume() {
        super.onResume();
        reloadPages();
    }

    private void reloadPages() {
        final android.content.Context c = requireContext().getApplicationContext();
        new Thread(() -> {
            List<OfflineStore.Saved> l = OfflineStore.list(c);
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> { pages = l; if (b != null) render(); });
        }).start();
    }

    private void render() {
        if (b == null) return;
        b.tabVideos.setTextColor(showVideos ? ThemeManager.color(requireContext(), R.attr.fhText) : ThemeManager.color(requireContext(), R.attr.fhTextDim));
        b.tabPages.setTextColor(showVideos ? ThemeManager.color(requireContext(), R.attr.fhTextDim) : ThemeManager.color(requireContext(), R.attr.fhText));
        boolean empty = showVideos ? videos.isEmpty() : pages.isEmpty();
        b.emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        b.emptyText.setText(showVideos
            ? "No downloaded videos yet.\nUse the Download button on any movie or episode."
            : "No saved pages yet.\nTap Save on a details page to keep it for offline.");
        adapter.notifyDataSetChanged();
    }

    @Override public void onDestroyView() { super.onDestroyView(); b = null; }

    private void confirmDelete(String what, Runnable action) {
        new AlertDialog.Builder(requireContext())
            .setMessage("Delete " + what + "?")
            .setPositiveButton("Delete", (d, w) -> action.run())
            .setNegativeButton("Cancel", null).show();
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(ItemDownloadBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
        }

        @Override public int getItemCount() { return showVideos ? videos.size() : pages.size(); }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            ItemDownloadBinding x = h.b;
            x.progress.setVisibility(View.GONE);
            x.action1.setVisibility(View.GONE);
            if (showVideos) bindVideo(x, videos.get(pos)); else bindPage(x, pages.get(pos));
        }

        private void bindVideo(ItemDownloadBinding x, DownloadItem d) {
            boolean movie = "movie".equals(d.type);
            x.title.setText(movie ? d.title : d.title + "  ·  S" + d.season + " E" + d.episode);
            long mb = d.totalBytes / (1024 * 1024);
            x.subtitle.setText((movie ? "Movie" : "Episode") + (mb > 0 ? "  ·  " + mb + " MB" : ""));
            Glide.with(x.thumb).load(OfflineStore.image(x.thumb.getContext(),
                d.backdropPath != null ? d.backdropPath : d.posterPath, "w300")).into(x.thumb);
            x.action1.setVisibility(View.VISIBLE);
            x.action1.setImageResource(R.drawable.ic_share);
            x.action1.setContentDescription("Share");
            x.action1.setOnClickListener(v -> com.fountainhome.streaming.download.ShareHelper.shareVideo(v.getContext(), d));
            x.getRoot().setOnClickListener(v -> {
                com.fountainhome.streaming.download.ExternalPlayers.play(v.getContext(), d);
            });
            x.action2.setOnClickListener(v -> confirmDelete("this download",
                () -> DownloadManager2.delete(requireContext().getApplicationContext(), d)));
        }

        private void bindPage(ItemDownloadBinding x, OfflineStore.Saved s) {
            x.title.setText(s.item.displayTitle());
            int eps = 0;
            for (com.fountainhome.streaming.api.Models.SeasonDetail sd : s.seasons.values())
                if (sd != null && sd.episodes != null) eps += sd.episodes.size();
            String y = s.item.year();
            final boolean anime = "anime".equals(s.item.mediaType);
            if (anime && eps == 0) eps = s.item.episodeCount;
            x.subtitle.setText((anime ? "Anime" : "tv".equals(s.item.mediaType) ? "TV Show" : "Movie")
                + (y.isEmpty() ? "" : "  ·  " + y)
                + (eps > 0 ? "  ·  " + eps + " episodes" : ""));
            Glide.with(x.thumb).load(OfflineStore.image(x.thumb.getContext(),
                s.item.backdropPath != null ? s.item.backdropPath : s.item.posterPath, "w300")).into(x.thumb);
            x.getRoot().setOnClickListener(v -> {
                Intent i;
                if (anime) {
                    i = new Intent(v.getContext(), com.fountainhome.streaming.ui.AnimeDetailActivity.class);
                    i.putExtra("anime_id", s.item.id);
                    i.putExtra("title", s.item.displayTitle());
                    i.putExtra("cover", s.item.posterPath);
                    i.putExtra("banner", s.item.backdropPath);
                    i.putExtra("description", s.item.overview);
                    i.putExtra("episodes", s.item.episodeCount);
                    i.putExtra("rating", s.item.rating);
                    i.putExtra("status", s.item.animeStatus);
                    i.putExtra("format", s.item.animeFormat);
                    i.putExtra("romaji", s.item.romajiTitle);
                    i.putExtra("offline", true);
                } else {
                    i = new Intent(v.getContext(), WatchActivity.class);
                    i.putExtra("type", s.item.mediaType);
                    i.putExtra("id", s.item.id);
                }
                startActivity(i);
            });
            x.action2.setOnClickListener(v -> confirmDelete("this saved page", () -> {
                final android.content.Context c = requireContext().getApplicationContext();
                OfflineStore.remove(c, s.item.mediaType, s.item.id);
                reloadPages();
            }));
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemDownloadBinding b;
            VH(ItemDownloadBinding b) { super(b.getRoot()); this.b = b; }
        }
    }
}
