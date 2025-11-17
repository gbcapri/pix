package br.com.sisdistribuidos.pix;

import javax.swing.*;
import java.awt.*;

/**
 * Painel que apresenta as opções de autenticação (Login, Criar Conta, Sair).
 * Este é o painel mostrado após a conexão ser bem-sucedida.
 */
public class OpcaoAuthPanel extends JPanel {

    private JButton btnFazerLogin, btnCriarConta, btnDesconectar;

    public OpcaoAuthPanel() {
        setBorder(BorderFactory.createTitledBorder("Autenticação"));
        // Um layout que centraliza os botões verticalmente
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // Botão 1: Fazer Login
        gbc.gridy = 0;
        btnFazerLogin = new JButton("Fazer Login");
        add(btnFazerLogin, gbc);

        // Botão 2: Criar Conta
        gbc.gridy = 1;
        btnCriarConta = new JButton("Criar Novo Usuário");
        add(btnCriarConta, gbc);

        // Botão 3: Desconectar (Sair)
        gbc.gridy = 2;
        gbc.weighty = 0.1; // Adiciona um pouco de espaço
        gbc.anchor = GridBagConstraints.PAGE_END; // Joga para baixo
        btnDesconectar = new JButton("Desconectar do Servidor");
        btnDesconectar.setForeground(Color.RED);
        add(btnDesconectar, gbc);
    }

    // Getters para os botões
    public JButton getBtnFazerLogin() { return btnFazerLogin; }
    public JButton getBtnCriarConta() { return btnCriarConta; }
    public JButton getBtnDesconectar() { return btnDesconectar; }
}