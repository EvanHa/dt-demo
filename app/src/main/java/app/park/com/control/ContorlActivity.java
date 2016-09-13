package app.park.com.control;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import app.park.com.R;
import app.park.com.bluetooth.BluetoothService;

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

public class ContorlActivity extends Activity implements SensorEventListener,
		GestureDetector.OnGestureListener {
	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;

	private BluetoothService mBluetoothService = null;
	private static List<Scenario> scenarioList;
    
	ImageView frame;
	ImageView turn_stick;
//	TextView textCTime;
//	TextView textView;
//	TextView textVelocity;
//	
//	TextView textStatus1;
//	TextView textStatus2;
//	TextView textScore;
	
	ImageView btnAcc;
	
	Timer timer;
	Timer timer2;
	Timer timer3;
	Timer timer4;
	Timer timer5;
	Timer scenarioTimer;

	static float roll1 = 0;
	static float azimut = 0;
	
	static float btnAccElapsedTime = 0;
	static float btnBrakeElapsedTime = 0;
//	static double velocity = 0;
//	static final double VELOCITY_INCREASE = 0.1;
//	static final double VELOCITY_BREAK_DECREASE= 0.5;
//	static final double VELOCITY_DECREASE= 0.1;
	static BigDecimal velocity = new BigDecimal(0);
	static final BigDecimal VELOCITY_INCREASE = new BigDecimal("0.1");
	static final BigDecimal VELOCITY_BREAK_DECREASE = new BigDecimal("0.5");
	static final BigDecimal VELOCITY_DECREASE = new BigDecimal("0.1");

	static DecimalFormat valueFormat = new DecimalFormat("0.0");

	private static boolean scenarioTask1 = false;
	private static boolean scenarioTask2 = false;
	private static boolean scenarioTask3 = false;
	private static boolean scenarioTask4 = false;
	private static boolean scenarioTask5 = false;
	private static boolean scenarioTask6 = false;
	private static boolean scenarioTask7 = true;
	
	private static boolean isAccPressed = false;
	private static boolean isBreakPressed = false;
	private GestureDetectorCompat mDetector;

	// tilt 처리
	float[] mGravity;
	float[] mGeomagnetic;
	
	// retry alert
	private static boolean exit = false;
	
	

	// 진행시간 및 시나리오 처리
	static int score = 0;
	
    static int seconds = 0;
    static int seconds_ = 0;
    static int minutes = 0;
    
    public Handler timerHandler = new Handler();
    public Runnable timerRunnable = new Runnable() {
        public void run() {
            timerHandler.postDelayed(this, 1000);
//			Log.d("test", "isAccPressed = " + isAccPressed + ", isBreakPressed = " + isBreakPressed);
			Log.d("test", "velocity = " + velocity);
			
			
            // 60점 이하면
            if(!exit && score < 60) {
            	// 일단 멈추고
            	onPause();
                
            	new AlertDialog.Builder(ContorlActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("점수가 60이하")
                .setMessage("다시 하겠습니까?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	                @Override
	                public void onClick(DialogInterface dialog, int which) {
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
            

    		Log.d("test", String.format("%02d:%02d", minutes, seconds_));

//            textCTime.setText(String.format("%02d:%02d", minutes, seconds_));
            
    		
            if(12 < seconds && seconds <= 13) {
            	if(!scenarioTask1) {
                	// 5점 감점
                	score -= 5;
//            		textScore.setText(score + " 점");
                	Log.d("test", "엑셀 안누름");
            	}
            }
            // 시나리오1 수행 여부에 따라서 점수 감점
    		// 요건 캡슐화 한거고.. 이걸 자동으로 해줘야함
//            applyScenarioResult(12, 13, scenarioTask1, -5);
            
            if(22 < seconds && seconds <= 23) {
            	if(!scenarioTask2) {
            		// 5점 감점
            		score -= 5;
//            		textScore.setText(score + " 점");
            		Log.d("test", "브레이크 안누름");
            	}
            }
            
            if(32 < seconds && seconds <= 33) {
            	if(!scenarioTask3 || !scenarioTask4) {
            		// 10점 감점
            		score -= 10;
//            		textScore.setText(score + " 점");
            		Log.d("test", "왼쪽 깜빡이 또는 좌회전 안했음");
            	}
            }
            
            if(42 < seconds && seconds <= 43) {
            	if(!scenarioTask5 || !scenarioTask6) {
            		// 10점 감점
            		score -= 10;
//            		textScore.setText(score + " 점");
            		Log.d("test", "오른쪽 깜빡이 또는 우회전 안했음");
            	}
            }
            
            if(52 < seconds && seconds <= 53) {
            	if(!scenarioTask7) {
            		// 10점 감점
            		score -= 10;
//            		textScore.setText(score + " 점");
            		Log.d("test", "직진 안했음");
            	}
            }
        }
    };
    
    
    public void init() {
    	seconds = 0;
    	seconds_ = 0;
    	score = 100;
    	exit = false;
    	
//		textCTime.setText("00:00");
//		textView.setText("0 sec.");
//		textVelocity.setText("0 km/h");
//		textScore.setText(score + " 점");
//		textStatus1.setText("go straight");
//		textStatus1.setText("");
    }
    
    

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control_view);

		if (mBluetoothService == null) {
			mBluetoothService = BluetoothService.getInstance();
		}
		
		scenarioList = new ArrayList<Scenario>();
		
		
		// 현재시간
//		textCTime = (TextView) findViewById(R.id.textCTime);
//
//		// 테스트용 텍스트뷰
//		textView = (TextView) findViewById(R.id.textView);
//
//		// 속도
//		textVelocity = (TextView) findViewById(R.id.textVelocity);
		
		// 점수
//		textScore = (TextView) findViewById(R.id.textScore);
		
		// 상태
//		textStatus1 = (TextView) findViewById(R.id.textStatus1);
//		
//		textStatus2 = (TextView) findViewById(R.id.textStatus2);

		init();
		
		// tilt 처리
	    mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
	    accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        
	    turn_stick = (ImageView) findViewById(R.id.turn_stick);
	    
	    // 깜빡이
	    LinearLayout layoutLeft = (LinearLayout) findViewById(R.id.layoutLeft);
	    layoutLeft.setOnTouchListener(new OnTouchListener() {
	    	private float x1,x2;
	    	static final int MIN_DISTANCE = 150;
	    	
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					x1 = event.getY();
					break;
				case MotionEvent.ACTION_UP:
					x2 = event.getY();
					float deltaX = x2 - x1;
					
					// 우회전 깜빡이
					if (deltaX < MIN_DISTANCE) {
						turn_stick.setScaleType(ImageView.ScaleType.FIT_START);
//						textStatus2.post(new Runnable() {
//			 				public void run() {
//			 					textStatus2.setText("swipe to turn right");
								Log.d("test", "swipe to turn right");
			 					
								if(40 <=seconds && seconds <=42) {
									scenarioTask5 = true;
									Log.d("test", "우회전 깜빡이 함");
								}
//			 				}
//			 			});
								
					// 좌회전 깜빡이
					} else if (deltaX > MIN_DISTANCE) {
						turn_stick.setScaleType(ImageView.ScaleType.FIT_END);
//						textStatus2.post(new Runnable() {
//			 				public void run() {
//			 					textStatus2.setText("swipe to turn left");

								Log.d("test", "swipe to turn left");
								
								if(30 <=seconds && seconds <=32) {
									scenarioTask4 = true;
									Log.d("test", "좌회전 깜빡이 함");
								}
//			 				}
//			 			});
					}
					break;
				}
				return true;
			}
		});
	    
	    
	    
	    
		
		
		
		// acc btn
		btnAcc = (ImageView) findViewById(R.id.btnAcc);
		btnAcc.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// (1) 엑셀
				// 눌렀을때
				
//				Log.d("test", "Thread.currentThread().getName() = " + Thread.currentThread().getName());

				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					isAccPressed = true;
					
					// 10~12초 사이에 엑셀 눌러야함
		            if(10 <= seconds && seconds <= 12) {
						scenarioTask1 = true;
						Log.d("test", "엑셀 누름");
		            }
					
		            // setNAme이 되긴 됐는데 다른 이벤트리스너에서도 되는지 보쟈..
//		            Thread.currentThread().setName("My onTouch event listener");
//		            Log.d("test", "thread name was set");
					
					// 시나리오1 검증
		    		// 요건 캡슐화 한거고.. 이걸 자동으로 해줘야함
//		            validateScenario(10, 12, scenarioTask1, "event_accelator", "엑셀 누름");
					
					// 시간은 static이라 접근가능하니.. 어떤 이벤트가 일어났고, 어떤 객체 정보인지를 넘겨주자..
//		            validateScenario(v.getId(), event.getAction());
		            

					
					
					
					timer = new Timer();

					// 엑셀 눌려있는 시간 체크
					timer.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							
							btnAccElapsedTime = (float) (btnAccElapsedTime + 0.1f);
//							Log.d("test", "btnAccElapsedTime = " + btnAccElapsedTime);
//							Log.d("test", "btnAccElapsedTime is bigger than 1 : " + (btnAccElapsedTime>=1));
//							textView.post(new Runnable() {
//								public void run() {
//									textView.setText(valueFormat.format(btnAccElapsedTime) + " sec.");
//								}
//							});
						}
					}, 0, 100);

					
					// 속도 증가
					timer2 = new Timer();
					timer2.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							// 1초 이상 눌려있었으면
							if(btnAccElapsedTime >= 1) {
								// 1초당 0.1씩 증가
	//							velocity += VELOCITY_INCREASE;
								velocity = velocity.add(VELOCITY_INCREASE);
								Log.d("test", "엑셀 속도증가 +0.1");
								
	//							velocity += VELOCITY_ACC;
	//							textVelocity.post(new Runnable() {
	//								public void run() {
	//									textVelocity.setText(velocity + " km/h");
	//								}
	//							});
							}
						}
					}, 0, 1000);
					
					if(timer4!=null) timer4.cancel();

					// 뗐을 때
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					isAccPressed = false;
					btnAccElapsedTime = 0;
					
//					textView.post(new Runnable() {
//						public void run() {
//							textView.setText("0 sec.");
//						}
//					});
					timer.cancel();
					if(timer2!=null) timer2.cancel();
					
					// 브레이크 누르고 있지 않으면 0.1씩 감소
//					if(!isBreakPressed) {
					if(!isAccPressed && !isBreakPressed) {
						timer4 = new Timer();
						timer4.scheduleAtFixedRate(new TimerTask() {
							public void run() {
								if (velocity.compareTo(VELOCITY_DECREASE) >= 0) {
									// 1초당 0.1씩 감소
									velocity = velocity.subtract(VELOCITY_INCREASE);
									Log.d("test", "자연 속도감소 -0.1");
								}
							}
						}, 1000, 1000);
					}
					
				}
				return true;
			}
		});

		// break btn
		ImageView btnBreak = (ImageView) findViewById(R.id.btnBreak);
		btnBreak.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// (1) 브레이크 눌렀다가 떼는 이벤트 처리
				// 눌렀을때
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					isBreakPressed = true;
					

					timer5 = new Timer();

					// 브레이크 눌려있는 시간 체크
					timer5.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							btnBrakeElapsedTime = (float) (btnBrakeElapsedTime + 0.1f);
						}
					}, 0, 100);
					
					
					
					// 20~22초 사이에 엑셀 눌러야함
		            if(20 <= seconds && seconds <= 22) {
						scenarioTask2 = true;
						Log.d("test", "브레이크 잘 누름");
		            }
		            
					// 속도 감소
					timer3 = new Timer();
					timer3.scheduleAtFixedRate(new TimerTask() {
						public void run() {

							// 1초 이상 눌려있었으면
							if(btnBrakeElapsedTime >= 1) {
								if (velocity.compareTo(VELOCITY_BREAK_DECREASE) >= 0) {
									// 1초당 0.5씩 감소
									velocity = velocity.subtract(VELOCITY_BREAK_DECREASE);
									Log.d("test", "브레이크 속도감소 -0.5");
								}
							}

//							textVelocity.post(new Runnable() {
//								public void run() {
//									textVelocity.setText(velocity + " km/h");
//								}
//							});
						}
					}, 0, 1000);

					if(timer4!=null) timer4.cancel();
					
				// 뗐을 때
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					isBreakPressed = false;
					btnBrakeElapsedTime = 0;
					
//					textView.post(new Runnable() {
//						public void run() {
//							textView.setText("0 sec.");
//						}
//					});
					timer5.cancel();
					timer3.cancel();
//					if(timer4!=null) timer4.cancel();
					
					timer4 = new Timer();
					timer4.scheduleAtFixedRate(new TimerTask() {
						public void run() {
							if (velocity.compareTo(VELOCITY_DECREASE) >= 0) {
								// 1초당 0.1씩 감소
								velocity = velocity.subtract(VELOCITY_INCREASE);
//								sendMessage("cmd||||" + velocity + "||||" + score);
								Log.d("test", "자연 속도감소 -0.1");
							}
						}
					}, 1000, 1000);
				}
				return true;
			}
		});
		mDetector = new GestureDetectorCompat(this, this);
		
		
		
		

		// 시나리오 객체(1) 생성
		addScenarioTask(
			new Scenario(10, // 시나리오 시작 시간
						12,  // 시나리오 끝 시간
						scenarioTask1,  // 수행여부 판단할 boolean
						btnAcc.getId(),  // 수행될 view 객체 id값
						"엑셀 누름",  // (디버깅용) 수행 성공시 log
						"엑셀 안 누름" // (디버깅용) 수행 실패시 log
						)
		);
		// 시나리오 객체(2) 생성 ...
		// 시나리오 객체(3) 생성 ...
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
				azimut = orientation[1]; // orientation contains: azimut, pitch and roll
				roll1 = -azimut * 360 / (2 * 3.14159f);

//				textStatus1.post(new Runnable() {
//					public void run() {
						// 좌회전
						if(-90 < roll1 && roll1 < -30) {
//							textStatus1.setText("tilt left");
							
							if(30 <=seconds && seconds <=32) {
								scenarioTask3 = true;
								Log.d("test", "좌회전 함");
							}
							
							
							if(50 <=seconds && seconds <=52) {
								scenarioTask7 = false;
								Log.d("test", "좌회전하면 안됨");
							}
							
							// 깜빡이 원래대로
							turn_stick.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
						}

						// 우회전
						if(30 < roll1 && roll1 < 90) {
//							textStatus1.setText("tilt right");
							
							if(40 <=seconds && seconds <=42) {
								scenarioTask5 = true;
								Log.d("test", "우회전 함");
							}
							

							if(50 <=seconds && seconds <=52) {
								scenarioTask7 = false;
								Log.d("test", "우회전하면 안됨");
							}
							
							// 깜빡이 원래대로
							turn_stick.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
						}
						
						// 직진
						if(-30 <= roll1 && roll1 <= 30) {
//							textStatus1.setText("go straigt");

							if(scenarioTask7) {
								if(50 <=seconds && seconds <=52) {
									scenarioTask7 = true;
									Log.d("test", "직진 함");
								}
							}
						}
//					}
//				});
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

        timerHandler.removeCallbacks(timerRunnable);

	    mSensorManager.unregisterListener(this);
	}
	
	
	

    // 시나리오1 수행 여부에 따라서 점수 감점
    public static void applyScenarioResult(int startSecond, int endSecond, boolean flag, boolean task, int decScore) {
      
	      if(startSecond < seconds && seconds <= endSecond) {
	    	if(!task) {
	        	// 5점 감점
	        	score -= decScore;
	//    		textScore.setText(score + " 점");
	        	Log.d("test", "엑셀 안누름");
	    	}
	    }
    }
    

	// 시나리오1 검증
    public static void validateScenario(int startSecond, int endSecond, boolean task, String eventName, String log) {
//    	Log.d("test", "validateScenario is called");
//		Log.d("test", "Thread.currentThread().getName() = " + Thread.currentThread().getName());
//    	Log.d("test", klass.getClass().toString());

		// 10~12초 사이에 엑셀 눌러야함
        if(startSecond <= seconds && seconds <= endSecond) {
        	task = true;
			Log.d("test", log + " inside of validateScenario");
        }
    }
    
    
    // 시나리오1 검증
    public static void validateScenario(int viewId, int eventName) {
    	Log.d("test", "validateScenario(int viewId, int eventName) is called");
    	// 시나리오 List 불러옴
    	List<Scenario> scenarioList_ = getScenarioTask();
    	
    	// 각 시간대에 호출되어야 하는 뷰객체명이랑 이벤트명 정렬?
    	// 루프 돌릴 생각하니... 걍 객체화 하지 말까 ㅡ,.ㅡ
    	
    	// 각 시나리오 잘 수행됐는지 검증
    	
    	
    	
    	// 일단 호출된 뷰객체명이랑 이벤트명..
		Log.d("test", "seconds = " + seconds + ", viewId = " + viewId + ", eventName = " + eventName);
    }
    
    
    public static void addScenarioTask(Scenario task) {
    	scenarioList.add(task);
    }
    
    public static List<Scenario> getScenarioTask() {
    	return scenarioList;
    }
}