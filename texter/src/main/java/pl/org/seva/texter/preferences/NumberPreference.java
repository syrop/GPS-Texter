package pl.org.seva.texter.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;

import pl.org.seva.texter.R;
import pl.org.seva.texter.fragments.NumberFragment;

/**
 * Created by wiktor on 5/20/16.
 */
public class NumberPreference extends DialogPreference {

    private NumberFragment numberFragment;
    private String number;

    public NumberPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.number_dialog);
        setDialogIcon(null);
    }

    @Override
    protected View onCreateDialogView() {
        View result = super.onCreateDialogView();
        numberFragment = (NumberFragment)
                ((android.support.v4.app.FragmentActivity) getContext()).
                        getSupportFragmentManager().findFragmentById(R.id.number_fragment);
        numberFragment.setNumber(number);

        return result;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return "";
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        number = restorePersistedValue ? getPersistedString("") : defaultValue.toString();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        if (numberFragment != null) {
            ((android.support.v4.app.FragmentActivity) getContext()).
                    getSupportFragmentManager().beginTransaction().remove(numberFragment).commit();
            numberFragment = null;
        }
        return super.onSaveInstanceState();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            number = numberFragment.toString();
            persistString(number);
        }
        if (numberFragment != null) {
            ((android.support.v4.app.FragmentActivity) getContext()).
                    getSupportFragmentManager().beginTransaction().remove(numberFragment).commit();
        }
        super.onDialogClosed(positiveResult);
    }
}
