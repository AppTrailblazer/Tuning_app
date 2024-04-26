#ifndef PIANOMETER_ANDROID_TUNINGPARTIALS_H
#define PIANOMETER_ANDROID_TUNINGPARTIALS_H

#include "ToneDetectorConstants.h"

class TuningPartials {
public:
    TuningPartials();
    int get(int partial, int noteZeroIndexed);
private:
    int tuningPartialsForNotes[MAX_PARTIALS][NOTES_ON_PIANO] = {0};
};


#endif //PIANOMETER_ANDROID_TUNINGPARTIALS_H
