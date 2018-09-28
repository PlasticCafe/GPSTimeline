package cafe.plastic.android.gpstimeline.data;

import android.databinding.ObservableList;
import android.support.v7.widget.RecyclerView;

import java.lang.ref.WeakReference;

public class GPSObservableListCallback extends ObservableList.OnListChangedCallback<ObservableList<GPSRecord>> {
    private WeakReference<RecyclerView.Adapter> mRecylcerViewAdapter;
    public GPSObservableListCallback(RecyclerView.Adapter adapter) {
        mRecylcerViewAdapter = new WeakReference<>(adapter);
    }
    @Override
    public void onChanged(ObservableList<GPSRecord> sender) {
        RecyclerView.Adapter adapter = mRecylcerViewAdapter.get();
        if(adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemRangeChanged(ObservableList<GPSRecord> sender, int positionStart, int itemCount) {

    }

    @Override
    public void onItemRangeInserted(ObservableList<GPSRecord> sender, int positionStart, int itemCount) {

    }

    @Override
    public void onItemRangeMoved(ObservableList<GPSRecord> sender, int fromPosition, int toPosition, int itemCount) {

    }

    @Override
    public void onItemRangeRemoved(ObservableList<GPSRecord> sender, int positionStart, int itemCount) {

    }
}
