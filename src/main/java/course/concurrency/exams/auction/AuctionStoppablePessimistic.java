package course.concurrency.exams.auction;

import java.util.concurrent.locks.ReentrantLock;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = new Bid(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
    private volatile boolean threshold = false;
    private final ReentrantLock lock = new ReentrantLock();

    public boolean propose(Bid bid) {
        if (bid.getPrice() > latestBid.getPrice()) {
            try {
                lock.lock();
                if (!threshold && bid.getPrice() > latestBid.getPrice()) {
                    notifier.sendOutdatedMessage(latestBid);
                    latestBid = bid;
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public Bid stopAuction() {
        try {
            lock.lock();
            threshold = true;
            return latestBid;
        } finally {
            lock.unlock();
        }
    }
}
