package com.example.sentrypda;

import androidx.appcompat.app.AppCompatActivity;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.animation.ArgbEvaluator;

import static android.animation.ValueAnimator.REVERSE;

public class MainActivity extends AppCompatActivity {
    Button play1, play2;
    ImageView sapperIcon;
    MediaPlayer mp;
    ValueAnimator colorAnim;
    ObjectAnimator sapperAnim, ammoAnim, sentryHealthAnim;
    ProgressBar ammoBar, sentryHealthBar;
    int shells = 100, sentryHealth = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sapperIcon = (ImageView)findViewById(R.id.imageView2);

        //final Animation animation = (Animation) AnimationUtils.loadAnimation(this, R.anim.sentryhealthanim);
        sentryHealthBar = (ProgressBar) findViewById(R.id.progressBar1);
        sentryHealthBar.setProgress(sentryHealth);

        ammoBar = (ProgressBar) findViewById(R.id.progressBar2);
        ammoBar.setProgress(shells);

        play1 = (Button)findViewById(R.id.button1);
        final MediaPlayer mp1 = MediaPlayer.create(this, R.raw.hud_warning);
        play1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //sentryHealthBar.startAnimation(animation);
                mp1.setLooping(true);
                mp1.start();
                sentryHealthAnim();
                setIconAnimColor();
                sapperIconAnim();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable(){
                    @Override
                    public void run() {
                        mp1.stop();
                        mp1.prepareAsync();
                    }
                }, 6000);
                //sapperIcon.setBackgroundColor(Color.parseColor("#e73520"));
            }
        });

        play2 = (Button)findViewById(R.id.button2);
        final MediaPlayer mp2 = MediaPlayer.create(this, R.raw.sentry_shoot);
        play2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                sentryShellAnim();
                mp2.setLooping(true);
                mp2.start();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mp2.stop();
                        mp2.prepareAsync();
                    }
                }, 3000);
            }
        });
    }

    void setIconAnimColor(){
        int colorOne = Color.parseColor("#e73520");
        int colorTwo = Color.parseColor("#d6cacb");
        colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), colorOne, colorTwo);
        colorAnim.setDuration(500); // milliseconds
        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                sapperIcon.setBackgroundColor((int) animator.getAnimatedValue());
            }

        });
        colorAnim.setRepeatCount(10);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.start();
    }

    void sentryShellAnim(){
        ammoAnim = ObjectAnimator.ofInt(ammoBar, "progress", shells-50);
        ammoAnim.setDuration(3000);
        ammoAnim.start();
        shells -= 50;
    }

    void sapperIconAnim(){
        float ht_px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 113, getResources().getDisplayMetrics());
        sapperAnim = ObjectAnimator.ofFloat(sapperIcon, "translationY", ht_px);
        sapperAnim.setDuration(500);
        sapperAnim.start();
    }

    void sentryHealthAnim(){
        sentryHealthAnim = ObjectAnimator.ofInt(sentryHealthBar, "progress", 0);
        sentryHealthAnim.setDuration(6000);
        sentryHealthAnim.start();
    }
}

