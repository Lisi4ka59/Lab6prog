package com.lisi4ka.common;

import com.lisi4ka.utils.*;
import com.lisi4ka.validation.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

import static com.lisi4ka.utils.CommandMap.byteBufferLimit;

public class ServerApp {
    public static CityLinkedList cities = new CityLinkedList();
    private CommandMap createCommandMap(){
        CommandMap commandMap = new CommandMap();
        commandMap.put("add", new AddValid());
        commandMap.put("add_if_min", new AddIfMinValid());
        commandMap.put("clear", new ClearValid());
        commandMap.put("execute_script", new ExecuteScriptValid());
        commandMap.put("exit", new ExitValid());
        commandMap.put("help", new HelpValid());
        commandMap.put("info", new InfoValid());
        commandMap.put("print_descending", new PrintDescendingValid());
        commandMap.put("print_field_ascending_standard_of_living", new PrintFieldAscendingStandardOfLivingValid());
        commandMap.put("print_unique_standard_of_living", new PrintUniqueStandardOfLivingValid());
        commandMap.put("remove_by_id", new RemoveByIdValid());
        commandMap.put("remove_first", new RemoveFirstValid());
        commandMap.put("remove_head", new RemoveHeadValid());
        commandMap.put("show", new ShowValid());
        commandMap.put("update", new UpdateIdValid());
        return commandMap;
    }
    private void run(){
        try {
            System.out.println("Server started");
            Invoker invoker = new Invoker(cities);
            Queue<String> queue = new LinkedList<>();
            queue.add(invoker.run("load"));
            InetAddress host = InetAddress.getByName("localhost");
            Selector selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(host, 9857));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            SelectionKey key;
            while (true) {
                if (selector.select() <= 0)
                    continue;
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    key = iterator.next();
                assert key != null;
                if (key.isValid() && key.isAcceptable()) {
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    System.out.println("Connection Accepted: " + sc.getRemoteAddress());
                    ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
                    ObjectOutputStream serializeObject = new ObjectOutputStream(stringOut);
                    PackagedResponse packagedResponse = new PackagedResponse(createCommandMap());
                    serializeObject.writeObject(packagedResponse);
                    String serializeCommand = Base64.getEncoder().encodeToString(stringOut.toByteArray());
                    ByteBuffer byteBuffer = ByteBuffer.wrap(serializeCommand.getBytes());
                    sc.write(byteBuffer);
                }
                if (key.isValid() && key.isReadable()) {
                    SocketChannel sc = (SocketChannel) key.channel();
                    ByteBuffer bb = ByteBuffer.allocate(byteBufferLimit);
                    boolean flag = true;
                    try {
                        sc.read(bb);
                    } catch (SocketException | EOFException ex) {
                        System.out.printf("Client %s close connection!\nServer will keep running\nTry running client again to re-establish connection\n", sc.getRemoteAddress().toString());
                        sc.close();
                        flag = false;
                    }
                    if (flag) {
                        String result = new String(bb.array()).trim();
                        byte[] data = Base64.getDecoder().decode(result);
                        PackagedCommand packagedCommand = null;
                        try {
                            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                            packagedCommand = (PackagedCommand) ois.readObject();
                            ois.close();
                        } catch (EOFException ignored) {}
                        String answer;
                        if (packagedCommand != null) {
                            if (packagedCommand.getCommandArguments() == null) {
                                answer = invoker.run(packagedCommand.getCommandName());
                            } else {
                                answer = invoker.run(packagedCommand);
                            }
                            queue.add(answer);
                        }
                    }
                }
                if (key.isValid() && key.isWritable() && !queue.isEmpty()) {
                    String answer = queue.poll();
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
                    ObjectOutputStream serializeObject = new ObjectOutputStream(stringOut);
                    PackagedResponse packagedResponse = new PackagedResponse(answer, ResponseStatus.OK);
                    serializeObject.writeObject(packagedResponse);
                    String serializeCommand = Base64.getEncoder().encodeToString(stringOut.toByteArray());
                    int packageCount = serializeCommand.getBytes().length / byteBufferLimit +
                            serializeCommand.getBytes().length % byteBufferLimit==0?0:1;
                    PackagedResponse firstPackagedResponse = new PackagedResponse(packageCount, ResponseStatus.BigCommand);
                    serializeObject.writeObject(firstPackagedResponse);
                    String firstSerialize = Base64.getEncoder().encodeToString(stringOut.toByteArray());
                    ByteBuffer byteBuff = ByteBuffer.wrap(firstSerialize.getBytes());
                    socketChannel.write(byteBuff);
                    var data = serializeCommand.getBytes();
                    for (int a=0; a <  data.length; a += byteBufferLimit){
                        ByteBuffer byteBuffer;
                        if (a+byteBufferLimit > data.length){
                            byteBuffer = ByteBuffer.wrap(Arrays.copyOfRange(data, a, data.length-1));
                        } else {
                            byteBuffer = ByteBuffer.wrap(Arrays.copyOfRange(data, a, a + byteBufferLimit));
                        }
                        socketChannel.write(byteBuffer);
                    }
                }
                    iterator.remove();
                }
            }
        }catch (Exception ex){
            System.out.println("This port is already in use!");
        }
    }
    public static void serverRun() {
        ServerApp serverApp = new ServerApp();
        serverApp.run();
    }
}