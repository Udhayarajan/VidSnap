package com.mugames.vidsnap.ui.main.Fragments;

import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.mugames.vidsnap.Utility.UtilityClass;
import com.mugames.vidsnap.ViewModels.VideoFragmentViewModel;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.Formats;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.ui.main.Adapters.DownloadableAdapter;

import java.util.ArrayList;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.mugames.vidsnap.Utility.FileUtil.removeStuffFromName;
import static com.mugames.vidsnap.Utility.UtilityInterface.*;
import static com.mugames.vidsnap.ViewModels.VideoFragmentViewModel.URL_KEY;


public class VideoFragment extends Fragment implements
        AnalyzeUICallback, DownloadableCardSelectedCallBack, TouchCallback {


    String TAG = Statics.TAG + ":VideoFragment";

    MainActivity activity;

    public static Uri directory;

    Button analysis;
    EditText urlBox;
    Button button;




    RecyclerView list;
    DownloadableAdapter adapter;



    long size;


    String src;


    private boolean isPaused;
    private boolean isPopupPresent;

    VideoFragmentViewModel viewModel;

    private QualityFragment dialogFragment = null;

    public static VideoFragment newInstance(String link) {
        VideoFragment fragment = new VideoFragment();
        Bundle bundle = new Bundle();
        bundle.putString(URL_KEY,link);
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
        directory = activity.getPath();

        viewModel = new ViewModelProvider(this).get(VideoFragmentViewModel.class);


        analysis = view.findViewById(R.id.analysis);
        urlBox = view.findViewById(R.id.url);
        list = view.findViewById(R.id.downloadable_recyclerView);

        button = view.findViewById(R.id.card_selected);
        button.setVisibility(View.GONE);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectResolution();
            }
        });

        String link = getArguments() != null ? getArguments().getString(URL_KEY) : null;


        analysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analysis.setEnabled(false);
                hideKeyboard(null);
                startProcess(urlBox.getText().toString());
            }
        });

        urlBox.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus)
                hideKeyboard(v);
        });

        if (link != null) startProcess(link);
        return view;

    }

    private void startProcess(String link) {
        urlBox.setText(link);
        viewModel.setAnalyzeUICallback(this);
        if(viewModel.onClickAnalysis(urlBox.getText().toString(), (MainActivity) getActivity())==null)
            unLockAnalysis();
        else linkFor(link);
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


    void linkFor(String link) {
        size = 0;
        if(dialogFragment != null && dialogFragment.isVisible())dialogFragment.dismiss();

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
        activity.dialog.dismiss();
        if (isMultipleFile) {
            adapter = new DownloadableAdapter(activity, viewModel.getFormatsArrayList(), this);
            list.setLayoutManager(new GridLayoutManager(activity, 2));
            list.setAdapter(adapter);
            button.setVisibility(View.VISIBLE);

        } else {
            SelectResolution();
        }
    }




    @Override
    public void onCardSelected(int index) {
        viewModel.getSelected().add(index);
        size += viewModel.getFormatsArrayList().get(index).videoSizes.get(0);
        button.setText("DOWNLOAD(" + UtilityClass.formatFileSize(size, false) + ")");
    }

    @Override
    public void onCardDeSelected(int index) {
        Log.e(TAG, "onCardDeSelected: " + index);
        viewModel.getSelected().remove((Object) index);
        size -= viewModel.getFormatsArrayList().get(index).videoSizes.get(0);
        button.setText("DOWNLOAD(" + UtilityClass.formatFileSize(size, false) + ")");
    }

    void SelectResolution() {
        if (viewModel.getSelected().isEmpty()) {
            actionForSOLOFile();
        } else {
            dialogFragment = null;
            urlBox.setText("");
            activity.download(viewModel.actionForMOREFile());
            button.setVisibility(View.GONE);
            adapter.clear();
        }
        viewModel.getFormatsArrayList().clear();
    }


    void actionForSOLOFile(){
        final DownloadDetails details = new DownloadDetails();

        final Formats formats;
        formats = viewModel.getFormatsArrayList().get(0);

        dialogFragment = QualityFragment.newInstance(formats.qualities, formats.videoSizeInString);

        dialogFragment.setRequired(new DownloadButtonCallBack() {
            @Override
            public void onDownloadButtonPressed(String fileName) {

                dialogFragment = null;
                urlBox.setText("");

                ArrayList<DownloadDetails> downloadDetails = new ArrayList<>();

                fileName = removeStuffFromName(fileName);

                details.fileType = ".mp4";
                details.fileName = fileName.split("\\.")[0];

                downloadDetails.add(details);
                activity.download(downloadDetails);
            }

            @Override
            public void onSelectedItem(int position, QualityFragment qualityFragment) {
                Log.d(TAG, "onSelectedItem: " + position);
                try {
                    details.mimeVideo = formats.videoMime.get(position);
                    details.mimeAudio = formats.audioMime.get(position);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
                details.videoSize = formats.videoSizes.get(position);
                try{
                    details.audioURL = formats.audioURLs.get(0);
                }catch (IndexOutOfBoundsException e){
                    details.audioURL=null;
                }
                try {
                    details.videoURL = formats.videoURLs.get(position);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    Log.d(TAG, "onSelectedItem: M3u8");
                }

                String quality = formats.qualities.get(position);
                formats.title = removeStuffFromName(formats.title);

                if (quality.equals("--")) quality = "";
                details.pathUri=directory;

                details.fileType=".mp4";

                details.fileName =formats.title + "_" + quality + "_";

                details.src = formats.src;


                qualityFragment.setName(details.fileName +details.fileType);

            }
        });
        details.thumbNail = formats.thumbNailsBitMap.get(0);

//
//        if (formats.thumbNail == null) {
//            //only in instagram case
//            details.thumbNail = formats.thumbNailsBitMap.get(0);
//        }

        dialogFragment.setThumbNail(details.thumbNail);

        if (!isPaused) dialogFragment.show(activity.getSupportFragmentManager(), "TAG");
    }


    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
        if(dialogFragment!=null)
            dialogFragment.dismiss();
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
        if (dialogFragment != null) {
            dialogFragment.show(activity.getSupportFragmentManager(), "TAG");
        }

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
}