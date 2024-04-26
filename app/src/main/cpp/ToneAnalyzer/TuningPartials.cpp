#include "TuningPartials.h"
#include "debug_util.h"

TuningPartials::TuningPartials() {
    for (int i = 0; i < NOTES_ON_PIANO; i++) {
        if (i < 6) {
            tuningPartialsForNotes[0][i] = 4;
            tuningPartialsForNotes[1][i] = 5;
            tuningPartialsForNotes[2][i] = 6;
            tuningPartialsForNotes[3][i] = 8;
            tuningPartialsForNotes[4][i] = 10;
        } else if (i < 12) {
            tuningPartialsForNotes[0][i] = 3;
            tuningPartialsForNotes[1][i] = 4;
            tuningPartialsForNotes[2][i] = 5;
            tuningPartialsForNotes[3][i] = 6;
            tuningPartialsForNotes[4][i] = 8;
        } else if (i < 24) {
            tuningPartialsForNotes[0][i] = 2;
            tuningPartialsForNotes[1][i] = 3;
            tuningPartialsForNotes[2][i] = 4;
            tuningPartialsForNotes[3][i] = 5;
            tuningPartialsForNotes[4][i] = 6;
        } else if (i < 33) {
            tuningPartialsForNotes[0][i] = 2;
            tuningPartialsForNotes[1][i] = 3;
            tuningPartialsForNotes[2][i] = 4;
            tuningPartialsForNotes[3][i] = 5;
            tuningPartialsForNotes[4][i] = 1;
        } else if (i < 48) {
            tuningPartialsForNotes[0][i] = 1;
            tuningPartialsForNotes[1][i] = 2;
            tuningPartialsForNotes[2][i] = 3;
            tuningPartialsForNotes[3][i] = 4;
        } else if (i < 61) {
            tuningPartialsForNotes[0][i] = 1;
            tuningPartialsForNotes[1][i] = 2;
            tuningPartialsForNotes[2][i] = 3;
        } else if (i < 73) {
            tuningPartialsForNotes[0][i] = 1;
            tuningPartialsForNotes[1][i] = 2;
            tuningPartialsForNotes[2][i] = 0;
        } else {
            tuningPartialsForNotes[0][i] = 1;
            tuningPartialsForNotes[1][i] = 0;
            tuningPartialsForNotes[2][i] = 0;
        }
    }
}

int TuningPartials::get(int partial, int noteZeroIndexed) {
    ASSERT_RANGE(noteZeroIndexed, 0, NOTES_ON_PIANO - 1);
    return tuningPartialsForNotes[partial][noteZeroIndexed];
}
