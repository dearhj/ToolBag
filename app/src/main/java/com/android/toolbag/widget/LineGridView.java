package com.android.toolbag.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

public class LineGridView extends GridView {

    private final Context mContext;

    public LineGridView(Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        // 获取每行的子项数量
        int lineCount = getWidth() / getChildAt(0).getWidth();
        // 获取子项总数
        int childCount = getChildCount();
        Paint paint = new Paint();
        paint.setStrokeWidth(5.0f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GRAY);
        int lastLineCount = childCount % lineCount;
        //绘制除最后一行外的垂直线和水平线
        for (int itemIndex = 0; itemIndex < childCount - lastLineCount; itemIndex++) {
            View childAt = getChildAt(itemIndex);
            if ((itemIndex + 1) % lineCount != 0) { //绘制垂直线
                float startY = ((float) (childAt.getBottom() - childAt.getTop()) / 4) + childAt.getTop();
                canvas.drawLine(childAt.getRight(), startY, childAt.getRight(), childAt.getBottom(), paint);
            }
            // 绘制水平线
            canvas.drawLine(childAt.getLeft(), childAt.getBottom(), childAt.getRight(), childAt.getBottom(), paint);
        }
        // 处理最后一行的水平线及其垂直线
        if (lastLineCount != 0) {
            // 获取最后一行的第一个子项
            View lastLineFirstItem = getChildAt(childCount - (childCount % lineCount));
            // 获取最后一行的最后一个子项
            View lastLineLastItem = getChildAt(childCount - 1);
            //绘制最后一行的垂直线
            float startY = ((float) (lastLineFirstItem.getBottom() - lastLineFirstItem.getTop()) / 4) + lastLineFirstItem.getTop();
            float startX = lastLineFirstItem.getRight();
            for (int lastLineIndex = 0; lastLineIndex < lineCount - 1; lastLineIndex ++) {
                canvas.drawLine(startX, startY, startX, lastLineFirstItem.getBottom(), paint);
                startX = startX + lastLineFirstItem.getWidth();
            }
            // 绘制最后一行的水平线
            canvas.drawLine(lastLineFirstItem.getLeft(), lastLineLastItem.getBottom(), lastLineLastItem.getRight() + (lastLineLastItem.getWidth() * (lineCount - (childCount % lineCount))), lastLineLastItem.getBottom(), paint);
        }
    }

    public LineGridView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mContext = context;
    }

    public LineGridView(Context context, AttributeSet attributeSet, int i2) {
        super(context, attributeSet, i2);
        this.mContext = context;
    }
}