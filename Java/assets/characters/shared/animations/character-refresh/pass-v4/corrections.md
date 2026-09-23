# Reviewed corrections

The first multi-direction knight walking atlas was rejected: its grid and stance
sequence did not meet the specification. The accepted walking format is 4x4
side-facing poses. The knight combat atlas received a flat-magenta background
correction, preserving its 24 poses and one-handed sword/shield support.

Lyra, Rafiq and Vesper received this exact built-in imagegen correction prompt:

> Correct ONLY the inconsistent facing directions in this 16-pose walking atlas. EXACT same character, identity, colors, clothing, pixel art, 4 columns x4 rows, same pose order and body size. ALL SIXTEEN figures must face SCREEN RIGHT in strict side profile: nose points right, toes point right, hair/cape trail left. The lower two rows wrongly turn to face LEFT: redraw those poses facing RIGHT while preserving their opposite-leg contact and knee-lift phases. 'Opposite leg' means the other leg leads, NEVER reversing facing. First two rows remain unchanged. The full16 poses are ONE walk cycle in ONE direction, not two directions. Maintain held gear and costume sides. Transparent background, no shadows, text, grid lines or colored fringe.

All three corrected atlases were inspected before replacing the candidate sources.
Ranger and Calder combat exports included detached grid/number annotations; the
importer removes disconnected non-character components when packing runtime cells.
