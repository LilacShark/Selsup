package mts.task;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Program {

    public static void main(String[] args) {

        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10, 2);

        Thread taskThread = crptApi.startTaskCreatorThread();
        crptApi.semaphoreReleasingThread();

        ArrayList<Product> products = new ArrayList<>();
        products.add(new Product("certificate_document",
                "certificate_document_number",
                "owner_inn", "producer_inn",
                "tnved_code", "uit_code", "uitu_code"));

        Document document = new Document(new Description("participantInn"),
                "doc_id",
                "doc_status",
                DocType.LP_INTRODUCE_GOODS,
                true,
                "owner_inn",
                "participant_inn",
                "producer_inn",
                "production_type",
                products,
                "reg_number");

        for (int i = 1; i <= 1000; i++) {
            crptApi.setToSend(document, "Signature " + i);
        }
    }

}
