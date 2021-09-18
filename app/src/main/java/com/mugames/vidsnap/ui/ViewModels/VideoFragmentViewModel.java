package com.mugames.vidsnap.ui.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.mugames.vidsnap.Extractor.Facebook;
import com.mugames.vidsnap.Extractor.Instagram;
import com.mugames.vidsnap.Extractor.Twitter;
import com.mugames.vidsnap.Extractor.YouTube;
import com.mugames.vidsnap.Extractor.Extractor;
import com.mugames.vidsnap.Utility.Bundles.Formats;
import com.mugames.vidsnap.Utility.UtilityInterface;
import com.mugames.vidsnap.ui.Activities.MainActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class VideoFragmentViewModel extends AndroidViewModel implements UtilityInterface.AnalyzeCallback {

    public static final String URL_KEY = "com.mugames.vidsnap.ui.ViewModels.VideoFragmentViewModel.URL_KEY";

    ArrayList<Integer> selected = new ArrayList<>();
    UtilityInterface.AnalyzeUICallback analyzeUICallback;

    Formats formats;

    public VideoFragmentViewModel(@NonNull @NotNull Application application) {
        super(application);
    }
    
    
    public Extractor onClickAnalysis(String url,MainActivity activity){
        Extractor extractor;
        if (url.contains("youtu")) {
            extractor = new YouTube();
        } else if (url.contains("instagram")) {
            extractor = new Instagram();
        } else if (url.contains("twitter.com")) {
            extractor = new Twitter();
        } else if (url.contains("fb") || url.contains("facebook")) {
            extractor = new Facebook();
        } else {
            extractor = null;
            activity.error("URL Seems to be wrong",null);
        }
        if(extractor!=null) {
            extractor.setContext(getApplication());
            extractor.setLoginHelper(activity);
            extractor.setDialogueInterface(activity);
            extractor.setAnalyzeCallback(this);
            extractor.setLink(url);
            extractor.start();
        }
        return extractor;

    }

    public ArrayList<Integer> getSelected() {
        return selected;
    }

    public void setSelected(ArrayList<Integer> selected) {
        this.selected = selected;
    }


    public Formats getFormats() {
        return formats;
    }


    public void setAnalyzeUICallback(UtilityInterface.AnalyzeUICallback analyzeUICallback) {
        this.analyzeUICallback = analyzeUICallback;
    }

    @Override
    public void onAnalyzeCompleted(Formats formats) {
        this.formats = formats;
        analyzeUICallback.onAnalyzeCompleted(formats.isMultipleFile());
    }


}
