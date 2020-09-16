package com.prouast.heartbeat;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;

public class setting extends AppCompatActivity {
    private SharedPreferences settingdata;
    private SharedPreferences.Editor  editor;

    EditText age;
    EditText sleeptime;


    TextView show;

    RadioGroup sex;
    RadioButton man;
    RadioButton woman;
    Switch testmode;
    Button makesure;
    ImageButton returnbutton;
    boolean tip = true;
    boolean TestMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        age = (EditText) findViewById(R.id.age);

        sleeptime = (EditText) findViewById(R.id.sleeptime);

        sex = (RadioGroup) findViewById(R.id.sex);
        makesure = (Button) findViewById(R.id.makesure);
        show = (TextView) findViewById(R.id.show);

        man = (RadioButton) findViewById(R.id.boy);
        woman = (RadioButton) findViewById(R.id.girl);
        testmode = (Switch) findViewById(R.id.testmode);
        returnbutton = (ImageButton) findViewById(R.id.returnbutton);
        settingdata = getSharedPreferences("userdatas",MODE_PRIVATE);
        editor = settingdata.edit();


        final String age_input = settingdata.getString("age"," ");
        String sleep_input = settingdata.getString("sleep"," ");
        String stdtime_input = settingdata.getString("stdtime"," ");
        final String stdheart_input = settingdata.getString("stdheart"," ");
        boolean sexstr = settingdata.getBoolean("sex",true);
        boolean testmode_input = settingdata.getBoolean("testmode",false);


        if(age_input!=" "&&sleep_input!=" "){
            age.setText(age_input);
            sleeptime.setText(sleep_input);

            if(sexstr){
                man.setChecked(true);
            }else {
                woman.setChecked(true);
            }
            if(testmode_input){
                testmode.setChecked(true);
                testmode.setText("运动模式");
            }else{
                testmode.setChecked(false);
                testmode.setText("标准模式");
            }

        }
//        if(stdheart_input == " "){
//            standardtime.setVisibility(View.INVISIBLE);
//            standardheart.setVisibility(View.INVISIBLE);
//            std_time_str.setVisibility(View.INVISIBLE);
//            std_heart_str.setVisibility(View.INVISIBLE);
//        }else {
//            standardtime.setText(stdtime_input);
//            standardheart.setText(stdheart_input);
//        }

        makesure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String age_inputs = age.getText().toString().trim();
                String sleep_inputs = sleeptime.getText().toString().trim();
//                String stdtime_inputs = standardtime.getText().toString().trim();
//                String stdheart_inputs = standardheart.getText().toString().trim();

                boolean sexstrs = tip;
                if (TextUtils.isEmpty(age_inputs)||TextUtils.isEmpty(sleep_inputs)){
                    String outputs ="请完善您的基本信息！";
                    show.setText(outputs);
                }else {
                    String outputs = "睡眠时间："+sleep_inputs+"年龄："+age_inputs;
                    show.setVisibility(View.VISIBLE);
                    show.setText(outputs);
                    editor.putString("age",age_inputs);
                    editor.putString("sleep",sleep_inputs);
                    editor.putBoolean("sex",sexstrs);
                    editor.putBoolean("testmode",TestMode);

//                    if (!TextUtils.isEmpty(stdheart_inputs)&&!TextUtils.isEmpty(stdtime_inputs)){
//                        editor.putString("stdtime",stdtime_inputs);
//                        editor.putString("stdheart",stdheart_inputs);
//                    }
                    editor.commit();
                    exitactivity();
                }
            }
        });

        sex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                tip = checkedId == R.id.boy? true:false;
            }
        });

        testmode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    TestMode = true;
                    testmode.setText("运动模式");
                }else {
                    TestMode = false;
                    testmode.setText("标准模式");
                }
            }
        });

        returnbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitactivity();
            }
        });
//
//        age.setOnTouchListener(new View.OnTouchListener() {
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    showDatePickDlg();
//                    return true;
//                }
//                return false;
//            }
//        });
//        age.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (hasFocus) {
//                    showDatePickDlg() ;
//                }
//            }
//        });
        sleeptime.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    showTimePickDlg();
                    return true;
                }
                return false;
            }
        });
        sleeptime.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showTimePickDlg();
                }
            }
        });

//        standardtime.setOnTouchListener(new View.OnTouchListener() {
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    showStdTimePickDlg();
//                    return true;
//                }
//                return false;
//            }
//        });
//
//        standardtime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (hasFocus) {
//                    showStdTimePickDlg();
//                }
//            }
//        });
    }

    protected void showDatePickDlg() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(setting.this, new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                setting.this.age.setText(year + "-" + monthOfYear + "-" + dayOfMonth);
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }


    protected void showTimePickDlg() {

        new TimePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT,new TimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                setting.this.sleeptime.setText(hourOfDay+":"+minute);
            }
        }, 0, 0, true).show();
    }

//    protected void showStdTimePickDlg() {
//
//        new TimePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT,new TimePickerDialog.OnTimeSetListener() {
//
//            @Override
//            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
//                setting.this.standardtime.setText(hourOfDay+":"+minute);
//            }
//        }, 0, 0, true).show();
//    }

    public void exitactivity(){
        int enter = settingdata.getInt("enter",1);
        if(enter ==1 ){
            Intent intent = new Intent(setting.this, Main.class);
            startActivity(intent);
            setting.this.finish();
        }else{
            Intent intent = new Intent(setting.this, bodytemperature.class);
            startActivity(intent);
            setting.this.finish();
        }
    }


}