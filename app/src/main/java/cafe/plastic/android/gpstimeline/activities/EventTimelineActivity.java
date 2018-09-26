package cafe.plastic.android.gpstimeline.activities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import cafe.plastic.android.gpstimeline.R;
import cafe.plastic.android.gpstimeline.tools.ActivityTools;

public class EventTimelineActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        ActivityTools.replaceFragment(this, R.id.fragment_one, new EventTimelineFragment());
    }
}
