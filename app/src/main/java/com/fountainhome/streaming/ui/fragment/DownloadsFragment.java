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
import com.fountainhome.streaming.ui.WatchActivity;
import java.io.File;
import java.util.List;
public class DownloadsFragment extends Fragment {
    private FragmentDownloadsBinding b;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentDownloadsBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){super.onViewCreated(v,s);b.backBtn.setOnClickListener(x->requireActivity().onBackPressed());AppDatabase.get(requireContext()).downloadDao().getCompleted().observe(getViewLifecycleOwner(),items->{if(items==null||items.isEmpty()){b.emptyText.setVisibility(View.VISIBLE);b.downloadsRv.setVisibility(View.GONE);}else{b.emptyText.setVisibility(View.GONE);b.downloadsRv.setVisibility(View.VISIBLE);b.downloadsRv.setLayoutManager(new LinearLayoutManager(getContext()));b.downloadsRv.setAdapter(new Adapter(items));}});}
    class Adapter extends RecyclerView.Adapter<Adapter.VH>{
        List<DownloadItem>items;Adapter(List<DownloadItem>i){items=i;}
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p,int t){LinearLayout row=new LinearLayout(p.getContext());row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(32,24,32,24);row.setBackgroundColor(0xFF141414);TextView tv=new TextView(p.getContext());tv.setTextColor(0xFFFFFFFF);tv.setTextSize(14);tv.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));TextView del=new TextView(p.getContext());del.setText("✕");del.setTextColor(0xFFCF6679);del.setTextSize(16);del.setPadding(16,0,0,0);row.addView(tv);row.addView(del);return new VH(row,tv,del);}
        @Override public void onBindViewHolder(@NonNull VH h,int pos){DownloadItem item=items.get(pos);String label=item.title+(item.season>0?" S"+item.season+"E"+item.episode:"");boolean playable=item.localPath!=null&&new File(item.localPath).exists();h.tv.setText((playable?"✓  ":"⬇  ")+label);h.tv.setTextColor(playable?0xFFFFFFFF:0xFF888888);h.tv.setOnClickListener(v->{if(playable){try{Intent intent=new Intent(Intent.ACTION_VIEW);intent.setDataAndType(Uri.parse("file://"+item.localPath),"video/*");intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(intent);}catch(Exception e){Toast.makeText(getContext(),"No video player found",Toast.LENGTH_SHORT).show();}}else{Intent i=new Intent(getContext(),WatchActivity.class);i.putExtra("type",item.type);i.putExtra("id",item.tmdbId);startActivity(i);}});h.del.setOnClickListener(v->{DownloadManager2.deleteDownload(requireContext(),item);Toast.makeText(getContext(),"Deleted",Toast.LENGTH_SHORT).show();});}
        @Override public int getItemCount(){return items.size();}
        class VH extends RecyclerView.ViewHolder{TextView tv,del;VH(View root,TextView t,TextView d){super(root);tv=t;del=d;}}
    }
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
