package com.example.uberv.reactiveretrofit;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyApp extends Application {
    public static final String LOG_TAG = MyApp.class.getSimpleName();

    private Retrofit mRetrofit;
    private GitHubService mGitHubService;
    private RxGitHubService mRxGitHubService;

    @Override
    public void onCreate() {
        super.onCreate();


        // setup cache
        File httpCacheDirectory = new File(getCacheDir(), "response");
        int cacheSize = 10 * 1024 * 1024;

        Cache cache = new Cache(httpCacheDirectory, cacheSize);

        // create client and set cache
        OkHttpClient client = new OkHttpClient.Builder()
//                .cache(cache)
                .addInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR)
                .build();

        mRetrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mGitHubService = mRetrofit.create(GitHubService.class);
        mRxGitHubService = mRetrofit.create(RxGitHubService.class);
    }

    public Retrofit getRetrofit() {
        return mRetrofit;
    }

    public GitHubService getGitHubService() {
        return mGitHubService;
    }

    public RxGitHubService getRxGitHubService() {
        return mRxGitHubService;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private final Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new Interceptor() {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            if (isNetworkAvailable()) {
                Log.d(LOG_TAG, "Getting new version");
                int maxAge = 60; // read from cache for 1 minute
                return originalResponse.newBuilder()
                        .header("Cache-Control", "public, max-age=" + maxAge)
                        .build();
            } else {
                Log.d(LOG_TAG, "No network, getting cached version");
                int maxStale = 60 * 60 * 3; // tolerate 3 hours stale
                return originalResponse.newBuilder()
                        .header("cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                        .build();
            }
        }
    };
}
