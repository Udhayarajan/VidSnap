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

import android.app.Activity;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FileUtil {
    static String PRIMARY_VOLUME = "primary";
    static String PRIMARY_TREE = "/tree/primary";

    public static String uriToPath(Context context, @Nullable final Uri uri) {

        if (uri == null) return null;
        String volumeID = getIDFromUri(uri, context);
        String volumePath = getVolumePath(volumeID, context);

        if (volumePath == null) return uri.getPath();

        if (volumePath.endsWith(File.separator))
            volumePath = volumePath.substring(0, volumePath.length() - 1);

        String documentPath = docPathFromUri(uri);
        String documentName = getFilePath(uri, context, documentPath);

        if (documentPath.endsWith(File.separator))
            documentPath = documentPath.substring(0, volumePath.length() - 1);

        if (volumePath.endsWith(File.separator))
            volumePath = volumePath.substring(0, volumePath.length() - 1);


        documentName = documentName == null ? "" : documentName;

        if (documentPath.endsWith(File.separator))
            documentPath = documentPath.substring(0, documentPath.length() - 1);

        if (documentPath.length() > 0) {
            if (documentName.startsWith(File.separator))
                return volumePath + File.separator + documentPath + documentName;
            else
                return volumePath + File.separator + documentPath + File.separator + documentName;
        } else return volumePath;
    }

    private static String getFilePath(Uri uri, Context context, String documentPath) {
        try {
            String[] split;
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                split = id.split(":");
            } else {
                split = uri.getPath().split(":");
            }

            if (split.length > 1) return split[1].replace(documentPath, "");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String getIDFromUri(Uri uri, Context context) {
        try {
            String[] split;
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                split = id.split(":");
            } else {
                split = uri.getPath().split(":");
            }

            if (split.length > 0) return split[0];
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getVolumePath(String volumeID, Context context) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            return getVolumePreR(volumeID, context);
        else
            return getVolumePostR(volumeID, context);//Includes R and above
    }


    private static String getVolumePreR(String volumeID, Context context) {
        try {
            StorageManager manager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Class<?> storageVolumeClass = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = manager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClass.getMethod("getUuid");
            Method getPath = storageVolumeClass.getMethod("getPath");
            Method isPrimary = storageVolumeClass.getMethod("isPrimary");

            Object result = getVolumeList.invoke(manager);

            final int length = Array.getLength(result);

            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                if (primary && (PRIMARY_VOLUME.equals(volumeID)) || PRIMARY_TREE.equals(volumeID))    // primary volume?
                    return (String) getPath.invoke(storageVolumeElement);

                if (uuid != null && uuid.equals(volumeID))    // other volumes?
                    return (String) getPath.invoke(storageVolumeElement);
            }

            // not found.
            return null;

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static String getVolumePostR(String volumeID, Context context) {
        StorageManager manager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

        List<StorageVolume> volumes = manager.getStorageVolumes();


        for (StorageVolume volume : volumes) {
            // primary volume?
            if (volume.isPrimary() && (PRIMARY_VOLUME.equals(volumeID) || PRIMARY_TREE.equals(volumeID)))
                return volume.getDirectory().getPath();

            // other volumes?
            String uuid = volume.getUuid();
            String uuid_tree = "/tree/" + uuid;

            if (uuid != null && (uuid.equals(volumeID) || uuid_tree.equals(volumeID)))
                return volume.getDirectory().getPath();

        }
        return null;
    }


    /**
     * Can't be used to write. Only for string purpose
     */
    public static String getExternalStoragePublicDirectory(Context context, @Nullable String type) {
        type = type==null?"":type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            StorageManager manager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

            List<StorageVolume> volumes = manager.getStorageVolumes();

            for (StorageVolume volume : volumes) {
                // primary volume?
                if (volume.isPrimary())
                    return volume.getDirectory().getPath() + File.separator + type;
            }
        }
        return String.valueOf(Environment.getExternalStoragePublicDirectory(type));
    }


    private static String docPathFromUri(Uri uri) {
        String id = DocumentsContract.getTreeDocumentId(uri);
        final String[] split = id.split(":");
        if ((split.length >= 2) && (split[1] != null)) return split[1];
        else return File.separator;
    }


    public static String displayFormatPath(Activity activity, Uri uri) {
        if (uri == null) return "Not set";
        String path = uriToPath(activity, uri);
        path = path.equals(File.separator) ? uri.getPath() : path;

        String internal = getExternalStoragePublicDirectory(activity, "");

        String external = isSDPresent(activity) ? String.valueOf(ContextCompat.getExternalFilesDirs(activity, null)[1]) : null;

        if (external != null)
            external = external.replaceAll("Android/data/com.mugames.vidsnap/files", "");

        if (path.contains(internal))
            path = path.replaceAll(internal, "Internal Storage/");
        else if (external != null && path.contains(external))
            path = path.replaceAll(external, "External Storage/");
        else {
            return "Not set";
        }
        return path.replaceAll(File.separator, ">");
    }

    public static boolean isSDPresent(Context context) {
        File[] media = ContextCompat.getExternalFilesDirs(context, null);
        try {
            return media[1] != null;
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    public static String removeStuffFromName(String name) {

        name = name.replaceAll("https:.*", "");
        name = name.replaceAll("[@#~\\n\\t]", "");
        if (name.length() > 45) {
            int i = 45;
            while (unicode(name, i)) i++;
            name = name.substring(0, i);
        }
        return name;
    }

    private static boolean unicode(String s, int i) {
        s = s.substring(0, i);
        char c = s.charAt(i - 1);
        return Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN;
    }

    public static String getValidFile(String path, String name, String extension) {
        int num = 1;

        if (path == null) {
            return name +"."+ extension;
        }
        path = path + name.replaceAll("[\\|\\\\\\?\\*<>\":\n]+", "_")+"." + extension;

        File file = new File(path);

        while (file.exists()) {
            path = file.getParent() + "/" + name + "(" + (num++) + ")" +"."+ extension;
            file = new File(path);
        }
        return path;
    }


    public static boolean isFileNotExists(String fileDirectory) {
        return !new File(fileDirectory).exists();
    }

    /*-----------------------------------NOTE-------------------------------------------------*
     * Below functions are heavy tasks. it might delay to perform so it is recommended to call*
     * below functions from separate thread to prevent UI lock                                *
     * If you intend to run task on UI thread, no need to create new thread                   *
     * in such case pass null for FileStreamCallback.                                         *
     * In some case if you think no need of callback on operation completion but you still    *
     * need to use as separate thread, just pass null to callback. Callbacks aren't mandatory. *
     *---------------------------------------------------------------------------------------*/

    public static void deleteFile(String path, FileStreamCallback callback) {
        File file = new File(path);
        if (file.isDirectory()) {
            deleteFolder(file.getAbsolutePath());
        }
        if (file.exists()) {
            if (file.delete()) {
                if (callback != null)
                    callback.onFileOperationDone();
                return;
            }
            try {
                file.getCanonicalFile().delete();
            } catch (IOException e) {
                Log.e("TAG", "deleteFile: ", e);
            }
            if (callback != null) callback.onFileOperationDone();
        }
    }


    public static void saveFile(String path, Object value, FileStreamCallback callback) {
        File file = new File(path);
        try {
            if (value instanceof String) {
                FileWriter writer = new FileWriter(file);
                writer.write(String.valueOf(value));
                writer.close();
            } else if (value instanceof byte[]) {
                OutputStream outputStream = new FileOutputStream(file);
                outputStream.write((byte[]) value);
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (callback != null) callback.onFileOperationDone();
    }

    public static byte[] loadImage(String path) {
        File file = new File(path);
        try {
            InputStream inputStream = new FileInputStream(file);
            long fileSize = file.length();
            byte[] allBytes = new byte[(int) fileSize];
            inputStream.read(allBytes);
            return allBytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void unzip(File zipFile, File targetDirectory, FileStreamCallback fileStreamCallback) throws IOException {
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
            /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
            }
        } finally {
            zis.close();
        }
        if (fileStreamCallback != null)
            fileStreamCallback.onFileOperationDone();
    }


    private static void deleteFolder(String path) {
        File directory = new File(path);
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                deleteFile(file.getAbsolutePath(), null);
            }
            directory.delete();
        }
    }

    public static void moveCache(String newValue, Context context, FileStreamCallback fileStreamCallback) {
        File src;
        File dest;
        if (newValue.equals("external")) {
            src = context.getExternalFilesDirs("")[0];
            dest = context.getExternalFilesDirs("")[1];
        } else {
            src = context.getExternalFilesDirs("")[1];
            dest = context.getExternalFilesDirs("")[0];
        }
        moveFile(context, src, dest, null);
        if (fileStreamCallback != null) fileStreamCallback.onFileOperationDone();
    }

    static void copyFolder(Context context, File src, File dest) {
        for (File child : Objects.requireNonNull(src.listFiles())) {
            if (!child.getName().equals(".essential"))
                moveFile(context, child, new File(dest, child.getName()), null);
        }
    }

    public static void moveFile(Context context,File src, File dest, FileStreamCallback fileStreamCallback) {
        if (src.isDirectory()) {
            copyFolder(context,src, dest);
            return;
        }

        if (src.getName().endsWith(".muout") || src.getName().endsWith(".muvideo") || src.getName().endsWith(".muaudio"))
            return;

        copyFile(context,Uri.fromFile(src), Uri.fromFile(src), fileStreamCallback);
        deleteFile(src.getAbsolutePath(), null);
        if (fileStreamCallback != null) fileStreamCallback.onFileOperationDone();
    }


    public static void copyFile(Context context, Uri src, Uri dest, FileStreamCallback callback) {
        try {
            FileChannel inChannel = ((FileInputStream) context.getContentResolver().openInputStream(src)).getChannel();
            FileChannel outChannel;
            try{
                outChannel = new FileOutputStream(context.getContentResolver().openFileDescriptor(dest,"w").getFileDescriptor()).getChannel();
            }catch (FileNotFoundException e){
                outChannel = new FileOutputStream(dest.getPath()).getChannel();
            }
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inChannel.close();
            outChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (callback != null) callback.onFileOperationDone();
    }

    /**
     *
     * @param context application context
     * @param parentUri parent directory's uri
     * @param fileName current filename without extensions
     * @param mimeType mime type of current file
     * @return New uri for a file eg. if file exist name will be file(1)
     */
    public static Uri pathToNewUri(Context context, Uri parentUri, String fileName, String mimeType){
        DocumentFile directory;
        try {
            directory = DocumentFile.fromTreeUri(context, parentUri);
            DocumentFile file = directory.createFile(mimeType, fileName);
            return file.getUri();
        } catch (IllegalArgumentException|NullPointerException e) {
            return Uri.fromFile(new File(FileUtil.getValidFile(parentUri.getPath() + File.separator, fileName, MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType))));
        }
    }


    public static synchronized void scanMedia(Context context, String fileUri, MediaScannerConnection.OnScanCompletedListener listener){
        Uri uri = Uri.parse(fileUri);

        String path = FileUtil.uriToPath(context, uri);
        if (path.equals(File.separator)) path = uri.getPath();

        MediaScannerConnection.scanFile(context, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {
                if(uri == null)
                    uri = FileProvider.getUriForFile(context,context.getPackageName()+".provider",new File(path));
                listener.onScanCompleted(path,uri);
            }
        });
    }
}
