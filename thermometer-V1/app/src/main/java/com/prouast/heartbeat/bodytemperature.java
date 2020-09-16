package com.prouast.heartbeat;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class bodytemperature extends AppCompatActivity {
    private double result_value;
    private double result_te;
    private SharedPreferences sharedata;
    private SharedPreferences.Editor  editor;
    private RPPG rppg;
    private Date date = new Date();
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
            result_te = get_temperature(result_value);
            String output = String.valueOf(result_te)+"℃";
            bodytemperature.setText(output);
        }else {
            bodytemperature.setText("请完善您的个人信息！");
        }

        //返回按钮
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent2 = new Intent(bodytemperature.this, Main.class);
                startActivity(intent2);
                bodytemperature.this.finish();
            }
        });

        //设置标定值按钮
        setStdvalue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(bodytemperature.this, modify.class);
                intent1.putExtra("rppg",result_value);
                intent1.putExtra("te",result_te);
                startActivity(intent1);
                bodytemperature.this.finish();

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
        SimpleDateFormat sdf_second = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String age_input = sharedata.getString("age"," ");
        String sleep_input = sharedata.getString("sleep"," ");
        String std_time_input = sharedata.getString("stdtime"," ");
        String std_value_input = sharedata.getString("stdheart","1.0");
        int std_value = (int)(Double.valueOf(std_value_input)+0.5);
        int age = Integer.parseInt(age_input);


        boolean sexstr = sharedata.getBoolean("sex",true);
        int gender_flag = sexstr ? 0 : 1;

        int  testmode = sharedata.getInt("testmode",0);//测试模式选择
        String store_time = sharedata.getString("store_time"," ");//当前测量完成时间
        String last_time = sharedata.getString("last_time"," ");//上次测量完成时间
        String last_value_input = sharedata.getString("last_value","0.00");//上次测量参数值
        String last_te_input = sharedata.getString("last_te","0.00");//上次测量体温值
        String ini_time_input = sharedata.getString("initime"," ");

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

        double diff_inihours;//当前测量时间与第一次测试时间差
        try{
            Date initime = sdf_second.parse(ini_time_input);
            Date now_time = sdf_second.parse(sdf_second.format(date));
            long diff = now_time.getTime() - initime.getTime();
            double hours = (double) diff/(1000*60*60);
            diff_inihours = (hours + 24) % 24;
        }
        catch(ParseException e){
            diff_inihours = 100.00;
        }

        try {
            // 创建指定路径的文件
            File file = new File(Environment.getExternalStorageDirectory(), "difftime.txt");
            // 如果文件不存在
            if (file.exists()) {
                // 创建新的空文件
                file.delete();
            }
            file.createNewFile();
            // 获取文件的输出流对象
//                    FileOutputStream outStream = new FileOutputStream(file);
            BufferedWriter bw=new BufferedWriter(new FileWriter(file));
            // 获取字符串对象的byte数组并写入文件流
            bw.write(String.valueOf(diff_mins)+"\r\n");
            bw.write(String.valueOf(diff_hours)+"\r\n");
            bw.write(String.valueOf(diff_inihours)+"\r\n");
            // 最后关闭文件输出流
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        rppg = new RPPG();
        double result_te =  rppg.temperature(age,diff_hours,diff_nowhours,testmode,std_value,gender_flag,diff_mins,last_value,last_te,now_value);
        //参数说明：rppg.temperature(年龄，标定时间与入睡时间差，测量时间与入睡时间差，模式选择（1为运动模式，0为标准模式），标定值，性别，与上次测量时间间隔，上次测量值，上次测量温度，现在测量值)

//        rppg.exit();

        if(result_te>37.3 && diff_inihours<48){
            editor.putString("stdtime",sdf_times.format(date));
            editor.putString("stdheart",String.valueOf(now_value));
            editor.commit();
            double result_tes =  rppg.temperature(age,diff_hours,diff_nowhours,0,(int)(Double.valueOf(now_value)+0.5),gender_flag,0,0,0,now_value);

            try {
                // 创建指定路径的文件
                File file = new File(Environment.getExternalStorageDirectory(), "results.txt");
                // 如果文件不存在
                if (file.exists()) {
                    // 创建新的空文件
                    file.delete();
                }
                file.createNewFile();
                // 获取文件的输出流对象
//                    FileOutputStream outStream = new FileOutputStream(file);
                BufferedWriter bw=new BufferedWriter(new FileWriter(file));
                // 获取字符串对象的byte数组并写入文件流
                bw.write(String.valueOf(now_value));
                bw.write(String.valueOf(result_te));
                bw.write(String.valueOf(result_tes));
                // 最后关闭文件输出流
                bw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            result_te = result_tes;


        }
        BigDecimal bd = new BigDecimal(result_te);
        result_te = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

        save_data(result_te);

        editor.putString("last_te",String.valueOf(result_te));
        editor.putString("last_time",store_time);
        editor.putString("last_value",String.valueOf(now_value));
        editor.commit();

        return result_te;
    }

}