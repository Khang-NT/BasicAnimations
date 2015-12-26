package com.hello.animation;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    View animateView;
    ViewGroup container;
    AppCompatSpinner animationType, interpolator;

    AppCompatButton start;
    SnowEffect snowEffect;

    int position = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        animateView = findViewById(R.id.tvAnimation);
        container = (ViewGroup) findViewById(R.id.llContainer);
        animationType = (AppCompatSpinner) findViewById(R.id.spinnerAnimationType);
        interpolator = (AppCompatSpinner) findViewById(R.id.spinnerInterpolator);
        start = (AppCompatButton) findViewById(R.id.bStart);
        snowEffect = (SnowEffect) findViewById(R.id.snowEffect);

        animateView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            boolean removedListener = false;

            @Override
            public void onGlobalLayout() {
                if (removedListener) return;
                removedListener = true;

                LayoutTransition layoutTransition = new LayoutTransition();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
                }

                layoutTransition.setDuration(1000);

                ObjectAnimator add = ObjectAnimator.ofFloat(null, "translationX", -(animateView.getLeft() + animateView.getWidth()), 0f).
                        setDuration(1000);
                ObjectAnimator remove = ObjectAnimator.ofFloat(null, "translationX", 0f, (animateView.getRight() + animateView.getWidth())).
                        setDuration(1000);
                layoutTransition.setAnimator(LayoutTransition.APPEARING, add);
                layoutTransition.setAnimator(LayoutTransition.DISAPPEARING, remove);

                container.setLayoutTransition(layoutTransition);
            }
        });

        List<String> animationTypes = new ArrayList<>();
        animationTypes.add("Alpha"); //0
        animationTypes.add("Slide Up"); //1
        animationTypes.add("Slide Down"); //2
        animationTypes.add("Rotation"); //3
        animationTypes.add("Scale"); //4
        animationTypes.add("Set"); //5

        List<String> interpolators = new ArrayList<>();
        interpolators.add("LinearInterpolator"); //0
        interpolators.add("AccelerateInterpolator"); //1
        interpolators.add("AnticipateInterpolator"); //2
        interpolators.add("OvershootInterpolator"); //3
        interpolators.add("BounceInterpolator"); //4

        ArrayAdapter<String> animationTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, animationTypes);
        animationTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<String> interpolatorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, interpolators);
        interpolatorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        animationType.setAdapter(animationTypeAdapter);
        interpolator.setAdapter(interpolatorAdapter);

        start.setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_remove_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                position++;
                container.addView(cloneATextView(), position);
                break;
            case R.id.menu_remove:
                if (position == 0)
                    break;
                container.removeViewAt(position);
                position--;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private View cloneATextView() {
        return LayoutInflater.from(this).inflate(R.layout.text_view_animation, container, false);
    }

    @Override
    public void onClick(View v) {
        start.setEnabled(false);

        final Interpolator timeInterpolator;
        switch (interpolator.getSelectedItemPosition()) {
            case 0:
                timeInterpolator = new LinearInterpolator();
                break;
            case 1:
                timeInterpolator = new AccelerateInterpolator();
                break;
            case 2:
                timeInterpolator = new AnticipateInterpolator();
                break;
            case 3:
                timeInterpolator = new OvershootInterpolator();
                break;
            default:
                timeInterpolator = new BounceInterpolator();
        }


        final Animation animation;
        switch (animationType.getSelectedItemPosition()) {
            case 0:
                animation = AnimationUtils.loadAnimation(this, R.anim.alpha);
                break;
            case 1:
                animation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                break;
            case 2:
                animation = AnimationUtils.loadAnimation(this, R.anim.slide_down);
                break;
            case 3:
                animation = AnimationUtils.loadAnimation(this, R.anim.rotation);
                break;
            case 4:
                animation = AnimationUtils.loadAnimation(this, R.anim.scale);
                break;
            default: //Animation set
                TranslateAnimation translateAnimation = new TranslateAnimation(
                        Animation.ABSOLUTE, 0f, Animation.ABSOLUTE, -animateView.getLeft(),
                        Animation.RELATIVE_TO_SELF, 0f, Animation.ABSOLUTE, -animateView.getTop()
                );
                translateAnimation.setDuration(1000);
                AnimationSet animationSet = new AnimationSet(true);
                RotateAnimation rotateAnimation = new RotateAnimation(0, 360,
                        Animation.ABSOLUTE, animateView.getWidth() / 2 - animateView.getLeft(),
                        Animation.ABSOLUTE, animateView.getHeight() / 2 - animateView.getTop());
                rotateAnimation.setDuration(1000);
                rotateAnimation.setStartOffset(1000);

                animationSet.addAnimation(translateAnimation);
                animationSet.addAnimation(rotateAnimation);

                animation = animationSet;
        }
        if (animationType.getSelectedItemPosition() != 5)
            animation.setDuration(2000);
        animation.setInterpolator(timeInterpolator);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                start.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animateView.startAnimation(animation);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        snowEffect.passGesture(event);
        return super.onTouchEvent(event);
    }
}
