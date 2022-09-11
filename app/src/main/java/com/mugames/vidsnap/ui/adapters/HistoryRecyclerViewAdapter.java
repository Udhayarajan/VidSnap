/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mugames.vidsnap.ui.adapters;

import static com.mugames.vidsnap.utility.UtilityClass.formatFileSize;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.database.History;
import com.mugames.vidsnap.database.HistoryDatabase;
import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.ui.activities.MainActivity;
import com.mugames.vidsnap.ui.viewmodels.HistoryViewModel;
import com.mugames.vidsnap.utility.Statics;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;


public class HistoryRecyclerViewAdapter extends ListAdapter<History, HistoryRecyclerViewAdapter.ViewHolder> {

    String TAG = Statics.TAG + ":HistoryRecyclerViewAdapter";

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
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull @NotNull History oldItem, @NonNull @NotNull History newItem) {
            return oldItem.equals(newItem);
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

    private boolean isNotImage(History currentHistory) {
        return !(currentHistory.fileType.equals("jpeg") || currentHistory.fileType.equals("jpg")) && !currentHistory.fileType.equals("png");
    }

    @Override
    public void onBindViewHolder(@NotNull final ViewHolder holder, int position) {
//        holder.details = formats.get(position);


        History currentHistory = getItem(position);
        if (currentHistory.fileType.contains("."))
            holder.name.setText(currentHistory.fileName + currentHistory.fileType);
        else
            holder.name.setText(currentHistory.fileName + "." + currentHistory.fileType);
        holder.src.setText(currentHistory.source);
        holder.date.setText(currentHistory.getDate());
        holder.size.setText(formatFileSize(Long.parseLong(currentHistory.size), false));
        holder.cardView.setVisibility(View.GONE);
        if (isNotImage(currentHistory)) {
            holder.cardView.setVisibility(View.VISIBLE);
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                //use one of overloaded setDataSource() functions to set your data source
                retriever.setDataSource(holder.date.getContext(), Uri.parse(currentHistory.uriString));
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long timeInMillisec = Long.parseLong(time);
                retriever.release();
                holder.duration.setText(String.format("%d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(timeInMillisec),
                        TimeUnit.MILLISECONDS.toSeconds(timeInMillisec) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInMillisec))
                ));
                holder.duration.setTextSize(12);
            } catch (IllegalArgumentException e) {
                holder.duration.setText("-NA-");
                // File not found
            }
        }


        Glide.with(holder.thumbnail.getContext()).asBitmap()
                .load(Base64.decode(currentHistory.image, Base64.DEFAULT))
                .into(holder.thumbnail);


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
                                intent.setDataAndType(currentHistory.getUri(), MimeTypeMap.getSingleton().getMimeTypeFromExtension(currentHistory.fileType));
                                activity.startActivity(Intent.createChooser(intent, "Select player"));
                                return true;
                            case R.id.menu_share:
                                intent = new Intent(Intent.ACTION_SEND);
                                intent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(currentHistory.fileType));
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                intent.putExtra(Intent.EXTRA_STREAM, currentHistory.getUri());
                                activity.startActivity(Intent.createChooser(intent, "Select Social Media"));
                                return true;
                            case R.id.menu_original_url:
                                if (currentHistory.sourceUrl == null) {
                                    Toast.makeText(activity.getApplicationContext(), "No URL Found :)", Toast.LENGTH_SHORT).show();
                                    return true;
                                }
                                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentHistory.sourceUrl));
                                activity.startActivity(intent);
                                return true;
                            case R.id.menu_remove_from_list:
                                new Thread(() -> HistoryDatabase.getInstance(activity.getApplicationContext()).historyDao().removeItem(currentHistory)).start();
                                return true;
                            case R.id.menu_delete:
                                deleteThis(currentHistory);
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

    private void deleteThis(History currentHistory) {
        if (!FileUtil.isFileExistsWithFileProviderUri(
                activity, currentHistory.getUri())) {
            Toast.makeText(activity, "File not found", Toast.LENGTH_SHORT).show();
            new Thread(() -> HistoryDatabase.getInstance(activity).historyDao().removeItem(currentHistory)).start();
            return;
        }
        historyViewModel.deleteThisItem(currentHistory);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView name;
        public final TextView src;
        public final TextView date;
        public final TextView size;
        public final ImageView thumbnail;
        public final TextView duration;
        public final MaterialCardView cardView;


        public ViewHolder(View view) {
            super(view);
            mView = view;
            //mIdView = (TextView) view.findViewById(R.id.item_number);
            name = view.findViewById(R.id.history_name);
            src = view.findViewById(R.id.history_src);
            date = view.findViewById(R.id.history_date);
            size = view.findViewById(R.id.history_size);
            thumbnail = view.findViewById(R.id.history_thumb);
            duration = view.findViewById(R.id.history_duration);
            cardView = view.findViewById(R.id.card_history_duration);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + name.getText() + "'";
        }
    }
}