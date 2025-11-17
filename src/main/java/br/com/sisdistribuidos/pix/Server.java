package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.database.DatabaseManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    
    private static final int DEFAULT_PORT = 12345;
    
    public static void main(String[] args) {
        
        ServerGUI serverGUI = new ServerGUI();
        serverGUI.start();
        
        Map<String, String> sessions = new ConcurrentHashMap<>();
        
        int port = DEFAULT_PORT;
        
        try {
            DatabaseManager.initialize();
            serverGUI.log("Banco de dados inicializado com sucesso.");
        } catch (Exception e) {
            serverGUI.log("Erro FATAL na inicialização do banco de dados: " + e.getMessage());
            return;
        }

        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            String ipAddress = findServerIpAddress();
            if (ipAddress != null) {
                serverGUI.log("Endereço IPv4 do Servidor: " + ipAddress);
            } else {
                 serverGUI.log("Não foi possível determinar o endereço IPv4 local.");
            }

            serverGUI.log("Servidor iniciado na porta " + port);
            serverGUI.log("Aguardando conexões de clientes...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                serverGUI.log("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());
                
                pool.submit(new ClientHandler(clientSocket, serverGUI, sessions));
            }
        } catch (IOException e) {
            serverGUI.log("Erro ao iniciar o servidor na porta " + port + ": " + e.getMessage());
        } finally {
             pool.shutdown();
        }
    }
    
    private static String findServerIpAddress() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            String ip = socket.getLocalAddress().getHostAddress();
            
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isSiteLocalAddress()) {
                 return ip;
            }
        } catch (SocketException | UnknownHostException e) {
            System.err.println("Método preferencial (UDP) falhou, tentando fallback: " + e.getMessage());
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(interfaces)) {
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                for (InetAddress addr : Collections.list(addresses)) {
                    if (addr.isSiteLocalAddress() && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            
            return InetAddress.getLocalHost().getHostAddress();
            
        } catch (SocketException | UnknownHostException e) {
            System.err.println("Erro ao obter endereço IP local: " + e.getMessage());
            return null;
        }
    }
}