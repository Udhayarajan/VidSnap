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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mugames.vidsnap.R
import com.mugames.vidsnap.firebase.FirebaseManager
import com.mugames.vidsnap.ui.activities.MainActivity
import com.mugames.vidsnap.utility.SingleEventLiveData
import com.mugames.vidsnap.utility.UtilityClass
import com.mugames.vidsnapkit.dataholders.Formats
import com.mugames.vidsnapkit.dataholders.Result
import com.mugames.vidsnapkit.extractor.Extractor
import com.mugames.vidsnapkit.extractor.Facebook
import com.mugames.vidsnapkit.extractor.Instagram
import kotlinx.coroutines.launch

/**
 * @author Udhaya
 * Created on 12-03-2022
 */

class VideoFragmentViewModelKt(application: Application) : VideoFragmentViewModel(application) {

    var ktFormats = listOf<Formats>()
        set(value) {
            formats.setIsMultiVideos(value.size > 1)
            value.forEach { format ->
                format.audioData.forEach { audioData ->
                    formats.audioURLs.add(audioData.url)
                    formats.audioMime.add(audioData.mimeType)
                }

                if (value.size > 1)
                    format.videoData[0].apply {
                        formats.mainFileURLs.add(url)
                        formats.fileMime.add(mimeType)
                        formats.qualities.add(quality)
                        formats.videoSizes.add(size)
                        formats.videoSizeInString.add(UtilityClass.formatFileSize(size,
                            false))
                    }
                else
                    format.videoData.forEach { videoData ->
                        formats.mainFileURLs.add(videoData.url)
                        formats.fileMime.add(videoData.mimeType)
                        formats.qualities.add(videoData.quality)
                        formats.videoSizes.add(videoData.size)
                        formats.videoSizeInString.add(UtilityClass.formatFileSize(videoData.size,
                            false))
                    }

                format.thumbnail.forEach {
                    formats.thumbNailsURL.add(it.second)
                }


                formats.title = format.title
                formats.src = format.src
            }
            field = value
        }
    val resultLiveData = SingleEventLiveData<Result>()

    private var cookies: String? = null
    private var extractorKt: Extractor? = null

    override fun onClickAnalysis(
        url: String,
        activity: MainActivity,
    ): com.mugames.vidsnap.extractor.Extractor? {
        urlLink = url
        extractorKt = Extractor.findExtractor(url)
        extractorKt?.let {
            cookies?.let { cookie ->
                it.cookies = cookie
            }

            viewModelScope.launch {
                it.start { res ->
                    resultLiveData.postValue(res)
                }
            }
            formats = com.mugames.vidsnap.utility.bundles.Formats()
            return EmptyExtractor()
        } ?: run {
            return super.onClickAnalysis(url, activity)
        }
    }

    fun onClickAnalysisWithCookies(url: String, activity: MainActivity) {

        fun getCookies(): String? {
            if (extractorKt is Instagram) {
                return activity.getCookies(R.string.key_instagram)
            } else if (extractorKt is Facebook) {
                return activity.getCookies(R.string.key_facebook)
            }
            return null
        }

        fun login() {
            var details: UtilityClass.LoginDetailsProvider? = null

            fun endFetchCookies(cookie: String) {
                cookies = cookie
                onClickAnalysis(url, activity)
            }

            //For Instagram first try with cloud cookies
            if (extractorKt is Instagram) {
                cookies?.let {
                    details = UtilityClass.LoginDetailsProvider(
                        "Instagram.com says you to login. To download it you need to login Instagram.com",
                        "https://www.instagram.com/accounts/login/",
                        arrayOf("https://www.instagram.com/"),
                        R.string.key_instagram,
                    ) { cookie ->
                        endFetchCookies(cookie)
                    }
                } ?: run {
                    FirebaseManager.getInstance(activity).getInstaCookie {
                        endFetchCookies(it)
                    }
                }
            } else if (extractorKt is Facebook) {
                details = UtilityClass.LoginDetailsProvider(
                    "Facebook requested you to sign-in. Without sign-in video can't be downloaded",
                    "https://www.facebook.com/login/",
                    arrayOf("https://m.facebook.com/login/save-device/?login_source=login#_=_",
                        "https://m.facebook.com/?_rdr",
                        "https://m.facebook.com/home.php?_rdr",
                        "https://m.facebook.com/home.php",
                        "https://m.facebook.com/?ref=dbl&_rdr",
                        "https://m.facebook.com/?ref=dbl&_rdr#~!/home.php?ref=dbl",
                        "https://m.facebook.com/?ref=dbl&_rdr#!/home.php?ref=dbl"),
                    R.string.key_facebook
                ) { cookie ->
                    endFetchCookies(cookie)
                }
            }

            details?.let {
                activity.signInNeeded(it)
            }

        }

        getCookies()?.let {
            cookies = it
            onClickAnalysis(url, activity)
        } ?: run { login() }
    }

    fun reset() {
        isShareOnly = false
        formats = com.mugames.vidsnap.utility.bundles.Formats()
    }
}

class EmptyExtractor : com.mugames.vidsnap.extractor.Extractor("Nothing") {
    override fun analyze(url: String?) {
        TODO("Not yet implemented")
    }
}