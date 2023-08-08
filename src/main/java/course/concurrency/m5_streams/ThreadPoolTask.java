package course.concurrency.m5_streams;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTask {

    // Task #1
    public ThreadPoolExecutor getLifoExecutor() {
        return new ThreadPoolExecutor(1,
                                      10,
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
        return new ThreadPoolExecutor(8,
                                      8,
                                      0L,
                                      TimeUnit.MILLISECONDS,
                                      new SynchronousQueue<>(),
                                      new ThreadPoolExecutor.DiscardPolicy()
        );
    }
}
