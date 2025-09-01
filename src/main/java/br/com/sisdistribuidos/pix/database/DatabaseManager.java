package br.com.sisdistribuidos.pix.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String JDBC_URL = "jdbc:h2:mem:pix_db";
    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.h2.Driver");
                connection = DriverManager.getConnection(JDBC_URL, "sa", "");
                System.out.println("Conexão com o banco de dados H2 estabelecida.");
                createTables();
            } catch (ClassNotFoundException e) {
                System.err.println("Driver do H2 não encontrado.");
                e.printStackTrace();
            }
        }
        return connection;
    }

    // Método para criar as tabelas 'usuario' e 'transacao'.
    private static void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Cria a tabela 'usuario'
            String createUsuarioTable = "CREATE TABLE IF NOT EXISTS usuario (" +
                                        "cpf VARCHAR(255) PRIMARY KEY," +
                                        "nome VARCHAR(255) NOT NULL," +
                                        "senha VARCHAR(255) NOT NULL," +
                                        "saldo DOUBLE NOT NULL" +
                                        ")";
            statement.execute(createUsuarioTable);
            System.out.println("Tabela 'usuario' criada ou já existente.");

            // Cria a tabela 'transacao' com os novos campos de data
            String createTransacaoTable = "CREATE TABLE IF NOT EXISTS transacao (" +
                                          "id VARCHAR(255) PRIMARY KEY," +
                                          "valor DOUBLE NOT NULL," +
                                          "cpf_enviador VARCHAR(255) NOT NULL," +
                                          "cpf_recebedor VARCHAR(255) NOT NULL," +
                                          "criado_em VARCHAR(255) NOT NULL," + // Adicionado
                                          "atualizado_em VARCHAR(255) NOT NULL," + // Adicionado
                                          "FOREIGN KEY (cpf_enviador) REFERENCES usuario(cpf)," +
                                          "FOREIGN KEY (cpf_recebedor) REFERENCES usuario(cpf)" +
                                          ")";
            statement.execute(createTransacaoTable);
            System.out.println("Tabela 'transacao' criada ou já existente.");
        }
    }
    
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Conexão com o banco de dados H2 fechada.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}