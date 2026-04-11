package com.ritesh.hoppeconnect;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class BuyTeaActivity extends AppCompatActivity {

    private final String UPI_ID = "8080769308@axl";
    private final String NAME = "HopeConnect Developer";

    private Button btn10, btn20, btn50, btn100, btn1000, btnCustom, btnPay, btnCopy;
    private EditText etAmount, etNote;
    private ImageView imgQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

       
        btn10 = findViewById(R.id.btn10);
        btn20 = findViewById(R.id.btn20);
        btn50 = findViewById(R.id.btn50);
        btn100 = findViewById(R.id.btn100);
        btn1000 = findViewById(R.id.btn1000);
        btnCustom = findViewById(R.id.btnCustom);
        btnPay = findViewById(R.id.btnPay);
        btnCopy = findViewById(R.id.btnCopy);

        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);

        imgQR = findViewById(R.id.imgQR);

       
        btn10.setOnClickListener(v -> onAmountSelected("10"));
        btn20.setOnClickListener(v -> onAmountSelected("20"));
        btn50.setOnClickListener(v -> onAmountSelected("50"));
        btn100.setOnClickListener(v -> onAmountSelected("100"));
        btn1000.setOnClickListener(v -> onAmountSelected("1000"));

       
        btnCustom.setOnClickListener(v -> {
            etAmount.setVisibility(View.VISIBLE);
            etAmount.setText("");
            etAmount.requestFocus();
            showKeyboard(etAmount);
        });

       
        btnPay.setOnClickListener(v -> {
            String amount = (etAmount.getText() != null) ? etAmount.getText().toString().trim() : "";
            if (TextUtils.isEmpty(amount)) {
                Toast.makeText(this, "Please select or enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            String note = (etNote.getText() != null) ? etNote.getText().toString().trim() : "Buy Tea Support";
            generateAndShowQr(amount, note);
        });

       
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("UPI ID", UPI_ID);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "UPI ID copied", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unable to copy", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onAmountSelected(String amount) {
        etAmount.setVisibility(View.VISIBLE);
        etAmount.setText(amount);
       
        String note = (etNote.getText() != null) ? etNote.getText().toString().trim() : "Buy Tea Support";
        generateAndShowQr(amount, note);
    }

    private void generateAndShowQr(String amount, String note) {
       
        amount = amount.replaceAll("[^0-9.]", "");
        if (TextUtils.isEmpty(amount)) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        String upiUri;
        try {
           
            String encodedName = URLEncoder.encode(NAME, "UTF-8");
            String encodedNote = URLEncoder.encode(note == null ? "" : note, "UTF-8");

            upiUri = "upi://pay?pa=" + UPI_ID
                    + "&pn=" + encodedName
                    + "&am=" + amount
                    + "&cu=INR"
                    + "&tn=" + encodedNote;
        } catch (UnsupportedEncodingException e) {
            upiUri = "upi://pay?pa=" + UPI_ID + "&pn=" + NAME + "&am=" + amount + "&cu=INR";
        }

        createQrBitmapAndSet(upiUri);
    }

    private void createQrBitmapAndSet(String text) {
        QRCodeWriter writer = new QRCodeWriter();
        final int sizePx = 800;
        try {
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx);

            Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < sizePx; x++) {
                for (int y = 0; y < sizePx; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            imgQR.setImageBitmap(bmp);
            imgQR.setVisibility(View.VISIBLE);

           
            hideKeyboard();

            Toast.makeText(this, "Scan this QR from your UPI app", Toast.LENGTH_SHORT).show();
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void showKeyboard(View v) {
        v.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}