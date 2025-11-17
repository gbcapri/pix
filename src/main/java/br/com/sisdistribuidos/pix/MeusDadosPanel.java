package br.com.sisdistribuidos.pix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;

/**
 * Painel de "Meus Dados" (Somente Leitura).
 * Mostra os dados básicos do usuário (Nome, CPF, Saldo).
 */
public class MeusDadosPanel extends JPanel {

    private PixClientManager clientManager;
    private ObjectMapper objectMapper;

    private JLabel lblNome, lblCpf, lblSaldo;
    private JButton btnAtualizar;

    public MeusDadosPanel(PixClientManager clientManager) {
        this.clientManager = clientManager;
        this.objectMapper = clientManager.getObjectMapper();

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Meus Dados"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Labels (Rótulos) ---
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.1; // Pouco espaço
        add(new JLabel("Nome:"), gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("CPF:"), gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Saldo Atual:"), gbc);

        // --- Valores (Dados) ---
        Font fontValores = new Font("Arial", Font.BOLD, 14);
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 0.9; // Mais espaço
        lblNome = new JLabel("Carregando...");
        lblNome.setFont(fontValores);
        add(lblNome, gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        lblCpf = new JLabel("Carregando...");
        lblCpf.setFont(fontValores);
        add(lblCpf, gbc);
        
        gbc.gridx = 1; gbc.gridy = 2;
        lblSaldo = new JLabel("Carregando...");
        lblSaldo.setFont(fontValores);
        add(lblSaldo, gbc);

        // --- Botão de Atualizar ---
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE; // Não esticar o botão
        gbc.weightx = 0;
        btnAtualizar = new JButton("Recarregar Dados");
        add(btnAtualizar, gbc);
        
        btnAtualizar.addActionListener(e -> carregarDadosUsuario());

        // --- Ação ao entrar na aba ---
        // Adiciona um listener para carregar os dados sempre que a aba ficar visível
        addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent e) {
                // Este evento é disparado quando o painel é mostrado
                carregarDadosUsuario();
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {}
            public void ancestorMoved(javax.swing.event.AncestorEvent e) {}
        });
    }
    
    /**
     * Busca os dados do usuário (operação usuario_ler) e atualiza os JLabels.
     */
    public void carregarDadosUsuario() {
        // Evita chamadas se o clientManager ainda não estiver pronto
        if (clientManager == null) return; 
        
        try {
            ObjectNode request = objectMapper.createObjectNode();
            request.put("operacao", "usuario_ler");

            String responseStr = clientManager.sendRequest(request);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            if (responseNode.get("status").asBoolean()) {
                JsonNode usuarioNode = responseNode.get("usuario");
                lblNome.setText(usuarioNode.get("nome").asText());
                lblCpf.setText(usuarioNode.get("cpf").asText());
                lblSaldo.setText(String.format("R$ %.2f", usuarioNode.get("saldo").asDouble()));
            } else {
                String info = responseNode.get("info").asText();
                JOptionPane.showMessageDialog(this, "Erro ao carregar dados: " + info, "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            // Em caso de erro de rede, limpa os campos
            lblNome.setText("---");
            lblCpf.setText("---");
            lblSaldo.setText("---");
            JOptionPane.showMessageDialog(this, "Erro de comunicação: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}