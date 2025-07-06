package com.example.menuesporas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class tutorial extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_tutorial);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tutorial_root_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Botón para cerrar el tutorial actual
        Button buttonCerrarTutorial = findViewById(R.id.buttonCerrarTutorial);
        buttonCerrarTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Cierra esta actividad
            }
        });

        // Botón para el siguiente tutorial
        Button buttonSiguienteTutorial = findViewById(R.id.buttonSiguienteTutorial);
        buttonSiguienteTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { // ¡Aquí estaba el doble 'void'!
                // Abre la siguiente actividad del tutorial
                Intent intent = new Intent(tutorial.this, tutorial2.class);
                startActivity(intent);
                finish(); // Cierra este tutorial una vez que se inicia el siguiente
            }
        });
    }
}