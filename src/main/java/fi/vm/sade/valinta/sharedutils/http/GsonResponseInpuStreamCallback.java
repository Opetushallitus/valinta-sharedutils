package fi.vm.sade.valinta.sharedutils.http;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.function.Consumer;
import java.util.function.Function;

public class GsonResponseInpuStreamCallback<T> extends ExtractSuccessfullResponseFromStreamCallback<T> {
    static <T>Function<InputStream, T> jsonExtractor(Gson gson, Type type) {
        return responseStream -> {
            try {
                return gson.fromJson(new InputStreamReader(responseStream), type);
            } finally {
                IOUtils.closeQuietly(responseStream);
            }
        };
    }

    public GsonResponseInpuStreamCallback(Gson gson, String url, Consumer<T> callback, Consumer<Throwable> failureCallback, Type type) {
        super(url, callback, failureCallback, jsonExtractor(gson, type));
    }
}
