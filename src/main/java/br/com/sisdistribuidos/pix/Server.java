package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.database.DatabaseManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) {
        // --- INÍCIO DA MUDANÇA ---
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite a porta para iniciar o servidor: ");
        int port = Integer.parseInt(scanner.nextLine());
        // --- FIM DA MUDANÇA ---

        try {
            DatabaseManager.initialize();
            System.out.println("Banco de dados inicializado com sucesso.");
        } catch (Exception e) {
            System.err.println("Erro FATAL na inicialização do banco de dados: " + e.getMessage());
            return;
        }

        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor TCP iniciado na porta " + port);
            System.out.println("Aguardando conexões de clientes...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());
                pool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor na porta " + port + ": " + e.getMessage());
        }
    }
}