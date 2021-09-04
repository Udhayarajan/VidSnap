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

package com.mugames.vidsnap.PostProcessor;

public class FFMPEGInfo {


    public String mime_video;
    public String mime_audio;

    public String videoPath;
    public String audioPath;

    public String outPut;
    public String hlsURL;

    public FFMPEGInfo() {
    }



    public FFMPEGInfo(String mime_video, String mime_audio, String videoPath, String audioPath, String outPut) {
        this.mime_video = mime_video;
        this.mime_audio = mime_audio;
        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.outPut = outPut;
    }
}
