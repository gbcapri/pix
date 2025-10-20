package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.database.DatabaseManager;
import br.com.sisdistribuidos.pix.database.TransacaoDAO;
import br.com.sisdistribuidos.pix.database.UsuarioDAO;
import br.com.sisdistribuidos.pix.model.Transacao;
import br.com.sisdistribuidos.pix.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ObjectMapper objectMapper;
    private final UsuarioDAO usuarioDao;
    private final TransacaoDAO transacaoDao;

    private static final Map<String, String> sessions = new HashMap<>();

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.objectMapper = new ObjectMapper();
        this.usuarioDao = new UsuarioDAO();
        this.transacaoDao = new TransacaoDAO();
    }

    @Override
    public void run() {
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            String firstLine = in.readLine();
            if (firstLine == null) return;
            System.out.println("Servidor recebeu: " + firstLine);

            JsonNode firstNode = objectMapper.readTree(firstLine);
            if (!firstNode.path("operacao").asText().equals("conectar")) {
                String errorResponse = createErrorResponse("conectar", "Protocolo violado: a primeira operação deve ser 'conectar'.");
                System.out.println("Servidor enviou: " + errorResponse);
                out.println(errorResponse);
                return;
            }

            String successResponse = createSuccessResponse("conectar", "Conexão estabelecida com sucesso.");
            System.out.println("Servidor enviou: " + successResponse);
            out.println(successResponse);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Servidor recebeu: " + inputLine);
                String response = processRequest(inputLine);
                System.out.println("Servidor enviou: " + response);
                out.println(response);
            }
        } catch (Exception e) {
            System.err.println("Erro na comunicação com o cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Cliente desconectado: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) { /* Ignorar */ }
        }
    }

    String processRequest(String jsonRequest) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonRequest);
            String operacao = rootNode.get("operacao").asText();

            switch (operacao) {
                case "usuario_criar": return handleCriarUsuario(rootNode);
                case "usuario_login": return handleLogin(rootNode);
                case "usuario_logout": return handleLogout(rootNode);
                case "usuario_ler": return handleLerUsuario(rootNode);
                case "usuario_atualizar": return handleAtualizarUsuario(rootNode);
                case "usuario_deletar": return handleDeletarUsuario(rootNode);
                case "transacao_criar": return handleCriarTransacao(rootNode);
                case "transacao_ler": return handleLerTransacoes(rootNode);
                default: return createErrorResponse("desconhecida", "Operação não reconhecida.");
            }
        } catch (Exception e) {
            return createErrorResponse("erro_processamento", "Erro ao processar: " + e.getMessage());
        }
    }

    private String handleCriarUsuario(JsonNode rootNode) {
    try {
        Usuario novoUsuario = new Usuario(
            rootNode.get("cpf").asText(),
            rootNode.get("nome").asText(),
            rootNode.get("senha").asText(),
            100.00
        );
        usuarioDao.criar(novoUsuario);
        return createSuccessResponse("usuario_criar", "Usuário criado com sucesso.");
    } catch (SQLException e) {
        // Verifica se o erro é de violação de chave única (CPF duplicado)
        // O código de erro 19 é comum para SQLITE_CONSTRAINT, e a mensagem geralmente contém "UNIQUE"
        if (e.getErrorCode() == 19 && e.getMessage().contains("UNIQUE constraint failed: usuario.cpf")) {
             System.err.println("ERRO em [usuario_criar]: Tentativa de criar CPF duplicado.");
            return createErrorResponse("usuario_criar", "Este CPF já está cadastrado.");
        } else {
            // Outro erro de SQL
            System.err.println("ERRO em [usuario_criar] (SQLException): " + e.getMessage());
            return createErrorResponse("usuario_criar", "Erro no banco de dados ao tentar criar usuário.");
        }
    } catch (Exception e) {
        // Outros erros inesperados (ex: falha ao ler JSON)
        System.err.println("ERRO em [usuario_criar] (Exception): " + e.getMessage());
        return createErrorResponse("usuario_criar", "Ocorreu um erro inesperado ao criar o usuário.");
    }
}
    
    private String handleLogin(JsonNode rootNode) throws Exception {
        String cpf = rootNode.get("cpf").asText();
        String senha = rootNode.get("senha").asText();
        Usuario usuario = usuarioDao.ler(cpf);
        if (usuario != null && usuario.getSenha().equals(senha)) {
            String token = UUID.randomUUID().toString();
            sessions.put(token, cpf);
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("operacao", "usuario_login");
            responseMap.put("status", true);
            responseMap.put("info", "Login bem-sucedido.");
            responseMap.put("token", token);
            return objectMapper.writeValueAsString(responseMap);
        } else {
            return createErrorResponse("usuario_login", "CPF ou senha inválidos.");
        }
    }

    private String handleLogout(JsonNode rootNode) throws Exception {
        String token = rootNode.get("token").asText();
        if (sessions.remove(token) != null) {
            return createSuccessResponse("usuario_logout", "Logout realizado com sucesso.");
        } else {
            return createErrorResponse("usuario_logout", "Token inválido.");
        }
    }
    
    private String handleLerUsuario(JsonNode rootNode) throws Exception {
        String token = rootNode.get("token").asText();
        String cpf = sessions.get(token);
        if (cpf == null) return createErrorResponse("usuario_ler", "Token inválido.");
        
        Usuario usuario = usuarioDao.ler(cpf);
        if (usuario == null) return createErrorResponse("usuario_ler", "Usuário não encontrado.");
        
        usuario.setSenha(null);
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("operacao", "usuario_ler");
        responseMap.put("status", true);
        responseMap.put("info", "Dados recuperados.");
        responseMap.put("usuario", usuario);
        return objectMapper.writeValueAsString(responseMap);
    }

    private String handleAtualizarUsuario(JsonNode rootNode) throws Exception {
        String token = rootNode.get("token").asText();
        String cpf = sessions.get(token);
        if (cpf == null) return createErrorResponse("usuario_atualizar", "Token inválido.");
        
        Usuario usuario = usuarioDao.ler(cpf);
        if (usuario == null) return createErrorResponse("usuario_atualizar", "Usuário não encontrado.");
        
        JsonNode usuarioNode = rootNode.get("usuario");
        if (usuarioNode.has("nome")) usuario.setNome(usuarioNode.get("nome").asText());
        if (usuarioNode.has("senha")) usuario.setSenha(usuarioNode.get("senha").asText());
        
        usuarioDao.atualizar(usuario);
        return createSuccessResponse("usuario_atualizar", "Usuário atualizado com sucesso.");
    }

    private String handleDeletarUsuario(JsonNode rootNode) throws Exception {
        String token = rootNode.get("token").asText();
        String cpf = sessions.get(token);
        if (cpf == null) return createErrorResponse("usuario_deletar", "Token inválido.");
        
        usuarioDao.deletar(cpf);
        sessions.remove(token);
        return createSuccessResponse("usuario_deletar", "Usuário deletado com sucesso.");
    }

    private String handleCriarTransacao(JsonNode rootNode) {
        Connection conn = null;
        try {
            String token = rootNode.get("token").asText();
            String cpfEnviador = sessions.get(token);
            if (cpfEnviador == null) {
                return createErrorResponse("transacao_criar", "Token de sessão inválido.");
            }
            
            String cpfRecebedor = rootNode.get("cpf_destino").asText();
            double valor = rootNode.get("valor").asDouble();

            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            Usuario enviador = usuarioDao.lerComConexao(conn, cpfEnviador);
            Usuario recebedor = usuarioDao.lerComConexao(conn, cpfRecebedor);

            if (enviador == null || recebedor == null || enviador.getSaldo() < valor) {
                conn.rollback();
                String motivo = "Saldo insuficiente ou um dos usuários não foi encontrado.";
                return createErrorResponse("transacao_criar", motivo);
            }

            enviador.setSaldo(enviador.getSaldo() - valor);
            recebedor.setSaldo(recebedor.getSaldo() + valor);
            usuarioDao.atualizarComConexao(conn, enviador);
            usuarioDao.atualizarComConexao(conn, recebedor);
            
            Transacao novaTransacao = new Transacao(valor, cpfEnviador, cpfRecebedor);
            transacaoDao.criarComConexao(conn, novaTransacao);
            conn.commit();

            return createSuccessResponse("transacao_criar", "Transação realizada com sucesso.");
        } catch (Exception e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            return createErrorResponse("transacao_criar", "Falha na transação: " + e.getMessage());
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
        }
    }

    private String handleLerTransacoes(JsonNode rootNode) throws Exception {
        String token = rootNode.get("token").asText();
        String cpf = sessions.get(token);
        if (cpf == null) {
            return createErrorResponse("transacao_ler", "Token de sessão inválido.");
        }
        
        String dataInicialStr = rootNode.get("data_inicial").asText();
        String dataFinalStr = rootNode.get("data_final").asText();
        
        List<Transacao> transacoes = transacaoDao.lerPorCpfComDatas(cpf, dataInicialStr, dataFinalStr);
        
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("operacao", "transacao_ler");
        responseMap.put("status", true);
        responseMap.put("info", "Transações recuperadas com sucesso.");
        responseMap.put("transacoes", transacoes);
        return objectMapper.writeValueAsString(responseMap);
    }
    
    private String createSuccessResponse(String operacao, String info) throws Exception {
        return objectMapper.writeValueAsString(Map.of("operacao", operacao, "status", true, "info", info));
    }

    private String createErrorResponse(String operacao, String info) {
        try {
            return objectMapper.writeValueAsString(Map.of("operacao", operacao, "status", false, "info", info));
        } catch (Exception e) {
            return "{\"status\":false, \"info\":\"Erro interno no servidor.\"}";
        }
    }
}