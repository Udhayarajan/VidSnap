package com.mugames.vidsnap.ViewModels;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
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
import com.mugames.vidsnap.NotificationChannelCreator;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.AppPref;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.FileUtil;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.Utility.UtilityInterface;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

import org.jetbrains.annotations.NotNull;

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



    public void removeDownloadDetails(DownloadDetails details) {
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


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplication(), NotificationChannelCreator.NOTIFY_DOWNLOADED);
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
//        downloadDetailsList.remove(index);
        removeDownloadDetails(downloadDetailsList.get(index));

    }

    @Override
    public void onFailedDownload(int index) {

        Intent play_Intent = new Intent(getApplication(), MainActivity.class);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplication(), NotificationChannelCreator.NOTIFY_DOWNLOADED);
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
}
