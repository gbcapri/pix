package br.com.sisdistribuidos.pix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;

/**
 * Painel para a funcionalidade de Transferência PIX (Operação "transacao_criar").
 */
public class PixPanel extends JPanel {

    private PixClientManager clientManager;
    private ObjectMapper objectMapper;
    private MainScreen mainScreen; // Para atualizar o saldo

    private JTextField tfCpfDestino, tfValor;
    private JButton btnEnviarPix;

    public PixPanel(PixClientManager clientManager, MainScreen mainScreen) {
        this.clientManager = clientManager;
        this.objectMapper = clientManager.getObjectMapper();
        this.mainScreen = mainScreen;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Linha 1: CPF Destino ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("CPF de Destino (000.000.000-00):"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        tfCpfDestino = new JTextField(15);
        add(tfCpfDestino, gbc);

        // --- Linha 2: Valor ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Valor (ex: 50.00):"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        tfValor = new JTextField(15);
        add(tfValor, gbc);

        // --- Linha 3: Botão ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnEnviarPix = new JButton("Enviar PIX");
        add(btnEnviarPix, gbc);
        
        // Espaçador
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        add(new JPanel(), gbc);

        // --- Ação ---
        btnEnviarPix.addActionListener(e -> realizarPix());
    }

    private void realizarPix() {
        String cpfDestino = tfCpfDestino.getText();
        double valor;
        try {
            valor = Double.parseDouble(tfValor.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Valor inválido. Use ponto para decimais (ex: 100.50).",
                    "Erro de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Prepara o JSON da requisição (operação "transacao_criar")
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "transacao_criar");
        request.put("cpf_destino", cpfDestino);
        request.put("valor", valor);
        // O token é adicionado automaticamente pelo clientManager

        try {
            String responseStr = clientManager.sendRequest(request);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            String info = responseNode.get("info").asText();
            if (responseNode.get("status").asBoolean()) {
                JOptionPane.showMessageDialog(this,
                        info, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                tfCpfDestino.setText(""); // Limpa os campos
                tfValor.setText("");
                // Atualiza o saldo na MainScreen
                mainScreen.carregarDadosUsuario();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Erro na transação: " + info, "Falha", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro de comunicação: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}