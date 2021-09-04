package com.mugames.vidsnap.DataBase;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

public class HistoryRepository {
    HistoryDao historyDao;
    LiveData<List<History>> allValues;

    public HistoryRepository(Application application){
        HistoryDatabase historyDatabase = HistoryDatabase.getInstance(application);
        historyDao = historyDatabase.historyDao();
        allValues = historyDao.getAllValues();
    }

    public void insertItem(History history){
        new InsertHistoryAsync(historyDao).backgroundTask(history);
    }

    public void clear(){
        new DeleteAllHistoryAsync(historyDao).backgroundTask();
    }
    public LiveData<List<History>> getAllValues(){
        return allValues;
    }

    private static class InsertHistoryAsync{
        HistoryDao historyDao;

        public InsertHistoryAsync(HistoryDao historyDao) {
            this.historyDao = historyDao;
        }

        public void backgroundTask(History history){
            Thread thread = new Thread(() -> {
                historyDao.insertItem(history);
            });
            thread.start();
        }
    }

    private static class DeleteAllHistoryAsync{
        HistoryDao historyDao;

        public DeleteAllHistoryAsync(HistoryDao historyDao) {
            this.historyDao = historyDao;
        }

        public void backgroundTask(){
            Thread thread = new Thread(() -> {
                historyDao.deleteTable();
            });
            thread.start();
        }
    }

}
