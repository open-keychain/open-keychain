/*
 * Copyright 2012 Evgeny Shishkin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.devspark.appmsg;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * In-layout notifications. Based on {@link android.widget.Toast} notifications
 * and article by Cyril Mottier (http://android.cyrilmottier.com/?p=773).
 *
 * @author e.shishkin
 */
public class AppMsg {

    /**
     * Show the view or text notification for a short period of time. This time
     * could be user-definable. This is the default.
     *
     * @see #setDuration
     */
    public static final int LENGTH_SHORT = 3000;

    /**
     * Show the view or text notification for a long period of time. This time
     * could be user-definable.
     *
     * @see #setDuration
     */
    public static final int LENGTH_LONG = 5000;

    /**
     * <p>Show the view or text notification for an undefined amount of time
     * -Usually until an invocation of {@link #cancel()}, {@link #cancelAll(android.app.Activity)},
     * {@link #cancelAll()} or {@link android.app.Activity#onDestroy()}-,
     * stacking on top of any other {@link com.devspark.appmsg.AppMsg} with this duration.</p>
     *
     * <p><b>Note</b>: You are responsible
     * for calling {@link #cancel()} on such {@link com.devspark.appmsg.AppMsg}.</p>
     *
     * @see #setDuration
     */
    public static final int LENGTH_STICKY = -1;

    /**
     * Lowest priority, messages with this priority will be showed after all messages with priority
     * {@link #PRIORITY_HIGH} and {@link #PRIORITY_NORMAL} have been shown.
     *
     * @see #setPriority(int)
     */
    public static final int PRIORITY_LOW = Integer.MIN_VALUE;
    /**
     * Normal priority, messages with this priority will be showed after all messages with priority
     * {@link #PRIORITY_HIGH} but before {@link #PRIORITY_LOW} have been shown.
     *
     * @see #setPriority(int)
     */
    public static final int PRIORITY_NORMAL = 0;
    /**
     * Highest priority, messages with this priority will be showed before any other message.
     *
     * @see #setPriority(int)
     */
    public static final int PRIORITY_HIGH = Integer.MAX_VALUE;

    /**
     * Show the text notification for a long period of time with a negative style.
     */
    public static final Style STYLE_ALERT = new Style(LENGTH_LONG, R.color.alert);

    /**
     * Show the text notification for a short period of time with a positive style.
     */
    public static final Style STYLE_CONFIRM = new Style(LENGTH_SHORT, R.color.confirm);

    /**
     * Show the text notification for a short period of time with a neutral style.
     */
    public static final Style STYLE_INFO = new Style(LENGTH_SHORT, R.color.info);

    private final Activity mActivity;
    private int mDuration = LENGTH_SHORT;
    private View mView;
    private ViewGroup mParent;
    private LayoutParams mLayoutParams;
    private boolean mFloating;
    Animation mInAnimation, mOutAnimation;
    int mPriority = PRIORITY_NORMAL;

    /**
     * Construct an empty AppMsg object. You must call {@link #setView} before
     * you can call {@link #show}.
     *
     * @param activity {@link android.app.Activity} to use.
     */
    public AppMsg(Activity activity) {
        mActivity =  activity;
    }

    /**
     * Make a {@link AppMsg} that just contains a text view.
     *
     * @param context The context to use. Usually your
     *                {@link android.app.Activity} object.
     * @param text    The text to show. Can be formatted text.
     * @param style   The style with a background and a duration.
     */
    public static AppMsg makeText(Activity context, CharSequence text, Style style) {
        return makeText(context, text, style, R.layout.app_msg);
    }
    
    /**
     * @author mengguoqiang 扩展支持设置字体大小
     * Make a {@link AppMsg} that just contains a text view.
     *
     * @param context The context to use. Usually your
     *                {@link android.app.Activity} object.
     * @param text    The text to show. Can be formatted text.
     * @param style   The style with a background and a duration.
     */
    public static AppMsg makeText(Activity context, CharSequence text, Style style, float textSize) {
        return makeText(context, text, style, R.layout.app_msg, textSize);
    }

    /**
     * Make a {@link AppMsg} with a custom layout. The layout must have a {@link TextView} com id {@link android.R.id.message}
     *
     * @param context The context to use. Usually your
     *                {@link android.app.Activity} object.
     * @param text    The text to show. Can be formatted text.
     * @param style   The style with a background and a duration.
     */
    public static AppMsg makeText(Activity context, CharSequence text, Style style, int layoutId) {
        LayoutInflater inflate = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(layoutId, null);

        return makeText(context, text, style, v, true);
    }
    
    /**
     * @author mengguoqiang 扩展支持字体大小
     * Make a {@link AppMsg} with a custom layout. The layout must have a {@link TextView} com id {@link android.R.id.message}
     *
     * @param context The context to use. Usually your
     *                {@link android.app.Activity} object.
     * @param text    The text to show. Can be formatted text.
     * @param style   The style with a background and a duration.
     */
    public static AppMsg makeText(Activity context, CharSequence text, Style style, int layoutId, float textSize) {
        LayoutInflater inflate = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflate.inflate(layoutId, null);

        return makeText(context, text, style, v, true, textSize);
    }

    /**
     * Make a non-floating {@link AppMsg} with a custom view presented inside the layout.
     * It can be used to create non-floating notifications if floating is false.
     *
     * @param context  The context to use. Usually your
     *                 {@link android.app.Activity} object.
     * @param customView
     *                 View to be used.
     * @param text     The text to show. Can be formatted text.
     * @param style    The style with a background and a duration.
     */
    public static AppMsg makeText(Activity context, CharSequence text, Style style, View customView) {
       return makeText(context, text, style, customView, false);
    }

    /**
     * Make a {@link AppMsg} with a custom view. It can be used to create non-floating notifications if floating is false.
     *
     * @param context  The context to use. Usually your
     *                 {@link android.app.Activity} object.
     * @param view
     *                 View to be used.
     * @param text     The text to show. Can be formatted text.
     * @param style    The style with a background and a duration.
     * @param floating true if it'll float.
     */
    private static AppMsg makeText(Activity context, CharSequence text, Style style, View view, boolean floating) {
        return makeText(context, text, style, view, floating, 0);
    }
    
    /**
     * 
     * @author mengguoqiang 扩展支持设置字体大小
     * Make a {@link AppMsg} with a custom view. It can be used to create non-floating notifications if floating is false.
     *
     * @param context  The context to use. Usually your
     *                 {@link android.app.Activity} object.
     * @param view
     *                 View to be used.
     * @param text     The text to show. Can be formatted text.
     * @param style    The style with a background and a duration.
     * @param floating true if it'll float.
     */
    private static AppMsg makeText(Activity context, CharSequence text, Style style, View view, boolean floating, float textSize) {
        AppMsg result = new AppMsg(context);

        view.setBackgroundResource(style.background);

        TextView tv = (TextView) view.findViewById(android.R.id.message);
        if(textSize > 0) tv.setTextSize(textSize);
        tv.setText(text);

        result.mView = view;
        result.mDuration = style.duration;
        result.mFloating = floating;

        return result;
    }

    /**
     * Make a {@link AppMsg} with a custom view. It can be used to create non-floating notifications if floating is false.
     *
     * @param context  The context to use. Usually your
     *                 {@link android.app.Activity} object.
     * @param resId    The resource id of the string resource to use. Can be
     *                 formatted text.
     * @param style    The style with a background and a duration.
     * @param floating true if it'll float.
     */
    public static AppMsg makeText(Activity context, int resId, Style style, View customView, boolean floating) {
        return makeText(context, context.getResources().getText(resId), style, customView, floating);
    }

    /**
     * Make a {@link AppMsg} that just contains a text view with the text from a
     * resource.
     *
     * @param context The context to use. Usually your
     *                {@link android.app.Activity} object.
     * @param resId   The resource id of the string resource to use. Can be
     *                formatted text.
     * @param style   The style with a background and a duration.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static AppMsg makeText(Activity context, int resId, Style style)
            throws Resources.NotFoundException {
        return makeText(context, context.getResources().getText(resId), style);
    }

    /**
     * Make a {@link AppMsg} with a custom layout using the text from a
     * resource. The layout must have a {@link TextView} com id {@link android.R.id.message}
     *
     * @param context The context to use. Usually your
     *                {@link android.app.Activity} object.
     * @param resId   The resource id of the string resource to use. Can be
     *                formatted text.
     * @param style   The style with a background and a duration.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static AppMsg makeText(Activity context, int resId, Style style, int layoutId)
            throws Resources.NotFoundException {
        return makeText(context, context.getResources().getText(resId), style, layoutId);
    }

    /**
     * Show the view for the specified duration.
     */
    public void show() {
        MsgManager manager = MsgManager.obtain(mActivity);
        manager.add(this);
    }

    /**
     * @return <code>true</code> if the {@link AppMsg} is being displayed, else <code>false</code>.
     */
    public boolean isShowing() {
        if (mFloating) {
            return mView != null && mView.getParent() != null;
        } else {
            return mView.getVisibility() == View.VISIBLE;
        }
    }

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet.
     * You do not normally have to call this.  Normally view will disappear on its own
     * after the appropriate duration.
     */
    public void cancel() {
        MsgManager.obtain(mActivity).clearMsg(this);

    }

    /**
     * Cancels all queued {@link AppMsg}s, in all Activities. If there is a {@link AppMsg}
     * displayed currently, it will be the last one displayed.
     */
    public static void cancelAll() {
        MsgManager.clearAll();
    }

    /**
     * Cancels all queued {@link AppMsg}s, in given {@link android.app.Activity}.
     * If there is a {@link AppMsg} displayed currently, it will be the last one displayed.
     * @param activity
     */
    public static void cancelAll(Activity activity) {
        MsgManager.release(activity);
    }

    /**
     * Return the activity.
     */
    public Activity getActivity() {
        return mActivity;
    }

    /**
     * Set the view to show.
     *
     * @see #getView
     */
    public void setView(View view) {
        mView = view;
    }

    /**
     * Return the view.
     *
     * @see #setView
     */
    public View getView() {
        return mView;
    }

    /**
     * Set how long to show the view for.
     *
     * @see #LENGTH_SHORT
     * @see #LENGTH_LONG
     */
    public void setDuration(int duration) {
        mDuration = duration;
    }

    /**
     * Return the duration.
     *
     * @see #setDuration
     */
    public int getDuration() {
        return mDuration;
    }

    /**
     * Update the text in a AppMsg that was previously created using one of the makeText() methods.
     *
     * @param resId The new text for the AppMsg.
     */
    public void setText(int resId) {
        setText(mActivity.getText(resId));
    }

    /**
     * Update the text in a AppMsg that was previously created using one of the makeText() methods.
     *
     * @param s The new text for the AppMsg.
     */
    public void setText(CharSequence s) {
        if (mView == null) {
            throw new RuntimeException("This AppMsg was not created with AppMsg.makeText()");
        }
        TextView tv = (TextView) mView.findViewById(android.R.id.message);
        if (tv == null) {
            throw new RuntimeException("This AppMsg was not created with AppMsg.makeText()");
        }
        tv.setText(s);
    }

    /**
     * Gets the crouton's layout parameters, constructing a default if necessary.
     *
     * @return the layout parameters
     */
    public LayoutParams getLayoutParams() {
        if (mLayoutParams == null) {
            mLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        return mLayoutParams;
    }

    /**
     * Sets the layout parameters which will be used to display the crouton.
     *
     * @param layoutParams The layout parameters to use.
     * @return <code>this</code>, for chaining.
     */
    public AppMsg setLayoutParams(LayoutParams layoutParams) {
        mLayoutParams = layoutParams;
        return this;
    }

    /**
     * Constructs and sets the layout parameters to have some gravity.
     *
     * @param gravity the gravity of the Crouton
     * @return <code>this</code>, for chaining.
     * @see android.view.Gravity
     */
    public AppMsg setLayoutGravity(int gravity) {
        mLayoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, gravity);
        return this;
    }

    /**
     * Return the value of floating.
     *
     * @see #setFloating(boolean)
     */
    public boolean isFloating() {
        return mFloating;
    }

    /**
     * Sets the value of floating.
     *
     * @param mFloating
     */
    public void setFloating(boolean mFloating) {
        this.mFloating = mFloating;
    }

    /**
     * Sets the Animations to be used when displaying/removing the Crouton.
     * @param inAnimation the Animation resource ID to be used when displaying.
     * @param outAnimation the Animation resource ID to be used when removing.
     */
    public AppMsg setAnimation(int inAnimation, int outAnimation) {
        return setAnimation(AnimationUtils.loadAnimation(mActivity, inAnimation),
                AnimationUtils.loadAnimation(mActivity, outAnimation));
    }

    /**
     * Sets the Animations to be used when displaying/removing the Crouton.
     * @param inAnimation the Animation to be used when displaying.
     * @param outAnimation the Animation to be used when removing.
     */
    public AppMsg setAnimation(Animation inAnimation, Animation outAnimation) {
        mInAnimation = inAnimation;
        mOutAnimation = outAnimation;
        return this;
    }

    /**
     * @return
     * Current priority
     *
     * @see #PRIORITY_HIGH
     * @see #PRIORITY_NORMAL
     * @see #PRIORITY_LOW
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * <p>Set priority for this message</p>
     * <p><b>Note</b>: This only affects the order in which the messages get shown,
     * not the stacking order of the views.</p>
     *
     * <p>Example: In the queue there are 3 messages [A, B, C],
     * all of them with priority {@link #PRIORITY_NORMAL}, currently message A is being shown
     * so we add a new message D with priority {@link #PRIORITY_HIGH}, after A goes away, given that
     * D has a higher priority than B an the reset, D will be shown, then once that D is gone,
     * B will be shown, and then finally C.</p>
     *
     * @param priority
     * A value indicating priority, although you can use any integer value, usage of already
     * defined is highly encouraged.
     *
     * @see #PRIORITY_HIGH
     * @see #PRIORITY_NORMAL
     * @see #PRIORITY_LOW
     */
    public void setPriority(int priority) {
        mPriority = priority;
    }

    /**
     * @return
     * Provided parent to add {@link #getView()} to using {@link #getLayoutParams()}.
     */
    public ViewGroup getParent() {
        return mParent;
    }

    /**
     * Provide a different parent than Activity decor view
     * @param parent
     * Provided parent to add {@link #getView()} to using {@link #getLayoutParams()}.
     *
     */
    public void setParent(ViewGroup parent) {
        mParent = parent;
    }

    /**
     * Provide a different parent than Activity decor view
     *
     * @param parentId
     * Provided parent id to add {@link #getView()} to using {@link #getLayoutParams()}.
     *
     */
    public void setParent(int parentId) {
        setParent((ViewGroup) mActivity.findViewById(parentId));
    }

    /**
     * The style for a {@link AppMsg}.
     *
     * @author e.shishkin
     */
    public static class Style {

        private final int duration;
        private final int background;

        /**
         * Construct an {@link AppMsg.Style} object.
         *
         * @param duration How long to display the message. Either
         *                 {@link #LENGTH_SHORT} or {@link #LENGTH_LONG}
         * @param resId    resource for AppMsg background
         */
        public Style(int duration, int resId) {
            this.duration = duration;
            this.background = resId;
        }

        /**
         * Return the duration in milliseconds.
         */
        public int getDuration() {
            return duration;
        }

        /**
         * Return the resource id of background.
         */
        public int getBackground() {
            return background;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof AppMsg.Style)) {
                return false;
            }
            Style style = (Style) o;
            return style.duration == duration
                    && style.background == background;
        }

    }

}
