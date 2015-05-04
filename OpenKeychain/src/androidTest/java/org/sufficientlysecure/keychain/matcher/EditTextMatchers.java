/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.matcher;

import android.content.Context;
import android.text.method.TransformationMethod;
import android.view.View;
import android.widget.EditText;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class EditTextMatchers {

    public static TypeSafeMatcher<View> withError(final int errorResId) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(View view) {
                Context context = view.getContext();

                if (view instanceof EditText) {
                    CharSequence error = ((EditText) view).getError();
                    return error != null && error.equals(context.getString(errorResId));
                }

                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("EditText with error");
            }

        };
    }

    public static TypeSafeMatcher<View> withTransformationMethod(final Class<? extends TransformationMethod> transformationClass) {
        return new TypeSafeMatcher<View>() {

            @Override
            public boolean matchesSafely(View view) {
                if (view instanceof EditText) {
                    TransformationMethod transformation = ((EditText) view).getTransformationMethod();
                    return transformation != null && transformationClass.isInstance(transformation);
                }

                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("EditText with transformation method");
            }

        };
    }

}
