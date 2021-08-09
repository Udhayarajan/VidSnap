package com.mugames.vidsnap;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import java.util.ArrayList;
import java.util.List;

public class NotificationChannelCreator extends Application {
    public static final String NOTIFY_DOWNLOADING ="downloadingChannel";
    public static final String NOTIFY_DOWNLOADED ="downloadedChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        CreateNotificationChannel();
    }

    void CreateNotificationChannel(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            List<NotificationChannel> channelList=new ArrayList<>();
            channelList.add(new NotificationChannel(NOTIFY_DOWNLOADING,"Downloading Notification Channel", NotificationManager.IMPORTANCE_LOW));
            channelList.add(new NotificationChannel(NOTIFY_DOWNLOADED,"Notify when downloaded", NotificationManager.IMPORTANCE_HIGH));

            NotificationManager notificationManager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannels(channelList);
        }
    }
}