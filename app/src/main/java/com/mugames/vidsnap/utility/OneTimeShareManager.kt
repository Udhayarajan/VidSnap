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

package com.mugames.vidsnap.utility

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity

/**
 * @author Udhaya
 * Created on 16-04-2022
 */

class OneTimeShareManager(
    private val activity: ComponentActivity,
    private val callback: ActivityResultCallback<ActivityResult>,
) {
    fun launch(intent: Intent) {
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            callback)
        shareResultLauncher.launch(intent)
    }

    private lateinit var shareResultLauncher: ActivityResultLauncher<Intent>


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun addReceiver(
        intent: Intent,
        uri: Uri? = null,
        list: ArrayList<out Parcelable?>? = null,
    ) {
        val receiver = Intent(activity, VideoSharedBroadcast::class.java)
        uri?.let {
            receiver.putExtra(Intent.EXTRA_STREAM, uri);
        }
        list?.let {
            receiver.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
        }

        val pendingIntent: PendingIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.getBroadcast(activity,
                0,
                receiver,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE) else PendingIntent.getBroadcast(
                activity,
                0,
                receiver,
                PendingIntent.FLAG_UPDATE_CURRENT)
        activity.startActivity(Intent.createChooser(intent,
            "Choose application to share",
            pendingIntent.intentSender))
    }

}