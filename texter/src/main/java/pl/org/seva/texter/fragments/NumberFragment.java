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

import pl.org.seva.texter.R;
/**
 * Created by wiktor on 5/20/16.
 */
public class NumberFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener {

    private final static String[] FROM_COLUMNS = {
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
    };

    private static final String[] PROJECTION = {
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.LOOKUP_KEY,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
    };

    private static final String SELECTION = ContactsContract.Contacts.HAS_PHONE_NUMBER + " = ?";

    // The column index for the _ID column
    private static final int CONTACT_ID_INDEX = 0;
    // The column index for the LOOKUP_KEY column
    private static final int LOOKUP_KEY_INDEX = 1;

    private final static int[] TO_IDS = {
            android.R.id.text1
    };

    private boolean contactsEnabled;

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
                    FROM_COLUMNS, TO_IDS,
                    0);
            contacts.setAdapter(adapter);
        }

        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (!contactsEnabled) {
            return null;
        }
        String[] selectionArgs = { "1", };
        return new CursorLoader(
                getActivity(),
                ContactsContract.Contacts.CONTENT_URI,
                PROJECTION,
                SELECTION,
                selectionArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}
