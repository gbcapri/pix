package br.com.sisdistribuidos.pix.database;

import br.com.sisdistribuidos.pix.model.Transacao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransacaoDAO {

    public void criarComConexao(Connection conn, Transacao transacao) throws SQLException {
        String sql = "INSERT INTO transacao (id, valor, cpf_enviador, cpf_recebedor, criado_em, atualizado_em) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transacao.getId());
            stmt.setDouble(2, transacao.getValor());
            stmt.setString(3, transacao.getCpfEnviador());
            stmt.setString(4, transacao.getCpfRecebedor());
            stmt.setString(5, transacao.getCriadoEm());
            stmt.setString(6, transacao.getAtualizadoEm());
            stmt.executeUpdate();
        }
    }

    public List<Transacao> lerPorCpfComDatas(String cpf, String dataInicial, String dataFinal) throws SQLException {
        List<Transacao> transacoes = new ArrayList<>();
        
        String sql = "SELECT * FROM transacao WHERE (cpf_enviador = ? OR cpf_recebedor = ?) AND criado_em BETWEEN ? AND ? ORDER BY criado_em DESC";// Usa BETWEEN para compatibilidade com mais bancos de dados

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            stmt.setString(2, cpf);
            stmt.setString(3, dataInicial);
            stmt.setString(4, dataFinal);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Transacao transacao = new Transacao();
                    transacao.setId(rs.getString("id"));
                    transacao.setValor(rs.getDouble("valor"));
                    transacao.setCpfEnviador(rs.getString("cpf_enviador"));
                    transacao.setCpfRecebedor(rs.getString("cpf_recebedor"));
                    transacao.setCriadoEm(rs.getString("criado_em"));
                    transacao.setAtualizadoEm(rs.getString("atualizado_em"));
                    transacoes.add(transacao);
                }
            }
        }
        return transacoes;
    }
}