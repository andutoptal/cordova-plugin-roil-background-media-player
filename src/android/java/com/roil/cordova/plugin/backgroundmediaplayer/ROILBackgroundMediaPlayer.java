package com.roil.cordova.plugin.backgroundmediaplayer;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class ROILBackgroundMediaPlayer extends CordovaPlugin {
    static final String CUSTOM_ACTION_SET_SESSION_METADATA_ACTION_NAME = "setSessionMetadata";
    static final String CUSTOM_ACTION_SET_SESSION_METADATA_ICON_PARAM_NAME = "icon";
    static final String CUSTOM_ACTION_SET_SESSION_METADATA_TITLE_PARAM_NAME = "title";
    static final String CUSTOM_ACTION_SET_PLAYBACK_SPEED_ACTION_NAME = "setPlaybackSpeed";
    static final String CUSTOM_ACTION_SET_PLAYBACK_SPEED_PARAM_NAME = "playbackSpeed";

    private static final int MICROSECONDS_PER_SECOND = 1000;
    private static final int PLAY_ACTION_PLAYBACK_SPEED_INDEX = 1;
    private static final int PLAY_ACTION_POSITION_INDEX = 0;
    private static final int SET_MEDIA_SOURCE_ACTION_IMAGE_URL_INDEX = 2;
    private static final int SET_MEDIA_SOURCE_ACTION_TITLE_INDEX = 1;
    private static final int SET_MEDIA_SOURCE_ACTION_URL_INDEX = 0;

    private Bitmap largeIcon;
    private Context context;
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;
    private String title = "";
    private Uri uri;

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                    try {
                        mediaController = new MediaControllerCompat(context, token);
                        mediaController.getTransportControls().prepareFromUri(uri, null);
                        if (largeIcon != null && !title.isEmpty()) {
                            setSessionMetadata();
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "pause":
                this.pause(callbackContext);
                return true;
            case "play":
                Double position = args.getDouble(PLAY_ACTION_POSITION_INDEX) * MICROSECONDS_PER_SECOND;
                double playbackSpeed = args.getDouble(PLAY_ACTION_PLAYBACK_SPEED_INDEX);
                this.play(position.intValue(), playbackSpeed, callbackContext);
                return true;
            case "setMediaSource":
                String url = args.getString(SET_MEDIA_SOURCE_ACTION_URL_INDEX);
                String title = args.getString(SET_MEDIA_SOURCE_ACTION_TITLE_INDEX);
                String imageUrl = args.getString(SET_MEDIA_SOURCE_ACTION_IMAGE_URL_INDEX);
                this.setMediaSource(Uri.parse(url), title, imageUrl, callbackContext);
                return true;
        }

        return false;
    }

    @Override
    protected void pluginInitialize() {
        context = cordova.getContext();
        mediaBrowser = new MediaBrowserCompat(
            context,
            new ComponentName(context, ROILBackgroundMediaPlaybackService.class),
            connectionCallbacks,
            null
        );
        mediaBrowser.connect();
    }

    private void pause(CallbackContext callbackContext) {
        PlaybackStateCompat playbackState = mediaController.getPlaybackState();
        float position = playbackState.getPosition() / 1000.0f;
        String state = playbackState.getState() == PlaybackStateCompat.STATE_PLAYING ? "playing" : "paused";
        mediaController.getTransportControls().stop();

        JSONObject result = new JSONObject();
        try {
            result.put("position", position);
            result.put("state", state);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
    }

    private void play(int position, Double playbackSpeed, CallbackContext callbackContext) {
        mediaController.getTransportControls().seekTo(position);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                (playbackSpeed < 1.0f || playbackSpeed > 1.0f)) {
            Bundle setPlaybackSpeedParams = new Bundle();
            setPlaybackSpeedParams.putFloat(CUSTOM_ACTION_SET_PLAYBACK_SPEED_PARAM_NAME, playbackSpeed.floatValue());
            mediaController.getTransportControls().sendCustomAction(CUSTOM_ACTION_SET_PLAYBACK_SPEED_ACTION_NAME, setPlaybackSpeedParams);
        } else {
            mediaController.getTransportControls().play();
        }

        callbackContext.success();
    }

    private void setMediaSource(Uri uri, String title, String imageUrl, CallbackContext callbackContext) {
        this.title = title;
        this.uri = uri;

        new Thread(() -> {
            try {
                largeIcon = BitmapFactory.decodeStream(new URL(imageUrl).openConnection().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (mediaBrowser.isConnected()) {
                setSessionMetadata();
            }
        }).start();

        if (mediaBrowser.isConnected()) {
            mediaController.getTransportControls().prepareFromUri(uri, null);
        }
        callbackContext.success();
    }

    private void setSessionMetadata() {
        Bundle setMetadataParams = new Bundle();
        setMetadataParams.putString(CUSTOM_ACTION_SET_SESSION_METADATA_TITLE_PARAM_NAME, title);
        setMetadataParams.putParcelable(CUSTOM_ACTION_SET_SESSION_METADATA_ICON_PARAM_NAME, largeIcon);
        mediaController.getTransportControls().sendCustomAction(CUSTOM_ACTION_SET_SESSION_METADATA_ACTION_NAME, setMetadataParams);
    }
}
