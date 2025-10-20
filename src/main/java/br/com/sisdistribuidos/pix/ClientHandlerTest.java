package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.database.TransacaoDAO;
import br.com.sisdistribuidos.pix.database.UsuarioDAO;
import br.com.sisdistribuidos.pix.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClientHandlerTest {

    @Mock
    private Socket mockClientSocket;
    @Mock
    private UsuarioDAO mockUsuarioDao;
    @Mock
    private TransacaoDAO mockTransacaoDao;
    // Não precisamos mockar os streams (PrintWriter, BufferedReader) para testar a lógica interna

    // Classe sob teste. Usaremos reflection para injetar mocks nos DAOs.
    private ClientHandler clientHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mapa de sessões simulado para testes que precisam de token
    private static Map<String, String> mockSessions;

    @BeforeEach
    void setUp() throws Exception {
        // Instancia ClientHandler passando o mock do Socket
        clientHandler = new ClientHandler(mockClientSocket);

        // Injeta os DAOs mocks no clientHandler usando reflection
        setMockField(clientHandler, "usuarioDao", mockUsuarioDao);
        setMockField(clientHandler, "transacaoDao", mockTransacaoDao);

        // Cria e injeta um mapa de sessões mockado para podermos controlar os tokens
        mockSessions = new HashMap<>();
        setStaticMockField(ClientHandler.class, "sessions", mockSessions);
    }

    // --- TESTES DE ERRO EXISTENTES (revisados) ---

    @Test
    void testProcessRequest_UnknownOperation() throws Exception {
        String jsonRequest = "{\"operacao\":\"operacao_invalida\"}";
        String expectedResponse = "{\"operacao\":\"desconhecida\",\"status\":false,\"info\":\"Operação não reconhecida.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testProcessRequest_InvalidJson() throws Exception {
        String jsonRequest = "{\"operacao\": invalido";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertTrue(actualResponse.contains("\"status\":false"));
        assertTrue(actualResponse.contains("\"operacao\":\"erro_processamento\""));
        // A mensagem exata do Jackson pode variar, então verificamos apenas partes
        assertTrue(actualResponse.contains("Erro ao processar:"));
    }

    @Test
    void testHandleCriarUsuario_DuplicateCpf() throws Exception {
        SQLException duplicateException = new SQLException("UNIQUE constraint failed: usuario.cpf", null, 19);
        doThrow(duplicateException).when(mockUsuarioDao).criar(any(Usuario.class));

        String jsonRequest = "{\"operacao\":\"usuario_criar\",\"cpf\":\"111.111.111-11\",\"nome\":\"Teste\",\"senha\":\"123456\"}";
        String expectedResponse = "{\"operacao\":\"usuario_criar\",\"status\":false,\"info\":\"Este CPF já está cadastrado.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleCriarUsuario_DatabaseError() throws Exception {
        SQLException dbException = new SQLException("Erro genérico no banco", null, 1);
        doThrow(dbException).when(mockUsuarioDao).criar(any(Usuario.class));

        String jsonRequest = "{\"operacao\":\"usuario_criar\",\"cpf\":\"111.111.111-11\",\"nome\":\"Teste\",\"senha\":\"123456\"}";
        String expectedResponse = "{\"operacao\":\"usuario_criar\",\"status\":false,\"info\":\"Erro no banco de dados ao tentar criar usuário.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleLogin_InvalidCredentials() throws Exception {
        when(mockUsuarioDao.ler(anyString())).thenReturn(null);
        String jsonRequest = "{\"operacao\":\"usuario_login\",\"cpf\":\"111.111.111-11\",\"senha\":\"senha_errada\"}";
        String expectedResponse = "{\"operacao\":\"usuario_login\",\"status\":false,\"info\":\"CPF ou senha inválidos.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleLogin_WrongPassword() throws Exception {
        Usuario user = new Usuario("111.111.111-11", "Teste", "senha_correta", 100.0);
        when(mockUsuarioDao.ler(eq("111.111.111-11"))).thenReturn(user);
        String jsonRequest = "{\"operacao\":\"usuario_login\",\"cpf\":\"111.111.111-11\",\"senha\":\"senha_errada\"}";
        String expectedResponse = "{\"operacao\":\"usuario_login\",\"status\":false,\"info\":\"CPF ou senha inválidos.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleLogout_InvalidToken() throws Exception {
        String jsonRequest = "{\"operacao\":\"usuario_logout\",\"token\":\"token_invalido\"}";
        String expectedResponse = "{\"operacao\":\"usuario_logout\",\"status\":false,\"info\":\"Token inválido.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleLerUsuario_InvalidToken() throws Exception {
        String jsonRequest = "{\"operacao\":\"usuario_ler\",\"token\":\"token_invalido\"}";
        String expectedResponse = "{\"operacao\":\"usuario_ler\",\"status\":false,\"info\":\"Token inválido.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleLerUsuario_UserNotFoundAfterLogin() throws Exception {
        // Simula um token válido no mapa
        mockSessions.put("token_valido", "111.111.111-11");
        // Simula o DAO não encontrando o usuário (caso raro, mas possível)
        when(mockUsuarioDao.ler(eq("111.111.111-11"))).thenReturn(null);

        String jsonRequest = "{\"operacao\":\"usuario_ler\",\"token\":\"token_valido\"}";
        String expectedResponse = "{\"operacao\":\"usuario_ler\",\"status\":false,\"info\":\"Usuário não encontrado.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    // --- NOVOS TESTES DE ERRO ---

    // -- ATUALIZAR USUÁRIO --
    @Test
    void testHandleAtualizarUsuario_InvalidToken() throws Exception {
        String jsonRequest = "{\"operacao\":\"usuario_atualizar\",\"token\":\"token_invalido\",\"usuario\":{\"nome\":\"Novo Nome\"}}";
        String expectedResponse = "{\"operacao\":\"usuario_atualizar\",\"status\":false,\"info\":\"Token inválido.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleAtualizarUsuario_UserNotFound() throws Exception {
        mockSessions.put("token_valido", "111.111.111-11");
        when(mockUsuarioDao.ler(eq("111.111.111-11"))).thenReturn(null);

        String jsonRequest = "{\"operacao\":\"usuario_atualizar\",\"token\":\"token_valido\",\"usuario\":{\"nome\":\"Novo Nome\"}}";
        String expectedResponse = "{\"operacao\":\"usuario_atualizar\",\"status\":false,\"info\":\"Usuário não encontrado.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleAtualizarUsuario_DatabaseError() throws Exception {
        mockSessions.put("token_valido", "111.111.111-11");
        Usuario user = new Usuario("111.111.111-11", "Nome Antigo", "senha", 100.0);
        when(mockUsuarioDao.ler(eq("111.111.111-11"))).thenReturn(user);
        // Simula erro ao tentar ATUALIZAR no banco
        doThrow(new SQLException("Erro ao atualizar")).when(mockUsuarioDao).atualizar(any(Usuario.class));

        String jsonRequest = "{\"operacao\":\"usuario_atualizar\",\"token\":\"token_valido\",\"usuario\":{\"nome\":\"Novo Nome\"}}";
        // A exceção será capturada pelo catch genérico no processRequest
        String expectedResponse = "{\"operacao\":\"erro_processamento\",\"status\":false,\"info\":\"Erro ao processar: Erro ao atualizar\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    // -- DELETAR USUÁRIO --
    @Test
    void testHandleDeletarUsuario_InvalidToken() throws Exception {
        String jsonRequest = "{\"operacao\":\"usuario_deletar\",\"token\":\"token_invalido\"}";
        String expectedResponse = "{\"operacao\":\"usuario_deletar\",\"status\":false,\"info\":\"Token inválido.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleDeletarUsuario_DatabaseError() throws Exception {
        mockSessions.put("token_valido", "111.111.111-11");
        // Simula erro ao tentar DELETAR no banco
        doThrow(new SQLException("Erro ao deletar")).when(mockUsuarioDao).deletar(eq("111.111.111-11"));

        String jsonRequest = "{\"operacao\":\"usuario_deletar\",\"token\":\"token_valido\"}";
        String expectedResponse = "{\"operacao\":\"erro_processamento\",\"status\":false,\"info\":\"Erro ao processar: Erro ao deletar\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
        // Verifica se o token ainda existe na sessão simulada (não deve ser removido se o BD falhou)
        assertTrue(mockSessions.containsKey("token_valido"));
    }

    // -- CRIAR TRANSAÇÃO --
    @Test
    void testHandleCriarTransacao_InvalidToken() throws Exception {
        String jsonRequest = "{\"operacao\":\"transacao_criar\",\"token\":\"token_invalido\",\"cpf_destino\":\"222.222.222-22\",\"valor\":10.0}";
        String expectedResponse = "{\"operacao\":\"transacao_criar\",\"status\":false,\"info\":\"Token de sessão inválido.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleCriarTransacao_InsufficientFunds() throws Exception {
        mockSessions.put("token_valido", "111.111.111-11");
        Usuario sender = new Usuario("111.111.111-11", "Remetente", "senha", 5.0); // Saldo baixo
        Usuario receiver = new Usuario("222.222.222-22", "Destinatario", "senha", 100.0);
        when(mockUsuarioDao.lerComConexao(any(), eq("111.111.111-11"))).thenReturn(sender);
        when(mockUsuarioDao.lerComConexao(any(), eq("222.222.222-22"))).thenReturn(receiver);

        String jsonRequest = "{\"operacao\":\"transacao_criar\",\"token\":\"token_valido\",\"cpf_destino\":\"222.222.222-22\",\"valor\":10.0}";
        String expectedResponse = "{\"operacao\":\"transacao_criar\",\"status\":false,\"info\":\"Saldo insuficiente ou um dos usuários não foi encontrado.\"}"; // Mensagem genérica da implementação atual
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleCriarTransacao_SenderNotFound() throws Exception {
        mockSessions.put("token_valido", "111.111.111-11");
        when(mockUsuarioDao.lerComConexao(any(), eq("111.111.111-11"))).thenReturn(null); // Remetente não encontrado
        Usuario receiver = new Usuario("222.222.222-22", "Destinatario", "senha", 100.0);
        when(mockUsuarioDao.lerComConexao(any(), eq("222.222.222-22"))).thenReturn(receiver);

        String jsonRequest = "{\"operacao\":\"transacao_criar\",\"token\":\"token_valido\",\"cpf_destino\":\"222.222.222-22\",\"valor\":10.0}";
        String expectedResponse = "{\"operacao\":\"transacao_criar\",\"status\":false,\"info\":\"Saldo insuficiente ou um dos usuários não foi encontrado.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleCriarTransacao_ReceiverNotFound() throws Exception {
        mockSessions.put("token_valido", "111.111.111-11");
        Usuario sender = new Usuario("111.111.111-11", "Remetente", "senha", 100.0);
        when(mockUsuarioDao.lerComConexao(any(), eq("111.111.111-11"))).thenReturn(sender);
        when(mockUsuarioDao.lerComConexao(any(), eq("222.222.222-22"))).thenReturn(null); // Destinatário não encontrado

        String jsonRequest = "{\"operacao\":\"transacao_criar\",\"token\":\"token_valido\",\"cpf_destino\":\"222.222.222-22\",\"valor\":10.0}";
        String expectedResponse = "{\"operacao\":\"transacao_criar\",\"status\":false,\"info\":\"Saldo insuficiente ou um dos usuários não foi encontrado.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

    @Test
    void testHandleCriarTransacao_DatabaseErrorDuringTransaction() throws Exception {
        mockSessions.put("token_valido", "111.111.111-11");
        Usuario sender = new Usuario("111.111.111-11", "Remetente", "senha", 100.0);
        Usuario receiver = new Usuario("222.222.222-22", "Destinatario", "senha", 100.0);
        when(mockUsuarioDao.lerComConexao(any(), eq("111.111.111-11"))).thenReturn(sender);
        when(mockUsuarioDao.lerComConexao(any(), eq("222.222.222-22"))).thenReturn(receiver);
        // Simula erro ao tentar salvar a transação no banco (após atualizar usuários)
        doThrow(new SQLException("Erro ao salvar transação")).when(mockTransacaoDao).criarComConexao(any(), any());

        String jsonRequest = "{\"operacao\":\"transacao_criar\",\"token\":\"token_valido\",\"cpf_destino\":\"222.222.222-22\",\"valor\":10.0}";
        String expectedResponse = "{\"operacao\":\"transacao_criar\",\"status\":false,\"info\":\"Falha na transação: Erro ao salvar transação\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }


    // -- LER TRANSAÇÕES --
    @Test
    void testHandleLerTransacoes_InvalidToken() throws Exception {
        String jsonRequest = "{\"operacao\":\"transacao_ler\",\"token\":\"token_invalido\",\"data_inicial\":\"2023-01-01T00:00:00Z\",\"data_final\":\"2023-01-31T23:59:59Z\"}";
        String expectedResponse = "{\"operacao\":\"transacao_ler\",\"status\":false,\"info\":\"Token de sessão inválido.\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }

     @Test
    void testHandleLerTransacoes_DatabaseError() throws Exception {
        mockSessions.put("token_valido", "111.111.111-11");
        // Simula erro ao tentar ler do banco
        doThrow(new SQLException("Erro ao ler transações")).when(mockTransacaoDao).lerPorCpfComDatas(anyString(), anyString(), anyString());

        String jsonRequest = "{\"operacao\":\"transacao_ler\",\"token\":\"token_valido\",\"data_inicial\":\"2023-01-01T00:00:00Z\",\"data_final\":\"2023-01-31T23:59:59Z\"}";
        String expectedResponse = "{\"operacao\":\"erro_processamento\",\"status\":false,\"info\":\"Erro ao processar: Erro ao ler transações\"}";
        String actualResponse = clientHandler.processRequest(jsonRequest);
        assertEquals(objectMapper.readTree(expectedResponse), objectMapper.readTree(actualResponse));
    }


    // --- Métodos Auxiliares para Reflection ---
    private void setMockField(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    private void setStaticMockField(Class<?> clazz, String fieldName, Object mock) throws Exception {
         Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        
        // Remove o modificador 'final' temporariamente se necessário
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, mock); // null para campos estáticos
    }
}