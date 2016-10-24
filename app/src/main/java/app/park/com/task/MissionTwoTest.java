package app.park.com.task;

import android.util.Log;

public class MissionTwoTest extends Mission {
    public static final String TAG = MissionTwoTest.class.getSimpleName();

    public static boolean task1 = false; // 0:55 ~ 1:05 정지
    public static boolean task2 = false; // 1:55 ~ 2:05 재출발
    public static boolean task3 = false; // 2:15~ 2:25 좌회전
    public static boolean task4 = false; // 3:20~3:26 좌회전

    @Override
    public int validate(String[] arr, long currentTime) {
        int second = safeLongToInt(currentTime / 1000);
        int returnPenalty = MISSION_CLEAR;
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

        switch (second) {
            case 0: //0:00 시작
                break;
            case 55: // 0:55 ~ 1:05 정지 - task1
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
                if (checkStop(arr)) {
                    task1 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 66: // task1 fail
                if (task1 == false) {
                    returnPenalty = MISSION_FAIL_STOP;
                }
                break;
            case 115: // 1:55 ~ 2:05 재출발 - task2
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
                if (checkStart(arr)) {
                    task2 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 126: // task2 fail
                if (task2 == false) {
                    returnPenalty = MISSION_FAIL_START;
                }
                break;
            case 135: // 2:15~ 2:25 좌회전 - task3
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
                if (checkTurnLeft(arr)) {
                    task3 = true;
                    returnPenalty = MISSION_CLEAR;
                }
            case 146: // task3 fail
                if (task3 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 200: // 3:20~3:26 좌회전 - task4
            case 201:
            case 202:
            case 203:
            case 204:
            case 205:
            case 206:
                if (checkTurnLeft(arr)) {
                    task4 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 207: // task4 fail
                if (task4 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            default:
                break;
        }
        return returnPenalty;
    }

    @Override
    public void reInit() {
        task1 = false;
        task2 = false;
        task3 = false;
        task4 = false;
    }

    @Override
    public boolean checkStart(String[] arr) {
        return super.checkStart(arr);
    }

    @Override
    public boolean checkStop(String[] arr) {
        return super.checkStop(arr);
    }

    @Override
    public boolean checkTurnLeft(String[] arr) {
        return super.checkTurnLeft(arr);
    }

    @Override
    public boolean checkTurnRight(String[] arr) {
        return super.checkTurnRight(arr);
    }

    @Override
    public int safeLongToInt(long l) {
        return super.safeLongToInt(l);
    }
}
