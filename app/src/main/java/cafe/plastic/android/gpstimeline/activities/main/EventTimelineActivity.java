package cafe.plastic.android.gpstimeline.activities.main;

import android.arch.lifecycle.ViewModelProviders;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.mapbox.mapboxsdk.Mapbox;

import cafe.plastic.android.gpstimeline.R;
import cafe.plastic.android.gpstimeline.activities.BaseViewModel;
import cafe.plastic.android.gpstimeline.data.GPSRecord;
import cafe.plastic.android.gpstimeline.tools.ActivityTools;

public class EventTimelineActivity extends AppCompatActivity {
    EventTimelineViewModel mViewModel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getApplicationContext(), getString(R.string.mapbox_access_token));
        mViewModel = ViewModelProviders.of(this).get(EventTimelineViewModel.class);
        setContentView(R.layout.activity_single_fragment);
        ActivityTools.replaceFragment(this, R.id.fragment_one, new EventTimelineFragment());
        FloatingActionButton fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewModel.addRecord(new GPSRecord());
            }
        });
    }


}
