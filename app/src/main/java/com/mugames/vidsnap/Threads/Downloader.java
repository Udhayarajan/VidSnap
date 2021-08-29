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

package com.mugames.vidsnap.Threads;

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
import static com.mugames.vidsnap.VidSnapApp.NOTIFY_DOWNLOADING;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;

import com.mugames.vidsnap.PostProcessor.FFMPEG;
import com.mugames.vidsnap.PostProcessor.FFMPEGInfo;
import com.mugames.vidsnap.PostProcessor.ReflectionInterfaces;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.FileUtil;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.Statics;
import com.mugames.vidsnap.ViewModels.MainActivityViewModel;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.HttpUrlConnectionDownloader;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.FetchObserver;
import com.tonyodev.fetch2core.MutableExtras;
import com.tonyodev.fetch2core.Reason;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

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

    class RealDownloader implements FetchObserver<Download> {
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


        Timer notificationTimer;

        Context context;
        Intent intent;
        ArrayList<String> chunkURLs = null;

        public RealDownloader(Context context, Intent intent, int startId) {
            this.context = context;
            this.intent = intent;
            this.startId = startId;
            init();
        }

        private void init() {
            manager = NotificationManagerCompat.from(context);
            ran = new Random().nextInt();

            path = context.getExternalFilesDir("").getPath() + File.separator;

            TEMP_VIDEO_NAME = FileUtil.getValidFile(path, ran + "Video", ".muvideo");
            TEMP_AUDIO_NAME = FileUtil.getValidFile(path, ran + "Audio", ".muaudio");
            TEMP_RESULT_NAME = FileUtil.getValidFile(path, ran + "FullVideo", ".muout");

            details = intent.getParcelableExtra(COMMUNICATOR);


            if (details.chunksPath != null) {
                String line = (String) FileUtil.loadFile(details.chunksPath, String.class);
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
                outputUri = Uri.fromFile(new File(FileUtil.getValidFile(details.pathUri.getPath() + File.separator, details.fileName, details.fileType)));
            }


            receiver = details.receiver;
            file_size = details.videoSize;
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

            if (details.audioURL != null) MainActivityViewModel.service_in_use = true;

            new Thread(() -> {

                builder.setContentTitle("Downloading " + details.fileName + details.fileType);

                if (details.chunkUrl == null || details.chunkUrl.isEmpty()) {
                    String video_url = details.videoURL;
                    video_url = video_url.replaceAll("\\\\", "");
                    PROCESS = PROGRESS_UPDATE_VIDEO;
                    download(video_url, TEMP_VIDEO_NAME, PROGRESS_UPDATE_VIDEO);

                } else {
                    initFFMPEG(hlsSoLoadCallbacks);
                }


            }).start();
        }

        void downloadChunk(FFMPEG ffmpeg) {
            AtomicInteger got = new AtomicInteger();
            final Timer notifyTimer = notifyProgress();
            ffmpeg.setExecuteCallback(session -> {
                Log.e(TAG, "apply: HLS Successfull");
                Bundle progressData = new Bundle();
                onDoneDownload(ffmpeg.getOutputPath());
                progressData.putInt(PROGRESS, 100);
                sendBundle(PROGRESS_UPDATE_MERGING, progressData);
                notifyTimer.cancel();
            });

            ffmpeg.downloadHLS(log -> {
                if (log.getMessage().contains("Opening")) got.getAndIncrement();
                int progress = (int) (got.get() * 100 / details.chunkCount);
                Bundle bundle = new Bundle();
                bundle.putInt(PROGRESS, progress);
                notificationVal = progress;
                sendBundle(PROGRESS_UPDATE_VIDEO, bundle);
            });
        }

        void fetch2Downloaded(Download download) {
            notificationTimer.cancel();

            int currentCode = download.getExtras().getInt(PROGRESS, PROGRESS_UPDATE_VIDEO);


            if (currentCode == PROGRESS_UPDATE_VIDEO) {
                if (details.audioURL == null)
                    onDoneDownload(null);
                else {
                    String audio_url = details.audioURL;
                    audio_url = audio_url.replaceAll("\\\\", "");
                    file_size = details.audioSize;
                    PROCESS = PROGRESS_UPDATE_AUDIO;
                    download(audio_url, TEMP_AUDIO_NAME, PROGRESS_UPDATE_AUDIO);
                }
            } else if (currentCode == PROGRESS_UPDATE_AUDIO)
                initFFMPEG(mergeSOCallback);

        }

        private void initFFMPEG(ReflectionInterfaces.SOLoadCallbacks soLoadCallbacks) {
            FFMPEGInfo info = new FFMPEGInfo();
            info.videoPath = TEMP_VIDEO_NAME;
            info.audioPath = TEMP_AUDIO_NAME;
            info.outPut = TEMP_RESULT_NAME;
            info.hlsURL = details.chunkUrl;
            info.mime_video = details.mimeVideo;
            info.mime_audio = details.mimeAudio;

            FFMPEG.newFFMPEGInstance(info, context, soLoadCallbacks);
        }

        private void onDownloadFailed(Bundle data) {
            sendBundle(PROGRESS_FAILED, data);
            manager.cancel(ran);
            activeDownload -= 1;
            if (activeDownload == 0)
                stopForeground(true);
        }

        void onDoneDownload(String finalPath) {
            if (finalPath == null) finalPath = TEMP_VIDEO_NAME;
            copyVideoToDestination(finalPath);
            Bundle bundle = new Bundle();
            bundle.putString(OUTFILE_URI, outputUri.toString());

            sendBundle(PROGRESS_DONE, bundle);
            manager.cancel(ran);
            activeDownload -= 1;
            Log.d(TAG, "onDoneDownload: " + activeDownload);
            if (activeDownload == 0)
                stopForeground(true);
        }

        void mergeFiles(FFMPEG ffmpeg) {
            int videoLength = MediaPlayer.create(context, Uri.fromFile(new File(TEMP_VIDEO_NAME))).getDuration();

            builder.setContentTitle("Merging Video...");
            notificationVal = 0;
            final Timer notifyTimer = notifyProgress();

            ffmpeg.setExecuteCallback(session -> {
                Bundle progressData = new Bundle();
                onDoneDownload(ffmpeg.getOutputPath());
                progressData.putInt(PROGRESS, 100);
                sendBundle(PROGRESS_UPDATE_MERGING, progressData);
                notifyTimer.cancel();
            });

            ffmpeg.mergeAsync(statistics -> {
                float progress = 0;
                progress = (Float.parseFloat(String.valueOf(statistics.getTime())) / videoLength) * 100;
                Bundle progressData = new Bundle();
                int val = (int) progress;
                notificationVal = val;
                progressData.putInt(PROGRESS, val);
                sendBundle(PROGRESS_UPDATE_MERGING, progressData);
            });
        }

        void copyVideoToDestination(String finalFile) {

            try {

                FileChannel inChannel = new FileInputStream(finalFile).getChannel();
                FileChannel outChannel;

                try {
                    outChannel = new FileOutputStream(getContentResolver().openFileDescriptor(outputUri, "w").getFileDescriptor()).getChannel();
                } catch (FileNotFoundException e) {
                    outChannel = new FileOutputStream(outputUri.getPath()).getChannel();
                }
                inChannel.transferTo(0, inChannel.size(), outChannel);
                inChannel.close();
                outChannel.close();

            } catch (IOException e) {
                Log.e(TAG, "copyVideoToDestination: ", e);
            }
            FileUtil.deleteFile(finalFile);
            FileUtil.deleteFile(TEMP_AUDIO_NAME);
            FileUtil.deleteFile(TEMP_VIDEO_NAME);
        }

        ReflectionInterfaces.SOLoadCallbacks mergeSOCallback = new ReflectionInterfaces.SOLoadCallbacks() {
            @Override
            public void onSOLoadingSuccess(FFMPEG ffmpeg) {
                mergeFiles(ffmpeg);
            }

            @Override
            public void onSOLoadingFailed(Exception e) {
                Log.e(TAG, "onSOLoadingFailed: ", e);
            }
        };

        ReflectionInterfaces.SOLoadCallbacks hlsSoLoadCallbacks = new ReflectionInterfaces.SOLoadCallbacks() {
            @Override
            public void onSOLoadingSuccess(FFMPEG ffmpegInstance) {
                downloadChunk(ffmpegInstance);
            }

            @Override
            public void onSOLoadingFailed(Exception e) {
                Log.e(TAG, "onSOLoadingFailed: ", e);
            }
        };


        void download(String url, String outPath, int whatDownloading) {
            notificationTimer = notifyProgress();

            FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(context)
                    .setHttpDownloader(new HttpUrlConnectionDownloader(com.tonyodev.fetch2core.Downloader.FileDownloaderType.PARALLEL))
                    .setDownloadConcurrentLimit(3)
                    .enableRetryOnNetworkGain(true)
                    .build();

            Fetch fetch = Fetch.Impl.getInstance(fetchConfiguration);


            url = url.replace("http://", "https://");

            Request request = new Request(url, outPath);
            MutableExtras extra = new MutableExtras();
            extra.putInt(PROGRESS, whatDownloading);
            request.setExtras(extra);


            fetch.attachFetchObserversForDownload(request.getId(), this).enqueue(request, result -> {
            }, error -> {
                Bundle bundle = new Bundle();
                bundle.putString(ERROR_DOWNLOADING, String.valueOf(error));
                Log.e(TAG, "run: " + error);
                onDownloadFailed(bundle);
            });

        }


        //        void Download(URL downloadingURL) {
//            try {
//
//                URLConnection connection = downloadingURL.openConnection();
//
//                BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
//
//                byte[] buff = new byte[8192];
//                int length;
//
//
//                Timer timer = new Timer();
//                timer.scheduleAtFixedRate(new TimerTask() {
//                    @Override
//                    public void run() {
//                        calculateSpeed();
//                        uiSpeed();
//                    }
//                }, 0, 1000);
//
//                Timer notificationTimer = notifyProgress();
//
//                while ((length = inputStream.read(buff)) > 0) {
//                    passed += length;
//                    notificationVal = (int) (passed * 100 / file_size);
//                    outputStream.write(buff, 0, length);
//                }
//
//                outputStream.close();
//                timer.cancel();
//                notificationTimer.cancel();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        }


        void uiSpeed() {
            Bundle progressData = new Bundle();
            progressData.putInt(PROGRESS, notificationVal);
            progressData.putLong(TOTAL_SIZE, file_size);
            progressData.putLong(DOWNLOADED, passed);
            progressData.putLong(DOWNLOAD_SPEED, (long) (speed * 1000));

            sendBundle(PROCESS, progressData);

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

        void sendBundle(int resultCode, Bundle progressData) {
            Log.d(TAG, "UpdateUI: " + resultCode);
            receiver.send(resultCode, progressData);
        }

        @Override
        public void onChanged(Download download, @NonNull Reason reason) {
            Log.e(TAG, "onChanged: " + reason);
            if (reason == Reason.DOWNLOAD_COMPLETED) {
                fetch2Downloaded(download);
            } else if (download.getDownloadedBytesPerSecond() != -1) {
                notificationVal = download.getProgress();
                passed = download.getDownloaded();
                speed = download.getDownloadedBytesPerSecond();
                Log.e(TAG, "onChanged: " + speed);
                speed /= 1024;
                uiSpeed();
            }
        }
    }


}
