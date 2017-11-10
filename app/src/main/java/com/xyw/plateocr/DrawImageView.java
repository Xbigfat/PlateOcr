package com.xyw.plateocr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by 31429 on 2017/11/8.
 */

@SuppressLint("AppCompatCustomView")
public class DrawImageView extends ImageView {
    public DrawImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    //绘画矩形
    Paint paintRectangle = new Paint();

    {
        paintRectangle.setAntiAlias(true);
        paintRectangle.setColor(Color.CYAN);
        paintRectangle.setStyle(Paint.Style.STROKE);
        paintRectangle.setStrokeWidth(10);// 设置线宽
        paintRectangle.setAlpha(100);
    }

    Paint mask = new Paint();

    {
        mask.setColor(Color.BLACK);
        mask.setAlpha(100);
        mask.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        // x 屏幕的宽度  y 屏幕的高度
/*        int x = canvas.getWidth();
        int y = canvas.getHeight();*/
        int x = this.getWidth();
        int y = this.getHeight();
        int top = (int) (x / 10 * 4);
        int bottom = (int) (x / 10 * 8);
        canvas.drawRect(new Rect(0, top, x, bottom), paintRectangle);// 绘制矩形
        canvas.drawRect(new Rect(0, 0, x, top), mask);
        canvas.drawRect(new Rect(0, bottom, x, y), mask);

    }
}
