"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var cordova = require("cordova");
var SERVICE_NAME = 'ROILBackgroundMediaPlayer';
var PlayerState;
(function (PlayerState) {
    PlayerState["PAUSED"] = "paused";
    PlayerState["PLAYING"] = "playing";
})(PlayerState || (PlayerState = {}));
var ROILBackgroundMediaPlayer = /** @class */ (function () {
    function ROILBackgroundMediaPlayer() {
    }
    /**
     * Pause the background media player
     *
     * @returns {Promise<number>} Returns a promise that resolves with the current time of the background player after it
     * pauses
     */
    ROILBackgroundMediaPlayer.prototype.pause = function () {
        return new Promise(function (resolve, reject) {
            cordova.exec(function (positionAndState) { return resolve(positionAndState); }, function () { return reject(); }, SERVICE_NAME, 'pause', []);
        });
    };
    /**
     * Play the background media player
     *
     * @param {number} currentTime Current time for the foreground player to use to synchronize with the background player
     * @param {number} [playbackSpeed=1.0] Playback speed for the foreground player to use to synchronize with the
     * background player
     *
     * @returns {Promise<void>} Returns a promise that resolves when the background player starts
     */
    ROILBackgroundMediaPlayer.prototype.play = function (currentTime, playbackSpeed) {
        if (playbackSpeed === void 0) { playbackSpeed = 1.0; }
        return new Promise(function (resolve, reject) {
            cordova.exec(function () { return resolve(); }, function () { return reject(); }, SERVICE_NAME, 'play', [currentTime, playbackSpeed]);
        });
    };
    /**
     * Pass information about the media to the background player, needs to be called before calling play or pause to init
     * the player
     *
     * @param {string} src Source for the media to be played
     * @param {string} [title=null] Optional title to be displayed on the lockscreen
     * @param {string} [imageUrl=null] Optional imageUrl for an image to be displayed on the lockscreen
     *
     * @returns {Promise<void>} Returns a promise that resolves after background player is initialized
     */
    ROILBackgroundMediaPlayer.prototype.setMediaSource = function (src, title, imageUrl) {
        if (title === void 0) { title = null; }
        if (imageUrl === void 0) { imageUrl = null; }
        return new Promise(function (resolve, reject) {
            cordova.exec(function () { return resolve(); }, function () { return reject(); }, SERVICE_NAME, 'setMediaSource', [src, title, imageUrl]);
        });
    };
    return ROILBackgroundMediaPlayer;
}());
var instance = new ROILBackgroundMediaPlayer();
exports.ROILBackgroundMediaPlayer = instance;
//# sourceMappingURL=index.js.map