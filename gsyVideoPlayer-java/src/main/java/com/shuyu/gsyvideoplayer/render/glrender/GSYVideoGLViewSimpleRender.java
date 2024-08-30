package com.shuyu.gsyvideoplayer.render.glrender;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import com.shuyu.gsyvideoplayer.render.view.GSYVideoGLView;
import com.shuyu.gsyvideoplayer.render.effect.NoEffect;
import com.shuyu.gsyvideoplayer.listener.GSYVideoShotListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


/**
 * 在videffects的基础上调整的
 * <p>
 * 原 @author sheraz.khilji
 */
@SuppressLint("ViewConstructor")
public class GSYVideoGLViewSimpleRender extends GSYVideoGLViewBaseRender {

    private static final int FLOAT_SIZE_BYTES = 4;

    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;

    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;

    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

    protected static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0.0f,
            0.0f, 0.0f, 1.0f,
            -1.0f, 0.0f, 1.0f,
            0.0f, -1.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f,
            1.0f, 1.0f,};

    private final String mVertexShader = "uniform mat4 uMVPMatrix;\n"
            + "uniform mat4 uSTMatrix;\n"
            + "attribute vec4 aPosition;\n"
            + "attribute vec4 aTextureCoord;\n"
            + "varying vec2 vTextureCoord;\n"
            + "void main() {\n"
            + "  gl_Position = uMVPMatrix * aPosition;\n"
            + "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n"
            + "}\n";

    private int mProgram;

    private int mTextureID[] = new int[2];

    private int muMVPMatrixHandle;

    private int muSTMatrixHandle;

    private int maPositionHandle;

    private int maTextureHandle;

    private boolean mUpdateSurface = false;

    private boolean mTakeShotPic = false;

    private FloatBuffer mTriangleVertices;

    private SurfaceTexture mSurface;

    private GSYVideoShotListener mGSYVideoShotListener;

    private GSYVideoGLView.ShaderInterface mEffect = new NoEffect();

    public GSYVideoGLViewSimpleRender() {
        mTriangleVertices = ByteBuffer
                .allocateDirect(
                        mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        Matrix.setIdentityM(mSTMatrix, 0);
        Matrix.setIdentityM(mMVPMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        synchronized (this) {
            if (mUpdateSurface) {
                // 从视频解码器获取新的帧数据并将其绑定到纹理上
                mSurface.updateTexImage();
                // 获取一个 4x4 矩阵，用于处理纹理坐标的变换。mSTMatrix 是一个 float 数组，用于存储这个矩阵。这个矩阵可以用于在渲染时对纹理进行旋转、缩放或其他变换操作。
                mSurface.getTransformMatrix(mSTMatrix);
                mUpdateSurface = false;
            }
        }

        // 可能用于初始化绘制帧所需的一些基本设置，比如清空屏幕颜色，设置视口等
        initDrawFrame();

        // 绑定要绘制的纹理
        bindDrawFrameTexture();

        // 置顶点指针和绘制图元
        initPointerAndDraw();

        // 捕获当前帧的内容并生成一个 Bitmap。它通常会在需要截图或对当前渲染内容进行后续处理时使用
        takeBitmap(glUnused);

        // 确保所有的 OpenGL 命令在这个点之前都已经完成执行。它通常用于确保渲染操作完成，尤其在需要捕获当前帧或进行某些依赖于渲染完成的操作时
        GLES20.glFinish();

    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

        mProgram = createProgram(getVertexShader(), getFragmentShader());
        if (mProgram == 0) {
            return;
        }
        maPositionHandle = GLES20
                .glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException(
                    "Could not get attrib location for aPosition");
        }
        maTextureHandle = GLES20.glGetAttribLocation(mProgram,
                "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException(
                    "Could not get attrib location for aTextureCoord");
        }

        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram,
                "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException(
                    "Could not get attrib location for uMVPMatrix");
        }

        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram,
                "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException(
                    "Could not get attrib location for uSTMatrix");
        }

        GLES20.glGenTextures(2, mTextureID, 0);

        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID[0]);
        checkGlError("glBindTexture mTextureID");

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        mSurface = new SurfaceTexture(mTextureID[0]);
        mSurface.setOnFrameAvailableListener(this);

        Surface surface = new Surface(mSurface);

        // ！！！ 将解码后的图像传送至播放器界面渲染
        Log.i("RENDER", "有新的surface需要绘制到界面中");
        sendSurfaceForPlayer(surface);
    }

    @Override
    synchronized public void onFrameAvailable(SurfaceTexture surface) {
        // 标识有新的帧需要复制
        mUpdateSurface = true;
    }

    @Override
    public void releaseAll() {

    }

    /**
     * 设置滤镜效果
     *
     * @param shaderEffect
     */
    @Override
    public void setEffect(GSYVideoGLView.ShaderInterface shaderEffect) {
        if (shaderEffect != null) {
            mEffect = shaderEffect;
        }
        mChangeProgram = true;
        mChangeProgramSupportError = true;
    }

    @Override
    public GSYVideoGLView.ShaderInterface getEffect() {
        return mEffect;
    }

    protected void initDrawFrame() {
        if (mChangeProgram) {
            mProgram = createProgram(getVertexShader(), getFragmentShader());
            mChangeProgram = false;
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT
                | GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");
    }


    protected void bindDrawFrameTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID[0]);
    }


    protected void takeBitmap(GL10 glUnused) {
        if (mTakeShotPic) {
            mTakeShotPic = false;
            if (mGSYVideoShotListener != null) {
                Bitmap bitmap = createBitmapFromGLSurface(0, 0, mSurfaceView.getWidth(), mSurfaceView.getHeight(), glUnused);
                mGSYVideoShotListener.getBitmap(bitmap);
            }
        }
    }


    protected void initPointerAndDraw() {
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT,
                false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(maTextureHandle, 3, GLES20.GL_FLOAT,
                false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandle");
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix,
                0);
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

        Log.i("RENDER", "开始绘制新surface");
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");

    }

    public int getProgram() {
        return mProgram;
    }

    public int getMuMVPMatrixHandle() {
        return muMVPMatrixHandle;
    }

    public int getMuSTMatrixHandle() {
        return muSTMatrixHandle;
    }

    public int getMaPositionHandle() {
        return maPositionHandle;
    }

    public int getMaTextureHandle() {
        return maTextureHandle;
    }

    public float[] getSTMatrix() {
        return mSTMatrix;
    }

    public int[] getTextureID() {
        return mTextureID;
    }

    protected String getVertexShader() {
        return mVertexShader;
    }

    protected String getFragmentShader() {
        return mEffect.getShader(mSurfaceView);
    }

    /**
     * 打开截图
     */
    public void takeShotPic() {
        mTakeShotPic = true;
    }

    /**
     * 截图监听
     */
    public void setGSYVideoShotListener(GSYVideoShotListener listener, boolean high) {
        this.mGSYVideoShotListener = listener;
        this.mHighShot = high;
    }
}


