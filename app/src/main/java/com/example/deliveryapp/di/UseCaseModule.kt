package com.example.deliveryapp.di

import com.example.deliveryapp.data.repository.ProductRepository
import com.example.deliveryapp.data.repository.OrderRepository
import com.example.deliveryapp.domain.usecase.GetProductsUseCase
import com.example.deliveryapp.domain.usecase.GetProductDetailUseCase
import com.example.deliveryapp.domain.usecase.GetOrderDetailUseCase
import com.example.deliveryapp.domain.usecase.PlaceOrderUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetProductsUseCase(repo: ProductRepository): GetProductsUseCase =
        GetProductsUseCase(repo)

    @Provides
    @Singleton
    fun provideGetProductDetailUseCase(repo: ProductRepository): GetProductDetailUseCase =
        GetProductDetailUseCase(repo)

    @Provides
    @Singleton
    fun provideGetOrderDetailUseCase(repo: OrderRepository): GetOrderDetailUseCase =
        GetOrderDetailUseCase(repo)

    @Provides
    @Singleton
    fun providePlaceOrderUseCase(repo: OrderRepository): PlaceOrderUseCase =
        PlaceOrderUseCase(repo)
}