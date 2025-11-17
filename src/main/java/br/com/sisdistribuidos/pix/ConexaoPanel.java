package br.com.sisdistribuidos.pix;

import javax.swing.*;
import java.awt.*;

/**
 * Painel dedicado para a interface de conexão com o servidor (IP, Porta).
 * [Imagem do painel de conexão]
 */
public class ConexaoPanel extends JPanel {

    private JTextField tfServerIp, tfServerPort;
    private JButton btnConectar;

    public ConexaoPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBorder(BorderFactory.createTitledBorder("Conexão"));
        
        add(new JLabel("IP:"));
        tfServerIp = new JTextField("", 10);
        add(tfServerIp);
        
        add(new JLabel("Porta:"));
        tfServerPort = new JTextField("20000", 5);
        add(tfServerPort);
        
        btnConectar = new JButton("Conectar");
        add(btnConectar);
    }

    // Getters para os campos de texto
    public String getIp() {
        return tfServerIp.getText();
    }

    public String getPort() {
        return tfServerPort.getText();
    }

    // Getter para o botão, para que a GUI principal adicione o ActionListener
    public JButton getBtnConectar() {
        return btnConectar;
    }

    /**
     * Desabilita os campos de conexão após conectar.
     */
    public void desabilitarCampos() {
        tfServerIp.setEnabled(false);
        tfServerPort.setEnabled(false);
        btnConectar.setEnabled(false);
    }
    public void habilitarCampos() {
        tfServerIp.setEnabled(true);
        tfServerPort.setEnabled(true);
        btnConectar.setEnabled(true);
    }
}