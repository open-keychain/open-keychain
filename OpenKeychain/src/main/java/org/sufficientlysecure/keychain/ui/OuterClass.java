package org.sufficientlysecure.keychain.ui;


import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.ui.util.SomeInterface;
import org.sufficientlysecure.keychain.ui.OuterClass.InnerStaticClass.ViewModel;

public class OuterClass {

    public static class InnerStaticClass extends RecyclerView.Adapter<OtherInnerStaticClass> {

        public class ViewModel {
            ViewModel() {
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public InnerStaticClass() {
        }

        // Create new views (invoked by the layout manager)
        @Override
        public OtherInnerStaticClass onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(OtherInnerStaticClass holder, final int position) {

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return 0;
        }

    }

    public static class OtherInnerStaticClass extends RecyclerView.ViewHolder implements SomeInterface {

        public OtherInnerStaticClass() {
            super(null);
        }

    }

}
