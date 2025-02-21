package com.android.toolbag.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.NumberPicker

class CustomNumberPicker : NumberPicker {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun addView(child: View) {
        super.addView(child)
        updateView(child)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        super.addView(child, index, params)
        updateView(child)
    }

    private fun updateView(view: View) {
        if (view is EditText) {
            view.setTextColor(Color.parseColor("#FFFFFF")) //白色
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }
    }
}