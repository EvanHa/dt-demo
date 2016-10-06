package app.park.com.task;

import android.util.Log;

public class MissionOne extends Mission {
    public static final String TAG = MissionOne.class.getSimpleName();

    public boolean task1 = false; // 1:30 ~ 1:40 우측 차선 변경
    public boolean task2 = false; // 2:15 ~ 2:20 감속 및 정지
    public boolean task3 = false; // 2:46 ~ 2:56 재 출발 및 유턴
    public boolean task4 = false; // 3:30 ~ 3:35 좌측 차선 변경
    public boolean task5 = false; // 4:30 ~ 4:35 감속
    public boolean task6 = false; // 4:55 ~ 5:00 좌측 차선 변경
    public boolean task7 = false; // 5:25 ~ 5:30 감속 및 정지
    public boolean task8 = false; // 5:45 ~ 5:55 재출발 및 유턴

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
            case 90: // 1:30 ~ 1:40 우측 차선 변경(task1)
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
                if (checkTurnRight(arr)) {
                    task1 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 101: // task1 fail
                if (task1 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 135: // 2:15 ~ 2:20 감속 및 정지(task2)
            case 136:
            case 137:
            case 138:
            case 139:
            case 140:
                if (checkStop(arr)) {
                    task2 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 141: // task2 fail
                if (task2 == false) {
                    returnPenalty = MISSION_FAIL_STOP;
                }
                break;
            case 166: // 2:46 ~ 2:56 재 출발 및 유턴 (task3)
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 172:
            case 173:
            case 174:
            case 175:
            case 176:
                if (checkStart(arr)&&checkTurnLeft(arr)) {
                    task3 = true;
                    returnPenalty = MISSION_CLEAR;
                }
            case 177: // task3 fail
                if (task3 == false) {
                    returnPenalty = MISSION_FAIL_START;
                }
                break;
            case 210: // 3:30 ~ 3:35 좌측 차선 변경(task4)
            case 211:
            case 212:
            case 213:
            case 214:
            case 215:
                if (checkTurnLeft(arr)) {
                    task4 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 216: // task4 fail
                if (task4 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 270: // 4:30 ~ 4:35 감속(task5)
            case 271:
            case 272:
            case 273:
            case 274:
            case 275:
                if (checkStop(arr)) {
                    task5 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 276: // task5 fail
                if (task5 == false) {
                    returnPenalty = MISSION_FAIL_STOP;
                }
                break;
            case 295: // 4:55 ~ 5:00 좌측 차선 변경(task6)
            case 296:
            case 297:
            case 298:
            case 299:
            case 300:
                if (checkTurnLeft(arr)) {
                    task6 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 301: // task6 fail
                if (task6 == false) {
                    returnPenalty = MISSION_FAIL_START;
                }
                break;
            case 325: // 5:25 ~ 5:30 감속 및 정지(task7)
            case 326:
            case 327:
            case 328:
            case 329:
            case 330:
                if (checkStop(arr)) {
                    task7 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 331: // task7 fail
                if (task7 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 345: // 5:45 ~ 5:55 재출발 및 유턴(task8)
            case 346:
            case 347:
            case 348:
            case 349:
            case 350:
            case 351:
            case 352:
            case 353:
            case 354:
            case 355:
                if (checkStart(arr)&&checkTurnLeft(arr)) {
                    task8 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 356: // task8 fail
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
