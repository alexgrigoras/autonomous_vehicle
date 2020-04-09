package com.example.dreambuilders.tdbremote;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Autonomous extends AppCompatActivity {

    /** Variables */

    // Message to send to other activity
    public final static String EXTRA_MESSAGE = "com.example.TDB.MESSAGE";
    String message = "";

    // Time elapsed variables
    int hour = 0;
    int minute = 0;
    int second = 0;

    // Global variable to send over Bluetooth
    int startstop = 0;  // start/stop command

    // Global variable to receive from Bluetooth
    byte[] bt_read = new byte[256];
    String bt_read_string = "";

    // Errors received
    String received_errors = "";
    int errors;

    // Slider value for second action
    int reload = 0;

    // SeekBar original progress
    int originalProgress;

    // Text View
    TextView battery_text, speed_text, distance_text, wheels_text, direction_text,
            time_elapsed_text, connection_text;

    // Timers
    Timer timerBTsend;
    Autonomous.MyTimerTaskBT TimerTaskBTsend;
    Timer timerBTreceive;
    Autonomous.MyTimerTaskBT2 TimerTaskBTreceive;
    Timer timerStart;
    MyTimerTaskStart TimerTaskStart;
    boolean timerStart_available = false;

    // Buttons
    Button btnConnection, btnBack;

    // SeekBar
    SeekBar sliderStart, sliderStop;

    // Bluetooth request/connection
    private static final int request_activation = 1;
    private static final int request_connection = 2;

    // Bluetooth adapter
    public BluetoothAdapter myBluetooth = null;
    public BluetoothDevice myDevice = null;
    public BluetoothSocket mySocket = null;

    // Connection variable
    boolean connection = false;

    // Images
    ImageView batt_error, speed_error, ultras_error, gyro_error;

    // Bluetooth MAC
    private static String MAC = null;

    // SPP UUID
    UUID my_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autonomous);

        // Get user name
        Intent intent = getIntent();
        message = intent.getStringExtra(EXTRA_MESSAGE);

        // TextView
        battery_text = (TextView) findViewById(R.id.battery_text);
        speed_text = (TextView) findViewById(R.id.speed_text);
        distance_text = (TextView) findViewById(R.id.distance_text);
        wheels_text = (TextView) findViewById(R.id.wheels_text);
        direction_text = (TextView) findViewById(R.id.direction_text);
        time_elapsed_text = (TextView) findViewById(R.id.time_elapsed_text);
        connection_text = (TextView) findViewById(R.id.connection_text);

        // Buttons
        btnConnection = (Button)findViewById(R.id.btnConnection);
        btnBack = (Button)findViewById(R.id.btnBack);

        // ImageView
        batt_error = (ImageView)findViewById(R.id.batt_error);
        speed_error = (ImageView)findViewById(R.id.speed_error);
        ultras_error = (ImageView)findViewById(R.id.ultras_error);
        gyro_error = (ImageView)findViewById(R.id.gyro_error);

        // SeekBar
        sliderStart = (SeekBar) findViewById(R.id.sliderStart);
        sliderStop = (SeekBar) findViewById(R.id.sliderStop);

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
                    Intent openList = new Intent(Autonomous.this, DeviceList.class);
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
                    Intent menu_back = new Intent(Autonomous.this, Menu.class);
                    menu_back.putExtra(EXTRA_MESSAGE, message);
                    startActivity(menu_back);
                    return false;
                }
                return false;
            }
        });

        // Slider modify action
        sliderStart.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                    if(fromTouch == true){
                        // only allow changes by 1 up or down
                        if ((progress > (originalProgress+24)) || (progress < (originalProgress-24))) {
                            seekBar.setProgress( originalProgress);
                        } else {
                            originalProgress = progress;
                        }
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if(seekBar.getProgress() > 90) {
                        reload = 0;
                        startstop = 1;

                        if (timerStart != null) {
                            timerStart.cancel();
                            timerStart = null;
                        }
                        timerStart = new Timer();
                        TimerTaskStart = new MyTimerTaskStart();
                        timerStart.schedule(TimerTaskStart, 0, 1000);
                    }
                    seekBar.setProgress(0);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    originalProgress = seekBar.getProgress();
                }
            });

        sliderStop.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                    if(fromTouch == true){
                        // only allow changes by 1 up or down
                        if ((progress > (originalProgress+24)) || (progress < (originalProgress-24))) {
                            seekBar.setProgress( originalProgress);
                        } else {
                            originalProgress = progress;
                        }
                    }
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if(seekBar.getProgress() > 90) {
                        if (timerStart_available) {
                            timerStart.cancel();
                            startstop = 0;

                            if(reload == 1) {
                                hour = 0;
                                minute = 0;
                                second = 0;
                                time_elapsed_text.setText(Integer.toString(hour) + " : " + Integer.toString(minute) + " : " + Integer.toString(second) + " s");
                            }
                            else {
                                msg("Car stopped!");
                            }

                            reload = 1;
                        }
                    }
                    seekBar.setProgress(0);
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
                            mySocket.getOutputStream().write( ("1|0|0|0|".toString() + Integer.toString(startstop) + "*".toString()).getBytes());
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
                            if(bt_read_string.length() > 0 && bt_read_string.startsWith("*")) {
                                bt_read_string = bt_read_string.substring(0, 18);
                                if(bt_read_string.endsWith("*")) {
                                    battery_text.setText(bt_read_string.substring(1, 3) + " %");
                                    speed_text.setText(bt_read_string.substring(3, 5) + " m/s");
                                    distance_text.setText(bt_read_string.substring(5, 9) + " cm");
                                    wheels_text.setText(bt_read_string.substring(9, 12) + " °");
                                    direction_text.setText(bt_read_string.substring(12, 14));

                                    if(bt_read_string.substring(14, 15).equals("1")) {
                                        msg("Car reached finnish line!");
                                        timerStart.cancel();
                                        startstop = 0;
                                    }

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

    // TimerTask class
    class MyTimerTaskStart extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable(){

                @Override
                public void run() {

                    timerStart_available = true;

                    second++;
                    if(second == 60){
                        minute ++;
                        second = 0;
                    }
                    if(minute == 60){
                        hour ++;
                        minute = 0;
                    }
                    if(hour == 24){
                        hour = 0;
                        minute = 0;
                        second = 0;
                    }

                    time_elapsed_text.setText(Integer.toString(hour) + " : " + Integer.toString(minute) + " : " + Integer.toString(second) + " s");
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

                        // Timer BT send
                        if(timerBTsend != null){
                            timerBTsend.cancel();
                            timerBTsend = null;
                        }
                        //re-schedule timer
                        timerBTsend = new Timer();
                        TimerTaskBTsend = new MyTimerTaskBT();
                        timerBTsend.schedule(TimerTaskBTsend, 0, 100);

                        // Timer BT receive
                        if(timerBTreceive != null){
                            timerBTreceive.cancel();
                            timerBTreceive = null;
                        }
                        //re-schedule timer
                        timerBTreceive = new Timer();
                        TimerTaskBTreceive = new MyTimerTaskBT2();
                        timerBTreceive.schedule(TimerTaskBTreceive, 100, 200);

                    } catch (IOException error) {
                        connection = false;
                        Toast.makeText(getApplicationContext(), "An error appeared! " + error, Toast.LENGTH_LONG).show();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "Failed to get mac!", Toast.LENGTH_LONG).show();
                }
        }
    }

    // fast way to call Toast - show a message
    public void msg(String s) {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    // Ovveride back button press to do nothing
    @Override
    public void onBackPressed() {
    }
}