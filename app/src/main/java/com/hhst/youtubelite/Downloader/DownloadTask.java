package com.hhst.youtubelite.Downloader;

import com.yausername.youtubedl_android.mapper.VideoFormat;

import java.io.File;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class DownloadTask {
    private String url;
    private String fileName;
    private String thumbnail;
    private VideoFormat videoFormat;
    private Boolean isAudio;
    private DownloaderState state;
    private File outputDir;
    private File output;
    private DownloadNotification notification;

    public DownloadTask(String url, String fileName, String thumbnail, VideoFormat videoFormat, Boolean isAudio, DownloaderState state) {
        this.url = url;
        this.fileName = fileName;
        this.thumbnail = thumbnail;
        this.videoFormat = videoFormat;
        this.isAudio = isAudio;
        this.state = state;
    }
}
