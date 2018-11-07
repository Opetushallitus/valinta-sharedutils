package fi.vm.sade.valinta.sharedutils.http;

import static fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse.IS_CAS_302_REDIRECT;
import com.google.gson.Gson;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;


public class HttpResourceImpl implements HttpResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final WebClient webClient;
    private final Gson gson;
    private final int maxRetries = 3;
    private final int millisecondsToWaitMultiplier = 100;

    HttpResourceImpl(Gson gson, WebClient webClient, long timeoutMillis) {
        this.gson = gson;
        this.webClient = webClient;
        ClientConfiguration c = WebClient.getConfig(webClient);
        c.getHttpConduit().getClient().setReceiveTimeout(timeoutMillis);
        c.getHttpConduit().getClient().setConnectionTimeout(timeoutMillis);
    }

    @Override
    public Gson gson() {
        return gson;
    }

    /**
     * @return klooni konfiguroidusta webclientistä. cxf:n webclient objekti muuttuu joka palvelukutsulla.
     * koheesion vuoksi käytetään kloonia.
     */
    private WebClient getWebClient() {
        return WebClient.fromClient(webClient);
    }

    /* *** New lazily evaluated, non-replayed methods start here *** */

    @Override
    public Observable<Response> getAsObservableLazily(String path) {
        return requestAsPlainObservableLazily(path, (client, callback) -> client.async().get(callback));
    }

    @Override
    public Observable<Response> getAsObservableLazily(String path, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return requestAsPlainObservableLazily(path, (client, cb) -> paramsHeadersAndStuff.apply(client).async().get(cb));
    }

    @Override
    public Observable<String> getStringAsObservableLazily(String path) {
        return getAsObservableLazily(path, x -> x, x -> x);
    }

    @Override
    public <T> Observable<T> getAsObservableLazily(String path, Type type) {
        return getAsObservableLazily(path, type, x -> x);
    }

    @Override
    public <T, A> Observable<T> getAsObservableLazily(String path, Type type, Entity<A> entity) {
        return requestAsValueExtractingObservableLazily(path, GsonResponseCallback.jsonExtractor(gson(), type), (client, cb) -> client.async().method("GET", entity, cb));
    }

    @Override
    public <T> Observable<T> getAsObservableLazily(String path, Type type, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return getAsObservableLazily(path, GsonResponseCallback.jsonExtractor(gson(), type), paramsHeadersAndStuff);
    }

    @Override
    public <T> Observable<T> getAsObservableLazily(String path, Function<String, T> extractor, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return requestAsValueExtractingObservableLazily(path, extractor, (client, cb) -> paramsHeadersAndStuff.apply(client).async().get(cb));
    }

    @Override
    public <A, B> Observable<B> postAsObservableLazily(String path, Type type, Entity<A> entity) {
        return requestAsJsonObservableLazily(path, type, (client, cb) -> client.async().post(entity, cb));
    }

    @Override
    public <A, B> Observable<B> postAsObservableLazily(String path, Type type, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return requestAsJsonObservableLazily(path, type, (client, cb) -> paramsHeadersAndStuff.apply(client).async().post(entity, cb));
    }

    @Override
    public <A> Observable<Response> postAsObservableLazily(String path, Entity<A> entity) {
        return requestAsPlainObservableLazily(path, (client, cb) -> client.async().post(entity, cb));
    }

    @Override
    public <A> Observable<Response> postAsObservableLazily(String path, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return requestAsPlainObservableLazily(path, (client, cb) -> paramsHeadersAndStuff.apply(client).async().post(entity, cb));
    }

    @Override
    public <A, B> Observable<B> putAsObservableLazily(String path, Type type, Entity<A> entity) {
        return requestAsJsonObservableLazily(path, type, (client, cb) -> client.async().put(entity, cb));
    }

    @Override
    public <A> Observable<Response> putAsObservableLazily(String path, Entity<A> entity) {
        return requestAsPlainObservableLazily(path, (client, cb) -> client.async().put(entity, cb));
    }

    @Override
    public <A> Observable<Response> putAsObservableLazily(String path, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return requestAsPlainObservableLazily(path, (client, cb) -> paramsHeadersAndStuff.apply(client).async().put(entity, cb));
    }

    @Override
    public <A, B> Observable<B> putAsObservableLazily(String path, Type type, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return requestAsJsonObservableLazily(path, type, (client, cb) -> paramsHeadersAndStuff.apply(client).async().put(entity, cb));
    }

    @Override
    public <A> Observable<A> deleteAsObservableLazily(String path, Type type, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return requestAsJsonObservableLazily(path, type, (client, cb) -> {
            WebClient apply = paramsHeadersAndStuff.apply(client);
            AsyncInvoker async = apply.async();
            return async.delete(cb);
        });
    }

    private Observable<Response> requestAsPlainObservableLazily(final String path, final BiFunction<WebClient, ResponseCallback, Future<Response>> f) {
        return requestAsObservableLazily(path, (webclient, subscriber) -> {
            final ResponseCallback callback = new ResponseCallback(path, response -> {
                subscriber.onNext(response);
                subscriber.onCompleted();
            }, subscriber::onError);
            return f.apply(webclient, callback);
        });
    }

    private <T> Observable<T> requestAsJsonObservableLazily(final String path, final Type type, final BiFunction<WebClient, InvocationCallback, Future<T>> f) {
        return requestAsValueExtractingObservableLazily(path, GsonResponseCallback.jsonExtractor(gson(), type), f);
    }

    private <T> Observable<T> requestAsValueExtractingObservableLazily(final String path, final Function<String, T> extractor, final BiFunction<WebClient, InvocationCallback, Future<T>> f) {
        return requestAsObservableLazily(path, (webclient, subscriber) -> {
            final InvocationCallback callback = new ExtractSuccessfullResponseCallback<>(path, value -> {
                subscriber.onNext(value);
                subscriber.onCompleted();
            }, subscriber::onError, extractor);
            return f.apply(webclient, callback);
        });
    }

    private <T> Observable<T> requestAsObservableLazily(final String path, final BiFunction<WebClient, Subscriber<? super T>, Future<T>> f) {
        return Observable.<T>create(subscriber -> {
            final Future<T> future = f.apply(getWebClient().path(path).accept(MediaType.APPLICATION_JSON_TYPE), subscriber);
            subscriber.add(Subscriptions.create(() -> future.cancel(true)));
        }).retryWhen(retryOnHttp302(path)).share();
    }

    private Func1<Observable<? extends Throwable>, Observable<Throwable>> retryOnHttp302(String path) {
        return exceptions -> {
            Observable<Throwable> retriesForCasRedirects = exceptions.filter(IS_CAS_302_REDIRECT)
                .zipWith(Observable.range(0, maxRetries), Pair::of).flatMap(counterAndThrowable -> {
                    Integer counter = counterAndThrowable.getRight();
                    int delayMillis = millisecondsToWaitMultiplier * counter;
                    TimeUnit delayUnit = TimeUnit.MILLISECONDS;
                    LOG.warn(String.format("Ran into CAS login redirect at %s, retrying: retry number %s/%s, waiting for %s %s",
                        path, counter + 1, maxRetries, delayMillis, delayUnit));
                    return Observable.timer(delayMillis, delayUnit).map(x -> {
                        Throwable http302RedirectException = counterAndThrowable.getLeft();
                        if (counter >= maxRetries - 1) {
                            throw new RuntimeException(http302RedirectException);
                        }
                        return http302RedirectException;
                    });
                });
            return retriesForCasRedirects.mergeWith(exceptions
                .filter(x1 -> !IS_CAS_302_REDIRECT.call(x1))
                .doOnNext(otherThanCasRedirectException -> {
                    if (otherThanCasRedirectException instanceof RuntimeException) {
                        throw (RuntimeException) otherThanCasRedirectException;
                    } else {
                        LOG.warn("Wrapping " + otherThanCasRedirectException + " in " + RuntimeException.class.getSimpleName());
                        throw new RuntimeException(otherThanCasRedirectException);
                    }
                }));
        };
    }
}
