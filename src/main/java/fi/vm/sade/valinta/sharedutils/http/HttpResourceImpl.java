package fi.vm.sade.valinta.sharedutils.http;

import static fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse.IS_CAS_302_REDIRECT;
import com.google.gson.Gson;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;


public class HttpResourceImpl implements fi.vm.sade.valinta.sharedutils.http.HttpResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final WebClient webClient;
    private final String callerId;
    private final Gson gson;
    private final int maxRetries = 3;
    private final int millisecondsToWaitMultiplier = 100;

    HttpResourceImpl(Gson gson, WebClient webClient, String callerId, long timeoutMillis) {
        this.gson = gson;
        this.webClient = webClient;
        this.callerId = callerId;

        Assert.isTrue(StringUtils.isNotBlank(callerId), "Please give non-empty callerId: got '" + callerId + "'");
        ClientConfiguration c = WebClient.getConfig(webClient);
        c.getHttpConduit().getClient().setReceiveTimeout(timeoutMillis);
        c.getHttpConduit().getClient().setConnectionTimeout(timeoutMillis);

        List<String> callerIdValues = webClient.getHeaders().get(CALLER_ID);
        if (!callerIdValues.isEmpty() && !callerId.equals(callerIdValues.get(0))) {
            throw new IllegalArgumentException(String.format("Expected callerId value '%s' " +
                "from constructor parameters to equal value '%s' in webClient. " +
                "WebClient whole list: %s", callerId, callerIdValues.get(0), callerIdValues));
        }
    }

    @Override
    public Gson gson() {
        return gson;
    }

    /**
     * Asettaa myös Caller-Id -headerin, joka ei näy WebClientistä eri säikeiden välillä.
     *
     * @return klooni konfiguroidusta webclientistä. cxf:n webclient objekti muuttuu joka palvelukutsulla.
     * koheesion vuoksi käytetään kloonia.
     */
    private WebClient getWebClient() {
        return fromClientWithCallerId(this.webClient);
    }

    protected WebClient fromClientWithCallerId(WebClient client) {
        return WebClient.fromClient(client).header(CALLER_ID, callerId);
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
        return requestAsValueExtractingObservableLazily(path, fi.vm.sade.valinta.sharedutils.http.GsonResponseCallback.jsonExtractor(gson(), type), (client, cb) -> client.async().method("GET", entity, cb));
    }

    @Override
    public <T> Observable<T> getAsObservableLazily(String path, Type type, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return getAsObservableLazily(path, fi.vm.sade.valinta.sharedutils.http.GsonResponseCallback.jsonExtractor(gson(), type), paramsHeadersAndStuff);
    }

    @Override
    public <T> Observable<T> getAsObservableLazilyWithInputStream(String path, Type type, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return getAsObservableLazilyWithInputStream(path, fi.vm.sade.valinta.sharedutils.http.GsonResponseInpuStreamCallback.jsonExtractor(gson(), type), paramsHeadersAndStuff);
    }

    private <T> Observable<T> getAsObservableLazilyWithInputStream(String path, Function<InputStream, T> extractor, Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return requestAsValueExtractingObservableLazilyWithInputStream(path, extractor, (client, cb) -> paramsHeadersAndStuff.apply(client).async().get(cb));
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

    private Observable<Response> requestAsPlainObservableLazily(final String path, final BiFunction<WebClient, fi.vm.sade.valinta.sharedutils.http.ResponseCallback, Future<Response>> f) {
        return requestAsObservableLazily(path, (webclient, subscriber) -> {
            final fi.vm.sade.valinta.sharedutils.http.ResponseCallback callback = new fi.vm.sade.valinta.sharedutils.http.ResponseCallback(path, response -> {
                subscriber.onNext(response);
                subscriber.onComplete();
            }, subscriber::onError);
            return f.apply(webclient, callback);
        });
    }

    private <T> Observable<T> requestAsJsonObservableLazily(final String path, final Type type, final BiFunction<WebClient, InvocationCallback, Future<T>> f) {
        return requestAsValueExtractingObservableLazily(path, fi.vm.sade.valinta.sharedutils.http.GsonResponseCallback.jsonExtractor(gson(), type), f);
    }

    private <T> Observable<T> requestAsValueExtractingObservableLazily(final String path, final Function<String, T> extractor, final BiFunction<WebClient, InvocationCallback, Future<T>> f) {
        return requestAsObservableLazily(path, (webclient, subscriber) -> {
            final InvocationCallback callback = new fi.vm.sade.valinta.sharedutils.http.ExtractSuccessfullResponseCallback<T>(path, value -> {
                subscriber.onNext(value);
                subscriber.onComplete();
            }, subscriber::onError, extractor);
            return f.apply(webclient, callback);
        });
    }

    private <T> Observable<T> requestAsValueExtractingObservableLazilyWithInputStream(final String path, final Function<InputStream, T> extractor, final BiFunction<WebClient, InvocationCallback, Future<T>> f) {
        return requestAsObservableLazily(path, (webclient, subscriber) -> {
            final InvocationCallback callback = new fi.vm.sade.valinta.sharedutils.http.ExtractSuccessfullResponseFromStreamCallback<T>(path, value -> {
                subscriber.onNext(value);
                subscriber.onComplete();
            }, subscriber::onError, extractor);
            return f.apply(webclient, callback);
        });
    }
    private <T> Observable<T> requestAsObservableLazily(final String path, final BiFunction<WebClient, ObservableEmitter<? super T>, Future<T>> f) {
        return Observable.<T>create(subscriber -> {
            final Future<T> future = f.apply(getWebClient().path(path).accept(MediaType.APPLICATION_JSON_TYPE), subscriber);
            // subscriber.setCancellable(() -> future.cancel(true)); // OY-280 : This causes a lot of output, but does not seem to break functionality...
        }).retryWhen(retryOnHttp302(path)).share();
    }

    private io.reactivex.functions.Function<Observable<? extends Throwable>, Observable<Throwable>> retryOnHttp302(String path) {
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
                .filter(x1 -> !IS_CAS_302_REDIRECT.test(x1))
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
