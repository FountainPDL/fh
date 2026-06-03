package com.fountainhome.streaming.ui.fragment;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainhome.streaming.R;
import com.fountainhome.streaming.databinding.FragmentMoreBinding;
public class MoreFragment extends Fragment {
    private FragmentMoreBinding b;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentMoreBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){super.onViewCreated(v,s);b.navWatchlistPage.setOnClickListener(x->go(new WatchListFragment()));b.navDownloadsPage.setOnClickListener(x->go(new DownloadsFragment()));b.navHistoryPage.setOnClickListener(x->go(new HistoryFragment()));b.navSettingsPage.setOnClickListener(x->go(new SettingsFragment()));b.navAboutPage.setOnClickListener(x->go(new AboutFragment()));}
    private void go(Fragment f){requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.slide_in_left,android.R.anim.fade_out,android.R.anim.fade_in,android.R.anim.slide_out_right).replace(R.id.fragment_container,f).addToBackStack(null).commit();}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
