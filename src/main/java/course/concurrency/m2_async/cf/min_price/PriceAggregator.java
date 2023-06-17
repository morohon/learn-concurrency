package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PriceAggregator {

    public static final long TIMEOUT = 2900L;
    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        ExecutorService executorService = Executors.newFixedThreadPool(shopIds.size());
        List<CompletableFuture<Double>> futures = shopIds
                .stream()
                .map(shopId -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executorService)
                        .handle((result, throwable) -> throwable == null ? result : null ))
                .collect(Collectors.toList());
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .completeOnTimeout(null, TIMEOUT, TimeUnit.MILLISECONDS)
                .thenApply(cf -> futures
                        .stream()
                        .filter(CompletableFuture::isDone)
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(Double.NaN))
                .join();
    }
}
