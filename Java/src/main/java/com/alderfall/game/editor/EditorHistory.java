package com.alderfall.game.editor;

import java.util.ArrayDeque;

/** One snapshot per gesture, with bounded memory and a saved-content checkpoint. */
public final class EditorHistory {
    private final ArrayDeque<MapDocument> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
    private MapDocument before, saved;
    public EditorHistory(MapDocument initial) { saved = initial.copy(); }
    public void begin(MapDocument d) { if (before == null) before = d.copy(); }
    public boolean commit(MapDocument d) {
        if (before == null) return false;
        boolean changed = !before.sameContent(d);
        if (changed) { undo.addLast(before); redo.clear(); while (undo.size()>60) undo.removeFirst(); }
        before = null; return changed;
    }
    public MapDocument undo(MapDocument d) { commit(d); if (undo.isEmpty()) return d; redo.addLast(d.copy()); return undo.removeLast(); }
    public MapDocument redo(MapDocument d) { if (redo.isEmpty()) return d; undo.addLast(d.copy()); return redo.removeLast(); }
    public void saved(MapDocument d) { saved = d.copy(); }
    public boolean dirty(MapDocument d) { return !saved.sameContent(d); }
}
