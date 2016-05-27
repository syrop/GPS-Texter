package pl.org.seva.texter.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import pl.org.seva.texter.R;

/**
 * Created by wiktor on 5/20/16.
 */
public class NumberFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener {

    private static final int CONTACTS_QUERY_ID = 0;
    private static final int DETAILS_QUERY_ID = 1;

    private Toast toast;

    private final static String[] FROM_COLUMNS = {
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
    };

    private static final String[] CONTACTS_PROJECTION = {  // SELECT
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
    };

    private static final String CONTACTS_SELECTION =  // FROM
            ContactsContract.Contacts.HAS_PHONE_NUMBER + " = ?";

    private static final String CONTACTS_SORT =  // ORDER_BY
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY;

    private static final String[] DETAILS_PROJECTION = {  // SELECT
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
    };

    private static final String DETAILS_SORT =  // ORDER_BY
            ContactsContract.CommonDataKinds.Phone._ID;

    private static final String DETAILS_SELECTION =  // WHERE
            ContactsContract.Data.LOOKUP_KEY + " = ?";



    // The column index for the LOOKUP_KEY column
    private static final int CONTACT_KEY_INDEX = 1;
    private static final int DETAILS_NUMBER_INDEX = 1;
    private static final int DETAILS_TYPE_INDEX = 2;

    private final static int[] TO_IDS = {
            android.R.id.text1
    };

    private boolean contactsEnabled;
    private String contactKey;

    private SimpleCursorAdapter adapter;
    private ListView contacts;
    private EditText number;

    @Nullable
    @Override
    public View onCreateView
            (LayoutInflater inflater,
             @Nullable ViewGroup container,
             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.number_fragment, container, false);
        number = (EditText) v.findViewById(R.id.number);

        contactsEnabled = ContextCompat.checkSelfPermission(
                getActivity(),
                Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED;

        contacts = (ListView) v.findViewById(R.id.contacts);
        if (!contactsEnabled) {
            contacts.setVisibility(View.GONE);
        }
        else {
            contacts.setOnItemClickListener(this);
            adapter = new SimpleCursorAdapter(
                    getActivity(),
                    R.layout.contacts_list_item,
                    null,
                    FROM_COLUMNS,
                    TO_IDS,
                    0);
            contacts.setAdapter(adapter);
        }

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(CONTACTS_QUERY_ID, null, this);
    }

    @Override
    public String toString() {
        return number.getText().toString();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (!contactsEnabled) {
            return null;
        }
        switch (id) {
            case CONTACTS_QUERY_ID:
                String[] contactsSelectionArgs = {"1",};
                return new CursorLoader(
                        getActivity(),
                        ContactsContract.Contacts.CONTENT_URI,
                        CONTACTS_PROJECTION,
                        CONTACTS_SELECTION,
                        contactsSelectionArgs,
                        CONTACTS_SORT);
            case DETAILS_QUERY_ID:
                String[] detailsSelectionArgs = { contactKey, };
                return new CursorLoader(
                        getActivity(),
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        DETAILS_PROJECTION,
                        DETAILS_SELECTION,
                        detailsSelectionArgs,
                        DETAILS_SORT);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case CONTACTS_QUERY_ID:
                adapter.swapCursor(data);
                break;
            case DETAILS_QUERY_ID:
                String number = null;
                while (data.moveToNext()) {
                    if (data.getInt(DETAILS_TYPE_INDEX) ==
                            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN) {
                        number = data.getString(DETAILS_NUMBER_INDEX);
                        break;
                    }
                    else if (number == null) {
                        number = data.getString(DETAILS_NUMBER_INDEX);
                    }
                }
                data.close();
                if (number != null) {
                    this.number.setText(number);
                }
                else {
                    toast = Toast.makeText(
                            getContext(),
                            R.string.no_number,
                            Toast.LENGTH_SHORT);
                    toast.show();
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case CONTACTS_QUERY_ID:
                adapter.swapCursor(null);
                break;
            case DETAILS_QUERY_ID:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (toast != null) {
            toast.cancel();
        }
        Cursor cursor = ((SimpleCursorAdapter) parent.getAdapter()).getCursor();
        cursor.moveToPosition(position);
        contactKey = cursor.getString(CONTACT_KEY_INDEX);
        getLoaderManager().restartLoader(DETAILS_QUERY_ID, null, this);
    }

    public void setNumber(String number) {
        this.number.setText(number);
    }
}
