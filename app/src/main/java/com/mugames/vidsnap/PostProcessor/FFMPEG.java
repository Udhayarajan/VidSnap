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

package com.mugames.vidsnap.PostProcessor;


import android.content.Context;
import android.util.Log;

import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.LogCallback;
import com.arthenica.ffmpegkit.NativeLoader;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.StatisticsCallback;
import com.mugames.vidsnap.Utility.FileUtil;
import com.mugames.vidsnap.Utility.MIMEType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import dalvik.system.DexClassLoader;

import static com.mugames.vidsnap.Utility.Statics.TAG;

public class FFMPEG {
//    static DexClassLoader loader;
    public static String jniPath;
    public static String filesDir;
    private static int INSTANCE_COUNT;
//    public static String dexPath;
//    public static String tempLibsPath;

    public static boolean isLibLoaded = false;

    ExecuteCallback executeCallback;
    FFMPEGInfo info;

    String opFilePath;


    public static void newFFMPEGInstance(FFMPEGInfo ffmpegInfo, Context context, ReflectionInterfaces.SOLoadCallbacks soLoadCallbacks){
        INSTANCE_COUNT++;
        new FFMPEG(ffmpegInfo,context,soLoadCallbacks);
    }
    ExecuteCallback wrappedExecuteCallback = new ExecuteCallback() {
        @Override
        public void apply(Session session) {
            deleteLibs();
            if(executeCallback!=null)
                executeCallback.apply(session);
        }
    };


    public FFMPEG(FFMPEGInfo ffmpegInfo, Context context, ReflectionInterfaces.SOLoadCallbacks soLoadCallbacks) {
        this.info = ffmpegInfo;
        String libsPath = context.getExternalFilesDir("libs").getAbsolutePath();
        if(jniPath==null) jniPath = libsPath + File.separator + "jni" + File.separator;

        if(!FileUtil.isFileExists(jniPath)) {
            try {
                FileUtil.unzip(new File(libsPath, "lib.zip"), new File(libsPath));
            } catch (IOException e) {
                Log.e(TAG, "onReceive: ", e);
                e.printStackTrace();
            }
        }
        if(filesDir==null) filesDir = context.getFilesDir().getAbsolutePath() + File.separator;
        loadSOFiles(jniPath,soLoadCallbacks);
    }

    public void setExecuteCallback(ExecuteCallback executeCallback) {
        this.executeCallback = executeCallback;
    }


    static void deleteLibs(){
        INSTANCE_COUNT--;
        Log.e(TAG, "deleteLibs: "+INSTANCE_COUNT);
        if(INSTANCE_COUNT==0)
            FileUtil.deleteFolder(jniPath);
    }

    //    static DexClassLoader getLoader() throws IOException {
//        if (loader == null) {
//            loadDEX();
//            loader = new DexClassLoader(tempLibsPath, null, filesDir, FFMPEG.class.getClassLoader());
//        }
//        return loader;
//    }

//    static void loadDEX() throws IOException {
//        File src = new File(dexPath);
//        FileInputStream inputStream = new FileInputStream(src.getAbsolutePath());
//        FileOutputStream outputStream = new FileOutputStream(tempLibsPath);
//        FileChannel inChannel = inputStream.getChannel();
//        FileChannel outChannel = outputStream.getChannel();
//        inChannel.transferTo(0, inChannel.size(), outChannel);
//        inputStream.close();
//        outputStream.close();
//    }


    public String getOutputPath() {
        return opFilePath;
    }

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
        if(isLibLoaded){
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
        } catch (IOException e) {
            e.printStackTrace();
            callbacks.onSOLoadingFailed(e);
        }
    }

    public void mergeAsync(StatisticsCallback statisticsCallback) {

//        try {
//            String command = findCommand(FFMPEGType.MERGE, info.mime_audio, info.mime_video);
//            opFilePath = String.format(opFilePath, info.outPut);
//
//            File jniDirectory = new File(jniPath);
//            File[] sos = jniDirectory.listFiles();
//
//
//            if(sos!=null){
//                loadJni(new File(jniPath,"libc++_shared.so"));
//                loadJni(new File(jniPath,"libavutil.so"));
//                loadJni(new File(jniPath,"libswresample.so"));
//                loadJni(new File(jniPath,"libswscale.so"));
//                loadJni(new File(jniPath,"libavcodec.so"));
//                loadJni(new File(jniPath,"libavformat.so"));
//                loadJni(new File(jniPath,"libavfilter.so"));
//
//                for (File file:sos) {
//                    if(file.getName().equals("libc++_shared.so")) continue;
//                    if(file.getName().equals("libavutil.so")) continue;
//                    if(file.getName().equals("libswresample.so")) continue;
//                    if(file.getName().equals("libavfilter.so")) continue;
//                    if(file.getName().equals("libswscale.so")) continue;
//                    if(file.getName().equals("libavformat.so")) continue;
//                    if(file.getName().equals("libavcodec.so")) continue;
//                    loadJni(file);
//                }
//            }
//
//
//            Class<?> Statistics = Class.forName("com.arthenica.ffmpegkit.Statistics",true,getLoader());
//            Class<?> StatisticsCallback = Class.forName("com.arthenica.ffmpegkit.StatisticsCallback",true,getLoader());
//
//
//            InvocationHandler statisticsInvocationHandler = (proxy, method, args) -> {
//                Log.e(TAG, "invoke: "+"Static Executing");
//                statisticsCallback.apply(Statistics,args[0]);
//                return null;
//            };
//
//
//            Class<?> FFmpegKitConfig = Class.forName("com.arthenica.ffmpegkit.FFmpegKitConfig",true,getLoader());
//            Method enableStatisticsCallback= FFmpegKitConfig.getMethod("enableStatisticsCallback",StatisticsCallback);
//            enableStatisticsCallback.invoke(FFmpegKitConfig, Proxy.newProxyInstance(FFmpegKitConfig.getClassLoader(),new Class[]{StatisticsCallback}, statisticsInvocationHandler));
//
//            Class<?> Session = Class.forName("com.arthenica.ffmpegkit.Session",true,getLoader());
//            Class<?> ExecuteCallback = Class.forName("com.arthenica.ffmpegkit.ExecuteCallback",true,getLoader());
//
//
//            InvocationHandler executeInvocationHandler = (proxy, method, args) -> {
//                Log.e(TAG, "invoke: "+"Execute Executing");
//                ffmpegCallback.apply(Session,args[0],opFilePath);
//                return null;
//            };
//
//            Class<?> FFmpegKit = Class.forName("com.arthenica.ffmpegkit.FFmpegKit",true,getLoader());
//            Method executeAsync = FFmpegKit.getMethod("executeAsync",String.class,ExecuteCallback);
//            executeAsync.invoke(FFmpegKit,String.format(command, info.videoPath, info.audioPath, opFilePath),Proxy.newProxyInstance(FFmpegKit.getClassLoader(),new Class[]{ExecuteCallback},executeInvocationHandler));
//
//
//        }catch (Exception e){
//            Log.e(TAG, "mergeAsync: ",e);
//        }


        String command = findCommand(FFMPEGType.MERGE, info);
        opFilePath = String.format(opFilePath, info.outPut);
        FFmpegKitConfig.enableStatisticsCallback(statisticsCallback);
        FFmpegKit.executeAsync(String.format(command, info.videoPath, info.audioPath, opFilePath), wrappedExecuteCallback);
    }


    String findCommand(String type, FFMPEGInfo info) {
        switch (type) {
            case FFMPEGType.MERGE:
                return findEncodeType(info.mime_audio, info.mime_video);
            case FFMPEGType.HLS_DOWNLOAD:
                return "-i \""+info.hlsURL+"\" -codec copy "+info.videoPath;
        }
        return null;
    }

    private  String findEncodeType(String audio_mime, String video_mime) {
        if (MIMEType.AUDIO_WEBM.equals(audio_mime) && MIMEType.VIDEO_WEBM.equals(video_mime)) {
            opFilePath = "%s.webm";
            return "-i %s -i %s -c copy %s";
        } else if (MIMEType.AUDIO_WEBM.equals(audio_mime) && MIMEType.VIDEO_MP4.equals(video_mime)) {
            opFilePath = "%s.mp4";
            return "-i %s -i %s -c:v copy -c:a aac %s";
        } else if (MIMEType.AUDIO_MP4.equals(audio_mime) && MIMEType.VIDEO_WEBM.equals(video_mime)) {
            opFilePath = "%s.webm";
            return "-i %s -i %s -c:v copy -c:a libopus %s";
        } else if (MIMEType.AUDIO_MP4.equals(audio_mime) && MIMEType.VIDEO_MP4.equals(video_mime)) {
            opFilePath = "%s.mp4";
            return "-i %s -i %s -c copy %s";
        }
        return null;
    }

    public void downloadHLS(LogCallback logCallback){
        info.outPut += ".mp4";
        String command = findCommand(FFMPEGType.HLS_DOWNLOAD,info);
        FFmpegKit.executeAsync(command, wrappedExecuteCallback, logCallback,null);
    }
}
