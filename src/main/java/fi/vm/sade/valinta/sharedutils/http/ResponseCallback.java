package fi.vm.sade.valinta.sharedutils.http;

import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.CancellationException;
import java.util.Optional;
import java.util.function.Consumer;

public class ResponseCallback implements InvocationCallback<Response> {
    private final static Logger LOG = LoggerFactory.getLogger(ResponseCallback.class);

    private final boolean only2xxIsCompleted;
    private final Consumer<Response> callback;
    private final Consumer<Throwable> failureCallback;
    private final String kutsu;

    public ResponseCallback(String kutsu, boolean only2xxIsCompleted, Consumer<Response> callback, Consumer<Throwable> failureCallback) {
        this.kutsu = kutsu;
        this.callback = callback;
        this.failureCallback = failureCallback;
        this.only2xxIsCompleted = only2xxIsCompleted;
    }

    public ResponseCallback(String kutsu, Consumer<Response> callback, Consumer<Throwable> failure) {
        this(kutsu, true, callback, failure);
    }

    @Override
    public void completed(Response response) {
        try {
            LOG.info("Kutsu {} onnistui, status:{}", kutsu, response.getStatus());
            if (callback != null && failureCallback != null) {
                if (only2xxIsCompleted) {
                    int status = response.getStatus();
                    if (status >= 200 && status < 300) {
                        callback.accept(response);
                    } else {
                        String msg = String.format("Expected status code 200-299 from %s but got code %d",
                            fi.vm.sade.valinta.sharedutils.http.CxfExceptionLogger.getCxfUrl(response),
                            response.getStatus());
                        LOG.error(msg);
                        failureCallback.accept(new fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse(msg, response));
                    }
                } else {
                    callback.accept(response);
                }
            } else {
                LOG.info("Ohitettiin ilmoittaminen {} {}", callback == null ? ", callback null" : "", failureCallback == null ? ", failureCallback null" : "");
            }
        } catch (Throwable t) {
            LOG.error("Jotain meni pieleen onnistuneen responsen käsittelyssä", t);
        }
    }

    @Override
    public void failed(Throwable throwable) {
        boolean cancellationException = throwable instanceof CancellationException;
        if (cancellationException) {
            LOG.debug("Saatiin " + CancellationException.class.getSimpleName());
        } else {
            Throwable t = Optional.ofNullable(throwable).orElse(new RuntimeException("Unknown exception!"));
            LOG.info("Kutsu epäonnistui", t);
        }
        try {
            if (failureCallback != null) {
                failureCallback.accept(throwable);
            } else {
                LOG.info("Ohitettiin virheilmoittaminen koska failure callback puuttui");
            }
        } catch (Throwable t) {
            LOG.error("Jotain meni pieleen epäonnistuneen responsen käsittelyssä", t);
        } finally {
            if (cancellationException) {
                LOG.warn("Oltiin epäonnistuneen kutsun käsittelyssä " + CancellationException.class.getSimpleName() + " :n kanssa");
            } else {
                LOG.info("Oltiin epäonnistuneen kutsun käsittelyssä");
            }
        }

    }

    private String entityToString(Object entity) {
        if (entity == null) {
            return "Palvelin ei antanut virheelle syytä";
        } else if (entity instanceof InputStream) {
            try {
                return IOUtils.toString((InputStream) entity);
            } catch (Exception e) {
                LOG.error("Palvelinvirheen luku epaonnistui", e);
                return "Palvelinvirhettä ei pystytty lukemaan";
            }
        } else {
            return entity.toString();
        }
    }
}
