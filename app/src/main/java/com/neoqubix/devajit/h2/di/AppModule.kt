package com.neoqubix.devajit.h2.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.neoqubix.devajit.h2.data.repository.AuthRepositoryImpl
import com.neoqubix.devajit.h2.data.repository.CartRepositoryImpl
import com.neoqubix.devajit.h2.data.repository.TransactionRepositoryImpl
import com.neoqubix.devajit.h2.data.repository.UserRepositoryImpl
import com.neoqubix.devajit.h2.data.update.GitHubUpdateRepository
import com.neoqubix.devajit.h2.domain.repository.AuthRepository
import com.neoqubix.devajit.h2.domain.repository.CartRepository
import com.neoqubix.devajit.h2.domain.repository.TransactionRepository
import com.neoqubix.devajit.h2.domain.repository.UpdateRepository
import com.neoqubix.devajit.h2.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    // The persistent offline cache is on by default, so reads work and writes queue without internet
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindCartRepository(impl: CartRepositoryImpl): CartRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(impl: GitHubUpdateRepository): UpdateRepository
}
