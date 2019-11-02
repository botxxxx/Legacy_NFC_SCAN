package com.iccl.nfctool;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.*;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.nfc.*;
import android.nfc.tech.*;
import android.os.*;
import android.text.InputType;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.widget.*;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private boolean write = false, nfc = false, clean = false;
    private static final String TAG = "tools"; // CLEANTAG
    private EditText m_tv;
    private WebView m_msg, m_log;
    private Button m_clean, m_write;
    private AlertDialog alertDialog;
    private IntentFilter[] gNdefExchangeFilters, gWriteTagFilters;
    private NfcAdapter nfcAdapter;
    private PendingIntent gNfcPendingIntent;
    private String tag;

    private String msg_front = "<![CDATA[\n" +
            "<html>\n" +
            "\t<head></head>\n" +
            "    <body>\n" +
            "        <div style=\"text-align:justify;font-size:30px;color:black;word-wrap:break-word;\">\n" +
            "\t]]>";
    private String msg_back = "<![CDATA[\n" +
            "      </div>\n" +
            " \t</body>\n" +
            " </html>\n" +
            "\t]]>";
    private String msg_test = "<![CDATA[\n" +
            "\t<html>\n" +
            "\t\t<head></head>\n" +
            "\t    <body>\n" +
            "\t        <div style=\"text-align:justify;font-size:30px;color:#000000;text-indent:2em;word-wrap:break-word;\">\n" +
            "\t       \t\t 大於消耗pppppppppppppppppppppp大於消耗ppppppppppppppppppppppppppppp大於消耗pppppppppppppppppppp大於消耗ppppppppppppppppppppp大於消耗」\n" +
            "\t        </div>\n" +
            "\t \t</body>\n" +
            "\t</html>\n" +
            " ]]>";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        findViewById();
        getDevice();
        setWebview();
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    protected void onPause() {
        super.onPause();
        if (nfc) {
            // 由於NfcAdapter啟動前景模式將相對花費更多的電力，要記得關閉。
            nfcAdapter.disableForegroundNdefPush(this);
        }
    }

    protected void onResume() {
        super.onResume();
        if (nfc) {
            // TODO 處理由Android系統送出應用程式處理的intent filter內容
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                NdefMessage[] messages = getNdefMessages(getIntent());
                String msg = new String(messages[0].getRecords()[0].getPayload());
                // 往下送出該intent給其他的處理對象
                setIntent(new Intent());
                // 往下送出該intent給其他的處理對象
                setIntent(new Intent());
                if (msg.length() > 0) {
                    getNFCString(msg);
                }
            }
            // 啟動前景模式支持Nfc intent處理
            enableNdefExchangeMode();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;
            default:
                break;
        }
        return false;
    }

    protected void onNewIntent(Intent intent) {
        if (!write && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NdefMessage[] msgs = getNdefMessages(intent);
            promptForContent(msgs[0]);
        }

        if (write && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            writeTag(getNoteAsNdef(), detectedTag);
        }
    }

    @SuppressWarnings("unused")
    private String getTimer() {
        String H, I, S;
        Calendar calendar = Calendar.getInstance();
        int h = calendar.get(Calendar.HOUR_OF_DAY);
        int i = calendar.get(Calendar.MINUTE);
        int s = calendar.get(Calendar.SECOND);
        if (h < 10) {
            H = "0" + h;
        } else {
            H = "" + h;
        }
        if (i < 10) {
            I = "0" + i;
        } else {
            I = "" + i;
        }
        if (s < 10) {
            S = "0" + s;
        } else {
            S = "" + s;
        }
        String time = H + "" + I + "" + S;
        return time;
    }

    private NdefMessage[] getNdefMessages(Intent intent) {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[]{};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[]{record});
                msgs = new NdefMessage[]{msg};
            }
        } else {
            Log.d(TAG, "Unknown intent.");
            finish();
        }
        return msgs;
    }

    private void promptForContent(final NdefMessage msg) {
        String body = new String(msg.getRecords()[0].getPayload());
        getNFCString(body);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void findViewById() {
        // m_log = (WebView) findViewById(R.id.m_log);
        m_msg = (WebView) findViewById(R.id.m_msg);
        // m_msg.setVerticalScrollBarEnabled(false);
        // m_msg.setHorizontalScrollBarEnabled(false);
        // m_msg.getSettings().setJavaScriptEnabled(true);
        // m_msg.getSettings().setDefaultTextEncodingName("UTF-8");
        m_tv = (EditText) findViewById(R.id.m_tv);
        m_clean = (Button) findViewById(R.id.m_clean);
        m_write = (Button) findViewById(R.id.m_write);
        m_clean.setOnClickListener(this);
        m_write.setOnClickListener(this);
        m_tv.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        m_tv.setGravity(Gravity.TOP);
        m_tv.setSingleLine(false);
        m_tv.setHorizontallyScrolling(false);
    }

    public void onClick(View v) {
        if (v == m_clean) {
            m_tv.setText("");
            tag = "";
            clean = true;
            addNFC();
        }
        if (v == m_write) {
            clean = false;
            if (m_tv.getText().toString().length() > 0) {
                tag = m_tv.getText().toString();
                addNFC();
            } else {
                toast("請在下方輸入字串");
            }
        }
    }

    private void setWebview() {
        m_log.getSettings().setJavaScriptEnabled(true);
        m_log.loadUrl("120.119.155.80/app");
        m_log.setWebViewClient(new WebViewClient() {
            @SuppressWarnings("unused")
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                SslCertificate sslCertificate = error.getCertificate();

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("SSL 憑證錯誤");
                builder.setMessage("無法驗證伺服器SSL憑證。\n仍要繼續嗎?");
                builder.setPositiveButton("繼續", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        handler.proceed();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        handler.cancel();
                    }
                });

                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void setWebviewString(String msg) {
        m_msg.loadDataWithBaseURL(null, msg_front + msg + msg_back, "text/html",
                "UTF-8", null);
    }

    private void getDevice() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            setWebviewString("裝置不支援NFC");
            m_clean.setEnabled(false);
            m_write.setEnabled(false);
        } else {
            nfc = true;
            setWebviewString("靠近可讀取NFC");
            gNfcPendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            try {
                ndefDetected.addDataType("text/plain");
            } catch (MalformedMimeTypeException e) {
            }
            gNdefExchangeFilters = new IntentFilter[]{ndefDetected};
        }
    }

    private void addNFC() {
        if (nfc) {
            disableNdefExchangeMode();
            enableTagWriteMode();
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("請靠近標籤.")
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            disableTagWriteMode();
                            enableNdefExchangeMode();

                        }
                    });
            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    @SuppressWarnings("deprecation")
    private void disableNdefExchangeMode() {
        nfcAdapter.disableForegroundNdefPush(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void enableTagWriteMode() {
        write = true;
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        gWriteTagFilters = new IntentFilter[]{tagDetected};
        nfcAdapter.enableForegroundDispatch(this, gNfcPendingIntent, gWriteTagFilters, null);
    }

    private void disableTagWriteMode() {
        write = false;
        nfcAdapter.disableForegroundDispatch(this);
    }

    @SuppressWarnings("deprecation")
    private void enableNdefExchangeMode() {
        nfcAdapter.enableForegroundNdefPush(MainActivity.this, getNoteAsNdef());

        nfcAdapter.enableForegroundDispatch(this, gNfcPendingIntent, gNdefExchangeFilters, null);
    }

    private NdefMessage getNoteAsNdef() {
        byte[] textBytes = setNFCString().getBytes();
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, "text/plain".getBytes(), new byte[]{},
                textBytes);
        return new NdefMessage(new NdefRecord[]{textRecord});
    }

    private void getNFCString(String tmp) {
        tag = tmp;
        Handler.obtainMessage().sendToTarget();
    }

    private String setNFCString() {
        return "" + tag;
    }

    private boolean writeTag(NdefMessage message, Tag tag) {

        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    toast("標籤只能讀取");
                    nfcClose();
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    toast("標籤容量不足");
                    toast("標籤容量:" + ndef.getMaxSize() + " bytes, 資料大小:" + size + " bytes.");
                    nfcClose();
                    return false;
                }

                ndef.writeNdefMessage(message);
                if (clean) {
                    toast("清除完成");
                } else {
                    toast("寫入完成");
                }
                nfcClose();
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        if (clean) {
                            toast("清除完成");
                        } else {
                            toast("寫入完成");
                        }
                        nfcClose();
                        return true;
                    } catch (IOException e) {
                        if (clean) {
                            toast("清除失敗");
                        } else {
                            toast("寫入失敗");
                        }
                        nfcClose();
                        return false;
                    }
                } else {
                    toast("標籤不支持NDEF格式");
                    nfcClose();
                    return false;
                }
            }
        } catch (Exception e) {
            if (clean) {
                toast("清除失敗");
            } else {
                toast("寫入失敗");
            }
            nfcClose();
            return false;
        }
    }

    private void nfcClose() {
        alertDialog.cancel();
        clean = false;
    }

    private void toast(String text) {
        Toast.makeText(this, text + "", Toast.LENGTH_SHORT).show();
    }

    private Handler Handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (tag.length() > 0) {
                setWebviewString(tag.toString());
            } else {
                setWebviewString("沒有資料");
            }
        }

        ;
    };
}