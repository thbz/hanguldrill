/*
 * Copyright (c) 2015 Thierry Bézecourt
 *
 * This file is part of HangulDrill.
 *
 * HangulDrill is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HangulDrill is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HangulDrill.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thbz.hanguldrill;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Generic confirm dialog box.
 * Created by Thierry on 08/02/15.
 */
public class ConfirmDialogFragment extends DialogFragment {
    public interface Listener {
        public void onConfirmDialogYes();
        public void onConfirmDialogNo();
    }

    Listener mListener;
    String message;

    // Must be called before show()
    public ConfirmDialogFragment setListener(Listener listener) {
        mListener = listener;
        return this;
    }

    // Must be called before show()
    public ConfirmDialogFragment setMessage(String _message) {
        message = _message;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onConfirmDialogYes();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onConfirmDialogNo();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
