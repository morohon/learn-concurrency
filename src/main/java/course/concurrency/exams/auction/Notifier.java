package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notifier {

    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
                                                                                         .availableProcessors());

    public void sendOutdatedMessage(Bid bid) {
        executorService.execute(this::imitateSending);
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
