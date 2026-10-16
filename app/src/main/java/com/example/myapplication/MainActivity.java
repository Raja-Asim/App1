package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private final Handler splashHandler = new Handler(Looper.getMainLooper());

    private final Runnable openNextScreen = () -> {
        if (!isFinishing() && !isDestroyed()) {
            FirebaseAuth auth = AuthHelper.getAuthOrNull(this);
            Class<?> destination = auth != null && auth.getCurrentUser() != null
                    ? HomeActivity.class
                    : LoginActivity.class;
            startActivity(new Intent(MainActivity.this, destination));
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_root), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        splashHandler.postDelayed(openNextScreen, 1400);
    }

    @Override
    protected void onDestroy() {
        splashHandler.removeCallbacks(openNextScreen);
        super.onDestroy();
    }
}
