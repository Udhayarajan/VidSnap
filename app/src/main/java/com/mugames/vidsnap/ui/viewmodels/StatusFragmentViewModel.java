/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.mugames.vidsnap.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mugames.vidsnap.extractor.Extractor;
import com.mugames.vidsnap.extractor.status.WhatsApp;
import com.mugames.vidsnap.ui.activities.MainActivity;
import com.mugames.vidsnap.utility.UtilityInterface;
import com.mugames.vidsnap.utility.bundles.Formats;
import com.mugames.vidsnapkit.dataholders.Result;

import java.util.ArrayList;
import java.util.List;

public class StatusFragmentViewModel extends AndroidViewModel implements UtilityInterface.AnalyzeCallback {

    Formats formats;
    ArrayList<Integer> selectedList;

    MutableLiveData<Formats> formatsLiveData = new MutableLiveData<>();
    MutableLiveData<Result.Failed> failedResultLiveData = new MutableLiveData<>();

    public StatusFragmentViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Formats> getFormatsLiveData() {
        return formatsLiveData;
    }

    public LiveData<Result.Failed> getFailedResultLiveData() {
        return failedResultLiveData;
    }

    public void searchForStatus(String url, MainActivity activity) {
        Extractor extractor;
        if (url == null) extractor = new WhatsApp();
        else return;
        extractor.setContext(getApplication());
        extractor.setAnalyzeCallback(this);
        extractor.setDialogueInterface(activity);
        extractor.setLink(null);
        extractor.start();
    }

    @Override
    public void onAnalyzeCompleted(Formats formats) {
        this.formats = formats;
        formatsLiveData.setValue(formats);
    }

    public Formats getFormats() {
        return formats;
    }

    public void setSelectedList(ArrayList<Integer> selectedList) {
        this.selectedList = selectedList;
    }

    public ArrayList<Integer> getSelectedList() {
        return selectedList;
    }


    public void downloadInstagramStory(String url, String cookies) {
        if (cookies == null) return;
        com.mugames.vidsnapkit.extractor.Extractor extractor = com.mugames.vidsnapkit.extractor.Extractor.Companion.findExtractor(url);
        extractor.setCookies(cookies);
        extractor.startAsync(result -> {
            if (result instanceof Result.Success) {
                List<com.mugames.vidsnapkit.dataholders.Formats> formats = ((Result.Success) result).getFormats();
                this.formats = VideoFragmentViewModelKtKt.toFormats(formats);
                formatsLiveData.postValue(this.formats);
            } else if (result instanceof Result.Failed) {
                failedResultLiveData.postValue(((Result.Failed) result));
                Log.e("TAG", "downloadInstagramStory: " + result + "Reason: " + ((Result.Failed) result).getError().getMessage(), ((Result.Failed) result).getError().getE());
            }
        });
    }
}
