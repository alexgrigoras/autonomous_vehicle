/**
 * Test Mode Screen
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
import android.os.AsyncTask;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Test extends AppCompatActivity {

    /** Variables */

    // Message to send to other activity
    public final static String EXTRA_MESSAGE = "com.example.TDB.MESSAGE";
    String message = "";

    // Global variables to send over Bluetooth
    int speed = 0;
    int drive = 1;
    int steering = 40;

    // Global variable to receive from Bluetooth
    byte[] bt_read = new byte[256];
    String bt_read_string = "";

    // Errors received
    String received_errors = "";
    int errors;

    // Timers
    Timer timerBTsend;
    Test.MyTimerTaskBT TimerTaskBTsend;
    Timer timerBTreceive;
    Test.MyTimerTaskBT2 TimerTaskBTreceive;

    // Buttons
    Button btnConnection, btnBack;

    // Text View
    TextView battery_text, speed_text, distance_text, wheels_text, direction_text,
            connection_text, parameters;

    // SeekBar
    SeekBar steering_slider, throttle_slider;

    // Bluetooth request/connection
    private static final int request_activation = 1;
    private static final int request_connection = 2;

    // Bluetooth adapter
    public BluetoothAdapter myBluetooth = null;
    public BluetoothDevice myDevice = null;
    public BluetoothSocket mySocket = null;

    // Connection variable
    boolean connection = false;

    // SeekBar progress
    //int originalProgress1;
    int originalProgress2;

    // Bluetooth MAC
    private static String MAC = null;
    // MAC = 20:16:02:14:54:75

    // Images
    ImageView batt_error, speed_error, ultras_error, gyro_error;

    // SPP UUID
    UUID my_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    /** End Variables */

    // On Create Activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

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
        parameters = (TextView) findViewById(R.id.parameters);

        // SeekBar
        steering_slider = (SeekBar) findViewById(R.id.steering_slider);
        throttle_slider = (SeekBar) findViewById(R.id.throttle_slider);

        // Buttons
        btnConnection = (Button)findViewById(R.id.btnConnection);
        btnBack = (Button)findViewById(R.id.btnBack);

        // ImageView
        batt_error = (ImageView)findViewById(R.id.batt_error);
        speed_error = (ImageView)findViewById(R.id.speed_error);
        ultras_error = (ImageView)findViewById(R.id.ultras_error);
        gyro_error = (ImageView)findViewById(R.id.gyro_error);

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
                    Intent openList = new Intent(Test.this, DeviceList.class);
                    startActivityForResult(openList, request_connection);
                }
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
                            // btnConnection.setBackground(getResources().getDrawable(R.drawable.bluetooth_icon));
                            Toast.makeText(getApplicationContext(), "BT has been disconnected!", Toast.LENGTH_LONG).show();
                        } catch (IOException error) {
                            Toast.makeText(getApplicationContext(), "An error appeared! " + error, Toast.LENGTH_LONG).show();
                        }
                    }
                    Intent menu = new Intent(Test.this, Menu.class);
                    menu.putExtra(EXTRA_MESSAGE, message);
                    startActivity(menu);
                    return false;
                }
                return false;
            }
        });
        // Slider modify action
        steering_slider.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if(fromTouch == true){
                    steering = steering_slider.getProgress();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                steering = 40;
                seekBar.setProgress(40);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
        });
        throttle_slider.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if(fromTouch == true){
                    // only allow changes by 1 up or down
                    if ((progress > (originalProgress2+50)) || (progress < (originalProgress2-50))) {
                        seekBar.setProgress(originalProgress2);
                    } else {
                        originalProgress2 = progress;
                        if (seekBar.getProgress() == 150) {
                            drive = 1;
                        } else if (seekBar.getProgress() > 100) {
                            drive = 0;
                            speed = seekBar.getProgress() - 100;
                        } else {
                            drive = 2;
                            speed = (100 - seekBar.getProgress());
                        }
                    }
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                drive = 1;
                speed = 0;
                seekBar.setProgress(100);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                originalProgress2 = seekBar.getProgress();
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
                                S = speed (0-200)
                                A = accelerate/idle/reverse
                                S = start/stop
                             */
                            mySocket.getOutputStream().write( ("2|".toString() + Integer.toString(steering) + "|".toString() + Integer.toString(speed) + "|".toString() + Integer.toString(drive) + "|0*".toString()).getBytes());
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
                                speed_text.setText(bt_read_string.substring(3, 5) + " km/h");
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
                        TimerTaskBTsend = new Test.MyTimerTaskBT();
                        timerBTsend.scheduleAtFixedRate(TimerTaskBTsend, 0, 40);

                        if(timerBTreceive != null){
                            timerBTreceive.cancel();
                            timerBTreceive = null;
                        }
                        //re-schedule timer
                        timerBTreceive = new Timer();
                        TimerTaskBTreceive = new Test.MyTimerTaskBT2();
                        timerBTreceive.scheduleAtFixedRate(TimerTaskBTreceive, 40, 200);

                    } catch (IOException error) {
                        connection = false;
                        Toast.makeText(getApplicationContext(), "An error appeared! " + error, Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Failed to get mac!", Toast.LENGTH_LONG).show();
                }
        }
    }

    // Ovveride back button press to do nothing
    @Override
    public void onBackPressed() {
    }

}