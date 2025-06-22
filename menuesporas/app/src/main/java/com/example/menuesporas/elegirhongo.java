package com.example.menuesporas;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class elegirhongo extends AppCompatActivity {

    Button btnFusarium, btnAspergillus, btnPenicillium, btnAlternaria, btnCladosporium, btnMedicionPorCuadro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elegirhongo);

        btnFusarium = findViewById(R.id.btnFusarium);
        btnAspergillus = findViewById(R.id.btnAspergillus);
        btnPenicillium = findViewById(R.id.btnPenicillium);
        btnAlternaria = findViewById(R.id.btnAlternaria);
        btnCladosporium = findViewById(R.id.btnCladosporium);
        btnMedicionPorCuadro = findViewById(R.id.btnMedicionPorCuadro);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tipo = "";

                if (view == btnFusarium) tipo = "Fusarium";
                else if (view == btnAspergillus) tipo = "Aspergillus";
                else if (view == btnPenicillium) tipo = "Penicillium";
                else if (view == btnAlternaria) tipo = "Alternaria";
                else if (view == btnCladosporium) tipo = "Cladosporium";

                Intent intent = new Intent(elegirhongo.this, MainActivity2.class);
                intent.putExtra("tipo_espora", tipo);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "Seleccionaste: " + tipo, Toast.LENGTH_SHORT).show();
            }
        };

        btnFusarium.setOnClickListener(listener);
        btnAspergillus.setOnClickListener(listener);
        btnPenicillium.setOnClickListener(listener);
        btnAlternaria.setOnClickListener(listener);
        btnCladosporium.setOnClickListener(listener);

        btnMedicionPorCuadro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(elegirhongo.this, MedicionEsporas.class);
                startActivity(intent);
            }
        });
    }
}
