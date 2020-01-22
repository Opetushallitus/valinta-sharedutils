package fi.vm.sade.valinta.sharedutils.http;

import com.google.gson.*;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    @Test
    public void serializesDate() {
        Gson GSON = DateDeserializer
                .gsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .create();
        final String str = GSON.toJson(new DateObject(new Date(1579680865000L)), DateObject.class);
        Assert.assertNotNull(str);
        Assert.assertEquals("{\"date\":\"2020-01-22T10:14:25Z\"}", str);
    }

    private static class DateObject {
        private Date date;

        public DateObject(Date date) {
            this.date = date;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}
