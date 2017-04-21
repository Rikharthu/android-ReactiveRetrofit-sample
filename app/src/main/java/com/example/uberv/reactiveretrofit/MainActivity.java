package com.example.uberv.reactiveretrofit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.adapter.rxjava2.Result;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private GitHubService mGitHubService;
    private RxGitHubService mRxGitHubService;

    private ListView mReposList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mReposList = (ListView) findViewById(R.id.repos_list);

        mGitHubService = ((MyApp) getApplication()).getGitHubService();
        mRxGitHubService = ((MyApp) getApplication()).getRxGitHubService();


    }

    @Override
    protected void onResume() {
        super.onResume();

//        fetchRepos();
        fetchReposRx();
    }

    private void fetchReposRx() {
        mRxGitHubService.listRepos("rikharthu")
                .flatMap(this.<List<Repo>>apiResponseFunction())
                .map(new Function<List<Repo>, List<Repo>>() {
                    @Override
                    public List<Repo> apply(@NonNull List<Repo> repos) throws Exception {
                        // filter data
                        Iterator<Repo> i = repos.iterator();
                        while (i.hasNext()) {
                            Repo repo = i.next();
                            // only show android-related repos
                            if (!repo.getName().toLowerCase().contains("android".toLowerCase())) {
                                i.remove();
                            }
                        }
                        return repos;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<List<Repo>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(LOG_TAG, "onSubscribe");
                    }

                    @Override
                    public void onNext(final List<Repo> repos) {
                        Log.d(LOG_TAG, "onNext: " + repos);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showRepos(repos);
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(LOG_TAG, "onError: " + e);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(LOG_TAG, "onComplete");
                    }
                });
    }

    /**
     * Create an observable function that takes retrofit result and emits an observable of
     * some generic type {@link T}. Function validates that retrofit call was a success
     *
     * @param <T> any generic type return from retrofit response.
     * @return
     */
    private <T> Function<Result<T>, Observable<T>> apiResponseFunction() {
        return new Function<Result<T>, Observable<T>>() {
            @Override
            public Observable<T> apply(@NonNull Result<T> result) throws Exception {
                if (result.isError()) {
                    return Observable.error(result.error());
                } else {
                    return Observable.just(result.response().body());
                }
            }
        };
    }

    private void fetchRepos() {
        Call<List<Repo>> repos = mGitHubService.listRepos("rikharthu");

        repos.enqueue(new Callback<List<Repo>>() {
            @Override
            public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {
                showRepos(response.body());
            }

            @Override
            public void onFailure(Call<List<Repo>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRepos(List<Repo> repos) {
        if (repos != null && repos.size() > 0) {
            ArrayList<Map<String, String>> data = new ArrayList<>();
            Map<String, String> m;
            for (Repo repo : repos) {
                m = new HashMap<>();
                m.put("name", repo.getName());
                m.put("owner", repo.getOwner().getLogin());
                m.put("url", repo.getUrl());
                data.add(m);
            }
            String[] from = {"name", "owner", "url"};
            int[] to = {R.id.repo_item_name_tv, R.id.repo_item_owner_tv, R.id.repo_item_url_tv};

            SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, data, R.layout.repo_item, from, to);
            mReposList.setAdapter(adapter);
        }
    }
}
