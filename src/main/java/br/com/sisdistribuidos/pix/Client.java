package br.com.sisdistribuidos.pix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    private static final String BASE_URL = "http://localhost:4567";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static String sessionToken = null;
    private static String loggedInUserName = null; // <-- NOVO: Armazena o nome do usuário logado

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            displayMenu(); // <-- NOVO: Função para exibir o menu dinâmico

            try {
                int option = Integer.parseInt(scanner.nextLine());

                if (sessionToken == null) { // Menu para usuários deslogados
                    switch (option) {
                        case 1: criarUsuario(scanner); break;
                        case 2: fazerLogin(scanner); break;
                        case 3:
                            System.out.println("Encerrando o cliente.");
                            scanner.close();
                            return;
                        default:
                            System.out.println("Opção inválida. Tente novamente.");
                    }
                } else { // Menu para usuários logados
                    switch (option) {
                        case 1: fazerLogout(); break;
                        case 2: lerUsuario(); break;
                        case 3: atualizarUsuario(scanner); break;
                        case 4: deletarUsuario(scanner); break;
                        case 5: fazerTransacao(scanner); break;
                        case 6: lerTransacoes(scanner); break;
                        case 7: depositar(scanner); break;
                        case 8:
                            System.out.println("Encerrando o cliente.");
                            scanner.close();
                            return;
                        default:
                            System.out.println("Opção inválida. Tente novamente.");
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Entrada inválida. Por favor, digite um número.");
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
            System.out.println("5. Fazer transação (PIX)");
            System.out.println("6. Ler minhas transações");
            System.out.println("7. Depositar dinheiro");
            System.out.println("8. Sair");
        } else {
            System.out.println("1. Criar usuário");
            System.out.println("2. Fazer login");
            System.out.println("3. Sair");
        }
        System.out.print("Escolha uma opção: ");
    }

    private static void criarUsuario(Scanner scanner) throws Exception {
        System.out.print("Digite o CPF (Formato: 000.000.000-00): ");
        String cpf = scanner.nextLine();
        System.out.print("Digite o Nome (Mínimo 6 caracteres): ");
        String nome = scanner.nextLine();
        System.out.print("Digite a Senha (Mínimo 6 caracteres): ");
        String senha = scanner.nextLine();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_criar");
        json.put("cpf", cpf);
        json.put("nome", nome);
        json.put("senha", senha);

        String response = sendRequest("/usuario/criar", json.toString());
        System.out.println("Resposta do Servidor: " + response);
    }

    private static void fazerLogin(Scanner scanner) throws Exception {
        System.out.print("Digite o CPF: ");
        String cpf = scanner.nextLine();
        System.out.print("Digite a Senha: ");
        String senha = scanner.nextLine();

        ObjectNode jsonLogin = objectMapper.createObjectNode();
        jsonLogin.put("operacao", "usuario_login");
        jsonLogin.put("cpf", cpf);
        jsonLogin.put("senha", senha);

        String response = sendRequest("/usuario/login", jsonLogin.toString());
        System.out.println("Resposta do Servidor: " + response);

        JsonNode responseNode = objectMapper.readTree(response);
        if (responseNode.get("status").asBoolean()) {
            sessionToken = responseNode.get("token").asText();
            System.out.println("Login bem-sucedido. Buscando dados do usuário...");
            // Após o login, busca os dados do usuário para obter o nome
            lerUsuario(); 
        } else {
            sessionToken = null;
            loggedInUserName = null;
        }
    }
    
    private static void fazerLogout() throws Exception {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_logout");
        json.put("token", sessionToken);
        
        String response = sendRequest("/usuario/logout", json.toString());
        System.out.println("Resposta do Servidor: " + response);
        
        sessionToken = null;
        loggedInUserName = null;
    }

    private static void lerUsuario() throws Exception {
        if (sessionToken == null) {
            System.out.println("É necessário fazer login primeiro.");
            return;
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_ler");
        json.put("token", sessionToken);

        String response = sendRequest("/usuario/ler", json.toString());
        System.out.println("Resposta do Servidor: " + response);

        // Se a leitura foi bem-sucedida, armazena/atualiza o nome do usuário
        JsonNode responseNode = objectMapper.readTree(response);
        if (responseNode.get("status").asBoolean()) {
            loggedInUserName = responseNode.path("usuario").path("nome").asText("Usuário Desconhecido");
        }
    }

    private static void atualizarUsuario(Scanner scanner) throws Exception {
        System.out.print("Digite o novo nome (deixe em branco para não alterar): ");
        String nome = scanner.nextLine();
        System.out.print("Digite a nova senha (deixe em branco para não alterar): ");
        String senha = scanner.nextLine();
        
        if (nome.trim().isEmpty() && senha.trim().isEmpty()) {
            System.out.println("Nenhuma alteração solicitada.");
            return;
        }

        ObjectNode usuarioNode = objectMapper.createObjectNode();
        if (!nome.trim().isEmpty()) {
            usuarioNode.put("nome", nome);
        }
        if (!senha.trim().isEmpty()) {
            usuarioNode.put("senha", senha);
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_atualizar");
        json.put("token", sessionToken);
        json.set("usuario", usuarioNode);

        String response = sendRequest("/usuario/atualizar", json.toString());
        System.out.println("Resposta do Servidor: " + response);

        // Se o nome foi alterado, atualiza a saudação no menu
        if (!nome.trim().isEmpty()) {
             loggedInUserName = nome;
        }
    }

    private static void deletarUsuario(Scanner scanner) throws Exception {
        System.out.print("Você tem certeza que deseja deletar sua conta? Esta ação não pode ser desfeita. (s/n): ");
        String confirmacao = scanner.nextLine();

        if (!confirmacao.equalsIgnoreCase("s")) {
            System.out.println("Operação cancelada.");
            return;
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_deletar");
        json.put("token", sessionToken);
        
        String response = sendRequest("/usuario/deletar", json.toString());
        System.out.println("Resposta do Servidor: " + response);

        JsonNode responseNode = objectMapper.readTree(response);
        if (responseNode.get("status").asBoolean()) {
            sessionToken = null;
            loggedInUserName = null;
            System.out.println("Usuário deletado com sucesso. Sessão encerrada.");
        }
    }

    private static void fazerTransacao(Scanner scanner) throws Exception {
        System.out.print("Digite o CPF de destino: ");
        String cpfDestino = scanner.nextLine();
        System.out.print("Digite o valor: ");
        double valor = Double.parseDouble(scanner.nextLine());

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "transacao_criar");
        json.put("token", sessionToken);
        json.put("cpf_destino", cpfDestino);
        json.put("valor", valor);

        String response = sendRequest("/transacao/criar", json.toString());
        System.out.println("Resposta do Servidor: " + response);
    }

    private static void lerTransacoes(Scanner scanner) throws Exception {
        System.out.print("Digite a data inicial (ex: 2023-10-27T10:00:00Z): ");
        String dataInicial = scanner.nextLine();
        System.out.print("Digite a data final (ex: 2023-10-28T10:00:00Z): ");
        String dataFinal = scanner.nextLine();

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "transacao_ler");
        json.put("token", sessionToken);
        json.put("data_inicial", dataInicial);
        json.put("data_final", dataFinal);

        String response = sendRequest("/transacao/ler", json.toString());
        System.out.println("Resposta do Servidor: " + response);
    }

    private static void depositar(Scanner scanner) throws Exception {
        System.out.print("Digite o valor a ser depositado: ");
        double valor = Double.parseDouble(scanner.nextLine());

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "depositar");
        json.put("token", sessionToken);
        json.put("valor_enviado", valor);
        
        String response = sendRequest("/usuario/depositar", json.toString());
        System.out.println("Resposta do Servidor: " + response);
    }

    private static String sendRequest(String endpoint, String jsonInputString) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; utf-8");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        con.setConnectTimeout(5000); // Timeout de 5 segundos
        con.setReadTimeout(5000);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        int statusCode = con.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
            statusCode > 299 ? con.getErrorStream() : con.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        return response.toString();
    }
}