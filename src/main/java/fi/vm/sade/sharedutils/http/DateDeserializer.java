package fi.vm.sade.valinta.sharedutils.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Date;

public class DateDeserializer {
    public static final Gson GSON = gsonBuilder().create();

    public static GsonBuilder gsonBuilder() {
        return DateDeserializer.register(new GsonBuilder());
    }
    public static GsonBuilder register(GsonBuilder gsonBuilder) {
        return gsonBuilder
            .registerTypeAdapter(Date.class, createDateDeserializer())
            .registerTypeAdapter(LocalDate.class, createLocalDateDeserializer());
    }

    private static JsonDeserializer createDateDeserializer() {
        return (json, typeOfT, context) -> {
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if(primitive.isString()) {
                try {
                    return new SimpleDateFormat("yyyy-MM-dd").parse(primitive.getAsString());
                } catch (Throwable t) {
                    throw new JsonParseException(t);
                }
            } else {
                return new Date(primitive.getAsLong());
            }
        };
    }

    private static JsonDeserializer createLocalDateDeserializer() {
        return (json, typeOfT, context) -> {
            if (!json.isJsonPrimitive()) {
                throw new JsonSyntaxException("Expected a primitive field but got " + json);
            }
            JsonPrimitive primitive = json.getAsJsonPrimitive();
            if (!primitive.isString()) {
                throw new JsonSyntaxException("Expected a string but got " + primitive);
            }
            String string = primitive.getAsString().trim();
            if (string.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(string);
            } catch (DateTimeParseException e1) {
                throw new JsonParseException(e1);
            }
        };
    }
}
