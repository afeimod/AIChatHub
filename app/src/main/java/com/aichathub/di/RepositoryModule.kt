package com.aichathub.di

import com.aichathub.data.repository.AIServiceRepositoryImpl
import com.aichathub.data.repository.APIKeyRepositoryImpl
import com.aichathub.data.repository.ChatSessionRepositoryImpl
import com.aichathub.data.repository.SettingsRepositoryImpl
import com.aichathub.domain.repository.AIServiceRepository
import com.aichathub.domain.repository.APIKeyRepository
import com.aichathub.domain.repository.ChatSessionRepository
import com.aichathub.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAIServiceRepository(
        impl: AIServiceRepositoryImpl
    ): AIServiceRepository

    @Binds
    @Singleton
    abstract fun bindAPIKeyRepository(
        impl: APIKeyRepositoryImpl
    ): APIKeyRepository

    @Binds
    @Singleton
    abstract fun bindChatSessionRepository(
        impl: ChatSessionRepositoryImpl
    ): ChatSessionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}