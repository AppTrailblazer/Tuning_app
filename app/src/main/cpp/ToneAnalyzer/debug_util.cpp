#include <iostream>
#include <sstream>
#include "debug_util.h"

std::string array_to_string(const int *arr, int len) {
    std::ostringstream os;
    for (int i = 0; i < len; i++) {
        os << *(arr + i) << " ";
    }
    return os.str();
}

std::string array_to_string(const short *arr, int len) {
    std::ostringstream os;
    for (int i = 0; i < len; i++) {
        os << *(arr + i) << " ";
    }
    return os.str();
}

void log_array(const char *tag, const short *arr, int len) {
    std::ostringstream os;
    int part = 1;
    for (int i = 0; i < len; i++) {
        os << *(arr + i) << " ";

        if (os.str().length() > LOG_LIMIT) {
            LOGVT(tag, "Part %d: %s", part, os.str().c_str());
            part++;
            os.str(std::string());
        }
    }
    if (os.str().length() > 0) {
        if (part > 1) {
            LOGVT(tag, "Part %d: %s", part, os.str().c_str());
        } else {
            LOGVT(tag, "%s", os.str().c_str());
        }
        os.str(std::string());
    }
}

std::string array_to_string(const float *arr, int len) {
    std::ostringstream os;
    for (int i = 0; i < len; i++) {
        os << *(arr + i) << " ";
    }
    return os.str();
}

void log_array(const char *tag, const float *arr, int len) {
    std::ostringstream os;
    int part = 1;
    for (int i = 0; i < len; i++) {
        os << *(arr + i) << " ";

        if (os.str().length() > LOG_LIMIT) {
            LOGVT(tag, "Part %d: %s", part, os.str().c_str());
            part++;
            os.str(std::string());
        }
    }
    if (os.str().length() > 0) {
        if (part > 1) {
            LOGVT(tag, "Part %d: %s", part, os.str().c_str());
        } else {
            LOGVT(tag, "%s", os.str().c_str());
        }
        os.str(std::string());
    }
}

std::string array_to_string(const double *arr, int len) {
    std::ostringstream os;
    for (int i = 0; i < len; i++) {
        os << *(arr + i) << " ";
    }
    return os.str();
}

void log_array(const char *tag, const double *arr, int len) {
    std::ostringstream os;
    int part = 1;
    for (int i = 0; i < len; i++) {
        os << *(arr + i) << " ";

        if (os.str().length() > LOG_LIMIT) {
            LOGVT(tag, "Part %d: %s", part, os.str().c_str());
            part++;
            os.str(std::string());
        }
    }
    if (os.str().length() > 0) {
        if (part > 1) {
            LOGVT(tag, "Part %d: %s", part, os.str().c_str());
        } else {
            LOGVT(tag, "%s", os.str().c_str());
        }
        os.str(std::string());
    }
}