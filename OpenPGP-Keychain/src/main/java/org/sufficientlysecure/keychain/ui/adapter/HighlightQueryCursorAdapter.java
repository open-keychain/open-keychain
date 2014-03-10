package org.sufficientlysecure.keychain.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.CursorAdapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HighlightQueryCursorAdapter extends CursorAdapter {

    private String mCurQuery;

    public HighlightQueryCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mCurQuery = null;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return null;
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {

    }

    public void setSearchQuery(String searchQuery){
        mCurQuery = searchQuery;
    }

    public String getSearchQuery(){
        return mCurQuery;
    }

    protected Spannable highlightSearchKey(String text) {
        Spannable  highlight;
        Pattern pattern;
        Matcher matcher;

        highlight  = Spannable.Factory.getInstance().newSpannable(text);;
        pattern = Pattern.compile("(?i)" + mCurQuery);
        matcher = pattern.matcher(text);
        if (matcher.find()) {
            highlight.setSpan(
                    new ForegroundColorSpan(0xFF33B5E5),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return highlight;
    }
}
