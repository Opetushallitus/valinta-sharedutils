package fi.vm.sade.valinta.sharedutils.http;

import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.log4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Looks like by default exceptions thrown by our CXF resources do not get logged.
 */
public class CxfExceptionLogger implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(CxfExceptionLogger.class);
    private final WebApplicationExceptionMapper defaultMapper = new WebApplicationExceptionMapper();

    @Override
    public Response toResponse(Exception exception) {
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
