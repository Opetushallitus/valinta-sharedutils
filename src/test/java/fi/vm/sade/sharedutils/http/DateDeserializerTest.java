package fi.vm.sade.valinta.sharedutils.http;

import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.Test;

public class DateDeserializerTest {

    @Test
    public void testDateDeserializer() {
        Type type = new TypeToken<Map<String, Date>>(){}.getType();
        Gson GSON = DateDeserializer.gsonBuilder().create();

        String json = "{'date1': 12345678, 'date2': '2017-02-03'}";

        Map<String, Date> jee = GSON.fromJson(json, type);
        for(Map.Entry<String, Date> entry : jee.entrySet()) {
            Assert.assertNotNull(entry.getValue());
        }
    }

}
