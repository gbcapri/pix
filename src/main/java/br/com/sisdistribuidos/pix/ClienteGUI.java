package br.com.sisdistribuidos.pix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;
import java.awt.CardLayout; 
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Point;

/**
 * Ponto de entrada da GUI. Gerencia os painéis (Cards) de:
 * 1. Conexão
 * 2. Opções de Autenticação (Menu)
 * 3. Login
 * 4. Criação de Usuário
 */
public class ClienteGUI extends JFrame {

    private PixClientManager clientManager;
    private ObjectMapper objectMapper;
    private ClientLogGUI logWindow;

    // Painéis ("Cards")
    private ConexaoPanel conexaoPanel;
    private OpcaoAuthPanel opcaoAuthPanel;
    private LoginPanel loginPanel;
    private CriarUsuarioPanel criarUsuarioPanel;
    
    private JLabel lblStatus;
    private CardLayout cardLayout;
    private JPanel mainCardPanel;
    
    // Nomes dos "Cards"
    private static final String CARD_CONEXAO = "CONEXAO";
    private static final String CARD_OPCAO = "OPCAO";
    private static final String CARD_LOGIN = "LOGIN";
    private static final String CARD_CRIAR = "CRIAR";

    public ClienteGUI() {
        this.logWindow = new ClientLogGUI();
        this.logWindow.setVisible(true);
        
        this.clientManager = new PixClientManager(this.logWindow); 
        this.objectMapper = clientManager.getObjectMapper();

        setTitle("Cliente PIX");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setLayout(new BorderLayout(10, 10));
        
        // Reposiciona a janela principal ao lado da janela de log
        Point logLocation = logWindow.getLocation();
        setLocation(logLocation.x + logWindow.getWidth() + 10, logLocation.y - 360);

        // --- Instancia os painéis ---
        conexaoPanel = new ConexaoPanel();
        opcaoAuthPanel = new OpcaoAuthPanel();
        loginPanel = new LoginPanel();
        criarUsuarioPanel = new CriarUsuarioPanel();

        // --- Painel de Status (Rodapé) ---
        lblStatus = new JLabel("Status: Desconectado.", SwingConstants.CENTER);
        lblStatus.setBorder(BorderFactory.createEtchedBorder());

        // --- Configura o CardLayout ---
        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);
        
        mainCardPanel.add(conexaoPanel, CARD_CONEXAO);
        mainCardPanel.add(opcaoAuthPanel, CARD_OPCAO);
        mainCardPanel.add(loginPanel, CARD_LOGIN);
        mainCardPanel.add(criarUsuarioPanel, CARD_CRIAR);

        // --- Montagem ---
        add(mainCardPanel, BorderLayout.CENTER); 
        add(lblStatus, BorderLayout.SOUTH);

        // --- Ações ---
        configurarAcoes();
        
        // Mostra o primeiro card (Conexão)
        cardLayout.show(mainCardPanel, CARD_CONEXAO);
    }
    
    /**
     * Adiciona todos os ActionListeners aos botões dos painéis.
     */
    private void configurarAcoes() {
        
        // --- 1. PAINEL DE CONEXÃO ---
        conexaoPanel.getBtnConectar().addActionListener(e -> {
            String ip = conexaoPanel.getIp();
            int port;
            try {
                port = Integer.parseInt(conexaoPanel.getPort());
            } catch (NumberFormatException ex) {
                lblStatus.setText("Status: Porta inválida.");
                return;
            }

            try {
                String msg = clientManager.connect(ip, port);
                lblStatus.setText("Status: " + msg);
                
                conexaoPanel.desabilitarCampos();
                cardLayout.show(mainCardPanel, CARD_OPCAO); // Mostra o menu de 3 botões
                
            } catch (Exception ex) {
                lblStatus.setText("Status: Falha na conexão.");
                JOptionPane.showMessageDialog(this, "Erro ao conectar: " + ex.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            }
        });

        // --- 2. PAINEL DE OPÇÕES (MENU) ---
        opcaoAuthPanel.getBtnFazerLogin().addActionListener(e -> {
            cardLayout.show(mainCardPanel, CARD_LOGIN); // Mostra tela de login
        });
        
        opcaoAuthPanel.getBtnCriarConta().addActionListener(e -> {
            cardLayout.show(mainCardPanel, CARD_CRIAR); // Mostra tela de criação
        });
        
        opcaoAuthPanel.getBtnDesconectar().addActionListener(e -> {
            resetParaConexao(); // Volta para tela de conexão
        });
        
        // --- 3. PAINEL DE LOGIN ---
        loginPanel.getBtnVoltar().addActionListener(e -> {
            cardLayout.show(mainCardPanel, CARD_OPCAO); // Volta para o menu
        });
        
        loginPanel.getBtnConfirmarLogin().addActionListener(e -> {
            handleLogin();
        });
        
        // --- 4. PAINEL DE CRIAR USUÁRIO ---
        criarUsuarioPanel.getBtnVoltar().addActionListener(e -> {
            cardLayout.show(mainCardPanel, CARD_OPCAO); // Volta para o menu
        });
        
        criarUsuarioPanel.getBtnConfirmarCriacao().addActionListener(e -> {
            handleCriarUsuario();
        });
    }

    /**
     * Lógica para processar o login (vinda do LoginPanel).
     */
    private void handleLogin() {
        String cpf = loginPanel.getCpf();
        String senha = loginPanel.getSenha();

        ObjectNode loginJson = objectMapper.createObjectNode();
        loginJson.put("operacao", "usuario_login");
        loginJson.put("cpf", cpf);
        loginJson.put("senha", senha);

        try {
            String responseStr = clientManager.sendRequest(loginJson);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            if (responseNode.get("status").asBoolean()) {
                loginPanel.limparCampos();
                
                MainScreen mainScreen = new MainScreen(clientManager, this);
                mainScreen.setVisible(true);
                
                this.setVisible(false);
                
            } else {
                String info = responseNode.get("info").asText();
                JOptionPane.showMessageDialog(this, "Erro no login: " + info, "Falha no Login", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
             JOptionPane.showMessageDialog(this, "Erro ao processar login: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Lógica para processar a criação de usuário (vinda do CriarUsuarioPanel).
     */
    private void handleCriarUsuario() {
        String nome = criarUsuarioPanel.getNome();
        String cpf = criarUsuarioPanel.getCpf();
        String senha = criarUsuarioPanel.getSenha();

        ObjectNode criarJson = objectMapper.createObjectNode();
        criarJson.put("operacao", "usuario_criar");
        criarJson.put("nome", nome);
        criarJson.put("cpf", cpf);
        criarJson.put("senha", senha);

        try {
            String responseStr = clientManager.sendRequest(criarJson);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            String info = responseNode.get("info").asText();
            if (responseNode.get("status").asBoolean()) {
                JOptionPane.showMessageDialog(this, info, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                criarUsuarioPanel.limparCampos();
                cardLayout.show(mainCardPanel, CARD_LOGIN); // Leva para o login
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao criar conta: " + info, "Falha", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
             JOptionPane.showMessageDialog(this, "Erro ao processar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Reseta a GUI para o estado inicial (tela de conexão).
     * Chamado pelo botão "Desconectar" ou pelo Logout da MainScreen.
     */
    public void resetParaConexao() {
        // 1. Fecha a conexão antiga e cria um novo manager
        clientManager.closeConnection();
        // Recria o manager, passando a MESMA janela de log
        this.clientManager = new PixClientManager(this.logWindow); // <-- MODIFICADO
        
        // 2. Mostra o card de conexão
        cardLayout.show(mainCardPanel, CARD_CONEXAO);
        
        // 3. Re-habilita os campos de conexão
        conexaoPanel.habilitarCampos();
        
        // 4. Limpa os campos dos outros painéis
        loginPanel.limparCampos();
        criarUsuarioPanel.limparCampos();
        
        // 5. Reseta a barra de status
        lblStatus.setText("Status: Desconectado.");
    }   
    
    public void resetParaOpcoes() {
        // 1. Limpa os campos
        loginPanel.limparCampos();
        criarUsuarioPanel.limparCampos();

        // 2. Mostra o card de opções
        cardLayout.show(mainCardPanel, CARD_OPCAO);

        // 3. Atualiza o status
        lblStatus.setText("Status: Conectado. (Logout realizado)");
    }

    /**
     * Método principal para iniciar a GUI do cliente.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClienteGUI().setVisible(true);
        });
    }
}