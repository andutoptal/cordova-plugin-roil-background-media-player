"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var cordova = require("cordova");
var SERVICE_NAME = 'ROILBackgroundMediaPlayer';
var ROILBackgroundMediaPlayer = /** @class */ (function () {
    function ROILBackgroundMediaPlayer() {
    }
    ROILBackgroundMediaPlayer.prototype.pause = function () {
        return new Promise(function (resolve, reject) {
            cordova.exec(function (currentTime) { return resolve(currentTime); }, function () { return reject(); }, SERVICE_NAME, 'pause', []);
        });
    };
    ROILBackgroundMediaPlayer.prototype.play = function (currentTime) {
        return new Promise(function (resolve, reject) {
            cordova.exec(function () { return resolve(); }, function () { return reject(); }, SERVICE_NAME, 'play', [currentTime]);
        });
    };
    ROILBackgroundMediaPlayer.prototype.setMediaSource = function (src) {
        return new Promise(function (resolve, reject) {
            cordova.exec(function () { return resolve(); }, function () { return reject(); }, SERVICE_NAME, 'setMediaSource', [src]);
        });
    };
    return ROILBackgroundMediaPlayer;
}());
var instance = new ROILBackgroundMediaPlayer();
exports.ROILBackgroundMediaPlayer = instance;
//# sourceMappingURL=index.js.map