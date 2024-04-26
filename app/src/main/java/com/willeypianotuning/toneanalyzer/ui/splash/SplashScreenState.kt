package com.willeypianotuning.toneanalyzer.ui.splash

sealed class SplashScreenState {
    object None : SplashScreenState()
    object Running : SplashScreenState()
    class Error(val error: Throwable) : SplashScreenState()
    object Success : SplashScreenState()
}
