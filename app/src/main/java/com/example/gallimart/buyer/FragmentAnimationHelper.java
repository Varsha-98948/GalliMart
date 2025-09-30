package com.example.gallimart.buyer;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;

import com.example.gallimart.R;

public class FragmentAnimationHelper {

    public enum AnimationType {
        FLY_IN, FADE_IN
    }

    // Apply animation to a view
    public static void applyAnimation(@NonNull Context context, @NonNull View view, @NonNull AnimationType type) {
        Animation anim = null;

        switch (type) {
            case FLY_IN:
                anim = AnimationUtils.loadAnimation(context, R.anim.fly_in);
                break;
            case FADE_IN:
                anim = AnimationUtils.loadAnimation(context, R.anim.fade_in);
                break;
        }

        if (anim != null) view.startAnimation(anim);
    }
}
