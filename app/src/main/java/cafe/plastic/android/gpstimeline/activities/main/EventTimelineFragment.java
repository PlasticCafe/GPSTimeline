package cafe.plastic.android.gpstimeline.activities.main;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import cafe.plastic.android.gpstimeline.R;
import cafe.plastic.android.gpstimeline.activities.BaseViewModel;
import cafe.plastic.android.gpstimeline.activities.map.MapActivity;
import cafe.plastic.android.gpstimeline.data.GPSRecord;
import cafe.plastic.android.gpstimeline.services.LocationLoggerService;
import cafe.plastic.android.gpstimeline.tools.DateFormatTools;


public class EventTimelineFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private Button mButton;
    private EventTimelineViewModel viewModel;

    private class GPSRecordHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private GPSRecord mRecord;

        public GPSRecordHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.timeline_item, parent, false));
            itemView.setOnClickListener(this);
        }

        public void bind(GPSRecord record) {
            mRecord = record;
            TextView tv_uuid = itemView.findViewById(R.id.tv_uuid);
            TextView tv_date = itemView.findViewById(R.id.tv_date);
            TextView tv_lat = itemView.findViewById(R.id.tv_lat);
            TextView tv_lon = itemView.findViewById(R.id.tv_lon);
            tv_lat.setText("Lat: " + record.getLat());
            tv_lon.setText("Lon: " + record.getLon());
            tv_date.setText(DateFormatTools.format("MMM dd hh:mm", record.getTimestamp()));
            tv_uuid.setText(record.getId().toString());

        }

        public GPSRecord getRecord() {
            return mRecord;
        }

        @Override
        public void onClick(View view) {
            Intent intent = MapActivity.getIntent(getActivity(), mRecord.getId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }
    }

    private class GPSRecordAdapter extends RecyclerView.Adapter<GPSRecordHolder> {
        private List<GPSRecord> mRecords = new ArrayList<>();
        GPSRecordAdapter() {
            viewModel.getGPSRecords().observe(getActivity(), new Observer<List<GPSRecord>>() {
                @Override
                public void onChanged(@Nullable List<GPSRecord> gpsRecords) {
                    mRecords.clear();
                    mRecords.addAll(gpsRecords);
                    notifyDataSetChanged();
                }
            });
        }

        @NonNull
        @Override
        public GPSRecordHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new GPSRecordHolder(LayoutInflater.from(getActivity()), viewGroup);
        }

        @Override
        public void onBindViewHolder(@NonNull GPSRecordHolder gpsRecordHolder, int position) {
            gpsRecordHolder.bind(mRecords.get(position));
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
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_event_timeline, container, false);
        mButton = v.findViewById(R.id.button);
        mRecyclerView = v.findViewById(R.id.recycler_view);
        mAdapter = new GPSRecordAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setAdapter(mAdapter);
        new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                return makeMovementFlags(0, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
                GPSRecord swipedRecord = ((GPSRecordHolder)viewHolder).getRecord();
                viewModel.deleteRecord(swipedRecord);
                Log.d("EventTimelineFragment", "swiped");
            }
        }).attachToRecyclerView(mRecyclerView);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loggerService = new Intent(getActivity(), LocationLoggerService.class);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getActivity().startForegroundService(loggerService);
                } else {
                    getActivity().startService(loggerService);
                }
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.getGPSRecords();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_event_timeline, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.get_permissions:
                requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA}, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
