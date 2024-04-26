package com.willeypianotuning.toneanalyzer.di

import android.content.Context
import com.willeypianotuning.toneanalyzer.billing.BillingClientFactory
import com.willeypianotuning.toneanalyzer.billing.DefaultBillingClientFactory
import com.willeypianotuning.toneanalyzer.billing.PurchaseStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    @Provides
    @Singleton
    fun provideBillingClientFactory(@ApplicationContext context: Context): BillingClientFactory {
        return DefaultBillingClientFactory(context)
    }

    @Provides
    @Singleton
    fun providePurchaseStore(@ApplicationContext context: Context): PurchaseStore {
        return PurchaseStore.getInstance(context)
    }
}