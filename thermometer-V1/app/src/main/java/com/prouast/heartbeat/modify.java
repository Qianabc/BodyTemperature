package com.prouast.heartbeat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class modify extends AppCompatActivity {
    private double max_difference = 0.4;
    private double result_value;
    private double result_te;
    private SharedPreferences sharedata;
    private SharedPreferences.Editor  editor;
    private Date date = new Date();
    TextView bodytemperature;
    ImageButton back;
    Button makesure;

    protected  void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.realte);

        bodytemperature = (TextView) findViewById(R.id.realte);
        makesure = (Button) findViewById(R.id.save);
        back = (ImageButton) findViewById(R.id.back_button1);

        Intent intent = getIntent();
        result_value = intent.getDoubleExtra("rppg",0.0);
        result_te = intent.getDoubleExtra("te",0.0);

        sharedata = getSharedPreferences("userdatas",MODE_PRIVATE);
        editor = sharedata.edit();

        //返回按钮
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(modify.this, bodytemperature.class);
                intent1.putExtra("rppg",result_value);
                startActivity(intent1);
                modify.this.finish();
            }
        });

        //设置标定值按钮
        makesure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double thermometer_value = Double.parseDouble(bodytemperature.getText().toString().trim());
                if(Math.abs(thermometer_value-result_te)>max_difference){
                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
                    editor.putString("stdtime",formatter.format(date));
                    editor.putString("stdheart",String.valueOf(result_value));
                    editor.commit();
                }

                makesure.setAlpha(0.5f);

                Toast.makeText(modify.this, "设置成功", Toast.LENGTH_SHORT).show();

            }
        });
    }


}