package cafe.plastic.android.gpstimeline.activities.map;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.UUID;

import cafe.plastic.android.gpstimeline.R;
import cafe.plastic.android.gpstimeline.data.GPSRecord;

public class MapFragment extends Fragment {
    public static final String TAG = MapFragment.class.getSimpleName();
    public static final String ARGUMENT_GPS_RECORD_UUID = "GPS_RECORD_UUID";
    private MapViewModel mViewModel;
    private MapView mMapView;
    public static MapFragment newInstance(UUID id) {
        MapFragment frag = new MapFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ARGUMENT_GPS_RECORD_UUID, id);
        frag.setArguments(bundle);
        return frag;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = ViewModelProviders.of(getActivity()).get(MapViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map_display, container, false);
        mMapView = v.findViewById(R.id.mapView);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(getArguments() == null) {
            getActivity().finish(); //Nothing to show!
            return;
        }

        UUID id = (UUID)getArguments().getSerializable(ARGUMENT_GPS_RECORD_UUID);
        mViewModel.getGPSRecord(id).observe(getActivity(), new Observer<GPSRecord>() {
            @Override
            public void onChanged(@Nullable GPSRecord gpsRecord) {
                Log.d(TAG, "Got record: " + gpsRecord.getId());
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(gpsRecord.getLat(), gpsRecord.getLon()))
                        .zoom(10)
                        .build();
                mMapView.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(MapboxMap mapboxMap) {
                        mapboxMap.setCameraPosition(position);
                    }
                });
            }
        });

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(mMapView != null) mMapView.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(mMapView != null) mMapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mMapView != null) mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mMapView != null) mMapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mMapView != null) mMapView.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mMapView != null) mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mMapView.onDestroy();
    }
}
