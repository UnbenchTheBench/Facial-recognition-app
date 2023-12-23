import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

//import deepface.*;

import java.awt.event.*;
import java.io.*;
import java.util.*;



import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.opencv.imgproc.Imgproc.INTER_CUBIC;


//add happy and sad
//add text on picture to display results
//learning how to project into an application package /excutable

public class Camera extends JFrame { //Camera can now alter the JFrame

    // private List<String> emotions = ['happy','sad'];

    private final JLabel cameraScreen;

    private VideoCapture capture;
    private Mat image;

    private boolean photoClicked = false;

    private boolean uploadClicked = false;

    private String name;

    private Mat grayPicture;

    private Mat source;

    private final CascadeClassifier faceFinder = new CascadeClassifier();

    private List<Rect> listOfFaces;

    private final JButton btnCapture;

    private boolean exitClicked = false;

    private final JButton exitButton = new JButton("Exit");

    private final JButton uploadBtn;

    private String resultText;


    public Camera() {

        setLayout(null);
        cameraScreen = new JLabel();
        cameraScreen.setBounds(0, 0, 640, 480);
        add(cameraScreen);

        btnCapture = new JButton("capture");
        btnCapture.setBounds(340, 480, 80, 40);
        add(btnCapture);
        uploadBtn = new JButton("Upload");
        uploadBtn.setBounds(260, 480, 80, 40);
        add(uploadBtn);

        btnCapture.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                photoClicked = true;
            }
        });

        uploadBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadClicked = true;
            }
        });

        //turn off the code if you close the window
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                capture.release();
                image.release();
                System.exit(0);
            }

        });

        setSize(new Dimension(640, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);

    }


    //create camera

    public void startCamera() {
        capture = new VideoCapture(0);
        image = new Mat();
        byte[] imageData;

        ImageIcon icon;
        try {
            while (true) {
                if (!capture.read(image) || image.empty()) {
                    continue;
                }


                MatOfByte buf = new MatOfByte();
                Imgcodecs.imencode(".jpg", image, buf);


                imageData = buf.toArray();
                icon = new ImageIcon(imageData);
                cameraScreen.setIcon(icon); //put the icon back on the screen so the user can see the results
                if (photoClicked) {
                    name = JOptionPane.showInputDialog(this, "Enter image name");
                    if (name == null) {
                        name = new SimpleDateFormat("yyyy-MM-dd-hh-s").format(new Date());
                    }
                    Imgcodecs.imwrite("images/" + name + ".jpg", image);

                    source = Imgcodecs.imread("images/" + name + ".jpg");

                    ConvertImagetoGrayScale();


                    photoClicked = false;
                }
                if (uploadClicked) {
                    String filePath;
                    JFileChooser choose = new JFileChooser();
                    int resultfile = choose.showOpenDialog(null);

                    if (resultfile == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = choose.getSelectedFile();

                        if (selectedFile != null) {
                            filePath = selectedFile.getAbsolutePath();
                            String path = choose.getSelectedFile().getAbsolutePath();
                            String outpath = "images/";


                            Mat originalImage = Imgcodecs.imread(filePath);

                            if (!originalImage.empty()) {
                                Mat clonedImage = new Mat();
                                originalImage.copyTo(clonedImage);

                                String outputFileName = "";

                                name = JOptionPane.showInputDialog(this, "Enter image name");
                                if (name == null) {
                                    name = new SimpleDateFormat("yyyy-MM-dd-hh-s").format(new Date());
                                }

                                String outputhPath = "images/" + name + ".jpg";
                                Imgcodecs.imwrite(outputhPath, clonedImage);
                                source = Imgcodecs.imread("images/" + name + ".jpg");

                                Mat resizeImage = new Mat(560, 640, source.type());
                                Imgproc.resize(source, resizeImage, resizeImage.size(), 0, 0, INTER_CUBIC);
                                source = resizeImage;
                                image = source;
                                ConvertImagetoGrayScale();

                                photoClicked = false;


                            }


                        } else {
                            System.out.println("File didn't open up");

                        }
                    }
                    uploadClicked = false;
                }
            }
        } finally {
            capture.release();
            image.release();
        }

    }

    public void ConvertImagetoGrayScale() {


        grayPicture = new Mat();
        if (source.empty()) {
            return;
        }
        //Mat destionation = new Mat();


        Imgproc.cvtColor(source, grayPicture, Imgproc.COLOR_RGB2GRAY);
        Imgproc.equalizeHist(grayPicture, grayPicture);
        loadClassifier();

    }

    public void loadClassifier() throws CvException {
        String fileName = "C:\\Users\\ASC_Student\\IdeaProjects\\practice\\src\\haarcascade_frontalface_alt.xml";

        if (!faceFinder.load(fileName)) {
            System.out.println("Error loading face classifier");
            System.exit(0);
        }


        if (faceFinder.empty()) {
            System.out.println("classifier is empty");
            System.exit(0);
        }


        detectFace();

    }

    public void detectFace() {

        MatOfRect faceInPicture = new MatOfRect();
        faceFinder.detectMultiScale(grayPicture, faceInPicture);

        listOfFaces = faceInPicture.toList();

        Scalar color = new Scalar(0, 0, 255); //not rgb but instead bgr
        //Scalar color = getScalar(); //self created code based on the emotion

        for (Rect face : listOfFaces) {
            Point center = new Point(face.x + (double) face.width / 2, face.y + (double) face.height / 2);
            Imgproc.ellipse(image, center, new Size((double) face.width / 2, (double) face.height / 2), 0, 0, 360, color);
        }

        Imgcodecs.imwrite("images/" + name + ".jpg", image);

        detectEmotions();


    }

    public void detectEmotions() {
        final String key = "23cc1011769e4027bbdf78650d85ac35";
        if (listOfFaces.isEmpty()) {
            resultText = "Their are no faces to detect emotions";
        } else {





            resultText = "Their are " + listOfFaces.size() + " detected";
        }


        displayImage();
    }

    public void displayImage() {
        photoClicked = true;
        byte[] imageData;
        ImageIcon icon;
        final MatOfByte buf = new MatOfByte();
        Imgcodecs.imencode(".jpg", image, buf);


        imageData = buf.toArray();
        icon = new ImageIcon(imageData);
        cameraScreen.setIcon(icon);
        exitClicked = false;
        while (!exitClicked) {
            btnCapture.setEnabled(false);
            btnCapture.setVisible(false);
            uploadBtn.setEnabled(false);
            uploadBtn.setVisible(false);


            exitButton.setBounds(300, 480, 80, 40);
            add(exitButton);

            exitButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    remove(exitButton);
                    btnCapture.setEnabled(true);
                    btnCapture.setVisible(true);
                    uploadBtn.setEnabled(true);
                    uploadBtn.setVisible(true);

                    exitClicked = true;
                }
            });


        }
    }

    public void getEmotion() {
         //model = DeepFace.build_model("Emotion");


    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                Camera camera = new Camera();
                new Thread(new Runnable() {
                    public void run() {
                        camera.startCamera();
                    }
                }).start();
            }
        });

    }


    public class Emotion {



    }

    public class Happy extends Emotion {
        public void displayEmotion(){

        }
    }
    public class Sad extends Emotion {
       public void displayEmotion(){

       }
    }

    public class photo {

    }

    public class faceInPhoto extends photo {

    }

    public class noFaceInPhoto extends photo {

    }



}