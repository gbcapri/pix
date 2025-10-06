package br.com.sisdistribuidos.pix;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Client {

    private static final String SERVER_URL = "http://localhost:4567";
    private static String sessionToken = null;
    private static String loggedInUserCpf = null; // Novo campo para o CPF logado
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static void main(String[] args) {
        System.out.println("Cliente PIX de Sistemas Distribuídos.");
        System.out.println("Iniciando...");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- Menu do Cliente ---");
            if (sessionToken != null) {
                System.out.println("  *** LOGADO: " + loggedInUserCpf + " ***"); // Exibe o status
                System.out.println("1. Criar usuário");
                System.out.println("2. Fazer LOGOUT"); // Opção 2 vira LOGOUT
            } else {
                System.out.println("1. Criar usuário");
                System.out.println("2. Fazer login"); // Opção 2 vira LOGIN
            }
            
            System.out.println("3. Ler dados do usuário");
            System.out.println("4. Fazer transação (PIX)");
            System.out.println("5. Ler transações por data");
            System.out.println("6. Sair");
            System.out.print("Escolha uma opção: ");

            try {
                int option = scanner.nextInt();
                scanner.nextLine();

                if (option == 2 && sessionToken != null) {
                    fazerLogout();
                    continue;
                }
                
                switch (option) {
                    case 1:
                        criarUsuario(scanner);
                        break;
                    case 2:
                        fazerLogin(scanner);
                        break;
                    case 3:
                        lerUsuario();
                        break;
                    case 4:
                        fazerTransacao(scanner);
                        break;
                    case 5:
                        lerTransacoes(scanner);
                        break;
                    case 6:
                        System.out.println("Encerrando o cliente.");
                        return;
                    default:
                        System.out.println("Opção inválida. Tente novamente.");
                }
            } catch (Exception e) {
                System.err.println("Ocorreu um erro: " + e.getMessage());
                scanner.nextLine();
            }
        }
    }

    private static void fazerLogout() throws Exception {
        String requestBody = String.format("{\"operacao\":\"usuario_logout\", \"token\":\"%s\"}", sessionToken);
        HttpResponse<String> response = sendPostRequest("/usuario/logout", requestBody);
        
        // Limpa a sessão local independentemente da resposta do servidor
        if (response != null && response.statusCode() == 200) {
            sessionToken = null;
            loggedInUserCpf = null;
            System.out.println("Sessão limpa. Usuário desconectado.");
        }
    }

    private static void criarUsuario(Scanner scanner) throws Exception {
        // ... (código de criarUsuario)
        // ... (código de criarUsuario)
        System.out.print("Nome: ");
        String nome = scanner.nextLine();
        System.out.print("CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        String requestBody = String.format("{\"operacao\":\"usuario_criar\", \"nome\":\"%s\", \"cpf\":\"%s\", \"senha\":\"%s\"}",
                nome, cpf, senha);

        sendPostRequest("/usuario/criar", requestBody);
    }

    private static void fazerLogin(Scanner scanner) throws Exception {
        System.out.print("CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Senha: ");
        String senha = scanner.nextLine();

        String requestBody = String.format("{\"operacao\":\"usuario_login\", \"cpf\":\"%s\", \"senha\":\"%s\"}",
                cpf, senha);

        HttpResponse<String> response = sendPostRequest("/usuario/login", requestBody);
        
        if (response != null && response.statusCode() == 200) {
            String body = response.body();
            if (body.contains("\"status\":true")) {
                int tokenStart = body.indexOf("\"token\":\"") + 9;
                int tokenEnd = body.indexOf("\"", tokenStart);
                sessionToken = body.substring(tokenStart, tokenEnd);
                loggedInUserCpf = cpf; // Armazena o CPF no login
                System.out.println("Login bem-sucedido. Token de sessão: " + sessionToken);
            }
        }
    }

    private static void lerUsuario() throws Exception {
        if (sessionToken == null) {
            System.out.println("Faça o login primeiro.");
            return;
        }
        
        String requestBody = String.format("{\"operacao\":\"usuario_ler\", \"token\":\"%s\"}", sessionToken);
        sendPostRequest("/usuario/ler", requestBody);
    }

    private static void fazerTransacao(Scanner scanner) throws Exception {
        if (sessionToken == null) {
            System.out.println("Faça o login primeiro.");
            return;
        }

        System.out.print("CPF do recebedor: ");
        String cpfRecebedor = scanner.nextLine();
        System.out.print("Valor da transação: ");
        double valor = scanner.nextDouble();
        scanner.nextLine();

        String requestBody = String.format("{\"operacao\":\"transacao_criar\", \"token\":\"%s\", \"cpf_destino\":\"%s\", \"valor\":%f}",
                sessionToken, cpfRecebedor, valor);

        sendPostRequest("/transacao/criar", requestBody);
    }

    private static void lerTransacoes(Scanner scanner) throws Exception {
        if (sessionToken == null) {
            System.out.println("Faça o login primeiro.");
            return;
        }

        System.out.print("Data de início (yyyy-MM-dd'T'HH:mm:ss'Z'): ");
        String dataInicialStr = scanner.nextLine();
        System.out.print("Data de fim (yyyy-MM-dd'T'HH:mm:ss'Z'): ");
        String dataFinalStr = scanner.nextLine();

        String requestBody = String.format("{\"operacao\":\"transacao_ler\", \"token\":\"%s\", \"data_inicial\":\"%s\", \"data_final\":\"%s\"}",
                sessionToken, dataInicialStr, dataFinalStr);

        sendPostRequest("/transacao/ler", requestBody);
    }

    private static HttpResponse<String> sendPostRequest(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + path))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Resposta do servidor (" + response.statusCode() + "): " + response.body());
        return response;
    }
}