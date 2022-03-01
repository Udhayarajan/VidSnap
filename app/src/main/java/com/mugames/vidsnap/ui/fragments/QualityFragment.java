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

package com.mugames.vidsnap.ui.fragments;

import static com.mugames.vidsnap.storage.FileUtil.removeStuffFromName;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.ui.activities.MainActivity;
import com.mugames.vidsnap.ui.viewmodels.VideoFragmentViewModel;
import com.mugames.vidsnap.utility.MIMEType;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityInterface;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.bundles.Formats;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * <p>A fragment that shows a list of items as a modal bottom sheet.</p>
 * It offers users to select their video quality options
 */

public class QualityFragment extends BottomSheetDialogFragment {
    String TAG = Statics.TAG + ":QualityFragment";

    int selectedItem = -1;
    Bitmap thumbNail;
    EditText editText;

    Formats formats;
    VideoFragmentViewModel viewModel;
    UtilityInterface.DownloadClickedCallback downloadClickedCallback;


    public QualityFragment() {
    }


    public void setThumbNail(Bitmap thumbNail) {
        this.thumbNail = thumbNail;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quality_list_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(VideoFragmentViewModel.class);

        formats = viewModel.getFormats();

        final RecyclerView recyclerView = view.findViewById(R.id.formats);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));


        QualityAdapter adapter = new QualityAdapter(formats.qualities, formats.videoSizeInString);
        recyclerView.setAdapter(adapter);

        if (savedInstanceState != null) {
            setThumbNail(viewModel.getDownloadDetails().getThumbNail());
        }

        Button download_mp_4 = view.findViewById(R.id.download_mp4);
        Button download_mp_3 = view.findViewById(R.id.download_mp3);
        Button share = view.findViewById(R.id.share);

        ImageView imageView = view.findViewById(R.id.thumbnail_img);
        imageView.setImageBitmap(thumbNail);
        editText = view.findViewById(R.id.edit_name);

        download_mp_4.setOnClickListener(v -> {
            downloadVideo(editText.getText().toString(), false);
        });

        download_mp_3.setOnClickListener(v -> {
            downloadMp3(editText.getText().toString());
        });
        share.setOnClickListener(v -> {
            downloadVideo(editText.getText().toString(), true);
        });
    }

    void downloadVideo(String fileName, boolean isOnlyShare) {
        if (downloadClickedCallback != null)
            downloadClickedCallback.onDownloadButtonPressed();
        dismiss();
        ArrayList<DownloadDetails> downloadDetails = new ArrayList<>();
        viewModel.getDownloadDetails().isShareOnlyDownload = isOnlyShare;
        viewModel.getDownloadDetails().fileName = removeStuffFromName(fileName);
        downloadDetails.add(viewModel.getDownloadDetails());
        ((MainActivity) requireActivity()).download(downloadDetails);
    }


    private void setName(String name) {
        editText.setText(name);
    }

    public void downloadMp3(String fileName) {
        if (downloadClickedCallback != null)
            downloadClickedCallback.onDownloadButtonPressed();
        dismiss();
        ArrayList<DownloadDetails> downloadDetails = new ArrayList<>();
        if (!viewModel.getFormats().audioURLs.isEmpty()) {
            fileName = removeStuffFromName(fileName + "_mp3 version");
            viewModel.getDownloadDetails().fileName = fileName.split("\\.")[0];
            viewModel.getDownloadDetails().fileType = "mp3";
            viewModel.getDownloadDetails().fileMime = viewModel.getDownloadDetails().mimeAudio;
            viewModel.getDownloadDetails().audioURL = null;
            viewModel.getDownloadDetails().videoSize = viewModel.getDownloadDetails().audioSize;
            viewModel.getDownloadDetails().fileMime = MIMEType.AUDIO_MP4;
            viewModel.getDownloadDetails().videoURL = viewModel.getFormats().audioURLs.get(0);
            downloadDetails.add(viewModel.getDownloadDetails());
            ((MainActivity) requireActivity()).download(downloadDetails);
        } else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Under Construction")
                    .setMessage("Mp3 feature other than YouTube is currently under development.\nStay tuned, cheers")
                    .setPositiveButton("Good Job!", null)
                    .setCancelable(true)
                    .show();
        }

    }


    public void onSelectedItem(int position) {
        DownloadDetails details = viewModel.getDownloadDetails();
        Formats formats = viewModel.getFormats();
        try {
            details.chunkUrl = formats.chunkUrlList.get(position);
            details.chunkCount = formats.manifest.get(position).size();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        try {
            details.mimeAudio = formats.audioMime.get(position);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        details.fileMime = formats.fileMime.get(position);
        details.fileType = MimeTypeMap.getSingleton().getExtensionFromMimeType(details.fileMime);

        details.videoSize = formats.videoSizes.get(position);
        try {
            details.audioURL = formats.audioURLs.get(0);
            details.audioSize = formats.audioSizes.get(0);
        } catch (IndexOutOfBoundsException e) {
            details.audioURL = null;
        }
        try {
            details.videoURL = formats.mainFileURLs.get(position);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        String quality = formats.qualities.get(position);
        formats.title = removeStuffFromName(formats.title);
        if (quality.equals("--")) quality = "";
        details.pathUri = AppPref.getInstance(getContext()).getSavePath();
        details.fileName = formats.title + "_" + quality + "_";
        details.src = formats.src;
        setName(details.fileName + "." + details.fileType);
    }

    public void setOnDownloadButtonClicked(UtilityInterface.DownloadClickedCallback downloadClickedCallback) {
        this.downloadClickedCallback = downloadClickedCallback;
    }


    private static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView qualityLabel;
        final TextView sizeText;
        final RadioButton radioButton;

        ViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.fragment_quality_list_dialog_item, parent, false));
            qualityLabel = itemView.findViewById(R.id.qualityLabel);
            sizeText = itemView.findViewById(R.id.size);
            radioButton = itemView.findViewById(R.id.radioButton);
        }
    }

    private class QualityAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final int mItemCount;
        final ArrayList<String> mQualities;
        final ArrayList<String> mSizes;
        ArrayList<RadioButton> radioButtons = new ArrayList<>();

        QualityAdapter(ArrayList<String> qualities, ArrayList<String> sizes) {
            mItemCount = qualities.size();
            mQualities = qualities;
            mSizes = sizes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.itemView.setTag(position);
            holder.radioButton.setTag(position);
            holder.qualityLabel.setText(mQualities.get(position));
            holder.sizeText.setText(String.format("%s MB", mSizes.get(position)));


            holder.itemView.setOnClickListener(this::itemCheckChanged);

            if (radioButtons.size() == 0) {
                holder.radioButton.setChecked(true);
                onSelectedItem(0);
                selectedItem = 0;
            } else holder.radioButton.setChecked(false);

            holder.radioButton.setOnClickListener(this::itemCheckChanged);

            Object test = null;

            try {
                test = radioButtons.get(position);
            } catch (IndexOutOfBoundsException e) {
            }
            if (test == null)
                radioButtons.add(holder.radioButton);
            else {
                if (position == selectedItem)
                    radioButtons.get(position).setChecked(true);
                radioButtons.set(position, holder.radioButton);
            }
        }


        @Override
        public int getItemCount() {
            return mItemCount;
        }

        void itemCheckChanged(View v) {
            selectedItem = (int) v.getTag();
            for (RadioButton radioButton : radioButtons) {
                radioButton.setChecked(false);
            }
            radioButtons.get(selectedItem).setChecked(true);
            onSelectedItem(selectedItem);
        }
    }
}