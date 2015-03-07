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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Créé par Thierry on 07/02/15.
 */
public class ConfigManageDialogFragment extends DialogFragment {
    Set<Integer> configsSelected = null;

    public interface NoticeDialogListener {
        public void onConfigManageSaveClick(Set<Integer>configIds);
        public void onConfigManageException(InternalException exc);
    }

    NoticeDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity _activity) {
        super.onAttach(_activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) _activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(_activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    private final static String CONFIGSSELECTEDKEY = "CONFIGSSELECTED";

    @Override
    public void onSaveInstanceState (Bundle outState) {
        if(configsSelected != null && configsSelected.size() > 0) {
            ArrayList<Integer> arr = new ArrayList<>();
            arr.addAll(configsSelected);
            outState.putIntegerArrayList(CONFIGSSELECTEDKEY, arr);
        }

        super.onSaveInstanceState(outState);
    }

    private void alert(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog dialog = builder.setMessage(message)
                .setPositiveButton("Ok", null)
                .create();
        dialog.show();
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configsSelected = new HashSet<>();
        if(savedInstanceState != null)
            configsSelected.addAll(savedInstanceState.getIntegerArrayList(CONFIGSSELECTEDKEY));
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        List<String> listConfigNames = new ArrayList<>();
        try {
            String[] configIds = Settings.ConfigManager.getAllConfigIds(getActivity());
            for (String configId: configIds) {
                Settings.Configuration config =
                        Settings.ConfigManager.getConfigFromId(getActivity(), configId);
                listConfigNames.add(config.getName(getActivity()));
            }
        }
        catch(InternalException exc) {
            mListener.onConfigManageException(exc);
        }

        String[] configNames = new String[listConfigNames.size()];
        listConfigNames.toArray(configNames);

        DialogInterface.OnMultiChoiceClickListener listener =
            new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    if(isChecked)
                        configsSelected.add(which);
                    else if(configsSelected.contains(which))
                        configsSelected.remove(which);
                }
            };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete configurations")
                .setMultiChoiceItems(configNames, null, listener)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                })
                .setNegativeButton("Do not delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { }
                });
        return builder.create();
    }

    // cf. http://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog-from-closing-when-a-button-is-clicked
    @Override
    public void onStart()
    {
        super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
        final AlertDialog d = (AlertDialog)getDialog();
        if(d != null)
        {
            Button positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    // Only dismiss the dialog if the user confirms the deletion
                    int nbToDelete = configsSelected.size();
                    if(nbToDelete == 0) {
                        alert("You have selected no configuration.");
                    }
                    else {
                        // Confirm
                        String message = "Do you really want to delete " + nbToDelete
                                + " configuration" + (nbToDelete >= 2 ? "s" : "") + "?";
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        AlertDialog dialog = builder.setMessage(message)
                                .setPositiveButton("Yes",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                // Save the configurations and dismiss the dialog
                                                mListener.onConfigManageSaveClick(configsSelected);
                                                d.dismiss();
                                            }
                                        })
                                .setNegativeButton("No", null)
                                .create();
                        dialog.show();
                    }
                }
            });
        }
    }
}
