package com.aichathub.di

import com.aichathub.data.repository.*
import com.aichathub.domain.repository.*
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
    abstract fun bindAIServiceRepository(impl: AIServiceRepositoryImpl): AIServiceRepository

    @Binds
    @Singleton
    abstract fun bindAPIKeyRepository(impl: APIKeyRepositoryImpl): APIKeyRepository

    @Binds
    @Singleton
    abstract fun bindChatSessionRepository(impl: ChatSessionRepositoryImpl): ChatSessionRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindCustomProviderRepository(impl: CustomProviderRepositoryImpl): CustomProviderRepository

    @Binds
    @Singleton
    abstract fun bindWorkspaceRepository(impl: WorkspaceRepositoryImpl): WorkspaceRepository

    @Binds
    @Singleton
    abstract fun bindTerminalLogRepository(impl: TerminalLogRepositoryImpl): TerminalLogRepository
}
