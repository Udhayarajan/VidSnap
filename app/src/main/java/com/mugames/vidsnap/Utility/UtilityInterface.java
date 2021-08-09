package com.mugames.vidsnap.Utility;

import android.graphics.Bitmap;
import android.net.Uri;
import android.view.MotionEvent;

import com.mugames.vidsnap.DataBase.History;
import com.mugames.vidsnap.Utility.Bundles.DownloadDetails;
import com.mugames.vidsnap.Utility.Bundles.HistoryDetails;
import com.mugames.vidsnap.ui.main.Fragments.QualityFragment;


public interface UtilityInterface{
    interface DownloadButtonCallBack {
        void onDownloadButtonPressed(String fileName);
        void onSelectedItem(int position, QualityFragment qualityFragment);
    }

    interface JSInterface {

        Object resf(Object[] values);

    }
    interface MiniExecutorCallBack {
        void onBitmapReceive(Bitmap image);
        void onSizeReceived(int size,int position);
    }
    interface ResponseCallBack {
        void onReceive(String response);
    }
    interface SignNotifier {
        void onDecrypted(String decryptedSign,String url);
    }

    interface CookiesInterface{
        void onReceivedCookies(String cookies);
    }

    interface LoginIdentifier{
        void loggedIn(String cookies);
    }
    interface DownloadCallback{
        void onDownloadCompleted(int index,Uri fileUri);
        void  onFailedDownload(int index);
    }
    interface AnalyzeCallback{
        void onAnalyzeCompleted(Formats formats,boolean isLast);
    }
    interface AnalyzeUICallback{
        void onAnalyzeCompleted(boolean isMultipleFile);
    }
    interface DownloadableCardSelectedCallBack{
        void onCardSelected(int index);
        void onCardDeSelected(int index);
    }
    interface TouchCallback{
        void onDispatchTouch(MotionEvent event);
    }

    interface LogoutCallBacks{
        void onLoggedOut();
    }

    interface RootDownloadCallback{
        void onDownloadCompleted(boolean isFinal);
    }
}
