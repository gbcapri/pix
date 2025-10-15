package br.com.sisdistribuidos.pix.database;

import br.com.sisdistribuidos.pix.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {

    public void criar(Usuario usuario) throws SQLException {
        String sql = "INSERT INTO usuario (cpf, nome, senha, saldo) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario.getCpf());
            stmt.setString(2, usuario.getNome());
            stmt.setString(3, usuario.getSenha());
            stmt.setDouble(4, usuario.getSaldo()); 
            stmt.executeUpdate();
        }
    }

    public Usuario ler(String cpf) throws SQLException {
        String sql = "SELECT cpf, nome, senha, saldo FROM usuario WHERE cpf = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                return buildUsuarioFromResultSet(rs);
            }
        }
    }
    
    public Usuario lerComConexao(Connection conn, String cpf) throws SQLException {
        String sql = "SELECT cpf, nome, senha, saldo FROM usuario WHERE cpf = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            try (ResultSet rs = stmt.executeQuery()) {
                return buildUsuarioFromResultSet(rs);
            }
        }
    }

    public void atualizar(Usuario usuario) throws SQLException {
        String sql = "UPDATE usuario SET nome = ?, senha = ?, saldo = ? WHERE cpf = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getSenha());
            stmt.setDouble(3, usuario.getSaldo());
            stmt.setString(4, usuario.getCpf());
            if (stmt.executeUpdate() == 0) {
                 throw new SQLException("Falha ao atualizar usuário: Usuário não encontrado.");
            }
            System.out.println("Usuário atualizado com sucesso: " + usuario.getCpf());
        }
    }
    
    public void atualizarComConexao(Connection conn, Usuario usuario) throws SQLException {
        String sql = "UPDATE usuario SET nome = ?, senha = ?, saldo = ? WHERE cpf = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getSenha());
            stmt.setDouble(3, usuario.getSaldo());
            stmt.setString(4, usuario.getCpf());
            if (stmt.executeUpdate() == 0) {
                 throw new SQLException("Falha ao atualizar usuário: Usuário não encontrado.");
            }
        }
    }

    public void deletar(String cpf) throws SQLException {
        String sql = "DELETE FROM usuario WHERE cpf = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            stmt.executeUpdate();
            System.out.println("Usuário deletado com sucesso: " + cpf);
        }
    }

    private Usuario buildUsuarioFromResultSet(ResultSet rs) throws SQLException {
        if (rs.next()) {
            Usuario usuario = new Usuario();
            usuario.setCpf(rs.getString("cpf"));
            usuario.setNome(rs.getString("nome"));
            usuario.setSenha(rs.getString("senha"));
            usuario.setSaldo(rs.getFloat("saldo")); 
            return usuario;
        }
        return null; 
    }
}