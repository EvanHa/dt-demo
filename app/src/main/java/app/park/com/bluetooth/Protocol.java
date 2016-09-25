package app.park.com.bluetooth;

/**
 * Defines Protocol message.
 */
public interface Protocol {
    // msg protocol
    // Case command is play -> play////playnumber[1,2,3]
    // Case command is control -> contorl////speed////handle[0,1]////signalLight[0,1,2]////accel[0,1]////break[0,1]
    // Case old version command -> cmd////velocity////score

    public static final String SEPARATOR  = "////";
    public static final String CMD_PLAY  = "play";
    public static final String CMD_PAUSE = "pause";
    public static final String CMD_STOP  = "stop";
    public static final String CMD_GAMERUN  = "gamerun";
    public static final String CMD_ACK  = "ack";

    public static final int  INDEX_CMD = 0;
    public static final int  INDEX_SPEED = 1;
    public static final int  INDEX_HANDLE = 2;
    public static final int  INDEX_SIGNALLIGHT = 3;
    public static final int  INDEX_ACCEL = 4;
    public static final int  INDEX_BREAK = 5;

    public static final int  INDEX_PLAY_NUM = 1; // If command is play, next value is play video number
    public static final int  INDEX_SCORE = 2;   // Should be removed.

    public static final String PLAY_VIDEO_NUMBER1 = "1";
    public static final String PLAY_VIDEO_NUMBER2 = "2";
    public static final String PLAY_VIDEO_NUMBER3 = "3";

    public static final int MISSION_FAIl = 1;
    public static final int GAVE_OVER = 2;
}
