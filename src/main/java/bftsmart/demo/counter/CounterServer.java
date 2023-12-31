/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bftsmart.demo.counter;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Optional;
import java.util.Set;

/**
 * Example replica that implements a BFT replicated service (a counter).
 * If the increment > 0 the counter is incremented, otherwise, the counter
 * value is read.
 *
 * @author alysson
 */

@Slf4j
public final class CounterServer extends DefaultSingleRecoverable {

    private int counter = 0;
    private int iterations = 0;
    private final ServiceReplica replica;
    private final Set<Integer> byzantineIds = Set.of(-1);

    public CounterServer(int id) {
        replica = new ServiceReplica(id, this, this);
        log.info("Replica ID is {}", replica.getId());
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        iterations++;
        System.out.println("(" + iterations + ") Counter current value: " + counter);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            if (byzantineIds.contains(Optional.ofNullable(replica).map(ServiceReplica::getId).orElse(Integer.MIN_VALUE))) {
                Thread.sleep(3000);
                new DataOutputStream(out).writeInt(Integer.MAX_VALUE);
            } else {
                new DataOutputStream(out).writeInt(counter);
            }
            return out.toByteArray();
        } catch (IOException ex) {
            System.err.println("Invalid request received!");
            return new byte[0];
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        iterations++;
        try {
            int increment = new DataInputStream(new ByteArrayInputStream(command)).readInt();
            counter += increment;

            System.out.println("(" + iterations + ") Counter was incremented. Current value = " + counter);

            Optional.ofNullable(replica)
                    .ifPresentOrElse(r -> log.info("current leader ID = {}", r.getTomLayer().execManager.getCurrentLeader()),
                            () -> log.info("replica is still empty, cant log leader"));
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            if (byzantineIds.contains(Optional.ofNullable(replica).map(ServiceReplica::getId).orElse(Integer.MIN_VALUE))) {
                Thread.sleep(3000);
                new DataOutputStream(out).writeInt(Integer.MAX_VALUE);
            } else {
                new DataOutputStream(out).writeInt(counter);
            }
            return out.toByteArray();
        } catch (IOException ex) {
            System.err.println("Invalid request received!");
            return new byte[0];
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Use: java CounterServer <processId>");
            System.exit(-1);
        }
        new CounterServer(Integer.parseInt(args[0]));
    }


    @SuppressWarnings("unchecked")
    @Override
    public void installSnapshot(byte[] state) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(state);
            ObjectInput in = new ObjectInputStream(bis);
            counter = in.readInt();
            in.close();
            bis.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Error deserializing state: "
                    + e.getMessage());
        }
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeInt(counter);
            out.flush();
            bos.flush();
            out.close();
            bos.close();
            return bos.toByteArray();
        } catch (IOException ioe) {
            System.err.println("[ERROR] Error serializing state: "
                    + ioe.getMessage());
            return "ERROR".getBytes();
        }
    }
}
