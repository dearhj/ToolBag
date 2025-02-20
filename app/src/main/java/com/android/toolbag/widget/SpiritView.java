package com.android.toolbag.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.android.toolbag.R;

public class SpiritView extends View {
    private Bitmap centerImage; // 中心图片
    private Paint paint;
    private float[] circleRadii; // 四个圆的半径
    private int[] circleColors; // 四个圆的颜色
    private float width;
    private float height;
    private float bubbleX;
    private float bubbleY;

    public SpiritView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SpiritView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context);
    }

    private void init(Context context) {
        // 获取屏幕宽度和高度
        width = context.getResources().getDisplayMetrics().widthPixels;
        height = context.getResources().getDisplayMetrics().heightPixels - 150; //消除顶部状态栏和底部导航栏带来的误差

        // 计算圆的半径
        float minDimension = Math.min(width, height);
        float maxSizeCircle = minDimension / 4;
        circleRadii = new float[]{maxSizeCircle, (maxSizeCircle / 4) * 3 , (maxSizeCircle / 4) * 2, maxSizeCircle / 4};

        // 配置四个圆的颜色
        circleColors = new int[]{
                ContextCompat.getColor(context, R.color.color_circle_4),
                ContextCompat.getColor(context, R.color.color_circle_3),
                ContextCompat.getColor(context, R.color.color_circle_2),
                ContextCompat.getColor(context, R.color.color_circle_1)
        };

        // 加载中心图片
        centerImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.gradienter_bubble);

        bubbleX = width / 2;
        bubbleY = height / 2;

        // 初始化画笔
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // 绘制四个圆
        for (int i = 0; i < circleRadii.length; i++) {
            paint.setColor(circleColors[i]);
            canvas.drawCircle(width / 2, height / 2, circleRadii[i], paint);
        }

        // 绘制十字线
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(4);
        canvas.drawLine(width / 2, 0, width / 2, height, paint); // 垂直线
        canvas.drawLine(0, height / 2, width, height / 2, paint); // 水平线

        // 绘制中心图片
        if (centerImage != null) {
            float targetWidth = circleRadii[3]; //将图片宽度设置最小内圆的半径
            // 保持图片为正方形
            Matrix matrix = new Matrix();
            matrix.postScale(targetWidth / centerImage.getWidth(), targetWidth / centerImage.getHeight());
            matrix.postTranslate(bubbleX - targetWidth / 2f, bubbleY - targetWidth / 2f);
            canvas.drawBitmap(centerImage, matrix, null);
        }
    }

    public float getScreenWidth() {
        return width;
    }

    public float getScreenHeight() {
        return height;
    }

    public void updateSpiritViewUI(float widthX, float heightY) {
        bubbleX = widthX;
        bubbleY = heightY;
        postInvalidate();
    }
}
