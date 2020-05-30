package org.sufficientlysecure.keychain;


import android.content.ContentValues;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;


public class TestHelpers {
    public static ContentValues cvContains(ContentValues value) {
        ArgumentMatcher<ContentValues> baseMatcher = new ArgumentMatcher<ContentValues>() {
            @Override
            public boolean matches(ContentValues item) {
                ContentValues cv = (ContentValues) item;
                for (String key : value.keySet()) {
                    if (!cv.containsKey(key)) {
                        return false;
                    }

                    Object ours = value.get(key);
                    Object theirs = cv.get(key);
                    if (ours == null && theirs == null) {
                        continue;
                    }
                    if (ours == null || !ours.equals(theirs)) {
                        return false;
                    }
                }
                return true;
            }
        };
        return Matchers.argThat(baseMatcher);
    }
}
