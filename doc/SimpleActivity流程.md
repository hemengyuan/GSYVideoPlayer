# 普通视频播放流程
**SimplePlayer**

1. SimplePlayer#init
2. StandardGSYVideoPlayer#startPlayLogic
3. GSYVideoControlView#prepareVideo
4. GSYVideoView#startPrepare

# 核心类理解
### GSYVideoControlView
播放UI的控制类（显示、手势操作）

### GSYVideoView
视频状态（播放、暂停） & 监听器（不同的状态主动触发回调）
```text
    实现了GSYMediaPlayerListener接口 可以在播放器状态变更时触发不同的回调函数
```

### GSYVideoBaseManager
管理视频播放器
```text
    1、实现了GSYVideoViewBridge接口(控制播放器 以及触发GSYMediaPlayerListener对应的回调函数)
    2、属性IPlayerManager为播放器内核，
    UI层的GSYVideoControlView，每当动作触发 会使用GSYVideoView，GSYVideoView调用自身的getGSYVideoManager()【实现类为GSYVideoPlayer】方法，
    获取视频管理器并对视频播放进行控制
```

### AbstractMediaPlayer
**bilibili写的抽象类**
```text
    每个播放器内核都继承这个抽象类，主要作用是触发不同的回调函数
    e.g. 当视频prepare完毕后，都需要主动调AbstractMediaPlayer#notifyOnPrepared方法，从而触发GSYVideoBaseManager中对应的回调方法(
        ijkplayer的核心bilibili做的，内部代码会遵守这个规则
        封装的第三方播放器，比如AliMediaPlayer,在代码中有主动触发notifyOnPrepared的规则，参考onPreparedListener这个属性
    )

```




