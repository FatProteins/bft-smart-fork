package bftsmart.demo.map;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MapConcurrentClient {

    public static void main(String[] args) {
        var levels = new ArrayList<Integer>();
        var keyCounter = new AtomicInteger();
        var clientCounter = new AtomicInteger();
        for (var concurrentClients = 1; concurrentClients <= 131072; concurrentClients *= 2) {
            levels.add(concurrentClients);
        }
        var value = "0";

        for (var concurrentClients : levels) {
            var requests = new ConcurrentLinkedQueue<Request>();
            var latch = new CountDownLatch(concurrentClients + 1);
            var exitThreads = new AtomicBoolean();

            writeRequestsToFile(requests, latch, exitThreads, concurrentClients);
            System.out.println("Starting concurrency level " + concurrentClients);
            for (var i = 0; i < concurrentClients; i++) {
                Thread.ofVirtual()
                        .start(() -> {
                            var client = new MapClient<String, String>(clientCounter.getAndIncrement());
                            while (!exitThreads.get()) {
                                var key = Integer.toString(keyCounter.getAndIncrement());
                                var startTime = System.nanoTime();
                                var result = client.put(key, value);
                                var endTime = System.nanoTime();
                                requests.add(new Request(key, value, result != null, startTime, endTime));
                            }

                            client.close();
                            latch.countDown();
                        });
            }

            try {
                Thread.sleep(Duration.ofSeconds(34));
                exitThreads.set(true);
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("Done with performance test");
    }

    private static void writeRequestsToFile(ConcurrentLinkedQueue<Request> requestChannel, CountDownLatch latch, AtomicBoolean exit, int numberOfClients) {
        var t = Thread.ofPlatform()
                .start(() -> {
                    var file = new File("/thesis/out/%d-bftsmart_%s.csv".formatted(numberOfClients, LocalDateTime.now().toString()));
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
