package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.database.DatabaseManager;
import br.com.sisdistribuidos.pix.database.TransacaoDAO;
import br.com.sisdistribuidos.pix.database.UsuarioDAO;
import br.com.sisdistribuidos.pix.model.Transacao;
import br.com.sisdistribuidos.pix.model.Usuario;
import br.com.sisdistribuidos.pix.validador.Validator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ObjectMapper objectMapper;
    private final UsuarioDAO usuarioDao;
    private final TransacaoDAO transacaoDao;

    private final ServerGUI serverGUI;
    private final Map<String, String> sessions;
    
    private final Map<String, String> activeClients; // Mapa compartilhado de exibições
    private final String clientConnectionId; // ID único da conexão (IP:Porta)
    private final String clientIpDisplay;    // Texto base (IP)
    
    private String userToken = null;

    public ClientHandler(Socket socket, ServerGUI serverGUI, Map<String, String> sessions, Map<String, String> activeClients) {
        this.clientSocket = socket;
        this.serverGUI = serverGUI;
        this.sessions = sessions;
        this.activeClients = activeClients;
        this.objectMapper = new ObjectMapper();
        this.usuarioDao = new UsuarioDAO();
        this.transacaoDao = new TransacaoDAO();
        
        // Define IDs para controle da lista
        // Usamos IP:Porta como chave única no mapa para não sobrescrever clientes no mesmo IP
        this.clientConnectionId = socket.getRemoteSocketAddress().toString(); 
        
        // Define o texto base de exibição (IP)
        // Dica: Adicionei a porta para você conseguir distinguir conexões locais, mas pode remover se quiser só o IP.
        this.clientIpDisplay = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        atualizarStatusGUI(clientIpDisplay);
        
        try (
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            String firstLine = in.readLine();
            if (firstLine == null) return;
            serverGUI.log("Servidor recebeu: " + firstLine);

            String operacaoConexao = "conectar";
            try {
                // Valida a sintaxe e o protocolo da primeira mensagem
                Validator.validateClient(firstLine); 
                JsonNode firstNode = objectMapper.readTree(firstLine);
                operacaoConexao = firstNode.path("operacao").asText();

                if (!operacaoConexao.equals("conectar")) {
                    throw new IllegalArgumentException("Protocolo violado: a primeira operação deve ser 'conectar'.");
                }
                
            } catch (Exception e) {
                String errorResponse = createErrorResponse(operacaoConexao, e.getMessage());
                serverGUI.log("Servidor enviou: " + errorResponse);
                out.println(errorResponse);
                return; 
            }

            String successResponse = createSuccessResponse("conectar", "Conexão estabelecida com sucesso.");
            serverGUI.log("Servidor enviou: " + successResponse);
            out.println(successResponse);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                serverGUI.log("Servidor recebeu: " + inputLine);
                String response = processRequest(inputLine);
                
                //Se a resposta for null, encerra a conexão
                if (response == null) {
                    serverGUI.log("Servidor: Erro de sintaxe JSON detectado. Encerrando conexão.");
                    break;
                }
                
                serverGUI.log("Servidor enviou: " + response);
                out.println(response);
            }
        } catch (Exception e) {
            serverGUI.log("Erro na comunicação com o cliente: " + e.getMessage());
        } finally {
            try {
                // Remove a sessão do usuário ao desconectar
                if (userToken != null) {
                    String cpf = sessions.remove(userToken);
                    //if (cpf != null) {
                    //    serverGUI.log("Sessão encerrada para: " + cpf);
                    //    serverGUI.updateUserList(new HashSet<>(sessions.values()));
                    //}
                }
                
                // 4. AO DESCONECTAR TOTALMENTE: Remove da lista visual
                activeClients.remove(clientConnectionId);
                serverGUI.updateUserList(new HashSet<>(activeClients.values()));
                
                clientSocket.close();
                serverGUI.log("Cliente desconectado: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) { /* Ignorar */ }
        }
    }
    
    // Método auxiliar para atualizar a GUI com o texto correto
    private void atualizarStatusGUI(String textoExibicao) {
        activeClients.put(clientConnectionId, textoExibicao);
        // Enviamos os valores do activeClients (os textos) para a GUI
        serverGUI.updateUserList(new HashSet<>(activeClients.values()));
    }

    String processRequest(String jsonRequest) {
        JsonNode rootNode;
        String operacao = "desconhecida";
        try {
            // Tenta parsear o JSON. Se falhar, é erro de sintaxe (Regra 5.2)
            rootNode = objectMapper.readTree(jsonRequest);
            if (rootNode.has("operacao")) {
                operacao = rootNode.get("operacao").asText();
            } else {
                 throw new Exception("O campo 'operacao' é obrigatório."); // Força erro de sintaxe
            }
        } catch (Exception e) {
            return null; // Encerra a conexão
        }

        try {
            Validator.validateClient(jsonRequest); 
        } catch (Exception e) {
            // REGRA 5.1: Erro de Validação (Regra de negócio/protocolo). Retorna erro JSON.
            serverGUI.log("ERRO de Validação: " + e.getMessage());
            return createErrorResponse(operacao, e.getMessage());
        }
        try {

            switch (operacao) {
                case "usuario_criar": return handleCriarUsuario(rootNode);
                case "usuario_login": return handleLogin(rootNode);
                case "usuario_logout": return handleLogout(rootNode);
                case "usuario_ler": return handleLerUsuario(rootNode);
                case "usuario_atualizar": return handleAtualizarUsuario(rootNode);
                case "usuario_deletar": return handleDeletarUsuario(rootNode);
                case "transacao_criar": return handleCriarTransacao(rootNode);
                case "transacao_ler": return handleLerTransacoes(rootNode);
                case "depositar": return handleDepositar(rootNode);
                case "erro_servidor": return handleErroServidor(rootNode);
                default: return createErrorResponse("desconhecida", "Operação não reconhecida.");
            }
        } catch (Exception e) {
            serverGUI.log("ERRO Interno no Servidor (Operação: " + operacao + "): " + e.getMessage());
            return createErrorResponse(operacao, "Erro interno no servidor: " + e.getMessage());
        }
    }
    
    private boolean hasMoreThanTwoDecimalPlaces(double valor) {
        // Usa BigDecimal para verificar a escala (número de casas decimais)
        return BigDecimal.valueOf(valor).scale() > 2;
    }

    private String handleCriarUsuario(JsonNode rootNode) {
    try {
        Usuario novoUsuario = new Usuario(
            rootNode.get("cpf").asText(),
            rootNode.get("nome").asText(),
            rootNode.get("senha").asText(),
            0.00
        );
        usuarioDao.criar(novoUsuario);
        return createSuccessResponse("usuario_criar", "Usuário criado com sucesso.");
    } catch (SQLException e) {
        String erroReal = e.getMessage(); // Captura o erro real
        if (e.getErrorCode() == 19 && erroReal.contains("UNIQUE constraint failed: usuario.cpf")) {
             serverGUI.log("ERRO em [usuario_criar]: Tentativa de criar CPF duplicado.");
            return createErrorResponse("usuario_criar", "Este CPF já está cadastrado.");
        } else {
            // Loga o erro real
            serverGUI.log("ERRO em [usuario_criar] (SQLException): " + erroReal);
            // --- CORREÇÃO AQUI ---
            // RETORNA O ERRO REAL PARA O CLIENTE
            return createErrorResponse("usuario_criar", "Erro no banco de dados: " + erroReal);
        }
    } catch (Exception e) {
        String erroReal = e.getMessage();
        serverGUI.log("ERRO em [usuario_criar] (Exception): " + erroReal);
        // --- CORREÇÃO AQUI ---
        // RETORNA O ERRO REAL PARA O CLIENTE
        return createErrorResponse("usuario_criar", "Ocorreu um erro inesperado: " + erroReal);
    }
}
    
    private String handleLogin(JsonNode rootNode) throws Exception {
        String cpf = rootNode.get("cpf").asText();
        String senha = rootNode.get("senha").asText();
        Usuario usuario = usuarioDao.ler(cpf);
        if (usuario != null && usuario.getSenha().equals(senha)) {
            String token = UUID.randomUUID().toString();
            
            this.userToken = token; // Armazena o token neste handler
            sessions.put(token, cpf); // Adiciona ao mapa compartilhado
            serverGUI.updateUserList(new HashSet<>(sessions.values())); // Atualiza a GUI
            serverGUI.log("Login OK: " + cpf);
            
            atualizarStatusGUI(clientIpDisplay + " - " + cpf);
            
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
        String cpf = sessions.get(token); // Pega o CPF antes de remover
        if (cpf != null && sessions.remove(token, cpf)) {
            this.userToken = null; // Limpa o token deste handler
            serverGUI.updateUserList(new HashSet<>(sessions.values())); // Atualiza a GUI
            serverGUI.log("Logout OK: " + cpf);
            
            atualizarStatusGUI(clientIpDisplay);
            
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
        
        usuario.setSenha(null); // Remove a senha da resposta
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
        
        usuarioDao.deletar(cpf); // O "ON DELETE CASCADE" ou "SET NULL" (depende da sua versão) cuida das transações
        sessions.remove(token);
        this.userToken = null; // Limpa o token deste handler
        serverGUI.updateUserList(new HashSet<>(sessions.values())); // Atualiza a GUI
        serverGUI.log("Conta Deletada: " + cpf);
        
        atualizarStatusGUI(clientIpDisplay);
        
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
            
            if (cpfEnviador.equals(cpfRecebedor)) {
                return createErrorResponse("transacao_criar", "Você não pode enviar um PIX para si mesmo. Use a operação 'Depositar'.");
            }
            
            if (!Double.isFinite(valor)) {
                 return createErrorResponse("transacao_criar", "Valor inválido (Infinito ou NaN).");
            }
            if (hasMoreThanTwoDecimalPlaces(valor)) {
                 return createErrorResponse("transacao_criar", "Valor inválido. A transação não pode ter mais que duas casas decimais.");
            }
            if (valor <= 0) { 
                 return createErrorResponse("transacao_criar", "Valor da transação deve ser positivo.");
            }

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
            
            // CONSTRUTOR CORRIGIDO: Passa os CPFs (String) como esperado
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
        
        try {
            LocalDate dataInicial = LocalDate.parse(dataInicialStr.substring(0, 10));
            LocalDate dataFinal = LocalDate.parse(dataFinalStr.substring(0, 10));
            
            long dias = ChronoUnit.DAYS.between(dataInicial, dataFinal);

            if (dias < 0) {
                 return createErrorResponse("transacao_ler", "Data inicial não pode ser maior que a data final.");
            }
            if (dias > 31) {
                return createErrorResponse("transacao_ler", "O período máximo do extrato é de 31 dias.");
            }
        } catch (Exception e) {
             serverGUI.log("Erro ao parsear datas (já validadas?): " + e.getMessage());
             return createErrorResponse("transacao_ler", "Formato de data inválido para cálculo de período.");
        }
        
        List<Transacao> transacoes = transacaoDao.lerPorCpfComDatas(cpf, dataInicialStr, dataFinalStr);
        
        // Esta é a lógica original (sem a Correção 1 - "usuário deletado")
        // Ela enriquece a transação com os nomes atuais do DB.
        for (Transacao t : transacoes) {
            Usuario enviadorDb = usuarioDao.ler(t.getCpfEnviador());
            Usuario recebedorDb = usuarioDao.ler(t.getCpfRecebedor());

            if (enviadorDb != null) {
                Usuario enviadorSimples = new Usuario();
                enviadorSimples.setNome(enviadorDb.getNome());
                enviadorSimples.setCpf(enviadorDb.getCpf());
                t.setUsuarioEnviador(enviadorSimples);
            }

            if (recebedorDb != null) {
                Usuario recebedorSimples = new Usuario();
                recebedorSimples.setNome(recebedorDb.getNome());
                recebedorSimples.setCpf(recebedorDb.getCpf());
                t.setUsuarioRecebedor(recebedorSimples);
            }
        }
        
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("operacao", "transacao_ler");
        responseMap.put("status", true);
        responseMap.put("info", "Transações recuperadas com sucesso.");
        responseMap.put("transacoes", transacoes);
        return objectMapper.writeValueAsString(responseMap);
    }
    
    private String handleDepositar(JsonNode rootNode) {
        Connection conn = null;
        try {
            String token = rootNode.get("token").asText();
            String cpf = sessions.get(token);
            if (cpf == null) {
                return createErrorResponse("depositar", "Token de sessão inválido.");
            }

            double valor = rootNode.path("valor_enviado").asDouble();
            
            if (!Double.isFinite(valor)) {
                 return createErrorResponse("depositar", "Valor inválido (Infinito ou NaN).");
            }
            if (hasMoreThanTwoDecimalPlaces(valor)) {
                 return createErrorResponse("depositar", "Valor inválido. O depósito não pode ter mais que duas casas decimais.");
            }
             if (valor <= 0) { 
                 return createErrorResponse("depositar", "Valor do depósito deve ser positivo.");
            }
            
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            Usuario usuario = usuarioDao.lerComConexao(conn, cpf);
            if (usuario == null) {
                conn.rollback();
                return createErrorResponse("depositar", "Usuário não encontrado.");
            }

            usuario.setSaldo(usuario.getSaldo() + valor);
            usuarioDao.atualizarComConexao(conn, usuario);

            // CONSTRUTOR CORRIGIDO: Passa o CPF (String) para enviador e recebedor
            Transacao deposito = new Transacao(valor, cpf, cpf); 
            transacaoDao.criarComConexao(conn, deposito);

            conn.commit();

            return createSuccessResponse("depositar", "Deposito realizado com sucesso.");

        } catch (Exception e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            serverGUI.log("ERRO em [depositar] (Exception): " + e.getMessage());
            return createErrorResponse("depositar", "Falha no depósito: " + e.getMessage());
        } finally {
            if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
        }
    }
    
    private String handleErroServidor(JsonNode rootNode) throws Exception {
        String operacaoEnviada = rootNode.path("operacao_enviada").asText();
        String infoErro = rootNode.path("info").asText();
        
        serverGUI.log("[ERRO REPORTADO PELO CLIENTE] Operação: " + operacaoEnviada + " | Info: " + infoErro);
        
        return createSuccessResponse("erro_servidor", "Erro logado pelo servidor.");
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