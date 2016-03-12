package com.awds333.chatiotest;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends Activity {
    Socket socket;
    Button buttonsend;
    Context c = this;
    MainActivity main = this;
    TextView numUsers;
    JSONObject j;
    EditText nick;
    LinearLayout scrollView;
    boolean activatyde;
    LinearLayout.LayoutParams liner;
    LinearLayout.LayoutParams linerrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        activatyde = false;
        liner = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linerrite = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linerrite.gravity = Gravity.RIGHT;
        try {
            socket = IO.socket("http://46.101.96.234/");
        } catch (URISyntaxException e) {
        }
        buttonsend = (Button) findViewById(R.id.button);
        nick = (EditText) findViewById(R.id.editText);
        buttonsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if (info != null) {
                    if (!nick.getText().toString().equals("")) {
                        socket.on("connect", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                main.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        socket.emit("login", nick.getText().toString());
                                        socket.emit("add user", nick.getText().toString());
                                    }
                                });
                            }
                        });
                        socket.on("disconnect", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                main.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(c, "связь с сервером потеряна!", Toast.LENGTH_LONG).show();
                                        ((TextView) findViewById(R.id.users)).setText("~");
                                    }
                                });
                            }
                        });
                        socket.connect();
                        socket.on("login", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                j = (JSONObject) args[0];
                                if (!activatyde) {
                                    activatyde = true;
                                    try {
                                        onSecondCreate(j.getInt("numUsers"));
                                    } catch (JSONException e) {
                                    }
                                } else {
                                    main.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(c, "связь с сервером востановлена!", Toast.LENGTH_LONG).show();
                                            try {
                                                ((TextView) findViewById(R.id.users)).setText(j.getInt("numUsers") + "");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        Toast.makeText(c, "введите ник!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(c, "проверьте сетевое соединение!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    void onSecondCreate(final int users) {
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonsend = null;
                InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                m.hideSoftInputFromWindow(nick.getWindowToken(), 0);
                nick = null;
                main.setContentView(R.layout.message);
                scrollView = (LinearLayout) findViewById(R.id.sc);
                buttonsend = (Button) findViewById(R.id.button2);
                nick = (EditText) findViewById(R.id.editText2);
                nick.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        socket.emit("typing");
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                socket.emit("stop typing");
                            }
                        });
                        t.start();
                    }
                });
                ((TextView) findViewById(R.id.users)).setText(users + "");
                buttonsend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (socket.connected()) {
                            if (!nick.getText().toString().equals("")) {
                                socket.emit("new message", nick.getText().toString());
                                TextView usmessage = new TextView(c);
                                usmessage.setText(nick.getText().toString());
                                usmessage.setTextSize(20);
                                scrollView.addView(usmessage, 0, linerrite);
                                nick.setText("");
                            } else {
                                Toast.makeText(c, "введите сообщение!", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(c, "проверьте сетевое соединение!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                socket.on("new message", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        j = (JSONObject) args[0];
                        main.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LinearLayout lin = new LinearLayout(c);
                                lin.setOrientation(LinearLayout.HORIZONTAL);
                                TextView usnick = new TextView(c);
                                TextView usmessage = new TextView(c);
                                try {
                                    usnick.setText(j.getString("username") + ":");
                                    usmessage.setText(j.getString("message"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                usnick.setTextSize(22);
                                usmessage.setTextSize(19);
                                scrollView.addView(lin, 0, liner);
                                lin.addView(usnick, liner);
                                lin.addView(usmessage, liner);
                            }
                        });

                    }
                });
                socket.on("user joined", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        j=(JSONObject) args[0];
                        main.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ((TextView) findViewById(R.id.users)).setText(j.getInt("numUsers") + "");
                                    Toast.makeText(c,"к нам присоединился "+j.getString("username"),Toast.LENGTH_LONG).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                socket.on("user left", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        j=(JSONObject) args[0];
                        main.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ((TextView) findViewById(R.id.users)).setText(j.getInt("numUsers") + "");
                                    Toast.makeText(c,"нас покинул "+j.getString("username"),Toast.LENGTH_LONG).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
                socket.on("typing", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        j=(JSONObject) args[0];
                        main.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Toast.makeText(c,j.getString("username")+" набирает сообщение",Toast.LENGTH_LONG).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
            }
        });
        c = this;
        main = this;
    }
}
