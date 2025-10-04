package com.lata.notes.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.lata.notes.entities.Note;

import java.util.List;
@Dao
public interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY last_updated DESC") List<Note> getAllNotes();
    @Insert(onConflict = OnConflictStrategy.REPLACE) void insertNote(Note note);
    @Delete void deleteNote(Note note);
}
