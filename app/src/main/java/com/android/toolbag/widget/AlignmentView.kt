package com.android.toolbag.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class AlignmentView(
    context: Context?,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attributeSet, defStyleAttr) {
    private var mDegree = 0.0
    private var mPaint: Paint? = null
    private var mPaint1: Paint? = null
    private var mViewHeight = 0
    private var mViewWidth = 0

    init {
        initPaint()
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    override fun onSizeChanged(newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight)
        mViewWidth = newWidth
        mViewHeight = newHeight
    }

    private fun initPaint() {
        val paint = Paint()
        mPaint = paint
        paint.color = Color.BLUE
        mPaint!!.flags = Paint.ANTI_ALIAS_FLAG
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeWidth = 5.0f
        mPaint!!.setPathEffect(DashPathEffect(floatArrayOf(20.0f, 20.0f), 0.0f))
        val paint2 = Paint()
        mPaint1 = paint2
        paint2.color = Color.WHITE
        mPaint1!!.flags = Paint.ANTI_ALIAS_FLAG
        mPaint1!!.strokeWidth = 5.0f
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = Rect()
        getDrawingRect(rect)
        canvas.drawLine(
            mViewWidth.toFloat() / 2, 0.0f, mViewWidth.toFloat() / 2, mViewHeight.toFloat(),
            mPaint1!!
        )
        canvas.drawLine(
            0.0f, mViewHeight.toFloat() / 2, mViewWidth.toFloat(), mViewHeight.toFloat() / 2,
            mPaint1!!
        )
        canvas.save()
        canvas.rotate(
            mDegree.toInt().toFloat(),
            rect.width().toFloat() / 2,
            rect.height().toFloat() / 2
        )
        val path = Path()
        path.moveTo(mViewWidth.toFloat() / 2, -mViewHeight.toFloat())
        path.lineTo(mViewWidth.toFloat() / 2, (mViewHeight * 3).toFloat())
        canvas.drawPath(path, mPaint!!)
        canvas.save()
        val path2 = Path()
        path2.moveTo(-48.0f, mViewHeight.toFloat() / 2)
        path2.lineTo((mViewWidth * 3).toFloat(), mViewHeight.toFloat() / 2)
        canvas.drawPath(path2, mPaint!!)
        canvas.save()
    }

    fun setDegree(f: Float) {
        mDegree = f.toDouble()
        invalidate()
    }
}
