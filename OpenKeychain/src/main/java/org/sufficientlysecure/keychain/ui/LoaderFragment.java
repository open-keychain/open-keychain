/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import org.sufficientlysecure.keychain.R;

/**
 * This is a fragment helper class, which implements a generic
 * progressbar/container view.
 * <p/>
 * To use it in a fragment, simply subclass, use onCreateView to create the
 * layout's root view, and ues getContainer() as root view of your subclass.
 * The layout shows a progress bar by default, and can be switched to the
 * actual contents by calling setContentShown().
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
