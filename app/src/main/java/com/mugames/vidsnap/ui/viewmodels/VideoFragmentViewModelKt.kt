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
import androidx.lifecycle.viewModelScope
import com.mugames.vidsnap.R
import com.mugames.vidsnap.firebase.FirebaseManager
import com.mugames.vidsnap.ui.activities.MainActivity
import com.mugames.vidsnap.utility.SingleEventLiveData
import com.mugames.vidsnap.utility.UtilityClass
import com.mugames.vidsnapkit.dataholders.Formats
import com.mugames.vidsnapkit.dataholders.Result
import com.mugames.vidsnapkit.dataholders.VideoResource
import com.mugames.vidsnapkit.extractor.Extractor
import com.mugames.vidsnapkit.extractor.Facebook
import com.mugames.vidsnapkit.extractor.Instagram
import com.mugames.vidsnapkit.network.HttpRequestService
import kotlinx.coroutines.launch
import java.net.URL


/**
 * @author Udhaya
 * Created on 12-03-2022
 */

class VideoFragmentViewModelKt(application: Application) : VideoFragmentViewModel(application) {

    private var isCookieUsed = false
    var ktFormats = listOf<Formats>()
        set(value) {
            formats = value.toFormats()
            field = value
        }
    val resultLiveData = SingleEventLiveData<Result>()

    private var cookies: String? = null
    private var extractorKt: Extractor? = null

    override fun onClickAnalysis(
        url: String,
        activity: MainActivity,
        isUserClickCall: Boolean
    ): com.mugames.vidsnap.extractor.Extractor? {
        urlLink = url
        if (url.contains("cdninstagram"))
            return null
        isCookieUsed = !isUserClickCall
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
            return super.onClickAnalysis(url, activity, false)
        }
    }

    fun onClickAnalysisWithCookies(url: String, activity: MainActivity) {
        if (isCookieUsed) {
            resultLiveData.postValue(
                Result.Failed(
                    com.mugames.vidsnapkit.dataholders.Error.InternalError(
                        "With provided cookies the media can't be accessed Try re-login with proper account"
                    )
                )
            )
            return
        }
        fun getCookies(): String? {
            if (extractorKt is Instagram) {
                isCookieUsed = true
                return activity.getCookies(R.string.key_instagram)
            } else if (extractorKt is Facebook) {
                isCookieUsed = true
                return activity.getCookies(R.string.key_facebook)
            }
            return null
        }

        fun login() {
            var details: UtilityClass.LoginDetailsProvider? = null

            fun endFetchCookies(cookie: String) {
                cookies = cookie
                onClickAnalysis(url, activity, false)
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
                    arrayOf(
                        "https://m.facebook.com/login/save-device/?login_source=login#_=_",
                        "https://m.facebook.com/?_rdr",
                        "https://m.facebook.com/home.php?_rdr",
                        "https://m.facebook.com/home.php",
                        "https://m.facebook.com/?ref=dbl&_rdr",
                        "https://m.facebook.com/?ref=dbl&_rdr#~!/home.php?ref=dbl",
                        "https://m.facebook.com/?ref=dbl&_rdr#!/home.php?ref=dbl"
                    ),
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
            onClickAnalysis(url, activity, false)
        } ?: run { login() }
    }

    fun reset() {
        isShareOnly = false
        formats = com.mugames.vidsnap.utility.bundles.Formats()
    }

    fun directDownloadFrom(url: String) {
        viewModelScope.launch {
            val path = URL(url).path
            val parts = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val formats = Formats(
                title = "Unrecognized direct download",
                src = if (parts.isNotEmpty()) parts[parts.size - 1] else "Direct download",
                url = url,
                videoData = mutableListOf(
                    VideoResource(
                        url = url,
                        mimeType = "",
                        quality = "",
                        size = HttpRequestService.create().getSize(url)
                    )
                )
            )
            resultLiveData.postValue(
                Result.Success(
                    formats = listOf(formats)
                )
            )
        }
    }
}

class EmptyExtractor : com.mugames.vidsnap.extractor.Extractor("Nothing") {
    override fun analyze(url: String?) {
        TODO("Not yet implemented")
    }
}

fun List<Formats>.toFormats(): com.mugames.vidsnap.utility.bundles.Formats {
    val formats = com.mugames.vidsnap.utility.bundles.Formats()
    formats.setIsMultiVideos(this.size > 1)
    this.forEach { format ->
        format.audioData.forEach { audioData ->
            formats.audioURLs.add(audioData.url)
            formats.audioMime.add(audioData.mimeType)
        }

        if (this.size > 1) {
            if (format.videoData.size > 0)
                format.videoData[0].apply {
                    formats.mainFileURLs.add(url)
                    formats.fileMime.add(mimeType)
                    formats.qualities.add(quality)
                    formats.videoSizes.add(size)
                    formats.videoSizeInString.add(
                        UtilityClass.formatFileSize(
                            size,
                            false
                        )
                    )
                }
            else if (format.audioData.size > 0) {
                format.audioData[0].apply {
                    formats.mainFileURLs.add(url)
                    formats.fileMime.add(mimeType)
                    formats.qualities.add("--")
                    formats.videoSizes.add(size)
                    formats.videoSizeInString.add(
                        UtilityClass.formatFileSize(
                            size,
                            false
                        )
                    )
                }
            } else if (format.imageData.size > 0) {
                format.imageData[0].apply {
                    formats.mainFileURLs.add(url)
                    formats.fileMime.add(mimeType)
                    formats.qualities.add(resolution)
                    formats.videoSizes.add(size)
                    formats.videoSizeInString.add(
                        UtilityClass.formatFileSize(
                            size,
                            false
                        )
                    )
                }
            }
        } else if (format.videoData.isNotEmpty())
            format.videoData.forEach { videoData ->
                formats.mainFileURLs.add(videoData.url)
                formats.fileMime.add(videoData.mimeType)
                formats.qualities.add(videoData.quality)
                formats.videoSizes.add(videoData.size)
                formats.videoSizeInString.add(
                    UtilityClass.formatFileSize(
                        videoData.size,
                        false
                    )
                )
            }
        else if (format.imageData.isNotEmpty()) {
            formats.apply {
                format.imageData.forEach {
                    mainFileURLs.add(it.url)
                    fileMime.add(it.mimeType)
                    videoSizes.add(it.size)
                    videoSizeInString.add(
                        UtilityClass.formatFileSize(
                            it.size,
                            false
                        )
                    )
                }
            }
        }

        if (format.audioData.isNotEmpty()) {
            format.audioData.sortBy { it.size }
            format.audioData.forEach {
                formats.audioURLs.add(it.url)
                formats.audioMime.add(it.mimeType)
                formats.audioSizes.add(it.size)
            }
        }

        format.imageData.forEach {
            formats.thumbNailsURL.add(it.url)
        }

        formats.title = format.title
        formats.src = format.src
    }

    return formats
}