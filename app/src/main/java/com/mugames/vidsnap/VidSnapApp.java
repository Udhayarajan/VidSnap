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

package com.mugames.vidsnap;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import java.util.ArrayList;
import java.util.List;

public class VidSnapApp extends Application {
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