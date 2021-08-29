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
