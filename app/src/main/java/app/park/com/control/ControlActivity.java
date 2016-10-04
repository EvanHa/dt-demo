package app.park.com.control;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;

import app.park.com.MainActivity;
import app.park.com.R;
import app.park.com.bluetooth.BluetoothHandler;
import app.park.com.bluetooth.BluetoothService;
import app.park.com.bluetooth.Constants;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import static app.park.com.R.layout.popup;

public class ControlActivity extends Activity implements SensorEventListener,
		GestureDetector.OnGestureListener {

	ImageView turn_stick;
	ImageView btnAcc;

	public static String TAG = "TAG";
	static final String BLUETHOOTH_MESSAGE_SEPARATOR = "////";
	static final String STATUS_GAMERUN = "gamerun";

	// 센서
	private GestureDetectorCompat mDetector;
	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;

	// 센서값
	float[] mGravity;
	float[] mGeomagnetic;
	static float roll1 = 0;
	static float azimut = 0;

	// 블루투스
	private BluetoothService mBluetoothService = null;
	private BluetoothHandler mHandler = null;
	private BluetoothHandler.ActivityCb mActivityCb = null;

	// 엑셀 처음 눌렀는지
	public static boolean isFirstAccleated = false;

	// 버튼들 눌려있는 시간 잴 타이머
	Timer timer;
	Timer timer2;
	Timer timer3;
	Timer timer4;
	Timer timer5;

	// 버튼 눌린 시간
	static float btnAccElapsedTime = 0;
	static float btnBrakeElapsedTime = 0;

	// -------------- 속도 관련 변수 --------------------------
	static final BigDecimal VELOCITY_DEFAULT = new BigDecimal("1. 0"); // 기본 속도  (시작속도)
	static final BigDecimal VELOCITY_MIN_INCREASE = new BigDecimal("1.0"); // 가속시 최소 속도 1.0 (속도가 0일때, 가속하면 1.0부터 시작함)
	static final BigDecimal VELOCITY_MIN_DECREASE = new BigDecimal("0"); // 감속시 최소 속도 0 (0까지 줄어듬)
	static final BigDecimal VELOCITY_MAX = new BigDecimal("1.5"); // 최대 속도

	static final BigDecimal VELOCITY_INCREASE = new BigDecimal("0.1"); // 속도 증가값 (엑셀)

	static final BigDecimal VELOCITY_BREAK_DECREASE = new BigDecimal("0.5"); // 속도 감소값 (브레이크)
	static final BigDecimal VELOCITY_DECREASE = new BigDecimal("0.1"); // 속도 감소값 (자연 감소)

	static BigDecimal velocity = VELOCITY_DEFAULT; // 속도 변화 여부 판단할 임시변수
	static BigDecimal velocity2 = VELOCITY_DEFAULT;
	// -------------- 속도 관련 변수 --------------------------

	private static boolean isAccPressed = false;
	private static boolean isBreakPressed = false;
	private static boolean exit = false;

	// 진행시간
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

			// 매초마다 블루투스로 현재 상태값 전송
			if(isFirstAccleated) {
				sendMessage("전체 1초당 스레드", STATUS_GAMERUN);
			}
		}
	};

	// 초기화 - 시간, 점수, 속도 등
	public void init() {
		seconds = 0;
		seconds_ = 0;
		minutes = 0;
		exit = false;
		velocity = VELOCITY_DEFAULT;
		velocity2 = VELOCITY_DEFAULT;
		isFirstAccleated = false;
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
					case Constants.MESSAGE_READ: // 블루투스 메시지 수신
						// vr쪽 나갔으면 여기도 메인으로 나감
						if(msg.equals("stop")) {
							Intent intent = new Intent(getApplicationContext(), MainActivity.class);
							startActivity(intent);
						}

						// 메시지가 미션페일이면 속도 0으로 하고 toast
						if(msg.equals("rewind")) {
							velocity = VELOCITY_DEFAULT;
							Toast.makeText(getApplicationContext(), "5초 전", Toast.LENGTH_SHORT).show();
						}

						// resume: when score is below 70 point, select resume or finish button on control panel
						if(msg.equals("resume")) {
							Toast.makeText(getApplicationContext(), "70점 이하", Toast.LENGTH_SHORT).show();
							onBackPressed();
						}
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

						// 0=좌 신호, 1=중립, 2=우 신호
						// 위로 스와이프
						if (deltaX < MIN_DISTANCE) {
							// 좌 신호였으면 중립으로
							if(TURN_SIGNAL_STATUS==0) {
								TURN_SIGNAL_STATUS = 1;
							}
							// 중립이었으면 우 신호로
							else if(TURN_SIGNAL_STATUS==1) {
								TURN_SIGNAL_STATUS = 2;
							}

						// 아래로 스와이프
						} else if (deltaX > MIN_DISTANCE) {
							// 우 신호였으면 중립으로
							if(TURN_SIGNAL_STATUS==2) {
								TURN_SIGNAL_STATUS = 1;
							}
							// 중립이었으면 좌 신호로
							else if(TURN_SIGNAL_STATUS==1) {
								TURN_SIGNAL_STATUS = 0;
							}
						}

						// 이전 상태값과 다르면 전송
						if(TURN_SIGNAL_STATUS != TURN_SIGNAL_STATUS2) {
							TURN_SIGNAL_STATUS2 = TURN_SIGNAL_STATUS;
							sendMessage("깜빡이 상태변경", STATUS_GAMERUN);
						}

						// 깜빡이 그림 위치바꿈
						if(TURN_SIGNAL_STATUS==2) {
							turn_stick.setScaleType(ImageView.ScaleType.FIT_START);
						}else if(TURN_SIGNAL_STATUS==1) {
							turn_stick.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
						}else if(TURN_SIGNAL_STATUS==0) {
							turn_stick.setScaleType(ImageView.ScaleType.FIT_END);
						}
						break;
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

					// 이전 상태값과 다르면 전송
					if(ACC_STATUS != ACC_STATUS2) {
						ACC_STATUS2 = 1;
						sendMessage("엑셀누름", STATUS_GAMERUN);
					}

					// 엑셀 누름 변수 = true
					isAccPressed = true;

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
							// 처음 액셀 눌렀으면 isFirstAccleated 변수 = true
							if(!isFirstAccleated) {
								isFirstAccleated = true;
							}

							// 0.5보다 작으면 0.5로 보정
							if(velocity.compareTo(VELOCITY_MIN_INCREASE) < 0) {
								velocity = VELOCITY_MIN_INCREASE;
							}

							// 1초당 0.1씩 속도 증가
							velocity = velocity.add(VELOCITY_INCREASE);

							// 최대 속도는 1.5임
							if(velocity.compareTo(VELOCITY_MAX) > 0) {
								velocity = VELOCITY_MAX;
							}
						}
					}, 0, 1000);

					if(timer4!=null) timer4.cancel();

					// 엑셀 뗐을 때
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					ACC_STATUS = 0;

					// 이전 상태값과 다르면 전송
					if(ACC_STATUS != ACC_STATUS2) {
						ACC_STATUS2 = 0;
						sendMessage("엑셀 뗌", STATUS_GAMERUN);
					}

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

									// 속도가 0.5보다 아래면 0으로 보정
									if(velocity.compareTo(VELOCITY_DEFAULT) < 0) {
										velocity = VELOCITY_MIN_DECREASE;
									}
								}
							}
						}, 0, 1000);
					}else {
						// 브레이크, 엑셀 중의 하나라도 누르고 있으면 자연속도 감소 타이머 cancel
						if(timer4!=null) timer4.cancel();
					}
				}
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

					// 이전 상태값과 다르면 전송
					if(BRAKE_STATUS != BRAKE_STATUS2) {
						BRAKE_STATUS2 = 1;
						sendMessage("브레이크 누름", STATUS_GAMERUN);
					}
					isBreakPressed = true;


					// 브레이크 눌려있는 시간 체크 타이머 시작
					timer5 = new Timer();
					timer5.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							btnBrakeElapsedTime += 1;
						}
					}, 0, 1000);

					// 속도 감소 타이머 시작
					timer3 = new Timer();
					timer3.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							// 속도 = 속도 - 브레이크 속도감소 변수값
							velocity = velocity.subtract(VELOCITY_BREAK_DECREASE);

							// 속도가 0보다 아래면 0으로 보정
							if(velocity.compareTo(VELOCITY_DEFAULT) < 0) {
								velocity = VELOCITY_MIN_DECREASE;
							}
						}
					}, 0, 1000);

					if(timer4!=null) timer4.cancel();

					// 뗐을 때
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					BRAKE_STATUS = 0;

					// 이전 상태값과 다르면 전송
					if(BRAKE_STATUS != BRAKE_STATUS2) {
						BRAKE_STATUS2 = 0;
						sendMessage("브레이크 똄", STATUS_GAMERUN);
					}

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

									// 속도가 0.5보다 아래면 0으로 보정
									if(velocity.compareTo(VELOCITY_DEFAULT) < 0) {
										velocity = VELOCITY_MIN_DECREASE;
									}
								}
							}
						}, 0, 1000);
					}else {
						if(timer4!=null) timer4.cancel();
					}
				}
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

					// 이전 상태값과 다르면 전송
					if(HANDLE_STATUS != HANDLE_STATUS2) {
						HANDLE_STATUS2 = 1;
						sendMessage("핸들 좌회전", STATUS_GAMERUN);
					}
				}

				// roll 각도가 30도, 90도 사이에 있으면 우회전
				else if(30 < roll1 && roll1 < 90) {
					HANDLE_STATUS = 2;

					// 이전 상태값과 다르면 전송
					if(HANDLE_STATUS != HANDLE_STATUS2) {
						HANDLE_STATUS2 = 2;
						sendMessage("핸들 우회전", STATUS_GAMERUN);
					}
				}

				// roll 각도가 -30도, 30도 사이에 있으면 직진
				else if(-30 <= roll1 && roll1 <= 30) {
					HANDLE_STATUS = 0;

					// 이전 상태값과 다르면 전송
					if(HANDLE_STATUS != HANDLE_STATUS2) {
						HANDLE_STATUS2 = 0;
						sendMessage("핸들 직진", STATUS_GAMERUN);

						// 좌, 우회전이었다가 직진이면 깜빡이 중립으로
						TURN_SIGNAL_STATUS = 1;
						turn_stick.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
					}
				}
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

		LayoutInflater layoutInflater
				= (LayoutInflater) getBaseContext()
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		final View popupView = layoutInflater.inflate(popup, null);
		final PopupWindow popupWindow = new PopupWindow(
				popupView,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT);
		// PopupWindow 위에서 Button의 Click이 가능하도록 setTouchable(true);
		popupWindow.setTouchable(true);
		// PopupWindow 상의 View의 Button 연결
		Button btnYes = (Button) popupView.findViewById(R.id.btn_yes);
		Button btnNo = (Button) popupView.findViewById(R.id.btn_no);

		popupWindow.showAtLocation(this.getWindow().getDecorView(), Gravity.CENTER, 0, 0);

		btnYes.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				// 정지 메시지 보냄
				mBluetoothService.sendMessage("stop////1");

				Intent intent = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(intent);
			}
		});

		btnNo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				popupWindow.dismiss();
			}
		});
	}

	public void sendMessage(String log, String cmd) {
		/*
			ex) cmd////velocity////handle[0,1,2]////깜빡이[0,1,2]////엑셀[0,1]////브레이크[0,1]

			handle
			0=직진, 1=좌회전, 2=우회전

			깜빡이
			0=좌 신호, 1=중립, 2=우 신호

			엑셀, 브레이크
			0=안누름, 1=누름
		 */

		// 속도가 달라졌거나, 속도 0이 아니면 보냄
		if (velocity.compareTo(velocity2) != 0 || velocity.compareTo(BigDecimal.ZERO) != 0) {
			mBluetoothService.sendMessage(cmd + BLUETHOOTH_MESSAGE_SEPARATOR +
					velocity + BLUETHOOTH_MESSAGE_SEPARATOR +
					HANDLE_STATUS + BLUETHOOTH_MESSAGE_SEPARATOR +
					TURN_SIGNAL_STATUS + BLUETHOOTH_MESSAGE_SEPARATOR +
					ACC_STATUS + BLUETHOOTH_MESSAGE_SEPARATOR +
					BRAKE_STATUS + BLUETHOOTH_MESSAGE_SEPARATOR);
		}
		velocity2 = velocity;
	}
}