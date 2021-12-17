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

package com.mugames.vidsnap.ui.viewmodels;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

import static com.mugames.vidsnap.utility.Statics.DOWNLOADED;
import static com.mugames.vidsnap.utility.Statics.DOWNLOAD_SPEED;
import static com.mugames.vidsnap.utility.Statics.FETCH_MESSAGE;
import static com.mugames.vidsnap.utility.Statics.PROGRESS;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_AUDIO;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_MERGING;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_VIDEO;
import static com.mugames.vidsnap.utility.Statics.RESULT_CODE;
import static com.mugames.vidsnap.utility.Statics.TOTAL_SIZE;
import static com.mugames.vidsnap.utility.UtilityClass.formatFileSize;

public class DownloadViewModel{

    final MutableLiveData<String> downloadProgress = new MutableLiveData<>();
    final MutableLiveData<String> progressPercentage = new MutableLiveData<>();
    final MutableLiveData<String> speed = new MutableLiveData<>();
    final MutableLiveData<String> status = new MutableLiveData<>();

    final MutableLiveData<Integer> valStr = new MutableLiveData<>();

    public DownloadViewModel() { }

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
        else if (resultCode == PROGRESS_UPDATE_VIDEO) status.setValue("Downloading Video");
        else if (resultCode == PROGRESS_UPDATE_MERGING) status.setValue("Merging");
        else status.setValue(resultData.getString(FETCH_MESSAGE));


        downloadProgress.setValue(String.format("%s/%s", formatFileSize(passed, false), formatFileSize(total, false)));

        speed.setValue(formatFileSize(s, true));

        progressPercentage.setValue(String.format("%s %%", val));

        valStr.setValue(val);

    }

}
