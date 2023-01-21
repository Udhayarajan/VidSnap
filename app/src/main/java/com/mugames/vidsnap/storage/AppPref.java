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

package com.mugames.vidsnap.storage;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.mugames.vidsnap.R;
import com.mugames.vidsnap.utility.Statics;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Helper class to access shared preferences
 */
public class AppPref {

    /**
     * This is database path
     */
    public static final String STATIC_CACHE = ".historyDB";
    /**
     * Contains temp thumbnails and short time video files
     */
    public static final String DYNAMIC_CACHE = ".essential";
    /**
     * Contains SO files for FFmpeg
     */
    public static final String LIBRARY_PATH = "libs";
    /**
     * Folder to store temporary spliced videos
     */
    public static final String SPLICE_PATH = "spliced";

    public static final String db_name = "historyCache";

    private static volatile AppPref instance;

    final SharedPreferences sharedPreferences;
    final Context context;

    String TAG = Statics.TAG + ":AppPref";


    private AppPref(Context context) {
        this.context = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static AppPref getInstance(Context context) {
        if (instance == null) {
            synchronized (AppPref.class) {
                if (instance == null)
                    instance = new AppPref(context.getApplicationContext());
            }
        }
        return instance;
    }


    public void setStringValue(int key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(key), value);
        editor.apply();
    }

    public String getStringValue(int id, String def) {
        return sharedPreferences.getString(context.getString(id), def);
    }

    public void setBooleanValue(int key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getString(key), value);
        editor.apply();
    }

    public boolean getBooleanValue(int id, boolean def) {
        return sharedPreferences.getBoolean(context.getString(id), def);
    }

    /**
     * save path got from DOCUMENT_TREE
     *
     * @param intent intent that we got from document tree
     *               displayable string eg: External Storage>folder> etc..
     */

    public void setSavePath(Intent intent) {
        String path;
        Uri uri;
        if (intent != null) {
            int takeFlags = intent.getFlags();
            takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            uri = intent.getData();
            context.getContentResolver().takePersistableUriPermission(uri, takeFlags);
            setStringValue(R.string.key_download, uri.toString());

        } else {
            path = FileUtil.getExternalStoragePublicDirectory(context, Environment.DIRECTORY_DOWNLOADS) + "/";
            setStringValue(R.string.key_download, Uri.fromFile(new File(path)).toString());
        }


    }

    public Uri getSavePath() {
        String p = getStringValue(R.string.key_download, null);
        if (p == null) return null;
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

    public void setWhatsAppUri(Intent intent) {
        Uri uri = intent.getData();
        int flag = intent.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        context.getContentResolver().takePersistableUriPermission(uri, flag);
        setStringValue(R.string.whatsappUri, uri.toString());
    }

    public Uri getWhatsAppUri() {
        String uri = getStringValue(R.string.whatsappUri, null);
        if (uri == null) return null;
        return Uri.parse(uri);
    }

    public void saveFcmToken(@NotNull String token) {
        setStringValue(R.string.key_fcm_token, token);
    }

    @Nullable
    public String getFcmToken() {
        return getStringValue(R.string.key_fcm_token, null);
    }
}
