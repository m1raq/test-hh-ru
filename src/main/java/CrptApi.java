import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final String url;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int requestLimit;
    private final TimeUnit timeUnit;
    private final AtomicInteger requestCount;
    private final AtomicLong lastRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit;
        this.requestCount = new AtomicInteger(0);
        this.lastRequestTime = new AtomicLong(System.currentTimeMillis());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);
        api.createDocument(new Object(), "token");
    }

    public void createDocument(Object document, String signature) throws IOException, InterruptedException {
        synchronized (this) {
            long currentTime = System.currentTimeMillis();
            if (requestCount.get() >= requestLimit) {
                long waitTime = timeUnit.toMillis(requestLimit) - (currentTime - lastRequestTime.get());
                if (waitTime > 0) {
                    Thread.sleep(waitTime);
                }
                requestCount.set(0);
                lastRequestTime.set(currentTime);
            }
            requestCount.incrementAndGet();
        }

        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(document)));
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Authorization", "Bearer " + signature);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                System.out.println(EntityUtils.toString(entity));
            }
        }
    }
}