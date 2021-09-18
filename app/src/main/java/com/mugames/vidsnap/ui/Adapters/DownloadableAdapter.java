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

package com.mugames.vidsnap.ui.Adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Bundles.Formats;

import java.util.ArrayList;

/**
 * For download able fragment
 * Some post in Instagram has edge car so to display thumbnail of video
 * it is recyclerview's java file
 * layout/downloadable.xml
 */
public class DownloadableAdapter extends RecyclerView.Adapter<DownloadableAdapter.ViewHolder> {


    Formats formats;
    Fragment fragment;

    ArrayList<Integer> selected;
    MutableLiveData<ArrayList<Integer>> selectedList;
    public boolean selectionStarted;

    public DownloadableAdapter(Fragment fragment, Formats formats) {
        this.formats = formats;
        this.fragment = fragment;
        selected = new ArrayList<>();
        selectedList = new MutableLiveData<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        fragment.requireActivity().getOnBackPressedDispatcher().addCallback(fragment,onBackPressedCallback);
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.downloadable_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RadioButton button = holder.view.findViewById(R.id.downloadable_check_circle);

        final FrameLayout frameLayout = holder.view.findViewById(R.id.downloadable_frame);
        final ImageView thumbNail = holder.view.findViewById(R.id.downloadable_imageView);
        ImageView videoIcon = holder.view.findViewById(R.id.downloadable_play_icon);

        String mime = formats.fileMime.get(position);

        if (MIMEType.VIDEO_MP4.equals(mime) || MIMEType.VIDEO_WEBM.equals(mime))
            videoIcon.setVisibility(View.VISIBLE);
        else videoIcon.setVisibility(View.GONE);


        holder.view.setTag(position);
        Glide.with(fragment)
                .asBitmap()
                .load(Uri.parse(formats.thumbNailsURL.get(position)))
                .override(1080,1920)
                .into(thumbNail);

        if (selectionStarted) {
            if (selected.contains(position)) check(frameLayout, button);
            else unCheck(frameLayout, button);
        } else defaultState(frameLayout, button);



        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(formats.mainFileURLs.get(position)));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        holder.view.setOnClickListener(v -> {
            if (!selectionStarted)
                v.getContext().startActivity(intent);
            else swapSelection(v, frameLayout, button);
            if(selected.isEmpty()) {
                selectionStarted = false;
                notifyDataSetChanged();
            }
        });

        holder.view.setOnLongClickListener(view -> {
            if(!selectionStarted) {
                selectionStarted = true;
                notifyDataSetChanged();
                swapSelection(view, frameLayout, button);
            }else {
                view.getContext().startActivity(intent);
            }
            if(selected.isEmpty()) {
                selectionStarted = false;
                notifyDataSetChanged();
            }

            return true;
        });
    }

    OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            if(selectionStarted) {
                clearSelection();
            }else {
                setEnabled(false);
                fragment.requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        }
    };

    private void swapSelection(View view, FrameLayout frameLayout, RadioButton button) {
        boolean isSelected = button.isChecked();
        if (!isSelected) {
            selected.add((Integer) view.getTag());
            check(frameLayout, button);
        } else {
            selected.remove(view.getTag());
            unCheck(frameLayout, button);
        }
        selectedList.setValue(selected);
    }


    void check(FrameLayout frameLayout, RadioButton button) {
        frameLayout.setForeground(ResourcesCompat.getDrawable(frameLayout.getContext().getResources(), R.drawable.downloadable_selected, null));
        button.setChecked(true);
        button.setVisibility(View.VISIBLE);
    }

    void unCheck(FrameLayout frameLayout, RadioButton button) {
        frameLayout.setForeground(ResourcesCompat.getDrawable(frameLayout.getContext().getResources(), R.drawable.downloadable_foreground, null));
        button.setChecked(false);
        button.setVisibility(View.VISIBLE);
    }


    void defaultState(FrameLayout frameLayout, RadioButton button) {
        frameLayout.setForeground(null);
        button.setVisibility(View.GONE);
    }


    @Override
    public int getItemCount() {
        return formats.getFileCount();
    }

    public void clear() {
        formats.setAsEmptyFile();
        notifyDataSetChanged();
    }

    public void clearSelection(){
        selected.clear();
        selectedList.setValue(selected);
        selectionStarted = false;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        final View view;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
        }
    }

    public LiveData<ArrayList<Integer>> getSelectedList() {
        return selectedList;
    }
}


