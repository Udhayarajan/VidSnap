package com.mugames.vidsnap.PostProcessor;

import com.arthenica.ffmpegkit.Session;

public interface FFmpegCallbacks {
    void apply(Session session, String outputPath);
}
