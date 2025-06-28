package com.example.menuesporas;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class tutorial extends AppCompatActivity { // Tu clase del tutorial

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Mantén esta línea si EdgeToEdge es parte de tu configuración

        // ¡CORRECTO! Aquí se carga el diseño de tu tutorial.
        setContentView(R.layout.activity_tutorial); // Asegúrate que apunta a tu XML del tutorial

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tutorial_root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}