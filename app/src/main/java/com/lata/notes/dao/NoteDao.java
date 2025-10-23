package com.lata.notes.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.lata.notes.entities.Note;

import java.util.List;
@Dao
public interface NoteDao {
    //@Query("SELECT * FROM notes ORDER BY last_updated DESC")
    // List<Note> getAllNotes();

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, id DESC")
    List<Note> getAllNotesOrdered();

    @Query("UPDATE notes SET isPinned = :pinned WHERE id IN (:noteIds)")
    void updatePinnedStatus(List<Integer> noteIds, boolean pinned);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNote(Note note);

    @Update
    void updateNote(Note note);

    @Delete void deleteNote(Note note);
}
