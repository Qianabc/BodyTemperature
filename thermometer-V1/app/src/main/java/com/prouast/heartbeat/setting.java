package com.prouast.heartbeat;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Date;

public class setting extends AppCompatActivity {
    private SharedPreferences settingdata;
    private SharedPreferences.Editor  editor;
    private Date date = new Date();

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
    int TestMode = 0;

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
        final String sleep_input = settingdata.getString("sleep"," ");
        final boolean sexstr = settingdata.getBoolean("sex",true);
        int testmode_input = settingdata.getInt("testmode",0);


        if(age_input!=" "&&sleep_input!=" "){
            age.setText(age_input);
            sleeptime.setText(sleep_input);

            if(sexstr){
                man.setChecked(true);
            }else {
                woman.setChecked(true);
            }
            if(testmode_input==1){
                testmode.setChecked(true);
                testmode.setText("运动模式");
            }else{
                testmode.setChecked(false);
                testmode.setText("标准模式");
            }

        }

        makesure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String age_inputs = age.getText().toString().trim();
                String sleep_inputs = sleeptime.getText().toString().trim();

                boolean sexstrs = tip;
                if (TextUtils.isEmpty(age_inputs)||TextUtils.isEmpty(sleep_inputs)){
                    String outputs ="请完善您的基本信息！";
                    show.setText(outputs);
                }else {
//                    if(!(age_input.equals(age_inputs)&&sleep_input.equals(sleep_inputs)&&sexstrs==sexstr)){
//
//                    }
                    SimpleDateFormat sdf_now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String std_now = sdf_now.format(date);
                    editor.putString("initime",std_now);

                    String outputs = "睡眠时间："+sleep_inputs+"年龄："+age_inputs;
                    show.setVisibility(View.VISIBLE);
                    show.setText(outputs);
                    editor.putString("age",age_inputs);
                    editor.putString("sleep",sleep_inputs);
                    editor.putBoolean("sex",sexstrs);
                    editor.putInt("testmode",TestMode);

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
                    TestMode = 1;
                    testmode.setText("运动模式");
                }else {
                    TestMode = 0;
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

    }



    protected void showTimePickDlg() {

        new TimePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT,new TimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                setting.this.sleeptime.setText(hourOfDay+":"+minute);
            }
        }, 0, 0, true).show();
    }

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