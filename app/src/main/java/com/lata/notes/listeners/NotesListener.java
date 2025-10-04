package com.lata.notes.listeners;

import com.lata.notes.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
    void onNoteLongClicked(Note note);
}
