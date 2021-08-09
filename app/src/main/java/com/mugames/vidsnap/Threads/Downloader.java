package com.mugames.vidsnap.Threads;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;


import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;
import com.mugames.vidsnap.PostProcessor.FFMPEG;
import com.mugames.vidsnap.PostProcessor.FFmpegCallbacks;
import com.mugames.vidsnap.PostProcessor.ReflectionInterfaces;
import com.mugames.vidsnap.R;

import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.PostProcessor.FFMPEGInfo;
import com.mugames.vidsnap.Utility.FileUtil;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.ViewModels.MainActivityViewModel;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.mugames.vidsnap.NotificationChannelCreator.NOTIFY_DOWNLOADING;
import static com.mugames.vidsnap.Utility.Statics.COMMUNICATOR;
import static com.mugames.vidsnap.Utility.Statics.DOWNLOADED;
import static com.mugames.vidsnap.Utility.Statics.DOWNLOAD_SPEED;
import static com.mugames.vidsnap.Utility.Statics.ERROR_DOWNLOADING;
import static com.mugames.vidsnap.Utility.Statics.OUTFILE_URI;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_DONE;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_FAILED;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_UPDATE;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_UPDATE_AUDIO;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_UPDATE_MERGING;
import static com.mugames.vidsnap.Utility.Statics.PROGRESS_UPDATE_VIDEO;
import static com.mugames.vidsnap.Utility.Statics.TOTAL_SIZE;

public class Downloader extends Service {
    String TAG = Statics.TAG + ":Downloader";
    int activeDownload;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            activeDownload += 1;
            RealDownloader downloader = new RealDownloader(getApplicationContext(), intent, startId);
        } catch (Exception e) {
            Log.e(TAG, "onStartCommand: ", e);
            Toast.makeText(getApplicationContext(), "Download failed", Toast.LENGTH_LONG).show();
        }

        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    class RealDownloader {
        String TEMP_VIDEO_NAME = "MUvideo";
        String TEMP_AUDIO_NAME = "MUaudio";
        String TEMP_RESULT_NAME = "final";

        //storage/emulated/0/Download/
        //storage/emulated/0/Aaaaa/

        int PROCESS = 0;

        String path;

        Uri outputUri;

        NotificationCompat.Builder builder;
        ResultReceiver receiver;
        NotificationManagerCompat manager;

        BufferedOutputStream outputStream;

        DownloadDetails details;

        int ran;
        long file_size;

        int startId;

        int val_cache;


        long passed = 0;
        long startTime = System.nanoTime();
        long startBytes = 0;
        double speed = -10;
        private int notificationVal;
        boolean isFirst = true;
        double passedTime;

        Context context;
        Intent intent;

        public RealDownloader(Context context, Intent intent, int startId) {
            this.context = context;
            this.intent = intent;
            this.startId = startId;
            init();
        }

        private void init() {
            manager = NotificationManagerCompat.from(context);
            ran = new Random().nextInt();

            path = context.getExternalFilesDir("").getPath();

            TEMP_AUDIO_NAME = path + "/" + TEMP_AUDIO_NAME;
            TEMP_VIDEO_NAME = path + "/" + TEMP_VIDEO_NAME;
            TEMP_RESULT_NAME = path + "/" + TEMP_RESULT_NAME;

            details = intent.getParcelableExtra(COMMUNICATOR);

            ArrayList<String> chunkURLs = null;

            if (details.chuncksPath != null) {
                String line = (String) FileUtil.loadFile(details.chuncksPath, String.class);
                if (line != null) {
                    String[] links = line.split(",");
                    chunkURLs = new ArrayList<>(Arrays.asList(links));
                }
            }


            DocumentFile directory;
            try {
                directory = DocumentFile.fromTreeUri(context, details.pathUri);
                DocumentFile file = directory.createFile(MIMEType.VIDEO_MP4, details.fileName);
                if (file != null)
                    outputUri = file.getUri();
            } catch (IllegalArgumentException e) {
                outputUri = Uri.fromFile(new File(FileUtil.GetValidFile(details.pathUri.getPath() + File.separator, details.fileName, details.fileType)));
            }


            receiver = details.receiver;
            file_size = details.fileSize;

            PROCESS = PROGRESS_UPDATE;

            PendingIntent downloading_PendingIntent = PendingIntent.getActivity(getBaseContext(), 10, new Intent(context, MainActivity.class), 0);

            builder = new NotificationCompat.Builder(context, NOTIFY_DOWNLOADING);
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setProgress(100, 0, false)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentIntent(downloading_PendingIntent);

            startForeground(ran, builder.build());

            ArrayList<String> finalChunkURLs = chunkURLs;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean isMergeNeeded = false;

                    try {
                        builder.setContentTitle("Downloading " + details.fileName + details.fileType);

                        try {
                            outputStream = new BufferedOutputStream(context.getContentResolver().openOutputStream(outputUri));
                        } catch (FileNotFoundException e) {
                            File outputFile = new File(outputUri.getPath());
                            outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                        }

                        if (finalChunkURLs == null || finalChunkURLs.size() == 0) {
                            String video_url = details.videoURL;
                            String audio_url = details.audioURL;

                            video_url = video_url.replaceAll("\\\\", "");

                            if (audio_url != null) {
                                MainActivityViewModel.service_in_use = true;
                                isMergeNeeded = true;
                                audio_url = audio_url.replaceAll("\\\\", "");
                                PROCESS = PROGRESS_UPDATE_VIDEO;
                                builder.setContentTitle("Downloading Video");
                                outputStream = new BufferedOutputStream(new FileOutputStream(TEMP_VIDEO_NAME));
                                Download(new URL(video_url));

                                ResetMetrics();
                                PROCESS = PROGRESS_UPDATE_AUDIO;
                                outputStream = new BufferedOutputStream(new FileOutputStream(TEMP_AUDIO_NAME));

                                builder.setContentTitle("Downloading audio");
                                URL aURl = new URL(audio_url);
                                file_size = aURl.openConnection().getContentLength();
                                Download(aURl);

                                outputStream = new BufferedOutputStream(context.getContentResolver().openOutputStream(outputUri));
                                ResetMetrics();
                            } else Download(new URL(video_url));

                        } else {
                            for (String s : finalChunkURLs) {
                                s = s.replaceAll("\\\\", "");
                                Download(new URL(s));
                            }
                        }
                    } catch (IOException e) {
                        Bundle bundle = new Bundle();
                        bundle.putString(ERROR_DOWNLOADING, String.valueOf(e));
                        Log.e(TAG, "run: ", e);
                        onDownloadFailed(bundle);
                        return;
                    }
                    if (!isMergeNeeded) onDoneDownload();
                    else loadAndMerge();

                }
            }).start();
        }

        private void onDownloadFailed(Bundle data) {
            UpdateUI(PROGRESS_FAILED, data);
            manager.cancel(ran);
            activeDownload -= 1;
            if (activeDownload == 0)
                stopForeground(true);
        }

        void onDoneDownload() {
            Bundle bundle = new Bundle();
            bundle.putString(OUTFILE_URI, outputUri.toString());

            UpdateUI(PROGRESS_DONE, bundle);
            manager.cancel(ran);
            activeDownload -= 1;
            Log.d(TAG, "onDoneDownload: " + activeDownload);
            if (activeDownload == 0)
                stopForeground(true);
        }

        void loadAndMerge(){
            FFMPEG.dexPath = context.getExternalFilesDir("libs").getPath() + File.separator + "ffmpeg.jar";
            FFMPEG.tempLibsPath = new File(context.getDir("dex", MODE_PRIVATE), "ffmpeg.jar").getAbsolutePath();
            FFMPEG.jniPath = context.getExternalFilesDir("libs") + File.separator + "jni" + File.separator;
            FFMPEG.filesDir = context.getFilesDir().getAbsolutePath() + File.separator;

            FFMPEG.loadSOFiles(context.getExternalFilesDir("libs") + File.separator + "jni" + File.separator,soLoadCallbacks);

            Log.e(TAG, FFMPEG.getString());
        }

        void mergeFiles() {
            int videoLength = MediaPlayer.create(context, Uri.fromFile(new File(TEMP_VIDEO_NAME))).getDuration();

            builder.setContentTitle("Merging Video...");
            notificationVal = 0;
            final Timer notifyTimer = notifyProgress();



//            FFMPEG.mergeAsync(new FFMPEGInfo(details.mimeVideo,
//                    details.mimeAudio, TEMP_VIDEO_NAME, TEMP_AUDIO_NAME, TEMP_RESULT_NAME),
//                    new ReflectionInterfaces.StatisticsCallback() {
//                @Override
//                public void apply(Class<?> statisticsKlass,Object instance) {
//                    float progress = 0;
//                    try {
//                        Method getTime = statisticsKlass.getMethod("getTime");
//                        progress = (Float.parseFloat(String.valueOf(getTime.invoke(instance))) / videoLength) * 100;
//                        Bundle progressData = new Bundle();
//                        int val = (int) progress;
//                        notificationVal = val;
//                        progressData.putInt(PROGRESS, val);
//                        UpdateUI(PROGRESS_UPDATE_MERGING, progressData);
//                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }, new ReflectionInterfaces.FFMPEGCallback() {
//                @Override
//                public void apply(Class<?> sessionKlass,Object instance, String outputPath) {
//                    Bundle progressData = new Bundle();
//
//                    try {
//                        FileInputStream inputStream = new FileInputStream(new File(outputPath));
//                        FileChannel inChannel = inputStream.getChannel();
//                        FileChannel outChannel = new FileOutputStream(getContentResolver().openFileDescriptor(outputUri,"w").getFileDescriptor()).getChannel();
//                        inChannel.transferTo(0, inChannel.size(), outChannel);
//                        inChannel.close();
//                        outChannel.close();
//                        inputStream.close();
//                        outputStream.close();
//                        notifyTimer.cancel();
//                        onDoneDownload();
//                        FileUtil.deleteFile(outputPath);
//                        FileUtil.deleteFile(TEMP_AUDIO_NAME);
//                        FileUtil.deleteFile(TEMP_VIDEO_NAME);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    progressData.putInt(PROGRESS, 100);
//                    UpdateUI(PROGRESS_UPDATE_MERGING, progressData);
//                }
//            });
            FFMPEG.mergeAsync(new FFMPEGInfo(details.mimeVideo,
                            details.mimeAudio, TEMP_VIDEO_NAME, TEMP_AUDIO_NAME, TEMP_RESULT_NAME),
                    new StatisticsCallback() {
                        @Override
                        public void apply(Statistics statistics) {
                            float progress = 0;
                            progress = (Float.parseFloat(String.valueOf(statistics.getTime())) / videoLength) * 100;
                            Bundle progressData = new Bundle();
                            int val = (int) progress;
                            notificationVal = val;
                            progressData.putInt(PROGRESS, val);
                            UpdateUI(PROGRESS_UPDATE_MERGING, progressData);
                        }
                    }, new FFmpegCallbacks() {
                        @Override
                        public void apply(Session session, String outputPath) {
                            Bundle progressData = new Bundle();

                            try {
                                FileInputStream inputStream = new FileInputStream(new File(outputPath));
                                FileChannel inChannel = inputStream.getChannel();
                                FileChannel outChannel = new FileOutputStream(getContentResolver().openFileDescriptor(outputUri, "w").getFileDescriptor()).getChannel();
                                inChannel.transferTo(0, inChannel.size(), outChannel);
                                inChannel.close();
                                outChannel.close();
                                inputStream.close();
                                outputStream.close();
                                notifyTimer.cancel();
                                onDoneDownload();
                                FileUtil.deleteFile(outputPath);
                                FileUtil.deleteFile(TEMP_AUDIO_NAME);
                                FileUtil.deleteFile(TEMP_VIDEO_NAME);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            progressData.putInt(PROGRESS, 100);
                            UpdateUI(PROGRESS_UPDATE_MERGING, progressData);
                        }
                    });

        }

        ReflectionInterfaces.SOLoadCallbacks soLoadCallbacks = new ReflectionInterfaces.SOLoadCallbacks() {
            @Override
            public void onSOLoadingSuccess() {
                mergeFiles();
            }

            @Override
            public void onSOLoadingFailed(Exception e) {
                Log.e(TAG, "onSOLoadingFailed: ",e );
            }
        };

        void Download(URL downloadingURL) {
            try {

                URLConnection connection = downloadingURL.openConnection();

                BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());

                byte[] buff = new byte[8192];
                int length;


                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        calculateSpeed();
                        uiSpeed();
                    }
                }, 0, 1000);

                Timer notificationTimer = notifyProgress();

                while ((length = inputStream.read(buff)) > 0) {
                    passed += length;
                    notificationVal = (int) (passed * 100 / file_size);
                    outputStream.write(buff, 0, length);
                }

                outputStream.close();
                timer.cancel();
                notificationTimer.cancel();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        void uiSpeed() {
            Bundle progressData = new Bundle();
            progressData.putInt(PROGRESS, notificationVal);
            progressData.putLong(TOTAL_SIZE, file_size);
            progressData.putLong(DOWNLOADED, passed);
            progressData.putLong(DOWNLOAD_SPEED, (long) (speed * 1000));

            UpdateUI(PROCESS, progressData);

        }

        void calculateSpeed() {
            long elapsedTime = System.nanoTime() - startTime;
            passedTime = (double) elapsedTime / 1_000_000_000.0;

            long passedBytes = (passed - startBytes) / 1024;
            speed = (double) passedBytes / passedTime;

            startTime = System.nanoTime();
            startBytes = passed;


            Log.d(TAG, "calculateSpeed: " + speed);
        }

        private void ResetMetrics() {
            passed = 0;
            val_cache = 0;
            startTime = System.nanoTime();
            startBytes = 0;
            speed = -10;
            isFirst = true;
        }

        Timer notifyProgress() {
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Log.d(TAG, "Notify: Called");
                    builder.setProgress(100, notificationVal, false);
                    builder.setContentText(notificationVal + " %");
                    manager.notify(ran, builder.build());
                }
            }, 0, 3000);
            return timer;
        }

        void UpdateUI(int resultCode, Bundle progressData) {
            Log.d(TAG, "UpdateUI: " + resultCode);
            receiver.send(resultCode, progressData);
        }
    }


}
