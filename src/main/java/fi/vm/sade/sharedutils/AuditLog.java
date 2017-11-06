package fi.vm.sade.sharedutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.vm.sade.auditlog.*;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;

public class AuditLog {

    private static final Audit AUDITLOG = new Audit(new AuditLogger(), "valintaperusteet", ApplicationType.BACKEND);
    private static final Logger LOG = LoggerFactory.getLogger(AuditLog.class);
    private static final JsonParser parser = new JsonParser();
    static final int MAX_FIELD_LENGTH = 32766;
    private static final ObjectMapper mapper = new ObjectMapper();

    public static final String UNKNOWN_USER_AGENT = "Unknown user agent";
    private static final String DUMMYOID_STR = "1.2.999.999.99.99999999999";
    private static final String UNKNOWN_SESSION = "Unknown session";
    private static final User ANONYMOUS_USER;
    private static Oid DUMMYOID;
    private static final String TARGET_EPASELVA = "Tuntematon tai muutosten implikoima kohde";

    static {
        User anon = null;
        try {
            DUMMYOID = new Oid(DUMMYOID_STR);
            anon = new User(DUMMYOID, InetAddress.getByName(""), null, null);
        } catch(GSSException | UnknownHostException e) {
            LOG.error("Creating anonymous anon failed", e);
        }
        ANONYMOUS_USER = anon;
    }

    public static <T> void log(Operation operation, ValintaResource valintaResource, String targetOid, T dtoAfterOperation, T dtoBeforeOperation, HttpServletRequest request, @NotNull Map<String, String> additionalInfo) {
        User user = getUser(request);
        Target.Builder target = getTarget(valintaResource, targetOid);
        additionalInfo.forEach(target::setField);
        Changes changes = getChanges(dtoAfterOperation, dtoBeforeOperation).build();
        AUDITLOG.log(user, operation, target.build(), changes);
    }

    public static <T> void log(Operation operation, ValintaResource valintaResource, String targetOid, T dtoAfterOperation, T dtoBeforeOperation, HttpServletRequest request) {
        log(operation, valintaResource, targetOid, dtoAfterOperation, dtoBeforeOperation, request, Maps.newHashMap());
    }

    private static User getUser(HttpServletRequest request) {
        try {
            String userOid = getUserOidFromSession();
            String userAgent = null;
            String session = null;
            InetAddress ip = null;
            if(request != null) {
                userAgent = getUserAgentHeader(request);
                session = getSession(request);
                ip = getInetAddress(request);
            }
            return getUser(userOid, ip, session, userAgent);
        } catch(Exception e) {
            LOG.error("Recording anonymous user", e);
            return ANONYMOUS_USER;
        }

    }

    private static String getUserAgentHeader(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    public static String getSession(HttpServletRequest request) {
        try {
            return request.getSession(false).getId();
        } catch(Exception e) {
            LOG.error("Couldn't log session for requst {}", request);
            return null;
        }
    }

    public static InetAddress getInetAddress(HttpServletRequest request) {
        try {
            return InetAddress.getByName(request.getRemoteAddr());
        } catch(Exception e) {
            LOG.error("Couldn't log InetAddress for log entry", e);
            return null;
        }
    }

    public static Oid getOid(String usernameFromSession) {
        try {
            return new Oid(usernameFromSession);
        } catch(Exception e) {
            LOG.error("Couldn't log oid {} for log entry", usernameFromSession, e);
            return null;
        }
    }

    public static String getUserOidFromSession() {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context != null) {
            Principal p = context.getAuthentication();
            if (p != null) {
                return p.getName();
            }
        }
        LOG.error("Returning null user oid");
        return null;
    }

    private static User getUser(String userOid, InetAddress ip, String session, String userAgent) {
        Oid oid;
        try {
            oid = getOid(userOid);
        } catch(Exception e) {
            LOG.error("Recording anonymous user", e);
            oid = DUMMYOID;
        }
        return new User(
                oid,
                ip != null ? ip : InetAddress.getLoopbackAddress(),
                session != null ? session : UNKNOWN_SESSION,
                userAgent != null ? userAgent : UNKNOWN_USER_AGENT
        );

    }

    //Temporarily public for debugging
    static <T> Changes.Builder getChanges(@Nullable T afterOperation, @Nullable T beforeOperation) {
        Changes.Builder builder = new Changes.Builder();
        try {
            if (afterOperation == null && beforeOperation != null) {
                builder.removed("change", toGson(mapper.valueToTree(beforeOperation)));
            } else if (afterOperation != null && beforeOperation == null) {
                builder.added("change", toGson(mapper.valueToTree(afterOperation)));
            } else if (afterOperation != null) {
                JsonNode afterJson = mapper.valueToTree(afterOperation);
                JsonNode beforeJson = mapper.valueToTree(beforeOperation);
                traverseAndTruncate(afterJson);
                traverseAndTruncate(beforeJson);

                final ArrayNode patchArray = (ArrayNode) JsonDiff.asJson(beforeJson, afterJson);
                builder.updated("change", toGson(beforeJson), toGsonArray(patchArray));
            }
        } catch(Exception e) {
            LOG.error("diff calculation failed", e);
        }
        return builder;
    }

    private static JsonObject toGson(@NotNull JsonNode json) {
        return parser.parse(json.toString()).getAsJsonObject();
    }

    private static JsonArray toGsonArray(@NotNull JsonNode json) {
        return parser.parse(json.toString()).getAsJsonArray();
    }

    static void traverseAndTruncate(JsonNode data) {
        if (data.isObject()) {
            ObjectNode object = (ObjectNode) data;
            for (Iterator<String> it = data.fieldNames(); it.hasNext(); ) {
                String fieldName = it.next();
                JsonNode child = object.get(fieldName);
                if (child.isTextual()) {
                    object.set(fieldName, truncate((TextNode) child));
                } else {
                    traverseAndTruncate(child);
                }
            }
        } else if (data.isArray()) {
            ArrayNode array = (ArrayNode) data;
            for (int i = 0; i < array.size(); i++) {
                JsonNode child = array.get(i);
                if (child.isTextual()) {
                    array.set(i, truncate((TextNode) child));
                } else {
                    traverseAndTruncate(child);
                }
            }
        }
    }

    private static TextNode truncate(TextNode data) {
        int maxLength = MAX_FIELD_LENGTH / 10; // Assume only a small number of fields can be extremely long
        if (data.textValue().length() <= maxLength) {
            return data;
        } else {
            String truncated = (new Integer(data.textValue().hashCode())).toString();
            return TextNode.valueOf(truncated);
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
