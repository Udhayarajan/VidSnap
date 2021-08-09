package com.mugames.vidsnap.Utility;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import androidx.preference.PreferenceManager;

import com.mugames.vidsnap.R;
import com.mugames.vidsnap.ui.main.Activities.MainActivity;

import java.io.File;

public class AppPref {
    SharedPreferences sharedPreferences;
    Context context;

    String TAG = Statics.TAG+":AppPref";

    public AppPref(Context context, SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        this.context = context;
    }

    public AppPref(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setStringValue(int key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(key), value);
        editor.apply();
    }

    public String getStringValue(int id, String def) {
        return sharedPreferences.getString(context.getString(id), def);
    }

    public void setBooleanValue(int key, boolean value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getString(key), value);
        editor.apply();
    }

    public boolean getBooleanValue(int id, boolean def){
        return sharedPreferences.getBoolean(context.getString(id), def);
    }

    /**
     * save path got from DOCUMENT_TREE
     * @param intent intent that we got from document tree
     * @return displayable string eg: External Storage>folder> etc..
     * */

    public void setSavePath(Intent intent){
        String path;
        Uri uri;
        if (intent != null) {
            final int takeFlags = intent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            uri = intent.getData();
            context.getContentResolver().takePersistableUriPermission(uri,takeFlags);
            setStringValue(R.string.key_download, uri.toString());

        } else {
            path = FileUtil.getExternalStoragePublicDirectory(context, Environment.DIRECTORY_DOWNLOADS) + "/";
            setStringValue(R.string.key_download, Uri.fromFile(new File(path)).toString());
        }


    }

    public Uri getSavePath(){
        String p= getStringValue(R.string.key_download, null);
        if(p==null)  return null;
        return Uri.parse(p);
    }

    public String getCachePath(String folderName) {
        int index = 0;
        String currentState = getStringValue(R.string.key_cache_path, null);
        if (currentState != null && currentState.equals("external") && FileUtil.isSDPresent(context)) {
            index = 1;
        }
        return context.getExternalFilesDirs(folderName)[index] + "/";
    }

}
