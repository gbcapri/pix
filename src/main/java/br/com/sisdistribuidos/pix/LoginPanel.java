package br.com.sisdistribuidos.pix;

import javax.swing.*;
import java.awt.*;

/**
 * Painel dedicado para a tela de Login (CPF e Senha).
 */
public class LoginPanel extends JPanel {

    private JTextField tfCpf;
    private JPasswordField pfSenha;
    private JButton btnConfirmarLogin, btnVoltar;

    public LoginPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Fazer Login"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 1: CPF
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("CPF (000.000.000-00):"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        tfCpf = new JTextField(15);
        add(tfCpf, gbc);

        // Linha 2: Senha
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Senha:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        pfSenha = new JPasswordField(15);
        add(pfSenha, gbc);

        // Linha 3: Botões
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnVoltar = new JButton("Voltar");
        add(btnVoltar, gbc);
        
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        btnConfirmarLogin = new JButton("Confirmar Login");
        add(btnConfirmarLogin, gbc);
    }

    // Getters
    public String getCpf() { return tfCpf.getText(); }
    public String getSenha() { return new String(pfSenha.getPassword()); }
    public JButton getBtnConfirmarLogin() { return btnConfirmarLogin; }
    public JButton getBtnVoltar() { return btnVoltar; }
    
    public void limparCampos() {
        tfCpf.setText("");
        pfSenha.setText("");
    }
}