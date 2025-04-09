package com.hhst.youtubelite.downloader;

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

}
