package mts.task;

import java.util.concurrent.TimeUnit;

public class Program {

    public static void main(String[] args) {

        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 10, 2);

        Thread taskThread = crptApi.startTaskCreatorThread();
        crptApi.semaphoreReleasingThread();

        for (int i = 1; i <= 1000; i++) {
            crptApi.setToSend(new Document(), "Signature " + i);
        }
    }

}
