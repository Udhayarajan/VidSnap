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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.DownloadReceiver;
import com.mugames.vidsnap.Storage.FileUtil;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityClass;
import com.mugames.vidsnap.ViewModels.DownloadViewModel;
import com.mugames.vidsnap.ViewModels.MainActivityViewModel;

import org.jetbrains.annotations.NotNull;

public class DownloadAdapter extends ListAdapter<DownloadDetails, DownloadAdapter.DownloadViewHolder> {


    String TAG = Statics.TAG + ":DownloadAdapter";

    MainActivityViewModel activityViewModel;
    DownloadViewModel viewModel;
    LifecycleOwner lifecycleOwner;


    public DownloadAdapter(MainActivityViewModel activityViewModel, DownloadViewModel downloadViewModel, LifecycleOwner owner) {
        super(DIFF_CALLBACK);
        lifecycleOwner = owner;
        viewModel = downloadViewModel;
        this.activityViewModel = activityViewModel;
    }

    private static final @NonNull
    @NotNull
    DiffUtil.ItemCallback<DownloadDetails> DIFF_CALLBACK = new DiffUtil.ItemCallback<DownloadDetails>() {
        @Override
        public boolean areItemsTheSame(@NonNull @NotNull DownloadDetails oldItem, @NonNull @NotNull DownloadDetails newItem) {
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull @NotNull DownloadDetails oldItem, @NonNull @NotNull DownloadDetails newItem) {
            return false;
        }
    };

//        public DownloadAdapter(ArrayList<DownloadDetails> list, ArrayList<DownloadReceiver> receivers) {
//            ((MainActivity)getActivity()).activityViewModel.getDownloadList().observe(getViewLifecycleOwner(), new Observer<List<DownloadDetails>>() {
//                @Override
//                public void onChanged(List<DownloadDetails> downloadDetails) {
//                    infos = (ArrayList<DownloadDetails>) downloadDetails;
//                }
//            });
//            downloadReceivers = new ArrayList<>(receivers);
//        }


    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DownloadViewHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadDetails details = getItem(position);
        holder.downloadText.setText(details.fileName + details.fileType);
        byte[] img = (byte[]) FileUtil.loadImage(details.thumbNailPath);
        Bitmap thumbnail = UtilityClass.bytesToBitmap(img, details.thumbWidth, details.thumbHeight);
        holder.thumbNailView.setImageBitmap(thumbnail);

        ((DownloadReceiver) details.receiver).getResultBundle().observe(lifecycleOwner, new Observer<Bundle>() {
            @Override
            public void onChanged(Bundle bundle) {
                viewModel.process(bundle);
                viewModel.getDownloadProgress().observe(lifecycleOwner, s -> holder.sizeText.setText(s));
                viewModel.getProgressPercentage().observe(lifecycleOwner, s -> holder.progressText.setText(s));
                viewModel.getSpeed().observe(lifecycleOwner, s -> holder.speedText.setText(s));
                viewModel.getStatus().observe(lifecycleOwner, s -> holder.statusText.setText(s));
                viewModel.getVal().observe(lifecycleOwner, holder.progressBar::setProgress);
            }
        });
    }


    //    @Override
//    public void onDownloadDone(int index) {
//
//        DownloadReceiver downloadReceiver = receivers.get(index);
//
//        index = downloadReceivers.indexOf(downloadReceiver);
//
//        downloadReceivers.remove(index);
//        downloadDetails.remove(index);
//        adapter.notifyItemRemoved(index);
//
//        if (downloadDetails.isEmpty()) {
//            receivers.clear();
//        }
//    }
    static class DownloadViewHolder extends RecyclerView.ViewHolder {

        TextView downloadText;
        public TextView speedText;
        public final ProgressBar progressBar;
        public ImageView thumbNailView;
        public TextView sizeText;
        public TextView progressText;
        public TextView statusText;


        DownloadViewHolder(LayoutInflater layoutInflater, ViewGroup parent) {
            super(layoutInflater.inflate(R.layout.fragment_downloading_item, parent, false));

            downloadText = itemView.findViewById(R.id.download_name);
            progressBar = itemView.findViewById(R.id.download_progress);
            speedText = itemView.findViewById(R.id.download_speed);
            sizeText = itemView.findViewById(R.id.download_size);
            thumbNailView = itemView.findViewById(R.id.download_thumb);
            progressText = itemView.findViewById(R.id.download_progress_text);
            statusText = itemView.findViewById(R.id.download_status);
        }



    }
}



