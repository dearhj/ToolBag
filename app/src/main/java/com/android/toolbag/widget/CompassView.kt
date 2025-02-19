package com.android.toolbag.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView

@SuppressLint("AppCompatCustomView")
class CompassView : ImageView {
    private var compass: Drawable?
    private var mDirection: Float

    constructor(context: Context?) : super(context) {
        mDirection = 0.0f
        compass = null
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {
        mDirection = 0.0f
        compass = null
    }

    constructor(context: Context?, attributeSet: AttributeSet?, i: Int) : super(
        context,
        attributeSet,
        i
    ) {
        mDirection = 0.0f
        compass = null
    }

    override fun onDraw(canvas: Canvas) {
        if (compass == null) {
            val drawable = drawable
            compass = drawable
            drawable.setBounds(0, 0, width, height)
        }
        canvas.save()
        canvas.rotate(mDirection, (width / 2).toFloat(), (height / 2).toFloat())
        compass!!.draw(canvas)
        canvas.restore()
    }

    fun updateDirection(f: Float) {
        mDirection = f
        invalidate()
    }
}