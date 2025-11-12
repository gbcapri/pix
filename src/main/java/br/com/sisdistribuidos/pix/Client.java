package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.validador.Validator;
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
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static String sessionToken = null;
    private static String loggedInUserName = null;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite o endereço IP do servidor: ");
        String serverIp = scanner.nextLine();
        System.out.print("Digite a porta do servidor: ");
        int serverPort = Integer.parseInt(scanner.nextLine());

        try {
            socket = new Socket(serverIp, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Conectado ao servidor em " + serverIp + ":" + serverPort);

            if (establishProtocol()) {
                runMenu(scanner);
            }//se tiver problemas de conectar tem que comentar o if e colocar somente o runMenu(scanner);

        } catch (IOException e) {
            System.err.println("Não foi possível conectar ao servidor: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private static boolean establishProtocol() throws IOException {
        ObjectNode connectJson = objectMapper.createObjectNode();
        connectJson.put("operacao", "conectar");

        String responseStr = sendRawRequest(connectJson.toString());
            
        if (responseStr == null) return false;

        try {
            Validator.validateServer(responseStr); 
            
            JsonNode responseNode = objectMapper.readTree(responseStr);
            boolean status = responseNode.get("status").asBoolean();
            if (status) {
                System.out.println("Protocolo iniciado com sucesso.");
            } else {
                System.err.println("Falha ao iniciar protocolo: " + responseNode.get("info").asText());
            }
            return status;
        } catch (Exception e) {
            System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'conectar': " + e.getMessage());
            return false;
        }
    }
    
    private static void reportarErroServidor(String operacaoEnviada, String infoErro) throws IOException {
        System.err.println("Reportando erro ao servidor...");
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "erro_servidor");
        json.put("operacao_enviada", operacaoEnviada);
        json.put("info", infoErro);
        
        // Apenas envia, o sendRequest já imprime a resposta do servidor ao log.
        sendRequest(json); 
    }

    private static void runMenu(Scanner scanner) {
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
            System.out.println("5. Fazer transação (PIX)");
            System.out.println("6. Ler minhas transações (Extrato)");
            System.out.println("7. Fazer Depósito");
            System.out.println("8. Sair");
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
            case 5: fazerTransacao(scanner); break;
            case 6: lerTransacoes(scanner);break;
            case 7: fazerDeposito(scanner); break;
            case 8:
                System.out.println("Encerrando o cliente.");
                closeConnection();
                break;
            default: System.out.println("Opção inválida.");
        }
    }
    
    private static String sendRawRequest(String request) throws IOException {
        System.out.println("Cliente enviou: " + request);
        out.println(request);
        String response = in.readLine();
        System.out.println("Servidor respondeu: " + response);
        return response;
    }

    private static String sendRequest(ObjectNode json) throws IOException {
        String request = json.toString();
        System.out.println("Cliente enviou: " + request);
        out.println(request);
        String response = in.readLine();
        System.out.println("Servidor respondeu: " + response); // Impressão centralizada aqui
        return response;
    }
    
    private static void fazerDeposito(Scanner scanner) throws IOException {
        System.out.print("Digite o valor a ser depositado: ");
        double valor;
        try {
            valor = Double.parseDouble(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.err.println("Valor inválido. Insira um número (ex: 150.75).");
            return;
        }

        if (valor <= 0) {
             System.err.println("O valor do depósito deve ser positivo.");
             return;
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "depositar"); //
        json.put("token", sessionToken);
        json.put("valor_enviado", valor); //

        String response = sendRequest(json);
        //System.out.println("Servidor respondeu: " + response);
        
        try {
            
            Validator.validateServer(response);
            
             JsonNode responseNode = objectMapper.readTree(response);
             String info = responseNode.get("info").asText();
             if (responseNode.get("status").asBoolean()) {
                 System.out.println("Sucesso: " + info);
             } else {
                 System.err.println("Erro: " + info);
             }
        } catch (Exception e) {
             System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'depositar': " + e.getMessage());
             reportarErroServidor("depositar", e.getMessage());
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
        if (response == null) return;
        
        try {
            
            Validator.validateServer(response);
            
             JsonNode responseNode = objectMapper.readTree(response);
             String info = responseNode.get("info").asText();
             if (responseNode.get("status").asBoolean()) {
                 System.out.println("Sucesso: " + info);
             } else {
                 System.err.println("Erro na transação: " + info);
             }
        } catch (Exception e) {
             System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'transacao_criar': " + e.getMessage());
             reportarErroServidor("transacao_criar", e.getMessage());
        }
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
        
        
        try {
            
            Validator.validateServer(response);
            
            JsonNode responseNode = objectMapper.readTree(response);

            if (responseNode.get("status").asBoolean()) {
                System.out.println("\n--- EXTRATO DA CONTA ---");
                System.out.println("Período: " + dataInicial + " até " + dataFinal);
                System.out.println("-----------------------------------------------------");

                JsonNode transacoesArray = responseNode.get("transacoes");
                if (transacoesArray == null || transacoesArray.isEmpty()) {
                    System.out.println("Nenhuma transação encontrada neste período.");
                } else {
                    for (JsonNode t : transacoesArray) {
                        // O nome do campo JSON é 'valor_enviado'
                        String data = t.path("criado_em").asText();
                        double valor = t.path("valor_enviado").asDouble(); 
                        String enviadorNome = t.path("usuario_enviador").path("nome").asText("N/A");
                        String recebedorNome = t.path("usuario_recebedor").path("nome").asText("N/A");
                        
                        String tipo;
                        String valorDisplay;
                        
                        // Checa se é depósito (enviador == recebedor)
                        if (enviadorNome.equals(recebedorNome)) {
                            tipo = "DEPÓSITO";
                            valorDisplay = String.format("+ R$ %.2f", valor);
                        } 
                        // Checa se é envio (o usuário logado é o enviador)
                        else if (enviadorNome.equals(loggedInUserName)) {
                            tipo = "ENVIO (PIX)";
                            valorDisplay = String.format("- R$ %.2f", valor);
                        } 
                        // Senão, é recebimento (o usuário logado é o recebedor)
                        else {
                            tipo = "RECEBIMENTO (PIX)";
                            valorDisplay = String.format("+ R$ %.2f", valor);
                        }
                        
                        System.out.println("\nData: " + data);
                        System.out.println("Tipo: " + tipo);
                        System.out.println("Valor: " + valorDisplay);
                        
                        if (tipo.equals("ENVIO (PIX)")) {
                            System.out.println("Para: " + recebedorNome);
                        } else if (tipo.equals("RECEBIMENTO (PIX)")) {
                            System.out.println("De: " + enviadorNome);
                        }
                    }
                }
                System.out.println("-----------------------------------------------------");

            } else {
                // Se status == false, apenas loga o erro (o raw JSON já foi impresso)
                System.err.println("Erro ao buscar extrato: " + responseNode.get("info").asText());
            }
        } catch (Exception e) {
            System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'transacao_ler': " + e.getMessage());
             reportarErroServidor("transacao_ler", e.getMessage());
        }
    }
    
    private static void fazerLogin(Scanner scanner) throws IOException {
        System.out.print("CPF: "); String cpf = scanner.nextLine();
        System.out.print("Senha: "); String senha = scanner.nextLine();
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_login");
        json.put("cpf", cpf);
        json.put("senha", senha);
        
        String response = sendRequest(json);
        if (response == null) return;
        
        try {
            // Valida a resposta (REGRA 2.0)
            Validator.validateServer(response);

            JsonNode responseNode = objectMapper.readTree(response);
            if (responseNode.get("status").asBoolean()) {
                sessionToken = responseNode.get("token").asText();
                System.out.println("Login bem-sucedido. Buscando dados do usuário...");
                lerUsuario();
            } else {
                sessionToken = null;
                loggedInUserName = null;
                String info = responseNode.path("info").asText("Erro desconhecido.");
                System.err.println("Erro no login: " + info);
            }
        } catch (Exception e) {
             System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'usuario_login': " + e.getMessage());
             reportarErroServidor("usuario_login", e.getMessage());
        }
    }
    
    private static void lerUsuario() throws IOException {
        if (sessionToken == null) { System.out.println("Faça login primeiro."); return; }
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_ler");
        json.put("token", sessionToken);
        
        String response = sendRequest(json);
        if (response == null) return;
        
        try {
            Validator.validateServer(response);

            JsonNode responseNode = objectMapper.readTree(response);
            if (responseNode.get("status").asBoolean()) {
                loggedInUserName = responseNode.path("usuario").path("nome").asText("Desconhecido");
            } else {
                 System.err.println("Erro ao ler dados: " + responseNode.path("info").asText());
            }
        } catch (Exception e) {
             System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'usuario_ler': " + e.getMessage());
             reportarErroServidor("usuario_ler", e.getMessage());
        }
    }
    
    private static void fazerLogout() throws IOException {
        if (sessionToken == null) { System.out.println("Nenhum usuário está logado."); return; }
        
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_logout");
        json.put("token", sessionToken);

        String response = sendRequest(json);
        if (response == null) return;
        
        try {
             Validator.validateServer(response);
             JsonNode responseNode = objectMapper.readTree(response);
             System.out.println(responseNode.get("info").asText()); 
        } catch (Exception e) {
             System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'usuario_logout': " + e.getMessage());
             reportarErroServidor("usuario_logout", e.getMessage());
        }
        
        sessionToken = null;
        loggedInUserName = null;
    }

    private static void deletarUsuario() throws IOException {
        if (sessionToken == null) { System.out.println("Faça login primeiro."); return; }
        
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "usuario_deletar");
        json.put("token", sessionToken);
        
        String response = sendRequest(json);
        if (response == null) return;
        
        try {
            Validator.validateServer(response);
            JsonNode responseNode = objectMapper.readTree(response);
            
            String info = responseNode.get("info").asText();
            if (responseNode.get("status").asBoolean()) {
                System.out.println("Sucesso: " + info);
                sessionToken = null;
                loggedInUserName = null;
            } else {
                System.err.println("Erro ao deletar: " + info);
            }
        } catch (Exception e) {
             System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'usuario_deletar': " + e.getMessage());
             reportarErroServidor("usuario_deletar", e.getMessage());
        }
    }

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
        if (response == null) return;

        try {
             Validator.validateServer(response);
             JsonNode responseNode = objectMapper.readTree(response);
             String info = responseNode.get("info").asText();
             if (responseNode.get("status").asBoolean()) {
                 System.out.println("Sucesso: " + info);
             } else {
                 System.err.println("Erro no cadastro: " + info); 
             }
        } catch (Exception e) {
             System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'usuario_criar': " + e.getMessage());
             reportarErroServidor("usuario_criar", e.getMessage());
        }
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
        if (response == null) return;
        
        try {
            Validator.validateServer(response);
             JsonNode responseNode = objectMapper.readTree(response);
             String info = responseNode.get("info").asText();
             if (responseNode.get("status").asBoolean()) {
                 System.out.println("Sucesso: " + info);
                 if (!nome.isEmpty()) {
                     loggedInUserName = nome; // Atualiza o nome para o extrato
                 }
             } else {
                 System.err.println("Erro ao atualizar: " + info);
             }
        } catch (Exception e) {
             System.err.println("ERRO DE PROTOCOLO: O servidor enviou uma resposta inválida para 'usuario_atualizar': " + e.getMessage());
             reportarErroServidor("usuario_atualizar", e.getMessage());
        }
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