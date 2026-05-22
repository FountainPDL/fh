package com.fountainhome.streaming.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.ActivityAnimeDetailBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.ui.player.PlayerActivity;

public class AnimeDetailActivity extends AppCompatActivity {

    private ActivityAnimeDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPreferences.applyTheme(this);
        super.onCreate(savedInstanceState);
        binding = ActivityAnimeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int animeId      = getIntent().getIntExtra("anime_id", 0);
        String title     = getIntent().getStringExtra("title");
        String cover     = getIntent().getStringExtra("cover");
        String banner    = getIntent().getStringExtra("banner");
        String desc      = getIntent().getStringExtra("description");
        int episodes     = getIntent().getIntExtra("episodes", 0);
        double rating    = getIntent().getDoubleExtra("rating", 0);

        binding.titleText.setText(title);
        binding.ratingText.setText(episodes > 0
            ? "★" + String.format("%.1f", rating) + "  ·  " + episodes + " episodes"
            : "★" + String.format("%.1f", rating));

        if (desc != null) {
            binding.descText.setText(Html.fromHtml(desc, Html.FROM_HTML_MODE_COMPACT));
        }

        Glide.with(this).load(banner != null ? banner : cover)
            .centerCrop().into(binding.backdropImage);
        Glide.with(this).load(cover)
            .centerCrop().into(binding.posterImage);

        binding.backBtn.setOnClickListener(v -> finish());

        // Watch via AniList streaming URLs (cr, hulu, etc.) or embed
        binding.watchBtn.setOnClickListener(v -> {
            // For anime we use external streaming via gogoanime embed
            // Build a gogoanime-style embed URL
            String searchTitle = title != null ? title.replace(" ", "-").toLowerCase() : "anime";
            String embedUrl = "https://gogoanime.tel/category/" + searchTitle;
            Intent i = new Intent(this, PlayerActivity.class);
            i.putExtra("type", "anime");
            i.putExtra("id", animeId);
            i.putExtra("title", title);
            i.putExtra("imdbId", "");
            // Use a working anime stream embed
            i.putExtra("anime_embed", "https://consumet-api.vercel.app/anime/gogoanime/episodes/" + searchTitle);
            startActivity(i);
        });
    }
}
