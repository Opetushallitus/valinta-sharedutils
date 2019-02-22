package fi.vm.sade.valinta.sharedutils.http;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class HttpExceptionWithResponse extends RuntimeException {
    public static final Map.Entry<String,String> CAS_302_REDIRECT_MARKER = Pair.of("X-Oph-CAS-Redirect", "true");
    public static final io.reactivex.functions.Predicate<Throwable> IS_CAS_302_REDIRECT = e ->
    {
        Optional<HttpExceptionWithResponse> exceptionWithResponse = HttpExceptionWithResponse.findWrappedHttpExceptionWithResponse(e);
        return exceptionWithResponse.map(hewr ->
            CAS_302_REDIRECT_MARKER.getValue().equals(hewr.response.getHeaderString(CAS_302_REDIRECT_MARKER.getKey()))).orElse(false);
    };

    public final Response response;
    public final int status;

    public HttpExceptionWithResponse(String msg, Response response) {
        super(msg);
        this.status = response.getStatus();
        this.response = response;
    }

    public String contentToString() {
        Object entity = response.getEntity();
        if (entity instanceof InputStream) {
            try {
                return IOUtils.toString((InputStream) entity);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return entity.toString();
    }

    public static String appendWrappedResponse(String message, Throwable exception) {
        Optional<String> responseContent = findResponseContents(exception);
        if (responseContent.isPresent()) {
            return StringUtils.join(Arrays.asList(message, responseContent.get()), ", response: ");
        }
        return message;
    }

    public static boolean isResponseWithStatus(Response.Status expected, Throwable throwable) {
        return matches(hewr -> hewr.status == expected.getStatusCode(), throwable);
    }

    public static boolean matches(java.util.function.Function<HttpExceptionWithResponse, Boolean> predicate, Throwable throwable) {
        return findWrappedHttpExceptionWithResponse(throwable)
            .map(predicate)
            .orElse(false);
    }

    private static Optional<String> findResponseContents(Throwable t) {
        return findWrappedHttpExceptionWithResponse(t).map(HttpExceptionWithResponse::contentToString);
    }

    private static Optional<HttpExceptionWithResponse> findWrappedHttpExceptionWithResponse(Throwable t) {
        if (t instanceof HttpExceptionWithResponse) {
            return Optional.of(((HttpExceptionWithResponse) t));
        }
        if (t.getCause() == null) {
            return Optional.empty();
        }
        return findWrappedHttpExceptionWithResponse(t.getCause());
    }
}
