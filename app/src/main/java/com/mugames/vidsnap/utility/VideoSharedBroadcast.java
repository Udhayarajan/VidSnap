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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.ui.viewmodels.MainActivityViewModel;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class VideoSharedBroadcast extends BroadcastReceiver {

    public static final String RESULT_BUNDLE = "com.mugames.vidsnap.utitlty.VideoSharedBroadcast.RESULT_BUNDLE";

    @Override
    public void onReceive(Context context, Intent intent) {
        long waitTime = 30 * 1000;
        Log.d("MUTube", "onReceive: VideoSharedBroadcast trigreed and media will be deleted with in 30 sec");
        Bundle tempResultBundle = intent.getBundleExtra(RESULT_BUNDLE);
        Uri uri = Uri.parse(tempResultBundle.getString(OUTFILE_URI));
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                FileUtil.deleteFile(
                        context.getExternalFilesDir(null) + File.separator + uri.getLastPathSegment(),
                        null
                );
                Log.d("MUTube", "run: file deleted" + uri);
            }
        }, waitTime);
    }
}