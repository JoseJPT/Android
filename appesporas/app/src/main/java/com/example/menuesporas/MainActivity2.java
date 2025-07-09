package com.example.menuesporas;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
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
    String tipoHongo = "Hongo 1"; // Se inicializa con "Hongo 1" como valor por defecto.

    static {
        // Inicializa la librería OpenCV. Si falla, el programa puede no funcionar correctamente.
        if (!OpenCVLoader.initDebug()) {
            // Se puede añadir un log o un Toast aquí para indicar que OpenCV no se cargó.
            // Para fines de producción, se recomienda un manejo de errores más robusto.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain2);

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.texto);
        Button btnAbrirGaleria = findViewById(R.id.btnAbrirGaleria);

        // Recupera el tipo de hongo enviado desde la actividad anterior.
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("tipo_espora")) {
            // El valor predeterminado "Hongo 1" se sobrescribirá si hay un extra.
            tipoHongo = intent.getStringExtra("tipo_espora");
        }

        // Configura el OnClickListener para el botón de abrir galería.
        if (btnAbrirGaleria != null) {
            btnAbrirGaleria.setOnClickListener(v -> abrirGaleria());
        }

        // Establece el texto inicial en el TextView.
        if (textView != null) {
            textView.setText(getString(R.string.numero_de_esporas_detectadas) + ": (Selecciona una imagen)");
        }
    }

    // Inicia una actividad para seleccionar una imagen de la galería.
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    // Maneja el resultado de la actividad de selección de imagen.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                // Carga el Bitmap desde la URI y lo procesa.
                Bitmap selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                if (textView != null) {
                    textView.setText(getString(R.string.numero_de_esporas_detectadas) + " (Analizando para: " + tipoHongo + ")...");
                }
                procesarImagen(selectedBitmap, tipoHongo);
            } catch (IOException e) {
                // Maneja errores al cargar la imagen.
                e.printStackTrace();
                Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_LONG).show();
            }
        } else if (resultCode == RESULT_CANCELED) {
            // Actualiza el texto si la selección de imagen es cancelada.
            if (textView != null) {
                textView.setText(getString(R.string.numero_de_esporas_detectadas) + ": (Ninguna imagen seleccionada)");
            }
        }
    }

    /**
     * Procesa la imagen de entrada para detectar y contar esporas.
     * Los parámetros del algoritmo se ajustan dinámicamente según el 'hongoType'
     * para una detección robusta de pequeños círculos y eliminación de ruido/elementos no deseados.
     *
     * @param bitmapOriginal Bitmap de la imagen seleccionada por el usuario.
     * @param hongoType      Cadena que define el tipo de hongo, usada para seleccionar el conjunto de parámetros.
     */
    private void procesarImagen(Bitmap bitmapOriginal, String hongoType) {
        // Convierte el Bitmap de entrada a un objeto Mat de OpenCV.
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmapOriginal, mat);

        // Crea un clon de la imagen original para dibujar los contornos detectados.
        Mat matWithContours = mat.clone();

        // Declaración y creación de Mats para las diferentes etapas de procesamiento.
        // Se crean aquí para asegurar que siempre haya objetos válidos para operar.
        Mat gris = new Mat();
        Mat grisDenoised = new Mat();
        Mat procesadaParaUmbral = new Mat();
        Mat mascaraBinaria = new Mat();
        Mat hierarchy = new Mat(); // Se declara aquí para asegurar su liberación al final.

        // Variables para los parámetros del algoritmo, inicializadas con valores de "Hongo 1".
        // Esto elimina las advertencias de "variable might not have been initialized".
        double adaptiveThreshMaxValue = 255;
        int adaptiveThreshBlockSize = 21;
        int adaptiveThreshC = 1;

        Size kernelOpenSize = new Size(3, 3);
        int kernelOpenIterations = 2;
        Size kernelCloseSize = new Size(5, 5);
        int kernelCloseIterations = 1;

        double minArea = 3;
        double maxArea = 150;
        double minCircularity = 0.40;
        double minSolidity = 0.60;
        double minAspectRatio = 0.5;
        double maxAspectRatio = 2.0;

        // --- Ajuste de parámetros según el tipo de hongo seleccionado ---
        // Los parámetros predeterminados ya están establecidos al inicio del método.
        // Solo se modifican si el 'hongoType' coincide con un caso específico.
        switch (hongoType) {
            case "Hongo 1":
                // Los valores predeterminados ya corresponden a "Hongo 1", así que no se necesita acción aquí.
                break;
            case "Hongo 2": // Perfil ligeramente más sensible/tolerante.
                adaptiveThreshBlockSize = 19;
                adaptiveThreshC = 0;
                kernelOpenSize = new Size(3, 3);
                kernelOpenIterations = 1;
                kernelCloseSize = new Size(3, 3);
                kernelCloseIterations = 1;
                minArea = 2;
                maxArea = 180;
                minCircularity = 0.35;
                minSolidity = 0.55;
                minAspectRatio = 0.4;
                maxAspectRatio = 2.5;
                break;
            case "Hongo 3": // Perfil ligeramente más estricto/menos propenso a ruido.
                adaptiveThreshBlockSize = 25;
                adaptiveThreshC = 2;
                kernelOpenSize = new Size(3, 3);
                kernelOpenIterations = 2;
                kernelCloseSize = new Size(5, 5);
                kernelCloseIterations = 1;
                minArea = 5;
                maxArea = 120;
                minCircularity = 0.45;
                minSolidity = 0.65;
                minAspectRatio = 0.6;
                maxAspectRatio = 1.8;
                break;
            case "Hongo 4": // Perfil con morfología más agresiva para mayor limpieza.
                adaptiveThreshBlockSize = 21;
                adaptiveThreshC = 1;
                kernelOpenSize = new Size(5, 5);
                kernelOpenIterations = 2;
                kernelCloseSize = new Size(7, 7);
                kernelCloseIterations = 1;
                minArea = 10;
                maxArea = 200;
                minCircularity = 0.40;
                minSolidity = 0.60;
                minAspectRatio = 0.5;
                maxAspectRatio = 2.0;
                break;
            // No se necesita un caso "default" ya que 'tipoHongo' siempre se inicializa
            // y se espera que provenga de una selección de botón que mapea a estos casos.
        }

        // --- Etapas de procesamiento de imagen ---

        // 1. Convierte la imagen original a escala de grises.
        Imgproc.cvtColor(mat, gris, Imgproc.COLOR_BGR2GRAY);

        // 2. Aplica un filtro bilateral para reducir el ruido mientras se preservan los bordes.
        // d: Diámetro del vecindario para el filtro.
        // sigmaColor: Sigma en el espacio de color.
        // sigmaSpace: Sigma en el espacio de coordenadas.
        Imgproc.bilateralFilter(gris, grisDenoised, 5, 75, 75);

        // 3. Aplica un desenfoque mediano para suavizar el ruido residual y preparar la imagen para el umbral.
        Imgproc.medianBlur(grisDenoised, procesadaParaUmbral, 3);

        // 4. Aplica umbralización adaptativa para convertir la imagen a una máscara binaria.
        // Se usa ADAPTIVE_THRESH_GAUSSIAN_C para calcular un umbral local.
        // THRESH_BINARY_INV invierte los colores (objetos oscuros se vuelven blancos sobre fondo negro).
        Imgproc.adaptiveThreshold(
                procesadaParaUmbral,
                mascaraBinaria,
                adaptiveThreshMaxValue,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                adaptiveThreshBlockSize,
                adaptiveThreshC
        );

        // 5. Realiza operaciones morfológicas para limpiar la máscara binaria.
        // MORPH_OPEN (apertura): Elimina pequeños objetos blancos (ruido) y separa objetos conectados débilmente.
        Mat kernelOpenElement = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, kernelOpenSize);
        Imgproc.morphologyEx(mascaraBinaria, mascaraBinaria, Imgproc.MORPH_OPEN, kernelOpenElement, new Point(-1, -1), kernelOpenIterations);
        kernelOpenElement.release(); // Libera el kernel utilizado.

        // MORPH_CLOSE (cierre): Rellena pequeños agujeros dentro de los objetos y une objetos cercanos.
        Mat kernelCloseElement = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, kernelCloseSize);
        Imgproc.morphologyEx(mascaraBinaria, mascaraBinaria, Imgproc.MORPH_CLOSE, kernelCloseElement, new Point(-1, -1), kernelCloseIterations);
        kernelCloseElement.release(); // Libera el kernel utilizado.

        // 6. Encuentra los contornos (bordes de los objetos) en la máscara binaria.
        // RETR_EXTERNAL: Recupera solo los contornos externos.
        // CHAIN_APPROX_SIMPLE: Comprime segmentos horizontales, verticales y diagonales y deja solo sus puntos finales.
        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(mascaraBinaria.clone(), contornos, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int conteoEsporas = 0; // Inicializa el contador de esporas.

        // 7. Itera a través de cada contorno encontrado y aplica los filtros definidos.
        for (MatOfPoint contorno : contornos) {
            double area = Imgproc.contourArea(contorno); // Calcula el área del contorno.
            double perimeter = Imgproc.arcLength(new MatOfPoint2f(contorno.toArray()), true); // Calcula el perímetro.

            // Calcula la circularidad del contorno. Evita división por cero.
            double circularity = (perimeter == 0) ? 0 : (4 * Math.PI * area / (perimeter * perimeter));

            // Calcula la solidez del contorno.
            MatOfInt hull = new MatOfInt(); // Declara MatOfInt para el casco convexo.
            MatOfPoint hullContour = new MatOfPoint(); // Declara MatOfPoint para el contorno del casco.
            try {
                Imgproc.convexHull(contorno, hull);
                List<Point> hullPointsList = new ArrayList<>();
                List<Point> contourPoints = contorno.toList();
                for (int i = 0; i < hull.rows(); i++) {
                    // Asegura que el índice sea válido antes de acceder.
                    int index = (int)hull.get(i, 0)[0];
                    if (index >= 0 && index < contourPoints.size()) {
                        hullPointsList.add(contourPoints.get(index));
                    }
                }
                hullContour.fromList(hullPointsList);
                // Evita división por cero si el área del casco convexo es cero.
                double hullArea = Imgproc.contourArea(hullContour);
                double solidity = (hullArea == 0) ? 0 : (area / hullArea);

                // Calcula la relación de aspecto de la caja delimitadora del contorno.
                Rect boundingBox = Imgproc.boundingRect(contorno);
                double aspectRatio = (double) boundingBox.width / boundingBox.height;

                // Aplica todos los criterios de filtrado. Si se cumplen, se cuenta como espora.
                if (area >= minArea && area <= maxArea &&
                        circularity >= minCircularity && solidity >= minSolidity &&
                        aspectRatio >= minAspectRatio && aspectRatio <= maxAspectRatio) {
                    conteoEsporas++;
                    // Dibuja el contorno validado en la imagen clonada (matWithContours) en color verde.
                    Imgproc.drawContours(matWithContours, List.of(contorno), -1, new Scalar(0, 255, 0), 2);
                }
            } finally {
                // Libera los objetos MatOfInt y MatOfPoint temporales dentro del bucle.
                // Esto es crucial para evitar fugas de memoria en cada iteración.
                if (hull != null) {
                    hull.release();
                }
                if (hullContour != null) {
                    hullContour.release();
                }
            }
        }

        // Actualiza el TextView en la interfaz de usuario con el número de esporas detectadas.
        if (textView != null) {
            textView.setText(getString(R.string.numero_de_esporas_detectadas) + ": " + conteoEsporas + " (" + hongoType + ")");
        }

        // Convierte la Mat resultante (con los contornos dibujados) de nuevo a un Bitmap
        // para mostrarla en el ImageView.
        Bitmap processedBitmap = Bitmap.createBitmap(matWithContours.cols(), matWithContours.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(matWithContours, processedBitmap);
        if (imageView != null) {
            imageView.setImageBitmap(processedBitmap);
        }

        // Libera todas las Mats para evitar fugas de memoria. Esto es crucial para el rendimiento.
        mat.release();
        matWithContours.release();
        gris.release();
        grisDenoised.release();
        procesadaParaUmbral.release();
        mascaraBinaria.release();
        hierarchy.release(); // Libera la Mat hierarchy también.
    }
}