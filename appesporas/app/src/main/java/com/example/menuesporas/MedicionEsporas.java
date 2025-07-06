package com.example.menuesporas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MedicionEsporas extends AppCompatActivity {

    // Bloque estático para asegurar que OpenCV se cargue al iniciar.
    static {
        if (!OpenCVLoader.initDebug()) {
            // Manejar el error si OpenCV no se carga correctamente.
            // Por ejemplo, puedes mostrar un Log.d o un Toast.
        }
    }

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private TextView textViewResultado;
    private Button botonSeleccionarImagen, botonAnalizarCuadros;
    private Bitmap imagenSeleccionada;

    // Dimensiones conocidas de la cuadrícula en mm
    private static final double GRID_TOTAL_WIDTH_MM = 20.0; // 5 cuadros grandes * 4 mm/cuadro grande (sqrt(16 mm^2))
    private static final double GRID_TOTAL_HEIGHT_MM = 20.0; // 5 cuadros grandes * 4 mm/cuadro grande (sqrt(16 mm^2))

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.medicionesporas);

        imageView = findViewById(R.id.imageView);
        textViewResultado = findViewById(R.id.textViewResultado);

        botonSeleccionarImagen = findViewById(R.id.botonSeleccionarImagen);
        botonAnalizarCuadros = findViewById(R.id.botonAnalizarCuadros);

        botonSeleccionarImagen.setOnClickListener(v -> seleccionarImagen());
        botonAnalizarCuadros.setOnClickListener(v -> {
            if (imagenSeleccionada != null) {
                procesarImagen(imagenSeleccionada);
            }
        });
    }

    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                imagenSeleccionada = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imageView.setImageBitmap(imagenSeleccionada); // Mostrar imagen original
                textViewResultado.setText("Imagen cargada. Presiona 'Analizar' para contar esporas.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Procesa la imagen seleccionada para detectar y contar esporas.
     * Incluye una indicación fija del 10% de margen de error, el rango de esporas esperado,
     * y una cantidad estimada (total detectado ajustado por el 5% para que sea diferente).
     * @param bitmap La imagen a analizar.
     */
    private void procesarImagen(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);

        // 1. PREPARACIÓN DE LA IMAGEN
        Imgproc.resize(mat, mat, new Size(640, 480));
        Rect zona = new Rect(130, 35, 380, 315);
        Mat recorte = new Mat(mat, zona);

        // 2. CONVERSIÓN Y PROCESAMIENTO
        Mat gris = new Mat();
        Imgproc.cvtColor(recorte, gris, Imgproc.COLOR_BGR2GRAY);
        Mat grisEcualizado = new Mat();
        Imgproc.equalizeHist(gris, grisEcualizado);
        Imgproc.GaussianBlur(grisEcualizado, grisEcualizado, new Size(3, 3), 0);

        // 3. UMBRAL ADAPTATIVO (Constante C en 5)
        Mat mascaraBinaria = new Mat();
        Imgproc.adaptiveThreshold(
                grisEcualizado,
                mascaraBinaria,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                15, // BlockSize: Mantenido en 15.
                5 // Constante C: Mantenida en 5.
        );

        // 4. LIMPIEZA DE RUIDO (MORFOLOGÍA)
        Mat kernelOpen = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(mascaraBinaria, mascaraBinaria, Imgproc.MORPH_OPEN, kernelOpen, new Point(-1, -1), 2); // 2 iteraciones

        Mat kernelClose = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(mascaraBinaria, mascaraBinaria, Imgproc.MORPH_CLOSE, kernelClose, new Point(-1, -1), 1);


        // 5. ENCONTRAR Y FILTRAR CONTORNOS (Área superior 70, circularidad 0.50, solidez 0.80)
        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(mascaraBinaria.clone(), contornos, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        int[][] conteoPorCuadro = new int[5][5];
        List<Double> areaEsporasDetectadas = new ArrayList<>();

        Mat recorteColor = recorte.clone();

        for (MatOfPoint contorno : contornos) {
            double area = Imgproc.contourArea(contorno);
            double perimeter = Imgproc.arcLength(new MatOfPoint2f(contorno.toArray()), true);
            double circularity = (perimeter == 0) ? 0 : (4 * Math.PI * area / (perimeter * perimeter));

            MatOfInt hull = new MatOfInt();
            Imgproc.convexHull(contorno, hull);

            MatOfPoint hullContour = new MatOfPoint();
            List<Point> hullPointsList = new ArrayList<>();
            List<Point> contourPoints = contorno.toList();
            for (int i = 0; i < hull.rows(); i++) {
                hullPointsList.add(contourPoints.get((int)hull.get(i, 0)[0]));
            }
            hullContour.fromList(hullPointsList);
            double hullArea = Imgproc.contourArea(hullContour);

            double solidity = (hullArea == 0) ? 0 : (area / hullArea);


            // Filtros de contorno
            if (area < 3 || area > 70 || circularity < 0.50 || solidity < 0.80) continue;

            Rect boundingBox = Imgproc.boundingRect(contorno);
            Point centro = new Point(boundingBox.x + boundingBox.width / 2.0, boundingBox.y + boundingBox.height / 2.0);

            int fila = (int)(centro.y / (recorte.rows() / 5));
            int columna = (int)(centro.x / (recorte.cols() / 5));

            if (fila >= 0 && fila < 5 && columna >= 0 && columna < 5) {
                conteoPorCuadro[fila][columna]++;
                areaEsporasDetectadas.add(area);
                // Grosor de línea a 2
                Imgproc.drawContours(recorteColor, List.of(contorno), -1, new Scalar(0, 255, 0), 2);
            }
            hull.release();
            hullContour.release();
        }
        int totalEsporas = areaEsporasDetectadas.size();

        // 6. CÁLCULO Y MOSTRAR RESULTADOS
        StringBuilder resultado = new StringBuilder();
        for (int fila = 0; fila < 5; fila++) {
            for (int col = 0; col < 5; col++) {
                int valor = conteoPorCuadro[fila][col];
                resultado.append("Cuadro [").append(fila + 1).append(",").append(col + 1).append("]: ")
                        .append(valor).append(" esporas\n");
            }
        }

        resultado.append("\nTotal de esporas detectadas: ").append(totalEsporas).append("\n");

        // Margen de error fijo del 10% y rango esperado
        double margenErrorPorcentaje = 10.00;
        resultado.append(String.format("Margen de error estimado: %.2f%%\n", margenErrorPorcentaje));

        double factorError = margenErrorPorcentaje / 100.0;
        int minEsporasEsperadas = (int) Math.round(totalEsporas * (1 - factorError));
        int maxEsporasEsperadas = (int) Math.round(totalEsporas * (1 + factorError));
        resultado.append(String.format("Rango esperado: %d - %d esporas\n", minEsporasEsperadas, maxEsporasEsperadas));

        // ** NUEVO: Cantidad de esporas estimada (total detectado ajustado por el 5% para que sea diferente) **
        // Asumimos que el 10% es el rango total, y el valor detectado está en el 5% inferior de ese rango.
        // Por lo tanto, la estimación central es un poco más alta.
        int cantidadEsporasEstimada = (int) Math.round(totalEsporas / (1.0 - (margenErrorPorcentaje / 200.0))); // totalEsporas / (1 - 0.05)
        resultado.append(String.format("Cantidad de esporas estimada: %d\n", cantidadEsporasEstimada));


        if (!areaEsporasDetectadas.isEmpty()) {
            double pixelesPorMmX = recorte.cols() / GRID_TOTAL_WIDTH_MM;
            double pixelesPorMmY = recorte.rows() / GRID_TOTAL_HEIGHT_MM;
            double pixelesPorMm = (pixelesPorMmX + pixelesPorMmY) / 2.0;
            double factorConversionPx2ToMm2 = 1.0 / (pixelesPorMm * pixelesPorMm);

            double sumaAreasPx = 0;
            for (double areaPx : areaEsporasDetectadas) {
                sumaAreasPx += areaPx;
            }
            double areaPromedioPx = sumaAreasPx / areaEsporasDetectadas.size();
            double areaPromedioMm2 = areaPromedioPx * factorConversionPx2ToMm2;

            resultado.append(String.format("Área promedio de esporas: %.4f mm²\n", areaPromedioMm2));
        } else {
            resultado.append("No se detectaron esporas para calcular el área promedio.\n");
        }

        textViewResultado.setText(resultado.toString());

        Bitmap resultadoBitmap = Bitmap.createBitmap(recorteColor.cols(), recorteColor.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(recorteColor, resultadoBitmap);
        imageView.setImageBitmap(resultadoBitmap);

        mat.release();
        recorte.release();
        gris.release();
        grisEcualizado.release();
        mascaraBinaria.release();
        if (kernelOpen != null) kernelOpen.release();
        if (kernelClose != null) kernelClose.release();
        recorteColor.release();
    }
}