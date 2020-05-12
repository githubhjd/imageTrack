
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;


/**
 * @Description
 * @Author hjd
 * @Date 2019年07月23日 10:55
 **/
public class ImageIdentify {

    public static Integer maxDisappeared = 25;
    public static Integer nextObjectId = 0;
    // 初始化对象队列
    public static Map<Integer, Point> objects = new LinkedHashMap<>();
    public static Map<Integer, Integer> disappeared = new LinkedHashMap<>();
    public static List<Integer> hasOptionList = new ArrayList<>();

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        // 打开摄像头或者视频文件
        VideoCapture capture = new VideoCapture();
        // capture.open(0);
        capture.open("D:\\Files\\video\\20190926_131808.MP4");

        if(!capture.isOpened()) {
            System.out.println("could not load video data...");
            return;
        }
        int frame_width = (int)capture.get(CAP_PROP_FRAME_WIDTH);
        int frame_height = (int)capture.get(CAP_PROP_FRAME_HEIGHT);
        ImageGUI gui = new ImageGUI();
        ImageGUI gui1 = new ImageGUI();
        gui.createWin("Java图像跟踪", new Dimension(frame_width, frame_height));
        gui1.createWin("result", new Dimension(1894, 1012));
        Mat frame = new Mat();
        Mat bg = Imgcodecs.imread("D:\\Files\\video\\bg.bmp");
        Imgproc.cvtColor(bg, bg, Imgproc.COLOR_RGB2GRAY);

        while(true) {
            boolean have = capture.read(frame);
            if(!have) break;
            if(!frame.empty()) {

                // 二值化处理
                Rect rect = new Rect(0, 0, 1894, 1012);
                frame = new Mat(frame, rect);
                Mat result = frame.clone(); // 复制界面
                Mat frame1 = new Mat();
                Imgproc.cvtColor(frame, frame1, Imgproc.COLOR_RGB2GRAY);
                Mat diff = new Mat();
                Core.absdiff(bg, frame1, diff);
                // 阈值
                Imgproc.threshold(diff, diff, 15, 255, Imgproc.THRESH_BINARY);
                // 腐蚀
                Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8));
                Imgproc.erode(diff, diff, element);
//                gui.imshow(conver2Image(diff));
//                gui.repaint();
                // 膨胀
                Mat element2 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(100, 100));
                Imgproc.dilate(diff, diff, element2);
                gui.imshow(conver2Image(diff));
                gui.repaint();
                // 发现轮廓并画最大外接矩形
                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(diff, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
                int n = 0;
                List<Point> inputCentroids = new ArrayList<>();
                Map<Point, Rect> RectsMap = new HashMap<>();

                for (MatOfPoint mf : contours) {
                    // 筛选面积
                    if (Imgproc.contourArea(mf) < 1000) {
                        continue;
                    }
//                    Imgproc.fillConvexPoly(frame, mf, new Scalar(0, 255, 255));
                    Rect r = Imgproc.boundingRect(mf);
                    // 跟踪框画到视频上
                    if ((r.x + r.width) < frame_width*0.95 && r.width > 40 && r.height > 40 && r.x > frame_width*0.05) {
                        Imgproc.rectangle(result, r.tl(), r.br(), new Scalar(0, 255, 0), 2, 8);
                    }
                    Double cX = (r.x + r.width) / 2.0;
                    Double cY = (r.y + r.height) / 2.0;
//                    System.out.print(cX);
//                    System.out.print(cY);
                    Point inputCentroid = new Point(cX, cY);
                    RectsMap.put(inputCentroid, r);

                    // 跟踪框x+width 小于视频总长度
                    if ((r.x + r.width) < frame_width*0.95 && r.width > 40 && r.height > 40 && r.x > frame_width*0.05) {
                        inputCentroids.add(inputCentroid);
                    }
                    n++;
                }

                Map<Integer, Point> objects = new HashMap<>();
                try {
                    objects = update(inputCentroids);
                } catch (Exception e) {
                    System.out.println(e);
                }
                if (objects.size() != 0){
                    for (Integer objectID: objects.keySet()) {
                        String text = String.format("ID {" + objectID + "}");
                        Point centroid = objects.get(objectID);
//                    Imgproc.putText(result, text, centroid, FONT_HERSHEY_SIMPLEX,  0.5, new Scalar(0, 255, 0), 2);
//                    Imgproc.circle(result, centroid, 4, new Scalar(0, 255, 0), -1);
                        while (!hasOptionList.contains(objectID)){
                            Mat img = new Mat(frame, RectsMap.get(centroid));
                            //输出图片
                            Imgcodecs.imwrite("D:\\Files\\test" + File.separator + System.currentTimeMillis() + "." + "jpg", img);
                            hasOptionList.add(objectID);
                        }
                    }

                    // 视频展示
                    gui1.imshow(conver2Image(result));
                    gui1.repaint();
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static BufferedImage conver2Image(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int dims = mat.channels();
        int[] pixels = new int[width*height];
        byte[] rgbdata = new byte[width*height*dims];
        mat.get(0, 0, rgbdata);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int index = 0;
        int r=0, g=0, b=0;
        for(int row=0; row<height; row++) {
            for(int col=0; col<width; col++) {
                if(dims == 3) {
                    index = row*width*dims + col*dims;
                    b = rgbdata[index]&0xff;
                    g = rgbdata[index+1]&0xff;
                    r = rgbdata[index+2]&0xff;
                    pixels[row*width+col] = ((255&0xff)<<24) | ((r&0xff)<<16) | ((g&0xff)<<8) | b&0xff;
                }
                if(dims == 1) {
                    index = row*width + col;
                    b = rgbdata[index]&0xff;
                    pixels[row*width+col] = ((255&0xff)<<24) | ((b&0xff)<<16) | ((b&0xff)<<8) | b&0xff;
                }
            }
        }
        setRGB( image, 0, 0, width, height, pixels);
        return image;
    }

    /**
     * A convenience method for setting ARGB pixels in an image. This tries to avoid the performance
     * penalty of BufferedImage.setRGB unmanaging the image.
     */
    public static void setRGB( BufferedImage image, int x, int y, int width, int height, int[] pixels ) {
        int type = image.getType();
        if ( type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB )
            image.getRaster().setDataElements( x, y, width, height, pixels );
        else
            image.setRGB( x, y, width, height, pixels, 0, width );
    }

    public static Double[][] myCdist(List<Point> objects, List<Point> inputCentroids){
        Double[][] result = new Double[objects.size()][inputCentroids.size()];
        for (int i = 0; i < objects.size(); i++){
            for (int j = 0; j < inputCentroids.size(); j++){
                double sum = Euclid(objects.get(i), inputCentroids.get(j));
                result[i][j] = sum;
            }
        }
        return result;
    }

    /*欧几里德距离公式*/
    public static double Euclid(Point object, Point centroid){
        double n = Double.parseDouble(String.valueOf(object.x));
        double m = Double.parseDouble(String.valueOf(object.y));
        double x = Double.parseDouble(String.valueOf(centroid.x));
        double y = Double.parseDouble(String.valueOf(centroid.y));
        double sum;
        sum=Math.sqrt((n-x)*(n-x)+(m-y)*(m-y));
//        System.out.printf("%.2f\r\n",sum);
        return sum;
    }

    public static Integer[] minArgSort(Double[][] arr) {
        Double[] myArray = new Double[arr.length];
        for (int i = 0; i < arr.length; i++){
            Double min = arr[i][0];
            //获取
            Integer Index = 0;
            for (int j = 0; j < arr[i].length; j++){
                Double num = arr[i][j];
                if (min > num){
                    min = num;
                    Index = j;
                }
            }
            myArray[i] = min;
        }
        Integer[] result = argSort(myArray);
        return result;
    }

    /*可重复的数组排序后得到索引*/
    public static Integer[] argSort(Double[] arr){
        Integer[] result = new Integer[arr.length];
        Double[] copy = new Double[arr.length];
        for (int i = 0; i < arr.length; i++){
            copy[i] = arr[i];
        }
        Arrays.sort(copy);
        for (int i = 0; i < arr.length; i++){
            for (int j = 0; j < copy.length; j++){
                if (copy[j] == arr[i]){
                    if (copy[j] != -1.0){
                        result[i] = j;
                    }
//                    System.out.print(result[i]);
                }
            }
        }
        return result;
    }


    public static Integer[] argMinRows(Double[][] arr, Integer[] rows){
        Integer[] myArray = new Integer[arr.length];
        Integer[] result = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++){
            Double min = arr[i][0];
            //获取
            Integer Index = 0;
            for (int j = 0; j < arr[i].length; j++){
                Double num = arr[i][j];
                if (min > num){
                    min = num;
                    Index = j;
                }
            }
            myArray[i] = Index;
        }
        for (int i = 0; i < rows.length; i++){
            result[i] = myArray[rows[i]];
        }
        return result;
    }

    //求差集
    public static Set<Integer> difference(Set<Integer> set1, Set<Integer> set2){
        Set<Integer> result = new HashSet<>();
        result.clear();
        result.addAll(set1);
        result.removeAll(set2);
        return result;

    }

    //注册对象
    public static void register(Point centroid){
        objects.put(nextObjectId, centroid);
        System.out.print("Register:  " + nextObjectId + "\n");
        disappeared.put(nextObjectId, 0);
        nextObjectId++;
    }

    //取消对象
    public static void deRegister(Integer objectID){
        objects.remove(objectID);
        hasOptionList.remove(objectID);
        System.out.print("deRegister:  " + objectID + "\n");
        disappeared.remove(objectID);
    }

    //更新对象
    public static Map<Integer, Point> update(List<Point> inputCentroids){

        if (objects.size() == 0) {
            for (int i = 0; i < inputCentroids.size(); i++){
                // register object
                register(inputCentroids.get(i));
            }
        } else {
            List<Integer> objectIDs = new ArrayList<>();
            Iterator iter1 = objects.entrySet().iterator();
            while (iter1.hasNext()){
                Map.Entry entry = (Map.Entry) iter1.next();
                objectIDs.add(Integer.parseInt(String.valueOf(entry.getKey())));
            }
            List<Point> objectCentroids = new ArrayList<>();
            for (Point value : objects.values()){
                objectCentroids.add(value);
            }

            Double[][] D = myCdist(objectCentroids, inputCentroids);
            Integer[] rows = minArgSort(D);
            Integer[] cols = argMinRows(D, rows);

            HashSet<Integer> usedRows = new HashSet<>();
            HashSet<Integer> usedCols = new HashSet<>();

            for (int i = 0; i < rows.length; i++){
                if (D[rows[i]][cols[i]] > 100) continue;
                Integer objectID = objectIDs.get(rows[i]);
                objects.put(objectID, inputCentroids.get(cols[i]));
                disappeared.put(objectID, 0);

                usedRows.add(rows[i]);
                usedCols.add(cols[i]);
            }
            Set<Integer> rowSet = new HashSet<>();
            Set<Integer> colSet = new HashSet<>();
            for (int i = 0; i < D.length; i++){
                rowSet.add(i);
            }
            for (int i = 0; i < D[0].length; i++){
                colSet.add(i);
            }
            Set<Integer> unusedRows = difference(rowSet, usedRows);
            Set<Integer> unusedCols = difference(colSet, usedCols);

            if (D.length >= D[0].length){
                //设置阈值，判断对象是否消失
                for (Integer row: unusedRows){
                    Integer objectID = objectIDs.get(row);
                    Integer num = disappeared.get(objectID) + 1;
                    disappeared.put(objectID, num);
                    System.out.print("第"+ objectID +"对象的disappeared值为"+ num +"\n");
                    if (disappeared.get(objectID) > maxDisappeared) {
                        deRegister(objectIDs.get(row));
                    }
                }
            } else {
                for (Integer col: unusedCols){
                    register(inputCentroids.get(col));
                }
            }
        }
        return objects;
    }
}
