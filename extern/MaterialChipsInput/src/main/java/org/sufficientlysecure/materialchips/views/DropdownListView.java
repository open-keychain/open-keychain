package org.sufficientlysecure.materialchips.views;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.RelativeLayout;

import org.sufficientlysecure.materialchips.R;
import org.sufficientlysecure.materialchips.adapter.FilterableAdapter;
import org.sufficientlysecure.materialchips.util.ViewUtil;


@SuppressLint("ViewConstructor") // this is a dropdown view, it doesn't come up in preview
public class DropdownListView extends RelativeLayout {
    private RecyclerView recyclerView;
    private ViewGroup rootView;

    public DropdownListView(Context context, ViewGroup layout) {
        super(context);
        this.rootView = layout;
        init();
    }

    private void init() {
        View view = inflate(getContext(), R.layout.list_filterable_view, this);

        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        setVisibility(GONE);
    }

    public void build(FilterableAdapter filterableAdapter) {
        recyclerView.setAdapter(filterableAdapter);

        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // size
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                        ViewUtil.getWindowWidth(getContext()),
                        ViewGroup.LayoutParams.WRAP_CONTENT);

                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    layoutParams.bottomMargin = ViewUtil.getNavBarHeight(getContext());
                }

                // If this child view is already added to the parent rootView, then remove it first
                ViewGroup parent = (ViewGroup) DropdownListView.this.getParent();
                if (parent != null) {
                    parent.removeView(DropdownListView.this);
                }
                // add view
                rootView.addView(DropdownListView.this, layoutParams);

                // remove the listener:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }

        });
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void fadeIn() {
        if (getVisibility() == VISIBLE) {
            return;
        }

        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(200);
        startAnimation(anim);
        setVisibility(VISIBLE);
    }

    public void fadeOut() {
        if (getVisibility() == GONE) {
            return;
        }

        AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(200);
        startAnimation(anim);
        setVisibility(GONE);
    }
}
