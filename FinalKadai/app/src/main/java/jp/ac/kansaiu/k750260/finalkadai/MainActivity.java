package jp.ac.kansaiu.k750260.finalkadai;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //ゲーム画面を開く
    public void gameStart(View view) {
        Intent intent = new Intent(this,GameActivity.class);
        startActivity(intent);
    }
}