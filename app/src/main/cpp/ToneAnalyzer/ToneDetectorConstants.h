#ifndef PIANOTUNING_TONEDETECTORCONSTANTS_H
#define PIANOTUNING_TONEDETECTORCONSTANTS_H

#include <array>

#define NOTES_ON_PIANO 88
#define TEMPERAMENT_SIZE 12
#define MAX_PARTIALS 5
#define BXFIT_SIZE 4
#define MAX_PEAKS 64

#define PI 3.141592653589793116

// arr[i] = round(1 + 8 * (pow(2, -(i+1) * 4.5 / 88))
constexpr static std::array<int, NOTES_ON_PIANO> MIN_PEAKS_REQUIRED_FOR_NOTE_ANALYSIS{
        9, 8, 8, 8, 8, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 5, 5, 5, 5, 5, 5, 5, 4, 4, 4, 4, 4, 4,
        4, 4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
};

constexpr static std::array<double, NOTES_ON_PIANO> B_LOWER_LIMIT{
        8.0096026e-05, 7.1866045e-05, 6.4621228e-05, 5.8263180e-05,
        5.2697302e-05, 4.7827878e-05, 4.3583572e-05, 3.9882078e-05,
        3.6664074e-05, 3.3874941e-05, 3.1462252e-05, 2.9390008e-05,
        2.7620839e-05, 2.6128275e-05, 2.4888301e-05, 2.3877725e-05,
        2.3087838e-05, 2.2504730e-05, 2.6759246e-05, 2.6261259e-05,
        2.6042975e-05, 2.6123904e-05, 2.6486490e-05, 2.7112050e-05,
        2.8051880e-05, 2.9286157e-05, 3.0836127e-05, 3.2687254e-05,
        3.4856719e-05, 3.7351609e-05, 4.0176488e-05, 4.3339140e-05,
        4.6847726e-05, 5.0711616e-05, 5.4954176e-05, 5.9589107e-05,
        6.4669199e-05, 7.0194255e-05, 7.6244898e-05, 8.2858649e-05,
        9.0091853e-05, 9.8020748e-05, 0.00010671420, 0.00011624670,
        0.00012671365, 0.00013818702, 0.00015079112, 0.00016462762,
        0.00017982203, 0.00019647642, 0.00021476681, 0.00023479846,
        0.00025678621, 0.00028087434, 0.00030725318, 0.00033614878,
        0.00036777544, 0.00040237894, 0.00044022314, 0.00048159334,
        0.00052679738, 0.00057616842, 0.00063006533, 0.00068887544,
        0.00075301621, 0.00082293706, 0.00089912192, 0.00098209165,
        0.0010724058, 0.0011706664, 0.0012775195, 0.0013936593, 0.0015198316,
        0.0016568360, 0.0018055314, 0.0019668397, 0.0021417497, 0.0023313223,
        0.0025366973, 0.0027590971, 0.0029998354, 0.0032603217, 0.0035420712,
        0.0038467115, 0.0041759931, 0.0045317980, 0.0049161511, 0.0053312308
};

constexpr static std::array<double, NOTES_ON_PIANO> B_UPPER_LIMIT{
        0.0023966467, 0.0022533878, 0.0021228713, 0.0020030602, 0.0018923966,
        0.0017897855, 0.0016941965, 0.0016050555, 0.0015217579, 0.0014438552,
        0.0013710610, 0.0013030269, 0.0012395633, 0.0011804429, 0.0011255051,
        0.0010746921, 0.0010278103, 0.00098483765, 0.00089920440,
        0.00086442527, 0.00083357398, 0.00080630265, 0.00078257330,
        0.00076252373, 0.00074592227, 0.00073307188, 0.00072406343,
        0.00071926170, 0.00071884907, 0.00072307914, 0.00073224073,
        0.00074659457, 0.00076641014, 0.00079197576, 0.00082348136,
        0.00086127210, 0.00090540195, 0.00095649064, 0.0010145219,
        0.0010799656, 0.0011532299, 0.0012346654, 0.0013248046, 0.0014242565,
        0.0015335914, 0.0016537115, 0.0017852476, 0.0019291808, 0.0020864799,
        0.0022584854, 0.0024461704, 0.0026512835, 0.0028749751, 0.0031192007,
        0.0033858554, 0.0036768855, 0.0039946656, 0.0043417159, 0.0047208313,
        0.0051351022, 0.0055879382, 0.0060831052, 0.0066247708, 0.0072175376,
        0.0078664990, 0.0085772891, 0.0093561457, 0.010209974, 0.011146424,
        0.012173975, 0.013302022, 0.014540983, 0.015902413, 0.017399136,
        0.019045373, 0.020856917, 0.022851292, 0.025047969, 0.027468560,
        0.030137077, 0.033080190, 0.036327545, 0.039912067, 0.043870360,
        0.048243091, 0.053075485, 0.058417797, 0.064325906
};

#endif //PIANOTUNING_TONEDETECTORCONSTANTS_H
