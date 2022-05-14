/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.mugames.vidsnap.utility;

import static com.mugames.vidsnap.utility.Statics.OUTFILE_URI;
import static com.mugames.vidsnap.utility.Statics.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.mugames.vidsnap.storage.FileUtil;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class VideoSharedBroadcast extends BroadcastReceiver {

    public static final String DETAILS_ID="com.mugames.vidsnap.utility.VideoSharedBroadcast.DETAILS_ID";
    public static void delete(Context context, Intent intent) {
        if (intent==null) return;
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (uri != null) {
            deleteSharedVideo(context, uri);
        } else {
            ArrayList<? extends Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (list != null) deleteSharedVideo(context, list);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long waitTime = 30 * 1000;
        Log.d("MUTube", "onReceive: VideoSharedBroadcast triggered and media will be deleted with in 30 sec");

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                delete(context, intent);
            }
        }, waitTime);
    }

    static void deleteSharedVideo(Context context, Uri uri) {
        FileUtil.deleteFile(
                FileUtil.getPathFromProviderUri(context, uri),
                null
        );
    }

    static void deleteSharedVideo(Context context, ArrayList<? extends Parcelable> list) {
        for (int i = 0; i < list.size(); i++) {
            Log.d(TAG, "deleteSharedVideo: " + FileUtil.getPathFromContentUri(context, ((Uri) list.get(i))));
            Log.d(TAG, "deleteSharedVideo: " + FileUtil.getPathFromProviderUri(context, ((Uri) list.get(i))));
            String path = FileUtil.getPathFromProviderUri(context, (Uri) list.get(i));
            FileUtil.deleteFile(path, null);
        }
    }
}