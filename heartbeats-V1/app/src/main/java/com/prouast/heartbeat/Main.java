package com.prouast.heartbeat;

import org.apache.commons.io.FileUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.MatOfRect;
import org.opencv.imgproc.Imgproc;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import android.view.View;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import static org.opencv.imgproc.Imgproc.putText;

/**
 * The main class.
 */
public class Main extends AppCompatActivity implements CvCameraViewListener2, RPPG.RPPGListener, RPPGNetworkClient.NetworkClientStateListener {

    /* Settings */
    private static final RPPG.RPPGAlgorithm ALGORITHM = RPPG.RPPGAlgorithm.g;
    private static  double SAMPLING_FREQUENCY = 1;
    private static  double RESCAN_FREQUENCY = 1;
    private static final double TIME_BASE = 0.001;
    private static  int MIN_SIGNAL_SIZE = 5;
    private static  int MAX_SIGNAL_SIZE = 5;
    private static  int OFFSET = 50;
    private static  int MIN_LIGHT = 70;
    private static  int MAX_LIGHT = 150;
    private static final boolean LOG = false;
    private static final boolean VIDEO = false;
    private static final boolean GUI = true;
    private static final int VIDEO_BITRATE = 100000;
    private boolean Image_En = true;

    /* Constants */
    private static final String TAG = "Heartbeat::Main";
    private static final Pattern PARTIAl_IP_ADDRESS =
            Pattern.compile("^((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])\\.){0,3}"+
                    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])){0,1}$");

    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView forebackground;
    private RPPG rPPG;
    private RPPGResultQueue queue;
    private Mat mRgba;
    private Mat mGray;
    private Mat mCache90Mat;
    private Mat Matlin;
    private Mat gMatlin;
    private Bitmap mCacheBitmap;
    private Size mNarrowSize;
    private long time;
    private Timer timer = new Timer();
    private int times = 0;

    private FFmpegEncoder encoder;
    private File videoFile;
    private int HEIGHT;
    private int WIDTH;
    private int count_validface = 0;
    private int processing_result = 0;
    private double BPMS = 0.0;
    private double FPS = 0.0;
    private int  seconds = 0;
    private int  senconds_old =0;
    private int  constant_fps = 1;
    private List<String> list_bpms = new ArrayList<>();

    private RPPGNetworkClient client = null;
    private String serverAddress = null;
    private boolean clientConnected = false;

    private SharedPreferences settingdata;
    private SharedPreferences.Editor  editor;
    private Date date = new Date();
    private TextView screenview;


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

        settingdata = getSharedPreferences("userdatas",MODE_PRIVATE);
        editor = settingdata.edit();

        MIN_SIGNAL_SIZE = settingdata.getInt("min_singal_size",5);
        MAX_SIGNAL_SIZE = settingdata.getInt("max_singal_size",5);
        OFFSET = settingdata.getInt("offset",50);
        MIN_LIGHT = settingdata.getInt("min_light",70);
        MAX_LIGHT = settingdata.getInt("max_light",150);

        float sample_f= settingdata.getFloat("sample_frequency",1);
        BigDecimal sample = new BigDecimal(String.valueOf(sample_f));
        SAMPLING_FREQUENCY = sample.doubleValue();
        float rescan_f = settingdata.getFloat("rescan_frequency",1);
        BigDecimal rescan = new BigDecimal(String.valueOf(rescan_f));
        RESCAN_FREQUENCY = rescan.doubleValue();


        timer1();//start time count;

        // Clear directory from old log files
        try {
            FileUtils.deleteDirectory(getApplicationContext().getExternalFilesDir(null));
        } catch (IOException e) {
            Log.e(TAG, "Exception while clearing directory: " + e);
        }


        // Set the user interface layout for this Activity
        // The layout file is defined in the project res/layout/activity_main.xml file
        setContentView(R.layout.activity_main);

        // Wire up the camera view
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        forebackground = (ImageView) findViewById(R.id.imageView3);
        forebackground.setVisibility(View.VISIBLE);

        screenview = (TextView) findViewById(R.id.textView19) ;
        String outputs = "你好！";
        screenview.setText(outputs);




        // Initialise the Network client, result queue and Heartrate monitor
        client = new RPPGNetworkClient(this);
        queue = RPPGResultQueue.getInstance();

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
                Intent intent = new Intent(Main.this, setting.class);
                startActivity(intent);
                Main.this.finish();
//                if (clientConnected) {
//                    client.stop();
//                } else {
//                    getServerAddress();
//                }
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
        //monitor.clearHRM();
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

    /**
     * Generate an alert dialog for IP address input
     * Called when user hits the network button
     */
    private void getServerAddress() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please enter the IP address of your Brownie client!");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            private String mPreviousText = "";

            @Override
            public void afterTextChanged(Editable s) {
                if (PARTIAl_IP_ADDRESS.matcher(s).matches()) {
                    mPreviousText = s.toString();
                } else {
                    s.replace(0, s.length(), mPreviousText);
                }
            }
        });
        if (serverAddress != null) {
            input.setText(serverAddress);
        }
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                serverAddress = input.getText().toString();
                client.setServerAddress(serverAddress);
                new Thread(client).start();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

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

        try {
            // 创建指定路径的文件
            File file = new File(Environment.getExternalStorageDirectory(), "imgview.txt");
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
            String l = "Height:"+String.valueOf(HEIGHT)+"WIDTH:"+String.valueOf(WIDTH);
            bw.write(l + "\r\n");
            // 最后关闭文件输出流
            bw.close();


        } catch (Exception e) {
            e.printStackTrace();
        }

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
            rPPG.load(this, ALGORITHM, width, height,OFFSET,MIN_LIGHT,MAX_LIGHT, TIME_BASE, 1,
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
        //forebackgrounds.setVisibility(View.VISIBLE);

        // Get RGBA and Gray versions
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

//        Mat mrgba = new Mat();
//        Mat mgray = new Mat();

        // Write frame to video
        if (VIDEO) {
            encoder.writeFrame(mRgba.dataAddr(), time);
        }
        Configuration mConfiguration = this.getResources().getConfiguration(); //获取设置的配置信息
        int ori = mConfiguration.orientation; //获取屏幕方向

        if (ori == mConfiguration.ORIENTATION_PORTRAIT) {
            Core.transpose(mRgba,mRgba);
            Core.flip(mRgba,mRgba,-1);
            mRgba = mRgba.submat(210,750,0,HEIGHT);
            Imgproc.resize(mRgba,mRgba,new Size(WIDTH,HEIGHT));

            Core.transpose(mGray,mGray);
            Core.flip(mGray,mGray,-1);
            mGray = mGray.submat(210,750,0,HEIGHT);
            Imgproc.resize(mGray,mGray,new Size(WIDTH,HEIGHT));
        }


        processing_result = rPPG.processFrame(mRgba.getNativeObjAddr(), mGray.getNativeObjAddr(), time);
        BPMS = (double)(processing_result / 1000)/10;
        FPS = (double) (processing_result % 1000)/10;


        if (200.0>= BPMS && BPMS >= 10.0 && FPS >=5.0) {
//            if(forebackgrounds.isShown()){
//                forebackgrounds.setVisibility(View.GONE);
//            }
            forebackground.setAlpha(0.0f);


            seconds = times;
            String hold_on = "倒计时:" + String.valueOf(21-seconds)+"s";
            //putText(mRgba,hold_on,new Point(WIDTH/2 -200,HEIGHT-20),1,4,new Scalar(100,255,255,255),5);
            screenview.setText(hold_on);
            if(senconds_old!=seconds){
                list_bpms.add(String.valueOf(BPMS));
            }

            senconds_old=seconds;
            count_validface ++;
            if (seconds >= 21) {
                try {
                    // 创建指定路径的文件
                    File file = new File(Environment.getExternalStorageDirectory(), "bpms.txt");
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
                    for (String l:list_bpms){
                        bw.write(l + "\r\n");
                    }
                    // 最后关闭文件输出流
                    bw.close();
//                    onCameraViewStopped();
                    timer.cancel();

                    SimpleDateFormat sdf_now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String std_now = sdf_now.format(date);
                    editor.putString("store_time",std_now);
                    editor.commit();

                    Intent intent = new Intent(Main.this, bodytemperature.class);
                    startActivity(intent);
                    Main.this.finish();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }else {
            forebackground.setAlpha(1.0f);

            count_validface = 0;
            list_bpms.clear();
            times = 0;
            if (BPMS == 2 || BPMS == 1){
                screenview.setText("请把面部保持在白色轮廓内");
            }else if(BPMS == 3){
                screenview.setText("请调整面部光照");
            }else {
                screenview.setText("准备开始测量");
            }
        }



        return mRgba;
    }

    /**
     * Called when the camera view stops
     */
    public void onCameraViewStopped() {

        // Stop the client
        if (client.isActive) {
            client.stop();
        }

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

        // Push the result to the queue
        if (client.isActive) {
            queue.push(result);
        }
        Log.i(TAG, "RPPGResult: " + result.getTime() + " – " + result.getMean());
    }

    /* NetworkClientStateListener methods */

    /**
     * Called when the client has connected to the network
     */
    public void onNetworkConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the network button
                Button networkClientButton = (Button) findViewById(R.id.btnNetworkClient);
                networkClientButton.setTextColor(Color.parseColor("#00ff00"));
                networkClientButton.setClickable(true);
            }
        });
        clientConnected = true;
    }

    /**
     * Called when the client has been disconnected from the network
     */
    public void onNetworkDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the network button
                Button networkClientButton = (Button) findViewById(R.id.btnNetworkClient);
                networkClientButton.setTextColor(Color.parseColor("#ff0000"));
                networkClientButton.setClickable(true);
            }
        });
        clientConnected = false;

        // Stop requested from Brownie
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Called when there was an exception while connecting to the network
     */
    public void onNetworkConnectException() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the network button
                Button networkClientButton = (Button) findViewById(R.id.btnNetworkClient);
                networkClientButton.setTextColor(Color.parseColor("#ff0000"));
                networkClientButton.setClickable(true);
                // Display an alert
                AlertDialog alertDialog = new AlertDialog.Builder(Main.this).create();
                alertDialog.setTitle("Brownie");
                alertDialog.setMessage("Could not establish a connection to Brownie server. Please check that the server is running and IP address is correct!");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });
    }

    /**
     * Called when the client has started trying to connect to the network
     */
    public void onNetworkTryingToConnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update the network button
                Button networkClientButton = (Button) findViewById(R.id.btnNetworkClient);
                networkClientButton.setClickable(false);
            }
        });
    }


    public  void timer1() {
        timer.schedule(new TimerTask() {
            public void run() {
                times ++;
            }
        },10,1000);
    }
}