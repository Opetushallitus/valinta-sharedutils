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

import fi.vm.sade.auditlog.Audit;
import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.Operation;
import fi.vm.sade.auditlog.Target;
import fi.vm.sade.auditlog.User;
import fi.vm.sade.javautils.http.HttpServletRequestUtils;
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
import java.util.Iterator;
import java.util.Map;

public class AuditLog {
    private static final Logger LOG = LoggerFactory.getLogger(AuditLog.class);
    private static final JsonParser parser = new JsonParser();
    private static final int MAX_FIELD_LENGTH = 32766;
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String TARGET_EPASELVA = "Tuntematon tai muutosten implikoima kohde";

    public static <T> void log(Audit audit, User user, Operation operation, ValintaResource valintaResource, String targetOid, T dtoAfterOperation, T dtoBeforeOperation, @NotNull Map<String, String> additionalInfo) {
        Target.Builder target = getTarget(valintaResource, targetOid);
        additionalInfo.forEach(target::setField);
        Changes changes = getChanges(dtoAfterOperation, dtoBeforeOperation).build();
        audit.log(user, operation, target.build(), changes);
    }

    public static <T> void log(Audit audit, User user, Operation operation, ValintaResource valintaResource, String targetOid, T dtoAfterOperation, T dtoBeforeOperation) {
        log(audit, user, operation, valintaResource, targetOid, dtoAfterOperation, dtoBeforeOperation, Maps.newHashMap());
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
            return null;
        }
    }

    private static InetAddress getInetAddress(HttpServletRequest request) {
        try {
            return InetAddress.getByName(HttpServletRequestUtils.getRemoteAddress(request));
        } catch(Exception e) {
            LOG.error("Couldn't log InetAddress for log entry", e);
            return null;
        }
    }

    private static User getUser(String userOid, InetAddress ip, String session, String userAgent) {
        try {
            return new User(new Oid(userOid), ip, session, userAgent);
        } catch (GSSException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> Changes.Builder getChanges(T afterOperation, T beforeOperation) {
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

    private static void traverseAndTruncate(JsonNode data) {
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
