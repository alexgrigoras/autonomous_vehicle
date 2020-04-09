/**
 * Menu Screen
 */

package com.example.dreambuilders.tdbremote;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class Menu extends AppCompatActivity {

    //Buttons
    Button btnTest, btnSettings, btnAuto, btnManual, btnLogout;

    // ImageView
    ImageView user;

    // Message to send to other activity
    String message = "";
    public final static String EXTRA_MESSAGE = "com.example.TDB.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Get user name
        Intent intent = getIntent();
        message = intent.getStringExtra(Login.EXTRA_MESSAGE);

        // ImageView
        user = (ImageView)findViewById(R.id.user);

        // Buttons
        btnTest = (Button)findViewById(R.id.btnTest);
        btnSettings = (Button)findViewById(R.id.btnAccel);
        btnAuto = (Button)findViewById(R.id.btnAuto);
        btnManual = (Button)findViewById(R.id.btnManual);
        btnLogout = (Button)findViewById(R.id.btnLogout);

        // Set user image
        if(message.equalsIgnoreCase("alex")){
            user.setBackground(getResources().getDrawable(R.drawable.user_alex));
        }
        else if(message.equalsIgnoreCase("guest")){
            user.setBackground(getResources().getDrawable(R.drawable.user));
        }

        // Button touch action
        btnManual.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    btnManual.setBackground(getResources().getDrawable(R.drawable.manual_mode_press));
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    btnManual.setBackground(getResources().getDrawable(R.drawable.manual_mode));
                    Intent manual = new Intent(Menu.this, MainActivity.class);
                    manual.putExtra(EXTRA_MESSAGE, message);
                    startActivity(manual);
                    return false;
                }
                return false;
            }
        });

        btnAuto.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    btnAuto.setBackground(getResources().getDrawable(R.drawable.autonomous_img_press));
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    btnAuto.setBackground(getResources().getDrawable(R.drawable.autonomous_img));
                    Intent autonom = new Intent(Menu.this, Autonomous.class);
                    autonom.putExtra(EXTRA_MESSAGE, message);
                    startActivity(autonom);
                    return false;
                }
                return false;
            }
        });

        btnTest.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    btnTest.setBackground(getResources().getDrawable(R.drawable.test_mode_press));
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    btnTest.setBackground(getResources().getDrawable(R.drawable.test_mode));
                    Intent test = new Intent(Menu.this, Test.class);
                    test.putExtra(EXTRA_MESSAGE, message);
                    startActivity(test);
                    return false;
                }
                return false;
            }
        });

        btnSettings.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    btnSettings.setBackground(getResources().getDrawable(R.drawable.settings_press));
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    btnSettings.setBackground(getResources().getDrawable(R.drawable.settings));
                    Toast.makeText(getApplicationContext(),"Not implemented yet!",Toast.LENGTH_LONG).show();
                    //Intent settings = new Intent(Menu.this, Settings.class);
                    //startActivity(settings);
                    return false;
                }
                return false;
            }
        });

        btnLogout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    btnLogout.setBackground(getResources().getDrawable(R.drawable.logout_press));
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    btnLogout.setBackground(getResources().getDrawable(R.drawable.logout));
                    Intent logout = new Intent(Menu.this, Login.class);
                    startActivity(logout);
                    return false;
                }
                return false;
            }
        });
    }

    // Ovveride back button press to do nothing
    @Override
    public void onBackPressed() {
    }
}
