package cafe.plastic.android.gpstimeline.tools;

import android.location.Location;

import com.google.android.gms.location.LocationListener;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class LocationObserver implements LocationListener {
    private final PublishSubject<Location> currentLocation = PublishSubject.create();

    public Subject<Location> getSubject() {
        return currentLocation;
    }
    @Override
    public void onLocationChanged(Location location) {
        currentLocation.onNext(location);
    }

}
