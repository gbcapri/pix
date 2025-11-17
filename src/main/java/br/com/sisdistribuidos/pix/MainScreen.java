package br.com.sisdistribuidos.pix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;

/**
 * Tela principal da aplicação, exibida após o login.
 * Mostra saldo, nome e permite realizar operações.
 */
public class MainScreen extends JFrame {

    private PixClientManager clientManager;
    private ObjectMapper objectMapper;
    private ClienteGUI loginScreen; // Referência para reexibir ao fazer logout

    // Componentes da GUI
    private JLabel lblNomeUsuario, lblSaldo;
    private JTabbedPane tabbedPane;
    private JButton btnLogout;

    public MainScreen(PixClientManager clientManager, ClienteGUI loginScreen) {
        this.clientManager = clientManager;
        this.objectMapper = clientManager.getObjectMapper();
        this.loginScreen = loginScreen;

        setTitle("PIX - Painel Principal");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- Painel Superior (Header) ---
        JPanel panelHeader = new JPanel(new BorderLayout(10, 10));
        panelHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblNomeUsuario = new JLabel("Carregando...");
        lblNomeUsuario.setFont(new Font("Arial", Font.BOLD, 16));
        panelHeader.add(lblNomeUsuario, BorderLayout.WEST);

        lblSaldo = new JLabel("Saldo: R$ ...");
        lblSaldo.setFont(new Font("Arial", Font.BOLD, 16));
        panelHeader.add(lblSaldo, BorderLayout.CENTER);

        btnLogout = new JButton("Logout");
        panelHeader.add(btnLogout, BorderLayout.EAST);

        // --- Painel de Abas (MODIFICADO) ---
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Início", createPanelInicio());
        tabbedPane.addTab("Depositar", new DepositoPanel(clientManager, this));
        tabbedPane.addTab("Fazer PIX", new PixPanel(clientManager, this));
        tabbedPane.addTab("Extrato", new ExtratoPanel(clientManager));
        tabbedPane.addTab("Meus Dados", new MeusDadosPanel(clientManager)); 
        tabbedPane.addTab("Atualizar Dados", new AtualizarDadosPanel(clientManager, this));
        
        // --- Montagem ---
        add(panelHeader, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        configurarAcoes();

        // Carrega os dados do usuário ao abrir a tela
        carregarDadosUsuario();
    }

    // Painel "placeholder" para a aba Início
    private JPanel createPanelInicio() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Bem-vindo ao seu painel.", SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }

    private void configurarAcoes() {
        // --- Ação de Logout ---
        btnLogout.addActionListener(e -> {
            fazerLogout();
        });
    }

    /**
     * Chama a operação "usuario_ler" para buscar nome e saldo.
     */
    public void carregarDadosUsuario() {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("operacao", "usuario_ler");
            // O token já é adicionado automaticamente pelo clientManager

            String responseStr = clientManager.sendRequest(request);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            if (responseNode.get("status").asBoolean()) {
                JsonNode usuarioNode = responseNode.get("usuario");
                String nome = usuarioNode.get("nome").asText();
                double saldo = usuarioNode.get("saldo").asDouble();

                // Atualiza a GUI com os dados
                lblNomeUsuario.setText("Olá, " + nome);
                lblSaldo.setText(String.format("Saldo: R$ %.2f", saldo));
                
                // Armazena o nome no manager (útil para o extrato)
                clientManager.setLoggedInUserName(nome);
            } else {
                // Se falhar (ex: token expirou), força o logout
                JOptionPane.showMessageDialog(this,
                        "Erro ao carregar dados: " + responseNode.get("info").asText(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
                fazerLogout();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro de comunicação: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            fazerLogout();
        }
    }

    public void fazerLogout() {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("operacao", "usuario_logout");
            clientManager.sendRequest(request);
        } catch (Exception ex) {
            // Ignora erros de rede no logout, apenas desloga localmente
        } finally {
            // Fecha esta tela
            this.dispose();
            
            // Reexibe a tela de login
            loginScreen.setVisible(true);
            
            // CHAMA O NOVO MÉTODO DE RESET
            loginScreen.resetParaOpcoes();
        }
    }
}