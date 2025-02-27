package com.android.toolbag.item

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.android.toolbag.R
import kotlin.math.max
import kotlin.math.min

class ProtractorActivity : AppCompatActivity() {
    private var backHomeButton: ImageButton? = null
    private var textureView: TextureView? = null
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var aspectRatioScreen = 0f
    private var rect: Rect? = null
    private var previewSizes: Size? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_protractor)
        val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = uiOptions

        rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val screenWidth = max(rect!!.width(), rect!!.height())
        val screenHeight = min(rect!!.width(), rect!!.height())
        aspectRatioScreen = screenWidth.toFloat() / screenHeight.toFloat()
        println("屏幕宽高比？   $aspectRatioScreen    ${rect!!.width()}    ${rect!!.height()}")

        backHomeButton = findViewById(R.id.btn_home)
        backHomeButton?.setOnClickListener { finish() }

        textureView = findViewById(R.id.preview_surface)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        if (cameraManager?.cameraIdList?.size == 0) finish()
        chooseBestPreViewSize()

        textureView!!.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                updateTextureViewTransform()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                closeCamera()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    override fun onResume() {
        super.onResume()
        openCamera()
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
    }

    private fun chooseBestPreViewSize() {
        try {
            if (cameraManager == null) return
            //获取摄像头属性描述
            val cameraCharacteristics =
                cameraManager!!.getCameraCharacteristics("0") //后置摄像头
            //获取摄像头支持的配置属性
            val map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val list =
                map!!.getOutputSizes(SurfaceTexture::class.java)!!.sortedByDescending { it.width }
            for (size in list) {
                if (size.width > 6000) continue //分辨率过大，使用textureView预览会有卡顿现象
                val aspectRatioCamera = size.width.toFloat() / size.height.toFloat()
                if (aspectRatioScreen - 0.25 <= aspectRatioCamera && aspectRatioCamera <= aspectRatioScreen + 0.25) {
                    if (aspectRatioScreen >= aspectRatioCamera) {
                        previewSizes = size
                        break
                    }
                }
            }
            if (previewSizes == null) previewSizes = Size(1920, 1200)
            println("预览尺寸大小是    ${previewSizes!!.width}    ${previewSizes!!.height}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTextureViewTransform() {
        val matrix = Matrix()
        val centerX = textureView!!.width / 2f
        val centerY = textureView!!.height / 2f
        // 计算缩放比例
        if (previewSizes == null) return
        val scaleX = previewSizes!!.width.toFloat() / previewSizes!!.height
        val scaleY = previewSizes!!.height.toFloat() / previewSizes!!.width
        // 旋转图像
        matrix.postRotate(270f, centerX, centerY)
        matrix.postScale(scaleX, scaleY, centerX, centerY)
        // 应用变换矩阵
        textureView!!.setTransform(matrix)
    }


    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            if (cameraManager == null) return
            cameraManager!!.openCamera(
                "0",
                object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        startPreview()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        closeCamera()
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        closeCamera()
                    }
                }, null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        try {
            //关闭相机
            if (cameraDevice != null) {
                cameraDevice?.close()
                cameraDevice = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPreview() {
        try {
            if (previewSizes == null) return
            val surfaceTexture = textureView!!.surfaceTexture
            surfaceTexture!!.setDefaultBufferSize(
                previewSizes!!.width,
                previewSizes!!.height
            ) //设置预览分辨率
            val surface = Surface(surfaceTexture)
            previewBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            //设置预览输出的界面
            val fpsRange = Range(25, 30)
            previewBuilder?.set<Range<Int>>(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            //设置自动对焦
            previewBuilder?.set(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            previewBuilder!!.addTarget(surface)

            cameraDevice!!.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        previewBuilder?.setTag("TAG_PREVIEW")
                        try {
                            cameraCaptureSession?.setRepeatingRequest(
                                previewBuilder!!.build(),
                                null,
                                null
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        session.close()
                        closeCamera()
                    }
                },
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            closeCamera()
        }
    }

}