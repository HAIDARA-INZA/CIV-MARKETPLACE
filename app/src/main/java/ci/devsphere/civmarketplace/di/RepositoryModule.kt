package ci.devsphere.civmarketplace.di

import ci.devsphere.civmarketplace.data.repository.AuthRepositoryImpl
import ci.devsphere.civmarketplace.data.repository.AppVersionRepositoryImpl
import ci.devsphere.civmarketplace.data.repository.ChatRepositoryImpl
import ci.devsphere.civmarketplace.data.repository.OrderRepositoryImpl
import ci.devsphere.civmarketplace.data.repository.PaymentRepositoryImpl
import ci.devsphere.civmarketplace.data.repository.ProductRepositoryImpl
import ci.devsphere.civmarketplace.data.repository.ReviewRepositoryImpl
import ci.devsphere.civmarketplace.data.repository.UserRepositoryImpl
import ci.devsphere.civmarketplace.domain.repository.AuthRepository
import ci.devsphere.civmarketplace.domain.repository.AppVersionRepository
import ci.devsphere.civmarketplace.domain.repository.ChatRepository
import ci.devsphere.civmarketplace.domain.repository.OrderRepository
import ci.devsphere.civmarketplace.domain.repository.PaymentRepository
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import ci.devsphere.civmarketplace.domain.repository.ReviewRepository
import ci.devsphere.civmarketplace.domain.repository.UserRepository
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
    abstract fun bindAppVersionRepository(
        appVersionRepositoryImpl: AppVersionRepositoryImpl
    ): AppVersionRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        paymentRepositoryImpl: PaymentRepositoryImpl
    ): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        orderRepositoryImpl: OrderRepositoryImpl
    ): OrderRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        reviewRepositoryImpl: ReviewRepositoryImpl
    ): ReviewRepository
}

