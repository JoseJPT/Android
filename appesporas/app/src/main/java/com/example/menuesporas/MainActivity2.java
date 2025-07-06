package com.example.menuesporas;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
// import android.widget.Toast; // Eliminado: No se necesita más para los Toast

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {

    ImageView imageView;
    TextView textView;
    Bitmap imageBitmap;
    String tipoHongo = ""; // Esta variable se sigue usando para la lógica interna

    private static final String TAG = "MainActivity2";

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Error al cargar OpenCV");
        } else {
            Log.d(TAG, "OpenCV cargado exitosamente");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain2);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.texto);
        Button btnAbrirGaleria = findViewById(R.id.btnAbrirGaleria);

        Intent intent = getIntent();
        tipoHongo = intent.getStringExtra("tipo_espora");

        // Se mantiene este log para depuración, pero no se muestra al usuario.
        if (tipoHongo != null && !tipoHongo.isEmpty()) {
            Log.d(TAG, "Modo de Hongo recibido: " + tipoHongo);
        } else {
            tipoHongo = "default";
            Log.d(TAG, "Modo de Hongo: default (no especificado en Intent)");
        }

        btnAbrirGaleria.setOnClickListener(v -> {
            Log.d(TAG, "Botón 'Abrir Galería' presionado");
            abrirGaleria();
        });
    }

    private void abrirGaleria() {
        Log.d(TAG, "Abriendo galería de imágenes");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "Imagen seleccionada desde la galería. Request Code: " + requestCode + ", Result Code: " + resultCode);
            Uri imageUri = data.getData();
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                procesarImagen(imageBitmap);
            } catch (IOException e) {
                Log.e(TAG, "Error al cargar imagen desde URI: " + e.getMessage());
                e.printStackTrace();
                // Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show(); // Eliminado: Toast de error
            }
        } else {
            Log.d(TAG, "Selección de imagen cancelada o fallida. Request Code: " + requestCode + ", Result Code: " + resultCode);
        }
    }

    private void procesarImagen(Bitmap bitmapOriginal) {
        Log.d(TAG, "Iniciando procesamiento de imagen...");
        Mat matOriginal = new Mat();
        Utils.bitmapToMat(bitmapOriginal, matOriginal);
        Log.d(TAG, "Bitmap convertido a Mat.");

        Mat imgProcesar = new Mat();
        Imgproc.resize(matOriginal, imgProcesar, new Size(640, 480));
        Log.d(TAG, "Imagen redimensionada a 640x480.");

        Mat imgGray = new Mat();
        Imgproc.cvtColor(imgProcesar, imgGray, Imgproc.COLOR_BGR2GRAY);
        Log.d(TAG, "Imagen convertida a escala de grises.");

        Mat imgEqualized = new Mat();
        Imgproc.equalizeHist(imgGray, imgEqualized);
        Log.d(TAG, "Ecualización de histograma aplicada.");

        Imgproc.GaussianBlur(imgEqualized, imgEqualized, new Size(3, 3), 0);
        Log.d(TAG, "Desenfoque Gaussiano aplicado.");

        // --- INICIO DE AJUSTES DE PARÁMETROS SEGÚN EL TIPO DE HONGO ---
        int adaptiveBlockSize = 15;
        double adaptiveC = 5;
        double minArea = 3.0;
        double maxArea = 70.0;
        double minCircularity = 0.50;
        double minSolidity = 0.80;
        int openIterations = 2;
        int closeIterations = 1;
        int drawContourThickness = 2;

        switch (tipoHongo) {
            case "Hongo 1":
                adaptiveBlockSize = 17;
                adaptiveC = 8;
                minArea = 5.0;
                maxArea = 100.0;
                minCircularity = 0.65;
                minSolidity = 0.85;
                openIterations = 2;
                closeIterations = 2;
                drawContourThickness = 3;
                break;
            case "Hongo 2":
                adaptiveBlockSize = 9;
                adaptiveC = 2;
                minArea = 0.5;
                maxArea = 25.0;
                minCircularity = 0.75;
                minSolidity = 0.90;
                openIterations = 1;
                closeIterations = 1;
                drawContourThickness = 1;
                break;
            case "Hongo 3":
                adaptiveBlockSize = 21;
                adaptiveC = 10;
                minArea = 10.0;
                maxArea = 150.0;
                minCircularity = 0.30;
                minSolidity = 0.70;
                openIterations = 3;
                closeIterations = 3;
                drawContourThickness = 4;
                break;
            case "Hongo 4":
                adaptiveBlockSize = 13;
                adaptiveC = 4;
                minArea = 4.0;
                maxArea = 80.0;
                minCircularity = 0.55;
                minSolidity = 0.82;
                openIterations = 1;
                closeIterations = 2;
                drawContourThickness = 2;
                break;
            default:
                // Si el tipo de hongo no es reconocido o es "default", usa los valores predeterminados.
                break;
        }

        if (adaptiveBlockSize % 2 == 0) adaptiveBlockSize++;
        if (adaptiveBlockSize < 3) adaptiveBlockSize = 3;
        Log.d(TAG, "Parámetros finales de detección: BlockSize=" + adaptiveBlockSize + ", C=" + adaptiveC +
                ", MinArea=" + minArea + ", MaxArea=" + maxArea +
                ", MinCirc=" + minCircularity + ", MinSolidity=" + minSolidity +
                ", OpenIters=" + openIterations + ", CloseIters=" + closeIterations +
                ", ContourThickness=" + drawContourThickness);
        // --- FIN DE AJUSTES DE PARÁMETROS SEGÚN EL TIPO DE HONGO ---

        Mat binaryMask = new Mat();
        Imgproc.adaptiveThreshold(
                imgEqualized,
                binaryMask,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                adaptiveBlockSize,
                adaptiveC
        );
        Log.d(TAG, "Umbralización adaptativa aplicada.");

        Mat kernelOpen = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(binaryMask, binaryMask, Imgproc.MORPH_OPEN, kernelOpen, new Point(-1, -1), openIterations);
        Log.d(TAG, "Operación de apertura (MORPH_OPEN) aplicada.");

        Mat kernelClose = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(binaryMask, binaryMask, Imgproc.MORPH_CLOSE, kernelClose, new Point(-1, -1), closeIterations);
        Log.d(TAG, "Operación de cierre (MORPH_CLOSE) aplicada.");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binaryMask.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Log.d(TAG, "Contornos encontrados. Número inicial de contornos: " + contours.size());

        Mat imgResult = imgProcesar.clone();

        int conteoEsporas = 0;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
            double circularity = (perimeter == 0) ? 0 : (4 * Math.PI * area / (perimeter * perimeter));

            MatOfInt hull = new MatOfInt();
            Imgproc.convexHull(contour, hull);

            MatOfPoint hullContour = new MatOfPoint();
            List<Point> hullPointsList = new ArrayList<>();
            List<Point> contourPoints = contour.toList();
            for (int i = 0; i < hull.rows(); i++) {
                hullPointsList.add(contourPoints.get((int)hull.get(i, 0)[0]));
            }
            hullContour.fromList(hullPointsList);
            double hullArea = Imgproc.contourArea(hullContour);

            double solidity = (hullArea == 0) ? 0 : (area / hullArea);

            if (area >= minArea && area <= maxArea && circularity >= minCircularity && solidity >= minSolidity) {
                Imgproc.drawContours(imgResult, List.of(contour), -1, new Scalar(0, 255, 0), drawContourThickness);
                conteoEsporas++;
            }

            if (hull != null) hull.release();
            if (hullContour != null) hullContour.release();
        }
        Log.d(TAG, "Filtrado de contornos completado. Esporas detectadas: " + conteoEsporas);
        String displayText = "Número total de esporas detectadas: " + conteoEsporas;

        Bitmap imgFinal = Bitmap.createBitmap(imgResult.cols(), imgResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgResult, imgFinal);

        imageView.setImageBitmap(imgFinal);
        textView.setText(displayText);
        Log.d(TAG, "Procesamiento de imagen finalizado y resultados mostrados.");

        matOriginal.release();
        imgProcesar.release();
        imgGray.release();
        imgEqualized.release();
        binaryMask.release();
        kernelOpen.release();
        kernelClose.release();
        hierarchy.release();
        imgResult.release();

    }
}