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
    EditText nation;
    EditText wight;
    EditText sleeptime;
    EditText standardtime;
    EditText standardheart;
    EditText sample_f;
    EditText rescan_f;
    EditText min_singal;
    EditText max_singal;
    EditText offset;
    EditText min_light;
    EditText max_light;
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
        nation =(EditText) findViewById(R.id.nation);
        wight = (EditText) findViewById(R.id.wight);
        sleeptime = (EditText) findViewById(R.id.sleeptime);
        standardtime = (EditText) findViewById(R.id.standard_time);
        standardheart = (EditText) findViewById(R.id.standard_heart);
        sample_f = (EditText) findViewById(R.id.sampling_f);
        rescan_f = (EditText) findViewById(R.id.rescan_f);
        min_singal = (EditText) findViewById(R.id.min_signal_size);
        max_singal = (EditText) findViewById(R.id.max_signal_size);
        offset = (EditText) findViewById(R.id.offset);
        min_light = (EditText) findViewById(R.id.min_light);
        max_light = (EditText) findViewById(R.id.max_light);

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
        final String nation_input = settingdata.getString("nation"," ");
        final String wight_input = settingdata.getString("wight"," ");
        String sleep_input = settingdata.getString("sleep"," ");
        String stdtime_input = settingdata.getString("stdtime"," ");
        final String stdheart_input = settingdata.getString("stdheart"," ");
        boolean sexstr = settingdata.getBoolean("sex",true);
        boolean testmode_input = settingdata.getBoolean("testmode",false);
        final int min_singal_input = settingdata.getInt("min_singal_size",5);
        final int max_singal_input = settingdata.getInt("max_singal_size",5);
        final float sample_input = settingdata.getFloat("sample_frequency",1);
        final float rescan_input = settingdata.getFloat("rescan_frequency",1);
        final int offset_input = settingdata.getInt("offset",50);
        final int min_light_input = settingdata.getInt("min_light",70);
        final int max_light_input = settingdata.getInt("max_light",150);

        if(age_input!=" "&&nation_input!=" "&&wight_input!=" "&&sleep_input!=" "){
            age.setText(age_input);
            nation.setText(nation_input);
            wight.setText(wight_input);
            sleeptime.setText(sleep_input);
            min_singal.setText(String.valueOf(min_singal_input));
            max_singal.setText(String.valueOf(max_singal_input));
            sample_f.setText(String.valueOf(sample_input));
            rescan_f.setText(String.valueOf(rescan_input));
            offset.setText(String.valueOf(offset_input));
            min_light.setText(String.valueOf(min_light_input));
            max_light.setText(String.valueOf(max_light_input));

            if(sexstr){
                man.setChecked(true);
            }else {
                woman.setChecked(true);
            }
            if(testmode_input){
                testmode.setChecked(true);
            }else{
                testmode.setChecked(false);
            }

        }
        standardtime.setText(stdtime_input);
        standardheart.setText(stdheart_input);

        makesure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String age_inputs = age.getText().toString().trim();
                String nation_inputs = nation.getText().toString().trim();
                double wight_inputs = 0;
                try{
                    wight_inputs = Double.parseDouble(wight.getText().toString().trim());
                }catch (Exception e){
                    AlertDialog alertDialog = new AlertDialog.Builder(setting.this).create();
                    alertDialog.setTitle("警告！");
                    alertDialog.setMessage("请输入正确的体重信息！");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }


                String sleep_inputs = sleeptime.getText().toString().trim();
                String stdtime_inputs = standardtime.getText().toString().trim();
                String stdheart_inputs = standardheart.getText().toString().trim();

                int min_singal_size = Integer.parseInt(min_singal.getText().toString().trim());
                int max_singal_size = Integer.parseInt(max_singal.getText().toString().trim());
                int offset_value = Integer.parseInt(offset.getText().toString().trim());
                int minLight = Integer.parseInt(min_light.getText().toString().trim());
                int maxLight = Integer.parseInt(max_light.getText().toString().trim());
                float sample_frequency = Float.parseFloat(sample_f.getText().toString().trim());
                float rescan_frequency = Float.parseFloat(rescan_f.getText().toString().trim());

                boolean sexstrs = tip;
                if (TextUtils.isEmpty(age_inputs)||TextUtils.isEmpty(nation_inputs)||wight_inputs==0||TextUtils.isEmpty(sleep_inputs)){
                    String outputs ="请完善您的基本信息！";
                    show.setText(outputs);
                }else {
                    String outputs = "睡眠时间："+sleep_inputs+"年龄："+age_inputs+"体重:"+String.valueOf(wight_inputs)+"民族："+nation_inputs;
                    show.setVisibility(View.VISIBLE);
                    show.setText(outputs);
                    editor.putString("age",age_inputs);
                    editor.putString("nation",nation_inputs);
                    editor.putString("wight",String.valueOf(wight_inputs));
                    editor.putString("sleep",sleep_inputs);
                    editor.putBoolean("sex",sexstrs);
                    editor.putBoolean("testmode",TestMode);
                    editor.putInt("min_singal_size",min_singal_size);
                    editor.putInt("max_singal_size",max_singal_size);
                    editor.putFloat("sample_frequency",sample_frequency);
                    editor.putFloat("rescan_frequency",rescan_frequency);
                    editor.putInt("offset",offset_value);
                    editor.putInt("min_light",minLight);
                    editor.putInt("max_light",maxLight);

                    if (!TextUtils.isEmpty(stdheart_inputs)&&!TextUtils.isEmpty(stdtime_inputs)){
                        editor.putString("stdtime",stdtime_inputs);
                        editor.putString("stdheart",stdheart_inputs);
                    }
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
                }else {
                    TestMode = false;
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
//                    showDatePickDlg();
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

        standardtime.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    showStdTimePickDlg();
                    return true;
                }
                return false;
            }
        });

        standardtime.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showStdTimePickDlg();
                }
            }
        });
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

    protected void showStdTimePickDlg() {

        new TimePickerDialog(this, AlertDialog.THEME_HOLO_LIGHT,new TimePickerDialog.OnTimeSetListener() {

            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                setting.this.standardtime.setText(hourOfDay+":"+minute);
            }
        }, 0, 0, true).show();
    }

    public void exitactivity(){
        Intent intent = new Intent(setting.this, bodytemperature.class);
        startActivity(intent);
        setting.this.finish();
    }
}