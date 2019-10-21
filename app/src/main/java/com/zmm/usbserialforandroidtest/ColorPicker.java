package com.zmm.usbserialforandroidtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 旋转取色圆环
 * Created by null on 2016/8/31.
 */

public class ColorPicker extends AppCompatImageView implements View.OnTouchListener {
    private int color;
    private Bitmap bp;//色轮图片
    private int bw, bh;//色轮图片的尺寸
    private float x, y, radio;
    private OnColorSelectListener onColorSelectListener;

    public ColorPicker(Context context) {
        this(context, null);
    }

    public ColorPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int i = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int j = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        measure(i, j);
        setOnTouchListener(this);
        setClickable(true);
        bp = BitmapFactory.decodeResource(context.getResources(), R.mipmap.color_circle);
        bw = bp.getWidth();
        bh = bp.getHeight();
        setImageBitmap(bp);

    }

    public int getColor() {
        return color;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        try {
            float xx = event.getX();
            float yy = event.getY();
            System.out.println("xx="+xx+" yy="+yy);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    invalidate();
                    if (onColorSelectListener != null && bp != null) {
                        onColorSelectListener.onStartSelect(bp.getPixel((int) xx, (int) yy));
                    }
                    System.out.println("ACTION_DOWN xx="+xx+" yy="+yy);
                    break;
                case MotionEvent.ACTION_MOVE:

                    invalidate();
                    if (onColorSelectListener != null && bp != null) {
                        onColorSelectListener.onColorSelect(color=bp.getPixel((int) xx, (int) yy));
                    }
                    System.out.println("ACTION_MOVE xx="+xx+" yy="+yy);
                    break;
                case MotionEvent.ACTION_UP:
                    invalidate();
                    if (onColorSelectListener != null && bp != null) {
                        onColorSelectListener.onStopSelect(bp.getPixel((int) xx, (int) yy));
                    }
                    System.out.println("ACTION_UP xx="+xx+" yy="+yy);
                    break;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        invalidate();
    }

    private boolean inCircle(float x, float y) {

        return  bp.getWidth()>x&&bp.getHeight()>y;
    }

    public interface OnColorSelectListener {
        void onStartSelect(int color);

        void onColorSelect(int color);

        void onStopSelect(int color);
    }

    public OnColorSelectListener getOnColorSelectListener() {
        return onColorSelectListener;
    }

    public void setOnColorSelectListener(OnColorSelectListener onColorSelectListener) {
        this.onColorSelectListener = onColorSelectListener;
    }

    /**
     * 回收Bitmap内存
     */
    public void recycle() {
        if (bp != null) {
            if (!bp.isRecycled()) {
                bp.recycle();
            }
            bp = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public boolean isRecycled() {
        return bp == null || bp.isRecycled();
    }
}
