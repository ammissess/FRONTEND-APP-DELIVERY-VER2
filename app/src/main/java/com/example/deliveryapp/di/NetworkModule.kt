package com.example.deliveryapp.di

import android.content.Context
import com.example.deliveryapp.data.local.DataStoreManager
import com.example.deliveryapp.data.remote.ApiClient
import com.example.deliveryapp.data.remote.api.AuthApi
import com.example.deliveryapp.data.remote.api.GeocodingApi
import com.example.deliveryapp.data.remote.api.OrderApi
import com.example.deliveryapp.data.remote.api.ProductApi
import com.example.deliveryapp.data.remote.interceptor.AuthInterceptor
import com.example.deliveryapp.data.repository.AuthRepository
import com.example.deliveryapp.data.repository.OrderRepository
import com.example.deliveryapp.data.repository.ProductRepository
import com.example.deliveryapp.ui.session.SessionViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // AuthApi "raw" (không interceptor) chỉ phục vụ cho refresh token
    @Provides
    @Singleton
    @RawAuthApi
    fun provideRawAuthApi(): AuthApi = ApiClient.create().create(AuthApi::class.java)

    // DataStoreManager
    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager =
        DataStoreManager(context)

    // Interceptor, cần RawAuthApi để gọi refresh (tránh vòng lặp)
    @Provides
    @Singleton
    fun provideAuthInterceptor(
        dataStore: DataStoreManager,
        @RawAuthApi rawAuthApi: AuthApi
    ): Interceptor = AuthInterceptor(dataStore, rawAuthApi)

    @Provides
    @Singleton
    fun provideRetrofit(interceptor: Interceptor): Retrofit = ApiClient.create(interceptor)

    @Provides
    @Singleton
    fun provideProductApi(retrofit: Retrofit): ProductApi = retrofit.create(ProductApi::class.java)

    @Provides
    @Singleton
    fun provideOrderApi(retrofit: Retrofit): OrderApi = retrofit.create(OrderApi::class.java)

    // Normal AuthApi (có interceptor) dành cho Repository
    @Provides
    @Singleton
    @NormalAuthApi
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProductRepository(api: ProductApi): ProductRepository = ProductRepository(api)

    @Provides
    @Singleton
    fun provideOrderRepository(
        api: OrderApi,
        dataStore: DataStoreManager
    ): OrderRepository = OrderRepository(api, dataStore)

    @Provides
    @Singleton
    fun provideAuthRepository(
        @NormalAuthApi api: AuthApi,
        dataStore: DataStoreManager
    ): AuthRepository = AuthRepository(api, dataStore)

    // Thêm vào NetworkModule cho mapbox
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "DeliveryApp/1.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideGeocodingApi(okHttpClient: OkHttpClient): GeocodingApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(GeocodingApi::class.java)
    }
    @Provides
    @Singleton
    fun provideSessionViewModel(
        dataStore: DataStoreManager,
        authRepository: AuthRepository,
        @NormalAuthApi authApi: AuthApi
    ): SessionViewModel = SessionViewModel(dataStore, authRepository, authApi)
}