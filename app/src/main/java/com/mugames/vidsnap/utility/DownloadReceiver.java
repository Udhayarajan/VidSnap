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

package com.mugames.vidsnap.utility;

import static com.mugames.vidsnap.ui.viewmodels.MainActivityViewModel.downloadDetailsList;
import static com.mugames.vidsnap.utility.Statics.ERROR_DOWNLOADING;
import static com.mugames.vidsnap.utility.Statics.IS_SHARE_ONLY_DOWNLOAD;
import static com.mugames.vidsnap.utility.Statics.OUTFILE_URI;
import static com.mugames.vidsnap.utility.Statics.PROGRESS;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_CANCELED;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_DONE;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_FAILED;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE;
import static com.mugames.vidsnap.utility.Statics.RESULT_CODE;
import static com.mugames.vidsnap.utility.UtilityInterface.DownloadCallback;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mugames.vidsnap.database.History;
import com.mugames.vidsnap.database.HistoryDatabase;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.VidSnapApp;
import com.mugames.vidsnap.ui.activities.MainActivity;

import java.io.File;
import java.util.Random;

/**
 * Kind of {@link ResultReceiver} that communicate between {@link android.app.Service} and UI
 */
public class DownloadReceiver extends ResultReceiver implements Parcelable {

    final String TAG = Statics.TAG + ":DownloadReceiver";
    final Context context;

    final MutableLiveData<Bundle> resultBundle = new MutableLiveData<>();


    final int id;
    DownloadCallback callback;

    public DownloadReceiver(Handler handler, Context context, int id, DownloadCallback callback) {
        super(handler);
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.id = id;
    }

    public void setCallback(DownloadCallback callback) {
        this.callback = callback;
    }


    public LiveData<Bundle> getResultBundle() {
        return resultBundle;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);

        if (resultCode == PROGRESS_CANCELED) {
            DownloadDetails.findDetails(id).deleteThumbnail();
            cancelDownload();
            return;
        }

        resultData.putInt(RESULT_CODE, resultCode);

        resultBundle.setValue(resultData);

        if (resultCode == PROGRESS_FAILED) {
            notificationFailed(resultData);
            downloadDetailsList.remove(DownloadDetails.findDetails(id));
            return;
        }
        if (resultCode == PROGRESS_DONE) {
            DownloadDetails downloadDetails = DownloadDetails.findDetails(id);
            if (!resultData.getBoolean(IS_SHARE_ONLY_DOWNLOAD)) {
                Uri outputUri = Uri.parse(resultData.getString(OUTFILE_URI, null));
                DownloadDetails details = DownloadDetails.findDetails(id);
                File file = new File(FileUtil.uriToPath(context, outputUri));
                details.fileName = file.getName().split("\\.")[0];
                details.fileType = file.getName().split("\\.")[1];
                details.fileMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(details.fileType);
                downloadDetailsList.remove(DownloadDetails.findDetails(id));
                scan(outputUri.toString(),downloadDetails);
            } else {
                downloadDetailsList.remove(DownloadDetails.findDetails(id));
                createNullSafeCallback(downloadDetails);
            }
        }
    }

    void createNullSafeCallback(DownloadDetails downloadDetails) {
        if (callback != null)
            callback.onDownloadCompleted(downloadDetails);
    }

    void scan(String fileUri, DownloadDetails downloadDetails) {
        FileUtil.scanMedia(context, fileUri, (s, uri1) -> notificationSetup(uri1,downloadDetails));
    }

    void notificationSetup(Uri uri, DownloadDetails details) {

        History history = new History(details, uri);


        new Thread(() -> addItemToDB(history)).start();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, VidSnapApp.NOTIFY_DOWNLOADED);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);


        Intent play_Intent = new Intent(Intent.ACTION_VIEW);
        play_Intent.setDataAndType(uri, MIMEType.VIDEO_MP4);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, 10, play_Intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 10, play_Intent, 0);
        }
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle("Download Completed")
                .setContentText(details.fileName + "." + details.fileType)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        managerCompat.notify(new Random().nextInt(), builder.build());
        details.deleteThumbnail();

        createNullSafeCallback(details);
    }

    synchronized void addItemToDB(History history) {
        HistoryDatabase.getInstance(context).historyDao().insertItem(history);
        AppPref pref = AppPref.getInstance(context);
        pref.setStringValue(R.string.key_clear_history_cache, "Cache is there");
    }

    void cancelDownload() {
        if (callback!=null)
            callback.onDownloadFailed("Canceled",null);
    }

    void notificationFailed(Bundle resultData) {
        DownloadDetails details = DownloadDetails.findDetails(id);
        details.deleteThumbnail();

        String failReason;
        if (resultData.getString(ERROR_DOWNLOADING).contains("REQUEST_NOT_SUCCESSFUL"))
            failReason = "Download Link expired/broken. It cannot be resumed";
        else
            failReason = "Sorry!! for inconvenience try changing name and re-download or change download location";

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(details.srcUrl));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, VidSnapApp.NOTIFY_DOWNLOADED);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 110, browserIntent, 0);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle("Download Failed")
                .setContentText(details.fileName + "." + details.fileType)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        managerCompat.notify(new Random().nextInt(), builder.build());
        if (callback != null)
            callback.onDownloadFailed(failReason, new Exception(resultData.getString(ERROR_DOWNLOADING)));
    }


}
