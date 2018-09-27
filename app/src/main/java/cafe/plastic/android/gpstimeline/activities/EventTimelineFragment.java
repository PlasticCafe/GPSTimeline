package cafe.plastic.android.gpstimeline.activities;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.EventLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cafe.plastic.android.gpstimeline.R;
import cafe.plastic.android.gpstimeline.data.GPSRecord;


public class EventTimelineFragment extends Fragment {

    private List<GPSRecord> mRecords;
    private RecyclerView mRecyclerView;
    private EventTimelineViewModel viewModel;
    private class GPSRecordHolder extends RecyclerView.ViewHolder {
        public GPSRecordHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.timeline_item, parent, false));
        }

        public void bind(int position) {
            TextView tv_uuid = itemView.findViewById(R.id.tv_uuid);
            tv_uuid.setText(mRecords.get(position).getId().toString());

        }
    }

    private class GPSRecordAdapter extends RecyclerView.Adapter<GPSRecordHolder> {

        @NonNull
        @Override
        public GPSRecordHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new GPSRecordHolder(LayoutInflater.from(getActivity()), viewGroup);
        }

        @Override
        public void onBindViewHolder(@NonNull GPSRecordHolder gpsRecordHolder, int position) {
            gpsRecordHolder.bind(position);
        }

        @Override
        public int getItemCount() {
            return mRecords.size();
        }
    }
    public static EventTimelineFragment newInstance() {
        return new EventTimelineFragment();
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(getActivity()).get(EventTimelineViewModel.class);
        mRecords = viewModel.getGPSRecords();

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_event_timeline, container, false);
        mRecyclerView = v.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(new GPSRecordAdapter());
        return v;
    }
}
