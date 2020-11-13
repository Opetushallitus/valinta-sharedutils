package fi.vm.sade.valinta.sharedutils.http;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import org.apache.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class SpringAccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {
    private static final Logger LOG = Logger.getLogger(SpringAccessDeniedExceptionMapper.class);

    @Override
    public Response toResponse(AccessDeniedException exception) {
        LOG.debug("Access denied", exception);
        return Response
            .status(FORBIDDEN)
            .entity("Käyttäjällä ei ole tarvittavia oikeuksia.")
            .type(TEXT_PLAIN).build();
    }
}
