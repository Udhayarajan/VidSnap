package com.mugames.vidsnap.Utility;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class FileUtil {
    static String PRIMARY_VOLUME = "primary";
    static String PRIMARY_TREE = "/tree/primary";

    public static String uriToPath(Activity activity, @Nullable final Uri uri) {

        if(uri==null) return null;
        String volumeID = getIDFromUri(uri, activity);
        String volumePath = getVolumePath(volumeID, activity);

        if (volumePath == null) return File.separator;

        if (volumePath.endsWith(File.separator))
            volumePath = volumePath.substring(0, volumePath.length() - 1);

        String documentPath = docPathFromUri(uri);
        String documentName = getFilePath(uri, activity, documentPath);

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

    private static String getFilePath(Uri uri, Activity activity, String documentPath) {
        try {
            String[] split;
            if (DocumentsContract.isDocumentUri(activity, uri)) {
                String id = DocumentsContract.getDocumentId(uri);
                split = id.split(":");
            } else {
                split = uri.getPath().split(":");
            }

            if (split.length > 1) return split[1].replace(documentPath,"");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String getIDFromUri(Uri uri, Activity activity) {
        try {
            String[] split;
            if (DocumentsContract.isDocumentUri(activity, uri)) {
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

    private static String getVolumePath(String volumeID, Activity activity) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            return getVolumePreR(volumeID, activity);
        else
            return getVolumePostR(volumeID, activity);//Includes R and above
    }


    private static String getVolumePreR(String volumeID, Activity activity) {
        try {
            StorageManager manager = (StorageManager) activity.getSystemService(Context.STORAGE_SERVICE);
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
    private static String getVolumePostR(String volumeID, Activity activity) {
        StorageManager manager = (StorageManager) activity.getSystemService(Context.STORAGE_SERVICE);

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

    public static boolean isInternalPath(Context context, String path) {
        return path.contains(String.valueOf(ContextCompat.getExternalFilesDirs(context, null)[0]));
    }


    public static String displayFormatPath(Activity activity, Uri uri) {
        if(uri==null) return "Not set";
        String path = uriToPath(activity,uri);
        path = path.equals(File.separator)?uri.getPath():path;
        String internal = getExternalStoragePublicDirectory(activity,"");

        String external = isSDPresent(activity) ? String.valueOf(ContextCompat.getExternalFilesDirs(activity, null)[1]) : null;

        if(external!=null)
            external = external.replaceAll("Android/data/com.mugames.vidsnap/files","");

        if (path.contains(internal))
            path = path.replaceAll(internal, "Internal Storage/");
        if (external != null && path.contains(external))
            path = path.replaceAll(external, "External Storage/");
        return path.replaceAll(File.separator, ">");
    }

    public static boolean isSDPresent(Context context) {
        return ContextCompat.getExternalFilesDirs(context, null).length >= 2;
    }

    public static String removeStuffFromName(String name) {

        name = name.replaceAll("https:.*", "");
        name = name.replaceAll("[@#~].*", "");
        if (name.length() > 45) {
            int i = 45;
            while (unicode(name, i)) i++;
            name = name.substring(0,i);
        }
        return name;
    }

    private static boolean unicode(String s, int i) {
        s=s.substring(0,i);
        char c = s.charAt(i-1);
        return Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN;
    }

    public static String GetValidFile(String path, String name, String extention) {
        int num = 1;

        if (path == null) {
            return name + extention;
        }
        path = path + name.replaceAll("[\\|\\\\\\?\\*<>\":\n]+", "_") + extention;

        File file = new File(path);

        while (file.exists()) {
            path = file.getParent() + "/" + name + "(" + (num++) + ")" + extention;
            file = new File(path);
        }
        return path;
    }

    public static boolean deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.delete())
                return true;
            try {
                return file.getCanonicalFile().delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public static void saveFile(String path, Object value) {
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
    }

    public static Object loadFile(String path, Class<?> type) {
        File file = new File(path);
        if (type == String.class) {
            StringBuilder result = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while (true) {
                    String line = reader.readLine();
                    if (line == null || line.isEmpty()) break;
                    line = line.substring(1, line.length() - 1);
                    if (line.contains("]")) throw new Exception("] is there");
                    result.append(line);
                }
                reader.close();
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        } else {
            try {
                InputStream inputStream = new FileInputStream(file);
                long fileSize = file.length();
                byte[] allBytes = new byte[(int) fileSize];
                inputStream.read(allBytes);
                return allBytes;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static boolean isFileExists(String fileDirectory){
        return new File(fileDirectory).exists();
    }

    public static void unzip(File zipFile, File targetDirectory) throws IOException {
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
    }


}
