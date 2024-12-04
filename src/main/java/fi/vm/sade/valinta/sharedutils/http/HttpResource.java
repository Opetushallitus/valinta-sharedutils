package fi.vm.sade.valinta.sharedutils.http;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import io.reactivex.Observable;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public interface HttpResource {
    Gson DEFAULT_GSON = fi.vm.sade.valinta.sharedutils.http.DateDeserializer.gsonBuilder().create();
    long DEFAULT_CLIENT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(120L);
    Function<WebClient, WebClient> ACCEPT_JSON = client -> client.accept(MediaType.APPLICATION_JSON_TYPE);
    String CALLER_ID = "Caller-Id";
    String CSRF_VALUE = "CSRF";

    static JAXRSClientFactoryBean getJaxrsClientFactoryBean() {
        return getJaxrsClientFactoryBean("");
    }

    static JAXRSClientFactoryBean getJaxrsClientFactoryBean(final String address) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        bean.setThreadSafe(true);
        List<Object> providers = Lists.newArrayList();
        providers.add(new com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider());
        providers.add(new fi.vm.sade.valinta.sharedutils.http.ObjectMapperProvider());
        bean.setProviders(providers);
        return bean;
    }

    Gson gson();

    Observable<Response> getAsObservableLazily(String path);

    Observable<Response> getAsObservableLazily(String path, Function<WebClient, WebClient> paramsHeadersAndStuff);

    Observable<String> getStringAsObservableLazily(String path);

    <T> Observable<T> getAsObservableLazily(String path, final Type type);

    <T, A> Observable<T> getAsObservableLazily(String path, final Type type, Entity<A> getBody);

    <T> Observable<T> getAsObservableLazily(String path, final Type type, Function<WebClient, WebClient> paramsHeadersAndStuff);

    <T> Observable<T> getAsObservableLazilyWithInputStream(String path, Type type, Function<WebClient, WebClient> paramsHeadersAndStuff);

    <T> Observable<T> getAsObservableLazily(String path, Function<String, T> extractor, Function<WebClient, WebClient> paramsHeadersAndStuff);

    <A, B> Observable<B> postAsObservableLazily(String path, final Type type, Entity<A> entity);

    <A, B> Observable<B> postAsObservableLazily(String path, final Type type, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff);

    <A> Observable<Response> postAsObservableLazily(String path, Entity<A> entity);

    <A> Observable<Response> postAsObservableLazily(String path, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff);

    <A, B> Observable<B> putAsObservableLazily(String path, final Type type, Entity<A> entity);

    <A> Observable<Response> putAsObservableLazily(String path, Entity<A> entity);

    <A> Observable<Response> putAsObservableLazily(String path, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff);

    <A, B> Observable<B> putAsObservableLazily(String path, final Type type, Entity<A> entity, Function<WebClient, WebClient> paramsHeadersAndStuff);

    <A> Observable<A> deleteAsObservableLazily(String path, final Type type, Function<WebClient, WebClient> paramsHeadersAndStuff);
}
