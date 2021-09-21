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

package com.mugames.vidsnap.extractor;

import android.content.Context;
//import android.os.Bundle;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.mugames.vidsnap.R;
import com.mugames.vidsnap.network.MiniExecute;
import com.mugames.vidsnap.utility.bundles.Formats;
import com.mugames.vidsnap.utility.Statics;
import com.mugames.vidsnap.utility.UtilityInterface;
import com.mugames.vidsnap.utility.UtilityInterface.AnalyzeCallback;
import com.mugames.vidsnap.utility.UtilityInterface.DialogueInterface;
import com.mugames.vidsnap.utility.UtilityInterface.LoginHelper;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Base class of all kind of analyzer/extractor automatically which runs on separate thread and change itself to main thread at end
 */

public abstract class Extractor extends Thread {


    Context applicationContext;
    static String TAG = Statics.TAG + ":Extractor";
    static final String EXTRA_isVideo = "com.mugames.vidsnap.Extractor.Extractor.isVideo";
    static final String EXTRA_INDEX = "com.mugames.vidsnap.Extractor.Extractor.indexOfQualities";

    public Formats formats;

    boolean isVideoSizeReady;
    boolean isAudioSizeReady;
    boolean isManifestReady;
    String url;


    AnalyzeCallback analyzeCallback;

    DialogueInterface dialogueInterface;

    LoginHelper loginHelper;


    int totalUrlsCount;

    public Extractor(String source, Context context, AnalyzeCallback analyzeCallback, DialogueInterface dialogueInterface) {
        this(source);
        this.applicationContext = context.getApplicationContext();
        this.analyzeCallback = analyzeCallback;
        this.dialogueInterface = dialogueInterface;
    }


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

    protected abstract void analyze(String url);

    @Override
    public void run() {
        super.run();
        analyze(url);
    }

    public String getUserCookies() {
        return loginHelper.getCookies(getCookiesKey());
    }

    /**
     * It is a exit point of extractor upon getting details like video url, thumbnail url
     * it must be called to estimate file size
     * NOTE: It is only for file size estimation
     */
    public void fetchDataFromURLs() {
        getDialogueInterface().show("Almost Done !!");
        new Thread(this::getSizeForManifest).start();
        new Thread(this::getSizeForVideos).start();
        new Thread(this::getSizeForAudio).start();
    }

    private void getSizeForManifest() {
        if (formats.manifest.isEmpty()) return;

        ArrayList<MiniExecute> miniExecutes = new ArrayList<>();

        getDialogueInterface().show("This may take a while\nPlease don't close app");


        for (ArrayList<String> chunk : formats.manifest) totalUrlsCount += chunk.size();

        CountDownLatch countDownLatch = new CountDownLatch(totalUrlsCount);

        for (int chunkIndex = 0; chunkIndex < formats.manifest.size(); chunkIndex++) {
            formats.videoSizes.add(0L);
            formats.videoSizeInString.add("");
            Bundle bundle = new Bundle();
            bundle.putBoolean(EXTRA_isVideo, true);
            bundle.putInt(EXTRA_INDEX, chunkIndex);

            for (String url : formats.manifest.get(chunkIndex)) {
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

        for (MiniExecute miniExecute : miniExecutes) {
            int chunkIndex = miniExecute.getBundle().getInt(EXTRA_INDEX);
            long oldSize = formats.videoSizes.get(chunkIndex);
            long size = oldSize + miniExecute.getSize();
            formats.videoSizes.set(chunkIndex, size);
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            formats.videoSizeInString.set(chunkIndex, decimalFormat.format(size / Math.pow(10, 6)));
        }
        isManifestReady = true;
        checkForManifestCompletion();

    }


    private void getSizeForVideos() {
        if (formats.mainFileURLs.isEmpty()) return;//It must be m3u8

        CountDownLatch countDownLatch = new CountDownLatch(formats.mainFileURLs.size());
        ArrayList<MiniExecute> miniExecutes = new ArrayList<>();

        for (int i = 0; i < formats.mainFileURLs.size(); i++) {
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_INDEX, i);
            bundle.putBoolean(EXTRA_isVideo, true);
            MiniExecute miniExecute = new MiniExecute(bundle, countDownLatch);
            miniExecutes.add(miniExecute);
            miniExecute.getSize(formats.mainFileURLs.get(i));
            formats.videoSizes.add(-1L);
            formats.videoSizeInString.add("");
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (MiniExecute miniExecute : miniExecutes) {
            int index = miniExecute.getBundle().getInt(EXTRA_INDEX);
            long size = miniExecute.getSize();
            formats.videoSizes.set(index, size);
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            formats.videoSizeInString.set(index, decimalFormat.format(size / Math.pow(10, 6)));
        }
        isVideoSizeReady = true;
        checkForCompletion();
    }

    private void getSizeForAudio() {

        if (formats.audioURLs.isEmpty()) {
            isAudioSizeReady = true;
            checkForCompletion();
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(formats.audioURLs.size());
        ArrayList<MiniExecute> miniExecutes = new ArrayList<>();

        for (int i = 0; i < formats.audioURLs.size(); i++) {
            if(formats.audioURLs.get(i)==null) {
                formats.audioSizes.add(0L);
                countDownLatch.countDown();
                continue;
            }
            Bundle bundle = new Bundle();
            bundle.putInt(EXTRA_INDEX, i);
            MiniExecute miniExecute = new MiniExecute(bundle, countDownLatch);
            miniExecutes.add(miniExecute);
            miniExecute.getSize(formats.audioURLs.get(i));
            formats.audioSizes.add(-1L);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (MiniExecute miniExecute : miniExecutes) {
            int index = miniExecute.getBundle().getInt(EXTRA_INDEX);
            long size = miniExecute.getSize();
            formats.audioSizes.set(index, size);
        }
        isAudioSizeReady = true;
        checkForCompletion();
    }

    public DialogueInterface getDialogueInterface() {
        return wrappedDialogueInterface;
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
        String cookies = loginHelper.getCookies(getCookiesKey());
        if (cookies == null)
            loginHelper.signInNeeded(notificationTxt, url, validDoneURLS, getCookiesKey(), loginIdentifier);
        else loginIdentifier.loggedIn(cookies);
    }


    private void checkForManifestCompletion() {
        if (formats.manifest.isEmpty()) return;
        if (isManifestReady) completed();
    }

    private void checkForCompletion() {
        if (isAudioSizeReady && isVideoSizeReady)
            completed();
    }

    void completed() {
        wrappedAnalyzeCallback.onAnalyzeCompleted(formats);
    }

    @Nullable
    public Context getContext() {
        return applicationContext;
    }

    public void setContext(Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    public void whatsAppStatusAnalyzed() {
        wrappedAnalyzeCallback.onAnalyzeCompleted(formats);
    }

    AnalyzeCallback wrappedAnalyzeCallback = new AnalyzeCallback() {
        @Override
        public void onAnalyzeCompleted(Formats formats) {
            wrappedDialogueInterface.dismiss();
            new Handler(applicationContext.getMainLooper()).post(()-> analyzeCallback.onAnalyzeCompleted(formats));
        }
    };

    DialogueInterface wrappedDialogueInterface = new DialogueInterface() {
        @Override
        public void show(String text) {
            new Handler(applicationContext.getMainLooper()).post(()-> dialogueInterface.show(text));
        }

        @Override
        public void error(String message, Exception e) {
            new Handler(applicationContext.getMainLooper()).post(()-> dialogueInterface.error(message,e));
        }

        @Override
        public void dismiss() {
            new Handler(applicationContext.getMainLooper()).post(()-> dialogueInterface.dismiss());
        }
    };

    public void setLink(String url) {
        this.url = url;
    }
}
