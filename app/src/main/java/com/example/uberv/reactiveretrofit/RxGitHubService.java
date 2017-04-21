package com.example.uberv.reactiveretrofit;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.adapter.rxjava2.Result;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface RxGitHubService {

    @GET("users/{user}/repos")
    Observable<Result<List<Repo>>> listRepos(@Path("user") String user);
}
