package ru.ytkab0bp.beamklipper.view;

import android.annotation.SuppressLint;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleRecyclerAdapter extends RecyclerView.Adapter {
    private Map<Class<?>, Integer> viewType = new HashMap<>();
    private Map<Integer, SimpleRecyclerItem> viewCreator = new HashMap<>();
    private int lastType;

    private List<SimpleRecyclerItem> items = new ArrayList<>();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(viewCreator.get(viewType).onCreateView(parent.getContext())) {};
    }

    /** @noinspection unchecked*/
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        items.get(position).onBindView(holder.itemView);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).hashCode();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(List<SimpleRecyclerItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Integer t = viewType.get(items.get(position).getClass());
        if (t == null) {
            viewType.put(items.get(position).getClass(), t = lastType++);
            viewCreator.put(t, items.get(position));
        }
        return t;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<SimpleRecyclerItem> getItems() {
        return items;
    }
}
