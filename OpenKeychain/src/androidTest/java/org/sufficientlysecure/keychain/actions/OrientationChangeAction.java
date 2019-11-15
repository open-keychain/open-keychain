package org.sufficientlysecure.keychain.actions;


import java.util.Collection;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ActivityInfo;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import android.view.View;

import org.hamcrest.Matcher;

import static androidx.test.espresso.matcher.ViewMatchers.isRoot;

public class OrientationChangeAction implements ViewAction {
    private final int orientation;

    private OrientationChangeAction(int orientation) {
        this.orientation = orientation;
    }

    @Override
    public Matcher<View> getConstraints() {
        return isRoot();
    }

    @Override
    public String getDescription() {
        return "change orientation to " + orientation;
    }

    @Override
    public void perform(UiController uiController, View view) {
        uiController.loopMainThreadUntilIdle();

        final Activity activity = findActivity(view.getContext());
        if (activity == null){
            throw new IllegalStateException("Could not find the current activity");
        }

        activity.setRequestedOrientation(orientation);

        Collection<Activity> resumedActivities = ActivityLifecycleMonitorRegistry
                .getInstance().getActivitiesInStage(Stage.RESUMED);

        if (resumedActivities.isEmpty()) {
            throw new RuntimeException("Could not change orientation");
        }
    }

    private static Activity findActivity(Context context) {
        if (context == null)
            return null;
        else if (context instanceof Activity)
            return (Activity) context;
        else if (context instanceof ContextWrapper)
            return findActivity(((ContextWrapper) context).getBaseContext());

        return null;
    }

    public static ViewAction orientationLandscape() {
        return new OrientationChangeAction(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public static ViewAction orientationPortrait() {
        return new OrientationChangeAction(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
}