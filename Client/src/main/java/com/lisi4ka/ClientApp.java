package com.lisi4ka;

import com.lisi4ka.utils.CommandMap;
import com.lisi4ka.utils.PackagedCommand;
import com.lisi4ka.utils.PackagedResponse;

import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;


public class ClientApp {
    static long timeOut = currentTimeMillis();
    static boolean serverWork = true;
    static Queue<ByteBuffer> queue = new LinkedList<>();
    public static CommandMap commandMap = null;
    static boolean connectionAccepted = true;
    private void run() {
        try {
            while (true) {
                boolean doneStatus = true;
                serverWork = true;
                InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("localhost"), 9856);
                Selector selector = Selector.open();
                SocketChannel sc = SocketChannel.open();
                sc.configureBlocking(false);
                sc.connect(addr);
                sc.register(selector, SelectionKey.OP_CONNECT |
                        SelectionKey.OP_READ | SelectionKey.
                        OP_WRITE);
                while (true) {
                    if (selector.select() > 0) {
                        try {
                            doneStatus = processReadySet(selector.selectedKeys());

                        } catch (ConnectException ex) {
                            System.out.println("Lost server connection. Repeat connecting in 10 seconds");
                            break;
                        }
                        if (doneStatus || !serverWork) {
                            break;
                        }
                    }
                }
                if (doneStatus) {
                    break;
                }
                sc.close();
                System.out.println("Lost server connection. Repeat connecting in 10 seconds");
                connectionAccepted = true;
                sleep(10000);
                timeOut = currentTimeMillis();
            }
        } catch (Exception ex) {
            System.out.println("This port is already in use!");
        }
    }

        public static Boolean processReadySet (Set readySet)
            throws Exception {

            SelectionKey key = null;
            Iterator iterator = readySet.iterator();
            while (iterator.hasNext()) {
                key = (SelectionKey) iterator.next();
                iterator.remove();
            }
            assert key != null;
            if (key.isConnectable()) {
                boolean connected = false;
                try {
                    connected = processConnect(key);
                } catch (Exception e) {
                    System.out.println("Lost server connection");
                }
                if (!connected) {
                    return false;
                }
                if (connectionAccepted) {
                    System.out.println("Connection Accepted");
                }
            }
            if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer bb = ByteBuffer.allocate(8192);
                try {
                    sc.read(bb);
                } catch (Exception e) {
                    serverWork = false;
                    return false;
                }
                String result = new String(bb.array()).trim();
                byte[] data = Base64.getDecoder().decode(result);
                PackagedResponse packagedResponse = null;
                try {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                    packagedResponse = (PackagedResponse) ois.readObject();
                    ois.close();
                } catch (EOFException ignored) {}
                assert packagedResponse != null;
                if (packagedResponse.getMessage() != null) {
                    System.out.println(packagedResponse.getMessage());
                } else {
                    commandMap = packagedResponse.getCommandMap();
                }
            }
            if (key.isWritable() && timeOut < (currentTimeMillis() - 300)) {
                SocketChannel socketChannel = (SocketChannel) key.channel();
                if (queue.isEmpty()) {
                    for (PackagedCommand packagedCommand : ClientValidation.validation()) {
                        if ("exit".equals(packagedCommand.getCommandName())) {
                            return true;
                        }
                        ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
                        ObjectOutputStream serializeObject = new ObjectOutputStream(stringOut);
                        serializeObject.writeObject(packagedCommand);
                        String serializeCommand = Base64.getEncoder().encodeToString(stringOut.toByteArray());
                        ByteBuffer byteBuffer = ByteBuffer.wrap(serializeCommand.getBytes());
                        queue.add(byteBuffer);
                    }
                }
                if (!queue.isEmpty()) {
                    try {
                        socketChannel.write(queue.poll());
                        timeOut = currentTimeMillis();
                    } catch (Exception e) {
                        System.out.println("Error while sending message!");
                        connectionAccepted = false;
                    }
                }
                return false;
            }
            return false;
        }
    public static Boolean processConnect(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            while (sc.isConnectionPending()) {
                sc.finishConnect();
            }
        } catch (IOException e) {
            key.cancel();
            serverWork = false;
            return false;
        }
        return true;
    }
    public static void clientRun() {
        ClientApp clientApp = new ClientApp();
        clientApp.run();
    }
}