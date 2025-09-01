package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.database.DatabaseManager;
import br.com.sisdistribuidos.pix.database.UsuarioDAO;
import br.com.sisdistribuidos.pix.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
    public static void main(String[] args) {
        System.out.println("Simulação do protocolo PIX iniciada.");
        
        // Exemplo de como usar a camada de persistência
        try {
            // Inicializa a conexão com o banco de dados e cria as tabelas
            DatabaseManager.getConnection();
            
            UsuarioDAO usuarioDao = new UsuarioDAO();
            
            // Exemplo de criação de usuário
            Usuario novoUsuario = new Usuario("123.456.789-00", "Gabriel Pereira Neves", "senhaSegura123", 1000.00);
            usuarioDao.criar(novoUsuario);
            
            // Exemplo de leitura de usuário
            Usuario usuarioLido = usuarioDao.ler("123.456.789-00");
            System.out.println("Dados do usuário lido: " + usuarioLido);
            
            // Exemplo de atualização de usuário
            usuarioLido.setSaldo(usuarioLido.getSaldo() + 500); // Adiciona R$ 500,00
            usuarioDao.atualizar(usuarioLido);
            
            // Exemplo de leitura do usuário atualizado
            Usuario usuarioAtualizado = usuarioDao.ler("123.456.789-00");
            System.out.println("Dados do usuário após atualização: " + usuarioAtualizado);

        } catch (Exception e) {
            System.err.println("Ocorreu um erro no acesso ao banco de dados: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Garante que a conexão com o banco de dados seja fechada
            DatabaseManager.closeConnection();
        }
    }
}