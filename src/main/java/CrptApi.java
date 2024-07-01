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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int requestLimit;
    private final TimeUnit timeUnit;
    private final AtomicInteger requestCount;
    private final AtomicLong lastRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit;
        this.requestCount = new AtomicInteger(0);
        this.lastRequestTime = new AtomicLong(System.currentTimeMillis());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        Document document = crptApi.new Document();
        String signature = "example_signature";
        crptApi.createDocument("https://ismp.crpt.ru/api/v3/lk/documents/create", document, signature);
    }

    public void createDocument(String url, Object document, String signature) throws IOException, InterruptedException {
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


    public class Document {

        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        public Document(Description description, String doc_id, String doc_status, String doc_type,
                        boolean importRequest, String owner_inn, String participant_inn, String producer_inn,
                        String production_date, String production_type, List<Product> products, String reg_date,
                        String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }

        public Document() {

        }
    }

    public static class Description {
        private String participantInn;

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private final String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        public Product(String certificate_document_number, String certificate_document,
                       String certificate_document_date, String owner_inn, String producer_inn, String production_date,
                       String tnved_code, String uit_code, String uitu_code) {
            this.certificate_document_number = certificate_document_number;
            this.certificate_document = certificate_document;
            this.certificate_document_date = certificate_document_date;
            this.owner_inn = owner_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.tnved_code = tnved_code;
            this.uit_code = uit_code;
            this.uitu_code = uitu_code;
        }
    }
}