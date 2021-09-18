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

package com.mugames.vidsnap.Extractor.Status;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.util.Size;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.mugames.vidsnap.BuildConfig;
import com.mugames.vidsnap.Extractor.Extractor;
import com.mugames.vidsnap.Storage.FileUtil;
import com.mugames.vidsnap.Utility.MIMEType;
import com.mugames.vidsnap.Utility.Statics;

import java.io.File;
import java.io.IOException;


public class WhatsApp extends Extractor {

    String TAG = Statics.TAG+":WhatsApp";
    static String WHATSAPP = "Android/media/com.whatsapp";
    String suffixPath = File.separator + "Media" + File.separator + ".Statuses";

    public WhatsApp() {
        super("WhatsApp");
    }

    @Override
    public void analyze(String url) {
        getDialogueInterface().show("Finding status...");
        String path = FileUtil.getExternalStoragePublicDirectory(getContext(), WHATSAPP) + File.separator + "WhatsApp"+suffixPath;
        File directory = new File(path);
        if (!directory.exists() || !directory.canRead() || directory.listFiles().length <= 0) {
            path = FileUtil.getExternalStoragePublicDirectory(getContext(), "WhatsApp") + suffixPath;
            directory = new File(path);
        }
        fetchDetails(directory);
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
        if(type!=null) {
            String fileUir = FileProvider.getUriForFile(getContext(), BuildConfig.APPLICATION_ID+".provider",file).toString();
            if (type.equals(MIMEType.VIDEO_MP4)) {
                formats.mainFileURLs.add(fileUir);
//                formats.thumbNailsBitMap.add(getThumbnailBitmap(file));
                formats.thumbNailsURL.add(fileUir);
                formats.videoSizes.add(file.length());
                formats.fileMime.add(MIMEType.VIDEO_MP4);
            }else if(type.equals(MIMEType.IMAGE_JPEG)){
                formats.mainFileURLs.add(fileUir);
//                formats.thumbNailsBitMap.add(getImageThumbnail(file));
                formats.thumbNailsURL.add(fileUir);
                formats.videoSizes.add(file.length());
                formats.fileMime.add(MIMEType.IMAGE_JPEG);
            }
        }

    }

    private Bitmap getImageThumbnail(File file){
        return ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(file.getAbsolutePath()),1080,1920);
    }

    private Bitmap getThumbnailBitmap(File file) {
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                bitmap =  ThumbnailUtils.createVideoThumbnail(file,new Size(1080,1920),new CancellationSignal());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            bitmap = ThumbnailUtils.createVideoThumbnail(file.getPath(),MediaStore.Images.Thumbnails.MINI_KIND);
        }
        return bitmap;
    }
}
