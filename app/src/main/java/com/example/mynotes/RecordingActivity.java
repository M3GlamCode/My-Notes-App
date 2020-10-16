package com.example.mynotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mynotes.Database.AppDatabase;
import com.example.mynotes.Database.AppExecutors;
import com.example.mynotes.Models.Note;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RecordingActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOG_TAG = RecordingActivity.class.getSimpleName();
    private static final int REQUEST_CODE = 200;
    private static final int TIME_INTERVAL = 1000;
    private static String filePath;
    private File audioFile;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private ImageButton recordBtn, playBtn;
    private Chronometer chronometer;
    private LinearLayout recordingLayout, mediaLayout;
    private TextView timePassedView, timeRemainingView;
    private SeekBar mediaSeekBar;
    private EditText recTitle, recNoteBody;
    private Runnable runnable;
    private Handler handler;

    private Intent intent;
    private AppDatabase appDb;
    private int noteId;
    private String retrievedFilePath;
    private String date;

    private boolean recording = true, playing = true, reset = false, recLayoutVisible = true, recordingExists = false;
    private long timeWhenStopped = 0;

    private boolean permissionToRecordGranted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            permissionToRecordGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if(!permissionToRecordGranted){
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE);

        initializeViews();

        Date calendar = Calendar.getInstance().getTime();

        // to set name of the recording
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.getDefault());
        String fileTime = dateFormat.format(calendar);
        String path = getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath();
        String fileName = "/REC_" + fileTime + ".3gp";
        filePath =  path + fileName;

        // day recording was taken
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        date = simpleDateFormat.format(calendar);

        mediaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                if (fromUser) {
                    if(mediaPlayer != null) {
                        Log.e(LOG_TAG, Integer.toString(progress));
                        //seekBar.setProgress(progress);
                        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                            @Override
                            public void onSeekComplete(MediaPlayer mp) {
                                mediaPlayer.seekTo(progress);
                            }
                        });
                    }
                    else{
                        Toast.makeText(RecordingActivity.this, "Hit the play button", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //Saving to the Room database
        appDb = AppDatabase.getInstance(getApplicationContext());
        intent = getIntent();

        if (intent != null && intent.hasExtra("note_id")) {
            recordingExists = true;
            viewVisibility(false);
            noteId = intent.getIntExtra("note_id", -1);

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    Note note = appDb.notesDao().loadNoteById(noteId);
                    populateUI(note);
                }
            });
        }
    }//END OF onCreate

    private void populateUI(Note note) {
        if (note == null){ return; }
        recTitle.setText(note.getTitle());
        recNoteBody.setText(note.getBody());
        retrievedFilePath = note.getRecording();

        audioFile = new File(retrievedFilePath);
    }//END OF populateUI

    private void initializeViews(){
        recordBtn = findViewById(R.id.record_button);
        recordBtn.setOnClickListener(this);
        playBtn = findViewById(R.id.play_button);
        playBtn.setOnClickListener(this);
        findViewById(R.id.stop_recording_button).setOnClickListener(this);
        findViewById(R.id.reset_button).setOnClickListener(this);
        findViewById(R.id.stop_media_button).setOnClickListener(this);
        findViewById(R.id.redo_button).setOnClickListener(this);

        findViewById(R.id.save_recording_btn).setOnClickListener(this);

        chronometer = findViewById(R.id.chronometer);
        recordingLayout = findViewById(R.id.recording_layout);
        mediaLayout = findViewById(R.id.play_media_layout);

        timePassedView = findViewById(R.id.media_time_Passed);
        timeRemainingView = findViewById(R.id.media_time_remaining);

        mediaSeekBar = findViewById(R.id.media_seek_bar);

        recTitle = findViewById(R.id.recording_note_title);
        recNoteBody = findViewById(R.id.recording_note_body);

        handler = new Handler();
    }//END OF initializeViews

    private void record(boolean recording){
        if (recording){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                recordBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause, null));
            }
            if (!reset && mediaRecorder != null){ // when media player exists and is paused
                Log.e(LOG_TAG, "resuming recording");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mediaRecorder.resume();
                }
                chronometer.setBase(SystemClock.elapsedRealtime() - timeWhenStopped);
                chronometer.start();
            }
            else { // when media player does not exist or is reset
                startRecording();
                if (reset){ reset = false; }
            }
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                recordBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_btn_speak_now, null));
            }
            pauseRecording();
        }
    }//END OF record

    private void startRecording(){
        Log.e(LOG_TAG, "starting recording");
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(filePath);
        mediaRecorder.setMaxDuration(300000); //5 minutes

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();

        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
                    Toast.makeText(RecordingActivity.this, "Time limit reached", Toast.LENGTH_SHORT).show();
                    stopRecording();

                }
            }
        });
        recordingExists = true;
        retrievedFilePath = filePath;
        audioFile = new File(retrievedFilePath);
    }//END OF startRecording

    private void pauseRecording(){
        Log.e(LOG_TAG, "pausing recording");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder.pause();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        timeWhenStopped = SystemClock.elapsedRealtime() - chronometer.getBase();
        chronometer.stop();
    }//END OF pauseRecording

    private void stopRecording(){
        if (mediaRecorder != null && !reset) {
            Log.e(LOG_TAG, "stopping recording");
            recLayoutVisible = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                recordBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_btn_speak_now, null));
            }
            try { mediaRecorder.stop(); }
            catch (IllegalStateException e) {
                e.printStackTrace();
            }
            mediaRecorder.release();
            mediaRecorder = null;
            recording = true;
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            viewVisibility(false);
        }
    }//END OF stopRecording

    public void resetRecording(){
        if (mediaRecorder != null){
            Log.e(LOG_TAG, "resetting recording");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                recordBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_btn_speak_now, null));
            }
            mediaRecorder.reset();
            recording = true;
            reset = true;
        }
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
    }//END OF resetRecording

    private void play(boolean playing){
        if (playing){
            if (mediaPlayer != null){
                Log.e(LOG_TAG, "resuming media");
                mediaPlayer.start();
            }
            else { startPlaying(); }
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                playBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play, null));
            }
            pauseMedia();
        }
    }//END OF play

    private void getAudioStats(){
        int duration = mediaPlayer.getDuration() / TIME_INTERVAL; //time in seconds
        int timeRemaining = duration - (mediaPlayer.getCurrentPosition() / TIME_INTERVAL);
        int timePassed = duration - timeRemaining;

        timePassedView.setText(String.format(Locale.getDefault(), "%d:%02d", timePassed / 60, timePassed % 60));
        timeRemainingView.setText(String.format(Locale.getDefault(), "%d:%02d", timeRemaining / 60, timeRemaining % 60));
    }//END OF getAudioStats

    private void initializeSeekBar(){
        mediaSeekBar.setMax(mediaPlayer.getDuration() / TIME_INTERVAL);

        runnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null){
                    int currentPosition = mediaPlayer.getCurrentPosition() / TIME_INTERVAL;
                    mediaSeekBar.setProgress(currentPosition);
                    getAudioStats();
                }
                handler.postDelayed(runnable, 50);
            }
        };
        handler.postDelayed(runnable, 50);
    }//END OF initializeSeekBar

    private void startPlaying() {
        Log.e(LOG_TAG, "starting media");
        if (audioFile != null){
            if (audioFile.exists()){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    playBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause, null));
                }
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(retrievedFilePath);
                    mediaPlayer.prepare();
                } catch (IllegalStateException | IOException e) {
                    Log.e(LOG_TAG, "Error in retrieving media file");
                }

                getAudioStats();
                initializeSeekBar();
                mediaPlayer.start();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.e(LOG_TAG, "end of media");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            playBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play, null));
                        }
                        mp.seekTo(0);
                        playing = true;
                    }
                });
            }
            else {
                playing = false;
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle(getResources().getString(R.string.missing_media_alert_title))
                        .setMessage(getResources().getString(R.string.missing_media_alert_message))
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                viewVisibility(true);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("DELETE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        });
                alert.show();
            }
        }
    }//END OF startPlaying

    private void pauseMedia(){
        Log.e(LOG_TAG, "pausing media");
        mediaPlayer.pause();
    }//END OF pauseMedia

    private void stopPlaying() {
        if (mediaPlayer != null) {
            Log.e(LOG_TAG, "stopping media");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                playBtn.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play, null));
            }
            mediaSeekBar.setProgress(0);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            playing = true;
        }
    }//END OF stopPlaying

    private void redoRecording(){
        Log.e(LOG_TAG, "redoing recording");
        recLayoutVisible = true;
        viewVisibility(true);
        stopPlaying();
    }//END OF redoRecording

    private void viewVisibility(boolean recVisible){
        if (recVisible) {
            chronometer.setVisibility(View.VISIBLE);
            recordingLayout.setVisibility(View.VISIBLE);
            mediaLayout.setVisibility(View.GONE);
        }
        else{
            chronometer.setVisibility(View.GONE);
            recordingLayout.setVisibility(View.GONE);
            mediaLayout.setVisibility(View.VISIBLE);
        }
    }//END OF viewVisibility

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.record_button:
                record(recording);
                recording = !recording;
                break;
            case R.id.stop_recording_button:
                stopRecording();
                break;
            case R.id.reset_button:
                resetRecording();
                break;
            case R.id.play_button:
                Log.e(LOG_TAG, "playing is " + playing);
                play(playing);
                playing = !playing;
                break;
            case R.id.stop_media_button:
                stopPlaying();
                break;
            case R.id.redo_button:
                redoRecording();
                break;
            case R.id.save_recording_btn:
                saveRecordingNote();
                break;
            default:
                break;
        }
    }

    private void deleteRecordingNote() {
        if (audioFile != null) {
            if (audioFile.exists()) {
                try {
                    audioFile.delete();
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Audio file doesn't exist", Toast.LENGTH_SHORT).show();
            }
        }
        String title = recTitle.getText().toString();
        String body = recNoteBody.getText().toString();
        if (intent != null && intent.hasExtra("note_id")) {
            final Note note = new Note(noteId, title, body, date, retrievedFilePath);

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    appDb.notesDao().deleteNote(note);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RecordingActivity.this, "Recording deleted", Toast.LENGTH_SHORT).show();
                        }
                    });
                    finish();
                }
            });
        }
        else {
            finish();
            Log.e(LOG_TAG, "Nothing to delete");
        }
    }//END OF deleteRecordingNote

    private void saveRecordingNote() {
        String title = recTitle.getText().toString();
        String body = recNoteBody.getText().toString();
        if(TextUtils.isEmpty(title) && TextUtils.isEmpty(body) && !recordingExists) {
            Toast.makeText(this, "Empty recording cannot be saved!", Toast.LENGTH_SHORT).show();
            finish();
        }
        else if (TextUtils.isEmpty(title)){
            if (mediaRecorder != null){
                stopRecording();
            }
            recTitle.setError("");
        }
        else if(!TextUtils.isEmpty(title) && recordingExists) {
            final Note note = new Note(title, body, date, retrievedFilePath);

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (!intent.hasExtra("note_id")) {
                        appDb.notesDao().insertNote(note);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RecordingActivity.this, "Recording saved!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        note.setId(noteId);
                        appDb.notesDao().updateNote(note);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RecordingActivity.this, "Recording updated!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    finish();
                }
            });
        }
        else {
            final Note note = new Note(title, body, date, null);

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (!intent.hasExtra("note_id")) {
                        appDb.notesDao().insertNote(note);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RecordingActivity.this, "Recording saved!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        note.setId(noteId);
                        appDb.notesDao().updateNote(note);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RecordingActivity.this, "Recording updated!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    finish();
                }
            });
        }
    }//END OF saveRecordingNote

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete) {
            deleteRecordingNote();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaRecorder != null){
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }//END OF onStop


}//end of public class