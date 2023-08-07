package course.concurrency.m5_streams;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTask {

    // Task #1
    public ThreadPoolExecutor getLifoExecutor() {
        return new ThreadPoolExecutor(1,
                                      1,
                                      0L,
                                      TimeUnit.MILLISECONDS,
                                      new LinkedBlockingDeque<>() {
                                          @Override
                                          public boolean offer(Runnable runnable) {
                                              return super.offerFirst(runnable);
                                          }
                                      }
        );
    }

    // Task #2
    public ThreadPoolExecutor getRejectExecutor() {
        return new ThreadPoolExecutor(7,
                                      7,
                                      0L,
                                      TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<>(1),
                                      new ThreadPoolExecutor.DiscardPolicy()
        );
    }
}
