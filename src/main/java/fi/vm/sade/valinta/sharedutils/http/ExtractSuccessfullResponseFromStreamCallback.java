package fi.vm.sade.valinta.sharedutils.http;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.PrettyXmlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class ExtractSuccessfullResponseFromStreamCallback<T> implements InvocationCallback<Response> {
    private final static Logger LOG = LoggerFactory.getLogger(ExtractSuccessfullResponseFromStreamCallback.class);
    private final String url;
    private final Consumer<T> callback;
    private final Consumer<Throwable> failureCallback;
    private final Function<InputStream, T> extractor;

    public ExtractSuccessfullResponseFromStreamCallback(final String url,
                                                        final Consumer<T> callback,
                                                        final Consumer<Throwable> failureCallback,
                                                        final Function<InputStream, T> extractor) {
        this.url = url;
        this.callback = callback;
        this.failureCallback = failureCallback;
        this.extractor = extractor;
    }

    @Override
    public void completed(Response response) {
        if (response.getStatus() >= 300) {
            String msg = String.format("%s HTTP %d", url, response.getStatus());
            failureCallback.accept(new HttpExceptionWithResponse(msg, response));
            return;
        }
        InputStream responseInputStream = new ByteArrayInputStream(new byte[]{});
        try {
            responseInputStream = readResponseAsInputStream(response);
            T t = extractor.apply(responseInputStream);
            try {
                if (t == null) {
                    String errorMessage = String.format("Saatiin null vastaus URLista '%s'", url);
                    LOG.error(errorMessage);
                    throw new RuntimeException(errorMessage);
                }
                callback.accept(t);
            } catch (Exception e) {
                LOG.error(
                        "Asynkronisen kutsun ({}) paluuarvonkasittelija heitti poikkeuksen:\r\nRESPONSE {} ->\r\n{} {}",
                        url,
                        response.getStatus(),
                        response.getMetadata().getFirst("Content-Type"),
                        format(response, responseInputStream),
                        e);
                failureCallback.accept(e);
            }
        } catch (Exception e) {
            LOG.error(
                    "Gson deserialisointi epaonnistui onnistuneelle asynkroniselle palvelin kutsulle ({}), {}:\r\nRESPONSE {} {} ->\r\n{}",
                    url, e.getMessage(), response.getStatus(),
                    response.getMetadata().getFirst("Content-Type"),
                    format(response, responseInputStream));
            LOG.error("Gson deserialization throws", e);
            try {
                failureCallback.accept(e);
            } catch (Exception ex) {
                LOG.error(
                        "Asynkronisen kutsun ({}) epaonnistuneesta sarjallistuksesta seuranneelle virheenkasittelijakutsusta lensi poikkeus: {}:\r\nRESPONSE {} {} ->\r\n{}",
                        url, ex.getMessage(),
                        response.getStatus(),
                        response.getMetadata().getFirst("Content-Type"),
                        format(response, responseInputStream));
                LOG.error("failureCallback throws", ex);
            }
        }
    }

    private InputStream readResponseAsInputStream(final Response response) {
        return (InputStream) response.getEntity();
    }

    @Override
    public void failed(Throwable throwable) {
        try {
            failureCallback.accept(throwable);
        } catch (Exception ex) {
            LOG.error("Epaonnistuneen asynkronisen kutsun (" + url + ") virheenkasittelija heitti poikkeuksen", ex);
        }
    }

    private String format(Response response, InputStream json) {
        try {
            if (isTextHtml(response)) {
                CleanerProperties cp = new CleanerProperties();
                cp.setAddNewlineToHeadAndBody(false);
                cp.setOmitHtmlEnvelope(true);
                try {
                    return new PrettyXmlSerializer(cp).getAsString(IOUtils.toString(json, "UTF-8"));
                } catch (Exception ex) {
                    return StringUtils.substring(IOUtils.toString(json, "UTF-8"), 0, 250);
                }
            } else {
                return StringUtils.substring(IOUtils.toString(json, "UTF-8"), 0, 250);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isTextHtml(Response response) {
        try {
            return Optional.of(response.getMetadata().getFirst("Content-Type"))
                .orElse(new Object()).toString()
                .contains(MediaType.TEXT_HTML);
        } catch (Exception e) {
            return false;
        }
    }
}
