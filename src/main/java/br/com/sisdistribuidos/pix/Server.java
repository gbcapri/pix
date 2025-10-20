package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.database.DatabaseManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite a porta para iniciar o servidor: ");
        int port = Integer.parseInt(scanner.nextLine());

        try {
            DatabaseManager.initialize();
            System.out.println("Banco de dados inicializado com sucesso.");
        } catch (Exception e) {
            System.err.println("Erro FATAL na inicialização do banco de dados: " + e.getMessage());
            return;
        }

        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            String ipAddress = findServerIpAddress();
            if (ipAddress != null) {
                System.out.println("Endereço IPv4 do Servidor: " + ipAddress);
            } else {
                 System.out.println("Não foi possível determinar o endereço IPv4 local.");
            }

            System.out.println("Servidor iniciado na porta " + port);
            System.out.println("Aguardando conexões de clientes...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());
                pool.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor na porta " + port + ": " + e.getMessage());
        } finally {
             scanner.close();
             pool.shutdown();
        }
    }
    
    private static String findServerIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(interfaces)) {
                if (!ni.isUp() || ni.isLoopback()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                for (InetAddress addr : Collections.list(addresses)) {
                    if (addr.isSiteLocalAddress() && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
             return InetAddress.getLocalHost().getHostAddress();
        } catch (SocketException | java.net.UnknownHostException e) {
            System.err.println("Erro ao obter endereço IP local: " + e.getMessage());
            return null;
        }
    }
}