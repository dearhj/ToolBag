package com.android.toolbag.item

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.android.toolbag.R
import java.math.BigDecimal
import java.math.RoundingMode
import android.widget.ImageView
import android.widget.TextView
import com.android.toolbag.getOptimalSize
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round


class GravityVertical : AppCompatActivity(), SensorEventListener {
    private var textureView: TextureView? = null
    private var mCameraManager: CameraManager? = null
    private var previewSizes: Size? = null
    private var cameraDevice: CameraDevice? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var backHomeButton: ImageButton? = null

    private var mAccelerometerSensor: Sensor? = null
    private var mSensorManager: SensorManager? = null

    private var imageView: ImageView? = null
    private var textView: TextView? = null

    private var screenWidth = 0
    private var screenHeight = 0

    private val alpha: Float = 0.8f // 低通滤波器的系数
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gravity_vertical)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        val defaultDisplay =
            (getSystemService("window") as WindowManager).defaultDisplay
        val displayMetrics = DisplayMetrics()
        defaultDisplay.getRealMetrics(displayMetrics)
        screenWidth =
            max(displayMetrics.widthPixels.toDouble(), displayMetrics.heightPixels.toDouble())
                .toInt()
        screenHeight =
            min(displayMetrics.widthPixels.toDouble(), displayMetrics.heightPixels.toDouble())
                .toInt()
        println("屏幕宽高比是？   ${screenWidth / screenHeight.toFloat()}    $screenWidth    $screenHeight")

        textureView = findViewById(R.id.preview_surface)
        mCameraManager = getSystemService(CameraManager::class.java)

        if (mCameraManager?.cameraIdList?.size == 0) finish()
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

        backHomeButton = findViewById(R.id.btn_home)
        backHomeButton?.setOnClickListener { finish() }

        imageView = findViewById(R.id.plumb_point_img)
        textView = findViewById(R.id.vertical_degree)
    }

    override fun onResume() {
        super.onResume()
        mSensorManager = getSystemService("sensor") as SensorManager
        mAccelerometerSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) //加速度传感器
        mSensorManager?.registerListener(
            this,
            mAccelerometerSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        openCamera()
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
        closeCamera()
    }

    private fun chooseBestPreViewSize() {
        try {
            if (mCameraManager == null) return
            //获取摄像头属性描述
            val cameraCharacteristics =
                mCameraManager!!.getCameraCharacteristics("0") //后置摄像头
            //获取摄像头支持的配置属性
            val map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val list =
                map!!.getOutputSizes(SurfaceTexture::class.java)
            previewSizes = getOptimalSize(list, screenWidth, screenHeight)
            if (previewSizes == null) previewSizes = Size(1920, 1200)
            println("预览尺寸大小是    ${previewSizes!!.width}    ${previewSizes!!.height}")
            val layoutParams = textureView!!.layoutParams
            layoutParams.height = previewSizes!!.height
            layoutParams.width = previewSizes!!.width
            textureView!!.layoutParams = layoutParams
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

    @SuppressLint("DefaultLocale")
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0] // X轴加速度
            val y = event.values[1] // Y轴加速度
            val z = event.values[2] // Z轴加速度

            // 应用低通滤波器
            val filteredX = alpha * lastX + (1 - alpha) * x
            val filteredY = alpha * lastY + (1 - alpha) * y
            val filteredZ = alpha * lastZ + (1 - alpha) * z

            lastX = filteredX
            lastY = filteredY
            lastZ = filteredZ

            val roll = Math.toDegrees(atan2(lastY.toDouble(), lastX.toDouble())).toFloat()
            val bigDecimal = BigDecimal(roll.toString())
            val roundedValue = bigDecimal.setScale(1, RoundingMode.HALF_UP).toFloat()
            updateUI(roundedValue)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(angle: Float) {
        imageView!!.pivotX = imageView!!.width.toFloat() / 2
        imageView!!.pivotY = 0.0f
        imageView!!.rotation = -angle
        textView?.text = round(abs(angle)).toInt().toString() + "°"
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //加速度传感器无须校准
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            if (mCameraManager == null) return
            mCameraManager!!.openCamera(
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