package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    static final int SERVER_PORT = 10000;

    static final String url = "content://edu.buffalo.cse.cse486586.groupmessenger1.provider";
    static final Uri p_uri = Uri.parse(url);

    static int sequence = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText1 = (EditText) findViewById(R.id.editText1);

        Button sendButton = (Button)findViewById(R.id.button4);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText1.getText().toString() + "\n";
                editText1.setText(""); // This is one way to reset the input box.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT0);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT1);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT2);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT3);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT4);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            while(true) {
                try {
                    Socket serverListener = serverSocket.accept();
                    InputStream inStream = serverListener.getInputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
                    String message = in.readLine();

                    ContentValues keyValueToInsert = new ContentValues();

                    keyValueToInsert.put("key", Integer.toString(sequence++));
                    keyValueToInsert.put("value", message);

                    getContentResolver().insert(p_uri, keyValueToInsert);
                    publishProgress(message);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //return null;
        }

        protected void onProgressUpdate(String... msg) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = msg[0].trim();
            TextView textView1 = (TextView) findViewById(R.id.textView1);
            textView1.append(strReceived + "\t\n");
            Log.v("message recieved : ", strReceived);
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {

                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(msgs[1]));

                String msgToSend = msgs[0];

                OutputStream outStream = socket.getOutputStream();
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outStream)));
                out.println(msgToSend);
                out.flush();
                socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }

}
