#include "SamplingRateConverter.h"
#include <math.h>
#include <string.h>

SamplingRateConverter::SamplingRateConverter() {
    reset();
}

int SamplingRateConverter::process(const short *inbuffer, unsigned int inBufferLen, short *outbuffer,
                                   float pitchCoeff) {
    ratio = pitchCoeff;

    for (int k = 0; k < inBufferLen; k++) {
        ibuffer[ibuffer_write++] = (float) inbuffer[k];
        if (ibuffer_write >= VSRC_BUFFER_LENGTH) {
            ibuffer_write = 0;
        }
    }

    float samplesAvailableF = (float) ibuffer_write - sampleIndex - 7.0f;
    if (samplesAvailableF < 0) {
        samplesAvailableF += VSRC_BUFFER_LENGTH;
    }
    samplesAvailableF *= ratio;
    int samplesAvailable = (int) floor(samplesAvailableF);

    double invRatio = 1.0 / ratio;
    const long offset = 2;
    double rem = 0.0;
    double qbuffer[6];

    for (int i = 0; i < samplesAvailable; i++) {
        int index = (int) floor(sampleIndex);
        rem = sampleIndex - (double) index;
        if (index >= VSRC_BUFFER_LENGTH)
            index = 0;
        sampleIndex = (double) index + rem;

        for (int k = 0; k < 6; k++) {
            if (index >= VSRC_BUFFER_LENGTH)
                index = 0;
            qbuffer[k] = ibuffer[index++];
        }

        double z = rem - 0.5;
        double even1 = qbuffer[offset + 1] + qbuffer[offset + 0], odd1 =
                qbuffer[offset + 1] - qbuffer[offset + 0];
        double even2 = qbuffer[offset + 2] + qbuffer[offset - 1], odd2 =
                qbuffer[offset + 2] - qbuffer[offset - 1];
        double even3 = qbuffer[offset + 3] + qbuffer[offset - 2], odd3 =
                qbuffer[offset + 3] - qbuffer[offset - 2];

        double c0 = even1 * 0.42685983409379380 + even2 * 0.07238123511170030
                    + even3 * 0.00075893079450573;
        double c1 = odd1 * 0.35831772348893259 + odd2 * 0.20451644554758297
                    + odd3 * 0.00562658797241955;
        double c2 = even1 * -0.217009177221292431 + even2 * 0.20051376594086157
                    + even3 * 0.01649541128040211;
        double c3 = odd1 * -0.25112715343740988 + odd2 * 0.04223025992200458
                    + odd3 * 0.02488727472995134;
        double c4 = even1 * 0.04166946673533273 + even2 * -0.06250420114356986
                    + even3 * 0.02083473440841799;
        double c5 = odd1 * 0.08349799235675044 + odd2 * -0.04174912841630993
                    + odd3 * 0.00834987866042734;

        outbuffer[i] = (short) round(((((c5 * z + c4) * z + c3) * z + c2) * z + c1) * z + c0);

        sampleIndex += invRatio;
    }

    return samplesAvailable;
}

void SamplingRateConverter::reset() {
    memset(ibuffer, 0, VSRC_BUFFER_LENGTH * sizeof(float));
    ibuffer_write = 0;
    ratio = 1.0f;
    sampleIndex = 0.0;
}
