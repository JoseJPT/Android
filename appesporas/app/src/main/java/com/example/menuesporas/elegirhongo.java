package com.example.menuesporas;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
// import android.widget.Toast; // Eliminamos esta importación, ya no es necesaria

public class elegirhongo extends AppCompatActivity {

    Button btnHongo1, btnHongo2, btnHongo3, btnHongo4, btnMedicionPorCuadro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elegirhongo);

        btnHongo1 = findViewById(R.id.btnHongo1);
        btnHongo2 = findViewById(R.id.btnHongo2);
        btnHongo3 = findViewById(R.id.btnHongo3);
        btnHongo4 = findViewById(R.id.btnHongo4);
        btnMedicionPorCuadro = findViewById(R.id.btnMedicionPorCuadro);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String tipo = "";

                if (view == btnHongo1) tipo = "Hongo 1";
                else if (view == btnHongo2) tipo = "Hongo 2";
                else if (view == btnHongo3) tipo = "Hongo 3";
                else if (view == btnHongo4) tipo = "Hongo 4";

                Intent intent = new Intent(elegirhongo.this, MainActivity2.class);
                intent.putExtra("tipo_espora", tipo);
                startActivity(intent);
                // Eliminado: Toast.makeText(getApplicationContext(), "Seleccionaste: " + tipo, Toast.LENGTH_SHORT).show();
            }
        };

        btnHongo1.setOnClickListener(listener);
        btnHongo2.setOnClickListener(listener);
        btnHongo3.setOnClickListener(listener);
        btnHongo4.setOnClickListener(listener);

        btnMedicionPorCuadro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(elegirhongo.this, MedicionEsporas.class);
                startActivity(intent);
            }
        });
    }
}