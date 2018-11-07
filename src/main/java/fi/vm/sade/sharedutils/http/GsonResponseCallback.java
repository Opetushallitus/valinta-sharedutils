package fi.vm.sade.valinta.sharedutils.http;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;

public class GsonResponseCallback<T> extends ExtractSuccessfullResponseCallback<T> {
    static <T>Function<String, T> jsonExtractor(Gson gson, Type type) {
        return (responseString) -> gson.fromJson(responseString, type);
    }

    public GsonResponseCallback(Gson gson, String url, Consumer<T> callback, Consumer<Throwable> failureCallback, Type type) {
        super(url, callback, failureCallback, jsonExtractor(gson, type));
    }
}
