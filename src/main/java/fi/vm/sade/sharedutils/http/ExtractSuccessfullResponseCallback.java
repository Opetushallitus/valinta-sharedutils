package fi.vm.sade.valinta.sharedutils.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.PrettyXmlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractSuccessfullResponseCallback<T> implements InvocationCallback<Response> {
    private final static Logger LOG = LoggerFactory.getLogger(ExtractSuccessfullResponseCallback.class);
    private final String url;
    private final Consumer<T> callback;
    private final Consumer<Throwable> failureCallback;
    private final Function<String, T> extractor;

    public ExtractSuccessfullResponseCallback(final String url, final Consumer<T> callback, final Consumer<Throwable> failureCallback, final Function<String, T> extractor) {
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
        String responseString = StringUtils.EMPTY;
        try {
            responseString = readResponseAsString(response, responseString);
            T t = extractor.apply(responseString);
            try {
                callback.accept(t);
            } catch (Exception e) {
                LOG.error(
                        "Asynkronisen kutsun ({}) paluuarvonkasittelija heitti poikkeuksen:\r\nRESPONSE {} ->\r\n{} {}",
                        url,
                        response.getStatus(),
                        response.getMetadata().getFirst("Content-Type"),
                        format(response, responseString),
                        e);
                failureCallback.accept(e);
            }
        } catch (Exception e) {
            LOG.error(
                    "Gson deserialisointi epaonnistui onnistuneelle asynkroniselle palvelin kutsulle ({}), {}:\r\nRESPONSE {} {} ->\r\n{}",
                    url, e.getMessage(), response.getStatus(),
                    response.getMetadata().getFirst("Content-Type"),
                    format(response, responseString));
            LOG.error("Gson deserialization throws", e);
            try {
                failureCallback.accept(e);
            } catch (Exception ex) {
                LOG.error(
                        "Asynkronisen kutsun ({}) epaonnistuneesta sarjallistuksesta seuranneelle virheenkasittelijakutsusta lensi poikkeus: {}:\r\nRESPONSE {} {} ->\r\n{}",
                        url, ex.getMessage(),
                        response.getStatus(),
                        response.getMetadata().getFirst("Content-Type"),
                        format(response, responseString));
                LOG.error("failureCallback throws", ex);
            }
        }
    }

    private String readResponseAsString(final Response response, String json) throws IOException {
        InputStream stream = (InputStream) response.getEntity();
        json = StringUtils.trimToEmpty(IOUtils.toString(stream));
        IOUtils.closeQuietly(stream);
        if (json.length() == 0 && response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
            LOG.error(
                    "Paluuarvona saadun viestin pituus oli nolla merkkia palvelukutsulle {} (Response {} {})",
                    url, response.getStatus(), response
                            .getMetadata().getFirst("Content-Type"));
        }
        return json;
    }

    @Override
    public void failed(Throwable throwable) {
        try {
            failureCallback.accept(throwable);
        } catch (Exception ex) {
            LOG.error("Epaonnistuneen asynkronisen kutsun (" + url + ") virheenkasittelija heitti poikkeuksen", ex);
        }
    }

    private String format(Response response, String json) {
        if (isTextHtml(response)) {
            CleanerProperties cp = new CleanerProperties();
            cp.setAddNewlineToHeadAndBody(false);
            cp.setOmitHtmlEnvelope(true);
            try {
                return new PrettyXmlSerializer(cp).getAsString(json);
            } catch (Exception ex) {
                return StringUtils.substring(json, 0, 250);
            }
        } else {
            return StringUtils.substring(json, 0, 250);
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
