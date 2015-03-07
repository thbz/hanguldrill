package org.thbz.hanguldrill;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.EditText;

/**
 * Created by Thierry on 21/12/14.
 */
public class InputConfigNameDialogFragment extends DialogFragment {

    public interface NoticeDialogListener {
        public void onInputConfigName(String newConfigName);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    public void setListener(NoticeDialogListener _mListener) {
        mListener = _mListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Enter a configuration name");

        final EditText input = new EditText(getActivity());

        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String value = input.getText().toString();
                // Send the positive button event back to the host activity
                mListener.onInputConfigName(value);
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
