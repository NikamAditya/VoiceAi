package com.example.voiceai;

import static android.Manifest.permission.RECORD_AUDIO;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private EditText editText;

    private String stringURLEndPoint = "https://api.openai.com/v1/chat/completions";
    private String stringAPIKey = "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";
    private String stringOutput = "";

    private TextToSpeech textToSpeech;

    private SpeechRecognizer speechRecognizer;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{RECORD_AUDIO},
                PackageManager.PERMISSION_GRANTED);

        textView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                textToSpeech.setSpeechRate((float) 0.8);
            }
        });


        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int i) {
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                String string = "";
                textView.setText("");
                if (matches != null) {
                    string = matches.get(0);
                    editText.setText(string);
                    chatGPTModel(string+" In maximum 3 sentences");
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
            }
        });
    }


    public void buttonAssist(View view){

        if (textToSpeech.isSpeaking()){
            textToSpeech.stop();
            return;
        }
        stringOutput = "";
        speechRecognizer.startListening(intent);
    }

    private void chatGPTModel( String stringInput){
        /*if (retryCount <= 0) {
            // If the maximum retry count is reached, display an error message.
            textView.setText("Error: Maximum retry count reached.");
            return;
        }*/
        textView.setText("In Progress ...");
        textToSpeech.speak("In Progress", TextToSpeech.QUEUE_FLUSH, null,null);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", "gpt-3.5-turbo");

            JSONArray jsonArrayMessage = new JSONArray();
            JSONObject jsonObjectMessage = new JSONObject();
            jsonObjectMessage.put("role", "user");
            jsonObjectMessage.put("content", stringInput);
            jsonArrayMessage.put(jsonObjectMessage);

            jsonObject.put("messages", jsonArrayMessage);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                stringURLEndPoint, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                String stringText = null;
                try {
                    stringText = response.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                stringOutput = stringOutput + stringText;
                textView.setText(stringOutput);
                textToSpeech.speak(stringOutput, TextToSpeech.QUEUE_FLUSH, null,null);
            }
        }/*,new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null && error.networkResponse.statusCode == 429) {
                    // If a 429 error is received, retry after the specified delay.
                    int retryAfter = Integer.parseInt(error.networkResponse.headers.get("Retry-After"));
                    try {
                        Thread.sleep(retryAfter * 1000); // Convert seconds to milliseconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    chatGPTModel(stringInput, retryCount - 1); // Retry with decreased retry count
                } else {
                    // Handle other errors here.
                    textView.setText("Error: " + error.getMessage());
                }
            }
        } */,new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                textView.setText("Error: Maximum retry count reached.");
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> mapHeader = new HashMap<>();
                mapHeader.put("Authorization", "Bearer " + stringAPIKey);
                mapHeader.put("Content-Type", "application/json");

                return mapHeader;
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                return super.parseNetworkResponse(response);
            }
        };

        int intTimeoutPeriod = 60000; // 60 seconds timeout duration defined
        RetryPolicy retryPolicy = new DefaultRetryPolicy(intTimeoutPeriod,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(retryPolicy);
        Volley.newRequestQueue(getApplicationContext()).add(jsonObjectRequest);
    }

}