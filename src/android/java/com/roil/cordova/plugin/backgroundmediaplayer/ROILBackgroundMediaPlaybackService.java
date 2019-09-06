package com.roil.cordova.plugin.backgroundmediaplayer;

import android.annotation.TargetApi;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ROILBackgroundMediaPlaybackService extends MediaBrowserServiceCompat {
    private static final String LOG_TAG = "ROILBackgroundMediaPlaybackService";
    private static final String EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private MediaPlayer player;
    private TimerTask progressUpdatesTask;
    private Timer progressUpdatesTimer;

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            if (player != null) {
                player.stop();
                player.release();
            }

            try {
                player = new MediaPlayer();
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
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
        public void onPause() {
            if (player.isPlaying()) {
                player.pause();
                clearProgressUpdates();
            }
        }

        @Override
        public void onPlay() {
            if (!player.isPlaying()) {
                player.start();
                scheduleProgressUpdates();
            }
        }

        @Override
        public void onSeekTo(long pos) {
            player.seekTo((int) pos);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.M)
        public void onCustomAction(String action, Bundle extras) {
            if (action.equals(ROILBackgroundMediaPlayer.CUSTOM_ACTION_SET_PLAYBACK_SPEED_ACTION_NAME)) {
                float playbackSpeed = extras.getFloat(ROILBackgroundMediaPlayer.CUSTOM_ACTION_SET_PLAYBACK_SPEED_PARAM_NAME);
                PlaybackParams playbackParams = new PlaybackParams();
                playbackParams.setSpeed(playbackSpeed);
                player.setPlaybackParams(playbackParams);
                scheduleProgressUpdates();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mediaSession = new MediaSessionCompat(this, LOG_TAG);

        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        mediaSession.setCallback(new MediaSessionCallback());

        setSessionToken(mediaSession.getSessionToken());
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

    private void scheduleProgressUpdates() {
        progressUpdatesTask = new TimerTask() {
            @Override
            public void run() {
                float playbackSpeed;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    playbackSpeed = player.getPlaybackParams().getSpeed();
                } else {
                    playbackSpeed = 1.0f;
                }
                PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, player.getCurrentPosition(), playbackSpeed)
                        .build();

                mediaSession.setPlaybackState(playbackState);
            }
        };
        progressUpdatesTimer = new Timer();
        progressUpdatesTimer.scheduleAtFixedRate(progressUpdatesTask, 0, 500);
    }
}
