package app.park.com.common.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import app.park.com.common.logger.Log;
import app.park.com.common.logger.LogWrapper;

public class ActivityBase extends FragmentActivity {
    public static final String TAG = ActivityBase.class.getSimpleName();
    public static final boolean DBG = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected  void onStart() {
        super.onStart();
        if (DBG) {
            initializeLogging();
        }
    }

    /** Set up targets to receive log data */
    public void initializeLogging() {
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        // Wraps Android's native log framework
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);

        Log.i(TAG, "Ready");
    }
}
