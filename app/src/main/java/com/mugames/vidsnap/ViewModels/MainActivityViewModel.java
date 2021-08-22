package com.mugames.vidsnap.ViewModels;

import android.app.Application;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mugames.vidsnap.DataBase.History;
import com.mugames.vidsnap.DataBase.HistoryDatabase;
import com.mugames.vidsnap.Firebase.FirebaseManager;
import com.mugames.vidsnap.VidSnapApp;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.AppPref;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.FileUtil;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityInterface;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import static com.mugames.vidsnap.Firebase.FirebaseCallBacks.ShareCallback;
import static com.mugames.vidsnap.Firebase.FirebaseCallBacks.UpdateCallbacks;

public class MainActivityViewModel extends AndroidViewModel implements UtilityInterface.DownloadCallback {

    String TAG = Statics.TAG + "MainActivityViewModel";

    public static boolean service_in_use = false;

    public static final String STATIC_CACHE = "cache";
    public static final String DYNAMIC_CACHE = ".essential";

    public static final String db_name = "historyCache";

    public FirebaseManager firebaseManager;

    public ArrayList<DownloadDetails> tempDetails = new ArrayList<>();

    MutableLiveData<Integer> activeDownload = new MutableLiveData<>();

    MutableLiveData<Integer> downloadProgressLiveData = new MutableLiveData<>();
    MutableLiveData<String> downloadStatusLiveData = new MutableLiveData<>();


    ArrayList<DownloadDetails> downloadDetailsList = new ArrayList<>();
    MutableLiveData<ArrayList<DownloadDetails>> downloadDetailsMutableLiveData = new MutableLiveData<>();


    public AppPref pref;

    public MainActivityViewModel(@NonNull @NotNull Application application) {
        super(application);

        pref = new AppPref(application);
        firebaseManager = new FirebaseManager(application);
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

    public void shareLink(ShareCallback shareCallback) {
        firebaseManager.getShareLink(shareCallback);
    }

    public void addDownloadDetails(DownloadDetails details) {
        downloadDetailsList.add(details);
        activeDownload.setValue(downloadDetailsList.size());
        downloadDetailsMutableLiveData.setValue(downloadDetailsList);
    }


    void removeDownloadDetails(DownloadDetails details) {
        downloadDetailsList.remove(details);
        downloadDetailsMutableLiveData.setValue(downloadDetailsList);
        activeDownload.setValue(downloadDetailsList.size());
    }


    public LiveData<ArrayList<DownloadDetails>> getDownloadDetailsLiveData() {
        return downloadDetailsMutableLiveData;
    }

    public boolean isAgree() {
        return pref.getBooleanValue(R.string.key_terms_con, false);
    }

    public ArrayList<DownloadDetails> getDownloadDetailsList() {
        return downloadDetailsList;
    }


    @Override
    public void onDownloadCompleted(int index, Uri uri) {
        History history = new History(downloadDetailsList.get(index), uri, (String) DateFormat.format("yyyy-MM-dd", new Date()));
        Log.d(TAG, "onScanCompleted: Completed" + uri);


        new Thread(() -> addItemToDB(history)).start();


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplication(), VidSnapApp.NOTIFY_DOWNLOADED);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(getApplication());


        Intent play_Intent = new Intent(Intent.ACTION_VIEW);
        play_Intent.setDataAndType(uri, MIMEType.VIDEO_MP4);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplication(), 10, play_Intent, 0);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle("Download Completed")
                .setContentText(downloadDetailsList.get(index).fileName + downloadDetailsList.get(index).fileType)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        managerCompat.notify(new Random().nextInt(), builder.build());
        FileUtil.deleteFile(downloadDetailsList.get(index).thumbNailPath);
        removeDownloadDetails(downloadDetailsList.get(index));

    }

    @Override
    public void onFailedDownload(int index) {

        Intent play_Intent = new Intent(getApplication(), MainActivity.class);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplication(), VidSnapApp.NOTIFY_DOWNLOADED);
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(getApplication());

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplication(), 10, play_Intent, 0);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentTitle("Download Failed")
                .setContentText(downloadDetailsList.get(index).fileName + downloadDetailsList.get(index).fileType)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        managerCompat.notify(new Random().nextInt(), builder.build());

        removeDownloadDetails(downloadDetailsList.get(index));
    }

    void addItemToDB(History history) {
        HistoryDatabase.getInstance(getApplication()).historyDao().insertItem(history);
        AppPref pref = new AppPref(getApplication());
        pref.setStringValue(R.string.key_clear_history_cache, "Cache is there");
    }

    DownloadManager downloadManager;
    long downloadId;
    boolean downloading = true;

    UtilityInterface.ModuleDownloadCallback moduleDownloadCallback;

    public void downloadSO(String url, UtilityInterface.ModuleDownloadCallback callback) {
        moduleDownloadCallback = callback;
        downloadManager = (DownloadManager) getApplication().getSystemService(Context.DOWNLOAD_SERVICE);
        getApplication().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        getApplication().registerReceiver(onNotificationClicked, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        Uri uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setDestinationInExternalFilesDir(getApplication(), "libs", "lib.zip");
        downloadId = downloadManager.enqueue(request);

        downloadValues();
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

    BroadcastReceiver onNotificationClicked = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "onReceive: Download completed" );
            downloadProgressLiveData.postValue(100);

            downloading = false;
            String libsPath = getApplication().getExternalFilesDir("libs").getAbsolutePath();
            try {
                FileUtil.unzip(new File(libsPath,"lib.zip"),new File(libsPath));
            } catch (IOException e) {
                Log.e(TAG, "onReceive: ",e);
                e.printStackTrace();
            }
            moduleDownloadCallback.onDownloadEnded();
        }
    };
}
