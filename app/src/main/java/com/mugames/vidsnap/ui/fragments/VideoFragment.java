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

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.mugames.vidsnap.storage.FileUtil.removeStuffFromName;
import static com.mugames.vidsnap.ui.viewmodels.VideoFragmentViewModel.URL_KEY;
import static com.mugames.vidsnap.utility.UtilityInterface.TouchCallback;

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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.ui.activities.MainActivity;
import com.mugames.vidsnap.ui.adapters.DownloadableAdapter;
import com.mugames.vidsnap.ui.viewmodels.VideoFragmentViewModelKt;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityClass;
import com.mugames.vidsnap.utility.UtilityInterface;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.bundles.Formats;
import com.mugames.vidsnapkit.dataholders.Error;
import com.mugames.vidsnapkit.dataholders.ProgressState;
import com.mugames.vidsnapkit.dataholders.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLProtocolException;

/**
 * A fragment that is opened when user selected video from {@link HomeFragment}
 */
public class VideoFragment extends Fragment implements
        TouchCallback {


    String TAG = Statics.TAG + ":VideoFragment";


    MainActivity activity;

    Button analysis;
    EditText urlBox;
    Button button;


    RecyclerView list;
    DownloadableAdapter adapter;


    long size;

    boolean isRecreated;

    VideoFragmentViewModelKt viewModel;
    String link;


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
        activity = (MainActivity) requireActivity();
        activity.setTouchCallback(this);

        isRecreated = savedInstanceState != null;

        viewModel = new ViewModelProvider(requireActivity()).get(VideoFragmentViewModelKt.class);
        viewModel.getFormatsLiveData().observe(getViewLifecycleOwner(), formats -> {
            if (!isRecreated)
                onAnalyzeCompleted(formats);
            isRecreated = false;
        });

        viewModel.getResultLiveData().observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                List<com.mugames.vidsnapkit.dataholders.Formats> formats = ((Result.Success) result).getFormats();
                onAnalyzeCompleted(formats);
            } else if (result instanceof Result.Failed) {
                Error error = ((Result.Failed) result).getError();
                if (error instanceof Error.LoginInRequired ||
                        error instanceof Error.InvalidCookies)
                    viewModel.onClickAnalysisWithCookies(urlBox.getText().toString(), activity);
                else if (error instanceof Error.InternalError && error.getE() instanceof SSLProtocolException)
                    startProcess(link);
                else if (error instanceof Error.Instagram404Error
                        && !((Error.Instagram404Error) error).isCookiesUsed())
                    viewModel.onClickAnalysisWithCookies(urlBox.getText().toString(), activity);
                else
                    activity.error(error.getMessage(), error.getE());
            } else {
                ProgressState state = ((Result.Progress) result).getProgressState();
                if (state instanceof ProgressState.End) {
                    ((UtilityInterface.DialogueInterface) activity).show("Almost done!!");
                }else {
                    ((UtilityInterface.DialogueInterface) activity).show("Analysing");
                }
            }
        });


        analysis = view.findViewById(R.id.analysis);
        urlBox = view.findViewById(R.id.url);
        list = view.findViewById(R.id.downloadable_recyclerView);

        button = view.findViewById(R.id.card_selected);
        button.setVisibility(View.GONE);
        button.setOnClickListener(v -> {
            // Running is separate thread to load image sync.. with Glide
            urlBox.setText("");
            new Thread(this::actionForMOREFile).start();
            resetMultiVideoList();
        });

        link = getArguments() != null ? getArguments().getString(URL_KEY) : null;

        if (link == null)
            link = viewModel.getUrlLink();
        urlBox.setText(link);

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

        if (link != null && savedInstanceState == null && !link.equals(viewModel.getUrlLink())) {
            urlBox.setText(link);
            startProcess(link);
        }
        return view;

    }

    private boolean isNewLink() {
        return link.equals(viewModel.getUrlLink());
    }

    public void startProcess(String link) {
        if (link == null) return;
        if (urlBox != null) urlBox.setText(link);
        if (getArguments() != null) {
            getArguments().putString(URL_KEY, link);
        }
        this.link = link;
        safeDismissBottomSheet();
        if (activity != null)
            ((UtilityInterface.DialogueInterface) activity).show("Analysing");
        if (viewModel != null) {
            if (viewModel.onClickAnalysis(link, (MainActivity) requireActivity()) == null)
                unLockAnalysis();
            else {
                viewModel.getDownloadDetails().srcUrl = link;
                viewModel.clearDetails();
                // Only if it is valid URL it reach here
                viewModel.getLoginDetailsProviderLiveData().observe(getViewLifecycleOwner(), new Observer<UtilityClass.LoginDetailsProvider>() {
                            @Override
                            public void onChanged(UtilityClass.LoginDetailsProvider loginDetailsProvider) {
                                if (loginDetailsProvider == null) return;
                                safeDismissBottomSheet();
                                urlBox.setText("");

                                viewModel.clearLoginAlert();
                                viewModel.getLoginDetailsProviderLiveData().removeObserver(this);
                                ((MainActivity) VideoFragment.this.requireActivity()).signInNeeded(loginDetailsProvider);
                            }
                        }
                );
                setCrashCauseLink(link);
            }
        }
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
        } catch (IllegalStateException | NullPointerException e) {
            e.printStackTrace();
        }
        dialogFragment = null;
    }


    void setCrashCauseLink(String link) {
        if (dialogFragment != null)
            dialogFragment.dismiss();
        FirebaseCrashlytics.getInstance().setCustomKey("URL", link);
    }


    public void unLockAnalysis() {
        try {
            analysis.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void onAnalyzeCompleted(Formats formats) {
        unLockAnalysis();
        if (viewModel.isRecreated() && !isNewLink()) return;
        viewModel.nullifyExtractor();
        if (formats.isMultipleFile()) {
            ((UtilityInterface.DialogueInterface) activity).dismiss();
            adapter = new DownloadableAdapter(this, viewModel.getFormats());
            adapter.getSelectedList().observe(getViewLifecycleOwner(), this::selectedItemChanged);
            list.setLayoutManager(new GridLayoutManager(activity, 2));
            list.setAdapter(adapter);
        } else {
            actionForSOLOFile(formats);
        }
    }

    private void onAnalyzeCompleted(List<com.mugames.vidsnapkit.dataholders.Formats> formats) {
        unLockAnalysis();
        if (viewModel.isRecreated() && !isNewLink()) return;
        viewModel.nullifyExtractor();
        viewModel.reset();
        viewModel.setKtFormats(formats);
        onAnalyzeCompleted(viewModel.getFormats());
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


    void actionForSOLOFile(Formats formats) {
        safeDismissBottomSheet();
        if (viewModel.getFormats() == null) return;
        QualityFragment dialogFragment = new QualityFragment();
        dialogFragment.setOnDownloadButtonClicked(this::safeDismissBottomSheet);

        Glide.with(requireContext())
                .asBitmap()
                .load(Uri.parse(formats.thumbNailsURL.get(0)))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        viewModel.getDownloadDetails().setThumbNail(getContext(), resource);
                        dialogFragment.setThumbNail(resource);
                        VideoFragment.this.dialogFragment = dialogFragment;
                        ((UtilityInterface.DialogueInterface) activity).dismiss();
                        dialogFragment.show(requireActivity().getSupportFragmentManager(), "TAG");
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (viewModel != null)
            viewModel.removeActivityReference();
        requireActivity().getViewModelStore().clear();
        activity.setTouchCallback(null);
        Log.e(TAG, "onDestroy: called");
        dialogFragment = null;
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

            FutureTarget<Bitmap> target = Glide.with(requireContext())
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

    @Override
    public void onPause() {
        super.onPause();
        safeDismissBottomSheet();
    }
}