package br.com.sisdistribuidos.pix;

import javax.swing.*;
import java.awt.*;

/**
 * Painel dedicado para a tela de Criação de Usuário (Nome, CPF, Senha).
 */
public class CriarUsuarioPanel extends JPanel {

    private JTextField tfNome, tfCpf;
    private JPasswordField pfSenha;
    private JButton btnConfirmarCriacao, btnVoltar;

    public CriarUsuarioPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Criar Novo Usuário"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha 1: Nome
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Nome Completo:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        tfNome = new JTextField(15);
        add(tfNome, gbc);

        // Linha 2: CPF
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("CPF (000.000.000-00):"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        tfCpf = new JTextField(15);
        add(tfCpf, gbc);

        // Linha 3: Senha
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        add(new JLabel("Senha:"), gbc);
        
        gbc.gridx = 1; gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        pfSenha = new JPasswordField(15);
        add(pfSenha, gbc);

        // Linha 4: Botões
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        btnVoltar = new JButton("Voltar");
        add(btnVoltar, gbc);
        
        gbc.gridx = 1; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        btnConfirmarCriacao = new JButton("Confirmar Criação");
        add(btnConfirmarCriacao, gbc);
    }

    // Getters
    public String getNome() { return tfNome.getText(); }
    public String getCpf() { return tfCpf.getText(); }
    public String getSenha() { return new String(pfSenha.getPassword()); }
    public JButton getBtnConfirmarCriacao() { return btnConfirmarCriacao; }
    public JButton getBtnVoltar() { return btnVoltar; }
    
    public void limparCampos() {
        tfNome.setText("");
        tfCpf.setText("");
        pfSenha.setText("");
    }
}