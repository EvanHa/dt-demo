package app.park.com.control;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import app.park.com.R;

public class TimerActivity extends Activity {
    TextView text, text2, text3;
    long starttime = 0;
    //this  posts a message to the TimerActivity thread from our timertask
    //and updates the textfield
   final Handler h = new Handler(new Callback() {

        @Override
        public boolean handleMessage(Message msg) {
           long millis = System.currentTimeMillis() - starttime;
           int seconds = (int) (millis / 1000);
           int minutes = seconds / 60;
           seconds     = seconds % 60;

           text.setText(String.format("%d:%02d", minutes, seconds));
            return false;
        }
    });
   //runs without timer be reposting self
   Handler h2 = new Handler();
   Runnable run = new Runnable() {

        @Override
        public void run() {
           long millis = System.currentTimeMillis() - starttime;
           int seconds = (int) (millis / 1000);
           int minutes = seconds / 60;
           seconds     = seconds % 60;

           text3.setText(String.format("%d:%02d", minutes, seconds));

           h2.postDelayed(this, 500);
        }
    };

   //tells handler to send a message
   class firstTask extends TimerTask {

        @Override
        public void run() {
            h.sendEmptyMessage(0);
        }
   };

   //tells activity to run on ui thread
   class secondTask extends TimerTask {

        @Override
        public void run() {
            TimerActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                   long millis = System.currentTimeMillis() - starttime;
                   int seconds = (int) (millis / 1000);
                   int minutes = seconds / 60;
                   seconds     = seconds % 60;

                   text2.setText(String.format("%d:%02d", minutes, seconds));
                }
            });
        }
   };


   Timer timer = new Timer();
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timer_layout);

        text = (TextView)findViewById(R.id.text);
        text2 = (TextView)findViewById(R.id.text2);
        text3 = (TextView)findViewById(R.id.text3);
        text.setText("0:00");
        text2.setText("0:00");
        text3.setText("0:00");

        Button b = (Button)findViewById(R.id.button);
        b.setText("start");
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Button b = (Button)v;
                if(b.getText().equals("stop")){
                    timer.cancel();
                    timer.purge();
                    h2.removeCallbacks(run);
                    b.setText("start");
                }else{
                    starttime = System.currentTimeMillis();
                    timer = new Timer();
                    timer.schedule(new firstTask(), 0,500);
                    timer.schedule(new secondTask(),  0,500);
                    h2.postDelayed(run, 0);
                    b.setText("stop");
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        timer.cancel();
        timer.purge();
        h2.removeCallbacks(run);
        Button b = (Button)findViewById(R.id.button);
        b.setText("start");
    }
}


