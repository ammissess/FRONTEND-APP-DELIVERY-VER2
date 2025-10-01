package com.example.deliveryapp.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RawAuthApi

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NormalAuthApi