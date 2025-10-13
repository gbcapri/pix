package br.com.sisdistribuidos.pix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    // Removidas as constantes de IP e Porta
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static String sessionToken = null;
    private static String loggedInUserName = null;

    public static void main(String[] args) {
        // --- INÍCIO DA MUDANÇA ---
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o endereço IP do servidor: ");
        String serverIp = scanner.nextLine();
        System.out.print("Digite a porta do servidor: ");
        int serverPort = Integer.parseInt(scanner.nextLine());
        // --- FIM DA MUDANÇA ---

        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Conectado ao servidor em " + serverIp + ":" + serverPort);

            runMenu(scanner); // Passa o scanner para o menu

        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private static void runMenu(Scanner scanner) { // Recebe o scanner
        while (socket != null && !socket.isClosed()) {
            displayMenu();
            try {
                int option = Integer.parseInt(scanner.nextLine());
                if (loggedInUserName == null) {
                    handleMenuDeslogado(option, scanner);
                } else {
                    handleMenuLogado(option, scanner);
                }
            } catch (Exception e) {
                System.err.println("Ocorreu um erro: " + e.getMessage());
            }
        }
    }

    private static void displayMenu() {
        System.out.println("\n--- Menu do Cliente ---");
        if (loggedInUserName != null) {
            System.out.println("Logado como: '" + loggedInUserName + "'");
            System.out.println("-------------------------");
            System.out.println("1. Fazer logout");
            System.out.println("2. Ler meus dados");
            System.out.println("3. Atualizar meus dados");
            System.out.println("4. Deletar minha conta");
            System.out.println("5. Fazer transação (PIX)"); // <-- Adicionado
            System.out.println("6. Ler minhas transações"); // <-- Adicionado
            System.out.println("7. Sair");
        } else {
            System.out.println("1. Criar usuário");
            System.out.println("2. Fazer login");
            System.out.println("3. Sair");
        }
        System.out.print("Escolha uma opção: ");
    }

    private static void handleMenuDeslogado(int option, Scanner scanner) throws Exception {
        switch (option) {
            case 1: criarUsuario(scanner); break;
            case 2: fazerLogin(scanner); break;
            case 3:
                System.out.println("Encerrando o cliente.");
                closeConnection();
                break;
            default: System.out.println("Opção inválida.");
        }
    }

    private static void handleMenuLogado(int option, Scanner scanner) throws Exception {
        switch (option) {
            case 1: fazerLogout(); break;
            case 2: lerUsuario(); break;
            case 3: atualizarUsuario(scanner); break;
            case 4: deletarUsuario(); break;
            case 5: fazerTransacao(scanner); break; // <-- Adicionado
            case 6: lerTransacoes(scanner); break; // <-- Adicionado
            case 7:
                System.out.println("Encerrando o cliente.");
                closeConnection();
                break;
            default: System.out.println("Opção inválida.");
        }
    }
    
    private static void fazerTransacao(Scanner scanner) throws IOException {
        System.out.print("Digite o CPF de destino: ");
        String cpfDestino = scanner.nextLine();
        System.out.print("Digite o valor: ");
        double valor = Double.parseDouble(scanner.nextLine());

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "transacao_criar");
        json.put("token", sessionToken);
        json.put("cpf_destino", cpfDestino);
        json.put("valor", valor);

        String response = sendRequest(json);
        System.out.println("Servidor respondeu: " + response);
    }
    
    private static void lerTransacoes(Scanner scanner) throws IOException {
        System.out.print("Digite a data inicial (ex: 2023-10-27T10:00:00Z): ");
        String dataInicial = scanner.nextLine();
        System.out.print("Digite a data final (ex: 2023-10-28T10:00:00Z): ");
        String dataFinal = scanner.nextLine();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "transacao_ler");
        json.put("token", sessionToken);
        json.put("data_inicial", dataInicial);
        json.put("data_final", dataFinal);

        String response = sendRequest(json);
        System.out.println("Servidor respondeu: " + response);
    }
    
    private static void fazerLogin(Scanner scanner) throws IOException {
        System.out.print("CPF: "); String cpf = scanner.nextLine();
        System.out.print("Senha: "); String senha = scanner.nextLine();
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_login");
        json.put("cpf", cpf);
        json.put("senha", senha);
        String response = sendRequest(json);
        System.out.println("Servidor respondeu: " + response);
        JsonNode responseNode = objectMapper.readTree(response);
        if (responseNode.get("status").asBoolean()) {
            sessionToken = responseNode.get("token").asText();
            System.out.println("Login bem-sucedido. Buscando dados do usuário...");
            lerUsuario();
        } else {
            sessionToken = null;
            loggedInUserName = null;
        }
    }
    
    private static void lerUsuario() throws IOException {
        if (sessionToken == null) { System.out.println("Faça login primeiro."); return; }
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_ler");
        json.put("token", sessionToken);
        String response = sendRequest(json);
        System.out.println("Servidor respondeu: " + response);
        JsonNode responseNode = objectMapper.readTree(response);
        if (responseNode.get("status").asBoolean()) {
            loggedInUserName = responseNode.path("usuario").path("nome").asText("Desconhecido");
        }
    }
    
    private static void fazerLogout() throws IOException {
        if (sessionToken == null) { System.out.println("Nenhum usuário está logado."); return; }
        
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_logout");
        json.put("token", sessionToken);

        String response = sendRequest(json);
        System.out.println("Servidor respondeu: " + response);
        
        // Limpa os dados da sessão localmente
        sessionToken = null;
        loggedInUserName = null;
    }

    private static void deletarUsuario() throws IOException {
        if (sessionToken == null) { System.out.println("Faça login primeiro."); return; }
        
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_deletar");
        json.put("token", sessionToken);
        
        String response = sendRequest(json);
        System.out.println("Servidor respondeu: " + response);
        
        JsonNode responseNode = objectMapper.readTree(response);
        if (responseNode.get("status").asBoolean()) {
            sessionToken = null;
            loggedInUserName = null;
        }
    }

    // --- Outros métodos (criarUsuario, atualizarUsuario, etc. permanecem aqui) ---

    private static void criarUsuario(Scanner scanner) throws IOException {
        System.out.print("CPF: "); String cpf = scanner.nextLine();
        System.out.print("Nome: "); String nome = scanner.nextLine();
        System.out.print("Senha: "); String senha = scanner.nextLine();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_criar");
        json.put("cpf", cpf);
        json.put("nome", nome);
        json.put("senha", senha);
        
        String response = sendRequest(json);
        System.out.println("Servidor respondeu: " + response);
    }

    private static void atualizarUsuario(Scanner scanner) throws IOException {
        if (sessionToken == null) { System.out.println("Faça login primeiro."); return; }
        
        System.out.print("Novo nome (deixe em branco para não alterar): "); String nome = scanner.nextLine();
        System.out.print("Nova senha (deixe em branco para não alterar): "); String senha = scanner.nextLine();

        ObjectNode usuarioNode = objectMapper.createObjectNode();
        if (!nome.isEmpty()) usuarioNode.put("nome", nome);
        if (!senha.isEmpty()) usuarioNode.put("senha", senha);
        
        if (usuarioNode.size() == 0) {
            System.out.println("Nenhuma alteração informada.");
            return;
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_atualizar");
        json.put("token", sessionToken);
        json.set("usuario", usuarioNode);

        String response = sendRequest(json);
        System.out.println("Servidor respondeu: " + response);

        // Atualiza o nome exibido no menu, se ele foi alterado
        if (!nome.isEmpty()) {
            loggedInUserName = nome;
        }
    }
    
    private static String sendRequest(ObjectNode json) throws IOException {
        String request = json.toString();
        // System.out.println("Cliente enviou: " + request); // Descomente para debug
        out.println(request);
        return in.readLine();
    }
    
    private static void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão: " + e.getMessage());
        }
    }
}