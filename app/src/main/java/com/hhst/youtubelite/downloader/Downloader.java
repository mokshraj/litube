package com.hhst.youtubelite.downloader;


import android.util.Log;

import com.google.gson.Gson;
import com.tencent.mmkv.MMKV;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static DownloadDetails info(String video_url)
            throws YoutubeDL.CanceledException, YoutubeDLException, InterruptedException {
        YoutubeDLRequest request = new YoutubeDLRequest(video_url);
        VideoInfo info = YoutubeDL.getInstance().getInfo(request);
        DownloadDetails details = new DownloadDetails();
        details.setId(info.getId());
        details.setTitle(info.getTitle());
        details.setAuthor(info.getUploader());
        details.setDescription(info.getDescription());
        details.setDuration((long) info.getDuration());
        details.setThumbnail(info.getThumbnail());
        details.setFormats(info.getFormats());
        return details;
    }

    private static final MMKV cache = MMKV.defaultMMKV();
    private static final Gson gson = new Gson();

    synchronized public static DownloadDetails infoWithCache(String url) throws Exception {
        // get video id from url
        Pattern pattern = Pattern.compile("^https?://.*(?:youtu\\.be/|v/|u/\\w/|embed/|watch\\?v=)([^#&?]*).*$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            String id = matcher.group(1);
            DownloadDetails details = gson.fromJson(cache.decodeString(id), DownloadDetails.class);
            // validate cached details
            if (details == null || details.getTitle() == null || details.getAuthor() == null
                    || details.getThumbnail() == null || details.getFormats() == null || details.getFormats().isEmpty()) {
                details = info(url);
                cache.encode(id, gson.toJson(details), 60 * 60 * 24 * 7);
            }
            return details;
        }
        throw new RuntimeException("Invalid url");
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
        request.addOption("--embed-thumbnail");
        request.addOption("--concurrent-fragments", 8);
        request.addOption("-f","bestaudio[ext=m4a]");
        request.addOption("-o", output.getAbsolutePath() + ".m4a");
        if (video_format != null && video_format.getFormatId() != null) {
            request.addOption("-f", String.format("%s+bestaudio[ext=m4a]", video_format.getFormatId()));
            request.addOption("-o", output.getAbsolutePath());
        }
        YoutubeDLResponse response = YoutubeDL.getInstance().execute(request, processId, callback);
        Log.d("yt-dlp download command", response.getCommand().toString());
    }

    public static boolean cancel(String processId) {
        return YoutubeDL.getInstance().destroyProcessById(processId);
    }

}
