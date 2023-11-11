package bftsmart.demo.map;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MapPerformanceClient {

    public static void main(String[] args) {
        var keyCounter = new AtomicInteger();
        var clientCounter = new AtomicInteger();

        if (args.length != 3) {
            System.out.println("Expected 3 program arguments, got " + args.length);
            System.exit(1);
        }

        var concurrentClients = Integer.parseInt(args[0]);
        var markerKey = args[1];
        var markerValue = args[2];

        var requests = new ConcurrentLinkedQueue<Request>();
        var latch = new CountDownLatch(concurrentClients + 1);
        var exitThreads = new AtomicBoolean();

        var writeThread = writeRequestsToFile(requests, latch, exitThreads, concurrentClients, markerKey, markerValue);
        System.out.println("Performance test with clients: " + concurrentClients);
        System.out.println("Marker Key: " + markerKey);
        System.out.println("Marker Value:" + markerValue);
        for (var i = 0; i < concurrentClients; i++) {
            Thread.ofVirtual()
                    .start(() -> {
                        var clientId = clientCounter.getAndIncrement();
                        var client = new MapClient<String, String>(clientId);
                        var injectedFault = false;
                        var injectedEnd = false;
                        var value = markerValue;

                        var experimentStart = LocalTime.now();
                        var injectStart = experimentStart.plusSeconds(35);
                        var injectEnd = injectStart.plusSeconds(15);

                        while (!exitThreads.get()) {
                            var key = Integer.toString(keyCounter.getAndIncrement());
                            var startTime = System.nanoTime();

                            if (clientId == 0 && !injectedFault && LocalTime.now().isAfter(injectStart)) {
                                injectedFault = true;
                                key = markerKey;
                            }

                            if (clientId == 0 && Objects.equals(markerValue, "S") && !injectedEnd && LocalTime.now().isAfter(injectEnd)) {
                                injectedEnd = true;
                                key = markerKey;
                                value = "U";
                            }

                            var result = client.put(key, value);
                            var endTime = System.nanoTime();
                            requests.add(new Request(key, value, result != null, startTime, endTime));
                        }

                        client.close();
                        latch.countDown();
                    });
        }

        try {
            Thread.sleep(Duration.ofSeconds(90));
            exitThreads.set(true);
            latch.await();
            writeThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Done with performance test");
    }

    private static Thread writeRequestsToFile(ConcurrentLinkedQueue<Request> requestChannel, CountDownLatch latch, AtomicBoolean exit, int numberOfClients, String markerKey, String markerValue) {
        return Thread.ofPlatform()
                .start(() -> {
                    var file = new File("/thesis/out/%d-%s-%s-bftsmart_%s.csv".formatted(numberOfClients, markerKey, markerValue, LocalDateTime.now().toString()));
                    BufferedWriter writer;
                    try {
                        writer = new BufferedWriter(new FileWriter(file));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        writer.write("key,value,success,timestampStart,timestampEnd\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try {
                        while (true) {
                            var nextRequest = requestChannel.poll();
                            if (nextRequest == null) {
                                if (exit.get()) {
                                    writer.close();
                                    latch.countDown();
                                    return;
                                }
                            } else {
                                writer.write("%s,%s,%s,%d,%d\n".formatted(nextRequest.key, nextRequest.value, Boolean.toString(nextRequest.success), nextRequest.timestampStart, nextRequest.timestampEnd));
                            }
                        }
                    } catch (IOException e) {
                        try {
                            writer.close();
                        } catch (IOException ex) {
                        }
                        throw new RuntimeException(e);
                    }
                });
    }

    static class Request {
        final String key;
        final String value;
        final boolean success;
        final long timestampStart;
        final long timestampEnd;

        Request(String key, String value, boolean success, long timestampStart, long timestampEnd) {
            this.key = key;
            this.value = value;
            this.success = success;
            this.timestampStart = timestampStart;
            this.timestampEnd = timestampEnd;
        }
    }

}
