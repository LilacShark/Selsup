package mts.task;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class CrptApi {

    private final String URL = "http://localhost:8080/api/documents/create";
    private final TimeUnit unit;
    private final int requestLimit;
    private final ExecutorService executorService;
    private final BlockingQueue<RequestData> sendingQueue;
    private final Semaphore semaphore;

    public CrptApi(TimeUnit unit, int requestLimit, int threadAmount) {
        this.unit = unit;
        this.requestLimit = requestLimit;
        sendingQueue = new ArrayBlockingQueue<RequestData>(requestLimit);
        executorService = Executors.newFixedThreadPool(threadAmount);
        semaphore = new Semaphore(requestLimit);
    }

    public synchronized void setToSend(Document document, String signature) {
        try {
            sendingQueue.put(new RequestData(document, signature));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void semaphoreReleasingThread() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() ->
                                semaphore.release(requestLimit - semaphore.availablePermits()),
                        1, 1, unit);
    }

    public Thread startTaskCreatorThread() {

        Runnable dataSendingTask = () -> {
            while (!Thread.interrupted()) {

                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                RequestData requestData;
                try {
                    requestData = sendingQueue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                Runnable taskToSend = () -> {
                    httpSendDocument(requestData);
                };

                executorService.execute(taskToSend);
            }
        };

        Thread taskThread = new Thread(dataSendingTask);
        taskThread.start();
        return taskThread;
    }

    private void httpSendDocument(RequestData requestData) {

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
                .create();

        String requestBody = gson.toJson(requestData.document());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .header("Signature", requestData.signature())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpClient client = HttpClient.newBuilder().build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            SomeResponse someResponse = gson.fromJson(response.body(), SomeResponse.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // дальнейшая обработка респонза
        
    }

}

record RequestData(Document document, String signature) {

}


class Document {
    private Description description;
    private String doc_id;
    private String doc_status;
    private DocType doc_type;
    private boolean importRequest;
    private String owner_inn;
    private String participant_inn;
    private String producer_inn;
    private LocalDate production_date = LocalDate.of(2020, 1, 23);
    private String production_type;
    private List<Product> products;
    private LocalDate reg_date = LocalDate.of(2020, 1, 23);
    private String reg_number;

    public Document() {
        this.description = new Description();
        this.doc_id = "doc_id";
        this.doc_status = "doc_status";
        this.doc_type = DocType.LP_INTRODUCE_GOODS;
        this.importRequest = true;
        this.owner_inn = "owner_inn";
        this.participant_inn = "participant_inn";
        this.producer_inn = "producer_inn";
        this.production_type = "production_type";
        this.products = new ArrayList<>();
        this.reg_number = "reg_number";
        products.add(new Product());
    }

    public Description getDescription() {
        return description;
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    public String getDoc_id() {
        return doc_id;
    }

    public void setDoc_id(String doc_id) {
        this.doc_id = doc_id;
    }

    public String getDoc_status() {
        return doc_status;
    }

    public void setDoc_status(String doc_status) {
        this.doc_status = doc_status;
    }

    public DocType getDoc_type() {
        return doc_type;
    }

    public void setDoc_type(DocType doc_type) {
        this.doc_type = doc_type;
    }

    public boolean isImportRequest() {
        return importRequest;
    }

    public void setImportRequest(boolean importRequest) {
        this.importRequest = importRequest;
    }

    public String getOwner_inn() {
        return owner_inn;
    }

    public void setOwner_inn(String owner_inn) {
        this.owner_inn = owner_inn;
    }

    public String getParticipant_inn() {
        return participant_inn;
    }

    public void setParticipant_inn(String participant_inn) {
        this.participant_inn = participant_inn;
    }

    public String getProducer_inn() {
        return producer_inn;
    }

    public void setProducer_inn(String producer_inn) {
        this.producer_inn = producer_inn;
    }

    public LocalDate getProduction_date() {
        return production_date;
    }

    public void setProduction_date(LocalDate production_date) {
        this.production_date = production_date;
    }

    public String getProduction_type() {
        return production_type;
    }

    public void setProduction_type(String production_type) {
        this.production_type = production_type;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public LocalDate getReg_date() {
        return reg_date;
    }

    public void setReg_date(LocalDate reg_date) {
        this.reg_date = reg_date;
    }

    public String getReg_number() {
        return reg_number;
    }

    public void setReg_number(String reg_number) {
        this.reg_number = reg_number;
    }
}

enum DocType {
    LP_INTRODUCE_GOODS
}

class Description {
    private String participantInn;

    public Description() {
        this.participantInn = "participantInn";
    }

    public String getParticipantInn() {
        return participantInn;
    }

    public void setParticipantInn(String participantInn) {
        this.participantInn = participantInn;
    }
}

class Product {
    private String certificate_document;
    private LocalDate certificate_document_date = LocalDate.of(2020, 01, 23);
    private String certificate_document_number;
    private String owner_inn;
    private String producer_inn;
    private LocalDate production_date = LocalDate.of(2020, 01, 23);
    private String tnved_code;
    private String uit_code;
    private String uitu_code;

    public Product() {
        this.certificate_document = "certificate_document";
        this.certificate_document_number = "certificate_document_number";
        this.owner_inn = "owner_inn";
        this.producer_inn = "producer_inn";
        this.tnved_code = "tnved_code";
        this.uit_code = "uit_code";
        this.uitu_code = "uitu_code";
    }

    public void setCertificate_document(String certificate_document) {
        this.certificate_document = certificate_document;
    }

    public void setCertificate_document_date(LocalDate certificate_document_date) {
        this.certificate_document_date = certificate_document_date;
    }

    public void setCertificate_document_number(String certificate_document_number) {
        this.certificate_document_number = certificate_document_number;
    }

    public void setOwner_inn(String owner_inn) {
        this.owner_inn = owner_inn;
    }

    public void setProducer_inn(String producer_inn) {
        this.producer_inn = producer_inn;
    }

    public void setProduction_date(LocalDate production_date) {
        this.production_date = production_date;
    }

    public void setTnved_code(String tnved_code) {
        this.tnved_code = tnved_code;
    }

    public void setUit_code(String uit_code) {
        this.uit_code = uit_code;
    }

    public void setUitu_code(String uitu_code) {
        this.uitu_code = uitu_code;
    }

    public String getCertificate_document() {
        return certificate_document;
    }

    public LocalDate getCertificate_document_date() {
        return certificate_document_date;
    }

    public String getCertificate_document_number() {
        return certificate_document_number;
    }

    public String getOwner_inn() {
        return owner_inn;
    }

    public String getProducer_inn() {
        return producer_inn;
    }

    public LocalDate getProduction_date() {
        return production_date;
    }

    public String getTnved_code() {
        return tnved_code;
    }

    public String getUit_code() {
        return uit_code;
    }

    public String getUitu_code() {
        return uitu_code;
    }
}

class SomeResponse {

    private String state;

    public LocalDate responseDate;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDate getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(LocalDate responseDate) {
        this.responseDate = responseDate;
    }

    @Override
    public String toString() {
        return "SomeResponse{" +
                "state='" + state + '\'' +
                ", responseDate=" + responseDate +
                '}';
    }
}

class LocalDateAdapter extends TypeAdapter<LocalDate> {
    @Override
    public void write(final JsonWriter jsonWriter, final LocalDate localDate ) throws IOException {
        jsonWriter.value(localDate.toString());
    }

    @Override
    public LocalDate read( final JsonReader jsonReader ) throws IOException {
        return LocalDate.parse(jsonReader.nextString());
    }
}