package jp.ac.kansaiu.k750260.finalkadai;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.view.View;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        //スコアの受け取り
        Intent intent = getIntent();
        int player1Score = intent.getIntExtra("player1Score", 0);
        int player2Score = intent.getIntExtra("player2Score", 0);

        TextView player1ResultTextView = (TextView) findViewById(R.id.player1Result);
        TextView player2ResultTextView = (TextView) findViewById(R.id.player2Result);
        TextView winnerTextView = (TextView) findViewById(R.id.winner);

        player1ResultTextView.setText("player1: " + player1Score + "セット");
        player2ResultTextView.setText("player2: " + player2Score + "セット");

        //勝敗管理 得点の大きな方が勝者とする　同じ場合はドローとする
        String winnerText;
        if (player1Score > player2Score) {
            winnerText = "winner player1";
        } else if (player2Score > player1Score) {
            winnerText = "winner player2";
        } else {
            winnerText = "draw";
        }
        winnerTextView.setText(winnerText);
    }

    //ホーム画面に戻る
    public void backHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}