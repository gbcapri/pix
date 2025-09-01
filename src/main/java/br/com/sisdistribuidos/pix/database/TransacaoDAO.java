package br.com.sisdistribuidos.pix.database;

import br.com.sisdistribuidos.pix.model.Transacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Esta classe é o Data Access Object (DAO) para a entidade Transacao.
// Ela contém os métodos para realizar operações de criação e leitura no banco de dados.
public class TransacaoDAO {

    // Cria uma nova transação no banco de dados.
    public void criar(Transacao transacao) throws SQLException {
        String sql = "INSERT INTO transacao (id, valor, cpf_enviador, cpf_recebedor) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transacao.getId());
            stmt.setDouble(2, transacao.getValor());
            stmt.setString(3, transacao.getCpfEnviador());
            stmt.setString(4, transacao.getCpfRecebedor());
            stmt.executeUpdate();
            System.out.println("Transação criada com sucesso: " + transacao.getId());
        }
    }

    // Lê as transações de um usuário específico, com paginação.
    // Retorna uma lista de transações com base no CPF, página e limite.
    public List<Transacao> lerPorCpf(String cpf, int pagina, int limite) throws SQLException {
        List<Transacao> transacoes = new ArrayList<>();
        int offset = (pagina - 1) * limite;
        
        // A consulta SQL seleciona transações onde o CPF é o remetente ou o destinatário,
        // ordenando pela coluna de criação (se existisse) e aplicando offset/limite para paginação.
        String sql = "SELECT * FROM transacao WHERE cpf_enviador = ? OR cpf_recebedor = ? LIMIT ? OFFSET ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf);
            stmt.setString(2, cpf);
            stmt.setInt(3, limite);
            stmt.setInt(4, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transacao transacao = new Transacao();
                    transacao.setId(rs.getString("id"));
                    transacao.setValor(rs.getDouble("valor"));
                    transacao.setCpfEnviador(rs.getString("cpf_enviador"));
                    transacao.setCpfRecebedor(rs.getString("cpf_recebedor"));
                    transacoes.add(transacao);
                }
            }
        }
        return transacoes;
    }
}
