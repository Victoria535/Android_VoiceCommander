package com.example.voicecommander;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.database.FirebaseDatabase;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.text.format.DateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    private static int SIGN_IN_CODE = 1;
    private RelativeLayout activity_main;
    private FirebaseListAdapter<Message> adapter;
    private FloatingActionButton sendBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity_main = findViewById(R.id.activity_main);
        sendBtn = findViewById(R.id.buttonSend);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText textField = findViewById(R.id.messageField);
                if(textField.getText().toString().isEmpty())
                    return;

                textCommand(textField.getText().toString());
                textField.setText("");
            }
        });

        sendBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onClickMic();
                return true;
            }
        });
        //польз еще не авторизован
        if(FirebaseAuth.getInstance().getCurrentUser() == null)
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_CODE);
        else {
            Snackbar.make(activity_main, "Вы авторизованы", Snackbar.LENGTH_SHORT).show();
            displayAllMessages();
        }
        init();
    }

    private void displayAllMessages() {
        ListView listOfMessages = findViewById(R.id.list);
        adapter = new FirebaseListAdapter<Message>(this, Message.class, R.layout.list_item, FirebaseDatabase.getInstance().getReference()) {
            @Override
            protected void populateView(View v, Message model, int position) {
                // TODO проверка на текущего пользователя
                TextView mess_user, mess_time, mess_text;
                mess_user = v.findViewById(R.id.message_user);
                mess_time = v.findViewById(R.id.message_time);
                mess_text = v.findViewById(R.id.message_text);

                mess_user.setText(model.getUserName());
                mess_text.setText(model.getTextMessage());
                mess_time.setText(DateFormat.format("dd-MM-yyyy HH:mm:ss", model.getMessageTime()));
            }
        };

        listOfMessages.setAdapter(adapter);
    }

    private void init(){
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR)
                {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });
    }

    private void onClickMic() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите слово для распознавания");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        startActivityForResult(intent, 10);
    }

    private void saveInDB(String vityaMessage, String userMessage){
        FirebaseDatabase.getInstance().getReference().push().setValue(
                new Message(FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                        userMessage
                )
        );

        FirebaseDatabase.getInstance().getReference().push().setValue(
                new Message("Витя",
                        vityaMessage
                )
        );

        textToSpeech.speak( vityaMessage,
                TextToSpeech.QUEUE_FLUSH, null);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode)
            {
                case 10:
                    ArrayList<String> arrayText =  data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    textCommand(arrayText.get(0));
                    break;
            }
        }
    }

    private void textCommand(String text) {
        if(text.contains("Привет") || text.contains("Приветик")
                || text.contains("приветик") || text.contains("привет")) {
            saveInDB("Привет", text);
        }else if(text.contains("Какая погода сегодня") || text.contains("Какая сегодня погода")) {
            saveInDB("Сегодня солнечно, можно идти гулять!", text);
        }else if(text.contains("пока")) {
            saveInDB("Пока, не забывай меня!", text);
            finish();
        }else if(text.contains("карты") || text.contains("Открой карту") || text.contains("карта") ){
            saveInDB("открываю карты", text);
            openApp("com.google.android.apps.maps");
        }else if(text.contains("Открой гугл") || text.contains("Google")){
            saveInDB("открываю гугл", text);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"));
            startActivity(browserIntent);
        }else if(text.contains("Открой Инстаграм") || text.contains("Инстаграм") ||
                text.contains("Открой инсту")) {
            saveInDB("открываю инстаграм", text);
            openApp("com.instagram.android");
        }else if(text.contains("Открой vk") || text.contains("открой vk")
                || text.contains("вк") || text.contains("ВК") ||
                text.contains("ВКонтакте") || text.contains("Открой ВКонтакте")) {
            saveInDB("открываю вконтакте", text);
            openApp("com.vkontakte.android");
        }else if(text.contains("Открой Telegram") || text.contains("Telegram")) {
            saveInDB("открываю телеграм", text);
            openApp("org.telegram.messenger");
        }else {
            saveInDB("Я пока не знаю такой команды", text);
        }

    }

    public void openApp(String link){
        Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(link);
        startActivity(LaunchIntent);
    }


}