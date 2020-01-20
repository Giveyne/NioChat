package NioChat;

import java.io.IOException;
import java.lang.*;
public class main {

        public static void main(String[] args) throws IOException {

            new Chat("localhost", 8080 ).start();
        }
}

