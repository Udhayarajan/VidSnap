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

package com.mugames.vidsnap.postprocessor;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback;
import com.arthenica.ffmpegkit.LogCallback;
import com.arthenica.ffmpegkit.NativeLoader;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.StatisticsCallback;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.utility.MIMEType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static com.mugames.vidsnap.utility.Statics.TAG;
import static com.mugames.vidsnap.storage.AppPref.LIBRARY_PATH;

import org.jetbrains.annotations.NotNull;

public class FFMPEG {
    public static String FFMPEG_VERSION = "451"; //v4.5.1.LTS

    public static String jniPath;
    public static String filesDir;
    private static volatile int INSTANCE_COUNT;

    public static boolean isLibLoaded = false;

    FFmpegSessionCompleteCallback executeCallback;
    FFMPEGInfo info;

    Context context;


    public static synchronized void newFFMPEGInstance(FFMPEGInfo ffmpegInfo, Context context, ReflectionInterfaces.SOLoadCallbacks soLoadCallbacks) {
        INSTANCE_COUNT++;
        new FFMPEG(ffmpegInfo, context, soLoadCallbacks);
    }

    FFmpegSessionCompleteCallback wrappedExecuteCallback = new FFmpegSessionCompleteCallback() {
        @Override
        public void apply(FFmpegSession session) {
            deleteLibs();
            if (executeCallback != null)
                executeCallback.apply(session);
        }
    };


    public FFMPEG(FFMPEGInfo ffmpegInfo, Context context, ReflectionInterfaces.SOLoadCallbacks soLoadCallbacks) {
        this.info = ffmpegInfo;
        this.context = context;

        if (soLoadCallbacks == null)
            return;// No need to perform anything because library is loaded statically

        String libsPath = AppPref.getInstance(context).getCachePath(LIBRARY_PATH);
        if (jniPath == null) jniPath = libsPath + "jni" + File.separator;

        if (FileUtil.isFileNotExists(jniPath)) {
            new Thread(() -> {
                try {
                    FileUtil.unzip(new File(libsPath, FFMPEG_VERSION+"lib.zip"), new File(libsPath), () -> {
                        new Handler(context.getMainLooper()).post(() -> {
                            if (filesDir == null)
                                filesDir = context.getFilesDir().getAbsolutePath() + File.separator;
                            loadSOFiles(jniPath, soLoadCallbacks);
                        });
                    });
                } catch (IOException e) {
                    Log.e(TAG, "onReceive: ", e);
                    e.printStackTrace();
                }
            }).start();
        } else {
            if (filesDir == null)
                filesDir = context.getFilesDir().getAbsolutePath() + File.separator;
            loadSOFiles(jniPath, soLoadCallbacks);
        }

    }


    public void setExecuteCallback(FFmpegSessionCompleteCallback executeCallback) {
        this.executeCallback = executeCallback;
    }


    static synchronized void deleteLibs() {
        INSTANCE_COUNT--;
        Log.e(TAG, "deleteLibs: " + INSTANCE_COUNT);
        if (INSTANCE_COUNT <= 0) {
            INSTANCE_COUNT = 0;
            new Thread(() -> FileUtil.deleteFile(jniPath, null)).start();
        }
    }


    @SuppressLint("UnsafeDynamicallyLoadedCode")
    static void loadJni(File file) throws IOException {
        if (!file.exists()) return;
        File lib = new File(filesDir + file.getName());
        FileInputStream inputStream = new FileInputStream(file.getAbsolutePath());
        FileOutputStream outputStream = new FileOutputStream(lib);
        FileChannel inChannel = inputStream.getChannel();
        FileChannel outChannel = outputStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inputStream.close();
        outputStream.close();
        if (!file.getName().contains("ffmpegkit"))
            System.load(lib.getAbsolutePath());
    }


    public void loadSOFiles(String jniPath, ReflectionInterfaces.SOLoadCallbacks callbacks) {
        if (isLibLoaded) {
            callbacks.onSOLoadingSuccess(this);
            return;
        }
        File jniDirectory = new File(jniPath);
        File[] sos = jniDirectory.listFiles();

        try {

            if (sos != null) {
                NativeLoader.setSOPath(filesDir + "lib");

                loadJni(new File(jniPath, "libc++_shared.so"));

                loadJni(new File(jniPath, "libffmpegkit_abidetect.so"));


                loadJni(new File(jniPath, "libavutil.so"));
                loadJni(new File(jniPath, "libavutil_neon.so"));

                loadJni(new File(jniPath, "libswscale.so"));
                loadJni(new File(jniPath, "libswscale_neon.so"));

                loadJni(new File(jniPath, "libswresample.so"));
                loadJni(new File(jniPath, "libswresample_neon.so"));

                loadJni(new File(jniPath, "libavcodec.so"));
                loadJni(new File(jniPath, "libavcodec_neon.so"));

                loadJni(new File(jniPath, "libavformat.so"));
                loadJni(new File(jniPath, "libavformat_neon.so"));

                loadJni(new File(jniPath, "libavfilter.so"));
                loadJni(new File(jniPath, "libavfilter_neon.so"));

                loadJni(new File(jniPath, "libavdevice.so"));
                loadJni(new File(jniPath, "libavdevice_neon.so"));

                loadJni(new File(jniPath, "libffmpegkit.so"));
                loadJni(new File(jniPath, "libffmpegkit_armv7a_neon.so"));

                isLibLoaded = true;
                callbacks.onSOLoadingSuccess(this);
            }
        } catch (IOException | UnsatisfiedLinkError e) {
            e.printStackTrace();
            callbacks.onSOLoadingFailed(e);
        }
    }

    public void mergeAsync(StatisticsCallback statisticsCallback) {
        String command = findCommand(FFMPEGType.MERGE);
        FFmpegKitConfig.enableStatisticsCallback(statisticsCallback);
        FFmpegKit.executeAsync(String.format(command, info.videoPath, info.audioPath, info.localOutputPath), wrappedExecuteCallback);
    }


    public void splitVideo(@NotNull Duration duration, StatisticsCallback statisticsCallback) {
        String cmd = findCommand(FFMPEGType.TRIM_VIDEO);
        cmd = String.format(cmd, info.videoPath, duration.getHours(),
                duration.getMinutes(),
                duration.getSeconds());
        FFmpegKitConfig.enableStatisticsCallback(statisticsCallback);
        FFmpegKit.executeAsync(cmd, wrappedExecuteCallback);
    }

    String findCommand(String type) {
        File file = new File(info.localOutputPath);
        info.localOutputPath = file.getParent() + File.separator + file.getName().split("\\.")[0];
        switch (type) {
            case FFMPEGType.MERGE:
                return findEncodeType()+" -force_key_frames 0";
            case FFMPEGType.HLS_DOWNLOAD:
                info.localOutputMime = MIMEType.VIDEO_MP4;
                info.localOutputPath += ".mp4";
                return "-i \"" + info.hlsURL + "\" -codec copy " + info.localOutputPath;
            case FFMPEGType.TRIM_VIDEO:
                return "-i \"%s\" -c copy -map 0 -segment_time %s:%s:%s -f segment -reset_timestamps 1 " + info.localOutputPath + "/%%03d" + ".mp4";
            case FFMPEGType.RE_ENCODE_AS_MP3:
                return "-i \"%s\" -c:a libmp3lame -q:a 8 \"%s\".mp3";
            // FIXME: 19-04-2022 I'm not worthy to recode entire VP9 webm as H264 MP4 :(
            case FFMPEGType.RE_ENCODE_AS_MP4:
                return "-i \"%s\".webm -crf 23 -c:v libx264 -cpu-used 5 -deadline realtime -preset ultrafast \"%s\".mp4";
        }
        return null;
    }

    public void reEncodeToMp3(StatisticsCallback statisticsCallback) {
        File audioFile = new File(info.videoPath);
        String name = audioFile.getName().split("\\.")[0];
        String cmd = findCommand(FFMPEGType.RE_ENCODE_AS_MP3);
        name = audioFile.getParent() + "/" + name;
        cmd = String.format(cmd, name, name);
        FFmpegKitConfig.enableStatisticsCallback(statisticsCallback);
        FFmpegKit.executeAsync(cmd, wrappedExecuteCallback);
    }

    public void reEncodeToMp4(StatisticsCallback statisticsCallback) {
        File videoFile = new File(info.videoPath);
        String name = videoFile.getName().split("\\.")[0];
        String cmd = findCommand(FFMPEGType.RE_ENCODE_AS_MP4);
        name = videoFile.getParent() + "/" + name;
        cmd = String.format(cmd, name, name);
        FFmpegKitConfig.enableStatisticsCallback(statisticsCallback);
        FFmpegKit.executeAsync(cmd, wrappedExecuteCallback);
    }

    private String findEncodeType() {
        if (MIMEType.AUDIO_WEBM.equals(info.audioMime) && MIMEType.VIDEO_WEBM.equals(info.videoMime)) {
            info.localOutputPath += ".webm";
            info.localOutputMime = MIMEType.VIDEO_WEBM;
            return "-i %s -i %s -c copy %s";
        } else if (MIMEType.AUDIO_WEBM.equals(info.audioMime) && MIMEType.VIDEO_MP4.equals(info.videoMime)) {
            info.localOutputPath += ".mp4";
            info.localOutputMime = MIMEType.VIDEO_MP4;
            return "-i %s -i %s -c:v copy -c:a aac %s";
        } else if (MIMEType.AUDIO_MP4.equals(info.audioMime) && MIMEType.VIDEO_WEBM.equals(info.videoMime)) {
            info.localOutputPath += ".webm";
            info.localOutputMime = MIMEType.VIDEO_WEBM;
            return "-i %s -i %s -c:v copy -c:a libopus %s";
        } else if (MIMEType.AUDIO_MP4.equals(info.audioMime) && MIMEType.VIDEO_MP4.equals(info.videoMime)) {
            info.localOutputPath += ".mp4";
            info.localOutputMime = MIMEType.VIDEO_MP4;
            return "-i %s -i %s -c copy %s";
        }
        return null;
    }


    public void downloadHLS(LogCallback logCallback) {
        String command = findCommand(FFMPEGType.HLS_DOWNLOAD);
        FFmpegKit.executeAsync(command, wrappedExecuteCallback, logCallback, null);
    }

    public FFMPEGInfo getInfo() {
        return info;
    }

    public void interrupt() {
        FFmpegKit.cancel();
    }


}
