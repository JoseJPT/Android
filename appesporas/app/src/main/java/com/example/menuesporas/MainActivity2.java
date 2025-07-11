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
 * MainActivity2 es la actividad encargada de permitir al usuario seleccionar una imagen de la galería,
 * procesarla utilizando la biblioteca OpenCV para detectar esporas, y mostrar los resultados.
 */
public class MainActivity2 extends AppCompatActivity {

    // Vistas de la interfaz de usuario
    ImageView imageView; // Muestra la imagen procesada con las esporas detectadas
    TextView textView;   // Muestra el número de esporas detectadas
    Bitmap imageBitmap;  // Almacena la imagen original seleccionada por el usuario
    String tipoHongo = ""; // Almacena el tipo de hongo seleccionado en la actividad anterior,
    // usado para ajustar los parámetros de detección.

    // Etiqueta para los mensajes de log, útil para depuración
    private static final String TAG = "MainActivity2";

    // Bloque estático para cargar la biblioteca OpenCV al inicio de la aplicación
    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Error al cargar OpenCV");
        } else {
            Log.d(TAG, "OpenCV cargado exitosamente");
        }
    }

    /**
     * Se llama cuando la actividad es creada por primera vez.
     * Inicializa las vistas y configura los listeners.
     * @param savedInstanceState Si la actividad se está recreando, este Bundle contiene
     * los datos de estado que más recientemente fueron guardados.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Establece el diseño de la interfaz de usuario para esta actividad, usando 'activitymain2'.
        setContentView(R.layout.activitymain2);

        // Inicializa las vistas conectándolas con sus IDs en el archivo de layout
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.texto);
        Button btnAbrirGaleria = findViewById(R.id.btnAbrirGaleria);

        // Recupera el tipo de hongo pasado desde la actividad anterior a través de un Intent
        Intent intent = getIntent();
        tipoHongo = intent.getStringExtra("tipo_espora");

        // Verifica si se recibió un tipo de hongo; si no, establece un valor por defecto
        if (tipoHongo != null && !tipoHongo.isEmpty()) {
            Log.d(TAG, "Modo de Hongo recibido: " + tipoHongo);
        } else {
            tipoHongo = "default"; // Si no se especifica, usa parámetros generales
            Log.d(TAG, "Modo de Hongo: default (no especificado en Intent)");
        }

        // Configura el listener para el botón "Abrir Galería"
        btnAbrirGaleria.setOnClickListener(v -> {
            Log.d(TAG, "Botón 'Abrir Galería' presionado");
            abrirGaleria(); // Llama al método para abrir la galería
        });
    }

    /**
     * Inicia un Intent para abrir la galería de imágenes del dispositivo
     * y permitir al usuario seleccionar una imagen.
     */
    private void abrirGaleria() {
        Log.d(TAG, "Abriendo galería de imágenes");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100); // Inicia la actividad esperando un resultado con el código 100
    }

    /**
     * Maneja el resultado de actividades externas (como la selección de imagen de la galería).
     * @param requestCode El código de solicitud original que se pasó a startActivityForResult().
     * @param resultCode El código de resultado devuelto por la actividad secundaria.
     * @param data Un Intent, que puede regresar datos de resultado a quien lo llama.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Verifica si el resultado es de la solicitud de la galería, si fue exitoso y si hay datos
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "Imagen seleccionada desde la galería. Request Code: " + requestCode + ", Result Code: " + resultCode);
            Uri imageUri = data.getData(); // Obtiene la URI de la imagen seleccionada
            try {
                // Convierte la URI en un Bitmap y lo pasa para procesamiento
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                procesarImagen(imageBitmap); // Llama al método para procesar la imagen
            } catch (IOException e) {
                // Registra cualquier error al cargar la imagen
                Log.e(TAG, "Error al cargar imagen desde URI: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Registra si la selección de imagen fue cancelada o fallida
            Log.d(TAG, "Selección de imagen cancelada o fallida. Request Code: " + requestCode + ", Result Code: " + resultCode);
        }
    }

    /**
     * Procesa la imagen de Bitmap utilizando OpenCV para detectar esporas.
     * Los parámetros de detección se ajustan según el 'tipoHongo' seleccionado.
     * @param bitmapOriginal El Bitmap de la imagen seleccionada por el usuario.
     */
    private void procesarImagen(Bitmap bitmapOriginal) {
        Log.d(TAG, "Iniciando procesamiento de imagen...");
        Mat matOriginal = new Mat();
        Utils.bitmapToMat(bitmapOriginal, matOriginal); // Convierte el Bitmap a un objeto Mat de OpenCV
        Log.d(TAG, "Bitmap convertido a Mat.");

        Mat imgProcesar = new Mat();
        // Redimensiona la imagen para un procesamiento consistente y menos intensivo computacionalmente
        Imgproc.resize(matOriginal, imgProcesar, new Size(640, 480));
        Log.d(TAG, "Imagen redimensionada a 640x480.");

        Mat imgGray = new Mat();
        // Convierte la imagen a escala de grises para simplificar el procesamiento
        Imgproc.cvtColor(imgProcesar, imgGray, Imgproc.COLOR_BGR2GRAY);
        Log.d(TAG, "Imagen convertida a escala de grises.");

        Mat imgEqualized = new Mat();
        // Aplica ecualización de histograma para mejorar el contraste de la imagen
        Imgproc.equalizeHist(imgGray, imgEqualized);
        Log.d(TAG, "Ecualización de histograma aplicada.");

        // Aplica un desenfoque Gaussiano para reducir el ruido y suavizar la imagen.
        // Se utiliza un kernel de (3, 3) para un suavizado ligero, preservando detalles.
        Imgproc.GaussianBlur(imgEqualized, imgEqualized, new Size(3, 3), 0);
        Log.d(TAG, "Desenfoque Gaussiano aplicado con kernel (3, 3).");

        // --- INICIO DE AJUSTES DE PARÁMETROS SEGÚN EL TIPO DE HONGO ---
        // Se establecen valores predeterminados para los parámetros de detección
        // Ajustes para una umbralización y filtrado de contornos más finos y para abarcar más esporas.
        int adaptiveBlockSize = 11; // Reducido para umbrales más locales y finos (debe ser impar)
        double adaptiveC = 2;       // Ajustado para modificar la sensibilidad del umbral

        double minArea = 2.0;       // Área mínima de un contorno (reducido para capturar esporas más pequeñas)
        double maxArea = 50.0;      // Área máxima de un contorno (reducido para evitar objetos grandes o fusionados)
        double minCircularity = 0.50; // Circularidad mínima del contorno (1.0 es un círculo perfecto)
        double minSolidity = 0.80;    // Solidez mínima del contorno (área del contorno / área del casco convexo)
        int openIterations = 1;     // Número de iteraciones para la operación morfológica de apertura
        int closeIterations = 1;    // Número de iteraciones para la operación morfológica de cierre
        int drawContourThickness = 1; // Grosor de la línea con la que se dibujan los contornos detectados (más delgado para precisión visual)

        // Parámetro: relación de aspecto máxima permitida para un contorno.
        // Ayuda a filtrar objetos alargados que no son esporas.
        double maxAspectRatio = 3.0;

        // Ajusta los parámetros de detección específicos para cada tipo de hongo.
        // Estos valores han sido optimizados para cada caso particular.
        switch (tipoHongo) {
            case "Hongo 1":
                adaptiveBlockSize = 11;
                adaptiveC = 3;
                minArea = 5.0;
                maxArea = 80.0;
                minCircularity = 0.55;
                minSolidity = 0.50;
                openIterations = 2;
                closeIterations = 3;
                drawContourThickness = 2;
                maxAspectRatio = 2.5;
                break;
            case "Hongo 2":
                adaptiveBlockSize = 15; // Podría ser útil un bloque ligeramente más grande si las esporas son más grandes.
                adaptiveC = 4;
                minArea = 7.0;
                maxArea = 70.0;
                minCircularity = 0.20;
                minSolidity = 0.50;
                openIterations = 2;
                closeIterations = 3;
                drawContourThickness = 1;
                maxAspectRatio = 4.0;
                break;
            case "Hongo 3":
                adaptiveBlockSize = 13;
                adaptiveC = 7;
                minArea = 7.0;
                maxArea = 70.0;
                minCircularity = 0.40;
                minSolidity = 0.85;
                openIterations = 2;
                closeIterations = 2;
                drawContourThickness = 2;
                maxAspectRatio = 3.0;
                break;
            case "Hongo 4":
                adaptiveBlockSize = 13;
                adaptiveC = 9;
                minArea = 5.0;
                maxArea = 80.0;
                minCircularity = 0.53;
                minSolidity = 0.40;
                openIterations = 2;
                closeIterations = 1;
                drawContourThickness = 1;
                maxAspectRatio = 3.5;
                break;
            default:
                // Si el tipo de hongo no es reconocido o es "default", se usan los valores predeterminados.
                break;
        }

        // Asegura que el tamaño del bloque adaptativo sea impar y al menos 3.
        if (adaptiveBlockSize % 2 == 0) adaptiveBlockSize++;
        if (adaptiveBlockSize < 3) adaptiveBlockSize = 3; // Mínimo tamaño para el kernel de OpenCV
        Log.d(TAG, "Parámetros finales de detección: BlockSize=" + adaptiveBlockSize + ", C=" + adaptiveC +
                ", MinArea=" + minArea + ", MaxArea=" + maxArea +
                ", MinCirc=" + minCircularity + ", MinSolidity=" + minSolidity +
                ", OpenIters=" + openIterations + ", CloseIters=" + closeIterations +
                ", ContourThickness=" + drawContourThickness +
                ", MaxAspectRatio=" + maxAspectRatio);
        // --- FIN DE AJUSTES DE PARÁMETROS SEGÚN EL TIPO DE HONGO ---

        Mat binaryMask = new Mat();
        // Aplica umbralización adaptativa para crear una máscara binaria.
        // ADAPTIVE_THRESH_GAUSSIAN_C calcula el umbral como la media ponderada Gaussiana de los vecinos.
        // THRESH_BINARY_INV invierte la imagen (objetos blancos sobre fondo negro).
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

        // Define un kernel elíptico para las operaciones morfológicas.
        // La operación de APERTURA (MORPH_OPEN) erosiona y luego dilata, eliminando pequeños objetos de ruido.
        // Se utiliza un kernel pequeño (3x3) para una limpieza muy suave, preservando formas finas.
        Mat kernelOpen = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(binaryMask, binaryMask, Imgproc.MORPH_OPEN, kernelOpen, new Point(-1, -1), openIterations);
        Log.d(TAG, "Operación de apertura (MORPH_OPEN) aplicada con kernel (3,3) y " + openIterations + " iteraciones.");

        // La operación de CIERRE (MORPH_CLOSE) dilata y luego erosiona, cerrando pequeños agujeros
        // dentro de los objetos y conectando componentes cercanos.
        // Se utiliza un kernel pequeño (3x3) para un cierre suave, evitando la fusión de esporas.
        Mat kernelClose = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(binaryMask, binaryMask, Imgproc.MORPH_CLOSE, kernelClose, new Point(-1, -1), closeIterations);
        Log.d(TAG, "Operación de cierre (MORPH_CLOSE) aplicada con kernel (3,3) y " + closeIterations + " iteraciones.");

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        // Encuentra los contornos de los objetos en la máscara binaria.
        // RETR_EXTERNAL recupera solo los contornos externos (evita contornos de agujeros internos).
        // CHAIN_APPROX_SIMPLE comprime los segmentos horizontales, verticales y diagonales.
        Imgproc.findContours(binaryMask.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Log.d(TAG, "Contornos encontrados. Número inicial de contornos: " + contours.size());

        Mat imgResult = imgProcesar.clone(); // Crea una copia de la imagen redimensionada para dibujar los resultados.

        int conteoEsporas = 0; // Contador de esporas detectadas

        // Itera sobre cada contorno encontrado para filtrarlos y contarlos.
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour); // Calcula el área del contorno.

            // Filtra contornos por área mínima y máxima para descartar ruido muy pequeño o objetos muy grandes.
            if (area < minArea || area > maxArea) {
                if (contour != null) contour.release(); // Libera la memoria del contorno si no se usa.
                continue; // Pasa al siguiente contorno
            }

            // Calcula el perímetro del contorno para determinar su circularidad.
            double perimeter = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
            // La circularidad se calcula como (4 * PI * área) / (perímetro * perímetro).
            // Un valor cercano a 1.0 indica una forma más circular.
            double circularity = (perimeter == 0) ? 0 : (4 * Math.PI * area / (perimeter * perimeter));

            // Filtra contornos por circularidad mínima.
            if (circularity < minCircularity) {
                if (contour != null) contour.release();
                continue;
            }

            // Calcula el casco convexo (convex hull) del contorno.
            MatOfInt hull = new MatOfInt();
            Imgproc.convexHull(contour, hull);

            // Convierte los índices del casco convexo en un contorno de puntos.
            MatOfPoint hullContour = new MatOfPoint();
            List<Point> hullPointsList = new ArrayList<>();
            List<Point> contourPoints = contour.toList();
            for (int i = 0; i < hull.rows(); i++) {
                hullPointsList.add(contourPoints.get((int)hull.get(i, 0)[0]));
            }
            hullContour.fromList(hullPointsList);
            double hullArea = Imgproc.contourArea(hullContour); // Área del casco convexo.

            // Calcula la solidez (solidity): área del contorno / área del casco convexo.
            // La solidez indica qué tan "sólido" o convexo es el objeto, sin grandes hendiduras.
            // Un valor cercano a 1.0 indica una forma compacta.
            double solidity = (hullArea == 0) ? 0 : (area / hullArea);

            // Filtra contornos por solidez mínima.
            if (solidity < minSolidity) {
                // Libera recursos si el contorno no pasa el filtro.
                if (hull != null) hull.release();
                if (hullContour != null) hullContour.release();
                if (contour != null) contour.release();
                continue;
            }

            // --- Filtro de Relación de Aspecto (Aspect Ratio) ---
            // Obtiene el cuadro delimitador rectangular del contorno.
            Rect boundingRect = Imgproc.boundingRect(contour);
            // Calcula la relación de aspecto: ancho / alto del cuadro delimitador.
            double aspectRatio = (double) boundingRect.width / boundingRect.height;

            // Filtra contornos con una relación de aspecto que no se ajuste a la de una espora.
            // Esto elimina objetos muy alargados o muy planos que suelen ser ruido.
            // Se verifica si el aspectRatio está fuera del rango [1/maxAspectRatio, maxAspectRatio].
            if (aspectRatio < (1.0 / maxAspectRatio) || aspectRatio > maxAspectRatio) {
                Log.d(TAG, "Contorno descartado por Aspect Ratio: " + aspectRatio + ". Area: " + area);
                // Libera recursos si el contorno no pasa el filtro.
                if (hull != null) hull.release();
                if (hullContour != null) hullContour.release();
                if (contour != null) contour.release();
                continue;
            }
            // --- Fin del filtro de Relación de Aspecto ---

            // Si el contorno pasa todos los filtros, se considera una espora.
            // Dibuja el contorno detectado en la imagen de resultados con un color verde.
            Imgproc.drawContours(imgResult, List.of(contour), -1, new Scalar(0, 255, 0), drawContourThickness);
            conteoEsporas++; // Incrementa el contador de esporas.

            // Libera la memoria de los objetos MatOfInt y MatOfPoint usados en esta iteración.
            if (hull != null) hull.release();
            if (hullContour != null) hullContour.release();
            if (contour != null) contour.release();
        }
        Log.d(TAG, "Filtrado de contornos completado. Esporas detectadas: " + conteoEsporas);
        String displayText = "Número total de esporas detectadas: " + conteoEsporas;

        // Convierte la imagen Mat de resultados de nuevo a Bitmap para mostrarla en el ImageView.
        Bitmap imgFinal = Bitmap.createBitmap(imgResult.cols(), imgResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgResult, imgFinal);

        // Muestra la imagen procesada y el conteo de esporas en la interfaz de usuario.
        imageView.setImageBitmap(imgFinal);
        textView.setText(displayText);
        Log.d(TAG, "Procesamiento de imagen finalizado y resultados mostrados.");

        // Es crucial liberar la memoria de todos los objetos Mat de OpenCV al finalizar su uso
        // para evitar fugas de memoria, ya que OpenCV usa memoria nativa.
        matOriginal.release();
        imgProcesar.release();
        imgGray.release();
        imgEqualized.release();
        binaryMask.release();
        kernelOpen.release();
        kernelClose.release();
        hierarchy.release();
        imgResult.release();
        // Asegúrate de liberar también los contornos si no fueron liberados individualmente
        // (aunque en este caso se liberan dentro del bucle si se descartan, es una buena práctica asegurar).
        for (MatOfPoint c : contours) {
            c.release();
        }
    }
}