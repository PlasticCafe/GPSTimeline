package cafe.plastic.android.gpstimeline.tools;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

public class ActivityTools {
    public static void replaceFragment(AppCompatActivity activity, int id, Fragment frag) {
        FragmentManager fm = activity.getSupportFragmentManager();
        Fragment existingFrag = fm.findFragmentById(id);
        if(existingFrag == null) {
            fm.beginTransaction()
                    .add(id, frag)
                    .commit();
        }
    }
}
