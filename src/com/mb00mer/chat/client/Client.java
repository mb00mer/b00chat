 package com.mb00mer.chat.client;

 import com.mb00mer.chat.*;
 import java.io.IOException;
 import java.net.Socket;

 public class Client {
     protected Connection connection;
     private volatile boolean clientConnected = false;

     public class SocketThread extends Thread {
         protected void processIncomingMessage(String message) {
             ConsoleHelper.writeMessage(message);
         }

         protected void informAboutAddingNewUser(String userName) {
             ConsoleHelper.writeMessage(String.format("Участник с именем '%s' присоединился к чату", userName));
         }

         protected void informAboutDeletingNewUser(String userName) {
             ConsoleHelper.writeMessage(String.format("Участник с именем '%s' покинул чат", userName));
         }

         protected void notifyConnectionStatusChanged(boolean clientConnected) {
             Client.this.clientConnected = clientConnected;
             synchronized (Client.this) {
                 Client.this.notify();
             }
         }

         protected void clientHandshake() throws IOException, ClassNotFoundException {
             Connection connection = Client.this.connection;
             while (true) {
                 Message message = connection.receive();
                 if (message == null)
                     continue;
                 if (message.getType() == MessageType.NAME_REQUEST) {
                     String userName = Client.this.getUserName();
                     connection.send( new Message(MessageType.USER_NAME, userName) );
                 }
                 else if (message.getType() == MessageType.NAME_ACCEPTED) {
                     notifyConnectionStatusChanged(true);
                     break;
                 }
                 else
                     throw new IOException("Unexpected message type");
             }
         }

         protected void clientMainLoop() throws IOException, ClassNotFoundException {
             while (true) {
                 Message message = Client.this.connection.receive();
                 if (message == null)
                     continue;
                 if (message.getType() == MessageType.TEXT) {
                     processIncomingMessage(message.getData());
                 }
                 else if (message.getType() == MessageType.USER_ADDED) {
                     informAboutAddingNewUser(message.getData());
                 }
                 else if (message.getType() == MessageType.USER_REMOVED) {
                     informAboutDeletingNewUser(message.getData());
                 }
                 else
                     throw new IOException("Unexpected message type");
             }
         }

         @Override
         public void run() {
             try {
                 String serverAddress = getServerAddress();
                 int serverPort = getServerPort();
                 Client.this.connection = new Connection( new Socket(serverAddress, serverPort) );
                 clientHandshake();
                 clientMainLoop();
             }
             catch (IOException | ClassNotFoundException e) {
                 notifyConnectionStatusChanged(false);
             }
         }
     }

     protected String getServerAddress() {
         return ConsoleHelper.readString();
     }

     protected int getServerPort() {
         return ConsoleHelper.readInt();
     }

     protected String getUserName() {
         return ConsoleHelper.readString();
     }

     protected boolean shouldSendTextFromConsole() {
         return true;
     }

     protected SocketThread getSocketThread() {
         return new SocketThread();
     }

     protected void sendTextMessage(String text) {
         if (text == null) {
             return;
         }
         try {
             Message msg = new Message(MessageType.TEXT, text);
             connection.send(msg);
         } catch (IOException e) {
             ConsoleHelper.writeMessage(String.format("Ошибка при отправке сообщения: %s", e.getMessage()));
             clientConnected = false;
         }
     }

     public void run() {
         Thread t = getSocketThread();
         t.setDaemon( true );
         t.start();
         synchronized (this) {
             try {
                 this.wait();
             } catch (InterruptedException e) {
                 ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
                 return;
             }
         }
         if (!clientConnected) {
             ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
             return;
         }
         ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
         while (clientConnected) {
             String text = ConsoleHelper.readString();
             if ("exit".equals(text))
                 break;
             if (shouldSendTextFromConsole())
                 sendTextMessage(text);
         }
     }

     public static void main(String[] args) {
         Client client = new Client();
         client.run();
     }
 }
