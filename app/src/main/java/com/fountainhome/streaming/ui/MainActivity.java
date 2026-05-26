package com.fountainhome.streaming.ui;
import android.content.res.ColorStateList;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.ActivityMainBinding;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.ui.fragment.*;
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding b;
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        b=ActivityMainBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
        applyAccent();
        if(s==null){load(new HomeFragment(),"home");b.bottomNav.setSelectedItemId(R.id.nav_home);}
        b.bottomNav.setOnItemSelectedListener(item->{int id=item.getItemId();if(id==R.id.nav_home){load(new HomeFragment(),"home");return true;}if(id==R.id.nav_movies){load(new MoviesFragment(),"movies");return true;}if(id==R.id.nav_tv){load(new TVFragment(),"tv");return true;}if(id==R.id.nav_anime){load(new AnimeFragment(),"anime");return true;}if(id==R.id.nav_more){load(new MoreFragment(),"more");return true;}return false;});
    }
    public void applyAccent(){int accent=AppPreferences.getAccentColor(this);ColorStateList csl=new ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{accent,0xFF666666});b.bottomNav.setItemIconTintList(csl);b.bottomNav.setItemTextColor(csl);b.bottomNav.setBackgroundColor(AppPreferences.getSurfaceColor(this));}
    private void load(Fragment f,String tag){getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in,android.R.anim.fade_out).replace(R.id.fragment_container,f,tag).commitAllowingStateLoss();}
}
