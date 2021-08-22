package com.mugames.vidsnap.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.mugames.vidsnap.Extractor.Facebook;
import com.mugames.vidsnap.Extractor.Instagram;
import com.mugames.vidsnap.Extractor.Twitter;
import com.mugames.vidsnap.Extractor.YouTube;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.Extractor;
import com.mugames.vidsnap.Utility.Formats;
import com.mugames.vidsnap.Utility.UtilityInterface;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static com.mugames.vidsnap.Utility.FileUtil.removeStuffFromName;

public class VideoFragmentViewModel extends AndroidViewModel implements UtilityInterface.AnalyzeCallback {

    public static final String URL_KEY = "com.mugames.vidsnap.ViewModels.VideoFragmentViewModel.URL_KEY";
    ArrayList<Formats> formatsArrayList = new ArrayList<>();
    ArrayList<Integer> selected = new ArrayList<>();
    UtilityInterface.AnalyzeUICallback analyzeUICallback;

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
            extractor.setLoginHelper(activity);
            extractor.setDialogueInterface(activity);
            extractor.analyze(url,this);
        }
        return extractor;

    }

    public ArrayList<Integer> getSelected() {
        return selected;
    }

    public ArrayList<Formats> getFormatsArrayList() {
        return formatsArrayList;
    }

    public void setAnalyzeUICallback(UtilityInterface.AnalyzeUICallback analyzeUICallback) {
        this.analyzeUICallback = analyzeUICallback;
    }

    @Override
    public void onAnalyzeCompleted(Formats formats, boolean isLast) {
        formatsArrayList.add(formats);
        if (isLast) {
            analyzeUICallback.onAnalyzeCompleted(formats.thumbNailsURL.size() > 1);
            if (formats.thumbNailsURL.size() > 1) {
                if (formats.src.equals("Instagram")) {
                    renewFormatArrayWith(formats);
                }
            }
        }
    }

    //used only by instagram
    private void renewFormatArrayWith(Formats format) {
        formatsArrayList.clear();
        for (int i = 0; i < format.thumbNailsURL.size(); i++) {
            Formats formats = new Formats();
            formats.thumbNailsBitMap.add(format.thumbNailsBitMap.get(i));
            formats.videoSizes.add(format.videoSizes.get(i));
            formats.title = format.title;
            formats.videoURLs.add(format.videoURLs.get(i));
            formatsArrayList.add(formats);
        }
    }

    public ArrayList<DownloadDetails> actionForMOREFile(){
        ArrayList<DownloadDetails> downloadDetails = new ArrayList<>();
        for (int i = 0; i < getSelected().size(); i++) {
            Formats formats = getFormatsArrayList().get(getSelected().get(i));
            DownloadDetails details = new DownloadDetails();
            details.videoSize = formats.videoSizes.get(0);
            details.videoURL = formats.videoURLs.get(0);
            formats.title = removeStuffFromName(formats.title);
            details.thumbNail = formats.thumbNailsBitMap.get(0);
            details.fileType=".mp4";
            details.fileName = formats.title + "_(" + (i + 1) + ")_";
            details.src = formats.src;

            downloadDetails.add(details);
        }
        return  downloadDetails;
    }
}
