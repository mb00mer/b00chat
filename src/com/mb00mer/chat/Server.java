package com.mb00mer.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket( ConsoleHelper.readInt() );
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Ошибка при создании сервера");
        }
        if (serverSocket == null) {
            System.exit(1);
        }
        ConsoleHelper.writeMessage("Сервер запущен......");
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                new Handler(clientSocket).start();
            } catch (Exception e) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) { /*NOP*/}
                ConsoleHelper.writeMessage("Ошибка при создании подключения");
                break;
            }
        }
    }

    private static class Handler extends Thread{
        private Socket socket;

        private Handler(Socket socket) {
            super();
            this.socket = socket;
        }

        @Override
        public void run() {
            String userName = null;
            ConsoleHelper.writeMessage(String.format("Соединение с удаленным адресом %s установлено", socket.getRemoteSocketAddress()));
            try {
                try (final Connection connection = new Connection(socket)) {
                    // Запрашиваем имя новичка
                    userName = serverHandshake(connection);
                    // Рассылаем всем участникам имя новичка
                    Server.sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                    // Посылаем список участников чата новичку
                    sendListOfUsers(connection, userName);
                    // Главный цикл обработки
                    /////////////////////////
                    serverMainLoop(connection, userName);
                    /////////////////////////
                }
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Ошибка при обмене данными с удаленным адресом");
            }
            finally {
                if (userName != null) {
                    connectionMap.remove(userName);
                    Server.sendBroadcastMessage( new Message(MessageType.USER_REMOVED, userName) );
                }
                ConsoleHelper.writeMessage(String.format("Соединение c удаленным адресом %s закрыто", socket.getRemoteSocketAddress()));
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            if (connection == null) {
                throw new IllegalArgumentException();
            }
            String name;
            final Message requestMessage = new Message(MessageType.NAME_REQUEST);
            while (true) {
                connection.send( requestMessage );
                Message msg = connection.receive();

                if (/*(msg != null) &&*/ (msg.getType().equals(MessageType.USER_NAME))/* || (msg.getData() == null)*/) {
                    name = msg.getData().trim();
                    if (!name.isEmpty() && (!connectionMap.containsKey(name))) {
                        connectionMap.put(name, connection);
                        connection.send(new Message(MessageType.NAME_ACCEPTED));
                        break;
                    }
                }
            }
            return name;
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            if ((connection == null) || (userName == null))
                throw new IllegalArgumentException();

            for (String name : connectionMap.keySet()) {
                if (!name.equals(userName)) {
                    connection.send( new Message(MessageType.USER_ADDED, name) );
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            if ((connection == null) || (userName == null))
                throw new IllegalArgumentException();

            while (true) {
                Message msg = connection.receive();
                if (msg.getType() != MessageType.TEXT)
                    ConsoleHelper.writeMessage("Ошибка при получении сообщения: неверный тип сообщения");
                else {
                    String text = String.format("%s: %s", userName, msg.getData());
                    sendBroadcastMessage( new Message(MessageType.TEXT, text) );
                }
            }
        }
    }

    public static void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
            try {
                entry.getValue().send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage(String.format("Не удалось отправить сообщение пользователю %s", entry.getKey()));
            }
        }
    }
}
