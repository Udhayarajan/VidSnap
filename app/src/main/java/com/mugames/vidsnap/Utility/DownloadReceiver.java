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

import static com.mugames.vidsnap.Utility.Statics.ERROR_DOWNLOADING;
import static com.mugames.vidsnap.Utility.Statics.OUTFILE_URI;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_DONE;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_FAILED;
import static com.mugames.vidsnap.Utility.Statics.RESULT_CODE;
import static com.mugames.vidsnap.Utility.UtilityInterface.DownloadCallback;
import static com.mugames.vidsnap.ui.ViewModels.MainActivityViewModel.downloadDetailsList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.text.format.DateFormat;
import android.webkit.MimeTypeMap;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mugames.vidsnap.DataBase.History;
import com.mugames.vidsnap.DataBase.HistoryDatabase;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Storage.FileUtil;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.VidSnapApp;
import com.mugames.vidsnap.ui.Activities.MainActivity;

import java.io.File;
import java.util.Date;
import java.util.Random;

/**
 * Kind of {@link ResultReceiver} that communicate between {@link android.app.Service} and UI
 */
public class DownloadReceiver extends ResultReceiver implements Parcelable {

    String TAG = Statics.TAG + ":DownloadReceiver";
    Context context;
    UtilityInterface.DialogueInterface dialogInterface;

    MutableLiveData<Bundle> resultBundle = new MutableLiveData<>();


    int id;
    DownloadCallback callback;

    public DownloadReceiver(Handler handler, Context context, UtilityInterface.DialogueInterface dialogueInterface, int id, DownloadCallback callback) {
        super(handler);
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.dialogInterface =dialogueInterface;
        this.id = id;
    }

    public void setDialogInterface(UtilityInterface.DialogueInterface dialogInterface) {
        this.dialogInterface = dialogInterface;
    }

    public void setCallback(DownloadCallback callback) {
        this.callback = callback;
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
            Uri outputUri = Uri.parse(resultData.getString(OUTFILE_URI, null));
            DownloadDetails details = DownloadDetails.findDetails(id);
            if(details==null) return;
            File file = new File(FileUtil.uriToPath(context,outputUri));
            details.fileName = file.getName().split("\\.")[0];
            details.fileType = file.getName().split("\\.")[1];
            details.fileMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(details.fileType);
            scan(outputUri.toString());
        }

    }

    void scan(String fileUri) {
        FileUtil.scanMedia(context, fileUri, (s, uri1) -> notificationSetup(uri1));
    }

    void notificationSetup(Uri uri) {
        DownloadDetails details = DownloadDetails.findDetails(id);
        if(details==null) return;
        History history = new History(details, uri);


        new Thread(() -> addItemToDB(history)).start();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, VidSnapApp.NOTIFY_DOWNLOADED);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);


        Intent play_Intent = new Intent(Intent.ACTION_VIEW);
        play_Intent.setDataAndType(uri, MIMEType.VIDEO_MP4);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 10, play_Intent, 0);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle("Download Completed")
                .setContentText(details.fileName + details.fileType)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        managerCompat.notify(new Random().nextInt(), builder.build());
        details.deleteThumbnail();

        callback.onDownloadCompleted(id);
    }

    synchronized void addItemToDB(History history) {
        HistoryDatabase.getInstance(context).historyDao().insertItem(history);
        AppPref pref = AppPref.getInstance(context);
        pref.setStringValue(R.string.key_clear_history_cache, "Cache is there");
    }

    void notificationFailed(Bundle resultData) {
        DownloadDetails details = DownloadDetails.findDetails(id);

        Intent play_Intent = new Intent(context, MainActivity.class);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, VidSnapApp.NOTIFY_DOWNLOADED);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 10, play_Intent, 0);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle("Download Failed")
                .setContentText(details.fileName + details.fileType)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        managerCompat.notify(new Random().nextInt(), builder.build());
        dialogInterface.error("Sorry!! for inconvenience try changing name and re-download or change download location",
                new Exception(resultData.getString(ERROR_DOWNLOADING)));
        callback.onDownloadCompleted(id);
    }




}
