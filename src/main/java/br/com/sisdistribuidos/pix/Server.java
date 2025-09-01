package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.database.DatabaseManager;
import br.com.sisdistribuidos.pix.database.TransacaoDAO;
import br.com.sisdistribuidos.pix.database.UsuarioDAO;
import br.com.sisdistribuidos.pix.model.Transacao;
import br.com.sisdistribuidos.pix.model.Usuario;
import br.com.sisdistribuidos.pix.validador.Validator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Spark;

import java.sql.Connection;
import java.time.LocalDateTime; // Adicionado para manipulação de datas
import java.time.format.DateTimeFormatter; // Adicionado para formatação de datas
import java.time.temporal.ChronoUnit; // Adicionado para calcular a diferença entre datas
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Server {

    private static final Map<String, String> sessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final UsuarioDAO usuarioDao = new UsuarioDAO();
    private static final TransacaoDAO transacaoDao = new TransacaoDAO();
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static void main(String[] args) {
        Spark.port(4567);

        try {
            DatabaseManager.getConnection();
        } catch (Exception e) {
            System.err.println("Erro ao conectar com o banco de dados: " + e.getMessage());
            return;
        }

        System.out.println("Servidor Spark iniciado na porta 4567.");

        Spark.post("/usuario/criar", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                Usuario novoUsuario = new Usuario(
                    rootNode.get("cpf").asText(),
                    rootNode.get("nome").asText(),
                    rootNode.get("senha").asText(),
                    100.00
                );

                usuarioDao.criar(novoUsuario);
                
                return createSuccessResponse("usuario_criar", "Usuário criado com sucesso.");
            } catch (Exception e) {
                return createErrorResponse("usuario_criar", e.getMessage());
            }
        });

        Spark.post("/usuario/login", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                String cpf = rootNode.get("cpf").asText();
                String senha = rootNode.get("senha").asText();

                Usuario usuario = usuarioDao.ler(cpf);

                if (usuario != null && usuario.getSenha().equals(senha)) {
                    String token = UUID.randomUUID().toString();
                    sessions.put(token, cpf);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("operacao", "usuario_login");
                    response.put("status", true);
                    response.put("info", "Login bem-sucedido.");
                    response.put("token", token);
                    return objectMapper.writeValueAsString(response);
                } else {
                    return createErrorResponse("usuario_login", "CPF ou senha inválidos.");
                }
            } catch (Exception e) {
                return createErrorResponse("usuario_login", e.getMessage());
            }
        });

        Spark.post("/usuario/logout", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                String token = rootNode.get("token").asText();

                sessions.remove(token);
                return createSuccessResponse("usuario_logout", "Logout realizado com sucesso.");
            } catch (Exception e) {
                return createErrorResponse("usuario_logout", e.getMessage());
            }
        });

        Spark.post("/usuario/ler", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                String token = rootNode.get("token").asText();
                
                String cpf = sessions.get(token);
                if (cpf == null) {
                    return createErrorResponse("usuario_ler", "Token de sessão inválido ou expirado.");
                }
                
                Usuario usuario = usuarioDao.ler(cpf);
                if (usuario == null) {
                    return createErrorResponse("usuario_ler", "Usuário não encontrado.");
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("operacao", "usuario_ler");
                response.put("status", true);
                response.put("info", "Dados do usuário recuperados com sucesso.");
                response.put("usuario", usuario);
                return objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                return createErrorResponse("usuario_ler", e.getMessage());
            }
        });
        
        Spark.post("/usuario/atualizar", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                String token = rootNode.get("token").asText();
                
                String cpf = sessions.get(token);
                if (cpf == null) {
                    return createErrorResponse("usuario_atualizar", "Token de sessão inválido ou expirado.");
                }
                
                Usuario usuarioExistente = usuarioDao.ler(cpf);
                if (usuarioExistente == null) {
                    return createErrorResponse("usuario_atualizar", "Usuário não encontrado.");
                }
                
                JsonNode usuarioNode = rootNode.get("usuario");
                if (usuarioNode != null) {
                    if (usuarioNode.hasNonNull("nome")) {
                        usuarioExistente.setNome(usuarioNode.get("nome").asText());
                    }
                    if (usuarioNode.hasNonNull("senha")) {
                        usuarioExistente.setSenha(usuarioNode.get("senha").asText());
                    }
                }
                usuarioDao.atualizar(usuarioExistente);
                
                return createSuccessResponse("usuario_atualizar", "Usuário atualizado com sucesso.");
            } catch (Exception e) {
                return createErrorResponse("usuario_atualizar", e.getMessage());
            }
        });
        
        Spark.post("/usuario/deletar", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                String token = rootNode.get("token").asText();
                
                String cpf = sessions.get(token);
                if (cpf == null) {
                    return createErrorResponse("usuario_deletar", "Token de sessão inválido ou expirado.");
                }
                
                usuarioDao.deletar(cpf);
                sessions.remove(token);
                
                return createSuccessResponse("usuario_deletar", "Usuário deletado com sucesso.");
            } catch (Exception e) {
                return createErrorResponse("usuario_deletar", e.getMessage());
            }
        });

        Spark.post("/transacao/criar", (req, res) -> {
            res.type("application/json");
            Connection conn = null;
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                String token = rootNode.get("token").asText();
                String cpfRecebedor = rootNode.get("cpf_destino").asText();
                double valor = rootNode.get("valor").asDouble();
                
                String cpfEnviador = sessions.get(token);
                if (cpfEnviador == null) {
                    return createErrorResponse("transacao_criar", "Token de sessão inválido ou expirado.");
                }
                
                conn = DatabaseManager.getConnection();
                conn.setAutoCommit(false);

                Usuario enviador = usuarioDao.lerComConexao(conn, cpfEnviador);
                Usuario recebedor = usuarioDao.lerComConexao(conn, cpfRecebedor);
                
                if (enviador == null || recebedor == null) {
                    conn.rollback();
                    return createErrorResponse("transacao_criar", "Usuário remetente ou recebedor não encontrado.");
                }

                if (enviador.getSaldo() < valor) {
                    conn.rollback();
                    return createErrorResponse("transacao_criar", "Saldo insuficiente.");
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
                if (conn != null) {
                    conn.rollback();
                }
                return createErrorResponse("transacao_criar", "Erro na transação: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.close();
                }
            }
        });

        // Rota para ler transações (transacao_ler)
        Spark.post("/transacao/ler", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                String token = rootNode.get("token").asText();
                String dataInicialStr = rootNode.get("data_inicial").asText(); // Novo campo
                String dataFinalStr = rootNode.get("data_final").asText();     // Novo campo
                
                // Valida o token
                String cpf = sessions.get(token);
                if (cpf == null) {
                    return createErrorResponse("transacao_ler", "Token de sessão inválido ou expirado.");
                }

                // Parse e validação do intervalo de 31 dias
                LocalDateTime dataInicial = LocalDateTime.parse(dataInicialStr, ISO_FORMATTER);
                LocalDateTime dataFinal = LocalDateTime.parse(dataFinalStr, ISO_FORMATTER);

                if (ChronoUnit.DAYS.between(dataInicial, dataFinal) > 31) {
                    return createErrorResponse("transacao_ler", "O intervalo de datas não pode exceder 31 dias.");
                }
                
                // Usa o novo método do DAO com os campos de data
                List<Transacao> transacoes = transacaoDao.lerPorCpfComDatas(cpf, dataInicialStr, dataFinalStr);
                
                Map<String, Object> response = new HashMap<>();
                response.put("operacao", "transacao_ler");
                response.put("status", true);
                response.put("info", "Transações recuperadas com sucesso.");
                response.put("transacoes", transacoes);
                return objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                return createErrorResponse("transacao_ler", e.getMessage());
            }
        });
    }

    private static String createSuccessResponse(String operacao, String info) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("operacao", operacao);
        response.put("status", true);
        response.put("info", info);
        return objectMapper.writeValueAsString(response);
    }
    
    private static String createErrorResponse(String operacao, String info) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("operacao", operacao);
        response.put("status", false);
        response.put("info", info);
        return objectMapper.writeValueAsString(response);
    }
}