package com.android.toolbag.item

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.android.toolbag.R
import com.android.toolbag.batteryChangeLevel
import com.android.toolbag.customToast
import com.android.toolbag.getOptimalSize
import kotlin.math.max
import kotlin.math.min


class MagnifierActivity : AppCompatActivity() {
    private var textureView: TextureView? = null
    private var flashButton: ImageView? = null
    private var changeCameraButton: ImageView? = null
    private var addButton: ImageView? = null
    private var minusButton: ImageView? = null
    private var seekBar: SeekBar? = null
    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var previewSizes: Size? = null
    private var zoomRect: Rect? = null
    private var flashAvailable = false
    private var flashIsOn = false
    private var cameraFrontId = "" //前摄1
    private var cameraBackId = "" //后摄0
    private var currentCameraId = "0" //当前ID
    private var hasFrontCamera = false
    private var hasBackCamera = false
    private var maxZoom = 0f  //最大变焦倍数
    private var currentZoomLevel = 1f  //当前变焦倍数
    private var batteryManager: BatteryManager? = null


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_magnifier)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { //实现底部导航栏透明
            // 设置窗口不适应系统窗口，允许内容绘制在系统栏后面
            WindowCompat.setDecorFitsSystemWindows(window, false)
            // 禁用导航栏对比度增强（防止出现半透明遮罩）
            window.isNavigationBarContrastEnforced = false
        }

        textureView = findViewById(R.id.preview_surface)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager

        flashButton = findViewById(R.id.ic_camera_flash)
        changeCameraButton = findViewById(R.id.ic_camera_switch)
        addButton = findViewById(R.id.ic_seekbar_up)
        minusButton = findViewById(R.id.ic_seekbar_down)
        seekBar = findViewById(R.id.ic_seekbar)

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
        println("屏幕高宽比是？   ${screenWidth / screenHeight.toFloat()}    $screenWidth    $screenHeight")


        textureView = findViewById(R.id.preview_surface)
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        textureView!!.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        val supportCamera = getCameraInfo()
        if (!supportCamera) finish()
        if (!hasBackCamera) changeCameraButton?.setImageDrawable(getDrawable(R.drawable.ic_camera_picker_front_img_icon))

        chooseBestPreViewSize()

        textureView!!.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
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

        flashButton?.setOnClickListener {
            if (!flashAvailable) customToast(this, getString(R.string.not_support_flash))
            else {
                val batteryValue =
                    batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
                if (batteryValue <= 15) {
                    customToast(this, getString(R.string.battery_low))
                    return@setOnClickListener
                }
                if (!flashIsOn) {
                    flashButton?.setImageDrawable(getDrawable(R.drawable.ic_flash_img_on_icon))
                    flashIsOn = true
                    changeFlashlightStatus(true)
                } else {
                    flashButton?.setImageDrawable(getDrawable(R.drawable.ic_flash_img_close_icon))
                    flashIsOn = false
                    changeFlashlightStatus(false)
                }
            }
        }

        batteryChangeLevel {
            if (it <= 15 && flashIsOn) {
                flashButton?.setImageDrawable(getDrawable(R.drawable.ic_flash_img_close_icon))
                flashIsOn = false
                changeFlashlightStatus(false)
            }
        }

        changeCameraButton?.setOnClickListener {
            if (currentCameraId == cameraBackId) {
                if (!hasFrontCamera) customToast(this, getString(R.string.cannot_find_front))
                else {
                    currentZoomLevel = 1f
                    seekBar?.progress = 10
                    currentCameraId = cameraFrontId
                    chooseBestPreViewSize()
                    changeCameraButton?.setImageDrawable(getDrawable(R.drawable.ic_camera_picker_front_img_icon))
                    flashIsOn = false
                    flashButton?.setImageDrawable(getDrawable(R.drawable.ic_flash_img_close_icon))
                    closeCamera()
                    openCamera()
                }
            } else if (currentCameraId == cameraFrontId) {
                if (!hasBackCamera) customToast(this, getString(R.string.cannot_find_back))
                else {
                    currentZoomLevel = 1f
                    seekBar?.progress = 10
                    currentCameraId = cameraBackId
                    chooseBestPreViewSize()
                    changeCameraButton?.setImageDrawable(getDrawable(R.drawable.ic_camera_picker_img_icon))
                    flashIsOn = false
                    flashButton?.setImageDrawable(getDrawable(R.drawable.ic_flash_img_close_icon))
                    closeCamera()
                    openCamera()
                }
            }
        }

        addButton?.setOnClickListener {
            if (currentZoomLevel < maxZoom) {
                currentZoomLevel += (maxZoom - 1) / 5
                if (currentZoomLevel > maxZoom) currentZoomLevel = maxZoom //用于解决浮点数运算误差
                updateCameraPreview()
                seekBar?.progress = (currentZoomLevel * 10).toInt()
            }

        }

        minusButton?.setOnClickListener {
            if (currentZoomLevel > 1) {
                currentZoomLevel -= (maxZoom - 1) / 5
                if (currentZoomLevel < 1) currentZoomLevel = 1f //用于解决浮点数运算误差
                updateCameraPreview()
                seekBar?.progress = (currentZoomLevel * 10).toInt()
            }
        }

        seekBar?.max = (maxZoom * 10).toInt()
        seekBar?.min = 1 * 10
        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val newProgress = progress.toFloat() / 10
                    currentZoomLevel = newProgress
                    updateCameraPreview()
                    seekBar.progress = progress  // 设置新的进度值
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

    }

    override fun onResume() {
        super.onResume()
        openCamera()
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
    }

    private fun updateCameraPreview() {
        if (cameraDevice != null && previewBuilder != null) {
            zoomRect = getZoomRect(currentZoomLevel)
            if (zoomRect == null) return
            previewBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
            cameraCaptureSession?.setRepeatingRequest(
                previewBuilder!!.build(),
                null,
                null
            )
        }
    }

    private fun changeFlashlightStatus(on: Boolean) {
        if (cameraDevice != null && previewBuilder != null) {
            val flashMode =
                if (on) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
            previewBuilder!!.set(CaptureRequest.FLASH_MODE, flashMode)
            try {
                cameraCaptureSession?.setRepeatingRequest(previewBuilder!!.build(), null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCameraInfo(): Boolean {
        try {
            val cameraIdList = cameraManager?.cameraIdList
            if (cameraIdList?.size == 0) return false
            else {
                for (cameraId in cameraIdList!!) {
                    val characteristics = cameraManager!!.getCameraCharacteristics(cameraId)
                    val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                        hasFrontCamera = true
                        if (cameraFrontId == "") cameraFrontId = cameraId
                    }
                    if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        hasBackCamera = true
                        if (cameraBackId == "") cameraBackId = cameraId
                    }
                }
                currentCameraId = if (!hasBackCamera) cameraFrontId else cameraBackId
                return hasFrontCamera || hasBackCamera
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun getZoomRect(zoomLevel: Float): Rect? {
        try {
            val cameraCharacteristics =
                cameraManager!!.getCameraCharacteristics(currentCameraId)
            val activeRect =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
            val minW = (activeRect.width() / maxZoom).toInt()
            val minH = (activeRect.height() / maxZoom).toInt()
            val difW = activeRect.width() - minW
            val difH = activeRect.height() - minH

            val cropW = (difW * (zoomLevel - 1) / (maxZoom - 1) / 2).toInt()
            val cropH = (difH * (zoomLevel - 1) / (maxZoom - 1) / 2).toInt()

            return Rect(
                cropW,
                cropH,
                activeRect.width() - cropW,
                activeRect.height() - cropH
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun chooseBestPreViewSize() {
        try {
            if (cameraManager == null) return
            //获取摄像头属性描述
            val cameraCharacteristics =
                cameraManager!!.getCameraCharacteristics(currentCameraId)
            //获取摄像头支持的配置属性
            val map =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            //是否支持闪光灯
            flashAvailable =
                cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            //最大变焦倍数
            maxZoom =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)!!

            val list =
                map!!.getOutputSizes(SurfaceTexture::class.java)
            previewSizes = getOptimalSize(list, screenWidth, screenHeight)
            if (previewSizes == null) previewSizes = Size(1920, 1200)
            println("最终的预览尺寸大小是    ${previewSizes!!.width}    ${previewSizes!!.height}")
            val layoutParams = textureView!!.layoutParams
            layoutParams.height = previewSizes!!.width
            layoutParams.width = previewSizes!!.height
            textureView!!.layoutParams = layoutParams
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            if (cameraManager == null) return
            cameraManager!!.openCamera(
                currentCameraId,
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
            cameraCaptureSession?.close()
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
            //设置闪光灯模式
            if (flashAvailable && flashIsOn) previewBuilder!!.set(
                CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_TORCH
            )
            //设置数码变焦倍数
            if (currentZoomLevel != 1f) previewBuilder!!.set(
                CaptureRequest.SCALER_CROP_REGION,
                zoomRect
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