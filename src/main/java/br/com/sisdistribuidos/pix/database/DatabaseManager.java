package br.com.sisdistribuidos.pix.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/pix_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "1234";
    
    private static volatile boolean tablesInitialized = false;

    // NOVO MÉTODO: OBTÉM A CONEXÃO (SEMPRE UMA NOVA)
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            Connection newConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            return newConnection; 
        } catch (ClassNotFoundException e) {
            System.err.println("Driver do PostgreSQL não encontrado.");
            throw new SQLException("Driver do PostgreSQL não encontrado.", e); 
        }
    }

    // MÉTODO DE INICIALIZAÇÃO ÚNICA (Chamado apenas no main)
    public static synchronized void initialize() throws SQLException {
        if (!tablesInitialized) {
            try (Connection conn = getConnection()) {
                createTables(conn);
                tablesInitialized = true;
                System.out.println("Banco de dados PostgreSQL inicializado com sucesso.");
            } catch (SQLException e) {
                System.err.println("Falha na inicialização do banco de dados: " + e.getMessage());
                throw e;
            }
        }
    }

    // O método createTables agora recebe a conexão
    private static void createTables(Connection conn) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            String createUsuarioTable = "CREATE TABLE IF NOT EXISTS usuario (" +
					                    "cpf VARCHAR(255) PRIMARY KEY," +
					                    "nome VARCHAR(255) NOT NULL," +
					                    "senha VARCHAR(255) NOT NULL," +
					                    "saldo DOUBLE PRECISION NOT NULL" +
					                    ")";
            statement.execute(createUsuarioTable);
            System.out.println("Tabela 'usuario' criada ou já existente.");

            String createTransacaoTable =   "CREATE TABLE IF NOT EXISTS transacao (" +
						                    "id VARCHAR(255) PRIMARY KEY," +
						                    "valor DOUBLE PRECISION NOT NULL," +
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
    
    public static void closeConnection() {
        // Obsoleto
    }
}