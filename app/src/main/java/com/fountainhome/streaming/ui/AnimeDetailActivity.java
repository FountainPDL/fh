package com.fountainhome.streaming.ui;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.fountainhome.streaming.databinding.ActivityAnimeDetailBinding;
import com.fountainhome.streaming.ui.player.PlayerActivity;
public class AnimeDetailActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        ActivityAnimeDetailBinding b=ActivityAnimeDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        String title=getIntent().getStringExtra("title"),cover=getIntent().getStringExtra("cover"),banner=getIntent().getStringExtra("banner"),desc=getIntent().getStringExtra("description");
        int eps=getIntent().getIntExtra("episodes",0);double rating=getIntent().getDoubleExtra("rating",0);
        b.titleText.setText(title);
        b.ratingText.setText((rating>0?String.format("★%.1f",rating):"")+(eps>0?"  ·  "+eps+" eps":""));
        if(desc!=null)b.descText.setText(Html.fromHtml(desc,Html.FROM_HTML_MODE_COMPACT));
        Glide.with(this).load(banner!=null&&!banner.isEmpty()?banner:cover).centerCrop().into(b.backdropImage);
        Glide.with(this).load(cover).centerCrop().into(b.posterImage);
        b.backBtn.setOnClickListener(v->finish());
        b.watchBtn.setOnClickListener(v->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra("type","anime");i.putExtra("id",0);i.putExtra("title",title);i.putExtra("imdbId","");startActivity(i);});
    }
}
