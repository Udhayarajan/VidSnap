package com.mugames.vidsnap.Utility;

import android.graphics.Bitmap;
//import android.os.Bundle;
import android.os.Bundle;
import android.util.Log;

import com.mugames.vidsnap.R;
import com.mugames.vidsnap.Threads.MiniExecute;
import com.mugames.vidsnap.Utility.UtilityInterface.AnalyzeCallback;
import com.mugames.vidsnap.Utility.UtilityInterface.DialogueInterface;
import com.mugames.vidsnap.Utility.UtilityInterface.LoginHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Extractor implements
        UtilityInterface.SizeCallback,
        UtilityInterface.ThumbnailCallbacks {

    static String TAG = Statics.TAG + ":Extractor";
    static final String EXTRA_isVideo = "com.mugames.vidsnap.Utility.Extractor.isVideo";
    static final String EXTRA_INDEX = "com.mugames.vidsnap.Utility.Extractor.indexOfQualities";
    static final String EXTRA_URL_INDEX = "com.mugames.vidsnap.Utility.Extractor.indexOfURLinChunk";

    public Formats formats;
    int fetchedSizeForVideos;
    int fetchedSizeForAudio;
    int fetchedThumbnails;

    boolean isVideoSizeReady;
    boolean isAudioSizeReady;
    boolean isThumbnailReady;
    boolean isManifestReady;


    AnalyzeCallback analyzeCallback;

    DialogueInterface dialogueInterface;

    LoginHelper loginHelper;

    String userCookies;

    ArrayList<MiniExecute> miniExecutes = new ArrayList<>();



    int totalUrlsCount;


    public Extractor(DialogueInterface dialogueInterface, String source) {
        this(source);
        this.dialogueInterface = dialogueInterface;
    }

    public Extractor(String source) {
        formats = new Formats();
        formats.src = source;
    }

    public void setAnalyzeCallback(AnalyzeCallback analyzeCallback) {
        this.analyzeCallback = analyzeCallback;
    }


    public void setLoginHelper(LoginHelper loginHelper) {
        this.loginHelper = loginHelper;
    }

    public abstract void analyze(String url, AnalyzeCallback analyzeCallback);

    public String getUserCookies() {
        return userCookies;
    }

    public void setUserCookies(String userCookies) {
        this.userCookies = userCookies;
    }

    public void fetchDataFromURLs() {
        getDialogueInterface().show("Almost Done !!");
        getSizeForManifest();
        getSizeForVideos();
        getSizeForAudio();
        getThumbnail();
    }

    private void getSizeForManifest() {
        if (formats.manifest.isEmpty()) return;

        getDialogueInterface().show("This may take a while\nPlease don't close app");

        CountDownLatch countDownLatch = new CountDownLatch(totalUrlsCount);



        for (ArrayList<String> chunk:formats.manifest) totalUrlsCount += chunk.size();


        for (int chunkIndex = 0; chunkIndex < formats.manifest.size(); chunkIndex++) {
            formats.videoSizes.add(0L);
            formats.videoSizeInString.add("");
            Bundle bundle = new Bundle();
            bundle.putBoolean(EXTRA_isVideo, true);
            bundle.putInt(EXTRA_INDEX, chunkIndex);

            for (String url:formats.manifest.get(chunkIndex)) {
                MiniExecute miniExecute = new MiniExecute(bundle, countDownLatch);
                miniExecutes.add(miniExecute);
                miniExecute.getSize(url);
//                new MiniExecute(bundle, countDownLatch).getSize(formats.manifest.get(chunkIndex).get(urlIndex), (size, extras) -> {
//                    receivedCounts++;
//                    progressPercentage = receivedCounts * 100 / totalUrlsCount;
//                    localTracker.put(extras.getInt(EXTRA_URL_INDEX),size);
//                    getDialogueInterface().show(String.format("This may take a while (%s%%)", progressPercentage));
//                    checkForManifestCompletion();
//                });
            }
        }



        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            try {
                throw e;
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }

        for (MiniExecute miniExecute: miniExecutes) {
            int chunkIndex = miniExecute.getBundle().getInt(EXTRA_INDEX);
            long oldSize = formats.videoSizes.get(chunkIndex);
            long size = oldSize + miniExecute.getSize();
            formats.videoSizes.set(chunkIndex, size);
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            formats.videoSizeInString.set(chunkIndex, decimalFormat.format( size/ Math.pow(10, 6)));
        }
        isManifestReady = true;
        checkForManifestCompletion();

    }


    private void getThumbnail() {
        for (int i = 0; i < formats.thumbNailsURL.size(); i++) {
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_INDEX,i);
            new MiniExecute(bundle).getThumbnail(formats.thumbNailsURL.get(i), this);
            formats.thumbNailsBitMap.add(null);
        }
    }

    public DialogueInterface getDialogueInterface() {
        return dialogueInterface;
    }

    public void setDialogueInterface(DialogueInterface dialogueInterface) {
        this.dialogueInterface = dialogueInterface;
    }

    private void getSizeForVideos() {
        for (int i = 0; i < formats.videoURLs.size(); i++) {
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_INDEX,i);
            bundle.putBoolean(EXTRA_isVideo,true);
            new MiniExecute(bundle).getSize(formats.videoURLs.get(i), this);
            formats.videoSizes.add(-1L);
            formats.videoSizeInString.add("");
        }
    }

    private void getSizeForAudio() {
        if (formats.audioURLs.isEmpty()) isAudioSizeReady = true;
        for (int i = 0; i < formats.audioURLs.size(); i++) {
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_INDEX,i);
            Log.e(TAG, "getSizeForAudio: "+i);
            new MiniExecute(bundle).getSize(formats.audioURLs.get(i), this);
            formats.audioSizes.add(-1L);
        }
    }

    public int getCookiesKey() {
        switch (formats.src) {
            case "FaceBook":
                return R.string.key_facebook;
            case "Instagram":
                return R.string.key_instagram;
        }
        return -1;
    }

    public void trySignIn(String notificationTxt, String url, String[] validDoneURLS, UtilityInterface.LoginIdentifier loginIdentifier) {
        loginHelper.signInNeeded(notificationTxt, url, validDoneURLS, getCookiesKey(), loginIdentifier);
    }

    private void videoSize(long size, Bundle extras) {
        int index = extras.getInt(EXTRA_INDEX);
        if (formats.manifest.isEmpty()) {
            fetchedSizeForVideos++;
            formats.videoSizes.set(index, size);

            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            formats.videoSizeInString.set(index, decimalFormat.format(size / Math.pow(10, 6)));
            Log.e("TAG", "onReceiveSizeVideo: " + fetchedSizeForVideos + "Total " + formats.videoURLs.size());
            isVideoSizeReady = fetchedSizeForVideos == formats.videoURLs.size();

        } else {
            int totalIndex = extras.getInt(EXTRA_URL_INDEX);


            formats.videoSizes.set(index, formats.videoSizes.get(index) + size);

            Log.e(TAG, "checkForManifestCompletion: " + totalIndex);

            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            formats.videoSizeInString.set(index, decimalFormat.format(formats.videoSizes.get(index) / Math.pow(10, 6)));
        }
    }

    private void audioSize(long size, Bundle bundle) {
        fetchedSizeForAudio++;
        formats.audioSizes.set(bundle.getInt(EXTRA_INDEX), size);
        isAudioSizeReady = fetchedSizeForAudio == formats.audioSizes.size();
        Log.e("TAG", "onReceiveSizeAudio: " + fetchedSizeForAudio + "Total " + formats.audioSizes.size());
    }


    @Override
    public void onReceiveSize(long size, Bundle extras) {
        if (extras.getBoolean(EXTRA_isVideo))
            videoSize(size, extras);
        else audioSize(size, extras);

        checkForManifestCompletion();
        checkForCompletion();
    }




    @Override
    public void onReceivedThumbnail(Bitmap thumbnail, Bundle bundle) {
        fetchedThumbnails++;
        formats.thumbNailsBitMap.set(bundle.getInt(EXTRA_INDEX), thumbnail);
        isThumbnailReady = fetchedThumbnails == formats.thumbNailsURL.size();
        Log.e("TAG", "onReceiveThumbnail: " + fetchedThumbnails + "Total " + formats.thumbNailsURL.size());
        checkForCompletion();
        checkForManifestCompletion();
    }


    private void checkForManifestCompletion() {

        if(formats.manifest.isEmpty()) return;

        if(isManifestReady&&isThumbnailReady) analyzeCallback.onAnalyzeCompleted(formats, true);



    }

    private void checkForCompletion() {
        if (isThumbnailReady && isAudioSizeReady && isVideoSizeReady)
            analyzeCallback.onAnalyzeCompleted(formats, true);
    }
}
