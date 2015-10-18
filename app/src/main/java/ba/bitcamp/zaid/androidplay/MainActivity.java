package ba.bitcamp.zaid.androidplay;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.test.UiThreadTest;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private EditText routeInput;
    private Button fetchButton;
    private TextView statusText;
    private TextView responseText;
    private String request;
    private RadioButton getButton;
    private RadioButton postButton;
    private boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        routeInput = (EditText) findViewById(R.id.route);
        fetchButton = (Button) findViewById(R.id.fetch);
        statusText = (TextView) findViewById(R.id.status);
        responseText = (TextView) findViewById(R.id.response);
        getButton = (RadioButton) findViewById(R.id.get);
        postButton = (RadioButton) findViewById(R.id.post);

        fetchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager connMgr = (ConnectivityManager)
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (!(networkInfo != null && networkInfo.isConnected())) {
                    Toast.makeText(MainActivity.this, R.string.no_network, Toast.LENGTH_LONG).show();
                    return;
                }

                if (running) {
                    Toast.makeText(MainActivity.this, R.string.already_running, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!(getButton.isChecked() || postButton.isChecked())) {
                    Toast.makeText(MainActivity.this, R.string.no_button_selected, Toast.LENGTH_LONG).show();
                } else {
                    request = getButton.isChecked()? "GET" : "POST";

                    statusText.setText("");
                    responseText.setText("");

                    FetchWebData task = new FetchWebData();
                    task.execute(routeInput.getText().toString());
                }
            }
        });
    }

    private class FetchWebData extends AsyncTask<String, Void, String> {
        private String status = "";

        @Override
        protected String doInBackground(String... urls) {
            running = true;
            InputStreamReader reader = null;

            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod(request);
                conn.setDoInput(true);

                conn.connect();
                int response = conn.getResponseCode();
                status = getString(R.string.response) + ": " + response + ", ";
                reader = new InputStreamReader(conn.getInputStream(), "UTF-8");

                String content = "";
                char[] buffer = new char[1024];
                int readBytes;
                while ( (readBytes = reader.read(buffer, 0, buffer.length)) > 0) {
                    for (int i = 0; i < readBytes; i++) {
                        content += buffer[i];
                    }
                }

                return content;
            } catch (MalformedURLException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.malformed_URL, Toast.LENGTH_LONG).show();
                    }
                });
                e.printStackTrace();
            } catch (IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.ioexception, Toast.LENGTH_LONG).show();
                    }
                });
                e.printStackTrace();
            }

            return getString(R.string.problem_reading);
        }

        @Override
        protected void onPostExecute(String res) {
            try {
                JSONObject json = new JSONObject(res);

                status += " " + getString(R.string.is_it_json) + " " + getString(R.string.is_json);
            } catch (JSONException e) {
                try {
                    JSONArray json = new JSONArray(res);
                    status += " " + getString(R.string.is_it_json) + " " + getString(R.string.is_json);
                } catch (JSONException e1) {
                    e.printStackTrace();
                    status += " " + getString(R.string.is_it_json) + " " + getString(R.string.not_json);
                }

            }

            final String result = res;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    statusText.setText(status);
                    responseText.setText(result);
                }
            });

            running = false;
        }
    }
}
