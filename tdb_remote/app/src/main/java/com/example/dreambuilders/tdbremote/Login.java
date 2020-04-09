/**
 * Login Screen
 */

package com.example.dreambuilders.tdbremote;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.TransitionSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Login extends AppCompatActivity {

    // Buttons
    Button btnLogin;

    // Text inputs
    EditText input_name, input_password;

    // Message to send to other activity
    String message = "";
    public final static String EXTRA_MESSAGE = "com.example.TDB.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Text input
        input_name = (EditText)findViewById(R.id.input_name);
        input_password = (EditText)findViewById(R.id.input_password);

        btnLogin = (Button)findViewById(R.id.btnLogin);

        btnLogin.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    btnLogin.setBackground(getResources().getDrawable(R.drawable.login_press));
                    return true;
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    btnLogin.setBackground(getResources().getDrawable(R.drawable.login));

                    if(input_name.getText().toString().equals("alex") && input_password.getText().toString().equals("1234")) {
                        Toast.makeText(getApplicationContext(), "Welcome " + input_name.getText().toString(), Toast.LENGTH_LONG).show();
                        message = input_name.getText().toString().substring(0,4);
                    } else{
                        Toast.makeText(getApplicationContext(), "Welcome guest" , Toast.LENGTH_LONG).show();
                        message = "guest";
                    }

                    Intent menu = new Intent(Login.this, Menu.class);
                    menu.putExtra(EXTRA_MESSAGE, message);
                    startActivity(menu);

                    return false;
                }
                return false;
            }
        });
    }

}
