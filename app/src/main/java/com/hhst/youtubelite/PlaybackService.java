package com.hhst.youtubelite;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.hhst.youtubelite.webview.YoutubeWebview;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;


public class PlaybackService extends Service {

    private MediaSessionCompat mediaSession;
    private static final String CHANNEL_ID = "player_channel";
    private static final int NOTIFICATION_ID = 100;
    private NotificationManager notificationManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
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

                // --- Update Notification ---
                Notification updatedNotification = buildNotification(true);
                if (updatedNotification != null && notificationManager != null) {
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification);
                }
                // --- End Update ---
            }

            @Override
            public void onPause() {
                super.onPause();
                webview.evaluateJavascript("window.dispatchEvent(new Event('pause'));", null);

                // --- Update Notification ---
                Notification updatedNotification = buildNotification(false);
                if (updatedNotification != null && notificationManager != null) {
                    notificationManager.notify(NOTIFICATION_ID, updatedNotification);
                }
                // --- End Update ---
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
        mediaSession.setActive(true);
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

    private final Handler handler = new Handler(Looper.getMainLooper());

    private long lastProgressPos = 0L;
    private final Runnable timeoutRunnable = () -> updateProgress(lastProgressPos, 1f, false);

    public void updateProgress(long pos, float playbackSpeed, boolean isPlaying) {
        handler.removeCallbacks(timeoutRunnable);
        handler.postDelayed(timeoutRunnable, 1000);
        lastProgressPos = pos;
        var state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(state, pos, playbackSpeed)
                .build();
        mediaSession.setPlaybackState(playbackState);
    }

    // Helper method to build the notification based on current state
    private Notification buildNotification(boolean isPlaying) {
        // 1. Get current metadata from MediaSession
        MediaMetadataCompat metadata = mediaSession.getController().getMetadata();
        if (metadata == null) {
            // Handle case where metadata isn't available yet
            Log.w("PlaybackService", "Cannot build notification: Metadata is null");
            return null;
        }

        Bitmap largeIcon = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
        String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        String artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);

        // 2. Choose icon and title based on isPlaying
        int playPauseIconResId = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        String playPauseActionTitle = isPlaying ? "Pause" : "Play";

        // 3. Rebuild PendingIntent for app launch
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent == null) {
            intent = new Intent(this, MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 101, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 4. Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(artist)
                .setLargeIcon(largeIcon)
                .setContentIntent(pendingIntent)
                .setOngoing(isPlaying) // Keep notification when playing
                // --- Actions ---
                .addAction(R.drawable.ic_previous, "Previous", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                .addAction(playPauseIconResId, playPauseActionTitle, MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)) // Dynamic Icon
                .addAction(R.drawable.ic_next, "Next", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
                // --- Media Style ---
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        return builder.build();
    }

    public void showNotification(String title, String author, String thumbnail, long duration) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Bitmap largeIcon = fetchThumbnail(thumbnail);

            // Build initial metadata
            MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, largeIcon)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, author)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration * 1000)
                    .build();
            mediaSession.setMetadata(metadata); // Set metadata FIRST

            // Build initial playback state
            PlaybackStateCompat initialState = new PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                            PlaybackStateCompat.ACTION_SEEK_TO)
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0L, 1f)
                    .build();
            mediaSession.setPlaybackState(initialState); // Set the state in the session

            // Build the initial notification
            Notification initialNotification = buildNotification(true);

            if (initialNotification != null) {
                // Start foreground service with the initial notification
                startForeground(NOTIFICATION_ID, initialNotification);
            } else {
                Log.e("PlaybackService", "Failed to create initial notification.");
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
