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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

/**
 * Created by Thierry on 08/02/15.
 */
public class HelpDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Initialiser le texte de l'aide à partir d'un fichier inclu dans l'APK
        StringBuilder htmlHelpText = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader
                    (new InputStreamReader(activity.getAssets().open("help_contents.html")));
            String line;
            int state = 0;
            while((line = in.readLine()) != null) {
                if(line.contains("<!-- START APP CONTENTS -->"))
                    state = 1;
                else if(state == 1) {
                    if (line.contains("<!-- END APP CONTENTS -->"))
                        state = 2;
                    else
                        htmlHelpText.append(line);
                }
            }
        }
        catch(IOException exc) {
            htmlHelpText.setLength(0);
            htmlHelpText.append("Help is not available (").append(exc.getMessage()).append(")");
        }

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_help, null);
        TextView helpTextView = (TextView)dialogView.findViewById(R.id.dialog_help_text);
        helpTextView.setText(Html.fromHtml(htmlHelpText.toString()));

        // Allow for external hyperlinks in the HTML text
        helpTextView.setMovementMethod(LinkMovementMethod.getInstance());

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        return builder.create();
    }
}
