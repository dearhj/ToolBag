package com.android.toolbag.item

import android.annotation.SuppressLint
import android.graphics.Rect
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
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.toolbag.R
import com.android.toolbag.widget.AlignmentView
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class PaintingActivity : AppCompatActivity(), SensorEventListener {
    private var surfaceView: SurfaceView? = null
    private var mCameraManager: CameraManager? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var previewSizes: Size? = null
    private var handler: Handler? = null
    private var handlerThread: HandlerThread? = null
    private var cameraDevice: CameraDevice? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var backHomeButton: ImageButton? = null

    private var mAccelerometerSensor: Sensor? = null
    private var mSensorManager: SensorManager? = null

    private var rect: Rect? = null
    private var aspectRatioScreen = 0f
    private var textViewV: TextView? = null
    private var textViewH: TextView? = null
    private var alignmentView: AlignmentView? = null

    private val alpha: Float = 0.8f // 低通滤波器的系数
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_painting)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        val screenWidth = max(rect!!.width(), rect!!.height())
        val screenHeight = min(rect!!.width(), rect!!.height())
        aspectRatioScreen = screenWidth.toFloat() / screenHeight.toFloat()
        println("屏幕宽高比？   $aspectRatioScreen    ${rect!!.width()}    ${rect!!.height()}")

        surfaceView = findViewById(R.id.camera_layout)
        mCameraManager = getSystemService(CameraManager::class.java)

        handlerThread = HandlerThread("CameraPreview")
        handlerThread?.start()
        handler = Handler(handlerThread!!.looper)
        if (mCameraManager?.cameraIdList?.size == 0) finish()

        backHomeButton = findViewById(R.id.btn_home)
        backHomeButton?.setOnClickListener { finish() }

        textViewV = findViewById(R.id.vertical_degree)
        textViewH = findViewById(R.id.horizontal_degree)
        alignmentView = findViewById(R.id.alignmentView)

        try {
            surfaceHolder = surfaceView?.holder
            surfaceHolder?.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    openCamera()
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int
                ) {
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    //关闭相机释放资源
                    closeCamera()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
    }

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

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(angle: Float) {
        alignmentView?.setDegree(-angle)
        textViewH?.text = round(abs(angle)).toInt().toString() + "°"
        textViewV?.text = (90 - round(abs(angle)).toInt()).toString() + "°"
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            //获取摄像头属性描述
            val cameraCharacteristics =
                mCameraManager!!.getCameraCharacteristics("0") //后置摄像头
            //获取摄像头支持的配置属性
            val map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val list =
                map!!.getOutputSizes(SurfaceTexture::class.java)!!.sortedByDescending { it.width }
            for (size in list) {
                val aspectRatioCamera = size.width.toFloat() / size.height.toFloat()
                if (aspectRatioScreen >= 1 && aspectRatioScreen < 1.5) {
                    if (aspectRatioCamera >= 1 && aspectRatioCamera < 1.5) {
                        previewSizes = size
                        break
                    }
                } else if (aspectRatioScreen >= 1.5 && aspectRatioScreen < 2) {
                    if (aspectRatioCamera >= 1.5 && aspectRatioCamera < 2) {
                        previewSizes = size
                        break
                    }
                } else if (aspectRatioScreen >= 2) {
                    if (aspectRatioCamera >= 2) {
                        previewSizes = size
                        break
                    }
                }
            }
            println("这里的预览尺寸大小是    ${previewSizes!!.width}    ${previewSizes!!.height}")

            //打开摄像头
            mCameraManager?.openCamera(
                "0",
                stateCallback,
                handler
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 关闭相机
     */
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

    /**
     * 打开相机的回调，
     */
    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            //打开相机后开启预览
            cameraDevice = camera
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            closeCamera()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            closeCamera()
        }
    }

    private fun startPreview() {
        try {
            //构建预览请求
            previewBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            //设置预览输出的界面
            val fpsRange = Range(25, 30)
            previewBuilder?.set<Range<Int>>(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
            //设置自动对焦
            previewBuilder?.set(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            previewBuilder?.addTarget(surfaceHolder!!.surface)

            //创建相机的会话Session
            cameraDevice!!.createCaptureSession(
                listOf(surfaceHolder!!.surface), sessionStateCallback, handler
            )
        } catch (e: Exception) {
            e.printStackTrace()
            closeCamera()
        }
    }

    /**
     * session的回调
     */
    private val sessionStateCallback: CameraCaptureSession.StateCallback =
        object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                //会话已经建立，可以开始预览了
                cameraCaptureSession = session
                repeatPreview()
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                //关闭会话
                session.close()
                closeCamera()
            }
        }

    private fun repeatPreview() {
        if (cameraCaptureSession != null) {
            previewBuilder?.setTag("TAG_PREVIEW")
            try {
                cameraCaptureSession?.setRepeatingRequest(
                    previewBuilder!!.build(),
                    null,
                    handler
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}