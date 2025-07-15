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

/**
 * *MainActivity2*: La actividad principal que gestiona la interfaz de usuario y el flujo de procesamiento
 * de imágenes para la detección de esporas. Esta clase integra las funcionalidades de Android para la
 * selección de imágenes con las capacidades de procesamiento de visión por computadora de OpenCV.
 * Su objetivo es proporcionar una herramienta robusta para identificar y cuantificar esporas en imágenes.
 */
public class MainActivity2 extends AppCompatActivity {

    // --- Componentes de la Interfaz de Usuario (UI) ---
    private ImageView imageView; // Componente visual para mostrar la imagen (original o procesada).
    private TextView textView;   // Componente de texto para visualizar el conteo de esporas detectadas.
    private Bitmap imageBitmap;  // Almacena la imagen original seleccionada por el usuario en formato Bitmap.
    private String tipoHongo = ""; // String para almacenar el identificador del tipo de hongo,
    // que se utiliza para cargar un conjunto de parámetros de detección optimizados para ese hongo específico.

    // Etiqueta para los mensajes de depuración (logs) en Logcat, facilitando el seguimiento del flujo de la aplicación.
    private static final String TAG = "MainActivity2";

    // --- Inicialización de la Biblioteca OpenCV ---
    // Bloque estático que se ejecuta una vez al cargar la clase en memoria.
    // Es fundamental para inicializar la biblioteca OpenCV de forma nativa antes de que
    // cualquier método de OpenCV sea invocado. Si la inicialización falla, se registra un error crítico.
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Error crítico: No se pudo cargar la biblioteca OpenCV.");
        } else {
            Log.d(TAG, "OpenCV cargado exitosamente. Listo para el procesamiento de imágenes.");
        }
    }

    /**
     * Método del ciclo de vida de la actividad: onCreate.
     * Se invoca cuando la actividad es creada por primera vez. Este método es el punto principal
     * para la inicialización de la UI, la configuración de listeners y la recuperación de datos
     * persistentes o pasados a través de un Intent.
     *
     * @param savedInstanceState Si la actividad se está recreando después de ser destruida
     * (ej. por un cambio de orientación), este Bundle contiene los datos
     * de estado más recientes guardados por onSaveInstanceState().
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain2);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.texto);
        Button btnAbrirGaleria = findViewById(R.id.btnAbrirGaleria);

        Intent intent = getIntent();
        tipoHongo = intent.getStringExtra("tipo_espora"); // Ahora esto recibirá el string completo de densidad

        if (tipoHongo != null && !tipoHongo.isEmpty()) {
            Log.d(TAG, "Parámetro 'tipo_espora' recibido: " + tipoHongo + ". Se ajustarán los parámetros de detección.");
        } else {
            tipoHongo = "default";
            Log.d(TAG, "Parámetro 'tipo_espora' no especificado. Usando parámetros de detección por defecto.");
        }

        btnAbrirGaleria.setOnClickListener(v -> {
            Log.d(TAG, "Botón 'Abrir Galería' pulsado. Iniciando selección de imagen.");
            abrirGaleria();
        });
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "Imagen seleccionada de la galería. Procesando imagen...");
            Uri imageUri = data.getData();
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                procesarImagen(imageBitmap);
            } catch (IOException e) {
                Log.e(TAG, "Error al cargar la imagen desde la URI: " + e.getMessage(), e);
            }
        } else {
            Log.d(TAG, "Selección de imagen cancelada o un error ocurrió durante la selección.");
        }
    }

    private void procesarImagen(Bitmap bitmapOriginal) {
        Log.d(TAG, "Iniciando pipeline de procesamiento de imagen con OpenCV...");

        Mat matOriginal = new Mat();
        Utils.bitmapToMat(bitmapOriginal, matOriginal);
        Log.d(TAG, "Imagen original convertida a formato Mat.");

        Mat imgProcesar = new Mat();
        Imgproc.resize(matOriginal, imgProcesar, new Size(640, 480));
        Log.d(TAG, "Imagen redimensionada a 640x480 píxeles para procesamiento uniforme y optimización.");

        Mat imgGray = new Mat();
        Imgproc.cvtColor(imgProcesar, imgGray, Imgproc.COLOR_BGR2GRAY);
        Log.d(TAG, "Imagen convertida a escala de grises para simplificar el procesamiento.");

        Mat imgEqualized = new Mat();
        Imgproc.equalizeHist(imgGray, imgEqualized);
        Log.d(TAG, "Ecualización de histograma aplicada para realzar el contraste de las esporas.");

        Imgproc.GaussianBlur(imgEqualized, imgEqualized, new Size(1, 1), 0);
        Log.d(TAG, "Desenfoque Gaussiano aplicado con kernel (1,1) para máxima preservación de detalles.");

        // --- Configuración de Parámetros de Detección Optimizada para un Recall más EQUILIBRADO ---
        int adaptiveBlockSize = 15;
        double adaptiveC = 5;

        double minArea = 5.0;
        double maxArea = 100.0;
        double minCircularity = 0.65;
        double minSolidity = 0.80;
        int openIterations = 1;
        int closeIterations = 0;
        int drawContourThickness = 1;

        double minAspectRatio = 0.5;
        double maxAspectRatio = 2.0;

        // Lógica para ajustar los parámetros específicos para cada tipoHongo.
        // Ahora los 'case' usan los nombres completos de las densidades desde strings.xml
        switch (tipoHongo) {
            case "Baja Densidad (0-170 esporas)": // Corresponde a "Hongo 1" anterior
                adaptiveBlockSize = 17;
                adaptiveC = 6;
                minArea = 8.0;
                maxArea = 90.0;
                minCircularity = 0.68;
                minSolidity = 0.82;
                openIterations = 1;
                closeIterations = 0;
                drawContourThickness = 4;
                minAspectRatio = 0.6;
                maxAspectRatio = 1.9;
                break;
            case "Densidad Moderada (170-500 esporas)": // Corresponde a "Hongo 2" anterior
                adaptiveBlockSize = 13;
                adaptiveC = 4;
                minArea = 7.0;
                maxArea = 80.0;
                minCircularity = 0.60;
                minSolidity = 0.75;
                openIterations = 1;
                closeIterations = 0;
                drawContourThickness = 4;
                minAspectRatio = 0.55;
                maxAspectRatio = 1.8;
                break;
            case "Alta Densidad (500-1000 esporas)": // Corresponde a "Hongo 3" anterior
                adaptiveBlockSize = 15;
                adaptiveC = 5;
                minArea = 6.0;
                maxArea = 85.0;
                minCircularity = 0.62;
                minSolidity = 0.78;
                openIterations = 1;
                closeIterations = 0;
                drawContourThickness = 4;
                minAspectRatio = 0.45;
                maxAspectRatio = 2.6;
                break;
            case "Densidad Muy Alta (1000+ esporas)": // Corresponde a "Hongo 4" anterior
                adaptiveBlockSize = 15;
                adaptiveC = 5;
                minArea = 2.5;
                maxArea = 85.0;
                minCircularity = 0.38;
                minSolidity = 0.58;
                openIterations = 0;
                closeIterations = 0;
                drawContourThickness = 4;
                minAspectRatio = 0.45;
                maxAspectRatio = 2.6;
                break;
            default:
                // Se mantienen los valores por defecto optimizados para un recall más equilibrado.
                break;
        }

        // Validación: adaptiveBlockSize debe ser impar y al menos 3. Se ajusta si no cumple.
        if (adaptiveBlockSize % 2 == 0) adaptiveBlockSize++;
        if (adaptiveBlockSize < 3) adaptiveBlockSize = 3;

        Log.d(TAG, "Parámetros finales de detección aplicados (optimizados para recall equilibrado):" +
                " BlockSize=" + adaptiveBlockSize + ", C=" + adaptiveC +
                ", MinArea=" + minArea + ", MaxArea=" + maxArea +
                ", MinCirc=" + minCircularity + ", MinSolidity=" + minSolidity +
                ", OpenIters=" + openIterations + ", CloseIters=" + closeIterations +
                ", ContourThickness=" + drawContourThickness +
                ", MinAspectRatio=" + minAspectRatio + ", MaxAspectRatio=" + maxAspectRatio);
        // --- FIN DE AJUSTES DE PARÁMETROS ---

        // 6. Umbralización Adaptativa:
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
        Log.d(TAG, "Umbralización adaptativa completada.");

        // 7. Operaciones Morfológicas: Apertura y Cierre.
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));

        Imgproc.morphologyEx(binaryMask, binaryMask, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), openIterations);
        Log.d(TAG, "Operación de apertura (MORPH_OPEN) aplicada con " + openIterations + " iteración(es).");

        Imgproc.morphologyEx(binaryMask, binaryMask, Imgproc.MORPH_CLOSE, kernel, new Point(-1, -1), closeIterations);
        Log.d(TAG, "Operación de cierre (MORPH_CLOSE) aplicada con " + closeIterations + " iteración(es).");

        // 8. Detección de Contornos:
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(
                binaryMask.clone(),
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_NONE
        );
        Log.d(TAG, "Detección de contornos completada. Número inicial de contornos: " + contours.size());

        Mat imgResult = imgProcesar.clone();
        int conteoEsporas = 0;

        // 9. Filtrado de Contornos con Umbrales más Estrictos:
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            if (area < minArea || area > maxArea) {
                if (contour != null) contour.release();
                continue;
            }

            double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
            double circularity = (perimeter == 0) ? 0 : (4 * Math.PI * area / (perimeter * perimeter));

            if (circularity < minCircularity) {
                Log.d(TAG, "Contorno descartado por Circularidad: " + String.format("%.2f", circularity) +
                        ". Área: " + String.format("%.2f", area));
                if (contour != null) contour.release();
                continue;
            }

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

            if (solidity < minSolidity) {
                Log.d(TAG, "Contorno descartado por Solidez: " + String.format("%.2f", solidity) +
                        ". Área: " + String.format("%.2f", area) + ". Circularidad: " + String.format("%.2f", circularity));
                if (hull != null) hull.release();
                if (hullContour != null) hullContour.release();
                if (contour != null) contour.release();
                continue;
            }

            Rect boundingRect = Imgproc.boundingRect(contour);
            double aspectRatio = (double) boundingRect.width / boundingRect.height;

            if (aspectRatio < minAspectRatio || aspectRatio > maxAspectRatio) {
                Log.d(TAG, "Contorno descartado por Relación de Aspecto: " + String.format("%.2f", aspectRatio) +
                        ". Área: " + String.format("%.2f", area) + ". Circularidad: " + String.format("%.2f", circularity) +
                        ". Solidez: " + String.format("%.2f", solidity));
                if (hull != null) hull.release();
                if (hullContour != null) hullContour.release();
                if (contour != null) contour.release();
                continue;
            }
            // --- Fin del Filtrado de Contornos ---

            Imgproc.drawContours(imgResult, List.of(contour), -1, new Scalar(0, 255, 0), drawContourThickness);
            conteoEsporas++;

            if (hull != null) hull.release();
            if (hullContour != null) hullContour.release();
            if (contour != null) contour.release();
        }
        Log.d(TAG, "Filtrado de contornos completado. Esporas válidas detectadas: " + conteoEsporas);

        String displayText = "Número total de esporas detectadas: " + conteoEsporas;

        Bitmap imgFinal = Bitmap.createBitmap(imgResult.cols(), imgResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgResult, imgFinal);

        imageView.setImageBitmap(imgFinal);
        textView.setText(displayText);
        Log.d(TAG, "Procesamiento de imagen finalizado y resultados actualizados en la UI.");

        matOriginal.release();
        imgProcesar.release();
        imgGray.release();
        imgEqualized.release();
        binaryMask.release();
        kernel.release();
        hierarchy.release();
        imgResult.release();
        Log.d(TAG, "Todos los recursos de OpenCV (Mat objects) han sido liberados para prevenir fugas de memoria.");
    }
}