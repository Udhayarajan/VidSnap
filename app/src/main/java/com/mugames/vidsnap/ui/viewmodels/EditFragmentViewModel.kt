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

package com.mugames.vidsnap.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mugames.vidsnap.BuildConfig
import com.mugames.vidsnap.VidSnapApp
import com.mugames.vidsnap.postprocessor.Duration
import com.mugames.vidsnap.postprocessor.FFMPEG
import com.mugames.vidsnap.postprocessor.FFMPEGInfo
import com.mugames.vidsnap.postprocessor.ReflectionInterfaces
import com.mugames.vidsnap.storage.AppPref
import com.mugames.vidsnap.storage.FileUtil
import com.mugames.vidsnap.utility.Statics.TAG
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * @author Udhaya
 * Created on 15-04-2022
 */

class EditFragmentViewModel(application: Application) : AndroidViewModel(application) {

    private var _intent = MutableSharedFlow<Intent>()
    val shareableMultiVideoIntent get() = _intent

    fun trimVideo(uri: Uri) {
        val mime = getApplication<VidSnapApp>().contentResolver.getType(uri)
        val tempFileUri = Uri.fromFile(
            File(
                AppPref.getInstance(getApplication())
                    .getCachePath(AppPref.SPLICE_PATH) + "src" + MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mime)
            )
        )
        viewModelScope.launch {
            FileUtil.copyFile(getApplication(), uri, tempFileUri)
            val ffmpegInfo = FFMPEGInfo().apply {
                this.videoPath = uri.toString()
                this.videoMime = getApplication<VidSnapApp>().contentResolver.getType(tempFileUri)
                localOutputPath = AppPref.getInstance(getApplication()).getCachePath(AppPref.SPLICE_PATH)
            }

            val soLoadCallbacks = object : ReflectionInterfaces.SOLoadCallbacks {
                override fun onSOLoadingSuccess(ffmpegInstance: FFMPEG?) {
                    ffmpegInstance?.apply {
                        setExecuteCallback {
                            Log.d(TAG, "onSOLoadingSuccess: ${this.info.localOutputPath}")
                            FileUtil.deleteFile(tempFileUri.path)
                            viewModelScope.launch {
                                _intent.emit(getIntent(info.localOutputPath))
                                _intent = MutableSharedFlow()
                            }
                        }

                        splitVideo(Duration(0, 0, 29, 0)) {
                            // FIXME: Show some progress to user while splitting video
                        }
                    }
                }

                override fun onSOLoadingFailed(e: Throwable) {
                    super.onSOLoadingFailed(e)
                    FFMPEG.newFFMPEGInstance(ffmpegInfo,
                        getApplication<VidSnapApp>().applicationContext,
                        this)
                }

            }
            FFMPEG.newFFMPEGInstance(ffmpegInfo,
                getApplication<VidSnapApp>().applicationContext,
                soLoadCallbacks)
        }


    }

    fun deleteAll(list: Array<Parcelable>?) {
        list?.forEach {
            FileUtil.deleteFile(it.toString())
        }
    }

    fun getIntent(path: String): Intent {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        val arrayList = ArrayList<Uri>()
        File(path).listFiles()?.forEach { file ->
            getApplication<VidSnapApp>().let { it2 ->
                FileProvider.getUriForFile(it2,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file).also { uri ->
                    arrayList.add(uri)
                }
            }
        }
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayList)
        return intent
    }
}

class EmptyIntent : Intent() {}