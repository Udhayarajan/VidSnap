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

import androidx.annotation.NonNull;
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
import com.tonyodev.fetch2.Download;
import com.tonyodev.fetch2.Fetch;
import com.tonyodev.fetch2.FetchConfiguration;
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
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.mugames.vidsnap.Utility.Statics.PROGRESS_UPDATE_CHUNCK;
import static com.mugames.vidsnap.VidSnapApp.NOTIFY_DOWNLOADING;
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

import okhttp3.OkHttpClient;

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

        int downloadedClip;

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

            path = context.getExternalFilesDir("").getPath();

            TEMP_AUDIO_NAME = path + "/" + TEMP_AUDIO_NAME;
            TEMP_VIDEO_NAME = path + "/" + TEMP_VIDEO_NAME;
            TEMP_RESULT_NAME = path + "/" + TEMP_RESULT_NAME;

            details = intent.getParcelableExtra(COMMUNICATOR);


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

            new Thread(new Runnable() {
                @Override
                public void run() {

                    builder.setContentTitle("Downloading " + details.fileName + details.fileType);

                    if (finalChunkURLs == null || finalChunkURLs.size() == 0) {
                        String video_url = details.videoURL;
                        video_url = video_url.replaceAll("\\\\", "");
                        PROCESS = PROGRESS_UPDATE_VIDEO;
                        Download(video_url,TEMP_VIDEO_NAME,PROGRESS_UPDATE_VIDEO);

                    } else {
                        //TODO:Fix for chunck urls
                        for (String s : finalChunkURLs) {
                            s = s.replaceAll("\\\\", "");
                            Download(s, TEMP_VIDEO_NAME, PROGRESS_UPDATE_CHUNCK);
                        }
                    }


                }
            }).start();
        }

        void fetch2Downloaded(Download download){
            notificationTimer.cancel();

            int currentCode = download.getExtras().getInt(PROGRESS,PROGRESS_UPDATE_VIDEO);

            if(currentCode==PROGRESS_UPDATE_VIDEO){
                if(details.audioURL==null)
                    onDoneDownload(TEMP_VIDEO_NAME);
                else {
                    String audio_url = details.audioURL;
                    audio_url = audio_url.replaceAll("\\\\", "");
                    file_size = details.audioSize;
                    PROCESS = PROGRESS_UPDATE_AUDIO;
                    Download(audio_url,TEMP_AUDIO_NAME, PROGRESS_UPDATE_AUDIO);
                }
            }
            else if(currentCode==PROGRESS_UPDATE_AUDIO)
                loadAndMerge();
            else if(currentCode==PROGRESS_UPDATE_CHUNCK){
                downloadedClip++;
                if(downloadedClip== chunkURLs.size()) onDoneDownload(TEMP_VIDEO_NAME);
            }

        }

        private void onDownloadFailed(Bundle data) {
            sendBundle(PROGRESS_FAILED, data);
            manager.cancel(ran);
            activeDownload -= 1;
            if (activeDownload == 0)
                stopForeground(true);
        }

        void onDoneDownload(String finalPath) {
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

        void loadAndMerge(){
            FFMPEG.dexPath = context.getExternalFilesDir("libs").getPath() + File.separator + "ffmpeg.jar";
            FFMPEG.tempLibsPath = new File(context.getDir("dex", MODE_PRIVATE), "ffmpeg.jar").getAbsolutePath();
            FFMPEG.jniPath = context.getExternalFilesDir("libs") + File.separator + "jni" + File.separator;
            FFMPEG.filesDir = context.getFilesDir().getAbsolutePath() + File.separator;

            Log.e(TAG, FFMPEG.getString());

            FFMPEG.loadSOFiles(context.getExternalFilesDir("libs") + File.separator + "jni" + File.separator,soLoadCallbacks);

        }

        void mergeFiles() {
            int videoLength = MediaPlayer.create(context, Uri.fromFile(new File(TEMP_VIDEO_NAME))).getDuration();

            builder.setContentTitle("Merging Video...");
            notificationVal = 0;
            final Timer notifyTimer = notifyProgress();

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
                            sendBundle(PROGRESS_UPDATE_MERGING, progressData);
                        }
                    }, new FFmpegCallbacks() {
                        @Override
                        public void apply(Session session, String outputPath) {
                            Bundle progressData = new Bundle();
                            onDoneDownload(outputPath);
                            progressData.putInt(PROGRESS, 100);
                            sendBundle(PROGRESS_UPDATE_MERGING, progressData);
                            notifyTimer.cancel();
                        }
                    });

        }

        void copyVideoToDestination(String finalFile){

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

            }catch (IOException e){
                Log.e(TAG, "copyVideoToDestination: ",e );
            }
            FileUtil.deleteFile(finalFile);
            FileUtil.deleteFile(TEMP_AUDIO_NAME);
            FileUtil.deleteFile(TEMP_VIDEO_NAME);
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



        void Download(String url, String outPath, int whatDownloading) {
            notificationTimer = notifyProgress();
            OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();

            FetchConfiguration fetchConfiguration = new FetchConfiguration.Builder(context)
                    .setHttpDownloader(new OkHttpDownloader(okHttpClient)).build();

            Fetch fetch = Fetch.Impl.getInstance(fetchConfiguration);

            Request request = new Request(url,outPath);
            MutableExtras extra = new MutableExtras();
            extra.putInt(PROGRESS,whatDownloading);
            request.setExtras(extra);


            fetch.attachFetchObserversForDownload(request.getId(),this).enqueue(request,result -> {},error->{
                Bundle bundle = new Bundle();
                bundle.putString(ERROR_DOWNLOADING, String.valueOf(error));
                Log.e(TAG, "run: "+error );
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

        void sendBundle(int resultCode, Bundle progressData) {
            Log.d(TAG, "UpdateUI: " + resultCode);
            receiver.send(resultCode, progressData);
        }

        @Override
        public void onChanged(Download download, @NonNull Reason reason) {
            if(reason==Reason.DOWNLOAD_COMPLETED){
                fetch2Downloaded(download);
            }else if(download.getDownloadedBytesPerSecond()!=-1) {
                notificationVal = download.getProgress();
                passed = download.getDownloaded();
                speed = download.getDownloadedBytesPerSecond();
                Log.e(TAG, "onChanged: "+speed);
                speed/=1024;
                uiSpeed();
            }
        }
    }


}
