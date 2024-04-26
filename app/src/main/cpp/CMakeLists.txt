cmake_minimum_required(VERSION 3.22)

project(ToneAnalyzer)

set(CMAKE_C_FLAGS_DEBUG "-O0")
set(CMAKE_C_FLAGS_RELEASE "-O2")
set(CMAKE_CXX_FLAGS_DEBUG "-O0")
set(CMAKE_CXX_FLAGS_RELEASE "-O2")

add_subdirectory(KissFFT)

include_directories(ToneAnalyzer)
include_directories(KissFFT)

file(GLOB ToneAnalyzerSources CONFIGURE_DEPENDS "ToneAnalyzer/*.cpp" "ToneAnalyzer/filters/*.cpp")
file(GLOB JniSources CONFIGURE_DEPENDS "jni/*.cpp")

add_library(WilleyToneAnalyzerLib
        SHARED
        ${ToneAnalyzerSources}
        ${JniSources}
        )

target_compile_features(WilleyToneAnalyzerLib PUBLIC cxx_std_17)

if (ANDROID)
    message("Building for Android")
    set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -DUSE_ANDROID=1 -DUSE_NSC=1")
    set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -DUSE_ANDROID=1 -DUSE_NSC=1")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -DUSE_ANDROID=1 -DUSE_NSC=1")
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -DUSE_ANDROID=1 -DUSE_NSC=1")

    find_library(log-lib log)
    target_link_libraries(WilleyToneAnalyzerLib
            ${log-lib}
            KissFFT
            )
endif (ANDROID)

if (APPLE OR (UNIX AND NOT ANDROID) OR WIN32)
    message("Building for Desktop")
    set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -DUSE_NSC=1")
    set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -DUSE_NSC=1")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -DUSE_NSC=1")
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -DUSE_NSC=1")

    # We are only interested in finding jni.h: we do not care about extended JVM
    # functionality or the AWT library.
    set(JAVA_AWT_LIBRARY NotNeeded)
    set(JAVA_JVM_LIBRARY NotNeeded)
    set(JAVA_AWT_INCLUDE_PATH NotNeeded)
    include(FindJNI)
    include(CheckCCompilerFlag)
    include_directories(${JNI_INCLUDE_DIRS})

    target_link_libraries(WilleyToneAnalyzerLib
            KissFFT
            )
endif (APPLE OR (UNIX AND NOT ANDROID) OR WIN32)

