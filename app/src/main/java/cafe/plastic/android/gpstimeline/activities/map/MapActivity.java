package cafe.plastic.android.gpstimeline.activities.map;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import java.util.UUID;

import cafe.plastic.android.gpstimeline.R;
import cafe.plastic.android.gpstimeline.tools.ActivityTools;

public class MapActivity extends AppCompatActivity {
    private MapViewModel mViewModel;

    public static Intent getIntent(Context context, UUID id) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra(MapFragment.ARGUMENT_GPS_RECORD_UUID, id);
        return intent;
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(MapViewModel.class);
        setContentView(R.layout.activity_single_fragment);
        ActivityTools.replaceFragment(this, R.id.fragment_one, MapFragment.newInstance((UUID)getIntent().getSerializableExtra(MapFragment.ARGUMENT_GPS_RECORD_UUID)));
    }
}

