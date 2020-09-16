package com.prouast.heartbeat;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.prouast.heartbeat.Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class bodytemperature extends AppCompatActivity {
    private double result_value;
    private SharedPreferences sharedata;
    private SharedPreferences.Editor  editor;
    private RPPG rppg;
    private int heartbeat;
    private Date date = new Date();
    Intent intent;
    TextView bodytemperature;
    ImageButton back;
    Button setStdvalue;


    protected  void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.temperature);
        Intent intent = getIntent();
        result_value = intent.getDoubleExtra("rppg",0.0);

        bodytemperature = (TextView) findViewById(R.id.bodytemperature);
        setStdvalue = (Button) findViewById(R.id.stdheart);
        back = (ImageButton) findViewById(R.id.back_button);

        sharedata = getSharedPreferences("userdatas",MODE_PRIVATE);
        editor = sharedata.edit();


        String age_input = sharedata.getString("age"," ");
        String sleep_input = sharedata.getString("sleep"," ");
        String std_time_input = sharedata.getString("stdtime"," ");
        String std_value_input = sharedata.getString("stdheart"," ");

        //获取标定值，如果没有，将当前测量结果作为标定值
        if(std_value_input==" "||std_time_input==" "){
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
            editor.putString("stdtime",formatter.format(date));
            editor.putString("stdheart",String.valueOf(result_value));
            editor.commit();

            std_time_input = sharedata.getString("stdtime"," ");
            std_value_input = sharedata.getString("stdheart"," ");
        }

        if(age_input!=" "&&sleep_input!=" "&&std_value_input!=" "&&std_time_input!=" "){
            String output = String.valueOf(get_temperature(result_value))+"℃";
            bodytemperature.setText(output);
        }else {
            bodytemperature.setText("请完善您的个人信息！");
        }

        //返回按钮
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(bodytemperature.this, Main.class);
                startActivity(intent);
                bodytemperature.this.finish();
            }
        });

        //设置标定值按钮
        setStdvalue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
                editor.putString("stdtime",formatter.format(date));
                editor.putString("stdheart",String.valueOf(result_value));
                editor.commit();
                setStdvalue.setAlpha(0.5f);

                Toast.makeText(bodytemperature.this, "设置成功", Toast.LENGTH_SHORT).show();

            }
        });


    }

    public void save_data (double te){
        try {
            // 创建指定路径的文件
            File file = new File(Environment.getExternalStorageDirectory(), "history.txt");
            // 如果文件不存在
            if (!file.exists()) {
                // 创建新的空文件
                file.createNewFile();
            }
            // 获取文件的输出流对象
            SimpleDateFormat sdf_now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String std_now = sdf_now.format(date);

            String content = std_now + " " +String.valueOf(te) +"℃";

            BufferedWriter out = null ;
            try  {
                out = new  BufferedWriter( new OutputStreamWriter(
                        new FileOutputStream(file,  true )));
                out.write(content+"\r\n");
            } catch  (Exception e) {
                e.printStackTrace();
            } finally  {
                try  {
                    out.close();
                } catch  (IOException e) {
                    e.printStackTrace();
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double get_temperature(double now_value){
        sharedata = getSharedPreferences("userdatas",MODE_PRIVATE);

        SimpleDateFormat sdf_times = new SimpleDateFormat("HH:mm");

        String age_input = sharedata.getString("age"," ");
        String sleep_input = sharedata.getString("sleep"," ");
        String std_time_input = sharedata.getString("stdtime"," ");
        String std_value_input = sharedata.getString("stdheart"," ");
        int std_value = (int)(Double.valueOf(std_value_input)+0.5);
        int age = Integer.parseInt(age_input);


        boolean sexstr = sharedata.getBoolean("sex",true);
        int gender_flag = sexstr ? 0 : 1;

        int  testmode = sharedata.getInt("testmode",0);//测试模式选择
        String store_time = sharedata.getString("store_time"," ");//当前测量完成时间
        String last_time = sharedata.getString("last_time"," ");//上次测量完成时间
        String last_value_input = sharedata.getString("last_value","0.00");//上次测量参数值
        String last_te_input = sharedata.getString("last_te","0.00");//上次测量体温值

        double last_value = Double.valueOf(last_value_input);
        double last_te = Double.valueOf(last_te_input);

        double diff_mins = 0;//与上次测量时间差
        if(last_value==0||last_te==0||last_time==" "){
            testmode = 0;
        }else if(testmode==1) {
            SimpleDateFormat sdf_now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try{
                Date store = sdf_now.parse(store_time);
                Date last = sdf_now.parse(last_time);
                long diff = store.getTime() - last.getTime();//这样得到的差值是微秒级别
                diff_mins =(double) diff / (1000 * 60);
            }
            catch (ParseException e)
            {
                diff_mins = 0;
            }
        }


        double diff_hours;//入睡时间与标定时间差
        try{
            Date sleep = sdf_times.parse(sleep_input);
            Date std_sleep = sdf_times.parse(std_time_input);
            long diff = std_sleep.getTime() - sleep.getTime();//这样得到的差值是微秒级别
            double hours =(double) diff / (1000 * 60 * 60);
            diff_hours = (hours + 24) % 24;
        }
        catch (ParseException e)
        {
            diff_hours = 0;
        }

        double diff_nowhours;//当前测量时间与入睡时间差
        try{
            Date sleep = sdf_times.parse(sleep_input);
            Date std_now = sdf_times.parse(sdf_times.format(date));
            long diff = std_now.getTime() - sleep.getTime();//这样得到的差值是微秒级别
            double hours =(double) diff / (1000 * 60 * 60);
            diff_nowhours = (hours + 24) % 24;
        }
        catch (ParseException e)
        {
            diff_nowhours = 0;
        }



        rppg = new RPPG();
        double result_te =  rppg.temperature(age,diff_hours,diff_nowhours,testmode,std_value,gender_flag,diff_mins,last_value,last_te,now_value);
        BigDecimal bd = new BigDecimal(result_te);
        result_te = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        rppg.exit();

        save_data(result_te);

        editor.putString("last_te",String.valueOf(result_te));
        editor.putString("last_time",store_time);
        editor.putString("last_value",String.valueOf(now_value));
        editor.commit();

        return result_te;
    }



}