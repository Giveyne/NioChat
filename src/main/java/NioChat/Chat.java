package NioChat;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Chat {
    private Selector selector; // Селектор для приема входящих соединений и запросов
    private InetSocketAddress address; // Создает поле с адресом и портом
    private Set<SocketChannel> session; // Множество для обработки входящих ченелов и их запросов

    public  Chat (String host, int port){
        this.address = new InetSocketAddress(host, port);
        this.session = new HashSet<SocketChannel>();
    }
    public void start() throws IOException{ // метод старт для запуска:
        this.selector = Selector.open();                            // Селектора
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open(); // Сервера
        serverSocketChannel.socket().bind(address);                           // задали адрес и порт
        serverSocketChannel.configureBlocking(false);                     //установили без блокировочный ввод данных
        serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);//зарегестрировали наш сервер в селекторе
        System.out.println("Server started....");

        while (true){  //цикл проверки чего хочет подключаемый сокет
            this.selector.select(); // вернем ключ если сокет готов к дейсвиям
            Iterator keys = this.selector.selectedKeys().iterator(); // загоняем всех кто готов в итератор
            while(keys.hasNext()){ // если есть кто то готовый
                SelectionKey key = (SelectionKey) keys.next(); // Присваеваем кей из готовых кею для дальнейших проверок
                keys.remove(); // удаляем кей из итератора
                if (!key.isValid()) continue; // если кей не валидный(отвалился) то пускаем новый кей из итератора в цикл
                if (key.isAcceptable()) accept (key); //  проверяем готов ли канал ключа к новому сокет-соединению (и подсоединяем к селектору)
                else if (key.isReadable()) read (key); // если готов к чтению то читаем что он напишет
            }

        }

    }
    private void accept (SelectionKey key) throws IOException{
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);
        this.session.add(channel);
        channel.register(this.selector, SelectionKey.OP_READ);
        broadcast ("System new user" + channel.socket().getRemoteSocketAddress());
    }
    private void read (SelectionKey key) throws IOException{
        SocketChannel channel = (SocketChannel)key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = -1;
        numRead = channel.read(buffer);
        if (numRead == -1){
            this.session.remove(channel);
            broadcast ("System user left" + channel.socket().getRemoteSocketAddress());
            channel.close();
            key.cancel();
            return;
        }
        byte [] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0 , data, 0, numRead);
        String gotData = new String(data);
        System.out.println(": " + gotData);
        broadcast (channel.socket().getRemoteSocketAddress() + ":" + gotData);
    }
    private void broadcast (String data){
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        buffer.put(data.getBytes());
        buffer.flip();
        this.session.forEach(socketChannel -> {
            try {
                socketChannel.write(buffer);
                buffer.flip();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        });
    }

}
