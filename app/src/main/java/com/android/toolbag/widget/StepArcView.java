package com.android.toolbag.widget;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.android.toolbag.R;

public class StepArcView extends View {

    /**
     * 圆弧的宽度
     */
    private final float borderWidth = dipToPx(14);
    /**
     * 画步数的数值的字体大小
     */
    private float numberTextSize = 0;
    /**
     * 步数
     */
    private String stepNumber = "0";
    /**
     * 今日目标步数
     */
    private int todayStepNumber = 7000;
    /**
     * 开始绘制圆弧的角度
     */
    private final float startAngle = 135;
    /**
     * 终点对应的角度和起始点对应的角度的夹角
     */
    private final float angleLength = 270;
    /**
     * 所要绘制的当前步数的绿色圆弧终点到起点的夹角
     */
    private float currentAngleLength = 0;

    private final Context mContext;

    public StepArcView(Context context) {
        super(context);
        mContext = context;
    }

    public StepArcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public StepArcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }


    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        // 中心点的x坐标
        float centerX = (float) (getWidth()) / 2;
        //指定圆弧的外轮廓矩形区域
        RectF rectF = new RectF(0 + borderWidth, borderWidth, 2 * centerX - borderWidth, 2 * centerX - borderWidth);

        //【第一步】绘制整体的灰色圆弧
        drawArcYellow(canvas, rectF);
        //【第二步】绘制当前进度的绿色圆弧
        drawArcRed(canvas, rectF);
        //【第三步】绘制当前进度的绿色数字
        drawTextNumber(canvas, centerX);
        //【第四步】绘制"今日目标"的灰色文字
        drawTextStepString(canvas, centerX);
        //【第五步】绘制"今日目标"的灰色数字
        drawTextStepToday(canvas, centerX);
    }

    /**
     * 1.绘制总步数的灰色圆弧
     *
     * @param canvas 画笔
     * @param rectF  参考的矩形
     */
    private void drawArcYellow(Canvas canvas, RectF rectF) {
        Paint paint = new Paint();
        // 默认画笔颜色，灰色
        paint.setColor(getResources().getColor(R.color.color_circle_2));
        // 结合处为圆弧
        paint.setStrokeJoin(Paint.Join.ROUND);
        // 设置画笔的样式 Paint.Cap.Round ,Cap.SQUARE等分别为圆形、方形
        paint.setStrokeCap(Paint.Cap.ROUND);
        // 设置画笔的填充样式 Paint.Style.FILL  :填充内部;Paint.Style.FILL_AND_STROKE  ：填充内部和描边;  Paint.Style.STROKE  ：仅描边
        paint.setStyle(Paint.Style.STROKE);
        //抗锯齿功能
        paint.setAntiAlias(true);
        //设置画笔宽度
        paint.setStrokeWidth(borderWidth);

        /*绘制圆弧的方法
         * drawArc(RectF oval, float startAngle, float sweepAngle, boolean useCenter, Paint paint)//画弧，
         参数一是RectF对象，一个矩形区域椭圆形的界限用于定义在形状、大小、电弧，
         参数二是起始角(度)在电弧的开始，圆弧起始角度，单位为度。
         参数三圆弧扫过的角度，顺时针方向，单位为度,从右中间开始为零度。
         参数四是如果这是true(真)的话,在绘制圆弧时将圆心包括在内，通常用来绘制扇形；如果它是false(假)这将是一个弧线,
         参数五是Paint对象；
         */
        canvas.drawArc(rectF, startAngle, angleLength, false, paint);

    }

    /**
     * 2.绘制当前步数的绿色圆弧
     */
    private void drawArcRed(Canvas canvas, RectF rectF) {
        Paint paintCurrent = new Paint();
        paintCurrent.setStrokeJoin(Paint.Join.ROUND);
        paintCurrent.setStrokeCap(Paint.Cap.ROUND);//圆角弧度
        paintCurrent.setStyle(Paint.Style.STROKE);//设置填充样式
        paintCurrent.setAntiAlias(true);//抗锯齿功能
        paintCurrent.setStrokeWidth(borderWidth);//设置画笔宽度
        paintCurrent.setColor(getResources().getColor(R.color.sound_value1));//设置画笔颜色
        canvas.drawArc(rectF, startAngle, currentAngleLength, false, paintCurrent);
    }

    /**
     * 3.圆环中心的步数
     */
    private void drawTextNumber(Canvas canvas, float centerX) {
        Paint vTextPaint = new Paint();
        vTextPaint.setTextAlign(Paint.Align.CENTER);
        vTextPaint.setAntiAlias(true);//抗锯齿功能
        vTextPaint.setTextSize(numberTextSize);
        Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        vTextPaint.setTypeface(font);//字体风格
        vTextPaint.setColor(getResources().getColor(R.color.sound_value1));
        Rect bounds_Number = new Rect();
        vTextPaint.getTextBounds(stepNumber, 0, stepNumber.length(), bounds_Number);
        canvas.drawText(stepNumber, centerX, (float) getHeight() / 2 + (float) bounds_Number.height() / 2, vTextPaint);
    }

    /**
     * 4.圆环中心[今日目标]的文字
     */
    private void drawTextStepString(Canvas canvas, float centerX) {
        Paint vTextPaint = new Paint();
        vTextPaint.setTextSize(dipToPx(16));
        vTextPaint.setTextAlign(Paint.Align.CENTER);
        vTextPaint.setAntiAlias(true);//抗锯齿功能
        vTextPaint.setColor(getResources().getColor(R.color.white));
        String stepString = mContext.getString(R.string.today_target);
        Rect bounds = new Rect();
        vTextPaint.getTextBounds(stepString, 0, stepString.length(), bounds);
        canvas.drawText(stepString, centerX, (float) getHeight() / 2 + bounds.height() + getFontHeight(numberTextSize), vTextPaint);
    }

    /**
     * 4.圆环中心[今日目标]的数字
     */
    private void drawTextStepToday(Canvas canvas, float centerX) {
        Paint vTextPaint = new Paint();
        vTextPaint.setTextSize(dipToPx(16));
        vTextPaint.setTextAlign(Paint.Align.CENTER);
        vTextPaint.setAntiAlias(true);//抗锯齿功能
        vTextPaint.setColor(getResources().getColor(R.color.white));
        String stepString = String.valueOf(todayStepNumber);
        Rect bounds = new Rect();
        vTextPaint.getTextBounds(stepString, 0, stepString.length(), bounds);
        canvas.drawText(stepString, centerX, (float) getHeight() / 2 + bounds.height() * 3 + getFontHeight(numberTextSize), vTextPaint);
    }

    /**
     * 获取当前步数的数字的高度
     *
     * @param fontSize 字体大小
     * @return 字体高度
     */
    public int getFontHeight(float fontSize) {
        Paint paint = new Paint();
        paint.setTextSize(fontSize);
        Rect bounds_Number = new Rect();
        paint.getTextBounds(stepNumber, 0, stepNumber.length(), bounds_Number);
        return bounds_Number.height();
    }

    /**
     * dip 转换成px
     */

    private int dipToPx(float dip) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dip * density + 0.5f * (dip >= 0 ? 1 : -1));
    }

    /**
     * 所走的步数进度
     *
     * @param currentCounts 所走步数
     */
    public void setCurrentCount(long currentCounts) {
        //如果当前走的步数超过总步数则圆弧还是270度，不能成为园
        long count = Math.min(currentCounts, todayStepNumber);
        //上次所走步数占用总共步数的百分比
        float scalePrevious = (float) Integer.parseInt(stepNumber) / todayStepNumber;
        if (scalePrevious >= 1) scalePrevious = 1f;
        //换算成弧度最后要到达的角度的长度-->弧长
        float previousAngleLength = scalePrevious * angleLength;
        //所走步数占用总共步数的百分比
        float scale = (float) count / todayStepNumber;
        //换算成弧度最后要到达的角度的长度-->弧长
        float currentAngleLength = scale * angleLength;
        //开始执行动画
        setAnimation(previousAngleLength, currentAngleLength);

        stepNumber = String.valueOf(currentCounts);
        setTextSize(currentCounts);
    }

    public void setTodayTargetCount(int totalStepNum) {
        todayStepNumber = totalStepNum;
        //所走步数占用总共步数的百分比
        float scale = (float) Integer.parseInt(stepNumber) / todayStepNumber;
        if (scale >= 1) scale = 1;
        //换算成弧度最后要到达的角度的长度-->弧长
        float currentAngleLength = scale * angleLength;
        //开始执行动画
        setAnimation(0f, currentAngleLength);
    }

    public void setTarget(int targetNum){
        todayStepNumber = targetNum;
    }

    /**
     * 为进度设置动画
     * ValueAnimator是整个属性动画机制当中最核心的一个类，属性动画的运行机制是通过不断地对值进行操作来实现的，
     * 而初始值和结束值之间的动画过渡就是由ValueAnimator这个类来负责计算的。
     * 它的内部使用一种时间循环的机制来计算值与值之间的动画过渡，
     * 我们只需要将初始值和结束值提供给ValueAnimator，并且告诉它动画所需运行的时长，
     * 那么ValueAnimator就会自动帮我们完成从初始值平滑地过渡到结束值这样的效果。
     *
     * @param start   初始值
     * @param current 结束值
     */
    private void setAnimation(float start, float current) {
        ValueAnimator progressAnimator = ValueAnimator.ofFloat(start, current);
        progressAnimator.setDuration(1200);
        progressAnimator.setTarget(currentAngleLength);
        progressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                //每次在初始值和结束值之间产生的一个平滑过渡的值，逐步去更新进度
                currentAngleLength = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        progressAnimator.start();
    }

    /**
     * 设置文本大小,防止步数特别大之后放不下，将字体大小动态设置
     */
    public void setTextSize(long num) {
        String s = String.valueOf(num);
        int length = s.length();
        if (length <= 4) {
            numberTextSize = dipToPx(50);
        } else if (length <= 6) {
            numberTextSize = dipToPx(40);
        } else if (length <= 8) {
            numberTextSize = dipToPx(30);
        } else {
            numberTextSize = dipToPx(25);
        }
    }
}

