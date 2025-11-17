package br.com.sisdistribuidos.pix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;

/**
 * Painel para a funcionalidade de Depósito (Operação "depositar").
 */
public class DepositoPanel extends JPanel {

    private PixClientManager clientManager;
    private ObjectMapper objectMapper;
    private MainScreen mainScreen; // Para atualizar o saldo no header

    private JTextField tfValor;
    private JButton btnDepositar;

    public DepositoPanel(PixClientManager clientManager, MainScreen mainScreen) {
        this.clientManager = clientManager;
        this.objectMapper = clientManager.getObjectMapper();
        this.mainScreen = mainScreen;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Linha 1: Rótulo do Valor ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Valor do Depósito (ex: 50.00):"), gbc);

        // --- Linha 1: Campo do Valor ---
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        tfValor = new JTextField(15);
        add(tfValor, gbc);

        // --- Linha 2: Botão de Depositar ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2; // Ocupa duas colunas
        gbc.anchor = GridBagConstraints.CENTER;
        btnDepositar = new JButton("Confirmar Depósito");
        add(btnDepositar, gbc);
        
        // Espaçador para empurrar tudo para cima
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        add(new JPanel(), gbc);

        // --- Ação ---
        btnDepositar.addActionListener(e -> realizarDeposito());
    }

    private void realizarDeposito() {
        double valor;
        try {
            valor = Double.parseDouble(tfValor.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Valor inválido. Use ponto para decimais (ex: 100.50).",
                    "Erro de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prepara o JSON da requisição (operação "depositar")
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "depositar");
        request.put("valor_enviado", valor);
        // O token é adicionado automaticamente pelo clientManager

        try {
            String responseStr = clientManager.sendRequest(request);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            String info = responseNode.get("info").asText();
            if (responseNode.get("status").asBoolean()) {
                JOptionPane.showMessageDialog(this,
                        info, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                tfValor.setText(""); // Limpa o campo
                // Atualiza o saldo na MainScreen
                mainScreen.carregarDadosUsuario();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Erro ao depositar: " + info, "Falha", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro de comunicação: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}