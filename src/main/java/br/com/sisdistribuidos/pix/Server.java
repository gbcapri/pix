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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Server {

    // Gerenciamento de tokens de sessão simples em memória.
    private static final Map<String, String> sessions = new HashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final UsuarioDAO usuarioDao = new UsuarioDAO();
    private static final TransacaoDAO transacaoDao = new TransacaoDAO();

    public static void main(String[] args) {
        Spark.port(4567);

        try {
            DatabaseManager.getConnection();
        } catch (Exception e) {
            System.err.println("Erro ao conectar com o banco de dados: " + e.getMessage());
            return;
        }

        System.out.println("Servidor Spark iniciado na porta 4567.");

        // Rota para criar um novo usuário (usuario_criar)
        Spark.post("/usuario/criar", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                Usuario novoUsuario = new Usuario(
                    rootNode.get("cpf").asText(),
                    rootNode.get("nome").asText(),
                    rootNode.get("senha").asText(),
                    100.00 // Saldo inicial
                );

                usuarioDao.criar(novoUsuario);
                
                return createSuccessResponse("usuario_criar", "Usuário criado com sucesso.");
            } catch (Exception e) {
                return createErrorResponse("usuario_criar", e.getMessage());
            }
        });

        // Rota para login de usuário (usuario_login)
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
                    sessions.put(token, cpf); // Associa o token ao CPF do usuário
                    
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

        // Rota para logout de usuário (usuario_logout)
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

        // Rota para ler dados do usuário (usuario_ler)
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
                response.put("usuario", usuario); // O Jackson serializará este objeto para JSON
                return objectMapper.writeValueAsString(response);
            } catch (Exception e) {
                return createErrorResponse("usuario_ler", e.getMessage());
            }
        });
        
        // Rota para atualizar dados do usuário (usuario_atualizar)
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
                
                // Atualiza apenas os campos presentes na requisição
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
        
        // Rota para deletar um usuário (usuario_deletar)
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
                sessions.remove(token); // Remove a sessão também
                
                return createSuccessResponse("usuario_deletar", "Usuário deletado com sucesso.");
            } catch (Exception e) {
                return createErrorResponse("usuario_deletar", e.getMessage());
            }
        });

        // Rota para criar uma transação (transacao_criar)
        Spark.post("/transacao/criar", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                String token = rootNode.get("token").asText();
                String cpfRecebedor = rootNode.get("cpf").asText();
                double valor = rootNode.get("valor").asDouble();
                
                String cpfEnviador = sessions.get(token);
                if (cpfEnviador == null) {
                    return createErrorResponse("transacao_criar", "Token de sessão inválido ou expirado.");
                }
                
                // Lógica de transação: checa saldo e atualiza saldos
                Usuario enviador = usuarioDao.ler(cpfEnviador);
                Usuario recebedor = usuarioDao.ler(cpfRecebedor);
                
                if (enviador.getSaldo() < valor) {
                    return createErrorResponse("transacao_criar", "Saldo insuficiente.");
                }
                
                enviador.setSaldo(enviador.getSaldo() - valor);
                recebedor.setSaldo(recebedor.getSaldo() + valor);
                
                // Atualiza saldos no banco de dados
                usuarioDao.atualizar(enviador);
                usuarioDao.atualizar(recebedor);
                
                // Cria e salva a transação no banco de dados
                Transacao novaTransacao = new Transacao(valor, cpfEnviador, cpfRecebedor);
                transacaoDao.criar(novaTransacao);

                return createSuccessResponse("transacao_criar", "Transação realizada com sucesso.");
            } catch (Exception e) {
                return createErrorResponse("transacao_criar", e.getMessage());
            }
        });

        // Rota para ler transações (transacao_ler)
        Spark.post("/transacao/ler", (req, res) -> {
            res.type("application/json");
            try {
                Validator.validateClient(req.body());
                JsonNode rootNode = objectMapper.readTree(req.body());
                String token = rootNode.get("token").asText();
                int pagina = rootNode.get("pagina").asInt();
                int limite = rootNode.get("limite").asInt();
                
                String cpf = sessions.get(token);
                if (cpf == null) {
                    return createErrorResponse("transacao_ler", "Token de sessão inválido ou expirado.");
                }

                List<Transacao> transacoes = transacaoDao.lerPorCpf(cpf, pagina, limite);
                
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

    // Método utilitário para gerar respostas de sucesso.
    private static String createSuccessResponse(String operacao, String info) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("operacao", operacao);
        response.put("status", true);
        response.put("info", info);
        return objectMapper.writeValueAsString(response);
    }
    
    // Método utilitário para gerar respostas de erro.
    private static String createErrorResponse(String operacao, String info) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("operacao", operacao);
        response.put("status", false);
        response.put("info", info);
        return objectMapper.writeValueAsString(response);
    }
}
