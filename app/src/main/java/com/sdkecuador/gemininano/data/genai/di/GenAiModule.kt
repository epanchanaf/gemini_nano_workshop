package com.sdkecuador.gemininano.data.genai.di

import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.client.MlKitGenAiClientFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class GenAiModule {
    @Binds
    abstract fun bindGenAiClientFactory(factory: MlKitGenAiClientFactory): GenAiClientFactory
}
