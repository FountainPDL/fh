package com.fountainhome.streaming.ui;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.fountainhome.streaming.CrashLogger;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.service.AppPreferences;
import com.fountainhome.streaming.ui.fragment.*;
public class MainActivity extends AppCompatActivity {
    private int selectedTab=0;
    private int accentColor;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String prev=CrashLogger.read(this);
        if(!prev.equals("No crash log yet.")){
            ScrollView sv=new ScrollView(this);
            LinearLayout ll=new LinearLayout(this);ll.setOrientation(LinearLayout.VERTICAL);ll.setBackgroundColor(0xFF000000);
            Button btn=new Button(this);btn.setText("DISMISS - OPEN APP");
            btn.setOnClickListener(v->{try{new java.io.File(getExternalFilesDir(null),"crash_log.txt").delete();}catch(Exception ignored){}launch(savedInstanceState);sv.setVisibility(View.GONE);});
            ll.addView(btn);
            TextView tv=new TextView(this);tv.setPadding(24,16,24,24);tv.setTextSize(10);tv.setTextColor(0xFFFF6666);tv.setText(prev);
            ll.addView(tv);sv.addView(ll);setContentView(sv);return;
        }
        launch(savedInstanceState);
    }
    private void launch(Bundle s){
        try{setContentView(R.layout.activity_main);}catch(Throwable t){TextView tv=new TextView(this);tv.setText("Layout error: "+t.getMessage());tv.setTextColor(Color.RED);tv.setPadding(32,80,32,32);setContentView(tv);return;}
        accentColor=AppPreferences.getAccentColor(this);
        findViewById(R.id.nav_home).setOnClickListener(v->selectTab(0));
        findViewById(R.id.nav_movies).setOnClickListener(v->selectTab(1));
        findViewById(R.id.nav_tv).setOnClickListener(v->selectTab(2));
        findViewById(R.id.nav_anime).setOnClickListener(v->selectTab(3));
        findViewById(R.id.nav_more).setOnClickListener(v->selectTab(4));
        if(s==null)selectTab(0);else applyTabColors(selectedTab);
    }
    public void selectTab(int tab){
        selectedTab=tab;
        Fragment f;
        switch(tab){case 1:f=new MoviesFragment();break;case 2:f=new TVFragment();break;case 3:f=new AnimeFragment();break;case 4:f=new MoreFragment();break;default:f=new HomeFragment();break;}
        try{getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in,android.R.anim.fade_out).replace(R.id.fragment_container,f).commitAllowingStateLoss();}
        catch(Throwable t){Toast.makeText(this,"Error: "+t.getMessage(),Toast.LENGTH_LONG).show();}
        applyTabColors(tab);
    }
    public void applyAccent(){accentColor=AppPreferences.getAccentColor(this);applyTabColors(selectedTab);}
    private void applyTabColors(int active){
        int[][]tabs={{R.id.icon_home,R.id.label_home},{R.id.icon_movies,R.id.label_movies},{R.id.icon_tv,R.id.label_tv},{R.id.icon_anime,R.id.label_anime},{R.id.icon_more,R.id.label_more}};
        for(int i=0;i<tabs.length;i++){int color=(i==active)?accentColor:0xFF888888;try{((android.widget.ImageView)findViewById(tabs[i][0])).setColorFilter(color);((android.widget.TextView)findViewById(tabs[i][1])).setTextColor(color);}catch(Exception ignored){}}
    }
}
