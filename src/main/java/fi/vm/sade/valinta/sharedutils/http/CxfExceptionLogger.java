package fi.vm.sade.valinta.sharedutils.http;

import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;

/**
 * Looks like by default exceptions thrown by our CXF resources do not get logged.
 */
public class CxfExceptionLogger implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(CxfExceptionLogger.class);
    private final WebApplicationExceptionMapper defaultMapper = new WebApplicationExceptionMapper();

    @Override
    public Response toResponse(Exception exception) {
        if(exception instanceof AccessDeniedException) {
            LOG.debug("Access denied", exception);
            return Response
                    .status(FORBIDDEN)
                    .entity("Käyttäjällä ei ole tarvittavia oikeuksia.")
                    .type(TEXT_PLAIN).build();
        }
        LOG.error("Uncaught exception", exception);
        if (exception instanceof WebApplicationException) {
            return defaultMapper.toResponse((WebApplicationException) exception);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    public static String getCxfUrl(Response response) {
        try {
            ResponseImpl r = (ResponseImpl) response;
            Object url = r.getOutMessage().get("org.apache.cxf.request.uri");
            return url.toString();
        } catch (Exception e) {
            LOG.warn(String.format("Could not determine CXF URL from response %s", response), e);
            return "<unknown cxf url>";
        }
    }
}
