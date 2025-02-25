package com.android.toolbag.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.icu.text.DecimalFormat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.android.toolbag.R
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ProtractorPoint(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attributeSet, defStyleAttr) {
    private var isChoosePointerOne: Boolean
    private var isChoosePointerTwo: Boolean
    private var mBitmapProtractor: Bitmap? = null
    private val mComputeBounds: Int
    private val mContext: Context
    private val mDecimalFormat: DecimalFormat
    private var mDegreeOne: Double
    private var mDegreeTwo: Double
    private var mDotPaint: Paint? = null
    private val mDotPath = Path()
    private var mPointPaint: Paint? = null
    private val mPointRadio = 0.76
    private val mPointerPathOne = Path()
    private val mPointerPathTwo = Path()
    private var mRect: Rect
    private var mRectPathOne = Region()
    private var mRectPathTwo = Region()
    private var mScreenHeight = 0
    private var mScreenWidth = 0
    private var mTextColor: Int
    private var mTextPaint: Paint? = null
    private var mTextSize: Int
    private var mViewHeight = 0
    private var mViewWidth = 0
    private val pointerColor = Color.parseColor("#1080C3")   //指针颜色
    private val protractorTextViewMarginLeft: Int
    private val protractorTextViewMarginTop: Int

    init {
        isChoosePointerOne = false
        isChoosePointerTwo = false
        mDegreeOne = 45.0
        mDegreeTwo = 135.0
        mRect = Rect()
        mTextColor = Color.parseColor("#39B54A")  //文字画笔
        mTextSize = 30
        mDecimalFormat = DecimalFormat("0.00") //度数保留两位小数
        mContext = context
        mComputeBounds = context.resources.getDimensionPixelSize(R.dimen.configs_compute_bounds)
        mTextSize = context.resources.getDimensionPixelSize(R.dimen.configs_protractor_textSize)
        protractorTextViewMarginTop =
            context.resources.getDimensionPixelSize(R.dimen.protractor_text_marginTop)
        protractorTextViewMarginLeft =
            context.resources.getDimensionPixelSize(R.dimen.protractor_text_marginLeft)
        initPaint()
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    private fun initPaint() {
        mTextColor = Color.parseColor("#39B54A")   //文字画笔
        val paint = Paint()
        mPointPaint = paint
        paint.color = pointerColor   //指针1颜色
        mPointPaint!!.isAntiAlias = true
        mPointPaint!!.style = Paint.Style.STROKE
        mPointPaint!!.strokeWidth = 5.0f
        val paint2 = Paint()
        mDotPaint = paint2
        paint2.color = pointerColor  //指针2颜色
        mDotPaint!!.isAntiAlias = true
        mDotPaint!!.style = Paint.Style.FILL
        mDotPaint!!.strokeWidth = 5.0f
        val paint3 = Paint()
        mTextPaint = paint3 //文字画笔
        paint3.isAntiAlias = true
        mTextPaint!!.color = mTextColor
        mTextPaint!!.textSize = mTextSize.toFloat()
    }

    override fun onSizeChanged(newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight)
        mScreenWidth = newWidth
        mScreenHeight = newHeight
        mViewWidth = width
        mViewHeight = height - 66  // 16 + 50 16为图片底部高度，50为上移距离
        val decodeResource = BitmapFactory.decodeResource(mContext.resources, R.drawable.protractor)
        Matrix().postRotate(-90.0f)
        mBitmapProtractor = Bitmap.createBitmap(
            decodeResource,
            0,
            0,
            decodeResource.width,
            decodeResource.height,
            null,
            true
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawPoint(canvas, mDegreeOne, mDegreeTwo)
        val text = mDecimalFormat.format(abs(mDegreeOne - mDegreeTwo)) + "°"
        mTextPaint!!.getTextBounds(text, 0, text.length, mRect)
        canvas.drawText(
            text,
            ((mViewWidth / 2) - (mRect.width() / 2)).toFloat(),
            (protractorTextViewMarginTop + mRect.height()).toFloat(),
            mTextPaint!!
        )
    }

    private fun drawPoint(canvas: Canvas, d: Double, d2: Double) {
        //第一条边
        mPointerPathOne.reset()
        mPointerPathOne.moveTo((mViewWidth / 2).toFloat(), mViewHeight.toFloat())
        val path = mPointerPathOne
        val d3 = (d / 180.0) * 3.141592653589793
        val cos = cos(d3)
        val i = mViewHeight
        path.lineTo(
            (mViewWidth / 2) - (((cos * i) * mPointRadio).toFloat()),
            i - (((sin(d3) * mViewHeight) * mPointRadio).toFloat())
        )
        mPointerPathOne.close() //关闭指针路径
        //计算边界矩形
        mRectPathOne = cRectF(d3)

        canvas.drawPath(mPointerPathOne, mPointPaint!!)

        //debug调试用
//        mPointerPathOne.computeBounds(mRectFOne, true)
//        val rectF = mRectFOne
//        val i2 = mComputeBounds //边界矩形误差，用于优化触摸检测
//        rectF[((rectF.left.toInt()) - i2).toFloat(), ((rectF.top.toInt()) - i2).toFloat(), ((rectF.right.toInt()) + i2).toFloat()] =
//            ((rectF.bottom.toInt()) + i2).toFloat()
//        val debugPaint = Paint().apply {
//            color = Color.RED
//            style = Paint.Style.STROKE
//            strokeWidth = 2f
//        }
//        canvas.drawRect(mRectFOne, debugPaint)

        //第二条边
        mPointerPathTwo.reset()
        mPointerPathTwo.moveTo((mViewWidth / 2).toFloat(), mViewHeight.toFloat())
        val path2 = mPointerPathTwo
        val d4 = (d2 / 180.0) * 3.141592653589793
        val cos2 = cos(d4)
        val i3 = mViewHeight
        path2.lineTo(
            (mViewWidth / 2) - (((cos2 * i3) * mPointRadio).toFloat()),
            i3 - (((sin(d4) * mViewHeight) * mPointRadio).toFloat())
        )
        mPointerPathTwo.close()
        //计算边界矩形
        mRectPathTwo = cRectF(d4)

        canvas.drawPath(mPointerPathTwo, mPointPaint!!)

        //debug调试用
//        mPointerPathTwo.computeBounds(mRectFTwo, true)
//        val rectF2 = mRectFTwo
//        val i4 = mComputeBounds
//        rectF2[((rectF2.left.toInt()) - i4).toFloat(), ((rectF2.top.toInt()) - i4).toFloat(), ((rectF2.right.toInt()) + i4).toFloat()] =
//            ((rectF2.bottom.toInt()) + i4).toFloat()
//        val debugPaint1 = Paint().apply {
//            color = Color.RED
//            style = Paint.Style.STROKE
//            strokeWidth = 2f
//        }
//        canvas.drawRect(mRectFTwo, debugPaint1)

        //底部原点
        mDotPath.reset()
        mDotPath.addCircle(
            (mViewWidth / 2).toFloat(),
            mViewHeight.toFloat(),
            15.0f,
            Path.Direction.CW
        )
        canvas.drawPath(mDotPath, mDotPaint!!)
    }

    private fun cRectF(degree: Double) :Region{
        /// 计算指针的起点和终点
        val startX = mViewWidth / 2f
        val startY = mViewHeight.toFloat()
        val endX = startX - (cos(degree) * mViewHeight * mPointRadio).toFloat()
        val endY = startY - (sin(degree) * mViewHeight * mPointRadio).toFloat()

        // 计算线段的方向向量
        val dx = endX - startX
        val dy = endY - startY

        // 计算法向量
        val normalX = -dy
        val length = sqrt(dx * dx + dy * dy)  // 线段长度
        val normalLength = 25
        val unitNormalX = normalX / length * normalLength
        val unitNormalY = dx / length * normalLength

        // 计算矩形的四个顶点
        val p1 = PointF(startX + unitNormalX, startY + unitNormalY)
        val p2 = PointF(startX - unitNormalX, startY - unitNormalY)
        val p3 = PointF(endX - unitNormalX, endY - unitNormalY)
        val p4 = PointF(endX + unitNormalX, endY + unitNormalY)

        // 创建矩形路径
        val rectPath = Path()
        rectPath.moveTo(p1.x, p1.y)
        rectPath.lineTo(p2.x, p2.y)
        rectPath.lineTo(p3.x, p3.y)
        rectPath.lineTo(p4.x, p4.y)
        rectPath.close()

        val rectF = RectF()
        rectPath.computeBounds(rectF, true)  // 计算 Path 的边界矩形
        val region = Region(rectF.left.toInt(), rectF.top.toInt(), rectF.right.toInt(), rectF.bottom.toInt())
        region.setPath(rectPath, region)

        return region
    }

    private fun setDegree(firstLineDegree: Double, secondLineDegree: Double) {
        if ((0.0 > firstLineDegree || firstLineDegree > 180.0) && (0.0 > secondLineDegree || secondLineDegree > 180.0)) return
        mDegreeOne = firstLineDegree
        mDegreeTwo = secondLineDegree
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == 0) {
            val x = motionEvent.x.toInt()
            val y = motionEvent.y.toInt()
            if (mRectPathOne.contains(x, y)) {
                isChoosePointerOne = true
                invalidate()
                return true
            } else if (mRectPathTwo.contains(x, y)) {
                isChoosePointerTwo = true
                invalidate()
                return true
            } else {
                return false
            }
        } else if (motionEvent.action == 2) {
            if (isChoosePointerOne) {
                val x2 = motionEvent.x
                val y2 = motionEvent.y
                val i = mViewHeight
                if (y2 <= i) {
                    val i2 = mViewWidth
                    if (x2 != (i2 / 2).toFloat()) {
                        setDegree(
                            (atan2(
                                (i - y2).toDouble(),
                                ((i2 / 2) - x2).toDouble()
                            ) / 3.141592653589793) * 180.0, mDegreeTwo
                        )
                        return true
                    }
                }
                if (y2 > i && x2 < mViewWidth / 2) {
                    setDegree(0.0, mDegreeTwo)
                } else if (y2 > i && x2 > mViewWidth / 2) {
                    setDegree(180.0, mDegreeTwo)
                }
                return true
            } else if (isChoosePointerTwo) {
                val x3 = motionEvent.x
                val y3 = motionEvent.y
                val i3 = mViewHeight
                if (y3 <= i3) {
                    val i4 = mViewWidth
                    if (x3 != (i4 / 2).toFloat()) {
                        setDegree(
                            mDegreeOne,
                            (atan2(
                                (i3 - y3).toDouble(),
                                ((i4 / 2) - x3).toDouble()
                            ) / 3.141592653589793) * 180.0
                        )
                        return true
                    }
                }
                if (y3 > i3 && x3 < mViewWidth / 2) {
                    setDegree(mDegreeOne, 0.0)
                } else if (y3 > i3 && x3 > mViewWidth / 2) {
                    setDegree(mDegreeOne, 180.0)
                }
                return true
            } else {
                return false
            }
        } else if (motionEvent.action == 1 || motionEvent.action == 3) {
            isChoosePointerOne = false
            isChoosePointerTwo = false
            invalidate()
            return true
        } else {
            return super.onTouchEvent(motionEvent)
        }
    }
}
