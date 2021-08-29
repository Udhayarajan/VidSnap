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
import java.util.concurrent.CountDownLatch;

public abstract class Extractor {

    static String TAG = Statics.TAG + ":Extractor";
    static final String EXTRA_isVideo = "com.mugames.vidsnap.Utility.Extractor.isVideo";
    static final String EXTRA_INDEX = "com.mugames.vidsnap.Utility.Extractor.indexOfQualities";

    public Formats formats;

    boolean isVideoSizeReady;
    boolean isAudioSizeReady;
    boolean isThumbnailReady;
    boolean isManifestReady;


    AnalyzeCallback analyzeCallback;

    DialogueInterface dialogueInterface;

    LoginHelper loginHelper;

    String userCookies;





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

    public abstract void analyze(String url);

    public String getUserCookies() {
        return userCookies;
    }

    //TODO:Cookies logic needed to be fixed
    public void setUserCookies(String userCookies) {
        this.userCookies = userCookies;
    }

    public void fetchDataFromURLs() {
        getDialogueInterface().show("Almost Done !!");
        new Thread(this::getSizeForManifest).start();
        new Thread(this::getSizeForVideos).start();
        new Thread(this::getSizeForAudio).start();
        new Thread(this::getThumbnail).start();
    }

    private void getSizeForManifest() {
        if (formats.manifest.isEmpty()) return;

        ArrayList<MiniExecute> miniExecutes = new ArrayList<>();

        getDialogueInterface().show("This may take a while\nPlease don't close app");


        for (ArrayList<String> chunk:formats.manifest) totalUrlsCount += chunk.size();

        CountDownLatch countDownLatch = new CountDownLatch(totalUrlsCount);

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


    private void getSizeForVideos() {
        if(formats.videoURLs.isEmpty()) return;//It must be m3u8
        CountDownLatch countDownLatch = new CountDownLatch(formats.videoURLs.size());
        ArrayList<MiniExecute> miniExecutes = new ArrayList<>();

        for (int i = 0; i < formats.videoURLs.size(); i++) {
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_INDEX,i);
            bundle.putBoolean(EXTRA_isVideo,true);
            MiniExecute miniExecute = new MiniExecute(bundle,countDownLatch);
            miniExecutes.add(miniExecute);
            miniExecute.getSize(formats.videoURLs.get(i));
            formats.videoSizes.add(-1L);
            formats.videoSizeInString.add("");
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (MiniExecute miniExecute: miniExecutes) {
            int index = miniExecute.getBundle().getInt(EXTRA_INDEX);
            long size = miniExecute.getSize();
            formats.videoSizes.set(index,size);
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            formats.videoSizeInString.set(index, decimalFormat.format(size / Math.pow(10, 6)));
        }
        isVideoSizeReady = true;
        checkForCompletion();
    }

    private void getSizeForAudio() {

        if (formats.audioURLs.isEmpty()){
            isAudioSizeReady = true;
            checkForCompletion();
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(formats.audioURLs.size());
        ArrayList<MiniExecute> miniExecutes = new ArrayList<>();

        for (int i = 0; i < formats.audioURLs.size(); i++) {
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_INDEX,i);
            MiniExecute miniExecute = new MiniExecute(bundle,countDownLatch);
            miniExecutes.add(miniExecute);
            miniExecute.getSize(formats.audioURLs.get(i));
            formats.audioSizes.add(-1L);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (MiniExecute miniExecute: miniExecutes) {
            int index = miniExecute.getBundle().getInt(EXTRA_INDEX);
            long size = miniExecute.getSize();
            formats.audioSizes.set(index,size);
        }
        isAudioSizeReady = true;
        checkForCompletion();
    }

    private void getThumbnail() {
        CountDownLatch countDownLatch = new CountDownLatch(formats.thumbNailsURL.size());
        ArrayList<MiniExecute> miniExecutes = new ArrayList<>();

        for (int i = 0; i < formats.thumbNailsURL.size(); i++) {
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_INDEX,i);
            MiniExecute miniExecute = new MiniExecute(bundle,countDownLatch);
            miniExecutes.add(miniExecute);
            miniExecute.getThumbnail(formats.thumbNailsURL.get(i));
            formats.thumbNailsBitMap.add(null);
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (MiniExecute miniExecute: miniExecutes) {
            int index = miniExecute.getBundle().getInt(EXTRA_INDEX);
            Bitmap bitmap = miniExecute.getBitmap();
            formats.thumbNailsBitMap.set(index, bitmap);
        }
        isThumbnailReady = true;
        checkForCompletion();
        checkForManifestCompletion();
    }

    public DialogueInterface getDialogueInterface() {
        return dialogueInterface;
    }

    public void setDialogueInterface(DialogueInterface dialogueInterface) {
        this.dialogueInterface = dialogueInterface;
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



    private void checkForManifestCompletion() {

        if(formats.manifest.isEmpty()) return;

        if(isManifestReady&&isThumbnailReady) analyzeCallback.onAnalyzeCompleted(formats, true);



    }

    private void checkForCompletion() {
        if (isThumbnailReady && isAudioSizeReady && isVideoSizeReady)
            analyzeCallback.onAnalyzeCompleted(formats, true);
    }
}
