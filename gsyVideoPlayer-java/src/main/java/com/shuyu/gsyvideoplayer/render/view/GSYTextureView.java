package com.shuyu.gsyvideoplayer.render.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.shuyu.gsyvideoplayer.listener.GSYVideoShotListener;
import com.shuyu.gsyvideoplayer.listener.GSYVideoShotSaveListener;
import com.shuyu.gsyvideoplayer.render.GSYRenderView;
import com.shuyu.gsyvideoplayer.render.glrender.GSYVideoGLViewBaseRender;
import com.shuyu.gsyvideoplayer.render.view.listener.IGSYSurfaceListener;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.FileUtils;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.MeasureHelper;

import java.io.File;

/**
 * 用于显示video的，做了横屏与竖屏的匹配，还有需要rotation需求的
 * Created by shuyu on 2016/11/11.
 * <p>
 *
 * 通过一个具体的例子来解释 `SurfaceTexture` 和 `Surface` 的区别和关系。
 *
 * ### 场景：视频播放器在 `TextureView` 中播放视频
 *
 * 假设我们正在开发一个简单的视频播放器应用，这个应用使用 `TextureView` 来播放视频。
 *
 * #### 1. **创建 `TextureView`**
 *
 * 首先，我们在布局文件中定义了一个 `TextureView`，它将作为视频显示的区域。
 *
 * ```xml
 * <com.example.GSYTextureView
 *     android:id="@+id/texture_view"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"/>
 * ```
 *
 * #### 2. **获取 `SurfaceTexture` 和 `Surface`**
 *
 * 在代码中，我们会获取 `TextureView` 的 `SurfaceTexture`，然后用这个 `SurfaceTexture` 创建一个 `Surface`，让视频解码器可以将解码后的图像渲染到这个 `Surface` 上。
 *
 * ```java
 * GSYTextureView textureView = findViewById(R.id.texture_view);
 *
 * // 监听 SurfaceTexture 的可用性
 * textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
 *     @Override
 *     public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
 *         // 创建 Surface 对象
 *         Surface surface = new Surface(surfaceTexture);
 *
 *         // 假设这里有一个视频播放器对象，用于控制视频播放
 *         videoPlayer.setSurface(surface);
 *         videoPlayer.play();
 *     }
 *
 *     @Override
 *     public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
 *         // 调整 Surface 的大小
 *     }
 *
 *     @Override
 *     public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
 *         // 当 SurfaceTexture 被销毁时释放资源
 *         return true;
 *     }
 *
 *     @Override
 *     public void onSurfaceTextureUpdated(SurfaceTexture surface) {
 *         // 每次 SurfaceTexture 更新时触发（例如新帧到达）
 *     }
 * });
 * ```
 *
 * #### 3. **SurfaceTexture 的角色**
 *
 * - **接收视频帧**: `SurfaceTexture` 充当一个图像缓冲区，用于接收解码器输出的每一帧视频数据。每次有新的一帧到达时，它都会触发 `onSurfaceTextureUpdated` 回调。
 *
 * - **OpenGL 纹理映射**: `SurfaceTexture` 还可以将图像帧映射为 OpenGL ES 的纹理，以便进一步处理（例如做图像特效、滤镜等）。
 *
 * #### 4. **Surface 的角色**
 *
 * - **视频渲染目标**: `Surface` 是 `SurfaceTexture` 的消费者。解码器将解码后的视频帧渲染到 `Surface` 上，`Surface` 再通过 `SurfaceTexture` 将这些帧显示在 `TextureView` 中。
 *
 * - **显示图像**: `Surface` 负责最终将图像显示到屏幕上，这是 `SurfaceTexture` 不直接处理的部分。通过 `Surface`，解码后的图像帧最终会在 `TextureView` 中显示出来。
 *
 * #### 5. **完整的流程**
 *
 * 1. **解码器解码视频数据**: 视频解码器（例如 `MediaCodec` 或 `ijkplayer`）解码视频文件，将解码后的图像帧传递到一个 `Surface`。
 *
 * 2. **Surface 接收数据**: 这个 `Surface` 是由 `SurfaceTexture` 创建的，它负责将图像帧传递给 `SurfaceTexture`。
 *
 * 3. **SurfaceTexture 缓存帧数据**: `SurfaceTexture` 接收到图像帧后，将其存储在内部的图像缓冲区中。
 *
 * 4. **TextureView 显示图像**: `SurfaceTexture` 将这些图像帧渲染到 `TextureView` 上，最终显示在用户的屏幕上。
 *
 * 5. **回调更新 UI**: 每当 `SurfaceTexture` 更新图像帧时，会触发 `onSurfaceTextureUpdated` 回调，你可以在这个回调中执行一些额外的操作（例如更新 UI）。
 *
 * ### **总结**
 *
 * - **`SurfaceTexture`** 主要负责接收和管理解码器输出的图像帧，它与 OpenGL 纹理结合，可以用于复杂的图像处理。
 *
 * - **`Surface`** 是一个显示接口，通过它，`SurfaceTexture` 中的图像帧可以被显示在 `TextureView` 上。
 *
 * 通过这个例子，`SurfaceTexture` 和 `Surface` 的关系就像是一个生产者和消费者：`SurfaceTexture` 生成或管理图像数据，而 `Surface` 消费这些数据并将其显示在屏幕上。
 */

public class GSYTextureView extends TextureView implements TextureView.SurfaceTextureListener, IGSYRenderView, MeasureHelper.MeasureFormVideoParamsListener {

    private IGSYSurfaceListener mIGSYSurfaceListener;

    private MeasureHelper.MeasureFormVideoParamsListener mVideoParamsListener;

    private MeasureHelper measureHelper;

    private SurfaceTexture mSaveTexture;
    private Surface mSurface;

    public GSYTextureView(Context context) {
        super(context);
        init();
    }

    public GSYTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        measureHelper = new MeasureHelper(this, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureHelper.prepareMeasure(widthMeasureSpec, heightMeasureSpec, (int) getRotation());
        setMeasuredDimension(measureHelper.getMeasuredWidth(), measureHelper.getMeasuredHeight());
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//        Debuger.printfLog(getClass().getSimpleName() + "---> available");
        if (GSYVideoType.isMediaCodecTexture()) {
            if (mSaveTexture == null) {
                mSaveTexture = surface;
                mSurface = new Surface(surface);
            } else {
                setSurfaceTexture(mSaveTexture);
            }
            if (mIGSYSurfaceListener != null) {
                mIGSYSurfaceListener.onSurfaceAvailable(mSurface);
            }
        } else {
            mSurface = new Surface(surface);
            if (mIGSYSurfaceListener != null) {
                mIGSYSurfaceListener.onSurfaceAvailable(mSurface);
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//        Debuger.printfLog(getClass().getSimpleName() + "---> changed");
        if (mIGSYSurfaceListener != null) {
            mIGSYSurfaceListener.onSurfaceSizeChanged(mSurface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//        Debuger.printfLog(getClass().getSimpleName() + "---> destroyed");
        //清空释放
        if (mIGSYSurfaceListener != null) {
            mIGSYSurfaceListener.onSurfaceDestroyed(mSurface);
        }
        if (GSYVideoType.isMediaCodecTexture()) {
            return (mSaveTexture == null);
        } else {
            return true;
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//        Debuger.printfLog(getClass().getSimpleName() + "---> update");
        // 每次刷新页面会调用此方法 每秒都会根据视频帧数触发此方法对应的次数
        //如果播放的是暂停全屏了
        if (mIGSYSurfaceListener != null) {
            mIGSYSurfaceListener.onSurfaceUpdated(mSurface);
        }
    }

    @Override
    public IGSYSurfaceListener getIGSYSurfaceListener() {
        return mIGSYSurfaceListener;
    }

    @Override
    public void setIGSYSurfaceListener(IGSYSurfaceListener surfaceListener) {
        setSurfaceTextureListener(this);
        mIGSYSurfaceListener = surfaceListener;
    }

    @Override
    public int getSizeH() {
        return getHeight();
    }

    @Override
    public int getSizeW() {
        return getWidth();
    }

    /**
     * 暂停时初始化位图
     */
    @Override
    public Bitmap initCover() {
        if (getSizeW() <= 0 || getSizeH() <= 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(
            getSizeW(), getSizeH(), Bitmap.Config.RGB_565);
        return getBitmap(bitmap);

    }

    /**
     * 暂停时初始化位图
     */
    @Override
    public Bitmap initCoverHigh() {
        if (getSizeW() <= 0 || getSizeH() <= 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(
            getSizeW(), getSizeH(), Bitmap.Config.ARGB_8888);
        return getBitmap(bitmap);

    }


    /**
     * 获取截图
     *
     * @param shotHigh 是否需要高清的
     */
    @Override
    public void taskShotPic(GSYVideoShotListener gsyVideoShotListener, boolean shotHigh) {
        if (shotHigh) {
            gsyVideoShotListener.getBitmap(initCoverHigh());
        } else {
            gsyVideoShotListener.getBitmap(initCover());
        }
    }

    /**
     * 保存截图
     *
     * @param high 是否需要高清的
     */
    @Override
    public void saveFrame(final File file, final boolean high, final GSYVideoShotSaveListener gsyVideoShotSaveListener) {
        GSYVideoShotListener gsyVideoShotListener = new GSYVideoShotListener() {
            @Override
            public void getBitmap(Bitmap bitmap) {
                if (bitmap == null) {
                    gsyVideoShotSaveListener.result(false, file);
                } else {
                    FileUtils.saveBitmap(bitmap, file);
                    gsyVideoShotSaveListener.result(true, file);
                }
            }
        };
        if (high) {
            gsyVideoShotListener.getBitmap(initCoverHigh());
        } else {
            gsyVideoShotListener.getBitmap(initCover());
        }

    }


    @Override
    public View getRenderView() {
        return this;
    }

    @Override
    public void onRenderResume() {
        Debuger.printfLog(getClass().getSimpleName() + " not support onRenderResume now");
    }

    @Override
    public void onRenderPause() {
        Debuger.printfLog(getClass().getSimpleName() + " not support onRenderPause now");
    }

    @Override
    public void releaseRenderAll() {
        Debuger.printfLog(getClass().getSimpleName() + " not support releaseRenderAll now");
    }

    @Override
    public void setRenderMode(int mode) {
        Debuger.printfLog(getClass().getSimpleName() + " not support setRenderMode now");
    }

    @Override
    public void setRenderTransform(Matrix transform) {
        setTransform(transform);
    }

    @Override
    public void setGLRenderer(GSYVideoGLViewBaseRender renderer) {
        Debuger.printfLog(getClass().getSimpleName() + " not support setGLRenderer now");
    }

    @Override
    public void setGLMVPMatrix(float[] MVPMatrix) {
        Debuger.printfLog(getClass().getSimpleName() + " not support setGLMVPMatrix now");
    }

    /**
     * 设置滤镜效果
     */
    @Override
    public void setGLEffectFilter(GSYVideoGLView.ShaderInterface effectFilter) {
        Debuger.printfLog(getClass().getSimpleName() + " not support setGLEffectFilter now");
    }


    @Override
    public void setVideoParamsListener(MeasureHelper.MeasureFormVideoParamsListener listener) {
        mVideoParamsListener = listener;
    }

    @Override
    public int getCurrentVideoWidth() {
        if (mVideoParamsListener != null) {
            return mVideoParamsListener.getCurrentVideoWidth();
        }
        return 0;
    }

    @Override
    public int getCurrentVideoHeight() {
        if (mVideoParamsListener != null) {
            return mVideoParamsListener.getCurrentVideoHeight();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (mVideoParamsListener != null) {
            return mVideoParamsListener.getVideoSarNum();
        }
        return 0;
    }

    @Override
    public int getVideoSarDen() {
        if (mVideoParamsListener != null) {
            return mVideoParamsListener.getVideoSarDen();
        }
        return 0;
    }


    /**
     * 添加播放的view
     */
    public static GSYTextureView addTextureView(Context context, ViewGroup textureViewContainer, int rotate,
                                                final IGSYSurfaceListener gsySurfaceListener,
                                                final MeasureHelper.MeasureFormVideoParamsListener videoParamsListener) {
        if (textureViewContainer.getChildCount() > 0) {
            textureViewContainer.removeAllViews();
        }
        GSYTextureView gsyTextureView = new GSYTextureView(context);
        gsyTextureView.setIGSYSurfaceListener(gsySurfaceListener);
        gsyTextureView.setVideoParamsListener(videoParamsListener);
        gsyTextureView.setRotation(rotate);
        GSYRenderView.addToParent(textureViewContainer, gsyTextureView);
        return gsyTextureView;
    }
}
