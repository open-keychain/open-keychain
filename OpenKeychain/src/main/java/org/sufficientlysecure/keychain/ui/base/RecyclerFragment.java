/*
 * Copyright (C) 2013 The Android Open Source Project
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

package org.sufficientlysecure.keychain.ui.base;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import timber.log.Timber;


/*
 * Implementation of taken from the sourcecode of
 * android.support.v4.app.ListFragment from the
 * Android Open Source Project and changed to use
 * RecyclerView instead of ListView.
 */
public class RecyclerFragment<A extends RecyclerView.Adapter> extends Fragment {
    protected static final int INTERNAL_LIST_VIEW_ID = android.R.id.list;
    protected static final int INTERNAL_EMPTY_VIEW_ID = android.R.id.empty;
    protected static final int INTERNAL_LIST_CONTAINER_ID = android.R.id.widget_frame;
    protected static final int INTERNAL_PROGRESS_CONTAINER_ID = android.R.id.progress;

    private final Handler handler = new Handler();
    private final Runnable requestFocus = new Runnable() {
        @Override
        public void run() {
            listView.focusableViewAvailable(listView);
        }
    };

    private boolean observerRegistered = false;
    private final RecyclerView.AdapterDataObserver dataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            checkDataSet();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            checkDataSet();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            checkDataSet();
        }
    };

    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            RecyclerFragment.this.onScrolled(dx, dy);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            RecyclerFragment.this.onScrollStateChanged(newState);
        }
    };

    private A adapter;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView listView;
    private View emptyView;
    private View progressContainer;
    private View listContainer;
    private boolean listShown;

    public RecyclerFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {

        final Context context = getContext();
        FrameLayout root = new FrameLayout(context);

        LinearLayout progressContainer = new LinearLayout(context);
        progressContainer.setId(INTERNAL_PROGRESS_CONTAINER_ID);
        progressContainer.setOrientation(LinearLayout.VERTICAL);
        progressContainer.setGravity(Gravity.CENTER);
        progressContainer.setVisibility(View.GONE);

        ProgressBar progressBar = new ProgressBar(context, null,
                android.R.attr.progressBarStyleLarge);

        progressContainer.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(progressContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        FrameLayout listContainer = new FrameLayout(context);
        listContainer.setId(INTERNAL_LIST_CONTAINER_ID);

        TextView textView = new TextView(context);
        textView.setId(INTERNAL_EMPTY_VIEW_ID);
        textView.setGravity(Gravity.CENTER);

        listContainer.addView(textView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        RecyclerView listView = new RecyclerView(context);
        listView.setId(INTERNAL_LIST_VIEW_ID);

        int padding = FormattingUtils.dpToPx(context, 8);
        listView.setPadding(padding, 0, padding, 0);
        listView.setClipToPadding(false);

        listContainer.addView(listView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(listContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(requestFocus);
        listView.setLayoutManager(null);
        listView.removeOnScrollListener(scrollListener);

        listView = null;
        listShown = false;
        listContainer = null;
        layoutManager = null;
        emptyView = null;
        progressContainer = null;

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        //setAdapter(null);
        super.onDestroy();
    }

    public Handler getHandler() {
        return handler;
    }

    public void onScrollStateChanged(int state) {
        // empty body
    }

    public void onScrolled(int dx, int dy) {
        // empty body
    }

    public void setAdapter(A adapter) {
        unregisterObserver();

        boolean hadAdapter = this.adapter != null;
        this.adapter = adapter;

        registerObserver();

        if (listView != null) {
            listView.setAdapter(adapter);

            if (!listShown && !hadAdapter) {
                if (getView() != null)
                    setListShown(true, getView().getWindowToken() != null);
            }
        }
    }

    public void setLayoutManager(RecyclerView.LayoutManager manager) {
        if (!manager.isAttachedToWindow()) {
            layoutManager = manager;

            if (listView != null) {
                listView.setLayoutManager(manager);
            }
        }
    }

    public int getItemCount() {
        if (adapter != null)
            return adapter.getItemCount();
        else
            return 0;
    }

    public long getItemId(int position) {
        if (adapter != null)
            return adapter.getItemId(position);
        else
            return View.NO_ID;
    }

    public RecyclerView getRecyclerView() {
        ensureList();
        return listView;
    }

    public RecyclerView.LayoutManager getLayoutManager() {
        ensureList();
        return layoutManager;
    }

    private void registerObserver() {
        if (!observerRegistered && adapter != null) {
            adapter.registerAdapterDataObserver(dataObserver);
            observerRegistered = true;
        }
    }

    private void unregisterObserver() {
        if (observerRegistered && adapter != null) {
            adapter.unregisterAdapterDataObserver(dataObserver);
            observerRegistered = false;
        }
    }

    private void checkDataSet() {
        boolean empty = treatAsEmpty(getItemCount());

        Timber.d("Dataset change detected! Count: "
                + getItemCount() + ", Empty: " + empty);

        if (emptyView != null) {
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Set whether the data set of the recycler view should be treated as empty.
     * This is useful e.g. if you have an empty padding row and therefore the item
     * count is always greater than 0.
     *
     * @param itemCount the number of items in the data set.
     * @return Whether to treat this as an empty set of data
     */
    protected boolean treatAsEmpty(int itemCount) {
        return itemCount < 1;
    }

    /**
     * Set whether the recycler view should have a fixed size or not
     */
    protected boolean isFixedSize() {
        return true;
    }

    public void hideList(boolean animated) {
        setListShown(false, animated);
    }

    public void showList(boolean animated) {
        setListShown(true, animated);
    }

    public void setEmptyText(String text) {
        ensureList();
        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(text);
        } else {
            Timber.e("Cannot set empty text on a view that is null" +
                    "or not an instance of android.view.View!");
        }
    }

    private void setListShown(boolean shown, boolean animated) {
        ensureList();

        if (progressContainer == null)
            throw new IllegalStateException("Can't be used with a custom content view");

        if (listShown == shown)
            return;

        listShown = shown;
        if (shown) {
            if (animated) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
                listContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
            } else {
                progressContainer.clearAnimation();
                listContainer.clearAnimation();
            }
            progressContainer.setVisibility(View.GONE);
            listContainer.setVisibility(View.VISIBLE);
        } else {
            if (animated) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_in));
                listContainer.startAnimation(AnimationUtils.loadAnimation(
                        getActivity(), android.R.anim.fade_out));
            } else {
                progressContainer.clearAnimation();
                listContainer.clearAnimation();
            }
            progressContainer.setVisibility(View.VISIBLE);
            listContainer.setVisibility(View.GONE);
        }
    }

    public A getAdapter() {
        return adapter;
    }

    @SuppressWarnings("unchecked")
    private void ensureList() {
        if (listView != null) {
            return;
        }

        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }

        if (root instanceof RecyclerView) {
            listView = (RecyclerView) root;
        } else {
            emptyView = root.findViewById(INTERNAL_EMPTY_VIEW_ID);
            if (emptyView != null) {
                emptyView.setVisibility(View.GONE);
            }

            progressContainer = root.findViewById(INTERNAL_PROGRESS_CONTAINER_ID);
            listContainer = root.findViewById(INTERNAL_LIST_CONTAINER_ID);

            View rawListView = root.findViewById(INTERNAL_LIST_VIEW_ID);
            if (!(rawListView instanceof RecyclerView)) {
                if (rawListView == null) {
                    throw new RuntimeException(
                            "Your content must have a RecyclerView whose id attribute is " +
                                    "'android.R.id.list'");
                }
                throw new RuntimeException(
                        "Content has view with id attribute 'android.R.id.list' "
                                + "that is not a ListView class");
            }

            listView = (RecyclerView) rawListView;
        }

        if (layoutManager != null) {
            RecyclerView.LayoutManager manager = layoutManager;
            this.layoutManager = null;
            setLayoutManager(manager);
        }

        listShown = true;
        listView.setHasFixedSize(isFixedSize());
        listView.addOnScrollListener(scrollListener);

        if (this.adapter != null) {
            A adapter = this.adapter;
            this.adapter = null;
            setAdapter(adapter);
        } else {
            // We are starting without an adapter, so assume we won't
            // have our data right away and start with the progress indicator.
            if (progressContainer != null) {
                setListShown(false, false);
            }
        }

        handler.post(requestFocus);
    }
}
