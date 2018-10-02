package fi.vm.sade.sharedutils;

import com.google.common.collect.Maps;
import fi.vm.sade.auditlog.*;
import fi.vm.sade.javautils.http.HttpServletRequestUtils;
import org.apache.commons.collections4.MapUtils;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.net.InetAddress;
import java.security.Principal;
import java.util.Map;

public class AuditLog {
    private static final Logger LOG = LoggerFactory.getLogger(AuditLog.class);
    private static final String TARGET_EPASELVA = "Tuntematon tai muutosten implikoima kohde";

    public static <T> void log(Audit audit, User user, Operation operation, ValintaResource valintaResource, String targetOid, Changes changes, @NotNull Map<String, String> additionalInfo) {
        Target.Builder target = getTarget(valintaResource, targetOid);
        if (!MapUtils.isEmpty(additionalInfo)) {
            additionalInfo.forEach(target::setField);
        }
        additionalInfo.forEach(target::setField);
        audit.log(user, operation, target.build(), changes);
    }

    public static <T> void log(Audit audit, User user, Operation operation, ValintaResource valintaResource, String targetOid, Changes changes) {
        log(audit, user, operation, valintaResource, targetOid, changes, Maps.newHashMap());
    }

    public static User getUser(HttpServletRequest request) {
        String userOid = loggedInUserOid();
        String userAgent = getUserAgentHeader(request);
        String session = getSession(request);
        InetAddress ip = getInetAddress(request);
        return getUser(userOid, ip, session, userAgent);
    }

    public static String loggedInUserOid() {
        SecurityContext context = SecurityContextHolder.getContext();
        Assert.notNull(context, "Null SecurityContext! Make sure to only call this method from request thread.");
        Principal p = context.getAuthentication();
        Assert.notNull(p, "Null principal! Something wrong in the authentication?");
        return p.getName();
    }

    private static String getUserAgentHeader(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    private static String getSession(HttpServletRequest request) {
        try {
            return request.getSession(false).getId();
        } catch(Exception e) {
            LOG.error("Couldn't log session for requst {}", request);
            throw new RuntimeException(e);
        }
    }

    private static InetAddress getInetAddress(HttpServletRequest request) {
        try {
            return InetAddress.getByName(HttpServletRequestUtils.getRemoteAddress(request));
        } catch(Exception e) {
            LOG.error("Couldn't log InetAddress for log entry", e);
            throw new RuntimeException(e);
        }
    }

    private static User getUser(String userOid, InetAddress ip, String session, String userAgent) {
        try {
            return new User(new Oid(userOid), ip, session, userAgent);
        } catch (GSSException e) {
            throw new RuntimeException(e);
        }
    }

    private static Target.Builder getTarget(ValintaResource valintaResource, String targetOid) {
        if (targetOid == null) {
            targetOid = TARGET_EPASELVA;
        }
        return new Target.Builder()
                .setField("type", valintaResource.name())
                .setField("oid", targetOid);
    }
}
