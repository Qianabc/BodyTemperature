package com.prouast.heartbeat;


import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by yiyang on 2020/03/25.
 */
public class calculatedatas {
    private int times = 0;
    private int len_list = 0;
    private int heartbeat = 0;

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


}