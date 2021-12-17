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
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.mugames.vidsnap.extractor.status;

import android.net.Uri;
import android.os.Build;
import android.webkit.MimeTypeMap;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import com.mugames.vidsnap.BuildConfig;
import com.mugames.vidsnap.extractor.Extractor;
import com.mugames.vidsnap.storage.AppPref;
import com.mugames.vidsnap.storage.FileUtil;
import com.mugames.vidsnap.utility.MIMEType;
import com.mugames.vidsnap.utility.Statics;

import java.io.File;


public class WhatsApp extends Extractor {

    String TAG = Statics.TAG + ":WhatsApp";
    public static final String SUFFIX_PATH = File.separator + "Media" + File.separator + ".Statuses";
    public static final String WHATSAPP = "Android/media/com.whatsapp";

    public WhatsApp() {
        super("WhatsApp");
    }

    @Override
    public void analyze(String url) {
        getDialogueInterface().show("Finding status...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            analyzeAboveR();
        } else {
            analyzeBelowR();
        }

    }

    @RequiresApi(Build.VERSION_CODES.R)
    void analyzeAboveR() {
        Uri whatsappUri = AppPref.getInstance(getContext()).getWhatsAppUri();
        DocumentFile[] statuses = DocumentFile.fromTreeUri(getContext(),whatsappUri).listFiles();
        for (DocumentFile file : statuses) {
            fetchDetailsOfFile(file);
        }
        whatsAppStatusAnalyzed();
    }

    void analyzeBelowR() {
        String path = FileUtil.getExternalStoragePublicDirectory(getContext(), "WhatsApp") + SUFFIX_PATH;
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files != null && (!directory.exists() || !directory.canRead() || files.length <= 0)) {
            fetchDetails(directory);
        } else {
            getDialogueInterface().error("WhatsApp Not installed", null);
        }
    }

    void fetchDetails(File directory) {
        for (File file : directory.listFiles()) {
            fetchDetailsOfFile(file);
        }
        whatsAppStatusAnalyzed();
    }

    void fetchDetailsOfFile(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (type != null) {
            String fileUir = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID + ".provider", file).toString();
            if (type.equals(MIMEType.VIDEO_MP4)) {
                formats.mainFileURLs.add(fileUir);
                formats.thumbNailsURL.add(fileUir);
                formats.videoSizes.add(file.length());
                formats.fileMime.add(MIMEType.VIDEO_MP4);
            } else if (type.equals(MIMEType.IMAGE_JPEG)) {
                formats.mainFileURLs.add(fileUir);
                formats.thumbNailsURL.add(fileUir);
                formats.videoSizes.add(file.length());
                formats.fileMime.add(MIMEType.IMAGE_JPEG);
            }
        }

    }

    void fetchDetailsOfFile(DocumentFile file) {
        String type = file.getType();
        if (type != null) {
            String fileUir = file.getUri().toString();
            if (type.equals(MIMEType.VIDEO_MP4)) {
                formats.mainFileURLs.add(fileUir);
                formats.thumbNailsURL.add(fileUir);
                formats.videoSizes.add(file.length());
                formats.fileMime.add(MIMEType.VIDEO_MP4);
            } else if (type.equals(MIMEType.IMAGE_JPEG)) {
                formats.mainFileURLs.add(fileUir);
                formats.thumbNailsURL.add(fileUir);
                formats.videoSizes.add(file.length());
                formats.fileMime.add(MIMEType.IMAGE_JPEG);
            }
        }

    }
}
