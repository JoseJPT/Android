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
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
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
    String tipoHongo = "";

    static {
        if (!OpenCVLoader.initDebug()) {
            // Falló la carga de OpenCV
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitymain2);  // <- ¡IMPORTANTE! Este nombre coincide con el XML de abajo

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.texto);
        Button btnAbrirGaleria = findViewById(R.id.btnAbrirGaleria);

        Intent intent = getIntent();
        tipoHongo = intent.getStringExtra("tipo_espora");

        btnAbrirGaleria.setOnClickListener(v -> abrirGaleria());
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                procesarImagen(imageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void procesarImagen(Bitmap bitmapOriginal) {
        Mat img = new Mat();
        Mat img2 = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Utils.bitmapToMat(bitmapOriginal, img);
        Utils.bitmapToMat(bitmapOriginal, img2);
        Imgproc.resize(img, img, new Size(1080, 802));
        Imgproc.resize(img2, img2, new Size(1080, 802));
        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2HSV);

        Core.inRange(img, new Scalar(0, 0, 0), new Scalar(0, 0, 90), img);

        Imgproc.medianBlur(img, img, 5);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.filter2D(img, img, -1, kernel);

        Mat img_result = img.clone();
        Imgproc.findContours(img_result, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(img2, contours, -1, new Scalar(74, 255, 1), 1);

        int conteo = contours.size();
        Imgproc.putText(img2, "Conteo: " + conteo, new Point(50, 100), Imgproc.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255, 0, 0), 2);

        Bitmap imgFinal = Bitmap.createBitmap(img2.cols(), img2.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img2, imgFinal);

        imageView.setImageBitmap(imgFinal);
        textView.setText("Número de esporas detectadas: " + conteo);
    }
}
