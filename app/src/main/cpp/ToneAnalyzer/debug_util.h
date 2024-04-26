#ifndef PIANOTUNING_DEBUG_UTIL_H
#define PIANOTUNING_DEBUG_UTIL_H

#include <iostream>
#include <sstream>

#ifdef NDEBUG
// disables logs in release mode
#define LOG_LIMIT 10000000
#define LOGV(...)
#define LOGD(...)
#define LOGI(...)
#define LOGW(...)
#define LOGVT(TAG, ...)
#define LOGDT(TAG, ...)
#define LOGIT(TAG, ...)
#define LOGWT(TAG, ...)
#define ASSERT_RANGE(N, MIN, MAX)
#else
#define LOG_TAG "NativeToneDetector"
#define ASSERT_RANGE(N, MIN, MAX) if (N < MIN || N > MAX) throw std::invalid_argument("Wrong argument range used. " + std::to_string(N) + " should be in range [" + std::to_string(MIN) + ";" + std::to_string(MAX) + "].")

#ifdef USE_ANDROID

#include <android/log.h>

#define LOG_LIMIT 2000
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGVT(TAG, ...) __android_log_print(ANDROID_LOG_VERBOSE,TAG,__VA_ARGS__)
#define LOGDT(TAG, ...) __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__)
#define LOGIT(TAG, ...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define LOGWT(TAG, ...) __android_log_print(ANDROID_LOG_WARN,TAG,__VA_ARGS__)
#else
#define LOG_LIMIT 10000000
#define LOGV(...) printf("%s ", LOG_TAG); printf(__VA_ARGS__); printf("\n")
#define LOGD(...) printf("%s ", LOG_TAG); printf(__VA_ARGS__); printf("\n")
#define LOGI(...) printf("%s ", LOG_TAG); printf(__VA_ARGS__); printf("\n")
#define LOGW(...) printf("%s ", LOG_TAG); printf(__VA_ARGS__); printf("\n")
#define LOGVT(TAG, ...) printf("%s ", TAG); printf(__VA_ARGS__); printf("\n")
#define LOGDT(TAG, ...) printf("%s ", TAG); printf(__VA_ARGS__); printf("\n")
#define LOGIT(TAG, ...) printf("%s ", TAG); printf(__VA_ARGS__); printf("\n")
#define LOGWT(TAG, ...) printf("%s ", TAG); printf(__VA_ARGS__); printf("\n")
#endif
#endif

std::string array_to_string(const int *arr, int len);

std::string array_to_string(const short *arr, int len);

void log_array(const char *tag, const short *arr, int len);

std::string array_to_string(const float *arr, int len);

void log_array(const char *tag, const float *arr, int len);

std::string array_to_string(const double *arr, int len);

void log_array(const char *tag, const double *arr, int len);

#endif //PIANOTUNING_DEBUG_UTIL_H
