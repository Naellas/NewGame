package com.alderfall.game.editor;

import com.alderfall.game.WorldProp;

/** Carries movement across tile boundaries while retaining sub-tile precision. */
final class EditorPropPosition {
    static WorldProp shift(WorldProp prop,int dx,int dy){
        return prop.shifted(dx,dy);
    }
}
