import * as cordova from 'cordova';

const SERVICE_NAME = 'ROILBackgroundMediaPlayer';

class ROILBackgroundMediaPlayer {
  pause() {
    return new Promise<number>((resolve, reject) => {
      cordova.exec(
        (currentTime) => resolve(currentTime),
        () => reject(),
        SERVICE_NAME,
        'pause',
        []
      );
    });
  }

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

  setMediaSource(src: string) {
    return new Promise<void>((resolve, reject) => {
      cordova.exec(
        () => resolve(),
        () => reject(),
        SERVICE_NAME,
        'setMediaSource',
        [src]
      );
    });
  }
}

const instance = new ROILBackgroundMediaPlayer();
export { instance as ROILBackgroundMediaPlayer };
