package app.park.com.task;

import android.util.Log;

public class MissionTwo extends Mission {
    public static final String TAG = MissionTwo.class.getSimpleName();

    public static boolean task1 = false; // 0:05
    public static boolean task2 = false; // 0:14 - 0:19
    public static boolean task3 = false; // 0:45
    public static boolean task4 = false;
    public static boolean task5 = false;
    public static boolean task6 = false;
    public static boolean task7 = false;
    public static boolean task8 = false;
    public static boolean task9 = false;
    public static boolean task10 = false;
    public static boolean task11 = false;
    public static boolean task12 = false;
    public static boolean task13 = false;
    public static boolean task14 = false;
    public static boolean task15 = false;
    public static boolean task16 = false;
    public static boolean task17 = false;
    public static boolean task18 = false;

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
            case 5: // 0:05 좌회전 - task1
                if (checkTurnLeft(arr)) {
                    task1 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 6: // task1 fail
                if (task1 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 14: // 0:14 - 0:19 정지 - task2
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
                if (checkStop(arr)) {
                    task2 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 20: // task2 fail
                if (task2 == false) {
                    returnPenalty = MISSION_FAIL_STOP;
                }
                break;
            case 45: // 0:45 재출발 - task3
                if (checkStart(arr)) {
                    task3 = true;
                    returnPenalty = MISSION_CLEAR;
                }
            case 46: // task3 fail
                if (task3 == false) {
                    returnPenalty = MISSION_FAIL_START;
                }
                break;
            case 206: // 3:26 우측 차선변경 - task4
                if (checkTurnRight(arr)) {
                    task4 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 207: // task4 fail
                if (task4 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 250: // 4:10- 4:16 감속 및 정지 - task5
            case 251:
            case 252:
            case 253:
            case 254:
            case 255:
            case 256:
                if (checkStop(arr)) {
                    task5 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 257: // task5 fail
                if (task5 == false) {
                    returnPenalty = MISSION_FAIL_STOP;
                }
                break;
            case 266: // 4:26 재출발 - task6
                if (checkStart(arr)) {
                    task6 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 267: // task6 fail
                if (task6 == false) {
                    returnPenalty = MISSION_FAIL_START;
                }
                break;
            case 270: // 4:30- 4:37 좌회전 - task7
            case 271:
            case 272:
            case 273:
            case 274:
            case 275:
            case 276:
            case 277:
                if (checkTurnLeft(arr)) {
                    task7 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 278: // task7 fail
                if (task7 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 287: // 4:47 우측방향 차선 변경 - task8
                if (checkTurnRight(arr)) {
                    task8 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 288: // task8 fail
                if (task8 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 292: // 4:52 우측방향 차선 변경 - task9
                if (checkTurnRight(arr)) {
                    task9 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 293: // task9 fail
                if (task9 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 332: // 5:32- 5:36 우회전 - task10
            case 333:
            case 334:
            case 335:
            case 336:
                if (checkTurnRight(arr)) {
                    task10 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 337: // task10 fail
                if (task10 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 351: // 5:51 우회전 - task11
                if (checkTurnRight(arr)) {
                    task11 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 352: // task11 fail
                if (task11 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 383: // 6:23 정지 - task12
                if (checkStop(arr)) {
                    task12 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 384: // task12 fail
                if (task12 == false) {
                    returnPenalty = MISSION_FAIL_STOP;
                }
                break;
            case 420: // 7:00 재출발 - task13
                if (checkStart(arr)) {
                    task13 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 421: // task13 fail
                if (task13 == false) {
                    returnPenalty = MISSION_FAIL_START;
                }
                break;
            case 482: // 8:02 좌측 방향 차선 변경 - task14
                if (checkTurnLeft(arr)) {
                    task14 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 483: // task14 fail
                if (task14 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 494: // 8:14 - 8:19 정지 - task15
            case 495:
            case 496:
            case 497:
            case 498:
            case 499:
                if (checkStop(arr)) {
                    task15 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 500: // task15 fail
                if (task15 == false) {
                    returnPenalty = MISSION_FAIL_STOP;
                }
                break;
            case 617: // 10:17 재출발 - task16
                if (checkStart(arr)) {
                    task16 =true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 618: // task16 fail
                if (task16 == false) {
                    returnPenalty = MISSION_FAIL_START;
                }
                break;
            case 619: // 10:19 - 10:27 좌회전 - task17
            case 620:
            case 621:
            case 622:
            case 623:
            case 624:
            case 625:
            case 626:
            case 627:
                if (checkTurnLeft(arr)) {
                    task17 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 628: // task17 fail
                if (task17 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 638: // 10:38 - 10:44 우회전 - task18
            case 639:
            case 640:
            case 641:
            case 642:
            case 643:
            case 644:
                if (checkTurnRight(arr)) {
                    task18 = true;
                    returnPenalty = MISSION_CLEAR;
                }
                break;
            case 645: // task18 fail
                if (task18 == false) {
                    returnPenalty = MISSION_FAIL_TURN;
                }
                break;
            case 653: // 10:53 종료
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
        task9 = false;
        task10 = false;
        task11 = false;
        task12 = false;
        task13 = false;
        task14 = false;
        task15 = false;
        task16 = false;
        task17 = false;
        task18 = false;
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
