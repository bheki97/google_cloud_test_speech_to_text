package za.ac.bheki97.googlecloud_test_speech_to_text;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeMetadata;
import com.google.cloud.speech.v1p1beta1.LongRunningRecognizeResponse;
import com.google.cloud.speech.v1p1beta1.RecognitionAudio;
import com.google.cloud.speech.v1p1beta1.RecognitionConfig;
import com.google.cloud.speech.v1p1beta1.SpeechClient;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1p1beta1.SpeechRecognitionResult;
import com.google.cloud.speech.v1p1beta1.SpeechSettings;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import za.ac.bheki97.googlecloud_test_speech_to_text.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private boolean audioRecordingPermissionGranted;
    private MediaRecorder mediaRecorder;
    private Handler mainHandler;
    private boolean isRecording;
    private String recordedFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mainHandler = new Handler();
        isRecording = false;
        mediaRecorder = new MediaRecorder();

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        //setup OnClickListener for the REcord BUtton
        setOnclickListenerForRecordBtn();


    }

    private void setOnclickListenerForRecordBtn() {
        binding.recordBtn.setOnClickListener( view ->{
            if (!isRecording) {
                if (audioRecordingPermissionGranted) {
                    try {
                        startAudioRecording();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                toggleRecording();

                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();

                try {
                    convertSpeech();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }


    public  void convertSpeech() throws Exception {
        // Instantiates a client with GOOGLE_APPLICATION_CREDENTIALS
        //System.out.println("Now Converting Speech!!!");
        GoogleCredentials credentials = null;
        SpeechSettings speechSettings = null;
        SpeechClient speech = null;
        try {
            credentials = GoogleCredentials.fromStream(getResources().openRawResource(R.raw.google_credentials));

            speechSettings = SpeechSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            speech = SpeechClient.create(speechSettings);

            Path path = Paths.get(recordedFileName);
            byte[] data = Files.readAllBytes(path);
            ByteString audioBytes = ByteString.copyFrom(data);

            // Configure request with local raw PCM audio
            RecognitionConfig config =
                    RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setLanguageCode("en-US")
                            .setSampleRateHertz(16000)
                            .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(audioBytes).build();

            // Use non-blocking call for getting file transcription
            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> response =
                    speech.longRunningRecognizeAsync(config, audio);
            System.out.println("Gotten the Results");
            while (!response.isDone()) {
                System.out.println("Waiting for response...");
                Thread.sleep(10000);
            }

            List<SpeechRecognitionResult> results = response.get().getResultsList();

            for (SpeechRecognitionResult result : results) {
                // There can be several alternative transcripts for a given chunk of speech. Just use the
                // first (most likely) one here.

                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                binding.transcriptionTxt.setText(alternative.getTranscript());
                System.out.println(alternative.getTranscript());
                System.out.printf("Transcription: %s%n", alternative.getTranscript());
            }
        }catch (IOException es){
                es.printStackTrace();
        }
    }

    private void startAudioRecording() throws IOException {
        toggleRecording();
        String uuid = UUID.randomUUID().toString();
        recordedFileName = getFilesDir().getPath() + "/" + uuid + ".3gp";

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(recordedFileName);

        mediaRecorder.prepare();
        mediaRecorder.start();
    }

    private void toggleRecording() {
        isRecording = !isRecording;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    binding.recordBtn.setText("Convert Speech");
                } else {
                    binding.recordBtn.setText("RECORD");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                audioRecordingPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }

        if (!audioRecordingPermissionGranted) {
            finish();
        }
    }
}