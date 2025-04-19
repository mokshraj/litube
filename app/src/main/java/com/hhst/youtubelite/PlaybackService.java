package com.hhst.youtubelite;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hhst.youtubelite.webview.YoutubeWebview;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;


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
                        Math.round(pos / 1000f)
                ), null);
            }
        });
    }


    private Bitmap fetchThumbnail(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(5000);
            Bitmap original = BitmapFactory.decodeStream(conn.getInputStream());

            // centered clip
            int width = original.getWidth();
            int height = original.getHeight();

            int size = Math.min(width, height);

            int x = (width - size) / 2;
            int y = (height - size) / 2;

            return Bitmap.createBitmap(original, x, y, size, size);
        } catch (IOException e) {
            Log.e("fetch thumbnail error", Log.getStackTraceString(e));
            return null;
        }
    }

    public void updateProgress(long pos, float playbackSpeed, boolean isPlaying) {
        var state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(state, pos, playbackSpeed)
                .build();
        mediaSession.setPlaybackState(playbackState);
    }

    public void showNotification(String title, String thumbnail, long duration) {
        Executors.newSingleThreadExecutor().execute(() -> {
            // build metadata
            MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, fetchThumbnail(thumbnail))
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration * 1000)
                    .build();
            mediaSession.setMetadata(metadata);

            // go back to app when click controller
            Intent intent = getPackageManager()
                    .getLaunchIntentForPackage(getPackageName());
            if (intent == null) {
                intent = new Intent(this, MainActivity.class);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    101,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // build notification
            var notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken()))
                    .setContentIntent(pendingIntent)
                    .build();

            // initialize progress
            updateProgress(0L, 1f, false);

            // set to foreground service
            startForeground(100, notification);

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
