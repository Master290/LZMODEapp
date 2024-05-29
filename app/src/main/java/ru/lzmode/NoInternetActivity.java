package ru.lzmode;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class NoInternetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        Button retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NoInternetActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
