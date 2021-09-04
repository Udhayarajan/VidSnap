package com.mugames.vidsnap.ui.main.Adapters;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.mugames.vidsnap.DataBase.History;
import com.mugames.vidsnap.ViewModels.HistoryViewModel;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Bundles.HistoryDetails;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.Statics;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.mugames.vidsnap.Utility.UtilityClass.formatFileSize;


public class HistoryRecyclerViewAdapter extends ListAdapter<History, HistoryRecyclerViewAdapter.ViewHolder> {

    String TAG = Statics.TAG + ":HistoryRecyclerViewAdapter";

    //    private final ArrayList<HistoryDetails> list;

    MainActivity activity;
    HistoryViewModel historyViewModel;
    LifecycleOwner lifecycleOwner;

    public HistoryRecyclerViewAdapter(LifecycleOwner owner, HistoryViewModel historyViewModel) {
        super(DIFF_CALLBACK);
        lifecycleOwner = owner;
        this.historyViewModel = historyViewModel;
    }

    private static final DiffUtil.ItemCallback<History> DIFF_CALLBACK = new DiffUtil.ItemCallback<History>() {
        @Override
        public boolean areItemsTheSame(@NonNull @NotNull History oldItem, @NonNull @NotNull History newItem) {
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull @NotNull History oldItem, @NonNull @NotNull History newItem) {
            return false;
        }
    };


    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_history_item, parent, false);
        activity = (MainActivity) parent.getContext();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull final ViewHolder holder, int position) {
//        holder.details = list.get(position);


        History currentHistory = getItem(position);
        holder.name.setText(currentHistory.fileName + currentHistory.fileType);
        holder.src.setText(currentHistory.source);
        holder.date.setText(currentHistory.date);
        holder.size.setText(formatFileSize(Long.parseLong(currentHistory.size), false));
        holder.thumbnail.setImageBitmap(currentHistory.getThumbnail());


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(activity, v);
                popupMenu.inflate(R.menu.popup_menu);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    popupMenu.setGravity(Gravity.END);
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent intent;
                        switch (item.getItemId()) {
                            case R.id.menu_play:
                                intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(currentHistory.getUri(), MIMEType.VIDEO_MP4);
                                activity.startActivity(Intent.createChooser(intent, "Select player"));
                                return true;
                            case R.id.menu_share:
                                intent = new Intent(Intent.ACTION_SEND);
                                intent.setType(MIMEType.VIDEO_MP4);
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.putExtra(Intent.EXTRA_STREAM, currentHistory.getUri());
                                activity.startActivity(Intent.createChooser(intent, "Select Social Media"));
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popupMenu.show();
            }
        });


    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView name;
        public final TextView src;
        public final TextView date;
        public final TextView size;
        public final ImageView thumbnail;


        public ViewHolder(View view) {
            super(view);
            mView = view;
            //mIdView = (TextView) view.findViewById(R.id.item_number);
            name = view.findViewById(R.id.history_name);
            src = view.findViewById(R.id.history_src);
            date = view.findViewById(R.id.history_date);
            size = view.findViewById(R.id.history_size);
            thumbnail = view.findViewById(R.id.history_thumb);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + name.getText() + "'";
        }
    }
}