package com.hhst.youtubelite.downloader;


import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;

import kotlin.Unit;
import kotlin.jvm.functions.Function3;

/**
 * the main class for download video and audio
 */
public class Downloader {

    /**
     *
     * @param video_url not the video id but the whole url
     * @return DownloadDetails contains everything we need
     */
    public static DownloadDetails info(String video_url) throws YoutubeDL.CanceledException, YoutubeDLException, InterruptedException {
        VideoInfo info = YoutubeDL.getInstance().getInfo(video_url);
        DownloadDetails details = new DownloadDetails();
        details.setId(info.getId());
        details.setTitle(info.getTitle());
        details.setAuthor(info.getUploader());
        details.setDescription(info.getDescription());
        details.setThumbnail(info.getThumbnail());
        details.setFormats(info.getFormats());
        return details;
    }

    public static void download(
            String processId,
            String video_url,
            VideoFormat video_format,
            File output,
            Function3<Float, Long, String, Unit> callback
    ) throws YoutubeDL.CanceledException, InterruptedException, YoutubeDLException {
        YoutubeDLRequest request = new YoutubeDLRequest(video_url);
        request.addOption("--no-mtime");
        request.addOption("--concurrent-fragments", 8);
        request.addOption("-f","bestaudio[ext=m4a]");
        request.addOption("-o", output.getAbsolutePath() + ".m4a");
        if (video_format != null && video_format.getFormatId() != null) {
            request.addOption("-f", String.format("%s+bestaudio[ext=m4a]", video_format.getFormatId()));
            request.addOption("-o", output.getAbsolutePath());
        }
        YoutubeDL.getInstance().execute(request, processId, callback);
    }

    public static boolean cancel(String processId) {
        return YoutubeDL.getInstance().destroyProcessById(processId);
    }

}
