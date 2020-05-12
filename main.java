import org.opencv.core.Core;
import org.opencv.videoio.VideoCapture;

import java.util.*;

import static java.util.Arrays.asList;

/**
 * @author huangjiadong
 * @date 2019/08/28 14:20
 */

public class main {

    public static void main(String[] args) {


        // 获取视频的帧率
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // 打开摄像头或者视频文件
        VideoCapture capture = new VideoCapture();
        capture.open("F:\\Files\\video\\201908301029.MP4");

    }

}
