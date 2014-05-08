package org.sufficientlysecure.keychain.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import org.sufficientlysecure.keychain.R;

/** This is a fragment helper class, which implements a generic
 * progressbar/container view.
 *
 * To use it in a fragment, simply subclass, use onCreateView to create the
 * layout's root view, and ues getContainer() as root view of your subclass.
 * The layout shows a progress bar by default, and can be switched to the
 * actual contents by calling setContentShown().
 * 
 */
public class LoaderFragment extends Fragment {
    private boolean mContentShown;
    private View mProgressContainer;
    private ViewGroup mContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.loader_layout, container, false);

        mContentShown = true;
        mContainer = (ViewGroup) root.findViewById(R.id.loader_container);
        mProgressContainer = root.findViewById(R.id.loader_progress);

        // content is not shown (by visibility statuses in the layout files)
        mContentShown = false;

        return root;

    }

    protected ViewGroup getContainer() {
        return mContainer;
    }

    public void setContentShown(boolean shown, boolean animate) {
        if (mContentShown == shown) {
            return;
        }
        mContentShown = shown;
        if (shown) {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                mContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            }
            mProgressContainer.setVisibility(View.GONE);
            mContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                mContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            }
            mProgressContainer.setVisibility(View.VISIBLE);
            mContainer.setVisibility(View.INVISIBLE);
        }
    }

    public void setContentShown(boolean shown) {
        setContentShown(shown, true);
    }

    public void setContentShownNoAnimation(boolean shown) {
        setContentShown(shown, false);
    }
}
