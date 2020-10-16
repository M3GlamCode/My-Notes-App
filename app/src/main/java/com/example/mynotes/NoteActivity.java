package com.example.mynotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.mynotes.Database.AppDatabase;
import com.example.mynotes.Database.AppExecutors;
import com.example.mynotes.Models.Note;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NoteActivity extends AppCompatActivity {
    private static final String LOG_TAG = NoteActivity.class.getSimpleName();
    private EditText title, body;
    private String date;
    private Intent intent;
    private AppDatabase appDb;
    int noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        //DebugDB.getAddressLog();
        initViews();

        appDb = AppDatabase.getInstance(getApplicationContext());
        intent = getIntent();

        if (intent != null && intent.hasExtra("note_id")) {
            noteId = intent.getIntExtra("note_id", -1);

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    Note note = appDb.notesDao().loadNoteById(noteId);
                    populateUI(note);
                }
            });
        }
    }//end of onCreate

    private void initViews() {
        title = findViewById(R.id.note_title);
        body = findViewById(R.id.note_body);
        FloatingActionButton saveNoteBtn = findViewById(R.id.save_note_btn);
        saveNoteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAllInfo();
            }
        });
    }//END OF initViews

    private void populateUI(Note note) {
        if (note == null) {
            return;
        }
        title.setText(note.getTitle());
        body.setText(note.getBody());
    }//END OF populateUI

    private void saveAllInfo() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        date = dateFormat.format(calendar.getTime());

        String titleString = title.getText().toString();
        String bodyString = body.getText().toString();

        if(TextUtils.isEmpty(titleString) && TextUtils.isEmpty(bodyString)) {
            Toast.makeText(this, "Empty note cannot be saved!", Toast.LENGTH_SHORT).show();
            finish();

        }else if (TextUtils.isEmpty(titleString)){
            title.setError("");
        }
        else {
            final Note note = new Note(
                    titleString,
                    bodyString,
                    date, null);

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    if (!intent.hasExtra("note_id")) {
                        appDb.notesDao().insertNote(note);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(NoteActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        note.setId(noteId);
                        appDb.notesDao().updateNote(note);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(NoteActivity.this, "Note updated!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    finish();
                }
            });
        }
    }//END OF saveAllInfo

    private void deleteNote() {
        String titleString = title.getText().toString();
        String bodyString = body.getText().toString();
        if (intent != null && intent.hasExtra("note_id")) {
            final Note note = new Note(
                    noteId,
                    titleString,
                    bodyString,
                    date, null);

            AppExecutors.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    appDb.notesDao().deleteNote(note);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(NoteActivity.this, "Note deleted", Toast.LENGTH_SHORT).show();
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
    }//END OF deleteNote

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete) {
            deleteNote();
        }
        return super.onOptionsItemSelected(item);
    }



}//end of public class
