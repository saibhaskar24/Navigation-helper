package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Telephony;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonElement;

import java.util.Locale;
import java.util.Map;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;




public class Main extends AppCompatActivity implements AIListener, TextToSpeech.OnInitListener {

    private Button send;
    ImageButton listenButton;
    private TextView resultTextView;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    TextToSpeech textToSpeech;
    boolean granted = false;
    EditText mess;
    private AIRequest aiRequest;
    private AIService aiService;
    AIDataService aiDataService;


    LocationManager locationManager;
    LocationListener listener;
    String lat = "",lon = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        while (!granted) {
            requestAudioPermissions();
        }

        final AIConfiguration config = new AIConfiguration("9a8fa0380cc343d7af56217ac01ceb75",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        textToSpeech = new TextToSpeech(this,  this);

        aiService = AIService.getService(this, config);
        aiService.setListener(this);
        aiDataService = new AIDataService(config);
        aiRequest = new AIRequest();
        listenButton = (ImageButton) findViewById(R.id.listenButton);
        resultTextView = (TextView) findViewById(R.id.resultTextView);
        send = (Button)findViewById(R.id.send);
        mess = (EditText)findViewById(R.id.mess);




        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d("Location Coordinates", location.getLatitude() + " , " + location.getLongitude());
                lat = location.getLatitude() + " ";
                lon = location.getLongitude() + " ";
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Map<String, Object> user = new HashMap<>();
                user.put("Loc", lon);
                user.put("Lat", lat);
                db.collection("users")
                        .add(user)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("Firestore", "DocumentSnapshot added with ID: " + documentReference.getId());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("Firestore", "Error adding document", e);
                            }
                        });

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        else {
            Log.d("Location Manager","Triggered");
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
            }
            catch (Exception e) {
                Log.d("EXception", e.getMessage().toString());
            }
        }





    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length < 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
            }
        }
    }

    @Override
    public void onResult(AIResponse response) {
        Result result = response.getResult();
        // Get parameters
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }

        // Show results in TextView.
        resultTextView.setText("Query:" + result.getResolvedQuery() +
                "\nAction: " + result.getAction() +
                "\nParameters: " + parameterString
                + "\nTO speech " + result.getFulfillment().getSpeech());
        speakOut(result.getFulfillment().getSpeech());
        String s = result.getFulfillment().getSpeech();
        Intent intent = new Intent(this, DetectorActivity.class);
        if(s.equals("detection starts now")) {
            Log.d("execute","starting");
            startActivity(intent);
        }
        else {
            if(s.substring(0,2).equals("D:")) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q="+s.substring(2,s.length()));
                Intent intent1 = new Intent(Intent.ACTION_VIEW,gmmIntentUri);
                intent1.setPackage("com.google.android.apps.maps");
                Intent[]  i = new Intent[2];
                i[0]=intent;
                i[1]=intent1;
                startActivities(i);
            }
        }
        listenButton.setEnabled(true);

    }
    @Override
    public void onError(AIError error) {
        resultTextView.setText(error.toString());
    }
    @Override
    public void onAudioLevel(float level) {

    }
    @Override
    public void onListeningStarted() {

    }
    @Override
    public void onListeningCanceled() {

    }
    @Override
    public void onListeningFinished() {

    }

    public void listenButtonOnClick(View view) {
        aiService.startListening();
        listenButton.setEnabled(false);
    }

    public void send(View view) {

        aiRequest.setQuery(mess.getText().toString());
        new AsyncTask<AIRequest, Void, AIResponse>() {
            @Override
            protected AIResponse doInBackground(AIRequest... requests) {
                final AIRequest request = requests[0];
                try {
                    final AIResponse response = aiDataService.request(aiRequest);
                    return response;
                } catch (AIServiceException e) {
                }
                return null;
            }
            @Override
            protected void onPostExecute(AIResponse aiResponse) {
                if (aiResponse != null) {
                    onResult(aiResponse);// process aiResponse here
                }
            }
        }.execute(aiRequest);
        mess.setText("");
    }


    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            granted = true;
        }
    }

    /**
     * Called to signal the completion of the TextToSpeech engine initialization.
     *
     *
     *
     */

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
        super.onDestroy();
    }

    public void speakOut(String out) {
        textToSpeech.speak(out, TextToSpeech.QUEUE_ADD, null);

    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = textToSpeech.setLanguage(Locale.ENGLISH);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                speakOut("Start speaking");
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }
    public void direct(View view) {

        Intent intent = new Intent(this, DetectorActivity.class);
        startActivity(intent);
    }
}
