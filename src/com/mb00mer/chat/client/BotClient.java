package com.mb00mer.chat.client;

import com.mb00mer.chat.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class BotClient extends Client {
    public class BotSocketThread extends SocketThread {
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            if (message == null) {
                return;
            }
            ConsoleHelper.writeMessage(message);
            String userName = "";
            String text = "";
            try {
                userName = message.substring(0, message.indexOf(':')).trim();
                text = message.substring(message.indexOf(':') + 1).trim();
            }
            catch (IndexOutOfBoundsException ignored) {/*NOP*/}
            if (text.isEmpty() || userName.isEmpty())
                return;
            Date d = new GregorianCalendar().getTime();
            String dPattern;
            switch (text) {
                case "дата":
                    dPattern = "d.MM.YYYY";
                    break;
                case "день":
                    dPattern = "d";
                    break;
                case "месяц":
                    dPattern = "MMMM";
                    break;
                case "год":
                    dPattern = "YYYY";
                    break;
                case "время":
                    dPattern = "H:mm:ss";
                    break;
                case "час":
                    dPattern = "H";
                    break;
                case "минуты":
                    dPattern = "m";
                    break;
                case "секунды":
                    dPattern = "s";
                    break;
                default:
                    return;
            }
            sendTextMessage( String.format("Информация для %s: %s", userName, new SimpleDateFormat(dPattern).format(d)) );
        }
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return String.format("date_bot_%d", (int) (Math.random() * 100));
    }

    public static void main(String[] args) {
        new BotClient().run();
    }
}
