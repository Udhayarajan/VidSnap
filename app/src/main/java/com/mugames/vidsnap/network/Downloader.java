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

package com.mugames.vidsnap.network;

import static com.mugames.vidsnap.utility.Statics.ACTION_CANCEL_DOWNLOAD;
import static com.mugames.vidsnap.utility.Statics.ACTIVE_DOWNLOAD;
import static com.mugames.vidsnap.utility.Statics.COMMUNICATOR;
import static com.mugames.vidsnap.utility.Statics.DOWNLOADED;
import static com.mugames.vidsnap.utility.Statics.DOWNLOAD_SPEED;
import static com.mugames.vidsnap.utility.Statics.ERROR_DOWNLOADING;
import static com.mugames.vidsnap.utility.Statics.FETCH_MESSAGE;
import static com.mugames.vidsnap.utility.Statics.FILE_MIME;
import static com.mugames.vidsnap.utility.Statics.ID_CANCEL_DOWNLOAD_DETAILS;
import static com.mugames.vidsnap.utility.Statics.IS_SHARE_ONLY_DOWNLOAD;
import static com.mugames.vidsnap.utility.Statics.OUTFILE_URI;
import static com.mugames.vidsnap.utility.Statics.PROGRESS;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_CANCELED;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_DEFAULT;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_DONE;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_FAILED;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_AUDIO;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_MERGING;
import static com.mugames.vidsnap.utility.Statics.PROGRESS_UPDATE_VIDEO;
import static com.mugames.vidsnap.utility.Statics.TOTAL_SIZE;
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
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.mugames.vidsnap.BuildConfig;
import com.mugames.vidsnap.VidSnapApp;
import com.mugames.vidsnap.postprocessor.FFMPEG;
import com.mugames.vidsnap.postprocessor.FFMPEGInfo;
import com.mugames.vidsnap.postprocessor.ReflectionInterfaces;
import com.mugames.vidsnap.R;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.utility.CancelDownloadReceiver;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.ui.activities.MainActivity;

import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
import com.tonyodev.fetch2.HttpUrlConnectionDownloader;
import com.tonyodev.fetch2.Request;
import com.tonyodev.fetch2core.FetchObserver;
import com.tonyodev.fetch2core.MutableExtras;
import com.tonyodev.fetch2core.Reason;
import com.tonyodev.fetch2okhttp.OkHttpDownloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;

public class Downloader extends Service {
    final String TAG = Statics.TAG + ":Downloader";
    volatile int activeDownload;
    volatile static ArrayList<RealDownloader> downloaders = new ArrayList<>();

    private static PendingIntent getActivityOpenerIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(ACTIVE_DOWNLOAD, true);
        return PendingIntent.getActivity(context, 10, intent, 0);
    }

    public static synchronized void cancelDownload(int detailsId) {
        for (RealDownloader downloader : downloaders) {
            if (downloader.details.id == detailsId) {
                downloader.cancelDownload();
                break;
            }
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (Downloader.class) {
            activeDownload += 1;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), VidSnapApp.NOTIFY_DOWNLOADER_SERVICE)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setContentTitle("Running in background")
                    .setContentText(String.format("%s Video(s) downloading in background",activeDownload))
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentIntent(getActivityOpenerIntent(getApplicationContext()));
            startForeground(10, builder.build());
            RealDownloader downloader = new RealDownloader(getApplicationContext(), intent, startId);
            downloaders.add(downloader);
        }

        return START_NOT_STICKY;
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

        Thread currentThread;
        boolean isCanceled = false;

        int PROCESS = 0;

        String path;

        Uri outputUri;

        NotificationCompat.Builder builder;
        ResultReceiver receiver;
        NotificationManagerCompat manager;



        DownloadDetails details;

        int ran;
        long file_size;

        final int startId;

        long passed = 0;

        Long speed = -10L;
        private int notificationVal;


        Timer notificationTimer;

        final Context context;
        final Intent intent;

        FFMPEG ffmpegInstance;

        Request request;
        Fetch fetch;

        public RealDownloader(Context context, Intent intent, int startId) {
            this.context = context;
            this.intent = intent;
            this.startId = startId;
            init();
        }

        private void init() {
            manager = NotificationManagerCompat.from(context);
            ran = new Random().nextInt();

            path = AppPref.getInstance(context).getCachePath("");

            TEMP_VIDEO_NAME = FileUtil.getValidFile(path, ran + "Video", "mp4");
            TEMP_AUDIO_NAME = FileUtil.getValidFile(path, ran + "Audio", "mp3");
            TEMP_RESULT_NAME = FileUtil.getValidFile(path, ran + "FullVideo", "mp4");

            details = intent.getParcelableExtra(COMMUNICATOR);


            receiver = details.receiver;
            file_size = details.videoSize;
            PROCESS = PROGRESS_UPDATE;


            Intent cancelIntent = new Intent(context, CancelDownloadReceiver.class);
            cancelIntent.putExtra(ID_CANCEL_DOWNLOAD_DETAILS, details.id);

            PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, ran, cancelIntent, PendingIntent.FLAG_ONE_SHOT);


            builder = new NotificationCompat.Builder(context, NOTIFY_DOWNLOADING);
            builder.setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_cancel, "Cancel", cancelPendingIntent)
                    .setProgress(100, 0, false)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentIntent(getActivityOpenerIntent(context));

            manager.notify(ran,builder.build());
            currentThread = new Thread(() -> {

                builder.setContentTitle("Downloading " + details.fileName + details.fileType);

                if (details.chunkUrl == null || details.chunkUrl.isEmpty()) {
                    String video_url = details.videoURL;
                    video_url = video_url.replaceAll("\\\\", "");
                    PROCESS = PROGRESS_UPDATE_VIDEO;
                    download(video_url, TEMP_VIDEO_NAME, PROGRESS_UPDATE_VIDEO);

                } else {
                    initFFMPEG(hlsSoLoadCallbacks);
                }


            });
            currentThread.start();
        }

        public void cancelDownload() {
            isCanceled = true;
            fetch.remove(request.getId());
            new Thread(() -> FileUtil.deleteFile(TEMP_AUDIO_NAME, null)).start();
            new Thread(() -> FileUtil.deleteFile(TEMP_VIDEO_NAME, null)).start();
            if (ffmpegInstance != null) {
                ffmpegInstance.interrupt();
                Log.e(TAG, "cancelDownload: " + ffmpegInstance.getInfo().localOutputPath);
                new Thread(() -> FileUtil.deleteFile(ffmpegInstance.getInfo().localOutputPath, null)).start();
            }
            removeDownloader();
            sendBundle(PROGRESS_CANCELED, null);
            currentThread.interrupt();
        }

        void downloadChunk(FFMPEG ffmpeg) {
            AtomicInteger got = new AtomicInteger();
            notificationTimer = notifyProgress();
            ffmpeg.setExecuteCallback(session -> {
                Bundle progressData = new Bundle();
                onDoneDownload(ffmpeg.getInfo().localOutputPath, ffmpeg.getInfo().localOutputMime);
                progressData.putInt(PROGRESS, 100);
                sendBundle(PROGRESS_UPDATE_MERGING, progressData);
                notificationTimer.cancel();
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
                    onDoneDownload(null, null);
                else {
                    String audio_url = details.audioURL;
                    audio_url = audio_url.replaceAll("\\\\", "");
                    file_size = details.audioSize;
                    PROCESS = PROGRESS_UPDATE_AUDIO;
                    download(audio_url, TEMP_AUDIO_NAME, PROGRESS_UPDATE_AUDIO);
                }
            } else if (currentCode == PROGRESS_UPDATE_AUDIO) {
                initFFMPEG(mergeSOCallback);
            }

        }

        /**
         * @param soLoadCallbacks interface to call when FFmpeg-kit library is loaded(when using dynamic loading); null on static loading
         * @return null on dynamic library loading and FFMPEG instance on static library loading
         */
        private FFMPEG initFFMPEG(ReflectionInterfaces.SOLoadCallbacks soLoadCallbacks) {
            FFMPEGInfo info = new FFMPEGInfo();
            info.videoPath = TEMP_VIDEO_NAME;
            info.audioPath = TEMP_AUDIO_NAME;
            info.localOutputPath = TEMP_RESULT_NAME;
            info.hlsURL = details.chunkUrl;
            info.videoMime = details.fileMime;
            info.audioMime = details.mimeAudio;
            if (soLoadCallbacks == null)
                return new FFMPEG(info, context, null);
            FFMPEG.newFFMPEGInstance(info, context, soLoadCallbacks);
            return null;
        }

        synchronized void onDownloadFailed(Bundle data) {
            sendBundle(PROGRESS_FAILED, data);
            removeDownloader();
        }

        synchronized void removeDownloader() {
            manager.cancel(ran);
            activeDownload -= 1;
            notificationTimer.cancel();
            downloaders.remove(this);
            if (activeDownload == 0)
                stopForeground(true);
        }

        synchronized void onDoneDownload(String finalPath, String finalMime) {
            if (isCanceled) return;
            if (finalPath == null) finalPath = TEMP_VIDEO_NAME;
            if (finalMime == null) finalMime = details.fileMime;
            copyVideoToDestination(finalPath, finalMime);
            removeDownloader();
        }

        void mergeFiles(FFMPEG ffmpeg) {
            int videoLength = MediaPlayer.create(context, Uri.fromFile(new File(TEMP_VIDEO_NAME))).getDuration();

            builder.setContentTitle("Merging Video...");
            notificationVal = 0;
            final Timer notifyTimer = notifyProgress();

            ffmpeg.setExecuteCallback(session -> {
                Bundle progressData = new Bundle();
                onDoneDownload(ffmpeg.getInfo().localOutputPath, ffmpeg.getInfo().localOutputMime);
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

        void copyVideoToDestination(String localFile, String localMime) {
            Bundle bundle = new Bundle();
            if (details.isShareOnlyDownload) {
                bundle.putString(
                        OUTFILE_URI,
                        FileProvider.getUriForFile(
                                context,
                                BuildConfig.APPLICATION_ID + ".provider",
                                new File(localFile)).toString()
                );
                bundle.putBoolean(IS_SHARE_ONLY_DOWNLOAD, true);
                bundle.putString(FILE_MIME, localMime);
                sendBundle(PROGRESS_DONE, bundle);
                return;
            }
            outputUri = FileUtil.pathToNewUri(context, details.pathUri, details.fileName, localMime);
            try {
                FileChannel inChannel = new FileInputStream(localFile).getChannel();
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
                sendErrorBundle(e.toString());
            }
            bundle.putString(OUTFILE_URI, outputUri.toString());
            sendBundle(PROGRESS_DONE, bundle);
        }

        final ReflectionInterfaces.SOLoadCallbacks mergeSOCallback = new ReflectionInterfaces.SOLoadCallbacks() {
            @Override
            public void onSOLoadingSuccess(FFMPEG ffmpeg) {
                ffmpegInstance = ffmpeg;
                mergeFiles(ffmpeg);
            }

            @Override
            public void onSOLoadingFailed(Exception e) {
                Log.e(TAG, "onSOLoadingFailed: ", e);
            }
        };

        final ReflectionInterfaces.SOLoadCallbacks hlsSoLoadCallbacks = new ReflectionInterfaces.SOLoadCallbacks() {
            @Override
            public void onSOLoadingSuccess(FFMPEG ffmpeg) {
                ffmpegInstance = ffmpeg;
                downloadChunk(ffmpeg);
            }

            @Override
            public void onSOLoadingFailed(Exception e) {
                Log.e(TAG, "onSOLoadingFailed: ", e);
            }
        };


        void download(String url, String outPath, int whatDownloading) {
            notificationTimer = notifyProgress();

            FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(context)
                    .setHttpDownloader(getOkHttpDownloader())
                    .setDownloadConcurrentLimit(3)
                    .enableRetryOnNetworkGain(true)
                    .build();

            fetch = Fetch.Impl.getInstance(fetchConfiguration);

            url = url.replace("http://", "https://");

            request = new Request(url, outPath);
            MutableExtras extra = new MutableExtras();
            extra.putInt(PROGRESS, whatDownloading);
            request.setExtras(extra);


            fetch.attachFetchObserversForDownload(request.getId(), this).enqueue(request, result -> {
            }, error -> sendErrorBundle(String.valueOf(error)));

        }

        void sendErrorBundle(String error) {
            Bundle bundle = new Bundle();
            bundle.putString(ERROR_DOWNLOADING, error);
            Log.e(TAG, "run: ", new Exception(error));
            onDownloadFailed(bundle);
        }


        void uiSpeed() {
            Bundle progressData = new Bundle();
            progressData.putInt(PROGRESS, notificationVal);
            progressData.putLong(TOTAL_SIZE, file_size);
            progressData.putLong(DOWNLOADED, passed);
            progressData.putLong(DOWNLOAD_SPEED, speed);

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
            }, 0, 2000);
            return timer;
        }

        void sendBundle(int resultCode, @Nullable Bundle progressData) {
            if (progressData != null)
                progressData.putBoolean(IS_SHARE_ONLY_DOWNLOAD, details.isShareOnlyDownload);
            Log.d(TAG, "UpdateUI: " + resultCode);
            receiver.send(resultCode, progressData);
        }

        @Override
        public void onChanged(Download download, @NonNull Reason reason) {
            if (reason == Reason.DOWNLOAD_COMPLETED) {
                fetch2Downloaded(download);
            } else if (download.getDownloadedBytesPerSecond() != -1) {
                notificationVal = download.getProgress();
                passed = download.getDownloaded();
                speed = download.getDownloadedBytesPerSecond() / 2;
                uiSpeed();
            } else if (reason == Reason.DOWNLOAD_ERROR) {
                sendErrorBundle(String.valueOf(download.getError()));
                Log.e(TAG, "onChanged: " + download.getError());
            } else if (reason != Reason.DOWNLOAD_PROGRESS_CHANGED) {
                Bundle bundle = new Bundle();
                bundle.putString(FETCH_MESSAGE, reason.toString());
                sendBundle(PROGRESS_DEFAULT, bundle);
            }
        }

        private OkHttpDownloader getOkHttpDownloader() {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(20000, TimeUnit.MILLISECONDS)
                    .connectTimeout(20000, TimeUnit.MILLISECONDS)
                    .build();
            return new OkHttpDownloader(okHttpClient, com.tonyodev.fetch2core.Downloader.FileDownloaderType.PARALLEL);
        }
    }
}
