package br.com.sisdistribuidos.pix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;

/**
 * Painel para atualizar dados (nome/senha) e deletar a conta.
 * Operações: "usuario_atualizar", "usuario_deletar".
 * (Este era o antigo MeusDadosPanel)
 */
public class AtualizarDadosPanel extends JPanel {

    private PixClientManager clientManager;
    private ObjectMapper objectMapper;
    private MainScreen mainScreen; // Para atualizar o nome no header ou forçar logout

    private JTextField tfNovoNome;
    private JPasswordField pfNovaSenha;
    private JButton btnAtualizar, btnDeletar;

    public AtualizarDadosPanel(PixClientManager clientManager, MainScreen mainScreen) {
        this.clientManager = clientManager;
        this.objectMapper = clientManager.getObjectMapper();
        this.mainScreen = mainScreen;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Painel de Atualização (Centro) ---
        JPanel panelAtualizar = new JPanel(new GridBagLayout());
        panelAtualizar.setBorder(BorderFactory.createTitledBorder("Atualizar Dados Pessoais"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 1: Novo Nome
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        panelAtualizar.add(new JLabel("Novo Nome:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        tfNovoNome = new JTextField(20);
        tfNovoNome.setToolTipText("Deixe em branco para não alterar");
        panelAtualizar.add(tfNovoNome, gbc);

        // Linha 2: Nova Senha
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        panelAtualizar.add(new JLabel("Nova Senha:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        pfNovaSenha = new JPasswordField(20);
        pfNovaSenha.setToolTipText("Deixe em branco para não alterar");
        panelAtualizar.add(pfNovaSenha, gbc);
        
        // Linha 3: Botão Atualizar
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnAtualizar = new JButton("Salvar Alterações");
        panelAtualizar.add(btnAtualizar, gbc);

        // --- Painel de Deleção (Abaixo) ---
        JPanel panelDeletar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelDeletar.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.RED), "Zona de Perigo"));
        btnDeletar = new JButton("Deletar Minha Conta Permanentemente");
        btnDeletar.setForeground(Color.WHITE);
        btnDeletar.setBackground(Color.RED);
        panelDeletar.add(btnDeletar);

        // --- Montagem ---
        add(panelAtualizar, BorderLayout.NORTH);
        add(panelDeletar, BorderLayout.SOUTH);

        // --- Ações ---
        btnAtualizar.addActionListener(e -> atualizarDados());
        btnDeletar.addActionListener(e -> deletarConta());
    }

    private void atualizarDados() {
        String nome = tfNovoNome.getText();
        String senha = new String(pfNovaSenha.getPassword());

        if (nome.trim().isEmpty() && senha.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Preencha pelo menos um campo (Nome ou Senha) para atualizar.",
                    "Nada a fazer", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Prepara o JSON (operação "usuario_atualizar")
        ObjectNode request = objectMapper.createObjectNode();
        ObjectNode usuarioSubNode = objectMapper.createObjectNode();
        
        if (!nome.trim().isEmpty()) {
            usuarioSubNode.put("nome", nome);
        }
        if (!senha.trim().isEmpty()) {
            usuarioSubNode.put("senha", senha);
        }

        request.put("operacao", "usuario_atualizar");
        request.set("usuario", usuarioSubNode);
        // Token é adicionado pelo clientManager

        try {
            String responseStr = clientManager.sendRequest(request);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            String info = responseNode.get("info").asText();
            if (responseNode.get("status").asBoolean()) {
                JOptionPane.showMessageDialog(this,
                        info, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                tfNovoNome.setText("");
                pfNovaSenha.setText("");
                
                // Se o nome foi atualizado, recarrega o header da MainScreen
                if (!nome.trim().isEmpty() && mainScreen != null) {
                    mainScreen.carregarDadosUsuario();
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Erro ao atualizar: " + info, "Falha", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro de comunicação: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deletarConta() {
        // Confirmação MUITO importante
        int confirm = JOptionPane.showConfirmDialog(this,
                "Você tem CERTEZA que deseja deletar sua conta?\nEsta ação é irreversível e fará seu logout.",
                "Confirmar Deleção de Conta",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Prepara o JSON (operação "usuario_deletar")
        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "usuario_deletar");
        // Token é adicionado pelo clientManager

        try {
            String responseStr = clientManager.sendRequest(request);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            String info = responseNode.get("info").asText();
            if (responseNode.get("status").asBoolean()) {
                JOptionPane.showMessageDialog(this,
                        info, "Conta Deletada", JOptionPane.INFORMATION_MESSAGE);
                // Força o logout, pois a conta não existe mais
                if(mainScreen != null) {
                    mainScreen.fazerLogout();
                }
            } else {
                 JOptionPane.showMessageDialog(this,
                        "Erro ao deletar: " + info, "Falha", JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro de comunicação: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            // Mesmo em erro de comunicação, força o logout por segurança
            if(mainScreen != null) {
                mainScreen.fazerLogout();
            }
        }
    }
}