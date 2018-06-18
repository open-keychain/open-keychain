package org.sufficientlysecure.keychain.model;


import java.util.Date;

import android.support.annotation.NonNull;

import com.squareup.sqldelight.ColumnAdapter;


public final class CustomColumnAdapters {

    private CustomColumnAdapters() { }

    static final ColumnAdapter<Date,Long> DATE_ADAPTER = new ColumnAdapter<Date,Long>() {
        @NonNull
        @Override
        public Date decode(Long databaseValue) {
            // Both SQLite and OpenPGP prefer a second granularity for timestamps - so we'll translate here
            return new Date(databaseValue * 1000);
        }

        @Override
        public Long encode(@NonNull Date value) {
            return value.getTime() / 1000;
        }
    };
}
