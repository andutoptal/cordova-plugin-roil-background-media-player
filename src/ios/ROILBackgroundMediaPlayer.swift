import Foundation
import AVFoundation
import MediaPlayer
import WebKit

@objc(ROILBackgroundMediaPlayer) open class ROILBackgroundMediaPlayer : CDVPlugin {
    var avSession = AVAudioSession.sharedInstance()
    var callbackId: String = ""
    var imageUrl: String = ""
    var position: Double = 0.0
    var playbackSpeed: Float = 1.0
    var player: AVPlayer?
    var playerContext = 0
    var playerItemContext = 0
    var readyToPlay = false
    var title: String = ""
    var shouldRestartPlay = false
    var shouldStartPlay = false

    open override func pluginInitialize() {
        avSession = AVAudioSession.sharedInstance()
    }

    @objc(setMediaSource:)
    func setMediaSource(_ command: CDVInvokedUrlCommand) -> Void {
        guard let url = URL(string: command.argument(at: 0) as! String) else {
            sendErrorResult()
            return
        }

        title = command.argument(at: 1, withDefault: title) as! String
        imageUrl = command.argument(at: 2, withDefault: imageUrl) as! String
        
        player = AVPlayer(url: url)
        player?.currentItem?.addObserver(self, forKeyPath: #keyPath(AVPlayerItem.status), options: [.old, .new], context: &playerItemContext)

        self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok), callbackId: command.callbackId)
    }

    @objc(play:)
    func play(command: CDVInvokedUrlCommand) -> Void {
        position = command.argument(at: 0, withDefault: 0.0) as! Double
        playbackSpeed = command.argument(at: 1, withDefault: 1.0) as! Float

        callbackId = command.callbackId
        if readyToPlay {
            startPlay()
        } else {
            shouldStartPlay = true
        }
    }

    @objc(pause:)
    func pause(_ command: CDVInvokedUrlCommand) -> Void {
        let currentTime = player?.currentTime().seconds ?? 0
        player?.rate = 0.0
        player = nil
        
        let result = [
            "position": currentTime,
            "state": "playing"
            ] as [String : Any];
        commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok, messageAs: result), callbackId: command.callbackId)
    }

    open override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        guard context == &playerItemContext || context == &playerContext else {
            super.observeValue(forKeyPath: keyPath, of: object, change: change, context: context)
            return
        }

        if keyPath == #keyPath(AVPlayerItem.status) {
            let status: AVPlayerItem.Status
            if let statusNumber = change?[.newKey] as? NSNumber {
                status = AVPlayerItem.Status(rawValue: statusNumber.intValue)!
            } else {
                status = .unknown
            }

            switch status {
            case .readyToPlay:
                self.readyToPlay = true
                self.player?.currentItem?.removeObserver(self, forKeyPath: #keyPath(AVPlayerItem.status))

                if self.shouldStartPlay {
                    self.startPlay()
                }
            case .failed:
                self.sendErrorResult()
            case .unknown:
                self.sendErrorResult()
            }
        }
    }

    func startPlay() {
        shouldStartPlay = false

        player?.seek(to: CMTime(seconds: position, preferredTimescale: 1), completionHandler: {_ in
            self.clearNowPlaying()
            self.configureAudioSessionAndStartPlay()
        })
    }
    
    func configureAudioSessionAndStartPlay() {
        do {
            try self.avSession.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            try self.avSession.setActive(true)

            if self.playbackSpeed == 1.0 {
                self.player?.play()
            } else {
                self.player?.rate = self.playbackSpeed
            }
            
            self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.ok), callbackId: self.callbackId)
            self.callbackId = ""
        } catch {
            print("Failed to activate audio session, error: \(error)")
        }
    }
    
    func sendErrorResult() {
        self.commandDelegate.send(CDVPluginResult(status: CDVCommandStatus.error), callbackId: self.callbackId)
        self.callbackId = ""
    }
    
    func clearNowPlaying() {
        MPNowPlayingInfoCenter.default().nowPlayingInfo = [:]
    }
}
