package com.roil.cordova.plugin.backgroundmediaplayer;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

public class ROILBackgroundMediaPlayer extends CordovaPlugin {
    final private static int PLAY_ACTION_PLAYBACK_SPEED_INDEX = 1;
    final private static int PLAY_ACTION_POSITION_INDEX = 0;
    final private static int SET_MEDIA_SOURCE_ACTION_URL_INDEX = 0;
    final private static int MICROSECONDS_PER_SECOND = 1000;

    final static String CUSTOM_ACTION_SET_PLAYBACK_SPEED_ACTION_NAME = "setPlaybackSpeed";
    final static String CUSTOM_ACTION_SET_PLAYBACK_SPEED_PARAM_NAME = "playbackSpeed";

    private Context context;
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                    try {
                        mediaController = new MediaControllerCompat(context, token);
                        mediaController.getTransportControls().prepareFromUri(uri, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            };
    private Uri uri;

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
                this.setMediaSource(Uri.parse(url), callbackContext);
                return true;
        }

        return false;
    }

    @Override
    protected void pluginInitialize() {
        context = cordova.getContext();
        mediaBrowser = new MediaBrowserCompat(context,
                new ComponentName(context, ROILBackgroundMediaPlaybackService.class),
                connectionCallbacks,
                null);
        mediaBrowser.connect();
    }

    private void pause(CallbackContext callbackContext) {
        mediaController.getTransportControls().pause();
        float position = mediaController.getPlaybackState().getPosition() / 1000.0f;
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, position));
    }

    private void play(int position, Double playbackSpeed, CallbackContext callbackContext) {
        mediaController.getTransportControls().seekTo(position);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
                (playbackSpeed < 1.0f || playbackSpeed > 1.0f)) {
            Bundle customCommandParams = new Bundle();
            customCommandParams.putFloat(CUSTOM_ACTION_SET_PLAYBACK_SPEED_PARAM_NAME, playbackSpeed.floatValue());
            mediaController.getTransportControls().sendCustomAction(CUSTOM_ACTION_SET_PLAYBACK_SPEED_ACTION_NAME, customCommandParams);
        } else {
            mediaController.getTransportControls().play();
        }
        callbackContext.success();
    }

    private void setMediaSource(Uri uri, CallbackContext callbackContext) {
        this.uri = uri;
        if (mediaBrowser.isConnected()) {
            mediaController.getTransportControls().prepareFromUri(uri, null);
        }
        callbackContext.success();
    }
}
