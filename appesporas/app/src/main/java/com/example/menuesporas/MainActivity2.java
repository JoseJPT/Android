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
 * **MainActivity2**: La actividad principal que gestiona la interfaz de usuario y el flujo de procesamiento
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
     * Método del ciclo de vida de la actividad: `onCreate`.
     * Se invoca cuando la actividad es creada por primera vez. Este método es el punto principal
     * para la inicialización de la UI, la configuración de listeners y la recuperación de datos
     * persistentes o pasados a través de un `Intent`.
     *
     * @param savedInstanceState Si la actividad se está recreando después de ser destruida
     * (ej. por un cambio de orientación), este Bundle contiene los datos
     * de estado más recientes guardados por `onSaveInstanceState()`.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Asocia el archivo de diseño XML 'activitymain2.xml' con esta actividad,
        // definiendo la estructura visual de la interfaz de usuario.
        setContentView(R.layout.activitymain2);

        // Enlaza las variables Java con los elementos de la UI definidos en el layout XML
        // utilizando sus IDs respectivos.
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.texto);
        Button btnAbrirGaleria = findViewById(R.id.btnAbrirGaleria);

        // Recupera el `tipoHongo` que se pasó como extra en el `Intent` desde la actividad anterior.
        // Esta información es crucial para seleccionar el conjunto de parámetros de detección adecuado.
        Intent intent = getIntent();
        tipoHongo = intent.getStringExtra("tipo_espora");

        // Verifica si se recibió un tipo de hongo válido. Si no, se asigna "default"
        // para utilizar un conjunto de parámetros genéricos o base.
        if (tipoHongo != null && !tipoHongo.isEmpty()) {
            Log.d(TAG, "Parámetro 'tipo_espora' recibido: " + tipoHongo + ". Se ajustarán los parámetros de detección.");
        } else {
            tipoHongo = "default";
            Log.d(TAG, "Parámetro 'tipo_espora' no especificado. Usando parámetros de detección por defecto.");
        }

        // Configura un `OnClickListener` para el botón de "Abrir Galería".
        // Al hacer clic, se invocará el método `abrirGaleria()` para iniciar el proceso de selección de imagen.
        btnAbrirGaleria.setOnClickListener(v -> {
            Log.d(TAG, "Botón 'Abrir Galería' pulsado. Iniciando selección de imagen.");
            abrirGaleria();
        });
    }

    /**
     * Inicia un `Intent` implícito para acceder a la galería de imágenes del dispositivo.
     * Permite al usuario seleccionar una imagen de su almacenamiento externo.
     * El resultado de esta selección es manejado por el método `onActivityResult()`.
     */
    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // `startActivityForResult` es crucial para lanzar una actividad externa y esperar un resultado.
        // El `requestCode` (100 en este caso) se utiliza para identificar el origen del resultado
        // cuando `onActivityResult` es llamado.
        startActivityForResult(intent, 100);
    }

    /**
     * Método del ciclo de vida de la actividad: `onActivityResult`.
     * Este método se invoca automáticamente cuando una actividad lanzada con `startActivityForResult()`
     * finaliza y devuelve un resultado.
     *
     * @param requestCode El código entero que se utilizó en `startActivityForResult()` para identificar la solicitud.
     * @param resultCode  El código de resultado devuelto por la actividad secundaria (ej. `RESULT_OK` para éxito, `RESULT_CANCELED` para cancelación).
     * @param data        Un `Intent` que contiene los datos del resultado. Para la selección de imágenes,
     * contendrá la `Uri` de la imagen seleccionada.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Se verifica que el resultado provenga de la solicitud de la galería (`requestCode == 100`),
        // que la operación haya sido exitosa (`resultCode == RESULT_OK`), y que haya datos disponibles.
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "Imagen seleccionada de la galería. Procesando imagen...");
            Uri imageUri = data.getData(); // Obtiene la URI (Uniform Resource Identifier) de la imagen seleccionada.
            try {
                // Convierte la `Uri` de la imagen en un objeto `Bitmap`, que es el formato con el que Android trabaja.
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                procesarImagen(imageBitmap); // Invoca la función principal de procesamiento de imágenes con OpenCV.
            } catch (IOException e) {
                // Captura y registra cualquier excepción que ocurra durante la carga del Bitmap,
                // lo que podría indicar un problema con la URI o el archivo de imagen.
                Log.e(TAG, "Error al cargar la imagen desde la URI: " + e.getMessage(), e);
            }
        } else {
            Log.d(TAG, "Selección de imagen cancelada o un error ocurrió durante la selección.");
        }
    }

    /**
     * Función central para el procesamiento de imágenes utilizando OpenCV.
     * Implementa un pipeline de visión por computadora para detectar esporas, que incluye:
     * preprocesamiento, umbralización adaptativa, operaciones morfológicas, detección de contornos
     * y filtrado de contornos basado en propiedades geométricas.
     *
     * @param bitmapOriginal El objeto `Bitmap` de la imagen original a procesar.
     */
    private void procesarImagen(Bitmap bitmapOriginal) {
        Log.d(TAG, "Iniciando pipeline de procesamiento de imagen con OpenCV...");

        // 1. Conversión de Bitmap a Mat:
        // OpenCV utiliza su propio tipo de datos `Mat` (matriz) para representar imágenes.
        // Esta conversión es el primer paso para aplicar cualquier función de OpenCV.
        Mat matOriginal = new Mat();
        Utils.bitmapToMat(bitmapOriginal, matOriginal);
        Log.d(TAG, "Imagen original convertida a formato Mat.");

        // 2. Redimensionamiento de la Imagen:
        // Se redimensiona la imagen a un tamaño estándar (640x480). Esto es crucial por varias razones:
        // a) Consistencia: Asegura que todas las imágenes se procesen con la misma escala, simplificando la calibración de parámetros.
        // b) Rendimiento: Reduce la cantidad de píxeles, lo que acelera significativamente las operaciones de OpenCV.
        // c) Memoria: Reduce el consumo de memoria, vital en dispositivos móviles.
        Mat imgProcesar = new Mat();
        Imgproc.resize(matOriginal, imgProcesar, new Size(640, 480));
        Log.d(TAG, "Imagen redimensionada a 640x480 píxeles para procesamiento uniforme y optimización.");

        // 3. Conversión a Escala de Grises:
        // Muchas operaciones de procesamiento de imágenes, incluyendo la umbralización y la detección de contornos,
        // son más eficientes y a menudo requieren imágenes de un solo canal (escala de grises).
        Mat imgGray = new Mat();
        Imgproc.cvtColor(imgProcesar, imgGray, Imgproc.COLOR_BGR2GRAY);
        Log.d(TAG, "Imagen convertida a escala de grises para simplificar el procesamiento.");

        // 4. Ecualización de Histograma:
        // Mejora el contraste de la imagen al distribuir uniformemente la intensidad de los píxeles.
        // Esto hace que las esporas, que pueden tener un contraste bajo, sean más fáciles de detectar.
        Mat imgEqualized = new Mat();
        Imgproc.equalizeHist(imgGray, imgEqualized);
        Log.d(TAG, "Ecualización de histograma aplicada para realzar el contraste de las esporas.");

        // 5. Desenfoque Gaussiano:
        // Aplica un filtro de suavizado para reducir el ruido aleatorio. Con un kernel de (1,1),
        // el desenfoque es mínimo, preservando al máximo los detalles de las esporas más pequeñas
        // y de bajo contraste, lo cual es fundamental para el alto recall.
        Imgproc.GaussianBlur(imgEqualized, imgEqualized, new Size(1, 1), 0);
        Log.d(TAG, "Desenfoque Gaussiano aplicado con kernel (1,1) para máxima preservación de detalles.");

        // --- Configuración de Parámetros de Detección Optimizada para Recall EXTREMO ---
        // Estos parámetros han sido ajustados para ser extremadamente permisivos, con el objetivo
        // de capturar la mayor cantidad ABSOLUTA posible de esporas, priorizando el recall por encima
        // de la precisión. Es esperable una mayor cantidad de falsos positivos que requerirán
        // un post-procesamiento o validación.
        int adaptiveBlockSize = 15; // Tamaño del bloque para la umbralización adaptativa.
        // Un valor más bajo (`15`) hace el umbral más local y sensible
        // a variaciones finas, crucial para esporas pequeñas y débiles. **Debe ser impar.**
        double adaptiveC = 5;       // Constante restada de la media. Un valor bajo (`5`) baja el umbral,
        // incluyendo más píxeles como parte de objetos y aumentando la detección
        // de esporas de bajo contraste.

        double minArea = 1.0;       // Área mínima (en píxeles cuadrados). Establecido en `1.0` para detectar
        // hasta los puntos más pequeños que puedan ser esporas.
        double maxArea = 100.0;     // Área máxima. Aumentado a `100.0` para ser muy tolerante con esporas
        // grandes o agrupaciones, asegurando que nada se descarte por tamaño.
        double minCircularity = 0.40; // Circularidad mínima. Relajado al extremo (`0.40`) para aceptar
        // esporas muy deformadas, elípticas, o con contornos irregulares por ruido.
        double minSolidity = 0.60;    // Solidez mínima. Relajado a `0.60` para incluir objetos con concavidades
        // o formas menos compactas, maximizando la detección.
        int openIterations = 0;     // Iteraciones de Apertura. Establecido a `0` para no eliminar ruido
        // ni erosionar esporas pequeñas. (0 significa ninguna operación)
        int closeIterations = 0;    // Iteraciones de Cierre. Establecido a `0` para no fusionar esporas
        // cercanas ni alterar sus formas. (0 significa ninguna operación)
        int drawContourThickness = 1; // Grosor del contorno a dibujar.

        // Parámetros de relación de aspecto. Rango muy amplio para capturar cualquier forma imaginable.
        double minAspectRatio = 0.3; // Rango extremadamente amplio para incluir esporas muy alargadas
        double maxAspectRatio = 3.0; // en cualquier dirección, o formas altamente irregulares.

        // Lógica para ajustar los parámetros específicos para cada `tipoHongo`.
        // Estos ajustes también se han relajado al máximo para favorecer el recall.
        switch (tipoHongo) {
            case "Hongo 1":
                adaptiveBlockSize = 13;
                adaptiveC = 4;
                minArea = 2.0;
                maxArea = 80.0;
                minCircularity = 0.35;
                minSolidity = 0.55;
                openIterations = 0;
                closeIterations = 0;
                drawContourThickness = 2;
                minAspectRatio = 0.4;
                maxAspectRatio = 2.5;
                break;
            case "Hongo 2":
                adaptiveBlockSize = 17;
                adaptiveC = 6;
                minArea = 3.0;
                maxArea = 90.0;
                minCircularity = 0.30;
                minSolidity = 0.50;
                openIterations = 0;
                closeIterations = 0;
                drawContourThickness = 1;
                minAspectRatio = 0.35;
                maxAspectRatio = 2.8;
                break;
            case "Hongo 3":
                adaptiveBlockSize = 15;
                adaptiveC = 5;
                minArea = 2.5;
                maxArea = 85.0;
                minCircularity = 0.38;
                minSolidity = 0.58;
                openIterations = 0;
                closeIterations = 0;
                drawContourThickness = 2;
                minAspectRatio = 0.45;
                maxAspectRatio = 2.6;
                break;
            case "Hongo 4":
                adaptiveBlockSize = 15;
                adaptiveC = 7;
                minArea = 1.5;
                maxArea = 95.0;
                minCircularity = 0.32;
                minSolidity = 0.52;
                openIterations = 0;
                closeIterations = 0;
                drawContourThickness = 1;
                minAspectRatio = 0.3;
                maxAspectRatio = 2.9;
                break;
            default:
                // Se mantienen los valores por defecto optimizados para un alto recall extremo.
                break;
        }

        // Validación: `adaptiveBlockSize` debe ser impar y al menos 3. Se ajusta si no cumple.
        // Esto es importante ya que los valores bajos pueden caer en rangos no válidos para OpenCV.
        if (adaptiveBlockSize % 2 == 0) adaptiveBlockSize++;
        if (adaptiveBlockSize < 3) adaptiveBlockSize = 3;

        Log.d(TAG, "Parámetros finales de detección aplicados (optimizados para recall EXTREMO):" +
                " BlockSize=" + adaptiveBlockSize + ", C=" + adaptiveC +
                ", MinArea=" + minArea + ", MaxArea=" + maxArea +
                ", MinCirc=" + minCircularity + ", MinSolidity=" + minSolidity +
                ", OpenIters=" + openIterations + ", CloseIters=" + closeIterations +
                ", ContourThickness=" + drawContourThickness +
                ", MinAspectRatio=" + minAspectRatio + ", MaxAspectRatio=" + maxAspectRatio);
        // --- FIN DE AJUSTES DE PARÁMETROS ---

        // 6. Umbralización Adaptativa:
        // Convierte la imagen en escala de grises en una imagen binaria (blanco y negro).
        // `ADAPTIVE_THRESH_GAUSSIAN_C` calcula un umbral diferente para cada región de la imagen.
        // Con los parámetros `adaptiveBlockSize` y `adaptiveC` muy bajos, la umbralización
        // será muy sensible y tenderá a clasificar más píxeles como "objeto", lo que es clave
        // para un recall extremo, aunque puede generar más ruido.
        Mat binaryMask = new Mat();
        Imgproc.adaptiveThreshold(
                imgEqualized,               // Imagen de entrada (escala de grises y ecualizada).
                binaryMask,                 // Imagen binaria de salida.
                255,                        // Valor máximo asignado a los píxeles que superan el umbral.
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, // Método de umbralización adaptativa.
                Imgproc.THRESH_BINARY_INV,          // Tipo de umbralización: binario invertido.
                adaptiveBlockSize,          // Tamaño de la vecindad para calcular el umbral.
                adaptiveC                   // Constante a restar, ajusta la sensibilidad del umbral.
        );
        Log.d(TAG, "Umbralización adaptativa completada con alta sensibilidad.");

        // 7. Operaciones Morfológicas: Apertura y Cierre.
        // Estas operaciones se han minimizado o eliminado para evitar la pérdida de esporas
        // pequeñas o delicadas. Su propósito aquí es muy limitado para no afectar el recall.
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));

        // a. Operación de Apertura (MORPH_OPEN):
        // Con `openIterations = 0`, no se realiza ninguna operación de apertura, preservando
        // todos los pequeños detalles y ruido.
        Imgproc.morphologyEx(binaryMask, binaryMask, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), openIterations);
        Log.d(TAG, "Operación de apertura (MORPH_OPEN) aplicada con " + openIterations + " iteración(es).");

        // b. Operación de Cierre (MORPH_CLOSE):
        // Con `closeIterations = 0`, no se realiza ninguna operación de cierre, evitando
        // la fusión accidental de esporas cercanas.
        Imgproc.morphologyEx(binaryMask, binaryMask, Imgproc.MORPH_CLOSE, kernel, new Point(-1, -1), closeIterations);
        Log.d(TAG, "Operación de cierre (MORPH_CLOSE) aplicada con " + closeIterations + " iteración(es).");

        // 8. Detección de Contornos:
        // Detecta los contornos de todos los objetos "blancos" en la máscara binaria resultante,
        // sin filtrar inicialmente, ya que los filtros se aplican después.
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(
                binaryMask.clone(),
                contours,
                hierarchy,
                Imgproc.RETR_LIST,     // Usamos RETR_LIST para recuperar todos los contornos, sin jerarquía.
                // Esto es menos restrictivo y asegura que se obtengan todos.
                Imgproc.CHAIN_APPROX_NONE // Usamos CHAIN_APPROX_NONE para obtener TODOS los puntos de contorno,
                // sin aproximación, lo que es útil si la forma de la espora es muy irregular.
        );
        Log.d(TAG, "Detección de contornos completada. Número inicial de contornos: " + contours.size());

        Mat imgResult = imgProcesar.clone();
        int conteoEsporas = 0;

        // 9. Filtrado de Contornos EXTREMADAMENTE Relajado:
        // Los filtros se aplican con umbrales mínimos para descartar solo lo más obviamente
        // irrelevante, priorizando la inclusión de cualquier posible espora.
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            // Filtro de Área: Rango muy amplio para capturar cualquier cosa que se parezca remotamente a una espora.
            if (area < minArea || area > maxArea) {
                if (contour != null) contour.release();
                continue;
            }

            // Filtro de Circularidad:
            double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
            double circularity = (perimeter == 0) ? 0 : (4 * Math.PI * area / (perimeter * perimeter));

            // Umbral de circularidad muy bajo para aceptar formas irregulares o elípticas.
            if (circularity < minCircularity) {
                if (contour != null) contour.release();
                continue;
            }

            // Filtro de Solidez (Solidity):
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

            // Umbral de solidez muy bajo para permitir contornos con concavidades o irregularidades.
            if (solidity < minSolidity) {
                if (hull != null) hull.release();
                if (hullContour != null) hullContour.release();
                if (contour != null) contour.release();
                continue;
            }

            // Filtro de Relación de Aspecto (Aspect Ratio):
            Rect boundingRect = Imgproc.boundingRect(contour);
            double aspectRatio = (double) boundingRect.width / boundingRect.height;

            // Rango extremadamente amplio para no descartar nada por su forma alargada.
            if (aspectRatio < minAspectRatio || aspectRatio > maxAspectRatio) {
                Log.d(TAG, "Contorno descartado por Relación de Aspecto: " + String.format("%.2f", aspectRatio) +
                        ". Área: " + String.format("%.2f", area) + " píxeles cuadrados. Circularidad: " + String.format("%.2f", circularity));
                if (hull != null) hull.release();
                if (hullContour != null) hullContour.release();
                if (contour != null) contour.release();
                continue;
            }
            // --- Fin del Filtrado de Contornos ---

            // Si el contorno ha pasado todos los filtros extremadamente relajados, se considera una espora válida.
            Imgproc.drawContours(imgResult, List.of(contour), -1, new Scalar(0, 255, 0), drawContourThickness);
            conteoEsporas++;

            if (hull != null) hull.release();
            if (hullContour != null) hullContour.release();
            if (contour != null) contour.release();
        }
        Log.d(TAG, "Filtrado de contornos completado. Esporas válidas detectadas: " + conteoEsporas);

        // 10. Actualización de la Interfaz de Usuario (UI):
        String displayText = "Número total de esporas detectadas: " + conteoEsporas;

        Bitmap imgFinal = Bitmap.createBitmap(imgResult.cols(), imgResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgResult, imgFinal);

        imageView.setImageBitmap(imgFinal);
        textView.setText(displayText);
        Log.d(TAG, "Procesamiento de imagen finalizado y resultados actualizados en la UI.");

        // --- Liberación de Recursos de Memoria de OpenCV ---
        matOriginal.release();
        imgProcesar.release();
        imgGray.release();
        imgEqualized.release();
        binaryMask.release();
        kernel.release();
        hierarchy.release();
        imgResult.release();
        for (MatOfPoint c : contours) {
            c.release();
        }
        Log.d(TAG, "Todos los recursos de OpenCV (Mat objects) han sido liberados para prevenir fugas de memoria.");
    }
}