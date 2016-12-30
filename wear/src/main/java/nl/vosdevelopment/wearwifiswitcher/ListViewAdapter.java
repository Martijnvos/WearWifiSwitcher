package nl.vosdevelopment.wearwifiswitcher;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

final class ListViewAdapter extends WearableListView.Adapter {
    private String[] mDataset;
    private final LayoutInflater mInflater;

    // Provide a suitable constructor (depends on the kind of dataset)
    public ListViewAdapter(Context context, String[] dataset) {
        mInflater = LayoutInflater.from(context);
        mDataset = dataset;
    }

    // Provide a reference to the type of views you're using
    public static class ItemViewHolder extends WearableListView.ViewHolder {
        private TextView wifiNameTextView;
        public ItemViewHolder(View itemView) {
            super(itemView);
            // find the text view within the custom item's layout
            wifiNameTextView = (TextView) itemView.findViewById(R.id.wifi_name);
        }
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // Inflate our custom layout for list items
        return new ItemViewHolder(mInflater.inflate(R.layout.list_item, null));
    }

    // Instead of creating new views, the list tries to recycle existing ones
    // (invoked by the WearableListView's layout manager)
    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder,
                                 int position) {
        // retrieve the text view
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        TextView view = itemHolder.wifiNameTextView;
        // replace text contents
        view.setText(mDataset[position]);
        // replace list item's metadata
        holder.itemView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
