package jp.ac.kansaiu.k750260.finalkadai;

//概要説明：
//制作期間：約3週間
//SurfaceViewを使い神経衰弱のゲームを作りました
//player1が先行で２人のプレイヤーが神経衰弱を行えるように設計した
//縦横4枚のカード,計16枚のカード
//ゲーム処理:
//めくったカードが異なっていた時の処理：
//カードをめくってから1.5秒が経過したらカードを裏にする.そして次の人の手番に変更
//めくったカードが一致していた時の処理:
//めくったカードはそのままで再度２枚がめくれるようにする。カードが一致するごとに得点を加算する。これをカードが異なるまで行う。
//勝敗処理は得点で比較して大きい方を勝者とする
//計算処理:
//説明PDFに詳しく記述しています。コードのコメントだけでは伝わらないと感じたためコメントには最低限のことしか記述していません。
//※多くの部分で外部から調べた関数や表現を使用しています。特にレイアウトなど。生成AIの使用を疑われるのは避けたいため参考にした記事は参考文献に詳しく記述しております。


import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameActivity extends AppCompatActivity implements SurfaceHolder.Callback, Runnable, View.OnTouchListener {
    int boardSize = 4;
    int totalCards = boardSize * boardSize;
    int screen_width, screen_height;
    Thread th_dr;
    SurfaceView sv;
    SurfaceHolder holder;
    boolean running;
    int[] afterCardNum = new int[totalCards];   //各マスに書かれている数字
    boolean[] openState = new boolean[totalCards]; //今めくれているか
    boolean[] isMatched = new boolean[totalCards]; //すでに一致したのか

    int firstCardNum = -1;   //1枚目にめくったカードの番号(初期値はあえて負の数にすることで選ばれていないことを示す)
    int secondCardNum = -1;  //2枚目にめくったカードの番号(初期値はあえて負の数にすることで選ばれていないことを示す)
    boolean touchLock = false;        //trueの間はタッチ無効かする
    int currentPlayer = 1; //今どちらの手番か（player1かplayer2か）
    int player1Score = 0;
    int player2Score = 0;
    int matchedPairs = 0;  //揃ったペアの数
    int cardSize; //カード一枚の大きさ(正方形)
    int startX;
    int startY;
    int margin;
    TextView player1Text;
    TextView player2Text;

    Paint backgroundPaint = new Paint();
    Paint backPaint = new Paint();   // カードの裏
    Paint frontPaint = new Paint();  // めくった時のカード
    Paint matchedPaint = new Paint();// 一致した時のカード
    Paint linePaint = new Paint();   // カードの枠線
    Paint textPaint = new Paint();   // 数字の文字

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // 手番表示用のTextViewを取得
        player1Text = (TextView) findViewById(R.id.player1TextView);
        player2Text = (TextView) findViewById(R.id.player2TextView);

        // SurfaceViewをつくる
        sv = (SurfaceView) findViewById(R.id.gameSurfaceView);
        holder = sv.getHolder();
        holder.addCallback(this);
        sv.setOnTouchListener(this);

        //準備処理を行う
        setColor();
        setCards();
        updatePlayerText();
    }

    @Override
    public void onStart() {
        super.onStart();
        th_dr = new Thread(this);
        th_dr.start(); // 描画スレッドを開始する
    }

    @Override
    public void onStop() {
        super.onStop();
        th_dr = null; // 描画スレッドを止める
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screen_width = width;
        screen_height = height;
        running = true;
        calcLayout(); // 画面サイズが分かったのでカードの位置を計算する
    }

    public void surfaceCreated(SurfaceHolder holder) {}
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
    }

    public void run() {
        while (th_dr != null) {
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                drawBoard(canvas); // カードを描く
                holder.unlockCanvasAndPost(canvas);
            }
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int index = getCardIndex((int) event.getX(), (int) event.getY());
                openCard(index);
                break;
        }
        return true;
    }

    //色の設定をする
    void setColor() {
        backgroundPaint.setColor(Color.parseColor("#FAFAFA"));
        backPaint.setColor(Color.parseColor("#90A4AE"));

        frontPaint.setColor(Color.parseColor("#FFFFFF"));
        matchedPaint.setColor(Color.parseColor("#A5D6A7"));
        linePaint.setColor(Color.parseColor("#37474F"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);

        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(52f);
    }

    // 1〜8の数字を2枚ずつ用意してシャッフルする（8ペアつまり16枚）
    void setCards() {
        //1~8の組を２つずつ作りリストに入れる
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= totalCards / 2; i++) {
            numbers.add(i);
            numbers.add(i);
        }
        // 順番をランダムにする
        Collections.shuffle(numbers);

        //シャッフルした後の数字、カードの表裏、一致の判定を分かりやすくするため再度配列に入れる
        for (int i = 0; i < totalCards; i++) {
            afterCardNum[i] = numbers.get(i);
            openState[i] = false;
            isMatched[i] = false;
        }

        firstCardNum = -1;
        secondCardNum = -1;
        touchLock = false;
        currentPlayer = 1;
        matchedPairs = 0;
        player1Score = 0;
        player2Score = 0;
    }

    // カードを1枚ずつ描画する
    void drawBoard(Canvas canvas) {
        //背景色を塗る
        canvas.drawRect(0, 0, screen_width, screen_height, backgroundPaint);

        //16枚のカード（正方形）を描画する
        for (int i = 0; i < totalCards; i++) {
            Rect rect = getRect(i);

            //カードに塗る色を決める
            Paint fill;
            if (isMatched[i]) {
                //一致してる時
                fill = matchedPaint;
            } else if (openState[i]) {
                //めくられている時
                fill = frontPaint;
            } else {
                fill = backPaint;
            }

            canvas.drawRect(rect, fill);
            canvas.drawRect(rect, linePaint);

            String text;
            if (openState[i] || isMatched[i]) {
                text = String.valueOf(afterCardNum[i]);
            } else {
                text = "?";
            }

            //テキストをカードの真ん中に書くための計算
            float textY = rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f;
            canvas.drawText(text, rect.centerX(), textY, textPaint);
        }
    }

    // 画面の真ん中に4×4のマスが収まるように、カードの大きさと位置を計算する
    void calcLayout() {
        margin = 4;
        cardSize = (screen_width - margin * 2 * boardSize) / boardSize;

        int gridSize = (cardSize + margin * 2) * boardSize;
        startX = (screen_width - gridSize) / 2;
        startY = (screen_height - gridSize) / 2;
    }

    // カードが画面のどこにあるのかを計算
    Rect getRect(int index) {

        //行と列
        int row = index / boardSize;
        int col = index % boardSize;

        //左と右
        int left = startX + col * (cardSize + margin * 2) + margin;
        int top = startY + row * (cardSize + margin * 2) + margin;

        return new Rect(left, top, left + cardSize, top + cardSize);
    }

    // タッチした場所がどのカードの上かを調べる　見つからなければ-1に
    int getCardIndex(int x, int y) {
        //0番から15番を順番に調べる
        for (int i = 0; i < totalCards; i++) {
            //タッチ座標をgetRectで取得しcontainsで含まれるか判定
            if (getRect(i).contains(x, y)) {
                return i;
            }
        }
        return -1;
    }


    //下記に記述している部分がゲーム処理
    // カードをめくる処理（タッチされたときによばれる
    void openCard(int index) {

        //タッチが可能であるのかを確認する 可能でない(touchLockがtrue)ときカードはめくれないようにreturnする
        if(touchLock) {
            return;
        }
        // カード以外の場所がタッチされたか確認する カード以外の場所がタッチされた場合終了するためreturn
        if (index == -1) {
            return;
        }

        //めくったカードを再度押しても反応しないようにするため条件としてすでにめくられているか一致しているときはめくれないようにreturn
        if (openState[index] || isMatched[index]) {
            return;
        }
        // カードをめくる
        openState[index] = true;

        // 1枚目
        if (firstCardNum == -1) {

            firstCardNum = index;
        } else {
            secondCardNum = index;
            touchLock = true;

            //2枚目をめくった時に遅延処理 1.5秒をおこなう
            sv.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //下に記述している一致判定をするメソッドを呼び出し一致判定を行う
                    checkMatch();
                }
            }, 1500);
        }
    }

    //一致しているかどうかの判定の処理
    void checkMatch() {
        //一致していた時の処理　一致した場合、手番は交代しない
        if (afterCardNum[firstCardNum] == afterCardNum[secondCardNum]) {
            //カードを表のままにして得点を加える
            isMatched[firstCardNum] = true;
            isMatched[secondCardNum] = true;

            matchedPairs++;

            if (currentPlayer == 1) {
                player1Score++;
            } else {
                player2Score++;
            }
        } else {
            //一致していないときカードを裏に戻して手番を交代する
            openState[firstCardNum] = false;
            openState[secondCardNum] = false;

            if (currentPlayer == 1) {
                currentPlayer = 2;
            } else {
                currentPlayer = 1;
            }
        }

        firstCardNum = -1;
        secondCardNum = -1;
        touchLock = false;

        //手番交代と得点を更新する
        updatePlayerText();

        //終了処理 カードのペア数（すべてのカード１６枚の半分）がそろったとき結果画面に移行する
        if (matchedPairs == totalCards / 2) {
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra("player1Score", player1Score);
            intent.putExtra("player2Score", player2Score);
            startActivity(intent);
        }
    }

    // 手番のプレイヤーの表示の色を明るくして得点更新する処理
    void updatePlayerText() {
        if (currentPlayer == 1) {
            player1Text.setBackgroundColor(Color.parseColor("#FFD54F"));
            player2Text.setBackgroundColor(Color.parseColor("#E0E0E0"));
        } else {
            player1Text.setBackgroundColor(Color.parseColor("#E0E0E0"));
            player2Text.setBackgroundColor(Color.parseColor("#FFD54F"));
        }
        player1Text.setText("Player1  " + player1Score + "セット");
        player2Text.setText("Player2  " + player2Score + "セット");
    }
}