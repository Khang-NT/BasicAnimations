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
 * Created on 25/12/2015.
 * @author KHANG NT
 */
public class SnowEffect extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "SnowEffect";
    private static final int UPDATE_INTERVAL_DEFAULT = 50;
    private int snowColor = 0xffffffff, numSnowObjects = 20;
    private Paint paint;
    private List<SnowObject> snowObjects = new ArrayList<>();
    private Random random = new Random();

    private boolean show;
    private final SurfaceHolder holder;

    private ThreadLoop paintThread;

    private float minRadius, maxRadius, minFallSpeed, maxFallSpeed, windSpeed, windSpeedRange;
    private float lastX = -1;
    private int updateInterval, numFPS;

    /**
     * ----------------------------Constructor----------------------------
     */

    public SnowEffect(Context context) {
        this(context, null);
    }

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
                numFPS = ta.getInt(R.styleable.SnowEffect_numFPS, -1);
                if (numFPS == -1) {
                    numFPS = 1000 / UPDATE_INTERVAL_DEFAULT;
                    updateInterval = UPDATE_INTERVAL_DEFAULT;
                } else {
                    updateInterval = 1000 / numFPS;
                }
                ta.recycle();
            }
        } else {
            minRadius = dipToPixels(1.5f);
            maxRadius = dipToPixels(4.5f);
            windSpeed = random.nextInt(1) == 0 ? 1 : -1;
            updateInterval = 1000 / numFPS;
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(snowColor);


        paintThread = new PaintThread(updateInterval);


        holder = getHolder();
        holder.setFormat(PixelFormat.TRANSPARENT);

        if (!isInEditMode()) {
            setZOrderOnTop(true);
            holder.addCallback(this);
        }
    }

    /**----------------------------Control functions----------------------------*/

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

    /**
     * Gesture swipe to change wind speed.
     *
     * @param event event data
     */
    public void passGesture(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN)
            lastX = event.getX();
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
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

    /**----------------------------Set - get methods.----------------------------*/

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

    public void setNumFPS(int numFPS) {
        this.numFPS = numFPS;
        updateInterval = 1000 / numFPS;
        paintThread.setUpdateInterval(updateInterval);
        calculateFallSpeedRange();
    }

    public int getNumFPS() {
        return numFPS;
    }

    /**----------------------------Private functions----------------------------*/

    private float dipToPixels(float dipValue) {
        DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }

    private void calculateFallSpeedRange() {
        minFallSpeed = (getHeight() / 20000f) * updateInterval;
        maxFallSpeed = (getHeight() / 12000f) * updateInterval;
    }

    private void prepareSnowObjects() {
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


    /**----------------------------Surface callback functions----------------------------*/


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        calculateFallSpeedRange();
        windSpeedRange = maxFallSpeed;
        prepareSnowObjects();
        show();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        calculateFallSpeedRange();
        windSpeedRange = maxFallSpeed;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }


    /**----------------------------Private inner class----------------------------*/


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

        public void setUpdateInterval(int updateInterval) {
            this.updateInterval = updateInterval;
        }
    }
}
