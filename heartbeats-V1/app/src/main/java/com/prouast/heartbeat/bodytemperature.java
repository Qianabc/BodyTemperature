package com.prouast.heartbeat;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

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
    private SharedPreferences sharedata;
    private SharedPreferences.Editor  editor;
//    private calculatedatas cal;
    private int heartbeat;
    private Date date = new Date();
    Intent intent;
    TextView heartbeats;
    TextView bodytemperature;
    ImageButton settingbutton;
    ImageButton back;
    Button setStdheart;


    protected  void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.temperature);

        heartbeats = (TextView) findViewById(R.id.heartbeat);
        bodytemperature = (TextView) findViewById(R.id.bodytemperature);
        settingbutton = (ImageButton) findViewById(R.id.settingbutton);
        setStdheart = (Button) findViewById(R.id.stdheart);
        back = (ImageButton) findViewById(R.id.back_button);

        sharedata = getSharedPreferences("userdatas",MODE_PRIVATE);
        editor = sharedata.edit();

        final int heartbeat_calculate = calculate();
        final String outheartbeat = String.valueOf(heartbeat_calculate)+" 次/min";
        heartbeats.setText(outheartbeat);


        String age_input = sharedata.getString("age"," ");
        String nation_input = sharedata.getString("nation"," ");
        String wight_input = sharedata.getString("wight"," ");
        String sleep_input = sharedata.getString("sleep"," ");
        String stdtime_input = sharedata.getString("stdtime"," ");
        String stdheart_input = sharedata.getString("stdheart"," ");
        boolean sexstr = sharedata.getBoolean("sex",true);
        boolean testmode = sharedata.getBoolean("testmode",false);
        String sexstrs = sexstr?"man":"woman";
        String store_time = sharedata.getString("store_time"," ");
        String last_time = sharedata.getString("last_time"," ");
        int store_rpms = sharedata.getInt("sotre_rpms",0);
        String test_temperature = sharedata.getString("test_te"," ");
        double body_temperature_now = 0.0;

        if(age_input!=" "&&nation_input!=" "&&wight_input!=" "&&sleep_input!=" "&&stdheart_input!=" "&&stdtime_input!=" "&&heartbeat_calculate!=0){
            if(testmode){
                if(last_time==store_time){
                    body_temperature_now = body_temperature(heartbeat_calculate,0);
                }else {
                    SimpleDateFormat sdf_now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    double diff_mins;
                    double delta_bt =0.0;
                    int diff_rpms = 0;
                    try{
                        Date store = sdf_now.parse(store_time);
                        Date last = sdf_now.parse(last_time);
                        long diff = store.getTime() - last.getTime();//这样得到的差值是微秒级别
                        diff_mins =(double) diff / (1000 * 60);
                        diff_rpms = Math.abs(store_rpms-heartbeat_calculate);
                    }
                    catch (ParseException e)
                    {
                        diff_mins = 0;
                    }
                    if(diff_mins>20.0){
                        body_temperature_now = body_temperature(heartbeat_calculate,0);
                    }else {
                        if(diff_rpms != 0){
                            delta_bt = body_temperature(heartbeat_calculate,1);
                        }

                        if(delta_bt>=0.2||diff_rpms==0){
                            if(diff_mins<=4){
                                delta_bt = 0;
                            }else if(diff_mins<=10){
                                delta_bt = 0.15;
                            }else {
                                delta_bt = 0.1;
                            }

                        }
                        body_temperature_now = body_temperature(heartbeat_calculate,0)+delta_bt;

                    }

                }

            }else {
                body_temperature_now = body_temperature(heartbeat_calculate,0);
            }

            BigDecimal bd = new BigDecimal(body_temperature_now);
            double last_bodyte = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            String output;

            if(testmode){
                body_temperature_now = body_temperature(heartbeat_calculate,0);
                bd = new BigDecimal(body_temperature_now);
                double ini_bodyte = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                if(last_time==store_time){
                    output = "测试： "+test_temperature+"℃" + "\r\n"+ "正常： "+String.valueOf(ini_bodyte)+"℃";
                }else {
                    output = "测试： "+String.valueOf(last_bodyte)+"℃" + "\r\n"+ "正常： "+String.valueOf(ini_bodyte)+"℃";
                    editor.putString("test_te",String.valueOf(last_bodyte));
                    editor.commit();
                }

            }else {
                output = "正常： "+String.valueOf(last_bodyte)+"℃";
            }



            if(store_time!=last_time){
                save_data(last_bodyte,heartbeat_calculate);
                editor.putString("last_time",store_time);
                editor.putInt("store_rpms",heartbeat_calculate);
                editor.commit();
            }

            bodytemperature.setText(output);
        }else if(age_input==" "||nation_input==" "||wight_input==" "||sleep_input==" ") {
            bodytemperature.setText("请完善您的个人信息！");
        }else if(heartbeat_calculate==0){
            bodytemperature.setText("开始测量吧！");
        }else if(stdheart_input==" "||stdtime_input==" "){
            bodytemperature.setText("没有标定心率！");
        }

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitactivity();
            }
        });

        settingbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(bodytemperature.this, setting.class);
                startActivity(intent);
                bodytemperature.this.finish();
            }
        });

        setStdheart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
                editor.putString("stdtime",formatter.format(date));
                editor.putString("stdheart",String.valueOf(heartbeat_calculate));
                editor.commit();
            }
        });


    }

    public void exitactivity(){
        Intent intent = new Intent(bodytemperature.this, Main.class);
        startActivity(intent);
        bodytemperature.this.finish();
    }

    public int  calculate(){
        double[] list_bpms = new double[100];
        File file = new File(Environment.getExternalStorageDirectory(), "bpms.txt");
        if (file.exists()){
            try (
                    FileReader reader = new FileReader(file);
                    BufferedReader br = new BufferedReader(reader) // 建立一个对象，它把文件内容转成计算机能读懂的语言
            ) {
                String line;
                //网友推荐更加简洁的写法
                int i = 0;
                while ((line = br.readLine()) != null) {
                    // 一次读入一行数据
                    list_bpms[i] = Double.parseDouble(line);
                    i++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            int len_list = 0;
            for(int i =0;i<100;i++) {
                len_list = i;
                if(list_bpms[i]==0) {
                    break;
                }
            }

            double[] s = new double[4];
            double[] result = new double[2];
            double[][] rpms = new double[len_list-4][2];
            for(int i = 0;i<len_list-4;i++) {

                for(int j =0;j<4;j++) {
                    s[j]=list_bpms[i+j];
                }
                result = mean(s);
                rpms[i][0] = result[0];
                rpms[i][1] = result[1];

            }
            int min_diff = 0;
            for(int i = 0;i<len_list-4;i++) {
                if (rpms[min_diff][1] >= rpms[i][1]) {
                    min_diff = i;
                }
            }
            heartbeat = (int) (rpms[min_diff][0]+0.5);
        }else {
            heartbeat = 0;
        }



//        System.out.printf("心率为：%d",(int) rpms[min_diff][0]);
        return  heartbeat;
    }

    public double[] mean(double[] s) {
        double sum = 0;
        for(int i=0;i<s.length;i++) {
            sum += s[i];
        }
        double mean = (double) sum/s.length;

        double diff = 0;
        for(int i = 1 ;i < s.length ; i++) {
            diff +=(s[i]-mean)*(s[i]-mean);
        }
        double[] result= {mean,diff};
        return result;
    }

    public double body_temperature(int now_heartbeats,int mode){
        sharedata = getSharedPreferences("userdatas",MODE_PRIVATE);
        Calendar cal = Calendar.getInstance();

        SimpleDateFormat sdf_age = new SimpleDateFormat("yyyy-MM-dd");
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf_times = new SimpleDateFormat("HH:mm");

        String age_input = sharedata.getString("age"," ");
        String nation_input = sharedata.getString("nation"," ");
        String wight_input = sharedata.getString("wight"," ");
        String sleep_input = sharedata.getString("sleep"," ");
        String stdtime_input = sharedata.getString("stdtime"," ");
        String stdheart_input = sharedata.getString("stdheart"," ");
        boolean sexstr = sharedata.getBoolean("sex",true);

        int hr_standard = Integer.parseInt(stdheart_input);

        int gender_flag = sexstr ? 0 : 1;

        int age;
        age = Integer.parseInt(age_input);
//        try{
//            Date birth = sdf_age.parse(age_input);
//            Date now = sdf_age.parse(sdf_age.format(date));
//            long diff = now.getTime() - birth.getTime();//这样得到的差值是微秒级别
//            int days =(int) (diff / (1000 * 60 * 60 * 24));
//            age = (days + 183) / 366;
//        }
//        catch (ParseException e)
//        {
//            age = 1;
//        }

        int age_flag;
        if (age <= 29){
            age_flag = 1;
        }else if(age <=39){
            age_flag = 2;
        }else if(age <=59){
            age_flag = 3;
        }else {
            age_flag = 4;
        }


        double diff_hours;
        try{
            Date sleep = sdf_times.parse(sleep_input);
            Date std_sleep = sdf_times.parse(stdtime_input);
            long diff = std_sleep.getTime() - sleep.getTime();//这样得到的差值是微秒级别
            double hours =(double) diff / (1000 * 60 * 60);
            diff_hours = (hours + 24) % 24;
        }
        catch (ParseException e)
        {
            diff_hours = 0;
        }

        double diff_nowhours;//当前时间与入睡时间差
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

        double hr_predict = hr_standard + hr_delta(diff_nowhours, age_flag) - hr_delta(diff_hours, age_flag);
        int fever_flag;
        if (now_heartbeats - hr_predict > 10) {    // 如果心率实际变化比预测变化大
            fever_flag = 0; //可能发烧了
        }else {
            fever_flag = 1; //判断未发烧
        }

        double bt_st = 36.7 + (-0.005763*age + 0.2373) - 0.1*Math.cos(diff_nowhours*0.2661) - 0.39*Math.sin(diff_nowhours*0.2661)
                + 0.04*Math.cos(diff_nowhours*0.5322) - 0.05*Math.sin(diff_nowhours*0.5322) + (0.05*gender_flag);
        double bt_hr = bt_st + (1-0.54*fever_flag)*(now_heartbeats - hr_predict) / (8.694*Math.exp(-0.03897*age)+5.432*Math.exp(-0.0017*age)+0.25*gender_flag);

        double delta_bt = (8.694*Math.exp(-0.03897*age)+5.432*Math.exp(-0.0017*age)+0.25*gender_flag);

        if (mode == 1){
            return delta_bt;
        }else {
            return bt_hr;
        }

    }

    public double hr_delta(double time,int flag){
        double delta = 0;
        switch (flag){
            case 1:
                delta = 9.272*Math.sin(0.2556*time -2.454)+16.95*Math.sin(0.00739*time+0.09574)
                        +3.864*Math.sin(0.5958*time+1.279);
                break;
            case 2:
                delta = 106*Math.sin(0.0049*time+0.0194) + 11.57*Math.sin(0.3075*time+3.388)
                        + 2.466*Math.sin(0.7002*time+0.3811);
                break;
            case 3:
                delta = 15.35*Math.sin(0.1308*time-0.6176) + 297.3*Math.sin(0.5407*time+0.07197)
                        + 297.2*Math.sin(0.5425*time+3.183);
                break;
            default:
                delta = 5.707*Math.sin(0.2844*time-2.59) + 5.67*Math.sin(0.01665*time+0.404)
                        + 1.527*Math.sin(0.5567*time+2.67);

        }

        return delta;
    }

    public void save_data (double te,int rpms){
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

            String content = std_now + " " +String.valueOf(te) + " " + String.valueOf(rpms) ;

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



}