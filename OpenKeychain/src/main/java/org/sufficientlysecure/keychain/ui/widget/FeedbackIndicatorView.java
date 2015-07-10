package org.sufficientlysecure.keychain.ui.widget;


import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;

/**
 * View that shows messages to the user in two different colors with the option to also display a
 * wrong/correct icon.
 */
public class FeedbackIndicatorView extends RelativeLayout {
	public static final String STATE_SAVE_TEXT = "STATE_STATE_TEXT";
	public static final String STATE_SAVE_STATE = "STATE_SAVE_STATE";
	public static final String STATE_SAVE_SHOW_IMAGE = "STATE_SAVE_SHOW_IMAGE";

	private ImageView mImageView;
	private TextView mTextView;
	private Context mContext;
	private boolean mState = false; //false - wrong - true correct
	private boolean mShowImage = false;

	public FeedbackIndicatorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		prepareView();
	}

	public FeedbackIndicatorView(Context context) {
		super(context);
		mContext = context;
		prepareView();
	}

	/**
	 * Prepares the view by loading the layout and settings its properties.
	 * The inflated layout is merged together with its parent to increase performance.
	 */
	private void prepareView() {

		ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(mContext).
				inflate(R.layout.layout_feedback_indicator, this, true);

		mImageView = (ImageView) viewGroup.findViewById(R.id.feedbackIndicatorImage);
		mTextView = (TextView) viewGroup.findViewById(R.id.feedbackIndicatorText);

		//hide everything
		mImageView.setVisibility(INVISIBLE);
	}

	/**
	 * Shows a green text message with the option to show its corresponding icon.
	 *
	 * @param text
	 * @param showIcon
	 */
	public void showCorrectTextMessage(CharSequence text, boolean showIcon) {
		mTextView.setText(text);
		mTextView.setTextColor(mContext.getResources().getColor(R.color.android_green_dark));
		mImageView.setVisibility(showIcon ? VISIBLE : INVISIBLE);
		updateImageView(R.drawable.ic_done_black_48dp, R.color.android_green_dark);

		mShowImage = showIcon;
		mState = true;
	}

	private void updateImageView(@DrawableRes int drawableId, int colorTint) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
			mImageView.setImageDrawable(mContext.getResources().getDrawable(drawableId));
		} else {
			mImageView.setImageDrawable(mContext.getDrawable(drawableId));
		}

		DrawableCompat.setTint(mImageView.getDrawable(), mContext.getResources().
				getColor(colorTint));
	}

	/**
	 * Shows a red text message with the option to show its corresponding icon.
	 *
	 * @param text
	 * @param showIcon
	 */
	public void showWrongTextMessage(CharSequence text, boolean showIcon) {
		mTextView.setText(text);
		mTextView.setTextColor(mContext.getResources().getColor(R.color.android_red_dark));
		mImageView.setVisibility(showIcon ? VISIBLE : INVISIBLE);
		updateImageView(R.drawable.ic_close_black_48dp, R.color.android_red_dark);
		mShowImage = showIcon;
		mState = false;
	}

	/**
	 * Hides and message and the icon.
	 */
	public void hideMessageAndIcon() {
		mShowImage = false;
		mState = false;
		mTextView.setText("");
		mImageView.setVisibility(INVISIBLE);
	}

	/**
	 * Save state handling
	 */

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putParcelable("instanceState", super.onSaveInstanceState());
		bundle.putCharSequence(STATE_SAVE_TEXT, mTextView.getText());
		bundle.putBoolean(STATE_SAVE_STATE, mState);
		bundle.putBoolean(STATE_SAVE_SHOW_IMAGE, mShowImage);
		return bundle;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;

			mTextView.setText(bundle.getCharSequence(STATE_SAVE_TEXT));
			mState = bundle.getBoolean(STATE_SAVE_STATE);
			mShowImage = bundle.getBoolean(STATE_SAVE_SHOW_IMAGE);

			bundle.putCharSequence(STATE_SAVE_TEXT, mTextView.getText());
			bundle.putBoolean(STATE_SAVE_STATE, mState);
			bundle.putBoolean(STATE_SAVE_SHOW_IMAGE, mShowImage);
			state = bundle.getParcelable("instanceState");
		}
		super.onRestoreInstanceState(state);
	}
}
