package org.sufficientlysecure.materialchips.adapter;


import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import android.widget.Filter;
import android.widget.Filterable;

import org.sufficientlysecure.materialchips.adapter.FilterableAdapter.FilterableItem;

public abstract class FilterableAdapter<T extends FilterableItem, VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements Filterable {
    private List<T> displayedList = new ArrayList<>();
    private List<T> hiddenItemsList = new ArrayList<>();
    private ItemFilter itemFilter;

    public FilterableAdapter(List<? extends T> itemList) {
        itemFilter = new ItemFilter(itemList);
        displayedList.addAll(itemList);

        setHasStableIds(true);
    }

    @Override
    public int getItemCount() {
        return displayedList.size();
    }

    public T getItem(int position) {
        return displayedList.get(position);
    }

    @Override
    public Filter getFilter() {
        return itemFilter;
    }

    @Override
    public long getItemId(int position) {
        FilterableItem item = getItem(position);
        return item != null ? item.getId() : RecyclerView.NO_ID;
    }

    private class ItemFilter extends Filter {
        private List<T> originalList;
        private List<T> filteredList;

        ItemFilter(List<? extends T> chipList) {
            super();
            this.originalList = new ArrayList<>(chipList);
            this.filteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            filteredList.clear();
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(originalList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (T item : originalList) {
                    if (item.isKeptForConstraint(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            FilterableAdapter.this.displayedList.clear();
            FilterableAdapter.this.displayedList.addAll((ArrayList<T>) results.values);
            notifyDataSetChanged();
        }
    }

    public void hideItem(T item) {
        if (!hiddenItemsList.contains(item)) {
            hiddenItemsList.add(item);
        }
        notifyDataSetChanged();
    }

    public void unhideItem(T item) {
        hiddenItemsList.remove(item);
        notifyDataSetChanged();
    }

    public interface FilterableItem {
        long getId();
        boolean isKeptForConstraint(CharSequence constraint);
    }
}
