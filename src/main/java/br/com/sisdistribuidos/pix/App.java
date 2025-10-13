package br.com.sisdistribuidos.pix;

import br.com.sisdistribuidos.pix.database.DatabaseManager;
import br.com.sisdistribuidos.pix.database.UsuarioDAO;
import br.com.sisdistribuidos.pix.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class App {
    public static void main(String[] args) {
        System.out.println("Simulação da camada de persistência iniciada.");
        
        try {
            // Inicializa a base de dados e cria as tabelas se não existirem
            DatabaseManager.initialize();
            
            UsuarioDAO usuarioDao = new UsuarioDAO();
            
            // Exemplo de criação de usuário
            // Para evitar erros de "chave duplicada", pode apagar o BD (o ficheiro .db) antes de executar
            System.out.println("\nTentando criar um novo usuário...");
            Usuario novoUsuario = new Usuario("123.456.789-00", "Gabriel Pereira Neves", "senhaSegura123", 1000.00);
            usuarioDao.criar(novoUsuario);
            System.out.println("Usuário criado com sucesso!");
            
            // Exemplo de leitura de usuário
            System.out.println("\nLendo o usuário do banco de dados...");
            Usuario usuarioLido = usuarioDao.ler("123.456.789-00");
            System.out.println("Dados do usuário lido: " + usuarioLido);
            
            // Exemplo de atualização de usuário
            System.out.println("\nAtualizando o saldo do usuário...");
            usuarioLido.setSaldo(usuarioLido.getSaldo() + 500); // Adiciona R$ 500,00
            usuarioDao.atualizar(usuarioLido);
            System.out.println("Saldo atualizado!");
            
            // Exemplo de leitura do usuário atualizado
            System.out.println("\nLendo novamente para confirmar a atualização...");
            Usuario usuarioAtualizado = usuarioDao.ler("123.456.789-00");
            System.out.println("Dados do usuário após atualização: " + usuarioAtualizado);

        } catch (Exception e) {
            System.err.println("Ocorreu um erro no acesso ao banco de dados: " + e.getMessage());
            // Descomente a linha abaixo para ver o stack trace completo se necessário
            // e.printStackTrace();
        }
        // O bloco 'finally' e a chamada a closeConnection() não são mais necessários.
    }
}