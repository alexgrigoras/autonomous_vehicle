/**
 * Manual Control Screen
 */

package com.example.dreambuilders.tdbremote;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /** Variables */

    // Message to send to other activity
    public final static String EXTRA_MESSAGE = "com.example.TDB.MESSAGE";
    String message = "";

    // Accelerometer
    float x, y, z = 0;

    // Global variables to send over Bluetooth
    int gear = 0;
    int drive = 1;

    // Global variable to receive from Bluetooth
    byte[] bt_read = new byte[256];
    String bt_read_string = "";

    // Errors received
    String received_errors = "";
    int errors;

    // Timers
    Timer timerBTsend;
    MainActivity.MyTimerTaskBT TimerTaskBTsend;
    Timer timerBTreceive;
    MainActivity.MyTimerTaskBT2 TimerTaskBTreceive;

    // Buttons
    Button btnConnection, btnAccel, btnBrake, btnBack, btnReverse;

    // Text View
    TextView battery_text, speed_text, distance_text, wheels_text, direction_text,
                connection_text, gear_text, parameters;

    // SeekBar
    SeekBar gear_slider;

    // Bluetooth request/connection
    private static final int request_activation = 1;
    private static final int request_connection = 2;

    // Bluetooth adapter
    public BluetoothAdapter myBluetooth = null;
    public BluetoothDevice myDevice = null;
    public BluetoothSocket mySocket = null;

    // MOTION
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // Connection variable
    boolean connection = false;

    // SeekBar progress
    int originalProgress;

    // Bluetooth MAC
    private static String MAC = null;

    // Images
    ImageView steering_wheel, batt_error, speed_error, ultras_error, gyro_error;

    // SPP UUID
    UUID my_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    /** End Variables */

    // On Create Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Get user name
        Intent intent = getIntent();
        message = intent.getStringExtra(EXTRA_MESSAGE);

        // Set Full Screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // TextView
        battery_text = (TextView) findViewById(R.id.battery_text);
        speed_text = (TextView) findViewById(R.id.speed_text);
        distance_text = (TextView) findViewById(R.id.distance_text);
        wheels_text = (TextView) findViewById(R.id.wheels_text);
        direction_text = (TextView) findViewById(R.id.direction_text);
        connection_text = (TextView) findViewById(R.id.connection_text);
        gear_text = (TextView) findViewById(R.id.gear_text);
        parameters = (TextView) findViewById(R.id.parameters);

        // SeekBar
        gear_slider = (SeekBar) findViewById(R.id.gear_slider);

        // Buttons
        btnConnection = (Button)findViewById(R.id.btnConnection);
        btnAccel = (Button)findViewById(R.id.btnAccel);
        btnBrake = (Button)findViewById(R.id.btnBrake);
        btnBack = (Button)findViewById(R.id.btnBack);

        // ImageView
        steering_wheel = (ImageView)findViewById(R.id.steering_wheel);
        batt_error = (ImageView)findViewById(R.id.batt_error);
        speed_error = (ImageView)findViewById(R.id.speed_error);
        ultras_error = (ImageView)findViewById(R.id.ultras_error);
        gyro_error = (ImageView)findViewById(R.id.gyro_error);

        //MOTION START - Accelerometer
        //get the sensor service
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //get the accelerometer sensor
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //MOTION END

        // Bluetooth state
        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        // Check BL state
        if(myBluetooth == null){
            Toast.makeText(getApplicationContext(), "Dispozitivul nu suporta Bluetooth!", Toast.LENGTH_LONG).show();
        } else if(!myBluetooth.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, request_activation);
        }

        // Button touch action
        btnConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connection) {
                    // disconnect
                    try {
                        mySocket.close();
                        connection = false;
                        btnConnection.setBackground(getResources().getDrawable(R.drawable.bluetooth_icon));
                        Toast.makeText(getApplicationContext(), "BT has been disconnected!", Toast.LENGTH_LONG).show();
                        connection_text.setText("None");
                        connection_text.setTextColor(Color.RED);
                        timerBTsend.cancel();
                        timerBTreceive.cancel();
                    } catch(IOException error) {
                        Toast.makeText(getApplicationContext(), "An error appeared! " + error, Toast.LENGTH_LONG).show();
                    }
                } else {
                    // connect
                    Intent openList = new Intent(MainActivity.this, DeviceList.class);
                    startActivityForResult(openList, request_connection);
                }
            }
        });

        btnAccel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    btnAccel.setBackground(getResources().getDrawable(R.drawable.throttle_press));
                    drive = 2;
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    btnAccel.setBackground(getResources().getDrawable(R.drawable.throttle));
                    drive = 1;
                    return false;
                }
                return false;
            }
        });

        btnBrake.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    btnBrake.setBackground(getResources().getDrawable(R.drawable.brake_press));
                    drive = 0;
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    btnBrake.setBackground(getResources().getDrawable(R.drawable.brake));
                    drive = 1;
                    return false;
                }
                return false;
            }
        });

        btnBack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    btnBack.setBackground(getResources().getDrawable(R.drawable.back_icon_press));
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    btnBack.setBackground(getResources().getDrawable(R.drawable.back_icon));
                    if(connection) {
                        try {
                            mySocket.close();
                            connection = false;
                            Toast.makeText(getApplicationContext(), "BT has been disconnected!", Toast.LENGTH_LONG).show();
                        } catch (IOException error) {
                            Toast.makeText(getApplicationContext(), "An error appeared! " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                    Intent menu_back = new Intent(MainActivity.this, Menu.class);
                    menu_back.putExtra(EXTRA_MESSAGE, message);
                    startActivity(menu_back);
                    return false;
                }
                return false;
            }
        });

        // Slider modify action
        gear_slider.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if(fromTouch == true){
                    // only allow changes by 1 up or down
                    if ((progress > (originalProgress+3)) || (progress < (originalProgress-3))) {
                        seekBar.setProgress( originalProgress);
                    } else {
                        originalProgress = progress;
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                gear = gear_slider.getProgress();
                gear_text.setText(gear + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                originalProgress = seekBar.getProgress();
            }
        });

    }

    // BT send data periodically
    class MyTimerTaskBT extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    if (mySocket!=null && connection) {
                        try {
                            /*
                                M|W|G|A|S*
                                M = mode = autonomous/manual
                                W = wheel position - x*(-10)+90
                                G = gear (0-5, reverse)
                                A = accelerate/brake
                                S = start/stop
                             */
                            mySocket.getOutputStream().write( ("0|".toString() + Integer.toString((int)((-x+10)*4)) + "|".toString() + Integer.toString(gear*15) + "|".toString() + Integer.toString(drive) + "|0*".toString()).getBytes());
                        } catch (IOException e) {
                            try {
                                mySocket.close();
                                connection = false;
                                btnConnection.setBackground(getResources().getDrawable(R.drawable.bluetooth_icon));
                                Toast.makeText(getApplicationContext(), "BT has been disconnected!", Toast.LENGTH_LONG).show();
                                connection_text.setText("None");
                                connection_text.setTextColor(Color.RED);
                                timerBTsend.cancel();
                                timerBTreceive.cancel();
                            }
                            catch(IOException error) {
                                Toast.makeText(getApplicationContext(), "An error appeared! " + error, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }});
        }

    }

    // BT receive data periodically
    class MyTimerTaskBT2 extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    if (mySocket!=null && connection) {
                        try {
                            /*
                                B|S|D|W|F|A|E*
                                B = Battery
                                S = Speed
                                D = Distance
                                W = Wheels Position
                                F = Direction
                                A = Autonomous Stop
                                E = Errors
                            */

                            if(mySocket.getInputStream().available() != 0) {
                                mySocket.getInputStream().read(bt_read);
                            }

                            bt_read_string = new String(bt_read);
                            bt_read_string = bt_read_string.substring(0, 18);
                            if(bt_read_string.length() > 0 && bt_read_string.startsWith("*") && bt_read_string.endsWith("*")) {
                                battery_text.setText(bt_read_string.substring(1, 3) + " %");
                                speed_text.setText(bt_read_string.substring(3, 5) + " m/s");
                                distance_text.setText(bt_read_string.substring(5, 9) + " cm");
                                wheels_text.setText(bt_read_string.substring(9, 12) + " °");
                                direction_text.setText(bt_read_string.substring(12, 14));
                                received_errors = bt_read_string.substring(15, 17);
                                errors = Integer.parseInt(received_errors);

                                if(errors % 2 == 1) {
                                    gyro_error.setVisibility(View.VISIBLE);
                                } else {
                                    gyro_error.setVisibility(View.INVISIBLE);
                                }
                                if((errors/2) % 2 == 1) {
                                    ultras_error.setVisibility(View.VISIBLE);
                                } else {
                                    ultras_error.setVisibility(View.INVISIBLE);
                                }
                                if((errors/4) % 2 == 1) {
                                    speed_error.setVisibility(View.VISIBLE);
                                } else {
                                    speed_error.setVisibility(View.INVISIBLE);
                                }
                                if((errors/8) % 2 == 1) {
                                    batt_error.setVisibility(View.VISIBLE);
                                } else {
                                    batt_error.setVisibility(View.INVISIBLE);
                                }
                            }


                        } catch (IOException e) {
                            try {
                                mySocket.close();
                                connection = false;
                                btnConnection.setBackground(getResources().getDrawable(R.drawable.bluetooth_icon));
                                Toast.makeText(getApplicationContext(), "BT has been disconnected!", Toast.LENGTH_LONG).show();
                                connection_text.setText("None");
                                connection_text.setTextColor(Color.RED);
                                timerBTsend.cancel();
                                timerBTreceive.cancel();
                            }
                            catch(IOException error) {
                                Toast.makeText(getApplicationContext(), "An error appeared! " + error, Toast.LENGTH_LONG).show();
                            }
                        } catch (NumberFormatException e) {
                            errors = 0;
                        }
                    }

                }});
        }

    }

    // Receive Bluetooth Mac from DeviceList Activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case request_activation:
                if(resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "Bluetooth a fost activat!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth NU a fost activat!", Toast.LENGTH_LONG).show();
                    //finish();
                }
                break;
            case request_connection:
                if(resultCode == Activity.RESULT_OK) {
                    MAC = data.getExtras().getString(DeviceList.Address_MAC);
                    //Toast.makeText(getApplicationContext(), "Mac final: " + MAC, Toast.LENGTH_LONG).show();
                    myDevice = myBluetooth.getRemoteDevice(MAC);

                    try {
                        mySocket = myDevice.createRfcommSocketToServiceRecord(my_UUID);
                        mySocket.connect();
                        connection = true;
                        btnConnection.setBackground(this.getResources().getDrawable(R.drawable.bluetooth_disconnect));
                        Toast.makeText(getApplicationContext(), "BT Connected: " + MAC, Toast.LENGTH_LONG).show();
                        connection_text.setText("Connected");
                        connection_text.setTextColor(Color.GREEN);
                        // Timer
                        if(timerBTsend != null){
                            timerBTsend.cancel();
                            timerBTsend = null;
                        }
                        //re-schedule timer
                        timerBTsend = new Timer();
                        TimerTaskBTsend = new MainActivity.MyTimerTaskBT();
                        timerBTsend.schedule(TimerTaskBTsend, 0, 40);

                        if(timerBTreceive != null){
                            timerBTreceive.cancel();
                            timerBTreceive = null;
                        }
                        //re-schedule timer
                        timerBTreceive = new Timer();
                        TimerTaskBTreceive = new MainActivity.MyTimerTaskBT2();
                        timerBTreceive.schedule(TimerTaskBTreceive, 40, 200);

                    } catch (IOException error) {
                        connection = false;
                        Toast.makeText(getApplicationContext(), "An error appeared! " + error, Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Failed to get mac!", Toast.LENGTH_LONG).show();
                }
        }
    }

    //MOTION STARTS - Accelerometer
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }
    @Override
    public final void onSensorChanged(SensorEvent event) {

        WindowManager windowMgr = (WindowManager)this.getSystemService(WINDOW_SERVICE);
        int rotationIndex = windowMgr.getDefaultDisplay().getRotation();
        if (rotationIndex == 1 || rotationIndex == 3){			// detect 90 or 270 degree rotation
            x = -event.values[1];
            y = event.values[0];
            z = event.values[2];
        }
        else{
            x = event.values[0];            //Force x axis in m s-2
            y = event.values[1];      	    //Force y axis in m s-2
            z = event.values[2];
        }

        // steering wheel rotation
        steering_wheel.setRotation(x * 9);
    }

    // Activity Resume
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // Activity Pause
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    // Ovveride back button press to do nothing
    @Override
    public void onBackPressed() {
    }

}