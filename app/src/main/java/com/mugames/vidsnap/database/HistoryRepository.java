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

package com.mugames.vidsnap.database;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;

import java.util.List;

public class HistoryRepository {
    HistoryDao historyDao;
    LiveData<List<History>> allValues;
    LiveData<Boolean> dataAvailable;

    public HistoryRepository(Context context) {
        HistoryDatabase historyDatabase = HistoryDatabase.getInstance(context);
        historyDao = historyDatabase.historyDao();
//        allValues = historyDao.getAllValues();
        dataAvailable = historyDao.isEntryAvailable();
    }

    public void insertItem(History history) {
        new InsertHistoryAsync(historyDao).backgroundTask(history);
    }

    public void clear() {
        new DeleteAllHistoryAsync(historyDao, null).backgroundTask();
    }

    public void delete(History history) {
        new DeleteAllHistoryAsync(historyDao, history).backgroundTask();
    }

    public LiveData<List<History>> getAllValues() {
        return allValues;
    }

    public LiveData<Boolean> isDataAvailable() {
        return dataAvailable;
    }

    private static class InsertHistoryAsync {
        HistoryDao historyDao;

        public InsertHistoryAsync(HistoryDao historyDao) {
            this.historyDao = historyDao;
        }

        public void backgroundTask(History history) {
            Thread thread = new Thread(() -> {
                historyDao.insertItem(history);
            });
            thread.start();
        }
    }

    private static class DeleteAllHistoryAsync {
        HistoryDao historyDao;
        History history;

        public DeleteAllHistoryAsync(HistoryDao historyDao, History history) {
            this.historyDao = historyDao;
            this.history = history;
        }

        public void backgroundTask() {
            Thread thread = new Thread(() -> {
                if (history == null)
                    historyDao.deleteTable();
                else
                    historyDao.removeItem(history);
            });
            thread.start();
        }
    }

    public PagingSource<Integer, History> getPaginationSource() {
        return new HistoryPagingSource(historyDao);
    }
}
