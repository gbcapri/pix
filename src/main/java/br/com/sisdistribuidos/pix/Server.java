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
import javax.swing.JOptionPane;

public class Server {
    
    private static final int DEFAULT_PORT = 20000;
    
    public static void main(String[] args) {
        
        int port = DEFAULT_PORT;
        String portStr = (String) JOptionPane.showInputDialog(
                null, // Janela pai (nenhuma)
                "Digite a porta do servidor:", // Mensagem
                "Configuração do Servidor", // Título
                JOptionPane.QUESTION_MESSAGE, // Tipo de ícone
                null, // Icone customizado (nenhum)
                null, // Opções de seleção (nenhuma)
                Integer.toString(DEFAULT_PORT) // Valor inicial
        );
        
        // Se o usuário clicou em "Cancel", encerra a aplicação
        if (portStr == null) {
            System.out.println("Inicialização do servidor cancelada pelo usuário.");
            return;
        }

        try {
            // Se o usuário não deixou em branco, tenta parsear
            if (!portStr.trim().isEmpty()) {
                port = Integer.parseInt(portStr);
            }
            // Se deixou em branco, 'port' continua sendo DEFAULT_PORT
        } catch (NumberFormatException e) {
            // Se digitou texto inválido
            JOptionPane.showMessageDialog(null, 
                    "Porta inválida. Usando a porta padrão: " + DEFAULT_PORT,
                    "Aviso", 
                    JOptionPane.WARNING_MESSAGE);
            port = DEFAULT_PORT;
        }
        
        ServerGUI serverGUI = new ServerGUI();
        serverGUI.start();
        
        Map<String, String> sessions = new ConcurrentHashMap<>();
        Map<String, String> activeClients = new ConcurrentHashMap<>();
                
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
                
                pool.submit(new ClientHandler(clientSocket, serverGUI, sessions, activeClients));
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