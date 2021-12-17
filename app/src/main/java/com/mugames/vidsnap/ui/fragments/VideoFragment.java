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

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.utility.MIMEType;
import com.mugames.vidsnap.utility.UtilityClass;
import com.mugames.vidsnap.ui.viewmodels.VideoFragmentViewModel;
import com.mugames.vidsnap.ui.activities.MainActivity;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.bundles.Formats;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.ui.adapters.DownloadableAdapter;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.mugames.vidsnap.storage.FileUtil.removeStuffFromName;
import static com.mugames.vidsnap.utility.UtilityInterface.*;
import static com.mugames.vidsnap.ui.viewmodels.VideoFragmentViewModel.URL_KEY;

/**
 * A fragment that is opened when user selected video from {@link HomeFragment}
 */
public class VideoFragment extends Fragment implements
        AnalyzeUICallback, TouchCallback, DownloadUICallBack {


    String TAG = Statics.TAG + ":VideoFragment";


    MainActivity activity;

    Button analysis;
    EditText urlBox;
    Button button;


    RecyclerView list;
    DownloadableAdapter adapter;


    long size;


    private boolean isPaused;

    VideoFragmentViewModel viewModel;


    private QualityFragment dialogFragment = null;

    public static VideoFragment newInstance(String link) {
        VideoFragment fragment = new VideoFragment();
        Bundle bundle = new Bundle();
        bundle.putString(URL_KEY, link);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_video, container, false);
        activity = (MainActivity) getActivity();
        activity.setTouchCallback(this);

        viewModel = new ViewModelProvider(this).get(VideoFragmentViewModel.class);
        viewModel.setAnalyzeUICallback(this);

        analysis = view.findViewById(R.id.analysis);
        urlBox = view.findViewById(R.id.url);
        list = view.findViewById(R.id.downloadable_recyclerView);

        button = view.findViewById(R.id.card_selected);
        button.setVisibility(View.GONE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogFragment = null;
                urlBox.setText("");
                new Thread(() -> actionForMOREFile()).start();// Running is separate thread to load image sync.. with Glide
                resetMultiVideoList();
            }
        });

        String link = getArguments() != null ? getArguments().getString(URL_KEY) : null;


        analysis.setOnClickListener(v -> {
            if (adapter != null) resetMultiVideoList();
            analysis.setEnabled(false);
            hideKeyboard(null);
            startProcess(urlBox.getText().toString());
        });

        urlBox.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus)
                hideKeyboard(v);
        });

        viewModel.updateActivityReference(activity);

        if (link != null) startProcess(link);
        return view;

    }

    public void startProcess(String link) {
        urlBox.setText(link);
        safeDismissBottomSheet();
        if (viewModel.onClickAnalysis(urlBox.getText().toString(), (MainActivity) getActivity()) == null)
            unLockAnalysis();
        else setCrashCauseLink(link); // Only if it is valid URL it reach here
    }

    private void hideKeyboard(View v) {
        if (v == null)
            v = activity.getCurrentFocus();
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void safeDismissBottomSheet() {
        try {
            dialogFragment.dismiss();
        } catch (IllegalArgumentException | NullPointerException e) {
            Log.e(TAG, "safeDismissBottomSheet: ",e );
        }
        dialogFragment = null;
    }


    void setCrashCauseLink(String link) {
        Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag("TAG");
        if (fragment != null)
            ((QualityFragment) fragment).dismiss();
        FirebaseCrashlytics.getInstance().setCustomKey("URL", link);
    }


    public void unLockAnalysis() {
        try {
            analysis.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onAnalyzeCompleted(boolean isMultipleFile) {
        unLockAnalysis();
        if (isMultipleFile) {
            adapter = new DownloadableAdapter(this, viewModel.getFormats());
            adapter.getSelectedList().observe(this, this::selectedItemChanged);
            list.setLayoutManager(new GridLayoutManager(activity, 2));
            list.setAdapter(adapter);
        } else {
            actionForSOLOFile();
        }
    }

    void selectedItemChanged(ArrayList<Integer> selectedValue) {
        viewModel.setSelected(selectedValue);
        size = 0;
        for (int selectedIndex : selectedValue) {
            size += viewModel.getFormats().videoSizes.get(selectedIndex);
        }
        if (selectedValue.size() > 0) {
            button.setVisibility(View.VISIBLE);
            button.setText("DOWNLOAD(" + UtilityClass.formatFileSize(size, false) + ")");
        } else button.setVisibility(View.GONE);
    }

    void resetMultiVideoList() {
        button.setVisibility(View.GONE);
        adapter.clear();
        adapter = null;
    }


    void actionForSOLOFile() {
        Formats formats = viewModel.getFormats();

        dialogFragment = QualityFragment.newInstance(formats.qualities, formats.videoSizeInString);
        dialogFragment.setRequired(this);

        Glide.with(requireContext())
                .asBitmap()
                .load(Uri.parse(formats.thumbNailsURL.get(0)))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        viewModel.getDownloadDetails().setThumbNail(getContext(), resource);
                        dialogFragment.setThumbNail(resource);
                        if (!isPaused)
                            dialogFragment.show(activity.getSupportFragmentManager(), "TAG");

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }


    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
        safeDismissBottomSheet();
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
        try {
            if (viewModel.getFormats() != null && !viewModel.getFormats().isMultipleFile()) {
                if (dialogFragment == null) actionForSOLOFile();
                else dialogFragment.show(activity.getSupportFragmentManager(), "TAG");
            }
        } catch (IllegalStateException e) {}

    }

    @Override
    public void onDestroy() {
        safeDismissBottomSheet();
        super.onDestroy();
    }

    @Override
    public void onDispatchTouch(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = activity.getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                }
            }
        }
    }

    public void actionForMOREFile() {
        ArrayList<DownloadDetails> downloadDetails = new ArrayList<>();
        for (int i = 0; i < viewModel.getSelected().size(); i++) {
            int index = viewModel.getSelected().get(i);
            DownloadDetails details = new DownloadDetails();
            details.videoSize = viewModel.getFormats().videoSizes.get(index);
            details.videoURL = viewModel.getFormats().mainFileURLs.get(index);
            viewModel.getFormats().title = removeStuffFromName(viewModel.getFormats().title);

            FutureTarget<Bitmap> target = Glide.with(getContext())
                    .asBitmap()
                    .load(viewModel.getFormats().thumbNailsURL.get(index))
                    .submit();

            try {
                details.setThumbNail(getContext(), target.get());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

            details.fileType = MimeTypeMap.getSingleton().getExtensionFromMimeType(viewModel.getFormats().fileMime.get(index));
            details.fileName = viewModel.getFormats().title + "_(" + (i + 1) + ")_";
            details.src = viewModel.getFormats().src;
            details.pathUri = AppPref.getInstance(getContext()).getSavePath();

            downloadDetails.add(details);
        }

        activity.runOnUiThread(() -> activity.download(downloadDetails));

    }

    void updateUiOnDownloadPressed(){
        viewModel.getDownloadDetails().srcUrl = urlBox.getText().toString();
        safeDismissBottomSheet();
        dialogFragment = null;
        urlBox.setText("");
    }

    @Override
    public void onDownloadMP4ButtonPressed(String fileName) {
        updateUiOnDownloadPressed();
        downloadVideo(fileName,false);

    }

    void downloadVideo(String fileName, boolean isOnlyShare){
        viewModel.nullifyFormats();
        ArrayList<DownloadDetails> downloadDetails = new ArrayList<>();
        fileName = removeStuffFromName(fileName);
        viewModel.getDownloadDetails().isShareOnlyDownload = isOnlyShare;
        viewModel.getDownloadDetails().fileName = fileName.split("\\.")[0];
        downloadDetails.add(viewModel.getDownloadDetails());
        activity.download(downloadDetails);
    }

    @Override
    public void onDownloadMP3ButtonPressed(String fileName) {
        updateUiOnDownloadPressed();
        ArrayList<DownloadDetails> downloadDetails = new ArrayList<>();
        if(!viewModel.getFormats().audioURLs.isEmpty()){
            fileName = removeStuffFromName(fileName+"_mp3 version");
            viewModel.getDownloadDetails().fileName = fileName.split("\\.")[0];
            viewModel.getDownloadDetails().fileType = "mp3";
            viewModel.getDownloadDetails().fileMime = viewModel.getDownloadDetails().mimeAudio;
            viewModel.getDownloadDetails().audioURL = null;
            viewModel.getDownloadDetails().videoSize = viewModel.getDownloadDetails().audioSize;
            viewModel.getDownloadDetails().fileMime = MIMEType.AUDIO_MP4;
            viewModel.getDownloadDetails().videoURL = viewModel.getFormats().audioURLs.get(0);
            downloadDetails.add(viewModel.getDownloadDetails());
            activity.download(downloadDetails);
        }else {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Under Construction")
                    .setMessage("Mp3 feature other than YouTube is currently under development.\nStay tuned, cheers")
                    .setPositiveButton("Good Job!",null)
                    .setCancelable(true)
                    .show();
        }
        viewModel.nullifyFormats();

    }

    @Override
    public void onShareButtonPressed(String fileName) {
        updateUiOnDownloadPressed();
        downloadVideo(fileName,true);
    }

    @Override
    public void onSelectedItem(int position, QualityFragment qualityFragment) {
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
        qualityFragment.setName(details.fileName + "." + details.fileType);
    }
}