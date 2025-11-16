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

/**
 * Gerenciador de Rede do Cliente.
 * Esta classe trata da comunicação (Socket, JSON) e validação.
 * Ela não interage com o console ou GUI diretamente, apenas retorna dados e exceções.
 */
public class PixClientManager {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final ObjectMapper objectMapper;
    private String sessionToken = null;
    private String loggedInUserName = null;

    public PixClientManager() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Tenta conectar ao servidor e estabelecer o protocolo.
     * @param ip IP do servidor
     * @param port Porta do servidor
     * @return String "Conectado" em caso de sucesso.
     * @throws IOException Se a conexão de rede falhar.
     * @throws Exception Se a validação do protocolo falhar.
     */
    public String connect(String ip, int port) throws IOException, Exception {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // 1. Envia 'conectar'
        ObjectNode connectJson = objectMapper.createObjectNode();
        connectJson.put("operacao", "conectar");
        String responseStr = sendRequestInternal(connectJson.toString()); // Envia JSON
        
        if (responseStr == null) {
            throw new IOException("Servidor não respondeu ao 'conectar'.");
        }

        // 2. Valida a resposta do 'conectar'
        try {
            Validator.validateServer(responseStr);
            JsonNode responseNode = objectMapper.readTree(responseStr);
            if (!responseNode.get("status").asBoolean()) {
                throw new Exception("Falha ao iniciar protocolo: " + responseNode.get("info").asText());
            }
            return "Conectado";
        } catch (Exception e) {
            throw new Exception("Resposta inválida do servidor: " + e.getMessage());
        }
    }

    /**
     * Envia uma requisição JSON ao servidor e retorna a resposta JSON como String.
     * Este é o método principal que a GUI usará.
     * @param requestNode O JSON (ObjectNode) a ser enviado.
     * @return A String JSON de resposta do servidor.
     * @throws IOException Se a conexão falhar.
     * @throws Exception Se a resposta do servidor violar o protocolo.
     */
    public String sendRequest(ObjectNode requestNode) throws IOException, Exception {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Não conectado ao servidor.");
        }

        // Adiciona o token à requisição, se existir e a operação não for de login/cadastro
        if (sessionToken != null &&
            !requestNode.get("operacao").asText().equals("usuario_login") &&
            !requestNode.get("operacao").asText().equals("usuario_criar")) {
            requestNode.put("token", sessionToken);
        }

        // 1. Envia a requisição
        String responseStr = sendRequestInternal(requestNode.toString());
        if (responseStr == null) {
            throw new IOException("Servidor não respondeu à operação: " + requestNode.get("operacao").asText());
        }

        // 2. Valida a resposta do servidor
        try {
            Validator.validateServer(responseStr);
        } catch (Exception e) {
            // Se a resposta do servidor for inválida, reportamos o erro
            reportarErroServidor(requestNode.get("operacao").asText(), e.getMessage());
            // E lançamos a exceção para a GUI tratar
            throw new Exception("Erro de Protocolo: Resposta inválida do Servidor: " + e.getMessage());
        }
        
        // 3. Processa tokens de login/logout
        JsonNode responseNode = objectMapper.readTree(responseStr);
        String operacao = requestNode.get("operacao").asText();

        if (operacao.equals("usuario_login") && responseNode.get("status").asBoolean()) {
            this.sessionToken = responseNode.get("token").asText();
            // GUI irá chamar 'lerUsuario' separadamente
        }
        if (operacao.equals("usuario_logout") && responseNode.get("status").asBoolean()) {
            this.sessionToken = null;
            this.loggedInUserName = null;
        }
        if (operacao.equals("usuario_deletar") && responseNode.get("status").asBoolean()) {
            this.sessionToken = null;
            this.loggedInUserName = null;
        }
        
        return responseStr;
    }

    /**
     * Método de baixo nível para enviar e receber.
     * (Este é o antigo sendRawRequest/sendRequest combinado, sem logs de console)
     */
    private String sendRequestInternal(String request) throws IOException {
        if (out == null || in == null) throw new IOException("Cliente não inicializado.");
        
        // O servidor verá isso nos logs da GUI dele
        out.println(request); 
        
        String response = in.readLine();
        
        // O servidor verá isso nos logs da GUI dele
        // (Nós não imprimimos mais nada no console do cliente)
        
        return response;
    }
    
    /**
     * Envia o reporte de erro ao servidor (Regra 4.11)
     */
    private void reportarErroServidor(String operacaoEnviada, String infoErro) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("operacao", "erro_servidor");
        json.put("operacao_enviada", operacaoEnviada);
        json.put("info", infoErro);
        sendRequestInternal(json.toString());
    }

    /**
     * Encerra a conexão com o servidor.
     */
    public void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignorar erro ao fechar
        }
    }
    
    // Getters para a GUI usar
    public String getSessionToken() { return sessionToken; }
    public String getLoggedInUserName() { return loggedInUserName; }
    public void setLoggedInUserName(String name) { this.loggedInUserName = name; }
    public ObjectMapper getObjectMapper() { return objectMapper; }
}