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
import android.app.RecoverableSecurityException;
import android.content.IntentSender;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mugames.vidsnap.database.History;
import com.mugames.vidsnap.database.HistoryDatabase;
import com.mugames.vidsnap.database.HistoryRepository;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {
    HistoryRepository repository;
    LiveData<List<History>> allValues;
    History pendingHistory;

    MutableLiveData<IntentSender> intentSenderLiveData = new MutableLiveData<>();

    public HistoryViewModel(@NonNull @NotNull Application application) {
        super(application);
        repository = new HistoryRepository(application);
        allValues = repository.getAllValues();
    }

    public void insert(History history) {
        repository.insertItem(history);
    }

    public void clearRepository() {
        repository.clear();
    }


    public LiveData<List<History>> getAllValues() {
        return allValues;
    }

    public void deleteThisItem(History currentHistory) {
        try {
            getApplication().getContentResolver().delete(
                    currentHistory.getUri(),
                    null,
                    null
            );
            new Thread(()-> HistoryDatabase.getInstance(getApplication().getApplicationContext()).historyDao().removeItem(currentHistory)).start();
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (e instanceof RecoverableSecurityException) {
                    pendingHistory = currentHistory;
                    intentSenderLiveData.postValue(((RecoverableSecurityException) e).getUserAction().getActionIntent().getIntentSender());
                    return;
                }
            }
            throw e;
        }
    }


    public LiveData<IntentSender> getIntentSender(){
        return intentSenderLiveData;
    }

    public void deletePendingUri() {
        if (pendingHistory != null){
            deleteThisItem(pendingHistory);
            pendingHistory = null;
        }
    }
}
