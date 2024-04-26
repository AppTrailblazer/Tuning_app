#ifndef __eLehra__SamplingRateConverter__
#define __eLehra__SamplingRateConverter__

#include <stdio.h>

#define VSRC_BUFFER_LENGTH   30000//8192*3

class SamplingRateConverter {

public:
    SamplingRateConverter();

    int process(const short *inbuffer, unsigned int inBufferLen, short *outbuffer, float pitchCoeff);

    void reset();

private:
    float ibuffer[VSRC_BUFFER_LENGTH] = {0.0f};
    int ibuffer_write = 0;
    float ratio = 1.0;
    float sampleIndex = 0.0;
};

#endif
