package br.com.sisdistribuidos.pix.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
                System.out.println("Banco de dados inicializado com sucesso.");
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
            
            insertMockData(conn);

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
    
    private static void insertMockData(Connection conn) throws SQLException {
        // Verifica se a tabela já tem dados
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuario")) {
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Tabela 'usuario' já contém dados. Mocks não serão inseridos.");
                return;
            }
        }

        System.out.println("Inserindo dados mock (usuários)...");
        String sql = "INSERT INTO usuario (cpf, nome, senha, saldo) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "111.111.111-11");
            pstmt.setString(2, "Alice");
            pstmt.setString(3, "111111");
            pstmt.setDouble(4, 1000.00);
            pstmt.addBatch();

            pstmt.setString(1, "222.222.222-22");
            pstmt.setString(2, "Bruno");
            pstmt.setString(3, "111111");
            pstmt.setDouble(4, 500.50);
            pstmt.addBatch();
            
            pstmt.setString(1, "333.333.333-33");
            pstmt.setString(2, "Carla");
            pstmt.setString(3, "111111");
            pstmt.setDouble(4, 2500.75);
            pstmt.addBatch();

            pstmt.executeBatch();
            System.out.println("Dados mock inseridos com sucesso.");
            
        } catch (SQLException e) {
             // Ignora erro de "UNIQUE constraint" caso os mocks já existam (segurança)
            if (!e.getMessage().contains("UNIQUE constraint failed")) {
                throw e;
            }
        }
    }
}