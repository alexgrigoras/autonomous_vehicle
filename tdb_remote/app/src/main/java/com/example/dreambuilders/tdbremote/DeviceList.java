/**
 * Bluetooth Devices Screen
 */

package com.example.dreambuilders.tdbremote;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Set;

/**
 * Created by Alex on 12/22/2016.
 */

public class DeviceList extends ListActivity {

    // Bluetooth Adapter
    private BluetoothAdapter myBluetooth2 = null;

    // Mac address of Bluetooth
    static String Address_MAC = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayAdapter<String> ArrayBluetooth = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        myBluetooth2 = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = myBluetooth2.getBondedDevices();

        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                String btName = device.getName();
                String btMac = device.getAddress();
                ArrayBluetooth.add(btName + "\n" + btMac);
            }
        }
        setListAdapter(ArrayBluetooth);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String generalInformation = ((TextView) v).getText().toString();
        //Toast.makeText(getApplicationContext(), "Info: " + generalInformation, Toast.LENGTH_LONG).show();

        String addressMac = generalInformation.substring(generalInformation.length() - 17);
        //Toast.makeText(getApplicationContext(), "Mac: " + addressMac, Toast.LENGTH_LONG).show();

        Intent returnMac = new Intent();
        returnMac.putExtra(Address_MAC, addressMac);
        setResult(RESULT_OK, returnMac);
        finish();
    }
}
