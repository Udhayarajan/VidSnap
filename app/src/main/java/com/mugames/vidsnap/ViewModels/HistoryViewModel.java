package com.mugames.vidsnap.ViewModels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mugames.vidsnap.DataBase.History;
import com.mugames.vidsnap.DataBase.HistoryRepository;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
