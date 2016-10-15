package app.park.com.task;

import android.util.Log;

public class MissionThree extends Mission {
    public static final String TAG = MissionThree.class.getSimpleName();

    public boolean task1 = false; // 0:45 ~ 0:50 감속 및 정지
    public boolean task2 = false; // 1:18~ 1:23 재출발
    public boolean task3 = false; // 2:30~ 2:40 우측 차선 변경
    public boolean task4 = false; // 3:03~ 3:13 유턴
    public boolean task5 = false; // 3:55~ 4:02 좌측 차선 변경
    public boolean task6 = false; // 5:15~ 5:20 좌측 차선 변경
    public boolean task7 = false; // 5:40~ 5:45 감속 및 정지
    public boolean task8 = false; // 6:45~ 6:52 유턴

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
            case 45: // 0:45 ~ 0:50 감속 및 정지(task1)
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
                if (checkStop(arr)) {
                    task1 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 51: // task1 fail
                if (task1 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 78: // 1:18~ 1:23 재출발(task2)
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
                if (checkStart(arr)) {
                    task2 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 84: // task2 fail
                if (task2 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 150: // 2:30~ 2:40 우측 차선 변경(task3)
            case 151:
            case 152:
            case 153:
            case 154:
            case 155:
            case 156:
            case 157:
            case 158:
            case 159:
            case 160:
                if (checkTurnRight(arr)) {
                    task3 = true;
                    returnPenalty = MISSION_CLEAR;
                }
            case 161: // task3 fail
                if (task3 == false) {
                    returnPenalty = MISSION_FAIL_START;
                }
                break;
            case 183: // 3:03~ 3:13 유턴(task4)
            case 184:
            case 185:
            case 186:
            case 187:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
            case 193:
                if (checkTurnLeft(arr)) {
                    task4 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 194: // task4 fail
                if (task4 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 235: // 3:55~ 4:02 좌측 차선 변경(task5)
            case 236:
            case 237:
            case 238:
            case 239:
            case 240:
            case 241:
            case 242:
                if (checkTurnLeft(arr)) {
                    task5 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 243: // task5 fail
                if (task5 == false) {
                    returnPenalty = MISSION_FAIL_STOP;
                }
                break;
            case 315: // 5:15~ 5:20 좌측 차선 변경(task6)
            case 316:
            case 317:
            case 318:
            case 319:
            case 320:
                if (checkTurnLeft(arr)) {
                    task6 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 321: // task6 fail
                if (task6 == false) {
                    returnPenalty = MISSION_FAIL_START;
                }
                break;
            case 340: // 5:40~ 5:45 감속 및 정지(task7)
            case 341:
            case 342:
            case 343:
            case 344:
            case 345:
                if (checkStop(arr)) {
                    task7 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 346: // task7 fail
                if (task7 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 405: // 6:45~ 6:52 유턴(task8)
            case 406:
            case 407:
            case 408:
            case 409:
            case 410:
            case 411:
            case 412:
                if (checkTurnLeft(arr)) {
                    task8 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 413: // task8 fail
                if (task8 == false) {
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
        task5 = false;
        task6 = false;
        task7 = false;
        task8 = false;
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
