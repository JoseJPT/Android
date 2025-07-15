package com.example.menuesporas;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button btnSeleccionHongo, btnInfoProyecto, btnQuienesSomos, btnTuto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Asocia los IDs de los botones a tus variables Java
        btnSeleccionHongo = findViewById(R.id.btnSeleccionHongo);
        btnInfoProyecto = findViewById(R.id.btnInfoProyecto);
        btnQuienesSomos = findViewById(R.id.btnQuienesSomos);
        btnTuto = findViewById(R.id.btnTuto);


        btnSeleccionHongo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, elegirhongo.class);
            startActivity(intent);
        });

        btnInfoProyecto.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, infoproyecto.class);
            startActivity(intent);
        });

        btnQuienesSomos.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, quienesomos.class);
            startActivity(intent);
        });


        btnTuto.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, tutorial.class); // Apunta a tu clase 'tutorial.java'
            startActivity(intent);
        });
    }
}