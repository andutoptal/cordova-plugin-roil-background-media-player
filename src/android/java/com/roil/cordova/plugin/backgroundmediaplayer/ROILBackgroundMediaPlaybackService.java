package com.roil.cordova.plugin.backgroundmediaplayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class ROILBackgroundMediaPlaybackService extends MediaBrowserServiceCompat {
    private static final String EMPTY_MEDIA_ROOT_ID = "empty_root_id";
    private static final String LOG_TAG = "ROILBackgroundMediaPlaybackService";
    private static final String NOTIFICATION_CHANNEL_ID = "ROILBackgroundPlayerNotificationChannel";

    private AtomicInteger nextNotificationId = new AtomicInteger();
    private MediaPlayer player;
    private MediaSessionCompat mediaSession;
    private TimerTask progressUpdatesTask;
    private Timer progressUpdatesTimer;

    private int currentNotificationId = 0;

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            boolean handled = true;
            KeyEvent keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                int keyCode = keyEvent.getKeyCode();
                switch(keyCode) {
                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        onPlay();
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        onPause();
                        break;
                    default:
                        handled = super.onMediaButtonEvent(mediaButtonEvent);
                        break;
                }
            }

            return handled;
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            if (player != null) {
                player.stop();
                player.release();
            }

            try {
                player = new MediaPlayer();
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build();
                player.setAudioAttributes(audioAttributes);
                player.setDataSource(ROILBackgroundMediaPlaybackService.this, uri);
                player.setOnCompletionListener(mediaPlayer -> {
                    clearProgressUpdates();
                    PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                            .setState(PlaybackStateCompat.STATE_PAUSED, player.getDuration(), 1.0f)
                            .build();

                    mediaSession.setPlaybackState(playbackState);
                });
                player.prepareAsync();
            } catch (IOException e) {
                //
            }
        }

        @Override
        public void onStop() {
            clearProgressUpdates();

            player.pause();
            mediaSession.setActive(false);

            stopForeground(false);
            clearNotification();
            stopSelf();
        }

        @Override
        public void onPause() {
            if (player.isPlaying()) {
                clearProgressUpdates();
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED, 0.0f);

                player.pause();
                mediaSession.setActive(false);

                stopForeground(false);

                buildNotification();
            }
        }

        @Override
        public void onPlay() {
            if (!player.isPlaying()) {
                player.start();
                mediaSession.setActive(true);

                scheduleProgressUpdates();
                buildNotification();
            }

        }

        @Override
        public void onSeekTo(long pos) {
            player.seekTo((int) pos);
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                    action.equals(ROILBackgroundMediaPlayer.CUSTOM_ACTION_SET_PLAYBACK_SPEED_ACTION_NAME)) {
                float playbackSpeed = extras.getFloat(ROILBackgroundMediaPlayer.CUSTOM_ACTION_SET_PLAYBACK_SPEED_PARAM_NAME);
                PlaybackParams playbackParams = new PlaybackParams();
                playbackParams.setSpeed(playbackSpeed);
                player.setPlaybackParams(playbackParams);
                scheduleProgressUpdates();
            } else if (action.equals(ROILBackgroundMediaPlayer.CUSTOM_ACTION_SET_SESSION_METADATA_ACTION_NAME)) {
                String notificationTitle = extras.getString(ROILBackgroundMediaPlayer.CUSTOM_ACTION_SET_SESSION_METADATA_TITLE_PARAM_NAME);
                Bitmap notificationIcon = extras.getParcelable(ROILBackgroundMediaPlayer.CUSTOM_ACTION_SET_SESSION_METADATA_ICON_PARAM_NAME);

                MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, notificationTitle.substring(0, notificationTitle.indexOf(" - ")));
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, notificationTitle.substring(notificationTitle.indexOf(" - ") + 3));
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, notificationIcon);

                mediaSession.setMetadata(metadataBuilder.build());
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = this;

        mediaSession = new MediaSessionCompat(context, LOG_TAG);
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE);

        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setCallback(new MediaSessionCallback());

        setSessionToken(mediaSession.getSessionToken());
    }

    @Override
    public void onDestroy() {
        this.clearProgressUpdates();
        this.clearNotification();

        if (player != null) {
            if (player.isPlaying()) {
                player.stop();
            }
            player.release();
        }

        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(EMPTY_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    private void clearNotification() {
        if (currentNotificationId != 0) {
            NotificationManagerCompat.from(this).cancel(currentNotificationId);
            currentNotificationId = 0;
        }
    }

    private void clearProgressUpdates() {
        if (progressUpdatesTask != null) {
            progressUpdatesTask.cancel();
            progressUpdatesTask = null;
        }

        if (progressUpdatesTimer != null) {
            progressUpdatesTimer.cancel();
            progressUpdatesTimer.purge();
            progressUpdatesTimer = null;
        }
    }

    private int getIconResourceId(String name) {
        return getResources().getIdentifier(name,"drawable", getPackageName());
    }

    private float getPlaybackSpeed() {
        float playbackSpeed;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            playbackSpeed = player.getPlaybackParams().getSpeed();
        } else {
            playbackSpeed = 1.0f;
        }

        return playbackSpeed;
    }

    private void buildNotification() {
        if (currentNotificationId == 0 || currentNotificationId == nextNotificationId.get()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String appName = getString(getResources().getIdentifier(
                        "app_name",
                        "string",
                        getPackageName()
                        )
                );
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, appName, importance);
                channel.setDescription(appName);
                NotificationManager notificationManager = this.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }

            if (currentNotificationId == 0) {
                currentNotificationId = nextNotificationId.incrementAndGet();
            }
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

            MediaControllerCompat mediaController = mediaSession.getController();
            MediaMetadataCompat metadata = mediaController.getMetadata();

            builder
                    .setContentTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                    .setContentText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                    .setLargeIcon(metadata.getDescription().getIconBitmap())
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSmallIcon(getIconResourceId(
                            getString(
                                    getResources().getIdentifier(
                                            "notification_app_icon",
                                            "string",
                                            getPackageName()
                                    )
                            )
                    ))
                    .setOnlyAlertOnce(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setStyle(
                        new androidx.media.app.NotificationCompat.MediaStyle()
                                .setMediaSession(mediaSession.getSessionToken())
                                .setShowActionsInCompactView(0)
                                .setShowCancelButton(true)
                                .setCancelButtonIntent(
                                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                                this,
                                                PlaybackStateCompat.ACTION_PAUSE
                                        )
                                )
                );
            }

            if (player.isPlaying()) {
                builder
                        .setOngoing(true)
                        .addAction(
                                new NotificationCompat.Action(
                                        getIconResourceId("ic_pause"),
                                        getString(getResources().getIdentifier(
                                                "notification_pause_button_label",
                                                "string",
                                                getPackageName()
                                                )
                                        ),
                                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                                this,
                                                PlaybackStateCompat.ACTION_PAUSE
                                        )
                                )
                        );

                startForeground(currentNotificationId, builder.build());
            } else {
                builder
                        .setOngoing(false)
                        .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackStateCompat.ACTION_STOP
                        ))
                        .addAction(
                                new NotificationCompat.Action(
                                        getIconResourceId("ic_play"),
                                        getString(getResources().getIdentifier(
                                                "notification_play_button_label",
                                                "string",
                                                getPackageName()
                                                )
                                        ),
                                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                                this,
                                                PlaybackStateCompat.ACTION_PLAY
                                        )
                                )
                        );
                NotificationManagerCompat.from(this).notify(currentNotificationId, builder.build());
            }
        }
    }

    private void scheduleProgressUpdates() {
        progressUpdatesTask = new TimerTask() {
            @Override
            public void run() {
                int playbackState = player.isPlaying() ? PlaybackStateCompat.STATE_PLAYING :
                        PlaybackStateCompat.STATE_PAUSED;
                setPlaybackState(playbackState, getPlaybackSpeed());
            }
        };
        progressUpdatesTimer = new Timer();
        progressUpdatesTimer.scheduleAtFixedRate(progressUpdatesTask, 0, 500);
    }

    private void setPlaybackState(int newState, float playbackSpeed) {
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setState(newState, player.getCurrentPosition(), playbackSpeed)
                .build();

        mediaSession.setPlaybackState(playbackState);
    }
}
