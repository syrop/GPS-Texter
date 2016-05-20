package pl.org.seva.texter.preferences;

import android.content.Context;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

import pl.org.seva.texter.R;
import pl.org.seva.texter.fragments.NumberFragment;

/**
 * Created by wiktor on 5/20/16.
 */
public class NumberPreference extends DialogPreference {

    private NumberFragment numberFragment;

    public NumberPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.number_dialog);
        setDialogIcon(null);
    }

    @Override
    protected View onCreateDialogView() {
        numberFragment = (NumberFragment)
                ((android.support.v4.app.FragmentActivity)getContext()).
                        getSupportFragmentManager().findFragmentById(R.id.number_fragment);

        return super.onCreateDialogView();
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
        if (numberFragment != null) {
            ((android.support.v4.app.FragmentActivity) getContext()).
                    getSupportFragmentManager().beginTransaction().remove(numberFragment).commit();
            numberFragment = null;
        }
        super.onDialogClosed(positiveResult);
    }
}
