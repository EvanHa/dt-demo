package app.park.com.task;

import android.util.Log;

import app.park.com.vr.VrVideoActivity;

public class TaskValidator {
    public static final String TAG = VrVideoActivity.class.getSimpleName();

    public static boolean task1 = false;
    public static boolean task2 = false;
    public static boolean task3 = false;
    public static boolean task4 = false;
    public static boolean task5 = false;
    public static boolean task6 = false;
    public static boolean task7 = false;

    public static int validate(String[] arr, long currentTime) {
        int second = safeLongToInt(currentTime / 1000);
        int returnPenalty = 0;
        Log.d(TAG, "######## taskValidator - currentTime = " + currentTime);
        Log.d(TAG, "######## taskValidator - currentTime(s) = " + second);
		/*
			cmd////velocity////handle[0,1,2]////깜빡이[0,1,2]////엑셀[0,1]////브레이크[0,1]

			handle
			0=직진, 1=좌회전, 2=우회전

			깜빡이
			0=좌 신호, 1=중립, 2=우 신호

			엑셀, 브레이크
			0=안누름, 1=누름
		 */

        // --------------------- task1 -------------------------------------------
        // 10~12초 사이에 엑셀 눌렀는지 체크
        if (10 <= second && second <= 12) {
            if(arr[4].equals("1")) {
                task1 = true;
            }
            Log.d(TAG, "######## taskValidator - 10~12 sec. task1 = " + task1);

        }
        // 13초에 task1 수행여부에 따라 감점
        if (second == 13) {
            if(! task1) {
                Log.d(TAG, "######## taskValidator - score  -= -5");
                returnPenalty = -5;
            }else {
                Log.d(TAG, "######## taskValidator - score. task1  OKAY");
                returnPenalty = 0;
            }
        }
        // --------------------- task1 -------------------------------------------




        // --------------------- task2 -------------------------------------------
        // 23초에 task(20~22초 사이에 브레이크 누름) 수행여부 판별하여 감점
        if (20 <= second && second <= 22) {
            if(arr[5].equals("1")) {
                task2 = true;
            }
            Log.d(TAG, "######## taskValidator - 20~22 sec. task2 = " + task2);

        }
        // 23초에 task2 수행여부에 따라 감점
        if (second == 23) {
            if(! task2) {
                Log.d(TAG, "######## taskValidator - score. task2-= -5");
                returnPenalty = -5;
            }else {
                Log.d(TAG, "######## taskValidator - score. task2  OKAY");
                returnPenalty = 0;
            }
        }
        // --------------------- task2 -------------------------------------------




        // --------------------- task3,4 -------------------------------------------
        // 33초에 task(30~32초 사이에 좌회전 깜빡이, 좌회전 ) 수행여부 판별하여 감점
        if (30 <= second && second <= 32) {
            if(arr[2].equals("1")) {
                task3 = true;
            }
            if(arr[3].equals("0")) {
                task4 = true;
            }
            Log.d(TAG, "######## taskValidator - 30~32 sec. task3 = " + task3 + ", task4 = " + task4);
        }
        // 33초에 task3 수행여부에 따라 감점
        if (second == 33) {
            if(! task3 && ! task4) {
                Log.d(TAG, "######## taskValidator - score. task3, task4 -= -10");
                returnPenalty = -10;
            }else {
                Log.d(TAG, "######## taskValidator - score. task3, task4  OKAY");
                returnPenalty = 0;
            }
        }
        // --------------------- task3,4 -------------------------------------------




        // --------------------- task5,6 -------------------------------------------
        // 43초에 task(40~42초 사이에 우회전 깜빡이, 우회전 ) 수행여부 판별하여 감점
        if (40 <= second && second <= 42) {
            if(arr[2].equals("2")) {
                task5 = true;
            }
            if(arr[3].equals("2")) {
                task6 = true;
            }
            Log.d(TAG, "######## taskValidator - 40~42 sec. task5 = " + task5 + ", task6 = " + task6);
        }
        // 43초에 task5 수행여부에 따라 감점
        if (second == 43) {
            if(! task5 && ! task6) {
                Log.d(TAG, "######## taskValidator - score. task5, task6 -= -10");
                returnPenalty = -10;
            }else {
                Log.d(TAG, "######## taskValidator - score. task5, task6  OKAY");
                returnPenalty = 0;
            }
        }
        // --------------------- task5,6 -------------------------------------------




        // --------------------- task7 -------------------------------------------
        // 53초에 task(50~52초 사이에 좌, 우회전 안함) 수행여부 판별하여 감점
        if (50 <= second && second <= 52) {
            if(! arr[2].equals("0")) {
                task7 = false;
            }
            Log.d(TAG, "######## taskValidator - 50~52 sec. task7 = " + task7);

        }
        // 53초에 task7 수행여부에 따라 감점
        if (second == 53) {
            if(! task7) {
                Log.d(TAG, "######## taskValidator - score. task7-= -10");
                returnPenalty = -10;
            }else {
                Log.d(TAG, "######## taskValidator - score. task7  OKAY");
                returnPenalty = 0;
            }
        }
        // --------------------- task7 -------------------------------------------




        return returnPenalty;
    }



    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
}
