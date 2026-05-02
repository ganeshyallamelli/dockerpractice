package com.example;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        SimpleController controller = new SimpleController();

        server.createContext("/", controller::handleRequest);
        server.setExecutor(Executors.newCachedThreadPool());

        server.start();
        System.out.println("Server started at http://localhost:8080");
        System.out.println("Use GET /project, GET /members, POST /members, PUT /project, PUT /members/{id}, DELETE /members/{id}");
    }
}
