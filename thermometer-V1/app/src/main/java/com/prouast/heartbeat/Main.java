package com.prouast.heartbeat;

import org.apache.commons.io.FileUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import static org.opencv.imgproc.Imgproc.putText;

/**
 * The main class.
 */
public class Main extends AppCompatActivity implements CvCameraViewListener2, RPPG.RPPGListener {

    /* Settings */
    private static final RPPG.RPPGAlgorithm ALGORITHM = RPPG.RPPGAlgorithm.g;
    private static final double SAMPLING_FREQUENCY = 1;
    private static final double RESCAN_FREQUENCY = 1;
    private static final double TIME_BASE = 0.001;
    private static final int MIN_SIGNAL_SIZE = 8;
    private static final int MAX_SIGNAL_SIZE = 8;
    private static final int OFFSET = 50;
    private static final int MIN_LIGHT = 10;
    private static final int MAX_LIGHT = 250;
    private static final int MIN_VARIANCE = 40;
    private static final boolean LOG = false;
    private static final boolean VIDEO = false;
    private static final boolean GUI = true;
    private static final int VIDEO_BITRATE = 100000;
    private boolean ProgressBar_En = true;
    private boolean rppg_En = false;

    /* Constants */
    private static final String TAG = "Thermometer::Main";
    private static final Pattern PARTIAl_IP_ADDRESS =
            Pattern.compile("^((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])\\.){0,3}"+
                    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])){0,1}$");

    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView forebackground;
    private RPPG rPPG;
    private RPPGResultQueue queue;
    private Mat mRgba;
    private Mat mGray;
    private long time;
    private Timer timer = new Timer();
    private int times = 0;

    private FFmpegEncoder encoder;
    private File videoFile;
    private int HEIGHT;
    private int WIDTH;
    private int processing_result = 0;
    private int  seconds = 0;
    private int  senconds_old =0;

    private SharedPreferences settingdata;
    private SharedPreferences.Editor  editor;
    private Date date = new Date();
    private TextView screenview;
    private TextView rpmsview;
    private  TextView modestyle;
    private ProgressBar bar_wait;
    private Switch modechange;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("RPPG");

                    rPPG = new RPPG();

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    private String loadCascadeFile(File cascadeDir, int id, String filename) throws IOException {

        InputStream is = getResources().openRawResource(id);
        File cascadeFile = new File(cascadeDir, filename);
        FileOutputStream os = new FileOutputStream(cascadeFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();

        return cascadeFile.getAbsolutePath();
    }

    static {
        try {
            System.loadLibrary("avformat-55");
            System.loadLibrary("avcodec-55");
            System.loadLibrary("avutil-52");
            System.loadLibrary("swscale-2");
            System.loadLibrary("FFmpegEncoder");
        } catch (UnsatisfiedLinkError e) {
            Log.e("Error importing lib: ", e.toString());
        }
    }

    /* ACTIVITY LIFE CYCLE */

    /**
     * First method that is called when the app is started.
     * @param savedInstanceState saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//Set Screen on always
        setContentView(R.layout.activity_main);


        settingdata = getSharedPreferences("userdatas",MODE_PRIVATE);
        int testmode_input = settingdata.getInt("testmode",0);
        String age_input = settingdata.getString("age"," ");
        String sleep_input = settingdata.getString("sleep"," ");
        boolean sexstr = settingdata.getBoolean("sex",true);
        editor = settingdata.edit();

        timer1();//start time count;

        // Clear directory from old log files
        try {
            FileUtils.deleteDirectory(getApplicationContext().getExternalFilesDir(null));
        } catch (IOException e) {
            Log.e(TAG, "Exception while clearing directory: " + e);
        }


        // Set the user interface layout for this Activity
        // The layout file is defined in the project res/layout/activity_main.xml file


        // Wire up the camera view
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        forebackground = (ImageView) findViewById(R.id.imageView3);
        forebackground.setVisibility(View.VISIBLE);
        bar_wait = (ProgressBar) findViewById(R.id.progressBar);
        bar_wait.setAlpha(0.0f);

        screenview = (TextView) findViewById(R.id.textView19) ;
        rpmsview = (TextView) findViewById(R.id.textView20);
        modestyle = (TextView) findViewById(R.id.modestyle);
        modechange = (Switch) findViewById(R.id.switch1);

        queue = RPPGResultQueue.getInstance();
        if (testmode_input==1){
            modestyle.setText("运动模式");
            modechange.setChecked(true);
        }else{
            modestyle.setText("标准模式");
            modechange.setChecked(false);
        }
        if(age_input==" "||sleep_input==" ") {
            rppg_En = false;
            AlertDialog alertDialog = new AlertDialog.Builder(Main.this).create();
            alertDialog.setTitle("提示！");
            alertDialog.setMessage("请先完善您的基本信息！");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            editor.putInt("enter",1);
                            editor.commit();
                            Intent intent = new Intent(Main.this, setting.class);
                            startActivity(intent);
                            Main.this.finish();
                        }
                    });
            alertDialog.show();
        }else {
            rppg_En = true;
        }

        // Initialise the video file and encoder
        if (VIDEO) {
            videoFile = new File(getApplicationContext().getExternalFilesDir(null), "Android_ffmpeg.mkv");
            encoder = new FFmpegEncoder();
        }

        // Setup network button
        Button networkClientButton = (Button)findViewById(R.id.btnNetworkClient);
        networkClientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Network Button clicked!");
                editor.putInt("enter",1);
                editor.commit();
                Intent intent = new Intent(Main.this, setting.class);
                startActivity(intent);
                Main.this.finish();
            }
        });

        modechange.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int TestMode;
                if (isChecked){
                    TestMode = 1;
                    modestyle.setText("运动模式");
                }else {
                    TestMode = 0;
                    modestyle.setText("标准模式");
                }
                editor.putInt("testmode",TestMode);
                editor.commit();
            }
        });
    }

    /**
     * Called when app is paused.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    /**
     * Called when app resumes from a pause.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /* Helper methods */


    /* CvCameraViewListener2 methods */

    /**
     * Called when the camera view starts
     * @param width - the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    public void onCameraViewStarted(int width, int height) {

        // Set up Mats .Cache the data entered by the camera for each frame
        mGray = new Mat();
        mRgba = new Mat();
        HEIGHT = height;
        WIDTH = width;


        // Prepare FFmpegEncoder
        if (VIDEO) {
            if (!encoder.openFile(videoFile.getAbsolutePath(), width, height, VIDEO_BITRATE, 30)) {
                Log.e(TAG, "Encoder failed to open");
            } else {
                Log.i(TAG, "Encoder loaded successfully");
            }
        }

        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);

        // Initialise rPPG

        try {
            rPPG.load(this, ALGORITHM, width, height,OFFSET,MIN_LIGHT,MAX_LIGHT, MIN_VARIANCE, TIME_BASE, 1,
                    SAMPLING_FREQUENCY, RESCAN_FREQUENCY, MIN_SIGNAL_SIZE, MAX_SIGNAL_SIZE,
                    getApplicationContext().getExternalFilesDir(null).getAbsolutePath(),
                    loadCascadeFile(cascadeDir, R.raw.haarcascade_frontalface_alt, "haarcascade_frontalface_alt.xml"),
                    LOG, GUI);
            Log.i(TAG, "Loaded rPPG");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }

        cascadeDir.delete();
    }

    /**
     * Called for processing of each camera frame
     * @param inputFrame - the delivered frame
     * @return mRgba
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // Retrieve timestamp
        // This is where the timestamp for each video frame originates
        time = System.currentTimeMillis();

        mRgba.release();
        mGray.release();

        final ImageView forebackgrounds = (ImageView) findViewById(R.id.imageView3);

        // Get RGBA and Gray versions
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();


        // Write frame to video
        if (VIDEO) {
            encoder.writeFrame(mRgba.dataAddr(), time);
        }
        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向

        //opencv 不支持竖屏显示，这里切换一下//
//        if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
//            Core.transpose(mRgba,mRgba);
//            Core.flip(mRgba,mRgba,-1);
//            mRgba = mRgba.submat(210,750,0,HEIGHT);
//            Imgproc.resize(mRgba,mRgba,new Size(WIDTH,HEIGHT));
//
//            Core.transpose(mGray,mGray);
//            Core.flip(mGray,mGray,-1);
//            mGray = mGray.submat(210,750,0,HEIGHT);
//            Imgproc.resize(mGray,mGray,new Size(WIDTH,HEIGHT));
//        }
        try {
            // 创建指定路径的文件
            File file = new File(Environment.getExternalStorageDirectory(), "references.txt");
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
            bw.write(String.valueOf(WIDTH)+"WIDTH");
            bw.write(String.valueOf(HEIGHT)+"HEIGHT");
            // 最后关闭文件输出流
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        processing_result = rPPG.processFrame(mRgba.getNativeObjAddr(), mGray.getNativeObjAddr(), time); //返回参数 1：未识别到人脸；2：人脸不在框内；3：光线问题,4:额头有遮盖；0：正常；

        if(rppg_En){
            if (processing_result == 5) {
                //调整背景颜色
                if(!ProgressBar_En){
                    bar_wait.setAlpha(0.0f);
                    ProgressBar_En =true;
                }
                forebackground.setAlpha(0.0f);

                seconds = times;
                String hold_on = "倒计时:" + String.valueOf(15-seconds)+"s";
                screenview.setText(hold_on);


                senconds_old=seconds;
                if (seconds >= 15) {
                    try {
                        timer.cancel();
                        SimpleDateFormat sdf_now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String std_now = sdf_now.format(date);
                        editor.putString("store_time",std_now);
                        editor.commit();

                        double result_value = rPPG.outputreference();
                        Intent intent = new Intent(Main.this, bodytemperature.class);
                        intent.putExtra("rppg",result_value);
                        startActivity(intent);
                        Main.this.finish();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }else {
                forebackground.setAlpha(1.0f);
                rpmsview.setText(" ");

                times = 0;
                if (processing_result == 2 || processing_result == 1){
                    if(!ProgressBar_En){
                        bar_wait.setAlpha(0.0f);
                        ProgressBar_En =true;
                    }
                    screenview.setText("请把面部保持在白色轮廓内");
                }else if(processing_result == 3){
                    if(!ProgressBar_En){
                        bar_wait.setAlpha(0.0f);
                        ProgressBar_En =true;
                    }
                    screenview.setText("请调整面部光照");
                }else if(processing_result == 4){
                    if(!ProgressBar_En){
                        bar_wait.setAlpha(0.0f);
                        ProgressBar_En =true;
                    }
                    screenview.setText("请露出额头部分");
                }
                else {
                    if(ProgressBar_En){
                        bar_wait.setAlpha(1.0f);
                        ProgressBar_En =false;
                    }
                    screenview.setText("准备开始测量");
                }
            }
        }

        return mRgba;
    }

    /**
     * Called when the camera view stops
     */
    public void onCameraViewStopped() {

        if (VIDEO) {
            encoder.closeFile();
        }

        // Release resources
        mGray.release();
        mRgba.release();
        rPPG.exit();
    }

    /* PRRGListener methods */

    /**
     * Called when a result from the HRM is delivered
     * Called from C++
     * @param result the RPPGResult
     */
    public void onRPPGResult(RPPGResult result) {
        Log.i(TAG, "sRPPGResult: " + result.getTime() + " – " + result.getMean());
    }



    public  void timer1() {
        timer.schedule(new TimerTask() {
            public void run() {
                times ++;
            }
        },10,1000);
    }
}