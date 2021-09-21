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

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.mugames.vidsnap.extractor.Extractor;
import com.mugames.vidsnap.extractor.status.WhatsApp;
import com.mugames.vidsnap.utility.bundles.Formats;
import com.mugames.vidsnap.utility.UtilityInterface;
import com.mugames.vidsnap.ui.activities.MainActivity;

import java.util.ArrayList;

public class StatusFragmentViewModel extends AndroidViewModel implements UtilityInterface.AnalyzeCallback {

    Formats formats;
    ArrayList<Integer> selectedList;

    UtilityInterface.AnalyzeUICallback analyzeUICallback;

    public StatusFragmentViewModel(@NonNull Application application) {
        super(application);
    }

    public void setAnalyzeUICallback(UtilityInterface.AnalyzeUICallback analyzeUICallback) {
        this.analyzeUICallback = analyzeUICallback;
    }

    public void searchForStatus(String url, MainActivity activity){
        Extractor extractor;
        if(url==null) extractor = new WhatsApp();
        else if(url.contains("insta")) {return;}
        else if(url.contains("twitter")){return;}
        else return;
        extractor.setContext(getApplication());
        extractor.setAnalyzeCallback(this);
        extractor.setDialogueInterface(activity);
        extractor.setLink(url);
        extractor.start();
    }

    @Override
    public void onAnalyzeCompleted(Formats formats) {
        this.formats =formats;
        analyzeUICallback.onAnalyzeCompleted(formats.isMultipleFile());
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


}
