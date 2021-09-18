/*
 *  This file is part of VidSnap.
 *
 *  VidSnap is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  VidSnap is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with VidSnap.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.mugames.vidsnap.ui.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.mugames.vidsnap.Firebase.FirebaseManager;
import com.mugames.vidsnap.R;

public class ReportActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    Spinner type_Spinner;
    Spinner serve_Spinner;
    String ty;
    String ser;
    View layout;
    Button button;
    EditText des;
    EditText mail;
    FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        Toolbar toolbar = findViewById(R.id.toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());


        firebaseManager = FirebaseManager.getInstance(this);
        firebaseManager.initReport();

        type_Spinner = findViewById(R.id.report_type);
        serve_Spinner = findViewById(R.id.report_service);
        layout = findViewById(R.id.layout_2);
        button = findViewById(R.id.report_send);
        des = findViewById(R.id.report_description);
        mail = findViewById(R.id.report_mail);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.support_type, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        type_Spinner.setAdapter(adapter);

        ArrayAdapter<CharSequence> adapter_service = ArrayAdapter.createFromResource(this, R.array.service_type, android.R.layout.simple_spinner_item);
        adapter_service.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serve_Spinner.setAdapter(adapter_service);

        type_Spinner.setOnItemSelectedListener(this);
        serve_Spinner.setOnItemSelectedListener(this);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseManager.getInstance(getApplicationContext()).checkUpdate((isUpdateAvailable, v1, isForced, changeLog, url) -> {
                    FirebaseManager.REPORT report;
                    if (ty.equals("Bug"))
                        report = FirebaseManager.REPORT.BUG;
                    else if (ty.equals("Suggestions"))
                        report = FirebaseManager.REPORT.SITE_REQUEST;
                    else report = FirebaseManager.REPORT.OTHER;


                    String descrp = des.getText().toString();
                    String mailID = mail.getText().toString();
                    if (descrp.isEmpty()) {
                        Toast.makeText(ReportActivity.this, "Description is mandatory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else if(report == FirebaseManager.REPORT.BUG){
                        if (isUpdateAvailable && isForced) {
                            Toast.makeText(ReportActivity.this, "Kindly,update app to check problem is resolved :)", Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    firebaseManager.saveReport(ReportActivity.this,report, ser, descrp, mailID);
                });
            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == type_Spinner) {
            ty = parent.getItemAtPosition(position).toString();
            layout.setEnabled(ty.equals("Bug"));
        } else if (parent == serve_Spinner) {
            ser = parent.getItemAtPosition(position).toString();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


}