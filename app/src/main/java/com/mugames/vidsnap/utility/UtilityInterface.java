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

package com.mugames.vidsnap.utility;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;

import com.mugames.vidsnap.utility.bundles.Formats;
import com.mugames.vidsnap.ui.fragments.QualityFragment;


public interface UtilityInterface {
    interface DownloadButtonCallBack {
        void onDownloadButtonPressed(String fileName, Bitmap image);

        void onSelectedItem(int position, QualityFragment qualityFragment);
    }

    interface JSInterface {

        Object resf(Object[] values);

    }

    interface MiniExecutorCallBack {
        void onBitmapReceive(Bitmap image);

        void onSizeReceived(long size, int position);
    }

    interface ResponseCallBack {
        void onReceive(String response);
    }

    interface SignNotifier {
        void onDecrypted(int index, String decryptedSign, String url);
    }

    interface CookiesInterface {
        void onReceivedCookies(String cookies);
    }

    interface LoginIdentifier {
        void loggedIn(String cookies);
    }

    interface DownloadCallback {
        void onDownloadCompleted(int id);
    }

    interface AnalyzeCallback {
        void onAnalyzeCompleted(Formats formats);
    }

    interface AnalyzeUICallback {
        void onAnalyzeCompleted(boolean isMultipleFile);
    }


    interface TouchCallback {
        void onDispatchTouch(MotionEvent event);
    }

    interface ConfigurationCallback {
        void onProcessDone();
    }

    interface ModuleDownloadCallback {
        void onDownloadEnded();
    }

    interface SizeCallback {
        void onReceiveSize(long size, Bundle extras);
    }

    interface DialogueInterface {
        void show(String text);

        void error(String message, Exception e);

        void dismiss();
    }

    interface LoginHelper{
        void signInNeeded(String reason, String loginURL, String[] loginDoneUrl, int cookiesKey, UtilityInterface.LoginIdentifier identifier);
        String getCookies(int cookiesKey);
    }
}
