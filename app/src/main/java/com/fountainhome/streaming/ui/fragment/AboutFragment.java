package com.fountainhome.streaming.ui.fragment;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.fountainhome.streaming.databinding.FragmentAboutBinding;
public class AboutFragment extends Fragment {
    private FragmentAboutBinding b;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentAboutBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){super.onViewCreated(v,s);b.backBtn.setOnClickListener(x->requireActivity().onBackPressed());}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
