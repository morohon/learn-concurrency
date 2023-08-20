package course.concurrency.exams.refactoring;

import org.apache.commons.math3.util.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MountTableRefresherServiceTests {

    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager manager;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = new MountTableRefresherService();
        service.setCacheUpdateTimeout(1000);
        routerStore = mock(Others.RouterStore.class);
        manager = mock(Others.MountTableManager.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
        // service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
        // service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        MountTableRefresherService mockedService = Mockito.spy(service);
        when(manager.refresh()).thenReturn(true);

        List<String> addresses = List.of("123", "local6", "789", "local");
        Queue<Others.UpdateTask> tasks = new LinkedList<>();
        addresses.forEach(address -> tasks.offer(new Others.UpdateTask(manager, address)));

        when(mockedService.getUpdateTask(anyString())).thenAnswer(inv -> tasks.poll());

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        MountTableRefresherService mockedService = Mockito.spy(service);

        List<String> addresses = List.of("123", "local6", "789", "local");
        Queue<Others.UpdateTask> tasks = new LinkedList<>();
        addresses.forEach(address -> tasks.offer(new Others.UpdateTask(manager, address)));

        when(mockedService.getUpdateTask(anyString())).thenAnswer(inv -> tasks.poll());

        when(manager.refresh()).thenReturn(false);
        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        addresses.forEach(addr -> verify(routerClientsCache).invalidate(addr));
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSuccessedTasks() {
        MountTableRefresherService mockedService = Mockito.spy(service);

        List<String> addresses = List.of("123", "local6", "789", "local");

        Others.MountTableManager successManager = mock(Others.MountTableManager.class);
        when(successManager.refresh()).thenReturn(true);
        Others.MountTableManager failedManager = mock(Others.MountTableManager.class);
        when(failedManager.refresh()).thenReturn(false);

        Queue<Pair<String, Others.MountTableManager>> tasks = new LinkedList<>();
        tasks.offer(new Pair<>(addresses.get(0), successManager));
        tasks.offer(new Pair<>(addresses.get(1), failedManager));
        tasks.offer(new Pair<>(addresses.get(2), successManager));
        tasks.offer(new Pair<>(addresses.get(3), failedManager));

        when(mockedService.getUpdateTask(anyString())).thenAnswer(inv -> {
            Pair<String, Others.MountTableManager> poll = tasks.poll();
            return new Others.UpdateTask(poll.getSecond(), poll.getFirst());
        });

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache).invalidate(addresses.get(1));
        verify(routerClientsCache).invalidate(addresses.get(3));
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        MountTableRefresherService mockedService = Mockito.spy(service);

        List<String> addresses = List.of("123", "local6", "789", "local");

        Others.MountTableManager successManager = mock(Others.MountTableManager.class);
        when(successManager.refresh()).thenReturn(true);
        Others.MountTableManager exceptionallyManager = mock(Others.MountTableManager.class);
        when(exceptionallyManager.refresh()).thenThrow(new RuntimeException());

        Queue<Pair<String, Others.MountTableManager>> tasks = new LinkedList<>();
        tasks.offer(new Pair<>(addresses.get(0), successManager));
        tasks.offer(new Pair<>(addresses.get(1), exceptionallyManager));
        tasks.offer(new Pair<>(addresses.get(2), successManager));
        tasks.offer(new Pair<>(addresses.get(3), successManager));

        when(mockedService.getUpdateTask(anyString())).thenAnswer(inv -> {
            Pair<String, Others.MountTableManager> poll = tasks.poll();
            return new Others.UpdateTask(poll.getSecond(), poll.getFirst());
        });

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("java.util.concurrent.CompletionException: java.lang.RuntimeException");
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache).invalidate(addresses.get(1));
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        MountTableRefresherService mockedService = Mockito.spy(service);

        List<String> addresses = List.of("123", "local6", "789", "local");

        Others.MountTableManager successManager = mock(Others.MountTableManager.class);
        when(successManager.refresh()).thenReturn(true);
        Others.MountTableManager timeoutManager = mock(Others.MountTableManager.class);
        when(timeoutManager.refresh()).thenAnswer(inv -> {
            Thread.sleep(2000);
            return true;
        });

        Queue<Pair<String, Others.MountTableManager>> tasks = new LinkedList<>();
        tasks.offer(new Pair<>(addresses.get(0), successManager));
        tasks.offer(new Pair<>(addresses.get(1), timeoutManager));
        tasks.offer(new Pair<>(addresses.get(2), successManager));
        tasks.offer(new Pair<>(addresses.get(3), successManager));

        when(mockedService.getUpdateTask(anyString()))
                .thenAnswer(inv -> {
                    Pair<String, Others.MountTableManager> poll = tasks.poll();
                    return new Others.UpdateTask(poll.getSecond(), poll.getFirst());
                });

        List<Others.RouterState> states = addresses.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache).invalidate(addresses.get(1));
    }

}
