package pl.org.seva.texter.listeners;

import android.location.Location;

import pl.org.seva.texter.model.LocationModel;

/**
 * Created by wiktor on 11.01.16.
 */
public interface ISMSListener {
    void onSendingSMS(LocationModel model);
    void onSMSSent(LocationModel model);
}
