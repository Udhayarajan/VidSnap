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

package com.mugames.vidsnap.ui.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mugames.vidsnap.DataBase.History;
import com.mugames.vidsnap.DataBase.HistoryRepository;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HistoryViewModel extends AndroidViewModel {
    HistoryRepository repository;
    LiveData<List<History>> allValues;

    public HistoryViewModel(@NonNull @NotNull Application application) {
        super(application);
        repository = new HistoryRepository(application);
        allValues = repository.getAllValues();
    }

    public void insert(History history){
        repository.insertItem(history);
    }

    public void delete(){
        repository.clear();
    }


    public LiveData<List<History>> getAllValues() {
        return allValues;
    }

}
