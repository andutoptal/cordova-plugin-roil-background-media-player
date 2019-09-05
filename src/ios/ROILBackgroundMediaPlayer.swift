import Foundation
import AVFoundation

@objc(ROILBackgroundMediaPlayer) open class ROILBackgroundMediaPlayer : CDVPlugin {
    var avSession: AVAudioSession?
    var callbackId: String?
    var player: AVPlayer?
    var playerItemContext = 0
    var source: String?

    open override func pluginInitialize() {
        avSession = AVAudioSession.sharedInstance()
        do {
            try avSession?.setCategory(AVAudioSessionCategoryPlayback, with: AVAudioSessionCategoryOptions.mixWithOthers)
            try avSession?.setActive(true)
        }
        catch {
            print("Oops")
        }
    }

    @objc(setMediaSource:)
    func setMediaSource(_ command: CDVInvokedUrlCommand) -> Void {
        source = command.argument(at: 0) as? String
        player = AVPlayer(url: URL(string: source!)!)
        player?.volume = 0.0
        player?.rate = 0.01
        player?.play()
        player?.currentItem?.addObserver(self, forKeyPath: #keyPath(AVPlayerItem.status), options: [.old, .new], context: &playerItemContext)
        callbackId = command.callbackId
    }

    @objc(play:)
    func play(command: CDVInvokedUrlCommand) -> Void {
        let position = command.argument(at: 0) as! Double
        player?.seek(to: CMTime(seconds: position, preferredTimescale: 1), completionHandler: {_ in
            self.player?.rate = 1.0
            self.player?.volume = 1.0
        })
    }

    @objc(pause:)
    func pause(_ command: CDVInvokedUrlCommand) -> Void {
        let currentTime = player?.currentTime().seconds
        player?.volume = 0.0
        player?.rate = 0.01
        commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok, messageAs: currentTime ?? 0), callbackId: command.callbackId)
    }

    open override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        guard context == &playerItemContext else {
            super.observeValue(forKeyPath: keyPath, of: object, change: change, context: context)
            return
        }

        if keyPath == #keyPath(AVPlayerItem.status) {
            let status: AVPlayerItemStatus
            if let statusNumber = change?[.newKey] as? NSNumber {
                status = AVPlayerItemStatus(rawValue: statusNumber.intValue)!
            } else {
                status = .unknown
            }

            switch status {
            case .readyToPlay:
                if callbackId != "" {
                    self.player?.currentItem?.removeObserver(self, forKeyPath: #keyPath(AVPlayerItem.status))
                    self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok), callbackId: self.callbackId)
                    self.callbackId = ""
                }
            case .failed:
                self.sendErrorResult()
            case .unknown:
                self.sendErrorResult()
            }
        }
    }

    func sendErrorResult() {
        self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.error), callbackId: self.callbackId)
        self.callbackId = ""
    }
}
