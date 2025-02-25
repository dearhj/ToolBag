package com.android.toolbag.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.android.toolbag.R

class ProtractorBackView(
    private val mContext: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(mContext, attributeSet, defStyleAttr) {
    private var mBitmapProtractor: Bitmap? = null
    private var mViewHeight = 0
    private var mViewWidth = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    override fun onSizeChanged(newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight)
        mViewWidth = newWidth
        mViewHeight = newHeight
        mBitmapProtractor = BitmapFactory.decodeResource(mContext.resources, R.drawable.protractor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(
            mViewWidth.toFloat() / 2,
            mViewHeight.toFloat() - 50
        )   //-50将图片上移50dp，防止用户拖动指针时误触退出
        drawProtractor(canvas)
    }

    private fun drawProtractor(canvas: Canvas) {
        val bitmap = mBitmapProtractor ?: return

        val targetHeight = (mViewHeight * 9 / 10).toFloat()
        val scale = targetHeight / bitmap.height  // 计算缩放比例

        // 根据缩放比例计算宽度
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale

        // 绘制图片
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(
                -scaledWidth / 2, // 水平居中
                -scaledHeight, // 底部对齐
                scaledWidth / 2,
                0f
            ),
            null
        )
    }
}
