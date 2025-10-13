package br.com.sisdistribuidos.pix.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:pix_database.db";
    
    private static volatile boolean tablesInitialized = false;

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(DB_URL); 
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver do SQLite não encontrado.", e); 
        }
    }

    public static synchronized void initialize() throws SQLException {
        if (!tablesInitialized) {
            try (Connection conn = getConnection()) {
                createTables(conn);
                tablesInitialized = true;
                System.out.println("Banco de dados SQLite inicializado com sucesso.");
            } catch (SQLException e) {
                System.err.println("Falha na inicialização do banco de dados: " + e.getMessage());
                throw e;
            }
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            String createUsuarioTable = "CREATE TABLE IF NOT EXISTS usuario (" +
                                        "cpf VARCHAR(255) PRIMARY KEY," +
                                        "nome VARCHAR(255) NOT NULL," +
                                        "senha VARCHAR(255) NOT NULL," +
                                        "saldo REAL NOT NULL" + 
                                        ")";
            statement.execute(createUsuarioTable);
            System.out.println("Tabela 'usuario' criada ou já existente.");

            String createTransacaoTable =   "CREATE TABLE IF NOT EXISTS transacao (" +
                                            "id VARCHAR(255) PRIMARY KEY," +
                                            "valor REAL NOT NULL," +
                                            "cpf_enviador VARCHAR(255) NOT NULL," +
                                            "cpf_recebedor VARCHAR(255) NOT NULL," +
                                            "criado_em VARCHAR(255) NOT NULL," +
                                            "atualizado_em VARCHAR(255) NOT NULL," +
                                            "FOREIGN KEY (cpf_enviador) REFERENCES usuario(cpf) ON DELETE CASCADE," +
                                            "FOREIGN KEY (cpf_recebedor) REFERENCES usuario(cpf) ON DELETE CASCADE" +
                                            ")";
            statement.execute(createTransacaoTable);
            System.out.println("Tabela 'transacao' criada ou já existente.");
        }
    }
}