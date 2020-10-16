package com.example.mynotes.Database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mynotes.Models.Note;

import java.util.List;

@Dao
public interface NotesDao {
    @Query("SELECT * FROM NOTES ORDER BY ID")
    List<Note> loadAllNotes();

    @Insert
    void insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Delete
    void deleteNote(Note note);

    @Query("SELECT * FROM NOTES WHERE id = :id")
    Note loadNoteById(int id);

    @Delete
    void deleteAllNotes(List<Note> notes);
}
