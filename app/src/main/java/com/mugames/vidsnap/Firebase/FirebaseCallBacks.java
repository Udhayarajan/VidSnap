package com.mugames.vidsnap.Firebase;

public interface FirebaseCallBacks {
    interface UpdateCallbacks{
        void onReceiveData(boolean isUpdateAvailable,String versionName, boolean isForced,String changeLog, String url);
    }
    interface ShareCallback{
        void onShareLinkGot(String link);
    }

    interface IssueCountCallback{
        void onFetchCount(Long issueIndex);
    }
}
