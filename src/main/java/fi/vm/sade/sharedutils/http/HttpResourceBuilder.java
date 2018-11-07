package fi.vm.sade.valinta.sharedutils.http;

import com.google.gson.Gson;

import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;

import java.util.concurrent.TimeUnit;

/**
 * Builder for creating a HttpResource.
 * <p>
 * Simplest possible use case:
 * {@code HttpResourceBuilder().build();}
 *
 * Set JAXRSClientFactoryBean manually (useful when CAS filter is needed):
 * {@code HttpResourceBuilder().jaxrsClientFactoryBean(someBean).build();}
 */
public class HttpResourceBuilder {

    private String address = "";
    private long timeoutMillis = TimeUnit.SECONDS.toMillis(120L);
    private Gson gson = HttpResource.DEFAULT_GSON;
    private JAXRSClientFactoryBean jaxrsClientFactoryBean;

    public HttpResourceBuilder() {}

    public HttpResourceBuilder address(String val) {
        address = val;
        return this;
    }

    public HttpResourceBuilder timeoutMillis(long val) {
        timeoutMillis = val;
        return this;
    }

    public HttpResourceBuilder jaxrsClientFactoryBean(JAXRSClientFactoryBean val) {
        jaxrsClientFactoryBean = val;
        return this;
    }

    public HttpResourceBuilder gson(Gson val) {
        gson = val;
        return this;
    }

    public HttpResource build() {
        // Hide dangerous direct access to WebClient.
        return buildExposingWebClientDangerously();
    }

    /**
     * This API should only be used in test code. In production, it's far better to not use WebClient
     * directly, but the wrapped getAsObservable, postAsObservable etc methods, which will retry CAS 302 redirect calls.
     */
    public WebClientExposingHttpResource buildExposingWebClientDangerously() {
        if (jaxrsClientFactoryBean == null) {
            this.jaxrsClientFactoryBean = HttpResource.getJaxrsClientFactoryBean(address);
        }
        return new WebClientExposingHttpResource(this.gson, this.jaxrsClientFactoryBean.createWebClient(), this.timeoutMillis);
    }

    public class WebClientExposingHttpResource extends HttpResourceImpl {
        private final WebClient webClient;

        WebClientExposingHttpResource(Gson gson, WebClient webClient, long timeoutMillis) {
            super(gson, webClient, timeoutMillis);
            this.webClient = webClient;
        }

        public WebClient getWebClient() {
            return WebClient.fromClient(webClient);
        }
    }
}
