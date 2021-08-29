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

package com.mugames.vidsnap.Utility;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mugames.vidsnap.DataBase.History;
import com.mugames.vidsnap.ViewModels.MainActivityViewModel;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

import java.io.File;

import static com.mugames.vidsnap.Utility.Statics.ERROR_DOWNLOADING;
import static com.mugames.vidsnap.Utility.Statics.OUTFILE_URI;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_DONE;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_FAILED;
import static com.mugames.vidsnap.Utility.Statics.RESULT_CODE;
import static com.mugames.vidsnap.Utility.UtilityInterface.DownloadCallback;

public class DownloadReceiver extends ResultReceiver {

    String TAG = Statics.TAG + ":DownloadReceiver";
    ProgressBar progressBar;
    Activity activity;

    TextView statusText;//eg: Downloading video..
    TextView progressText;// eg: 98%
    TextView speedText;// eg:5mbps
    TextView downloadText;//eg:1mb/2mb
    View card;

    MutableLiveData<Bundle> resultBundle = new MutableLiveData<>();


    int index;
    DownloadCallback callback;
    History history;

    public DownloadReceiver(Handler handler, Activity activity, int index, DownloadCallback callback) {
        super(handler);
        this.activity = activity;
        this.callback = callback;
        this.index = index;

    }

//    public void viewHolderToUI(DownloadViewHolder viewHolder) {
//        this.progressBar = viewHolder.progressBar;
//        this.progressText = viewHolder.progressText;
//        this.speedText = viewHolder.speedText;
//        this.downloadText = viewHolder.sizeText;
//        this.statusText = viewHolder.statusText;
//        this.card = viewHolder.itemView;
//    }

    public LiveData<Bundle> getResultBundle() {
        return resultBundle;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);

        resultData.putInt(RESULT_CODE,resultCode);

        resultBundle.setValue(resultData);

        if (resultCode == PROGRESS_FAILED) {
            notificationFailed(resultData);
            return;
        }
        if (resultCode == PROGRESS_DONE) {
            scan(resultData.getString(OUTFILE_URI, null));
        }

    }

    void scan(String fileUri) {
        Log.e(TAG, "scan: Starting");
        MainActivityViewModel.service_in_use = false;
        Uri uri = Uri.parse(fileUri);

        String path = FileUtil.uriToPath(activity, uri);
        if (path.equals(File.separator)) path = uri.getPath();

        MediaScannerConnection.scanFile(activity, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                notificationSetup(uri);
            }
        });
    }

    void notificationSetup(Uri uri) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.onDownloadCompleted(index,uri);
            }
        });
        Log.d(TAG, "onScanCompleted: " + uri);
    }

    void notificationFailed(Bundle resultData) {
        callback.onFailedDownload(index);
        ((MainActivity) activity).error("Sorry!! for inconvenience try changing name and re-download or change download location",
                new Exception(resultData.getString(ERROR_DOWNLOADING)));
    }




}
