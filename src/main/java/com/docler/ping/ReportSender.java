package com.docler.ping;

import com.docler.ping.model.ReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

import static com.docler.ping.ConfigUtils.readStringFromConfig;

public class ReportSender {
    private final static CloseableHttpClient httpclient = HttpClients.createDefault();
    private final static ObjectMapper mapper = new ObjectMapper();


    public static void sendReport(ReportRequest request) throws IOException {
        StringEntity requestEntity = new StringEntity(
                mapper.writeValueAsString(request),
                ContentType.APPLICATION_JSON);
        HttpPost postMethod = new HttpPost(readStringFromConfig("REPORT_SERVER"));
        postMethod.setEntity(requestEntity);
        httpclient.execute(postMethod);
    }
}
