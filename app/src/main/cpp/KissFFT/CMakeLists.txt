cmake_minimum_required(VERSION 3.22)

set(CMAKE_C_FLAGS_DEBUG "-O2")
set(CMAKE_C_FLAGS_RELEASE "-O2")

add_library(KissFFT
    STATIC
        kiss_fft.c
        kiss_fftr.c)
