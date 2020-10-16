package com.example.mynotes;

import android.app.Activity;
import android.content.Intent;

public class ThemeUtils {

    private static int currentTheme;
    public final static int theme1 = 0;
    public final static int theme2 = 1;

    public static void setCurrentTheme(Activity activity, int currentTheme) {
        activity.finish();
        activity.startActivity(new Intent(activity, activity.getClass()));
        ThemeUtils.currentTheme = currentTheme;
    }

    public static void onActivityCreateSetTheme(Activity activity){
        switch (currentTheme){
            default:
            case theme1:
                activity.setTheme(R.style.AppTheme);
                break;
            case theme2:
                activity.setTheme(R.style.ThemeTwo);
                break;
        }
    }



}//END OF public class
