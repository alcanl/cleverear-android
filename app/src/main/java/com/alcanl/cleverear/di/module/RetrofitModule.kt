package com.alcanl.cleverear.di.module

import android.content.Context
import com.alcanl.cleverear.R
import com.alcanl.cleverear.api.API_BASE_URL
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import javax.inject.Singleton
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {

    @Provides
    @Singleton
    fun create(@ApplicationContext context: Context) : Retrofit
    {
        val sslSocketFactory = getCustomSSLSocketFactory(context)

        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
            .client(OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, getX509TrustManager())
                .hostnameVerifier { _, _ -> true }
                .build())
            .build()
    }

    private fun getX509TrustManager(): X509TrustManager
    {
        val trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(null as KeyStore?) }
            .trustManagers

        return trustManagers.filterIsInstance<X509TrustManager>().first()
    }


    private fun getCustomSSLSocketFactory(context: Context): SSLSocketFactory
    {
        val keyStore = KeyStore.getInstance("PKCS12")
        val certificateInputStream = context.resources.openRawResource(R.raw.ear_keystore)

        keyStore.load(certificateInputStream, "ear**123".toCharArray())
        certificateInputStream.close()

        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, "ear**123".toCharArray())

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, null, null)
        return sslContext.socketFactory
    }

}