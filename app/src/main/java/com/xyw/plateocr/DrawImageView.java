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
    private Paint mLinePaint;
    private Paint mAreaPaint;
    private Context mContext;
    private int widthScreen, heightScreen;

    public DrawImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
        mContext = context;
        widthScreen = context.getResources().getDisplayMetrics().widthPixels;
        heightScreen = context.getResources().getDisplayMetrics().heightPixels;
    }

    private void initPaint() {
        //�����м�͸��������α߽��Paint
        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setColor(Color.BLUE);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(10f);
        mLinePaint.setAlpha(30);

        //����������Ӱ����
        mAreaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAreaPaint.setColor(Color.GRAY);
        mAreaPaint.setStyle(Paint.Style.FILL);
        mAreaPaint.setAlpha(180);


    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mRect == null)
            return;
        canvas.drawRect(0, 0, widthScreen, mRect.top, mAreaPaint);
        canvas.drawRect(0, mRect.bottom + 1, widthScreen, heightScreen, mAreaPaint);
        canvas.drawRect(0, mRect.top, mRect.left - 1, mRect.bottom + 1, mAreaPaint);
        canvas.drawRect(mRect.right + 1, mRect.top, widthScreen, mRect.bottom + 1, mAreaPaint);

        //����Ŀ��͸������
        canvas.drawRect(mRect, mLinePaint);
        super.onDraw(canvas);

    }

    private Rect mRect = null;

    public void setRect(Rect maskRect) {
        this.mRect = maskRect;
        postInvalidate();
    }
}
