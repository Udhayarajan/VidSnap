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

import static com.mugames.vidsnap.firebase.FirebaseCallBacks.UpdateCallbacks;
import static com.mugames.vidsnap.utility.Statics.OUTFILE_URI;

import android.app.Application;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mugames.vidsnap.firebase.FirebaseManager;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.network.Response;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.DownloadReceiver;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityInterface;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class MainActivityViewModel extends AndroidViewModel implements UtilityInterface.DownloadCallback {

    String TAG = Statics.TAG + "MainActivityViewModel";


    public FirebaseManager firebaseManager;

    public ArrayList<DownloadDetails> tempDetails = new ArrayList<>();

    MutableLiveData<Integer> activeDownload = new MutableLiveData<>();

    //Shared Object downloading progress
    MutableLiveData<Integer> downloadProgressLiveData = new MutableLiveData<>();
    MutableLiveData<String> downloadStatusLiveData = new MutableLiveData<>();

    MutableLiveData<Response> downloadResponse = new MutableLiveData<>();


    public static final ArrayList<DownloadDetails> downloadDetailsList = new ArrayList<>();
    MutableLiveData<ArrayList<DownloadDetails>> downloadDetailsMutableLiveData = new MutableLiveData<>();

    boolean isProgressDialogVisible;
    String progressDialogText;
    private Bundle tempResultBundle;

    Random random = new Random();

    public MainActivityViewModel(@NonNull @NotNull Application application) {
        super(application);
        firebaseManager = FirebaseManager.getInstance(application);
        for (DownloadDetails details : downloadDetailsList) {
            ((DownloadReceiver) details.receiver).setCallback(this);
        }
        activeDownload.setValue(downloadDetailsList.size());
    }

    public void checkUpdate(UpdateCallbacks updateCallbacks) {
        firebaseManager.checkUpdate(updateCallbacks);
    }

    public LiveData<Integer> getActiveDownload() {
        return activeDownload;
    }

    public static String intentString(Intent intent) {
        if (intent == null) return null;
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                return intent.getStringExtra(Intent.EXTRA_TEXT);
            }
        }
        try {
            return intent.getStringExtra(Intent.EXTRA_TEXT);
        } catch (Exception e) {
            return null;
        }
    }

    public void addDownloadDetails(DownloadDetails details) {
        downloadDetailsList.add(details);
        activeDownload.setValue(downloadDetailsList.size());
        downloadDetailsMutableLiveData.setValue(downloadDetailsList);
    }


    synchronized void removeDownloadDetails() {
        downloadDetailsMutableLiveData.postValue(downloadDetailsList);
        activeDownload.postValue(downloadDetailsList.size());

    }


    public LiveData<ArrayList<DownloadDetails>> getDownloadDetailsLiveData() {
        return downloadDetailsMutableLiveData;
    }

    public boolean isNotAgreed() {
        return !AppPref.getInstance(getApplication()).getBooleanValue(R.string.key_terms_con, false);
    }

    public ArrayList<DownloadDetails> getDownloadDetailsList() {
        return downloadDetailsList;
    }

    public int getUniqueDownloadId() {
        int ran = random.nextInt();
        for (DownloadDetails details : tempDetails) {
            while (ran == details.id) ran = random.nextInt();
        }
        return ran;
    }

    MutableLiveData<Bundle> shareOnlyDownloadLiveData = new MutableLiveData<>();

    @Override
    public void onDownloadCompleted(DownloadDetails downloadDetails) {
        if (downloadDetails.isShareOnlyDownload) {
            shareOnlyDownloadLiveData.setValue(((DownloadReceiver) downloadDetails.receiver).getResultBundle().getValue());
        }
        removeDownloadDetails();
    }

    public LiveData<Bundle> getShareOnlyDownloadLiveData() {
        return shareOnlyDownloadLiveData;
    }

    @Override
    public void onDownloadFailed(String reason, Exception e) {
        removeDownloadDetails();
        if (e == null) return;
        Response response = new Response(e);
        response.setResponse(reason);
        downloadResponse.setValue(response);
    }

    public LiveData<Response> getDownloadFailedResponseLiveData() {
        return downloadResponse;
    }


    DownloadManager downloadManager;
    long downloadId;
    private boolean downloading = false;

    UtilityInterface.ModuleDownloadCallback moduleDownloadCallback;

    public void downloadSO(String url, UtilityInterface.ModuleDownloadCallback callback) {
        downloading = true;
        moduleDownloadCallback = callback;
        downloadManager = (DownloadManager) getApplication().getSystemService(Context.DOWNLOAD_SERVICE);
        getApplication().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationUri(Uri.fromFile(new File(AppPref.getInstance(getApplication()).getCachePath(AppPref.LIBRARY_PATH) + "lib.zip")));
        downloadId = downloadManager.enqueue(request);

        downloadValues();
    }

    public boolean isProgressDialogVisible() {
        return isProgressDialogVisible;
    }

    public String getProgressDialogText() {
        return progressDialogText;
    }

    public void setProgressDialogState(boolean isVisible, String text) {
        isProgressDialogVisible = isVisible;
        progressDialogText = text;
    }

    public boolean isDownloadingSOFile() {
        return downloading;
    }

    private void downloadValues() {
        new Thread(() -> {
            while (downloading) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
                cursor.moveToFirst();
                int downloadedBytes = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                int totalSizeByte = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                int columnStatus = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int bytesDownloaded = cursor.getInt(downloadedBytes);
                int bytesTotal = cursor.getInt(totalSizeByte);
                int status = cursor.getInt(columnStatus);


                downloadStatusLiveData.postValue(statusMessage(status));

                if (status == DownloadManager.STATUS_SUCCESSFUL) downloading = false;

                int downloadProgress = (int) ((bytesDownloaded * 100L) / bytesTotal);

                downloadProgressLiveData.postValue(downloadProgress);

            }
        }).start();
    }

    String statusMessage(int status) {

        switch (status) {
            case DownloadManager.STATUS_FAILED:
                return "Download failed!";
            case DownloadManager.STATUS_PAUSED:
                return "Download paused!";
            case DownloadManager.STATUS_PENDING:
                return "Download pending!";
            case DownloadManager.STATUS_RUNNING:
                return "Download in progress!";
            case DownloadManager.STATUS_SUCCESSFUL:
                return "Download complete!";
            default:
                return "Download is nowhere in sight";
        }
    }

    public LiveData<Integer> getDownloadProgressLiveData() {
        return downloadProgressLiveData;
    }

    public LiveData<String> getDownloadStatusLiveData() {
        return downloadStatusLiveData;
    }


    BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "onReceive: Download completed");
            downloadProgressLiveData.postValue(100);
            downloading = false;
            moduleDownloadCallback.onDownloadEnded();
        }
    };

    public Bundle getTempResultBundle() {
        return tempResultBundle;
    }

    public void setTempResultBundle(Bundle tempResultBundle) {
        this.tempResultBundle = tempResultBundle;
    }

    public void deleteSharedFile() {
        Uri uri = Uri.parse(tempResultBundle.getString(OUTFILE_URI));
        FileUtil.deleteFile(
                getApplication().getExternalFilesDir(null) + File.separator + uri.getLastPathSegment(),
                null
        );
    }

    public void setDownloadingSOFile(boolean downloadingSOFile) {
        this.downloading = downloadingSOFile;
    }

    public static final int NO_SHARE_ONLY_FILES_DOWNLOADING = -1;

    public int shareOnlyDownloadStatus() {
        for (DownloadDetails details :
                downloadDetailsList) {
            if (details.isShareOnlyDownload) return details.id;
        }
        return NO_SHARE_ONLY_FILES_DOWNLOADING;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        for (DownloadDetails details : downloadDetailsList) {
            ((DownloadReceiver) details.receiver).setCallback(null);
        }
    }
}
