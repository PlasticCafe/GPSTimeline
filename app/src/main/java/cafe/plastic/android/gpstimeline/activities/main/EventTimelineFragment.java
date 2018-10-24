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
import android.support.v7.recyclerview.extensions.AsyncListDiffer;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
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
import android.widget.ImageView;
import android.widget.TextView;


import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import cafe.plastic.android.gpstimeline.R;
import cafe.plastic.android.gpstimeline.activities.BaseViewModel;
import cafe.plastic.android.gpstimeline.activities.map.MapActivity;
import cafe.plastic.android.gpstimeline.data.GPSRecord;
import cafe.plastic.android.gpstimeline.services.LocationLoggerService;
import cafe.plastic.android.gpstimeline.tools.DateFormatTools;


public class EventTimelineFragment extends Fragment {
    private static final String TAG = EventTimelineFragment.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private GPSRecordAdapter mAdapter;
    private Button mButton;
    private EventTimelineViewModel viewModel;


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
        mAdapter = new GPSRecordAdapter(new GPSRecordDiff());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        viewModel.getGPSRecords().observe(this, list -> mAdapter.submitList(list));
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
                GPSRecord swipedRecord = ((GPSRecordHolder) viewHolder).getRecord();
                viewModel.deleteRecord(swipedRecord);
                Log.d("EventTimelineFragment", "swiped");
            }
        }).attachToRecyclerView(mRecyclerView);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loggerService = new Intent(getActivity(), LocationLoggerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        switch (item.getItemId()) {
            case R.id.get_permissions:
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA}, 0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private class GPSRecordHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private GPSRecord mRecord;

        public GPSRecordHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.timeline_item, parent, false));
            itemView.setOnClickListener(this);
        }

        public void bind(GPSRecord record) {
            mRecord = record;
            TextView tv_date = itemView.findViewById(R.id.tv_date);
            TextView tv_lat = itemView.findViewById(R.id.tv_lat);
            TextView tv_lon = itemView.findViewById(R.id.tv_lon);
            ImageView iv_pic = itemView.findViewById(R.id.iv_pic);
            tv_lat.setText("Lat: " + record.getLat());
            tv_lon.setText("Lon: " + record.getLon());
            tv_date.setText(DateFormatTools.format("MMM dd hh:mm", record.getTimestamp()));
            if(record.getPicture()) {
                String path = "file://"+getActivity().getApplicationContext().getFilesDir()+"/"+record.getId().toString()+".jpg";
                Log.d(TAG, "We have a picture at " + path);
                Glide.with(getActivity())
                        .load(path)
                        .thumbnail(0.4f)
                        .into(iv_pic);
            } else {
                Glide.with(getActivity())
                        .load(android.R.drawable.ic_menu_report_image)
                        .into(iv_pic);
            }

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

    private class GPSRecordAdapter extends ListAdapter<GPSRecord, GPSRecordHolder> {
        public GPSRecordAdapter(GPSRecordDiff differ) {
            super(differ);
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public GPSRecordHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new GPSRecordHolder(LayoutInflater.from(getActivity()), viewGroup);
        }

        @Override
        public void onBindViewHolder(@NonNull GPSRecordHolder gpsRecordHolder, int position) {
            gpsRecordHolder.bind(getItem(position));
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId().getLeastSignificantBits();
        }

    }

    public class GPSRecordDiff extends DiffUtil.ItemCallback<GPSRecord> {
        @Override
        public boolean areItemsTheSame(@NonNull GPSRecord oldRec, @NonNull GPSRecord newRec) {
            return oldRec.getId().equals(newRec.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull GPSRecord oldRec, @NonNull GPSRecord newRec) {
            return oldRec.equals(newRec);
        }

        @Nullable
        @Override
        public Object getChangePayload(@NonNull GPSRecord oldItem, @NonNull GPSRecord newItem) {
            return super.getChangePayload(oldItem, newItem);
        }
    }
}
