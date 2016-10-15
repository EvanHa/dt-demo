package app.park.com.task;

import app.park.com.bluetooth.Protocol;

public class Mission implements MissionInterface {

    protected int MISSION_CLEAR = 0;
    protected int MISSION_FAIL_START = -5; //신호등 초록불인데 출발 하지 않았을 때
    protected int MISSION_FAIL_STOP = -5; //신호등이 빨간불이거나 앞에 차가 있는데 멈추지 않았을 때
    protected int MISSION_FAIL_TURN = -10; //좌측 코너링 or 좌측으로 차선 변경을 할 때 방향지시등과 핸들을 틀지 않았을 때

    @Override
    public int validate(String[] arr, long currentTime) {
        return 0;
    }

    @Override
    public int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    @Override
    public boolean checkStart(String[] arr) {
        boolean result = false;
        if (arr[Protocol.INDEX_ACCEL].equals(Protocol.ACCEL_ON)) {
            result = true;
        }
        return result;
    }

    @Override
    public boolean checkStop(String[] arr) {
        boolean result = false;
        if (arr[Protocol.INDEX_BREAK].equals(Protocol.BREAK_ON)) {
            result = true;
        }
        return result;
    }

    @Override
    public boolean checkTurnLeft(String[] arr) {
        boolean result = false;
        if (arr[Protocol.INDEX_SIGNALLIGHT].equals(Protocol.SIGNALLIGHT_LEFT) &&
                arr[Protocol.INDEX_HANDLE].equals(Protocol.HANDLE_LEFT)) {
            result = true;
        }
        return result;
    }

    @Override
    public boolean checkTurnRight(String[] arr) {
        boolean result = false;
        if (arr[Protocol.INDEX_SIGNALLIGHT].equals(Protocol.SIGNALLIGHT_RIGHT)
                && arr[Protocol.INDEX_HANDLE].equals(Protocol.HANDLE_RIGHT)) {
            result = true;
        }
        return result;
    }

    @Override
    public void reInit() {

    }
}
