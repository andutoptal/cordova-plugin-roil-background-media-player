import * as cordova from 'cordova';

const SERVICE_NAME = 'ROILBackgroundMediaPlayer';

enum PlayerState {
  PAUSED = 'paused',
  PLAYING = 'playing'
}

interface PlayerPositionAndState {
  position: number;
  state: PlayerState;
}

class ROILBackgroundMediaPlayer {
  /**
   * Pause the background media player
   *
   * @returns {Promise<number>} Returns a promise that resolves with the current time of the background player after it
   * pauses
   */
  pause() {
    return new Promise<PlayerPositionAndState>((resolve, reject) => {
      cordova.exec(
        (positionAndState) => resolve(positionAndState),
        () => reject(),
        SERVICE_NAME,
        'pause',
        []
      );
    });
  }

  /**
   * Play the background media player
   *
   * @param {number} currentTime Current time for the foreground player to use to synchronize with the background player
   * @param {number} [playbackSpeed=1.0] Playback speed for the foreground player to use to synchronize with the
   * background player
   *
   * @returns {Promise<void>} Returns a promise that resolves when the background player starts
   */
  play(currentTime: number, playbackSpeed = 1.0) {
    return new Promise<void>((resolve, reject) => {
      cordova.exec(
        () => resolve(),
        () => reject(),
        SERVICE_NAME,
        'play',
        [currentTime, playbackSpeed]
      );
    });
  }

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
  setMediaSource(src: string, title = null, imageUrl = null) {
    return new Promise<void>((resolve, reject) => {
      cordova.exec(
        () => resolve(),
        () => reject(),
        SERVICE_NAME,
        'setMediaSource',
        [src, title, imageUrl]
      );
    });
  }
}

const instance = new ROILBackgroundMediaPlayer();
export { instance as ROILBackgroundMediaPlayer };
