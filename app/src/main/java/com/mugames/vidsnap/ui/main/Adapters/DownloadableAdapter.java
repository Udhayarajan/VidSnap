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

package com.mugames.vidsnap.ui.main.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Formats;
import com.mugames.vidsnap.Utility.UtilityInterface.DownloadableCardSelectedCallBack;

import java.util.ArrayList;

/**
 * For download able fragment
 * Some post in Instagram has edge car so to display thumbnail of video
 * it is recyclerview's java file
 * layout/downloadable.xml
 */
public class DownloadableAdapter extends RecyclerView.Adapter<DownloadableAdapter.ViewHolder> {


    MainActivity activity;
    ArrayList<Formats> list;

    DownloadableCardSelectedCallBack callBack;

    public DownloadableAdapter(MainActivity activity, ArrayList<Formats> list, DownloadableCardSelectedCallBack callBack) {
        this.activity = activity;
        this.list=list;
        this.callBack=callBack;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.downloadable_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RadioButton button = holder.view.findViewById(R.id.downloadable_check_circle);

        final FrameLayout frameLayout = holder.view.findViewById(R.id.downloadable_frame);
        final ImageView thumbNail = holder.view.findViewById(R.id.downloadable_imageView);

        holder.view.setTag(position);
        thumbNail.setImageBitmap(list.get(position).thumbNailsBitMap.get(0));

        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isSelected = button.isChecked();
                if (!isSelected) {
                    frameLayout.setForeground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.downloadable_selected, null));
                    callBack.onCardSelected((int) v.getTag());
                } else {
                    frameLayout.setForeground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.downloadable_foreground, null));
                    callBack.onCardDeSelected((int) v.getTag());
                }
                button.setChecked(!isSelected);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void clear() {
        list.clear();
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        final View view;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
        }
    }
}


