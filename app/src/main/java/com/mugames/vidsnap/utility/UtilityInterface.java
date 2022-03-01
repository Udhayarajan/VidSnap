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

import com.mugames.vidsnap.network.Response;
import com.mugames.vidsnap.utility.bundles.DownloadDetails;
import com.mugames.vidsnap.utility.bundles.Formats;
import com.mugames.vidsnap.ui.fragments.QualityFragment;


public interface UtilityInterface {
    interface DownloadClickedCallback {
        void onDownloadButtonPressed();
    }

    interface JSInterface {
        Object resf(Object[] values);
    }


    interface ResponseCallBack {
        void onReceive(Response response);
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
        void onDownloadCompleted(DownloadDetails downloadDetails);
        void onDownloadFailed(String reason, Exception e);
    }

    interface AnalyzeCallback {
        void onAnalyzeCompleted(Formats formats);
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
        void signInNeeded(UtilityClass.LoginDetailsProvider loginDetailsProvider);
        String getCookies(int cookiesKey);
    }
}
