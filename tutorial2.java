package com.example.menuesporas;

import android.content.Intent; // Necesario para Intent
import android.os.Bundle;
import android.view.View;    // Necesario para View
import android.widget.Button; // Necesario para Button

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class tutorial2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Asegúrate de que este setContentView apunte a tu layout del tutorial2
        setContentView(R.layout.activity_tutorial2);

        // ¡CORRECCIÓN AQUÍ! Referencia el ID del layout raíz de activity_tutorial2.xml
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tutorial2_root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Botón para volver al tutorial anterior
        Button buttonTutorial2Atras = findViewById(R.id.buttonTutorial2Atras);
        buttonTutorial2Atras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Inicia la actividad del tutorial anterior
                Intent intent = new Intent(tutorial2.this, tutorial.class);
                startActivity(intent);
                finish(); // Cierra esta actividad (tutorial2)
            }
        });

        // Botón para finalizar todos los tutoriales e ir al menú principal (activity_main)
        Button buttonTutorial2Finalizar = findViewById(R.id.buttonTutorial2Finalizar);
        buttonTutorial2Finalizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Aquí, inicias tu actividad de menú principal
                Intent intent = new Intent(tutorial2.this, MainActivity.class); // Asumo que tu menú es MainActivity
                startActivity(intent);
                finish(); // Cierra esta actividad (tutorial2)
            }
        });
    }
}