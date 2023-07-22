package course.concurrency.m3_shared.immutable;

import java.util.Collections;
import java.util.List;

import static course.concurrency.m3_shared.immutable.Order.Status.NEW;

public class Order {

    public enum Status { NEW, IN_PROGRESS, DELIVERED;}
    private final Long id;

    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;
    public Order(Long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        this.id = id;
        this.items = Collections.unmodifiableList(items);
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public Order withPaymentInfo(PaymentInfo paymentInfo) {
        return new Order(this.id, this.items, paymentInfo, this.isPacked, this.status);
    }

    public Order withStatus(Status status) {
        if (this.status.equals(status)) {
            return this;
        } else {
            return new Order(this.id, this.items, this.paymentInfo, this.isPacked, status);
        }
    }

    public synchronized boolean checkStatus() {
        if (items != null && !items.isEmpty() && paymentInfo != null && isPacked) {
            return true;
        }
        return false;
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items;
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Order withPacked(boolean packed) {
        return new Order(this.id, this.items, this.paymentInfo, packed, Status.IN_PROGRESS);
    }

    public Status getStatus() {
        return status;
    }

}
