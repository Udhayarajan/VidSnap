package com.mugames.vidsnap.ViewModels;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import org.jetbrains.annotations.NotNull;

import static com.mugames.vidsnap.Utility.Statics.DOWNLOADED;
import static com.mugames.vidsnap.Utility.Statics.DOWNLOAD_SPEED;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_UPDATE_AUDIO;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_UPDATE_MERGING;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_UPDATE_VIDEO;
import static com.mugames.vidsnap.Utility.Statics.RESULT_CODE;
import static com.mugames.vidsnap.Utility.Statics.TOTAL_SIZE;
import static com.mugames.vidsnap.Utility.UtilityClass.formatFileSize;

public class DownloadViewModel extends AndroidViewModel {

    MutableLiveData<String> downloadProgress = new MutableLiveData<>();
    MutableLiveData<String> progressPercentage = new MutableLiveData<>();
    MutableLiveData<String> speed = new MutableLiveData<>();
    MutableLiveData<String> status = new MutableLiveData<>();

    MutableLiveData<Integer> valStr = new MutableLiveData<>();

    public DownloadViewModel(@NonNull @NotNull Application application) {
        super(application);
    }

    public LiveData<String> getDownloadProgress() {
        return downloadProgress;
    }

    public LiveData<String> getProgressPercentage() {
        return progressPercentage;
    }

    public LiveData<String> getSpeed() {
        return speed;
    }

    public LiveData<String> getStatus() {
        return status;
    }

    public LiveData<Integer> getVal() {
        return valStr;
    }

    public void process(Bundle resultData) {
        int val = resultData.getInt(PROGRESS);
        long s = resultData.getLong(DOWNLOAD_SPEED);
        long passed = resultData.getLong(DOWNLOADED);
        long total = resultData.getLong(TOTAL_SIZE);
        int resultCode =resultData.getInt(RESULT_CODE);

        if (resultCode == PROGRESS_UPDATE_AUDIO) status.setValue("Downloading Audio");
        if (resultCode == PROGRESS_UPDATE_VIDEO) status.setValue("Downloading Video");

        if (resultCode == PROGRESS_UPDATE_MERGING) status.setValue("Merging");


        downloadProgress.setValue(String.format("%s/%s", formatFileSize(passed, false), formatFileSize(total, false)));

        speed.setValue(formatFileSize(s, true));

        progressPercentage.setValue(String.format("%s %%", val));

        valStr.setValue(val);

    }

}
