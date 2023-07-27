package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderService {

    private final Map<Long, Order> currentOrders = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0);

    private long nextId() {
        return nextId.incrementAndGet();
    }

    public long createOrder(List<Item> items) {
        long id = nextId();
        Order order = new Order(id, items, null, false, Order.Status.NEW);
        currentOrders.put(id, order);
        return id;
    }

    public void updatePaymentInfo(long orderId, PaymentInfo paymentInfo) {
        currentOrders.compute(orderId, (aLong, order) -> order.withPaymentInfo(paymentInfo));
        if (currentOrders.get(orderId).checkStatus()) {
            deliver(currentOrders.get(orderId));
        }
    }

    public void setPacked(long orderId) {
        currentOrders.compute(orderId, (aLong, order) -> order.withPacked(true));
        if (currentOrders.get(orderId).checkStatus()) {
            deliver(currentOrders.get(orderId));
        }
    }

    private void deliver(Order order) {
        if (!Order.Status.DELIVERED.equals(order.getStatus())) {
            currentOrders.compute(order.getId(),
                                  (aLong, findedOrder) -> findedOrder.withStatus(Order.Status.DELIVERED)
            );
        }
    }

    public boolean isDelivered(long orderId) {
        return currentOrders.get(orderId).getStatus().equals(Order.Status.DELIVERED);
    }
}
