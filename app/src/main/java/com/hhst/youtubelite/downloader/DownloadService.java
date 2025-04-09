package com.hhst.youtubelite.downloader;

import android.app.Service;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.hhst.youtubelite.R;
import com.tencent.mmkv.MMKV;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadService extends Service {

    public final int max_download_tasks = 5;
    private ConcurrentHashMap<Integer, DownloadTask> download_tasks;
    private ExecutorService download_executor;

    private final Handler notificationHandler = new Handler(Looper.getMainLooper());

    private MMKV cache;
    private final Gson gson = new Gson();

    public DownloadDetails infoWithCache(String url) throws Exception {
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
                details = Downloader.info(url);
                cache.encode(id, gson.toJson(details), 60 * 60 * 24 * 7);
            }
            return details;
        }
        throw new RuntimeException("Invalid url");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        download_tasks = new ConcurrentHashMap<>();
        download_executor = Executors.newFixedThreadPool(max_download_tasks);
        MMKV.initialize(this);
        cache = MMKV.defaultMMKV();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        int taskId = intent.getIntExtra("taskId", -1);
        if ("CANCEL_DOWNLOAD".equals(action)) {
            cancelDownload(taskId);
        } else if ("RETRY_DOWNLOAD".equals(action)) {
            retryDownload(taskId);
        } else if ("DELETE_DOWNLOAD".equals(action)) {
            deleteDownload(taskId);
        } else if ("DOWNLOAD_THUMBNAIL".equals(action)) {
            String url = intent.getStringExtra("thumbnail");
            String filename = sanitizeFileName(intent.getStringExtra("filename"));
            File outputDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), getString(R.string.app_name));
            File outputFile = new File(outputDir, filename + ".jpg");
            downloadThumbnail(url, outputFile);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public class DownloadBinder extends Binder {
        public DownloadService getService() {
            return DownloadService.this;
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new DownloadBinder();
    }

    private String sanitizeFileName(String fileName) {
        Pattern INVALID_FILENAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");
        return INVALID_FILENAME_PATTERN.matcher(fileName).replaceAll("_");
    }

    private void showToast(String content) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
                this,
                content,
                Toast.LENGTH_SHORT
        ).show());
    }

    private void downloadThumbnail(String thumbnail, File outputFile) {
        if (thumbnail != null) {
            download_executor.submit(() -> {
                try {
                    FileUtils.copyURLToFile(new URL(thumbnail), outputFile);
                    // notify to scan
                    MediaScannerConnection.scanFile(this,
                            new String[]{outputFile.getAbsolutePath()}, null, null);
                    showToast(getString(R.string.thumbnail_has_been_saved_to) + outputFile);
                } catch (Exception e) {
                    Log.e(getString(R.string.failed_to_download_thumbnail),
                            Log.getStackTraceString(e));
                    showToast(getString(R.string.failed_to_download_thumbnail));
                }

            });
        }
    }

    private void executeDownload(DownloadTask task) {
        String fileName = task.getIsAudio() ? String.format("(audio only) %s", task.getFileName())
                : task.getFileName();
        int taskId = download_tasks.size();
        DownloadNotification notification = new DownloadNotification(this, taskId);

        // Show notification
        notificationHandler.post(() -> notification.showNotification(fileName, 0));


        task.setNotification(notification);
        download_tasks.put(taskId, task);

        download_executor.submit(() -> {
            try {
                Downloader.download(
                        "download_task" + taskId,
                        task.getUrl(),
                        task.getVideoFormat(),
                        new File(getCacheDir(), task.getFileName()),
                        (progress, eta, information) -> {
                            notificationHandler.post(()
                                    -> notification.updateProgress(Math.round(progress), information));
                            return null;
                        });
            } catch (YoutubeDLException e) {
                Log.e(getString(R.string.failed_to_download), Log.getStackTraceString(e));
                showToast(getString(R.string.failed_to_download) + e);
                task.getNotification().cancelDownload(getString(R.string.failed_to_download));
                task.setState(DownloaderState.STOPPED);
                return;
            } catch (YoutubeDL.CanceledException e) {
                Log.e(getString(R.string.failed_to_download), Log.getStackTraceString(e));
                showToast(getString(R.string.download_canceled));
                task.getNotification().cancelDownload(getString(R.string.download_canceled));
                task.setState(DownloaderState.STOPPED);
                return;
            } catch (InterruptedException e) {
                Log.e(getString(R.string.failed_to_download), Log.getStackTraceString(e));
                task.setState(DownloaderState.STOPPED);
                return;
            }
            File audio = new File(getCacheDir(), task.getFileName() + ".m4a");
            File video = new File(getCacheDir(), task.getFileName() + ".mp4");
            File output;
            // after download
            if (task.getIsAudio()) {
                output = new File(task.getOutputDir(), task.getFileName() + ".m4a");
                task.setOutput(output);
                // Move audio file to public directory
                try {
                    FileUtils.moveFile(audio, output);
                } catch (IOException e) {
                    Log.e(getString(R.string.audio_move_error), Log.getStackTraceString(e));
                    notificationHandler.post(()
                            -> notification.cancelDownload(getString(R.string.audio_move_error)));
                    showToast(getString(R.string.audio_move_error));
                    return;
                }
                notificationHandler.post(() -> notification.completeDownload(
                        String.format(getString(R.string.download_finished), fileName, output.getPath()),
                        output,
                        "audio/*"
                ));
            } else {
                output = new File(task.getOutputDir(), task.getFileName() + ".mp4");
                task.setOutput(output);
                // Move video file to public directory
                try {
                    FileUtils.moveFile(video, output);
                } catch (IOException e) {
                    Log.e(getString(R.string.video_move_error), Log.getStackTraceString(e));
                    notificationHandler.post(() ->
                            notification.cancelDownload(getString(R.string.video_move_error)));
                    showToast(getString(R.string.video_move_error));
                    return;
                }
                notificationHandler.post(() ->
                        notification.completeDownload(
                                String.format(getString(R.string.download_finished), fileName, output.getPath()),
                                output,
                                "video/*"
                        ));
            }

            showToast(String.format(getString(R.string.download_finished), fileName, output.getPath()));
            // notify to scan
            MediaScannerConnection.scanFile(this, new String[]{output.getAbsolutePath()}, null, null);
            task.setState(DownloaderState.FINISHED);
        });

    }


    public void initiateDownload(DownloadTask task) {
        download_executor.submit(() -> {
            // check and create output directory
            File outputDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    getString(R.string.app_name));

            if (!outputDir.exists() && !outputDir.mkdir()) {
                return;
            }

            // replace illegal character in filename
            task.setFileName(sanitizeFileName(task.getFileName()));

            // download thumbnail
            downloadThumbnail(task.getThumbnail(), new File(outputDir, task.getFileName() + ".jpg"));

            // download audio
            if (task.getIsAudio()) {
                executeDownload(new DownloadTask(
                        task.getUrl(),
                        task.getFileName(),
                        null,
                        null,
                        true,
                        DownloaderState.RUNNING,
                        outputDir,
                        null,
                        null
                ));
            }
            // download video
            if (task.getVideoFormat() != null) {
                executeDownload(new DownloadTask(
                        task.getUrl(),
                        task.getFileName(),
                        null,
                        task.getVideoFormat(),
                        false,
                        DownloaderState.RUNNING,
                        outputDir,
                        null,
                        null
                ));
            }

        });
    }


    // user click cancel button to trigger cancel task event
    private void cancelDownload(int taskId) {
        DownloadTask task = download_tasks.get(taskId);
        if (task == null) {
            return;
        }
        if (task.getState() == DownloaderState.RUNNING) {
            if (Downloader.cancel("download_task" + taskId)) {
                showToast(getString(R.string.download_canceled));
            } else {
                // cancel error
                showToast(getString(R.string.download_canceled_err));
            }
        }

        // remove output file
        removeFile(task.getOutput());

        // Set the running flag to false to stop the task
        // This will halt the progress updates and allow for the next notification handling
        task.getNotification().cancelDownload("");
        task.setState(DownloaderState.STOPPED);
    }

    // user click retry button to trigger this
    private void retryDownload(int taskId) {
        DownloadTask task = download_tasks.get(taskId);
        if (task == null) {
            return;
        }
        // try cancel original task first
        if (task.getState() == DownloaderState.RUNNING) {
            if (Downloader.cancel("download_task" + taskId)) {
                showToast(getString(R.string.download_canceled));
            } else {
                // cancel error
                showToast(getString(R.string.download_canceled_err));
            }
        }

        showToast(getString(R.string.retry_download) + task.getFileName());
        // remove output files
        removeFile(task.getOutput());

        // Set the running flag to false to stop the task
        // This will halt the progress updates and allow for the next notification handling
        task.setState(DownloaderState.STOPPED);

        // dismiss the notification
        task.getNotification().clearDownload();

        if (task.getIsAudio()) {
            // initiate new download task for the video
            executeDownload(new DownloadTask(
                    task.getUrl(),
                    task.getFileName(),
                    null,
                    null,
                    true,
                    DownloaderState.RUNNING,
                    task.getOutputDir(),
                    null,
                    null
            ));
        } else {
            executeDownload(new DownloadTask(
                    task.getUrl(),
                    task.getFileName(),
                    null,
                    task.getVideoFormat(),
                    false,
                    DownloaderState.RUNNING,
                    task.getOutputDir(),
                    null,
                    null
            ));
        }

    }

    // delete download file after download has finished
    private void deleteDownload(int taskId) {
        DownloadTask task = download_tasks.get(taskId);
        if (task == null) {
            return;
        }
        if (task.getState() == DownloaderState.FINISHED) {
            // only triggered this under the COMPLETED flag
            try {
                FileUtils.forceDelete(task.getOutput());
                // show toast
                showToast(getString(R.string.file_deleted));
            } catch (IOException e) {
                Log.e(getString(R.string.failed_to_delete), Log.getStackTraceString(e));
                showToast(getString(R.string.failed_to_delete));
            } finally {
                // dismiss the notification
                task.getNotification().clearDownload();
            }
        }
    }

    private void removeFile(File file) {
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            Log.e(getString(R.string.failed_to_delete), Log.getStackTraceString(e));
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        // clear all notifications
        if (!download_tasks.isEmpty()) {
            Objects.requireNonNull(download_tasks.get(0)).getNotification().clearAll();
        }

        // cancel all task
        for (Map.Entry<Integer, DownloadTask> entry : download_tasks.entrySet()) {
            DownloadTask task = entry.getValue();
            if (task.getState() == DownloaderState.RUNNING) {
                Downloader.cancel("download_task" + entry.getKey());
            }
        }

        if (download_executor != null && !download_executor.isShutdown()) {
            download_executor.shutdownNow();
        }
        super.onDestroy();
    }

}