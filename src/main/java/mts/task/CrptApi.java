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
            System.out.println(someResponse);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // дальнейшая обработка респонза
        
    }

}

record RequestData(Document document, String signature) {

}


class Document {
    private final Description description;
    private final String doc_id;
    private final String doc_status;
    private final DocType doc_type;
    private final boolean importRequest;
    private final String owner_inn;
    private final String participant_inn;
    private final String producer_inn;
    private final LocalDate production_date = LocalDate.of(2020, 1, 23);
    private final String production_type;
    private final List<Product> products;
    private final LocalDate reg_date = LocalDate.of(2020, 1, 23);
    private final String reg_number;

    public Document(Description description, String doc_id, String doc_status, DocType doc_type, boolean importRequest,
                    String owner_inn, String participant_inn, String producer_inn, String production_type,
                    List<Product> products, String reg_number) {
        this.description = description;
        this.doc_id = doc_id;
        this.doc_status = doc_status;
        this.doc_type = doc_type;
        this.importRequest = importRequest;
        this.owner_inn = owner_inn;
        this.participant_inn = participant_inn;
        this.producer_inn = producer_inn;
        this.production_type = production_type;
        this.products = products;
        this.reg_number = reg_number;
    }

    public Description getDescription() {
        return description;
    }

    public String getDoc_id() {
        return doc_id;
    }

    public String getDoc_status() {
        return doc_status;
    }

    public DocType getDoc_type() {
        return doc_type;
    }

    public boolean isImportRequest() {
        return importRequest;
    }

    public String getOwner_inn() {
        return owner_inn;
    }

    public String getParticipant_inn() {
        return participant_inn;
    }

    public String getProducer_inn() {
        return producer_inn;
    }

    public LocalDate getProduction_date() {
        return production_date;
    }

    public String getProduction_type() {
        return production_type;
    }

    public List<Product> getProducts() {
        return products;
    }

    public LocalDate getReg_date() {
        return reg_date;
    }

    public String getReg_number() {
        return reg_number;
    }
}

enum DocType {
    LP_INTRODUCE_GOODS
}

class Description {
    private final String participantInn;

    public Description(String participantInn) {
        this.participantInn = participantInn;
    }

    public String getParticipantInn() {
        return participantInn;
    }
}

class Product {
    private final String certificate_document;
    private final LocalDate certificate_document_date = LocalDate.of(2020, 1, 23);
    private final String certificate_document_number;
    private final String owner_inn;
    private final String producer_inn;
    private final LocalDate production_date = LocalDate.of(2020, 1, 23);
    private final String tnved_code;
    private final String uit_code;
    private final String uitu_code;

    public Product(String certificate_document, String certificate_document_number, String owner_inn,
                   String producer_inn, String tnved_code, String uit_code, String uitu_code) {
        this.certificate_document = certificate_document;
        this.certificate_document_number = certificate_document_number;
        this.owner_inn = owner_inn;
        this.producer_inn = producer_inn;
        this.tnved_code = tnved_code;
        this.uit_code = uit_code;
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

    private final String state;

    private final LocalDate responseDate;

    public SomeResponse(String state, LocalDate responseDate) {
        this.state = state;
        this.responseDate = responseDate;
    }

    @Override
    public String toString() {
        return "SomeResponse{" +
                "state='" + state + '\'' +
                ", responseDate=" + responseDate +
                '}';
    }

    public String getState() {
        return state;
    }

    public LocalDate getResponseDate() {
        return responseDate;
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