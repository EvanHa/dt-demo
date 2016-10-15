package app.park.com.task;

public interface MissionInterface {
    public int validate(String[] arr, long currentTime);
    public int safeLongToInt(long l);
    public boolean checkStart(String[] arr);
    public boolean checkStop(String[] arr);
    public boolean checkTurnLeft(String[] arr);
    public boolean checkTurnRight(String[] arr);
    public void reInit();
}
