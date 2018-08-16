package com.jam.morsecontroller;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;

public class StartActivity extends AppCompatActivity {


    long TIME_TO_DISPLAY_LETTER = 2000; // w ms
    long DURATION_SHORT_SIGNAL = 500; // w ms
    int MAX_SIGNAL_TAP = 5; // określa maksymalną liczbę zapisywanych stuknięć w sensor


    // Hashmapa do przetwarzania kodów morse'a
    HashMap<String, String> morseCode;
    String recognizedCodes[];
    String possibleValues[];

    // Tablica przechowujac sygnały wypukane w sensor
    // false - sygnał krótki
    // true - sygnał długi
    Boolean[] listOfSignals = new Boolean[MAX_SIGNAL_TAP]; // Rozważyć czy nie lepiej zmienić na liste
    int indexOfLastSignal = -1;
    String textInLatin = "";
    String letterInMorse = "";

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener proximitySensorListener;



    // Timery //
    private Handler signalHandler; // Handler do liczenia czasu aby wykryc rodzaj sygnalu
    private Handler letterHandler; // Handler do liczenia czasu aby wykryć koniec nadawania
    private TimeLong signalTimer = new TimeLong(0);
    private TimeLong letterTimer = new TimeLong(TIME_TO_DISPLAY_LETTER);
    boolean isFirstSignal = true; // Flaga do ustalenia czy po raz piewszy nadano sygnal

    Runnable signalRunnable = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {


            if(signalTimer.getTime() < DURATION_SHORT_SIGNAL) {
                signalTimer.setTime(signalTimer.getTime() + 50);

               // TextView timerField = findViewById(R.id.timerField);
                //timerField.setText(Long.toString(signalTimer.getTime()));
                // delayMillis niekoniecznie w milisekundach
                signalHandler.postDelayed(signalRunnable, 50);
            }

        }
    };

    Runnable letterRunnable = new Runnable() {
        @SuppressLint("SetTextI18n")
        @Override
        public void run() {

            if(letterTimer.getTime() > 0 ) {
                letterTimer.setTime(letterTimer.getTime() - 50);

                //TextView timerField = findViewById(R.id.timerField2);
                //timerField.setText(Long.toString(letterTimer.getTime()));
                // delayMillis niekoniecznie w milisekundach
                letterHandler.postDelayed(letterRunnable, 50);
            }
            else {
                displayText();
                //letterHandler.removeCallbacks(letterRunnable);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        ///////        OBSLUGA ZDARZEN     ////////
        Switch showTableOfCodes = findViewById(R.id.switch_options_or_tip);
        showTableOfCodes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    tradeTableOfCodeAndSettings(isChecked);
            }
        });


        // Disply seekbar //
        final int minDisplayTime = 300;
        final TextView textDisplayTime = findViewById(R.id.value_of_display_time);
        float time = (float) TIME_TO_DISPLAY_LETTER / 1000;
        String valueDisplayTime = Float.toString(time) + " sec";
        textDisplayTime.setText(valueDisplayTime);

        final SeekBar setDisplayTime = findViewById(R.id.set_time_to_display);
        setDisplayTime.setProgress((int)TIME_TO_DISPLAY_LETTER);

        setDisplayTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                long progressValue = (long) seekBar.getProgress() + minDisplayTime;
                TIME_TO_DISPLAY_LETTER = progressValue;
                letterTimer.setTime(TIME_TO_DISPLAY_LETTER);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                float sec = (float)(progress + minDisplayTime)/1000;
                String value = Float.toString(sec) + " sec";
                textDisplayTime.setText(value);
                //Toast.makeText(getApplicationContext(), String.valueOf(progress),Toast.LENGTH_LONG).show();

            }

        });

        // Duration short signal seekbar //
        final int minDurShortSignal = 250;
        final TextView textDurShortSignal = findViewById(R.id.value_of_short_signal);
        float timeShortSignal = (float) DURATION_SHORT_SIGNAL / 1000;
        String valueDurShortSignal = Float.toString(timeShortSignal) + " sec";
        textDurShortSignal.setText(valueDurShortSignal);

        final SeekBar setDurShortSignal = findViewById(R.id.set_duration_short_signal);
        setDurShortSignal.setProgress((int)DURATION_SHORT_SIGNAL);

        setDurShortSignal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                long progressValue = (long) seekBar.getProgress() + minDurShortSignal;
                DURATION_SHORT_SIGNAL = progressValue;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // TODO Auto-generated method stub
                float sec = (float)(progress + minDurShortSignal)/1000;
                String value = Float.toString(sec) + " sec";
                textDurShortSignal.setText(value);
                //Toast.makeText(getApplicationContext(), String.valueOf(progress),Toast.LENGTH_LONG).show();
            }

        });

            //                       MIODEK                     //
        // Wpisanie kluczy i wartości do hashmapy
        morseCode = new HashMap<>();
        recognizedCodes = getResources().getStringArray(R.array.recognizedMorseCode);
        possibleValues = getResources().getStringArray(R.array.answerForMorseCode);

        for(int i = 0; i < recognizedCodes.length; i++) {
            morseCode.put(recognizedCodes[i], possibleValues[i]);
        }


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        assert sensorManager != null;
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        // Handlery do obługi timerów
        signalHandler = new Handler();
        letterHandler = new Handler();


        // Sprawdza czy telefon obługuje wymagany sensor PROXIMITY
        if(proximitySensor == null) {
            Toast.makeText(this,
                    "Proximity sensor is not available",
                    Toast.LENGTH_LONG)
                    .show();
            finish();
        }

        proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                ImageView lighting = findViewById(R.id.image_animation_light_on);
                AnimationDrawable lighting_animation = (AnimationDrawable) lighting.getBackground();
                startCount(letterRunnable);
                if(sensorEvent.values[0] < proximitySensor.getMaximumRange()) { // Jak blisko
                    indexOfLastSignal++;
                    startCount(signalRunnable);
                    isFirstSignal = false;
                    //ImageView ligtOff = findViewById(R.id.image_animation_light_off);
                    //ligtOff.setVisibility(View.INVISIBLE);

                    lighting_animation.start();
                    stopCount(letterHandler, letterRunnable);
                    letterTimer.setTime(TIME_TO_DISPLAY_LETTER);
                }
                else {
                    //getWindow().getDecorView().setBackgroundColor(Color.GREEN);
                    lighting_animation.stop();
                    lighting_animation.selectDrawable(0);
                    if(!isFirstSignal) {
                        stopCount(signalHandler,signalRunnable);

                        if(indexOfLastSignal > MAX_SIGNAL_TAP-1) {
                            Toast.makeText(getApplicationContext(),
                                    "You've tapped the sensor too many times",
                                    Toast.LENGTH_LONG)
                                    .show();

                            // Reset nadawania kodu
                            resetSignalList();
                        }
                        else {
                            checkSignal();
                            displaySignal();
                        }
                    }
                    signalTimer.setTime(0);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(proximitySensorListener,
                proximitySensor,
                2 * 1000 * 1000);
    }

    @Override
    protected  void onPause() {
        super.onPause();
        sensorManager.unregisterListener(proximitySensorListener);
    }



////////   Dodatkowe metody do aplikacji  ///////

    void startCount(Runnable runnable) {
        runnable.run();
    }

    void stopCount(Handler handler, Runnable runnable) {
        handler.removeCallbacks(runnable);
    }

    private void displayText() {
        TextView morseText = findViewById(R.id.morseText);
        textInLatin += checkLetter();
        morseText.setText(textInLatin);

        if(textInLatin.length() > 0) {
            ImageView removeLetter = findViewById(R.id.btnRemoveLastLetter);
            ImageView sendMessage = findViewById(R.id.btnActionSend);
            removeLetter.setVisibility(View.VISIBLE);
            sendMessage.setVisibility(View.VISIBLE);
        }

        resetSignalList();
    }

    private void displaySignal() {
        letterInMorse = getReceivedSignals();

        //TextView receivedSignals = findViewById(R.id.receivedSignals);
        //String currentSignals = "Odebrane sygnały: " + letterInMorse;



        //receivedSignals.setText(currentSignals);
    }


    private boolean checkSignal() {
        LinearLayout ll = findViewById(R.id.receivedSignalsLayout);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        ImageView signalToDisplay = new ImageView(getApplicationContext());
        if(signalTimer.getTime() >= DURATION_SHORT_SIGNAL) {
            listOfSignals[indexOfLastSignal] = true;
            signalToDisplay.setImageResource(R.drawable.dash);

        } else {
            listOfSignals[indexOfLastSignal] = false;
            signalToDisplay.setImageResource(R.drawable.dot);
        }
        signalToDisplay.setLayoutParams(params);
        ll.addView(signalToDisplay);
        return listOfSignals[indexOfLastSignal];
    }

    private String checkLetter() {
        // 1 - długi sygnał
        // 0 - krótki sygnał

        String sentCode = "";
        String letterInLatin = "";

        // Wyczyszczenie pola do wyświetlania sygnałów
        LinearLayout ll = findViewById(R.id.receivedSignalsLayout);
        if(ll.getChildCount() > 0) {
            ll.removeAllViews();
        }

        int boolVal;
        for(int i = 0; i < indexOfLastSignal + 1; i++) {
            boolVal = listOfSignals[i] ? 1 : 0;
            sentCode += Integer.toString(boolVal);
        }

        if(morseCode.get(sentCode) != null) {
            letterInLatin = morseCode.get(sentCode);
        } else if(sentCode.length() > 0) {
            Toast.makeText(getApplicationContext(),
                    "Invalid code!",
                    Toast.LENGTH_LONG)
                    .show();
        }

        return letterInLatin;
    }

    // zwraca sygnały w formie stringu "-- *"
    private String getReceivedSignals() {
        String receivedSignals = "";
        for(int i = 0; i < indexOfLastSignal + 1; i++) {
            receivedSignals += listOfSignals[i] ? "--" : "*";
            receivedSignals += " ";
        }
        return receivedSignals;
    }

    // Reset nadawanego kodu
    private void resetSignalList() {
        Arrays.fill(listOfSignals, null);
        indexOfLastSignal = -1;
    }

    // METODY DO LISTENERÓW //
    private void tradeTableOfCodeAndSettings(boolean isCodeTable){
        ImageView imgAlphabetMorse = findViewById(R.id.alphabetOfMorse);
        SeekBar signalDuration = findViewById(R.id.set_duration_short_signal);
        SeekBar timeDisplay = findViewById(R.id.set_time_to_display);
        TextView textSignalDuration = findViewById(R.id.text_to_set_signal_duration);
        TextView textTimeDisply = findViewById(R.id.text_to_set_display_time);
        TextView valueSignalDur = findViewById(R.id.value_of_short_signal);
        TextView valueTimeDisplay = findViewById(R.id.value_of_display_time);
        if(isCodeTable){

            imgAlphabetMorse.setVisibility(View.VISIBLE);

            signalDuration.setVisibility(View.INVISIBLE);
            timeDisplay.setVisibility(View.INVISIBLE);
            textSignalDuration.setVisibility(View.INVISIBLE);
            textTimeDisply.setVisibility(View.INVISIBLE);
            valueSignalDur.setVisibility(View.INVISIBLE);
            valueTimeDisplay.setVisibility(View.INVISIBLE);
        } else {
            imgAlphabetMorse.setVisibility(View.INVISIBLE);

            signalDuration.setVisibility(View.VISIBLE);
            timeDisplay.setVisibility(View.VISIBLE);
            textSignalDuration.setVisibility(View.VISIBLE);
            textTimeDisply.setVisibility(View.VISIBLE);
            valueSignalDur.setVisibility(View.VISIBLE);
            valueTimeDisplay.setVisibility(View.VISIBLE);
        }
    }


    // Usuwanie ostatniej wprowadzonej litery
    public void deleteLastLetter(View view) {
        TextView message = findViewById(R.id.morseText);
        textInLatin = textInLatin.substring(0, textInLatin.length() - 1);
        message.setText(textInLatin);

        if(textInLatin.length() == 0 ) {
            ImageView removeLetter = findViewById(R.id.btnRemoveLastLetter);
            ImageView sendMessage = findViewById(R.id.btnActionSend);
            removeLetter.setVisibility(View.INVISIBLE);
            sendMessage.setVisibility(View.INVISIBLE);
        }
    }

    // Wysyłanie wiadomości
    public void sendMessage(View view) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, textInLatin);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}
