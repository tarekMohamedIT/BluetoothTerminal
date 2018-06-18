package com.r3tr0.bluetoothterminal.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.r3tr0.bluetoothterminal.R;
import com.r3tr0.bluetoothterminal.adapters.ListAdapter;
import com.r3tr0.bluetoothterminal.communications.BluetoothService;
import com.r3tr0.bluetoothterminal.communications.BluetoothServiceReceiver;
import com.r3tr0.bluetoothterminal.enums.ServiceCommand;
import com.r3tr0.bluetoothterminal.enums.ServiceFlag;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    BluetoothServiceReceiver receiver;
    Intent btIntent;


    ListAdapter adapter;
    RecyclerView terminalRecyclerView;

    TextView statusTextView;
    EditText commandEditText;
    Button sendButton;
    Button readingButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    void initialize(){
        terminalRecyclerView = findViewById(R.id.terminalRecyclerView);
        statusTextView = findViewById(R.id.statusTextView);
        sendButton = findViewById(R.id.commandButton);
        readingButton = findViewById(R.id.stopReadingButton);
        commandEditText = findViewById(R.id.commandsEditText);
        adapter = new ListAdapter(this, new ArrayList<String>());
        btIntent = new Intent(this, BluetoothService.class);
        receiver = new BluetoothServiceReceiver(new BluetoothServiceReceiver.OnMessageReceivedListener() {
            @Override
            public void onMessageReceived(Object message) {
                Intent intent = (Intent) message;

                String data = intent.getStringExtra("data");
                ServiceFlag status = (ServiceFlag) intent.getSerializableExtra("status");

                if (data != null){

                    byte[] bytes = data.getBytes();

                    if (bytes.length > 7) {

                        if (!data.startsWith("TDA")) {
                            int firstPar = Integer.parseInt(data.charAt(4) + "" + data.charAt(5), 16);
                            Toast.makeText(MainActivity.this, "current fault codes : " + (firstPar), Toast.LENGTH_LONG).show();
                        }
                        /*
                        int secondPar = Integer.parseInt(data.charAt(6) + "" + data.charAt(7), 16);

                        double rpm = 0.25 * (firstPar * 256 + secondPar);
                        Toast.makeText(MainActivity.this, "current rpm : " + rpm, Toast.LENGTH_LONG).show();*/
                    }
                    adapter.getStringsList().add("from receiver : " + data);
                    adapter.notifyDataSetChanged();
                }

                if (status != null) {
                    switch (status){
                        case connected:
                            statusTextView.setText("Status : Connected");
                            break;
                        case disconnected:
                            statusTextView.setText("Status : Not connected");
                            break;
                        case connectionFailed:
                            statusTextView.setText("Status : Connection failed");
                            break;
                    }
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(statusTextView.getText().equals("Status : Connected")){
                    btIntent.putExtra("command", ServiceCommand.write);
                    btIntent.putExtra("data", commandEditText.getText().toString());
                    startService(btIntent);
                    btIntent.removeExtra("command");
                    btIntent.removeExtra("data");
                    adapter.getStringsList().add("From you : " + commandEditText.getText().toString());
                    adapter.notifyDataSetChanged();
                }
            }
        });

        readingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (readingButton.getText().equals("Stop reading")) {
                    btIntent.putExtra("command", ServiceCommand.stopReading);
                    startService(btIntent);
                    btIntent.removeExtra("command");
                    readingButton.setText("Start reading");
                    Toast.makeText(MainActivity.this, "Stopped reading", Toast.LENGTH_LONG).show();
                } else {
                    btIntent.putExtra("command", ServiceCommand.startReading);
                    startService(btIntent);
                    btIntent.removeExtra("command");
                    readingButton.setText("Stop reading");
                    Toast.makeText(MainActivity.this, "Started reading", Toast.LENGTH_LONG).show();
                }
            }
        });
        terminalRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        terminalRecyclerView.setAdapter(adapter);

        btIntent.putExtra("command", ServiceCommand.startReading);
        startService(btIntent);
        btIntent.removeExtra("command");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(BluetoothService.RECEIVER_ACTION));
        btIntent.putExtra("command", ServiceCommand.getStatus);
        startService(btIntent);
        btIntent.removeExtra("command");
    }
}
