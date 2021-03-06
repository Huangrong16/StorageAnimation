package com.huangr.storageanimation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/*****************************************
 * 水波纹效果
 *
 * @author huangr
 *
 ****************************************/
public class WaterView extends View {

    private static final String TAG = "WaterView";
    private boolean isShowFrame;
    private int mWidth;
    private int mHeight;
    private float centerX;
    private float centerY;

    private Path canvasPath = new Path();
    private Path firstPath = new Path();
    private Paint firstPaint;
    private Path secondPath = new Path();
    private Paint secondPaint;
    private Paint framePaint;

    private float sin_cycle = 0.01f;//周期 ， 0.01f左右
    float sin_offset = 0.0f;//初项，偏移量
    float h = 0f;

    private int secondPaintColor = Color.parseColor("#9292DA");
    private int firstPaintColor = Color.parseColor("#5353C7");
    private int frameColor = Color.parseColor("#27AFED");
    private float frameWidth;
    private int sin_amplitude = 30;//振幅 ，10到100之间
    private float sin_offset_increment_value = 0.4f;//初项递增值，表示波浪的快慢
    private int sin_up_velocity = 5;//上升速度，参考值3
    private int sleep_time = 100; //休眠时间，参考值100
    private float keep_percent = 0.5f; //保持高度百分比
    private boolean isStart = true;
    private boolean isStop = false;
    private boolean isKeepHeight = false;

    private int type;
    public static final int TYPE_CIRCLE = 1;
    public static final int TYPE_RECT = 2;
    private int color[] = new int[2];   //渐变颜色

    private List<Bubble> bubbles = new ArrayList<Bubble>();
    private Random random = new Random();
    private boolean starting = false;
    private boolean isPause = false;
    private boolean isShowBubble = true;
    private int isShowBubbleCount = 0;
    private boolean isCustomSpeedY = false;
    //private boolean hasShowBubble = false;

    public WaterView(Context context) {
        this(context, null);
    }

    public WaterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

//        初始化值
        secondPaintColor = Color.parseColor("#9292DA");
        firstPaintColor = Color.parseColor("#5353C7");
        sin_amplitude = 20;//振幅 ，10到100之间
        sin_offset_increment_value = 0.4f;//初项递增值，表示波浪的快慢
        sin_up_velocity = 5;//上升速度，参考值3
        sleep_time = 150; //休眠时间，参考值100
        frameColor = Color.parseColor("#27AFED");
        //圆环渐变的颜色
        color[0] = Color.parseColor("#27AFED");
        color[1] = Color.parseColor("#4FD8F7");
        frameWidth = dip2px(context, 7);
        type = TYPE_CIRCLE;
        isShowFrame = false;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.WaterView);
        firstPaintColor = typedArray.getColor(R.styleable.WaterView_waterview_paint_color_first, firstPaintColor);
        secondPaintColor = typedArray.getColor(R.styleable.WaterView_waterview_paint_color_second, secondPaintColor);
        sin_amplitude = typedArray.getInt(R.styleable.WaterView_waterview_amplitude, sin_amplitude);
        sin_offset_increment_value = typedArray.getFloat(R.styleable.WaterView_waterview_offset_increment_value, sin_offset_increment_value);
        sin_up_velocity = typedArray.getInt(R.styleable.WaterView_waterview_up_velocity, sin_up_velocity);
        sleep_time = typedArray.getInt(R.styleable.WaterView_waterview_sleep_time, sleep_time);
        frameWidth = typedArray.getDimension(R.styleable.WaterView_waterview_frame_width, frameWidth);
        frameColor = typedArray.getColor(R.styleable.WaterView_waterview_frame_color, frameColor);
        isShowFrame = typedArray.getBoolean(R.styleable.WaterView_waterview_frame_color, false);
        typedArray.recycle();

        if (frameWidth == 0) isShowFrame = false;

        firstPaint = new Paint();
        firstPaint.setColor(firstPaintColor);
        firstPaint.setAntiAlias(true);
        secondPaint = new Paint();
        secondPaint.setColor(secondPaintColor);
        secondPaint.setAntiAlias(true);
        framePaint = new Paint();
        framePaint.setStrokeWidth(frameWidth);
        framePaint.setAntiAlias(true);
        //framePaint.setColor(frameColor);
        framePaint.setStyle(Paint.Style.STROKE );

    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.h = mHeight;
        canvasPath.addCircle(centerX, centerY, mHeight / 2 , Path.Direction.CCW);
        reset();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        /**
         * 设置宽度
         * 单位 px
         */
        int specMode = MeasureSpec.getMode(widthMeasureSpec);
        int specSize = MeasureSpec.getSize(widthMeasureSpec);

        if (specMode == MeasureSpec.EXACTLY)// match_parent , accurate
        {
            mWidth = specSize;
        } else {
            mWidth = dip2px(getContext(), 208);
        }

        /***
         * 设置高度
         */
        specMode = MeasureSpec.getMode(heightMeasureSpec);
        specSize = MeasureSpec.getSize(heightMeasureSpec);
        if (specMode == MeasureSpec.EXACTLY)// match_parent , accurate
        {
            mHeight = specSize;
        } else {
            mHeight = dip2px(getContext(), 208);
        }
        centerX = mWidth / 2;
        centerY = mHeight / 2;

        setMeasuredDimension(mWidth, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.clipPath(canvasPath, Region.Op.INTERSECT);
        if (isStart) {
            canvas.drawPath(secondPath(), secondPaint);
            canvas.drawPath(firstPath(), firstPaint);
        }

        if (isShowFrame){
            framePaint.setShader(new LinearGradient(mWidth/2, mHeight, mWidth/2, 0, color[0], color[1], Shader.TileMode.CLAMP));
            canvas.drawCircle(centerX, centerY, mHeight / 2 - frameWidth/2, framePaint);
        }

        if(isStart && isShowBubble /*&& !hasShowBubble*/){
            drawBubble(canvas);
            //hasShowBubble = true;
            isShowBubbleCount++;
            if(25 == isShowBubbleCount){
                isShowBubbleCount = 0;
            }
        }
    }

    private void drawBubble(Canvas canvas) {
        isPause = false;
        /*if (!starting) {
            starting = true;
        }*/
        new Thread() {
            public void run() {
                Log.d(TAG, "气泡 isShowBubbleCount = " + isShowBubbleCount);
                if(isPause || isShowBubbleCount != 1){
                    return;
                }
                /*try {
                    Thread.sleep(random.nextInt(10) * 300);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }*/
                Log.d(TAG, "气泡展示 isShowBubbleCount = " + isShowBubbleCount);
                Bubble bubble = new Bubble();
                int radius = random.nextInt(10) + 10;
                while (radius == 0) {
                    radius = random.nextInt(10) + 10;
                }
                //float speedY = 3;
                float speedY = random.nextFloat()*5;
                while (speedY < 1) {
                    speedY = random.nextFloat()*5;
                }
                /*if(isCustomSpeedY){
                    speedY = 3;
                }*/
                bubble.setRadius(radius);
                bubble.setSpeedY(speedY);
                bubble.setX(mWidth / 2);
                //开始的位置在顶部
                //bubble.setY(0);
                bubble.setY(mHeight - frameWidth*2);

                float speedX = random.nextFloat()-0.5f;
                while (speedX == 0) {
                    speedX = random.nextFloat()-0.5f;
                }
                bubble.setSpeedX(speedX*2);
                //bubbles.clear();
                bubbles.add(bubble);
                /*while (true) {
                }*/
            };
        }.start();

        Paint paint = new Paint();

        //背景透明
        paint.setColor(Color.TRANSPARENT);
        canvas.drawRect(0, 0, mWidth, mHeight, paint);
        paint.reset();
        paint.setColor(Color.WHITE);
        paint.setAlpha(200);
        List<Bubble> list = new ArrayList<Bubble>(bubbles);

        for (Bubble bubble : list) {
            if (bubble.getY() + bubble.getSpeedY() < 0) {
                bubbles.remove(bubble);
            } else {
                int i = bubbles.indexOf(bubble);
                if (bubble.getX() + bubble.getSpeedX() <= bubble.getRadius()) {
                    bubble.setX(bubble.getRadius());
                } else if (bubble.getX() + bubble.getSpeedX() >= mWidth - bubble.getRadius()) {
                    bubble.setX(mWidth  - bubble.getRadius());
                } else {
                    bubble.setX(bubble.getX() + bubble.getSpeedX());
                }
                bubble.setY(bubble.getY() - bubble.getSpeedY());
                bubbles.set(i, bubble);
                canvas.drawCircle(bubble.getX(), bubble.getY(),
                        bubble.getRadius(), paint);
            }
            /*if (bubble.getY() + bubble.getSpeedY() > mHeight) {
                bubbles.remove(bubble);
            } else {
                int i = bubbles.indexOf(bubble);
                if (bubble.getX() + bubble.getSpeedX() <= bubble.getRadius()) {
                    bubble.setX(bubble.getRadius());
                } else if (bubble.getX() + bubble.getSpeedX() >= mWidth
                        - bubble.getRadius()) {
                    bubble.setX(mWidth - bubble.getRadius());
                } else {
                    bubble.setX(bubble.getX() + bubble.getSpeedX());
                }
                bubble.setY(bubble.getY() + bubble.getSpeedY());
                bubbles.set(i, bubble);
                canvas.drawCircle(bubble.getX(), bubble.getY(),
                        bubble.getRadius(), paint);
            }*/
        }
    }

    //y = Asin(wx+b)+h ，这个公式里：w影响周期，A影响振幅，h影响y位置，b为初相；
    private Path firstPath() {
        firstPath.reset();
        firstPath.moveTo(0, mHeight);// 移动到左下角的点

        for (float x = 0; x <= mWidth; x++) {
            float y = (float) (sin_amplitude * Math.sin(sin_cycle * x + sin_offset + 40)) + h;
            firstPath.lineTo(x, y);
        }
        firstPath.lineTo(mWidth, mHeight);
        firstPath.lineTo(0, mHeight);
        firstPath.close();
        return firstPath;
    }

    private Path secondPath() {
        secondPath.reset();
        secondPath.moveTo(0, mHeight);// 移动到左下角的点

        for (float x = 0; x <= mWidth; x++) {
            float y = (float) (sin_amplitude * Math.sin(sin_cycle * x + sin_offset)) + h;
            secondPath.lineTo(x, y);
        }
        secondPath.lineTo(mWidth, mHeight);
        secondPath.lineTo(0, mHeight);
        secondPath.close();
        return secondPath;
    }

    @Override
    public void postInvalidate() {
        // TODO Auto-generated method stub
        super.postInvalidate();
        isPause = true;
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void reset() {
        if (runThread != null) {
            runThread = null;
        }
        //isStart = false;
        isStop = false;
        isKeepHeight = false;
        h = mHeight;
        sin_offset = 0;
        invalidate();
    }

    private RunThread runThread;

    public void start() {
        isStart = true;
        runThread = new RunThread();
        runThread.start();
        isCustomSpeedY = true;
        /*if (!isStart) {
        }*/
    }

    public void stop() {
        isStop = true;
        isKeepHeight = false;
    }

    public void recover() {
        isStop = false;
    }

    public void keepHeight() {
        isStop = true;
        isKeepHeight = true;
        isCustomSpeedY = false;
    }

    public void setKeepPercent(float keep_percent) {
        this.keep_percent = keep_percent;
    }

    private class RunThread extends Thread {

        @Override
        public void run() {
            while (isStart) {

                if (isStop) {
                    if (isKeepHeight) {
                        try {
                            Thread.sleep(sleep_time);
                            sin_offset += sin_offset_increment_value;
                            postInvalidate();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                try {
                    Thread.sleep(sleep_time);
                    h -= sin_up_velocity;
                    Log.d(TAG, "run() called h = " + h);
                    sin_offset += sin_offset_increment_value;
                    if(h < mHeight * (1 - keep_percent)){
                        keepHeight();
                    }
                    postInvalidate();
                    if (h + sin_amplitude < 0) {
                        if (listener != null) {
                            WaterView.this.post(new Runnable() {
                                @Override
                                public void run() {
                                    listener.finish();
                                }
                            });
                        }
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    Listener listener;

    public interface Listener {
        void finish();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private class Bubble {
        /** 气泡半径 */
        private int radius;
        /** 上升速度 */
        private float speedY;
        /** 平移速度 */
        private float speedX;
        /** 气泡x坐标 */
        private float x;
        /** 气泡y坐标 */
        private float y;

        /**
         * @return the radius
         */
        public int getRadius() {
            return radius;
        }

        /**
         * @param radius
         *            the radius to set
         */
        public void setRadius(int radius) {
            this.radius = radius;
        }

        /**
         * @return the x
         */
        public float getX() {
            return x;
        }

        /**
         * @param x
         *            the x to set
         */
        public void setX(float x) {
            this.x = x;
        }

        /**
         * @return the y
         */
        public float getY() {
            return y;
        }

        /**
         * @param y
         *            the y to set
         */
        public void setY(float y) {
            this.y = y;
        }

        /**
         * @return the speedY
         */
        public float getSpeedY() {
            return speedY;
        }

        /**
         * @param speedY
         *            the speedY to set
         */
        public void setSpeedY(float speedY) {
            this.speedY = speedY;
        }

        /**
         * @return the speedX
         */
        public float getSpeedX() {
            return speedX;
        }

        /**
         * @param speedX
         *            the speedX to set
         */
        public void setSpeedX(float speedX) {
            this.speedX = speedX;
        }

    }
}

