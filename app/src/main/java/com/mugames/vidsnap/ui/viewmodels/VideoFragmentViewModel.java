package com.mugames.vidsnap.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mugames.vidsnap.extractor.Facebook;
import com.mugames.vidsnap.extractor.Instagram;
import com.mugames.vidsnap.extractor.Twitter;
import com.mugames.vidsnap.extractor.YouTube;
import com.mugames.vidsnap.extractor.Extractor;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.utility.SingleEventLiveData;
import com.mugames.vidsnap.utility.UtilityClass;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.bundles.Formats;
import com.mugames.vidsnap.utility.UtilityInterface;
import com.mugames.vidsnap.ui.activities.MainActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class VideoFragmentViewModel extends AndroidViewModel implements UtilityInterface.AnalyzeCallback, UtilityInterface.LoginHelper {

    public static final String URL_KEY = "com.mugames.vidsnap.ui.ViewModels.VideoFragmentViewModel.URL_KEY";


    ArrayList<Integer> selected = new ArrayList<>();

    protected SingleEventLiveData<Formats> formatsLiveData = new SingleEventLiveData<>();

    Formats formats;
    DownloadDetails details;
    Extractor extractor;

    String urlLink;

    public boolean isShareOnly = false;

    public VideoFragmentViewModel(@NonNull Application application) {
        super(application);
    }


    public Extractor onClickAnalysis(String url, MainActivity activity) {
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
            activity.error("URL Seems to be wrong", null);
        }
        if (extractor != null) {
            extractor.setContext(getApplication());
            extractor.setAnalyzeCallback(this);
            extractor.setLink(url);
            urlLink = url;
            formats = null;
            extractor.setLoginHelper(this);
            updateActivityReference(activity);
            extractor.start();
        }
        return extractor;

    }

    public String getUrlLink(){
        return urlLink;
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

    public void nullifyExtractor() {
        extractor = null;
    }

    public DownloadDetails getDownloadDetails() {
        if (details == null) details = new DownloadDetails();
        return details;
    }

    public void updateActivityReference(MainActivity activity) {
        if (extractor == null) return;
        extractor.setDialogueInterface(activity);
    }

    public LiveData<Formats> getFormatsLiveData() {
        return formatsLiveData;
    }


    @Override
    public void onAnalyzeCompleted(Formats formats) {
        this.formats = formats;
        formatsLiveData.setValue(formats);
    }

    public void removeActivityReference() {
        updateActivityReference(null);
    }

    public boolean isRecreated() {
        return extractor != null && formats == null;
    }

    MutableLiveData<UtilityClass.LoginDetailsProvider> loginDetailsProviderMutableLiveData = new MutableLiveData<>();

    @Override
    public void signInNeeded(UtilityClass.LoginDetailsProvider loginDetailsProvider) {
        loginDetailsProviderMutableLiveData.postValue(loginDetailsProvider);
    }

    public LiveData<UtilityClass.LoginDetailsProvider> getLoginDetailsProviderLiveData() {
        return loginDetailsProviderMutableLiveData;
    }

    @Override
    public String getCookies(int cookiesKey) {
        return AppPref.getInstance(getApplication()).getStringValue(cookiesKey, null);
    }

    public void clearLoginAlert() {
        loginDetailsProviderMutableLiveData.setValue(null);
    }

    //To prevent same instance of `details` it must be `null`ed before extraction
    public void clearDetails(){details = null;}
}
