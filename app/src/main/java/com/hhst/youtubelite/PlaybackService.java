package com.hhst.youtubelite;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hhst.youtubelite.downloader.DownloadDetails;
import com.hhst.youtubelite.downloader.Downloader;
import com.hhst.youtubelite.webview.YoutubeWebview;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlaybackService extends Service {

    private MediaSessionCompat mediaSession;
    private static final String CHANNEL_ID = "player_channel";
    private NotificationManager notificationManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public class PlaybackBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new PlaybackBinder();
    }

    public void initialize(YoutubeWebview webview) {
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                webview.evaluateJavascript("window.dispatchEvent(new Event('play'));", null);
            }

            @Override
            public void onPause() {
                super.onPause();
                webview.evaluateJavascript("window.dispatchEvent(new Event('pause'));", null);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                webview.evaluateJavascript("window.dispatchEvent(new Event('skipToNext'));", null);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                webview.evaluateJavascript("window.dispatchEvent(new Event('skipToPrevious'));", null);
            }

            @SuppressLint("DefaultLocale")
            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
                webview.evaluateJavascript(String.format(
                        "window.dispatchEvent(new CustomEvent('seek', { detail: { time: %d } }));",
                        pos / 1000
                ), null);
            }
        });
    }


    private Bitmap fetchThumbnail(String url) {
        Request request = new Request.Builder().url(url).build();
        try (Response response = new OkHttpClient().newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                InputStream inputStream = response.body().byteStream();
                return BitmapFactory.decodeStream(inputStream);
            } else {
                Log.e("Failed to fetch image", response.message());
            }
        } catch (IOException e) {
            Log.e("Failed to fetch image", Log.getStackTraceString(e));
        }
        return null;
    }

    public void updateProgress(long pos, float playbackSpeed, boolean isPlaying) {
        var state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(state , pos, playbackSpeed)
                .build();
        mediaSession.setPlaybackState(playbackState);
    }

    public void showNotification(String url) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                DownloadDetails details = Downloader.infoWithCache(url);
                MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, fetchThumbnail(details.getThumbnail()));
                builder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, details.getTitle());
                builder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, details.getAuthor());
                builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, details.getDuration() * 1000);
                mediaSession.setMetadata(builder.build());

                updateProgress(0L, 1f, false);

                var notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                                .setMediaSession(mediaSession.getSessionToken()))
                        .build();

                startForeground(100, notification);
            } catch (Exception e) {
                if (e instanceof InterruptedException) return;
                Log.e(getString(R.string.failed_to_load_video_details), Log.getStackTraceString(e));
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(this, R.string.failed_to_load_video_details, Toast.LENGTH_SHORT).show());
            }
        });

    }

    public void hideNotification() {
        stopForeground(true);
        notificationManager.cancelAll();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(this, "MediaSession");

        // create notification channel
        var channel = new NotificationChannel(
                CHANNEL_ID,
                "Player Channel",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Channel for player controller notifications");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        notificationManager.cancelAll();
        mediaSession.release();
    }
}
