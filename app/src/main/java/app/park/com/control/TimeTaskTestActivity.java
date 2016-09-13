package app.park.com.control;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import app.park.com.R;

public class TimeTaskTestActivity extends Activity implements
		SensorEventListener, GestureDetector.OnGestureListener {
	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;

	long startTime = 0;
	long pauseTime = 0;
	long resumeTime = 0;
	long tempTime = 0;

	TextView textCTime;
	TextView textView;
	TextView textVelocity;

	TextView textStatus1;
	TextView textStatus2;

	Timer timer;
	Timer timer2;
	Timer timer3;
	Timer timer4;
	Timer timer5;
	Timer scenarioTimer;

	static float roll1 = 0;
	static float roll2 = 0;
	static float roll3 = 0;
	static float azimut = 0;

	static float elapsedTime = 0;
	static int velocity = 0;
	static final int VELOCITY_ACC = 1;

	static DecimalFormat valueFormat = new DecimalFormat("0.0");

	// private static final String DEBUG_TAG = "Gestures";
	// private static final boolean DEBUG = true;
	// private static final String TAG = "Gestures";
	//
	private static boolean mBooleanIsPressed = false;
	// private static boolean mBooleanIsPressed2 = false;
	private GestureDetectorCompat mDetector;

	// tilt 처리
	float[] mGravity;
	float[] mGeomagnetic;

	// 진행시간 및 시나리오 처리
	static int seconds = 0;
	static int minutes = 0;

	static boolean flag1 = false;

	
	
	
	// 시간 잴 핸들러
	Handler timerHandler = new Handler();
	Runnable timerRunnable = new Runnable() {
		public void run() {
			seconds++;
			if (seconds / 60 > 0)
				minutes++;

			seconds = seconds % 60;

			textCTime.setText(String.format("%02d:%02d", minutes, seconds));
			// 1초...
			timerHandler.postDelayed(this, 1000);

			
			// 0~3초일때 버튼A 눌렸는지 체크
			if (0 <= seconds && seconds <= 3) {
				if (mBooleanIsPressed) {
					Toast.makeText(TimeTaskTestActivity.this, "ok!", Toast.LENGTH_SHORT) .show();
				} else {
					Toast.makeText(TimeTaskTestActivity.this, "press accelator!", Toast.LENGTH_SHORT).show();
				}
			}
		}
	};
	
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
/*
		// 현재시간
		textCTime = (TextView) findViewById(R.id.textCTime);
		textCTime.setText("0.0");

		// 테스트용 텍스트뷰
		textView = (TextView) findViewById(R.id.textView);
		textView.setText("0 sec.");

		// 속도
		textVelocity = (TextView) findViewById(R.id.textVelocity);
		textVelocity.setText("0 km/h");

		// 상태
		textStatus1 = (TextView) findViewById(R.id.textStatus1);
		textStatus1.setText("go straight");

		textStatus2 = (TextView) findViewById(R.id.textStatus2);
		textStatus1.setText("");

		// tilt 처리
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		// 현재 진행시간
		startTime = System.currentTimeMillis();


		// acc btn
		Button btnAcc = (Button) findViewById(R.id.btnAcc);
		btnAcc.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// (1) 엑셀레이터 눌렀다가 떼는 이벤트 처리
				// 눌렀을때
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					mBooleanIsPressed = true;
					timer = new Timer();

					// 눌려있는 시간 체크
					timer.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							elapsedTime = (float) (elapsedTime + 0.1f); // increase
																		// every
																		// 0.1
																		// sec
							// mHandler.obtainMessage(1).sendToTarget();
							// thread 호출
							// handler.postDelayed(runnable, (int) elapsedTime);
							textView.post(new Runnable() {
								public void run() {
									textView.setText(valueFormat
											.format(elapsedTime) + " sec.");
								}
							});
							// mBooleanIsPressed = true;
						}
					}, 0, 100);

					// 속도 증가
					timer2 = new Timer();
					timer2.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							velocity += VELOCITY_ACC;
							// mHandler.obtainMessage(1).sendToTarget();
							// thread 호출
							// handler.postDelayed(runnable2, (int)
							// elapsedTime);
							textVelocity.post(new Runnable() {
								public void run() {
									textVelocity.setText(velocity + " km/h");
								}
							});
							// mBooleanIsPressed = true;
						}
					}, 0, 100);

					// 뗐을 때
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					elapsedTime = 0;
					textView.post(new Runnable() {
						public void run() {
							textView.setText("0 sec.");
						}
					});
					timer.cancel();
					timer2.cancel();
				}
				return true;
			}
		});
*/
		mDetector = new GestureDetectorCompat(this, this);
	}
	

	// public boolean dispatchTouchEvent(MotionEvent ev) {};

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;

		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
					mGeomagnetic);

			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				azimut = orientation[1]; // orientation contains: azimut, pitch
											// and roll
				roll1 = -azimut * 360 / (2 * 3.14159f);

				// timer4 = new Timer();
				// timer4.scheduleAtFixedRate(new TimerTask() {
				// public void run() {
				textStatus1.post(new Runnable() {
					public void run() {
						// textAngle1.setText(valueFormat.format(roll1) + "º");

						if (-90 < roll1 && roll1 < -30)
							textStatus1.setText("tilt left");

						if (30 < roll1 && roll1 < 90)
							textStatus1.setText("tilt right");

						if (-30 <= roll1 && roll1 <= 30)
							textStatus1.setText("go straigt");
					}
				});
				// }
				// }, 500, 1000);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	// 눌리면
	// private final Handler handler = new Handler();
	// private final Runnable runnable = new Runnable() {
	// public void run() {
	// // if(mBooleanIsPressed) {
	// textView.post(new Runnable() {
	// public void run() {
	// textView.setText(valueFormat.format(elapsedTime) + " sec.");
	// }
	// });
	// // }
	// }
	// };
	// private final Runnable runnable2 = new Runnable() {
	// public void run() {
	// textVelocity.post(new Runnable() {
	// public void run() {
	// textVelocity.setText(velocity + " km/h");
	// }
	// });
	// }
	// };

	// Register the event listener and sensor type.
	public void setListners(SensorManager sensorManager,
			SensorEventListener mEventListener) {
		sensorManager.registerListener(mEventListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(mEventListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_NORMAL);
	}


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// swipe이벤트 처리
		this.mDetector.onTouchEvent(event);
		// Be sure to call the superclass implementation
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent event) {
		// Toast.makeText(TimeTaskTestActivity.this, "onDown: ",
		// Toast.LENGTH_SHORT).show();

		// float eventDuration = event.getEventTime() - event.getDownTime();
		// Toast.makeText(TimeTaskTestActivity.this, "duration: " + eventDuration,
		// Toast.LENGTH_SHORT).show();
		return true;
	}

	@Override
	public boolean onFling(MotionEvent event1, MotionEvent event2,
			float velocityX, float velocityY) {
		// this.onScroll(event1, event2, velocityX, velocityY);
		// Toast.makeText(TimeTaskTestActivity.this, "onFling: " + event1.toString() +
		// event2.toString(), Toast.LENGTH_SHORT).show();
		return true;
	}

	@Override
	public void onLongPress(MotionEvent event) {
		// Toast.makeText(TimeTaskTestActivity.this, "onLongPress: ",
		// Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// Toast.makeText(TimeTaskTestActivity.this, "onScroll: " + e1.toString() +
		// e2.toString(), Toast.LENGTH_SHORT).show();
		// Toast.makeText(TimeTaskTestActivity.this, "onScroll: X = " + distanceX +
		// ", Y = " + distanceY, Toast.LENGTH_SHORT).show();

		// if (distanceX - distanceY > 0) {
		// // Toast.makeText(TimeTaskTestActivity.this, "turn left",
		// Toast.LENGTH_SHORT).show();
		// textStatus2.post(new Runnable() {
		// public void run() {
		// textStatus2.setText("swipe to turn left");
		// }
		// });
		// }
		// if (distanceX - distanceY < 0) {
		// // Toast.makeText(TimeTaskTestActivity.this, "turn right",
		// Toast.LENGTH_SHORT).show();
		// textStatus2.post(new Runnable() {
		// public void run() {
		// textStatus2.setText("swipe to turn right");
		// }
		// });
		// }

		return true;
	}

	@Override
	public void onShowPress(MotionEvent event) {
		// Toast.makeText(TimeTaskTestActivity.this, "onShowPress: ",
		// Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		// Toast.makeText(TimeTaskTestActivity.this, "onSingleTapUp: ",
		// Toast.LENGTH_SHORT).show();
		return true;
	}

	// @Override
	// public boolean onDoubleTap(MotionEvent event) {
	// // Toast.makeText(TimeTaskTestActivity.this, "onDoubleTap: ",
	// // Toast.LENGTH_SHORT).show();
	// return true;
	// }
	//
	// @Override
	// public boolean onDoubleTapEvent(MotionEvent event) {
	// // Toast.makeText(TimeTaskTestActivity.this, "onDoubleTapEvent: ",
	// // Toast.LENGTH_SHORT).show();
	// return true;
	// }
	//
	// @Override
	// public boolean onSingleTapConfirmed(MotionEvent event) {
	// // Toast.makeText(TimeTaskTestActivity.this, "onSingleTapConfirmed: ",
	// // Toast.LENGTH_SHORT).show();
	// return true;
	// }

	@Override
	public void onResume() {
		super.onResume();

		timerHandler.postDelayed(timerRunnable, 0);

		mSensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, magnetometer,
				SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onPause() {
		super.onPause();

		timerHandler.removeCallbacks(timerRunnable);

		mSensorManager.unregisterListener(this);
	}
}