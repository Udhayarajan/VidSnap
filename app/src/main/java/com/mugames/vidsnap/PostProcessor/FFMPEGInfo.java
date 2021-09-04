package com.mugames.vidsnap.PostProcessor;

public class FFMPEGInfo {


    public String mime_video;
    public String mime_audio;

    public String videoPath;
    public String audioPath;

    public String outPut;

    public FFMPEGInfo(String mime_video, String mime_audio, String videoPath, String audioPath, String outPut) {
        this.mime_video = mime_video;
        this.mime_audio = mime_audio;
        this.videoPath = videoPath;
        this.audioPath = audioPath;
        this.outPut = outPut;
    }
}
