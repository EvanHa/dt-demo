package app.park.com.task;

public class MissionManager {
    public static final String TAG = MissionManager.class.getSimpleName();

    public static String currentVideo;
    public static MissionOne mOne = new MissionOne();
    public static MissionTwo mTwo = new MissionTwo();
    public static MissionThree mThree = new MissionThree();

    public static void setVideoNumber(String number) {
        currentVideo = number;
    }

    public static int getVideoNumber() {
        int number;
        if (currentVideo == null) {
            number = 1; // defalt video
        } else {
            number = Integer.parseInt(currentVideo);
        }
        return number;
    }

    public static int validate(String[] arr, long currentTime) {
        int result = 0;
        int videoNum = getVideoNumber();
        switch (videoNum) {
            case 1:
                result = mOne.validate(arr, currentTime);
                break;
            case 2:
                result = mTwo.validate(arr, currentTime);
                break;
            case 3:
                result = mThree.validate(arr, currentTime);
                break;
            default:
                break;
        }
        return  result;
    }

    public static void reInit() {
        int videoNum = getVideoNumber();
        switch (videoNum) {
            case 1:
                mOne.reInit();
                break;
            case 2:
                mTwo.reInit();
                break;
            case 3:
                mThree.reInit();
                break;
            default:
                break;
        }
    }
}
