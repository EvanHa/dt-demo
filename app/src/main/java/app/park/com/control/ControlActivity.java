package app.park.com.control;

import java.math.BigDecimal;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import app.park.com.R;
import app.park.com.bluetooth.BluetoothHandler;
import app.park.com.bluetooth.BluetoothService;
import app.park.com.bluetooth.Constants;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ControlActivity extends Activity implements SensorEventListener,
		GestureDetector.OnGestureListener {

	public static String TAG = "TAG";

	private GestureDetectorCompat mDetector;
	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;

	float[] mGravity;
	float[] mGeomagnetic;

	private BluetoothService mBluetoothService = null;
	private BluetoothHandler mHandler = null;
	private BluetoothHandler.ActivityCb mActivityCb = null;

	boolean isFirstAccleated = false;

	boolean doubleBackToExitPressedOnce = false;

	ImageView turn_stick;
	ImageView btnAcc;

	Timer timer;
	Timer timer2;
	Timer timer3;
	Timer timer4;
	Timer timer5;

	Timer accTimer;
	Timer brakeTimer;
	Timer handleTimer;
	Timer turnSignalTimer;



	static float roll1 = 0;
	static float azimut = 0;

	static float btnAccElapsedTime = 0;
	static float btnBrakeElapsedTime = 0;

	static final BigDecimal VELOCITY_DEFAULT = new BigDecimal("0.1");
	static final BigDecimal VELOCITY_INCREASE = new BigDecimal("0.1");
	static final BigDecimal VELOCITY_BREAK_DECREASE = new BigDecimal("0.5");
	static final BigDecimal VELOCITY_DECREASE = new BigDecimal("0.1");

	static final String BLUETHOOTH_MESSAGE_SEPARATOR = "////";
	static final String STATUS_GAMERUN = "gamerun";

	static BigDecimal velocity = VELOCITY_DEFAULT;

	private static boolean scenarioTask1 = false;
	private static boolean scenarioTask2 = false;
	private static boolean scenarioTask3 = false;
	private static boolean scenarioTask4 = false;
	private static boolean scenarioTask5 = false;
	private static boolean scenarioTask6 = false;
	private static boolean scenarioTask7 = true;
	private static boolean isAccPressed = false;
	private static boolean isBreakPressed = false;
	private static boolean exit = false;

	// 진행시간 및 시나리오 처리
	static int score = 0;
	static int seconds = 0;
	static int seconds_ = 0;
	static int minutes = 0;

	// 핸들, 브레이크, 깜빡이, 엑셀 상태값
	public static int ACC_STATUS = 0;
	public static int BRAKE_STATUS = 0;
	public static int HANDLE_STATUS = 0;
	public static int TURN_SIGNAL_STATUS = 0;
	public static int ACC_STATUS2 = 0;
	public static int BRAKE_STATUS2 = 0;
	public static int HANDLE_STATUS2 = 0;
	public static int TURN_SIGNAL_STATUS2 = 0;

	public Handler timerHandler = new Handler();
	public Runnable timerRunnable = new Runnable() {
		public void run() {
			timerHandler.postDelayed(this, 1000);
			Log.d(TAG, "velocity = " + velocity);


			// 60점 이하면
			if(!exit && score <= 60) {
				// 일단 멈추고
				onPause();

				// 다시 할지 물어봄
				new AlertDialog.Builder(ControlActivity.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle("점수가 60이하")
						.setMessage("다시 하겠습니까?")
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// 초기화
								init();
							}

						})
						.setNegativeButton("No", null)
						.show();

				exit = true;
				// 아니오 누르면 인트로 액티비티로 ㄱㄱ

			}

			seconds++;
			seconds_++;
			if(seconds_ / 60 > 0) minutes++;

			seconds_ = seconds_ % 60;


			Log.d(TAG, String.format("%02d:%02d", minutes, seconds_));

			// 13초에 task1(10~12초 사이에 엑셀 누름) 수행여부 판별하여 감점
			if(12 < seconds && seconds <= 13) {
				if(!scenarioTask1) {
					// 5점 감점
					score -= 5;
					Log.d(TAG, "엑셀 안누름");
					mBluetoothService.sendMessage("gamerun////" + velocity.doubleValue() + "////" + score);
					Log.d(TAG, "gamerun////" + velocity + "////" + score);
				}
			}

			// 23초에 task(20~22초 사이에 브레이크 누름) 수행여부 판별하여 감점
			if(22 < seconds && seconds <= 23) {
				if(!scenarioTask2) {
					// 5점 감점
					score -= 5;
					Log.d(TAG, "브레이크 안누름");
					mBluetoothService.sendMessage("gamerun////" + velocity.doubleValue() + "////" + score);
					Log.d(TAG, "gamerun////" + velocity + "////" + score);
				}
			}

			// task3, task4
			if(32 < seconds && seconds <= 33) {
				if(!scenarioTask3 || !scenarioTask4) {
					// 10점 감점
					score -= 10;
					Log.d(TAG, "왼쪽 깜빡이 또는 좌회전 안했음");
					mBluetoothService.sendMessage("gamerun////" + velocity.doubleValue() + "////" + score);
					Log.d(TAG, "gamerun////" + velocity + "////" + score);
				}
			}

			// task5, task6
			if(42 < seconds && seconds <= 43) {
				if(!scenarioTask5 || !scenarioTask6) {
					// 10점 감점
					score -= 10;
					Log.d(TAG, "오른쪽 깜빡이 또는 우회전 안했음");
					mBluetoothService.sendMessage("gamerun////" + velocity.doubleValue() + "////" + score);
					Log.d(TAG, "gamerun////" + velocity + "////" + score);
				}
			}

			// task7
			if(52 < seconds && seconds <= 53) {
				if(!scenarioTask7) {
					// 10점 감점
					score -= 10;
					Log.d(TAG, "직진 안했음");
					mBluetoothService.sendMessage("gamerun////" + velocity.doubleValue() + "////" + score);
					Log.d(TAG, "gamerun////" + velocity + "////" + score);
				}
			}
		}
	};

	// 초기화 - 시간, 점수, 속도 등
	public void init() {
		seconds = 0;
		seconds_ = 0;
		minutes = 0;
		score = 100;
		exit = false;
		velocity = VELOCITY_DEFAULT;
	}

	@Override
	public void onStart() {
		super.onStart();


		mHandler = new BluetoothHandler();
		mActivityCb = new BluetoothHandler.ActivityCb() {
			@Override
			public void sendCbMessage(int msgType, String msg) {

				switch (msgType) {
					case Constants.MESSAGE_STATE_CHANGE:
						switch (msg) {
							case Constants.BLUETOOTH_CONNECTED:
								break;
							case Constants.BLUETOOTH_CONNECTING:
								break;
							case Constants.BLUETOOTH_NONE:
								Toast.makeText(getApplicationContext(), "BT DISCONNECT!!!", Toast.LENGTH_SHORT).show();
								init();
								break;
						}
						break;
					case Constants.MESSAGE_READ:
						break;
					case Constants.MESSAGE_WRITE:

						break;
					case Constants.MESSAGE_DEVICE_NAME:
						break;
					case Constants.MESSAGE_TOAST:
						break;
					default:
						break;
				}
			}
		};
		mHandler.setActivityCb(mActivityCb);
		mBluetoothService.setActivityHandler(mHandler);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control_view);

		if (mBluetoothService == null) {
			mBluetoothService = BluetoothService.getInstance();
		}

		init();

		// tilt 처리
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


		turn_stick = (ImageView) findViewById(R.id.turn_stick);

		// 깜빡이
		LinearLayout layoutLeft = (LinearLayout) findViewById(R.id.layoutLeft);
		layoutLeft.setOnTouchListener(new OnTouchListener() {
			private float y1,y2;
			static final int MIN_DISTANCE = 150;

			@Override
			public boolean onTouch(View v, MotionEvent event) {



				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						y1 = event.getY();
						break;
					case MotionEvent.ACTION_UP:
						y2 = event.getY();
						float deltaX = y2 - y1;

						// 우회전 깜빡이 켬
						if (deltaX < MIN_DISTANCE) {
							TURN_SIGNAL_STATUS = 2;
							turn_stick.setScaleType(ImageView.ScaleType.FIT_START);
							Log.d(TAG, "swipe to turn right");

							// 40~42초 사이에 수행했으면 task5 변수에 true값 저장
							if(40 <=seconds && seconds <=42) {
								scenarioTask5 = true;
								Log.d(TAG, "우회전 깜빡이 함");
							}

							// 좌회전 깜빡이 켬
						} else if (deltaX > MIN_DISTANCE) {
							TURN_SIGNAL_STATUS = 1;
							turn_stick.setScaleType(ImageView.ScaleType.FIT_END);

							Log.d(TAG, "swipe to turn left");

							// 30~32초 사이에 수행했으면 task5 변수에 true값 저장
							if(30 <=seconds && seconds <=32) {
								scenarioTask4 = true;
								Log.d(TAG, "좌회전 깜빡이 함");
							}
						}

						turnSignalTimer = new Timer();
						turnSignalTimer.scheduleAtFixedRate(new TimerTask() {
							public void run() {
								sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);
							}
						}, 0, 1000);

						break;
				}
				if(TURN_SIGNAL_STATUS != TURN_SIGNAL_STATUS2) {
					sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);
					TURN_SIGNAL_STATUS2 = TURN_SIGNAL_STATUS;
				}
				return true;
			}
		});




		// 엑셀 버튼
		btnAcc = (ImageView) findViewById(R.id.btnAcc);
		btnAcc.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {

				// 눌렀을때
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					ACC_STATUS = 1;
					sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);

					// 엑셀 누름 변수 = true
					isAccPressed = true;

					// 10~12초 사이에 엑셀 눌러야함
					if(10 <= seconds && seconds <= 12) {
						scenarioTask1 = true;
						Log.d(TAG, "엑셀 누름");
					}

					// 엑셀 눌려있는 시간 체크
					timer = new Timer();
					timer.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							btnAccElapsedTime = (float) (btnAccElapsedTime + 0.1f);
						}
					}, 0, 100);


					// 속도 증가
					timer2 = new Timer();
					timer2.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							// 1초 이상 눌려있었으면
							if(btnAccElapsedTime >= 1) {
								// 처음 액셀 눌렀으면 재생 신호 보내줌
								if(!isFirstAccleated) {
									mBluetoothService.sendMessage("play////1");
									Log.d(TAG, "isFirstAccleated!!!!       play////1");
									isFirstAccleated = true;
								}


								// 1초당 0.1씩 속도 증가
								velocity = velocity.add(VELOCITY_INCREASE);
								Log.d(TAG, "엑셀 속도증가 +0.1");

								// 최대 속도는 1.5임
								if(velocity.compareTo(new BigDecimal("1.5")) > 0) {
									velocity = new BigDecimal("1.5");
									Log.d(TAG, "최대속도 제한 1.5");
								}

								mBluetoothService.sendMessage("gamerun////" + velocity.doubleValue() + "////" + score);
								Log.d(TAG, "gamerun////" + velocity + "////" + score);
							}
						}
					}, 0, 1000);

					if(timer4!=null) timer4.cancel();

					// 엑셀 뗐을 때
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					ACC_STATUS = 0;
					sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);

					// 엑셀 누름 변수 = false
					isAccPressed = false;
					// 엑셀 누른시간 변수 = 0
					btnAccElapsedTime = 0;

					// 엑셀 누른시간 체크 타이머 cancel
					if(timer!=null) timer.cancel();
					// 속도 증가 타이머 cancel
					if(timer2!=null) timer2.cancel();

					// 브레이크 누르고 있지 않으면 0.1씩 감소
					if(!isAccPressed && !isBreakPressed) {
						// 자연속도 감소 타이머 시작
						timer4 = new Timer();
						timer4.scheduleAtFixedRate(new TimerTask() {
							public void run() {
								if (velocity.compareTo(VELOCITY_DECREASE) >= 0) {
									// 1초당 0.1씩 감소
									velocity = velocity.subtract(VELOCITY_INCREASE);
									Log.d(TAG, "자연 속도감소 -0.1");

									mBluetoothService.sendMessage("gamerun////" + velocity.doubleValue() + "////" + score);
									Log.d(TAG, "gamerun////" + velocity + "////" + score);
								}
							}
						}, 1000, 1000);
					}else {
						// 브레이크, 엑셀 중의 하나라도 누르고 있으면 자연속도 감소 타이머 cancel
						if(timer4!=null) timer4.cancel();
					}
				}



//				accTimer = new Timer();
//				accTimer.scheduleAtFixedRate(new TimerTask() {
//					public void run() {
//						sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);
//					}
//				}, 0, 1000);

				return true;
			}
		});

		// 브레이크 버튼
		ImageView btnBreak = (ImageView) findViewById(R.id.btnBreak);
		btnBreak.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 눌렀을때
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					BRAKE_STATUS = 1;
					sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);

					isBreakPressed = true;

					// 브레이크 눌려있는 시간 체크 타이머 시작
					timer5 = new Timer();
					timer5.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							btnBrakeElapsedTime = (float) (btnBrakeElapsedTime + 0.1f);
						}
					}, 0, 100);

					// 20~22초 사이에 브레이크 눌러야함
					if(20 <= seconds && seconds <= 22) {
						scenarioTask2 = true;
						Log.d(TAG, "브레이크 잘 누름");
					}

					// 속도 감소 타이머 시작
					timer3 = new Timer();
					timer3.scheduleAtFixedRate(new TimerTask() {
						public void run() {

							// 1초 이상 눌려있었으면
							if(btnBrakeElapsedTime >= 1) {
								// 속도 = 속도 - 브레이크 속도감소 변수값
								velocity = velocity.subtract(VELOCITY_BREAK_DECREASE);
								Log.d(TAG, "브레이크 속도감소 -0.5");

								// 속도가 0.5보다 아래면 0.5으로 보정
								if(velocity.compareTo(VELOCITY_DEFAULT) < 0) {
									velocity = VELOCITY_DEFAULT;
								}
								mBluetoothService.sendMessage("gamerun////" + velocity.doubleValue() + "////" + score);
							}
						}
					}, 0, 1000);

					if(timer4!=null) timer4.cancel();

					// 뗐을 때
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					BRAKE_STATUS = 0;
					sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);

					// 브레이크 누름 변수 = false
					isBreakPressed = false;
					// 브레이크 누른 시간 = 0
					btnBrakeElapsedTime = 0;

					if(timer5!=null) timer5.cancel();
					if(timer3!=null) timer3.cancel();

					if(!isAccPressed && !isBreakPressed) {
						// 자연 속도감소 타이머 시작
						timer4 = new Timer();
						timer4.scheduleAtFixedRate(new TimerTask() {
							public void run() {
								if (velocity.compareTo(VELOCITY_DECREASE) >= 0) {
									// 1초당 0.1씩 감소
									velocity = velocity.subtract(VELOCITY_INCREASE);
									mBluetoothService.sendMessage("gamerun////" + velocity.doubleValue() + "////" + score);
									Log.d(TAG, "gamerun////" + velocity + "////" + score);
									Log.d(TAG, "[2]자연 속도감소 -0.1");
								}
							}
						}, 1000, 1000);
					}else {
						if(timer4!=null) timer4.cancel();
					}
				}


//				brakeTimer = new Timer();
//				brakeTimer.scheduleAtFixedRate(new TimerTask() {
//					public void run() {
//						sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);
//					}
//				}, 0, 1000);
				return true;
			}
		});
		mDetector = new GestureDetectorCompat(this, this);
	}


	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;

		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				azimut = orientation[1];
				roll1 = -azimut * 360 / (2 * 3.14159f);

				// roll 각도가 -90도, -30도 사이에 있으면 좌회전
				if(-90 < roll1 && roll1 < -30) {
					HANDLE_STATUS = 1;
					sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);

					// 30~32초 사이에 좌회전 했으면 task3 변수 = true
					if(30 <=seconds && seconds <=32) {
						scenarioTask3 = true;
						Log.d(TAG, "좌회전 함");
					}

					// 50~52초 사이에 좌회전 했으면 task3 변수 = false
					if(50 <=seconds && seconds <=52) {
						scenarioTask7 = false;
						Log.d(TAG, "좌회전하면 안됨");
					}

					// 깜빡이 그림위치 원래대로
					turn_stick.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				}

				// roll 각도가 30도, 90도 사이에 있으면 우회전
				if(30 < roll1 && roll1 < 90) {
					HANDLE_STATUS = 2;
					sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);

					// 40~42초 사이에 우회전 했으면 task5 변수 = true
					if(40 <=seconds && seconds <=42) {
						scenarioTask5 = true;
						Log.d(TAG, "우회전 함");
					}

					// 50~52초 사이에 우회전 했으면 task7 변수 = false
					if(50 <=seconds && seconds <=52) {
						scenarioTask7 = false;
						Log.d(TAG, "우회전하면 안됨");
					}

					// 깜빡이 그림위치 원래대로
					turn_stick.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
				}

				// roll 각도가 -30도, 30도 사이에 있으면 직진
				if(-30 <= roll1 && roll1 <= 30) {
					HANDLE_STATUS = 0;
					sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);

					// 50~52초 사이에 직진 했으면 task7 변수 = true
					if(scenarioTask7) {
						if(50 <=seconds && seconds <=52) {
							scenarioTask7 = true;
							Log.d(TAG, "직진 함");
						}
					}
				}


//				handleTimer = new Timer();
//				handleTimer.scheduleAtFixedRate(new TimerTask() {
//					public void run() {
//						sendMessage(STATUS_GAMERUN, velocity, HANDLE_STATUS, TURN_SIGNAL_STATUS, ACC_STATUS, BRAKE_STATUS);
//					}
//				}, 0, 1000);
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	public void setListners(SensorManager sensorManager, SensorEventListener mEventListener) {
		sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(mEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
	}




	@Override
	public boolean onTouchEvent(MotionEvent event) {
		this.mDetector.onTouchEvent(event);
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent event) {
		return true;
	}

	@Override
	public boolean onFling(MotionEvent event1, MotionEvent event2,
						   float velocityX, float velocityY) {
		return true;
	}

	@Override
	public void onLongPress(MotionEvent event) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
							float distanceY) {

		return true;
	}

	@Override
	public void onShowPress(MotionEvent event) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		return true;
	}



	@Override
	public void onResume() {
		super.onResume();

		timerHandler.postDelayed(timerRunnable, 1000);

		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onPause() {
		super.onPause();

		// 모든 타이머 멈춤
		if(timer!=null) timer.cancel();
		if(timer2!=null) timer2.cancel();
		if(timer3!=null) timer3.cancel();
		if(timer4!=null) timer4.cancel();
		if(timer5!=null) timer5.cancel();

		timerHandler.removeCallbacks(timerRunnable);

		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onBackPressed() {
		if (doubleBackToExitPressedOnce) {
			super.onBackPressed();
			return;
		}

		this.doubleBackToExitPressedOnce = true;
		Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				/*
				백키를 한번 누르고 2초 이내에 한번 더 눌렀으면
				stop 메시지 보내고 초기화
				 */
				doubleBackToExitPressedOnce = false;
				mBluetoothService.sendMessage("stop////1");
				init();
				Log.d(TAG, "stop////1");
			}
		}, 2000);
	}

	public void sendMessage(String cmd, BigDecimal velocity, int handleStatus,
							int turnSignalStatus, int AccStatus, int BrakeStatus) {

		// cmd////velocity////handle[0,1,2]////깜빡이[0,1,2]////엑셀[0,1]////브레이크[0,1]
		mBluetoothService.sendMessage(cmd + BLUETHOOTH_MESSAGE_SEPARATOR +
				velocity + BLUETHOOTH_MESSAGE_SEPARATOR +
				handleStatus + BLUETHOOTH_MESSAGE_SEPARATOR +
				turnSignalStatus + BLUETHOOTH_MESSAGE_SEPARATOR +
				AccStatus + BLUETHOOTH_MESSAGE_SEPARATOR +
				BrakeStatus + BLUETHOOTH_MESSAGE_SEPARATOR);

		Log.d(TAG, cmd + BLUETHOOTH_MESSAGE_SEPARATOR +
				velocity + BLUETHOOTH_MESSAGE_SEPARATOR +
				handleStatus + BLUETHOOTH_MESSAGE_SEPARATOR +
				turnSignalStatus + BLUETHOOTH_MESSAGE_SEPARATOR +
				AccStatus + BLUETHOOTH_MESSAGE_SEPARATOR +
				BrakeStatus + BLUETHOOTH_MESSAGE_SEPARATOR);
	}
}