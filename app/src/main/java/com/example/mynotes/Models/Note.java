package com.example.mynotes.Models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String body;
    private String date;
    private String recording;

    public Note(int id, String title, String body, String date, String recording) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.date = date;
        this.recording = recording;
    }

    @Ignore
    public Note(String title, String body, String date) {
        this.title = title;
        this.body = body;
        this.date = date;
    }

    @Ignore
    public Note(String title, String body, String date, String recording) {
        this.title = title;
        this.body = body;
        this.date = date;
        this.recording = recording;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getDate() {
        return date;
    }

    public String getRecording() {
        return recording;
    }
}
