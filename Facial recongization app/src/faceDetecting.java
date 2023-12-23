import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class faceDetecting extends JFrame {

    private JLabel cameraScreen;
    private VideoCapture capture;
    private Mat image;
    private boolean photoClicked = false;
    private boolean uploadClicked = false;
    private String name;
    private Mat grayPicture;
    private Mat source;
    private final FaceDetector faceDetector;
    private JButton btnCapture;
    private boolean exitClicked = false;
    private final JButton exitButton = new JButton("Exit");
    private JButton uploadBtn;
    private String resultText;

    //creates the objected used to detect the face
    public faceDetecting() {
        initializeUI();
        faceDetector = new FaceDetector();
        //System.out.println(faceDetector.getDetectionResult());
    }

    //steps up the gui
    private void initializeUI() {
        setLayout(new BorderLayout());

        cameraScreen = new JLabel();
        add(cameraScreen, BorderLayout.CENTER);


        //adds actionlisterns for all the buttons
        btnCapture = new JButton("Capture");
        btnCapture.addActionListener(e -> photoClicked = true);

        uploadBtn = new JButton("Upload");
        uploadBtn.addActionListener(e -> uploadClicked = true);

        exitButton.addActionListener(e -> {
            remove(exitButton);
            btnCapture.setEnabled(true);
            btnCapture.setVisible(true);
            uploadBtn.setEnabled(true);
            uploadBtn.setVisible(true);
            exitClicked = true;
        });

        //adds the button to the jframe
        JPanel controlPanel = new JPanel();
        controlPanel.add(btnCapture);
        controlPanel.add(uploadBtn);
        controlPanel.add(exitButton);

        add(controlPanel, BorderLayout.SOUTH);

        //adds windowlistern to end code if you close of the screen
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                releaseResources();
                System.exit(0);
            }
        });

        setSize(new Dimension(640, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    //starts the camera and emptys out the frame and image matrix
    private void startCamera() {
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
                cameraScreen.setIcon(icon);

                if (photoClicked) {
                    handlePhotoCapture();
                }

                if (uploadClicked) {
                    handleImageUpload();
                }
            }
        } finally {
            releaseResources();
        }
    }

    private void handlePhotoCapture() {
        name = JOptionPane.showInputDialog(this, "Enter image name");
        if (name == null || name.isEmpty()) {
            name = new SimpleDateFormat("yyyy-MM-dd-hh-s").format(new Date());
        }

        Imgcodecs.imwrite("images/" + name + ".jpg", image);

        source = Imgcodecs.imread("images/" + name + ".jpg");
        convertImageToGrayScale();
        photoClicked = false;
    }

    private void handleImageUpload() {
        String filePath;
        JFileChooser choose = new JFileChooser();
        int resultfile = choose.showOpenDialog(null);

        if (resultfile == JFileChooser.APPROVE_OPTION) {
            File selectedFile = choose.getSelectedFile();

            if (selectedFile != null) {
                filePath = selectedFile.getAbsolutePath();
                handleImageProcessing(filePath);
            } else {
                System.out.println("File didn't open up");
            }
        }
        uploadClicked = false;
    }

    private void handleImageProcessing(String filePath) {
        Mat originalImage = Imgcodecs.imread(filePath);

        if (!originalImage.empty()) {
            Mat clonedImage = new Mat();
            originalImage.copyTo(clonedImage);

            name = JOptionPane.showInputDialog(this, "Enter image name");
            if (name == null || name.isEmpty()) {
                name = new SimpleDateFormat("yyyy-MM-dd-hh-s").format(new Date());
            }

            String outputhPath = "images/" + name + ".jpg";
            Imgcodecs.imwrite(outputhPath, clonedImage);

            source = Imgcodecs.imread("images/" + name + ".jpg");
            Mat resizeImage = new Mat(560, 640, source.type());
            Imgproc.resize(source, resizeImage, resizeImage.size(), 0, 0, Imgproc.INTER_CUBIC);
            source = resizeImage;
            image = source;
            convertImageToGrayScale();
            photoClicked = false;
        }
    }

    private void convertImageToGrayScale() {
        grayPicture = new Mat();
        if (source.empty()) {
            return;
        }

        Imgproc.cvtColor(source, grayPicture, Imgproc.COLOR_RGB2GRAY);
        Imgproc.equalizeHist(grayPicture, grayPicture);
        faceDetector.detectFaces(grayPicture);
    }

    private void releaseResources() {
        if (capture != null) {
            capture.release();
        }
        if (image != null) {
            image.release();
        }
    }

    private void displayImage() {
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
        }
    }

    private void detectEmotions() {
        resultText = faceDetector.getDetectionResult();
        displayImage();
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        SwingUtilities.invokeLater(() -> {
            Camera camera = new Camera();
            new Thread(camera::startCamera).start();
        });
    }
}

class FaceDetector {
    private final CascadeClassifier faceFinder;
    private List<Rect> listOfFaces;

    public FaceDetector() {
        faceFinder = new CascadeClassifier();
        loadClassifier();
    }

    private void loadClassifier() {
        String fileName = "C:\\Users\\ASC_Student\\IdeaProjects\\practice\\src\\haarcascade_frontalface_alt.xml";

        if (!faceFinder.load(fileName)) {
            System.out.println("Error loading face classifier");
            System.exit(0);
        }

        if (faceFinder.empty()) {
            System.out.println("classifier is empty");
            System.exit(0);
        }
    }

    public void detectFaces(Mat grayPicture) {
        MatOfRect faceInPicture = new MatOfRect();
        faceFinder.detectMultiScale(grayPicture, faceInPicture);

        listOfFaces = faceInPicture.toList();


    }

    public String getDetectionResult() {
        return listOfFaces.isEmpty() ?
                "There are no faces to detect emotions" :
                "There are " + listOfFaces.size() + " faces detected";
    }
}