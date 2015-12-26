package com.hello.animation;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Khang on 25/12/2015.
 */
public class SnowEffect extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "SnowEffect";
    private static final int UPDATE_INTERVAL = 70;
    private static final float MAX_SPEED = 8f, MIN_SPEED = 3.5f, MIN_WIND_SPEED = -4.5f, MAX_WIND_SPEED = 4.5f;
    private int snowColor = 0xffffffff, numSnowObjects = 20;
    private Paint paint;
    private List<SnowObject> snowObjects = new ArrayList<>();
    private Random random = new Random();

    private boolean show;
    private final SurfaceHolder holder;

    private ThreadLoop paintThread;

    private float minRadius, maxRadius, minFallSpeed, maxFallSpeed, windSpeed, windSpeedRange;
    private float lastX = -1;

    public SnowEffect(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        if (attributeSet != null) {
            TypedArray ta = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.SnowEffect, 0, 0);
            if (ta != null) {
                snowColor = ta.getColor(R.styleable.SnowEffect_snowColor, 0xffffffff);
                numSnowObjects = ta.getInt(R.styleable.SnowEffect_numSnowObjects, 20);
                minRadius = ta.getDimension(R.styleable.SnowEffect_minRadius,
                        dipToPixels(1.5f));
                maxRadius = ta.getDimension(R.styleable.SnowEffect_maxRadius,
                        dipToPixels(4.5f));
                windSpeed = ta.getFloat(R.styleable.SnowEffect_windSpeed, random.nextInt(1) == 0 ? 1 : -1);
                ta.recycle();
            }
        } else {
            minRadius = dipToPixels(1.5f);
            maxRadius = dipToPixels(4.5f);
            windSpeed = random.nextInt(1) == 0 ? 1 : -1;
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(snowColor);



        paintThread = new PaintThread(UPDATE_INTERVAL);


        holder = getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);

        if (!isInEditMode()) {
            setZOrderOnTop(true);
            holder.addCallback(this);
        }
    }

    public void show(){
        this.show = true;
        if (!paintThread.isRunning())
            paintThread.start();
    }

    public void pause(){
        this.show = false;
        if (paintThread.isRunning())
            paintThread.stopAndJoin();
    }

    public boolean isShowAnimation() {
        return show;
    }

    public int getSnowColor() {
        return snowColor;
    }

    public void setSnowColor(int snowColor) {
        this.snowColor = snowColor;
        paint.setColor(snowColor);
    }

    public int getNumSnowObjects() {
        return numSnowObjects;
    }

    public void setNumSnowObjects(int numSnowObjects) {
        this.numSnowObjects = numSnowObjects;
        prepareSnowObjects();
    }

    private float dipToPixels(float dipValue) {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    public void passGesture(MotionEvent event){
        if (event.getAction() == MotionEvent.ACTION_DOWN)
            lastX = event.getX();
        else if (event.getAction() == MotionEvent.ACTION_MOVE){
            float offset = (event.getX() - lastX) / 50f;

            lastX = event.getX();
            if (windSpeed + offset > windSpeedRange)
                windSpeed = windSpeedRange;
            else if (windSpeed + offset < -windSpeedRange)
                windSpeed = -windSpeedRange;
            else
                windSpeed += offset;
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            for (SnowObject snowObject : snowObjects) {
                paint.setAlpha(snowObject.alpha);
                canvas.drawCircle(snowObject.x, snowObject.y, snowObject.radius, paint);
            }
        }
    }

    private void prepareSnowObjects(){
        if (snowObjects.size() < numSnowObjects) {
            int width = getWidth();
            while (snowObjects.size() < numSnowObjects) {
                snowObjects.add(
                        new SnowObject(random.nextFloat() * (maxFallSpeed - minFallSpeed) + minFallSpeed,
                                random.nextFloat() * (maxRadius - minRadius) + minRadius,
                                random.nextInt(200) + 55,
                                random.nextInt(width), 0,
                                random.nextInt(1500)
                        )
                );
            }
        }

        while (snowObjects.size() > numSnowObjects) {
            snowObjects.remove(snowObjects.size() - 1);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        minFallSpeed = (getHeight() / 15000f) * UPDATE_INTERVAL;
        maxFallSpeed = (getHeight() / 8000f) * UPDATE_INTERVAL;
        windSpeedRange = maxFallSpeed;
        prepareSnowObjects();
        show();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        minFallSpeed = (height / 15000f) * UPDATE_INTERVAL;
        maxFallSpeed = (height / 8000f) * UPDATE_INTERVAL;
        windSpeedRange = maxFallSpeed;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    private class SnowObject {
        float fallSpeed;
        float radius;
        int alpha;
        float x, y;
        int startOffset;

        public SnowObject(float fallSpeed, float radius, int alpha, float x, float y, int startOffset) {
            this.fallSpeed = fallSpeed;
            this.radius = radius;
            this.alpha = alpha;
            this.x = x;
            this.y = y;
            this.startOffset = startOffset;
        }
    }

    private class PaintThread extends ThreadLoop {

        public PaintThread(int updateInterval) {
            super(updateInterval);
        }

        @Override
        public void run() {
            Canvas canvas = null;
            while (!reqStop){
                //long drawTime = SystemClock.currentThreadTimeMillis();
                try {
                    canvas = holder.lockCanvas();
                    synchronized (holder){
                        draw(canvas);//onDraw(canvas);
                    }
                } finally {
                    if (canvas != null)
                        holder.unlockCanvasAndPost(canvas);
                }

                for (SnowObject snowObject : snowObjects) {
                    if (snowObject.startOffset > 0)
                        snowObject.startOffset -= updateInterval;
                    else {
                        if (snowObject.y > getHeight() || snowObject.x > getWidth()){
                            snowObject.x = random.nextInt(getWidth());
                            snowObject.y = 0;
                            snowObject.fallSpeed = random.nextFloat() * (maxFallSpeed - minFallSpeed) + minFallSpeed;
                            snowObject.alpha = random.nextInt(200) + 55;
                            snowObject.startOffset = random.nextInt(1500);
                            snowObject.radius = random.nextFloat() * (maxRadius - minRadius) + minRadius;
                        } else {
                            snowObject.y += snowObject.fallSpeed;
                            snowObject.x += windSpeed;
                        }
                    }
                }

                //drawTime = SystemClock.currentThreadTimeMillis() - drawTime;
                try {
                    //if (updateInterval - drawTime > 0)
                    Thread.sleep(updateInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

//    private class CalculateThread extends ThreadLoop {
//
//        public CalculateThread(int updateInterval) {
//            super(updateInterval);
//        }
//
//        @Override
//        public void run() {
//            while (!reqStop){
//                try {
//                    Thread.sleep(updateInterval);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                long calculateTime = SystemClock.currentThreadTimeMillis();
//                for (SnowObject snowObject : snowObjects) {
//                    if (snowObject.startOffset > 0)
//                        snowObject.startOffset -= updateInterval;
//                    else {
//                        if (snowObject.y > getHeight() || snowObject.x > getWidth()){
//                            snowObject.x = random.nextInt(getWidth());
//                            snowObject.y = 0;
//                            snowObject.fallSpeed = random.nextFloat()*2.5f + 2.5f;
//                            snowObject.alpha = random.nextInt(256);
//                            snowObject.startOffset = random.nextInt(1500);
//                            snowObject.radius = random.nextFloat()*7f + 4;
//                        } else {
//                            snowObject.y += snowObject.fallSpeed;
//                            snowObject.x += windSpeed;
//                        }
//                    }
//                }
//                calculateTime = SystemClock.currentThreadTimeMillis() - calculateTime;
//                Log.e(TAG, "run: calculate time: " + calculateTime );
//            }
//        }
//    }

    private abstract class ThreadLoop implements Runnable {
        protected Thread thread;
        protected boolean reqStop = true;
        protected int updateInterval;

        public ThreadLoop(int updateInterval) {
            this.updateInterval = updateInterval;
        }

        public void start(){
            reqStop = false;
            thread = new Thread(this);
            thread.start();
        }

        public void stopAndJoin(){
            reqStop = true;
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean isRunning() {
            return !reqStop;
        }
    }
}
