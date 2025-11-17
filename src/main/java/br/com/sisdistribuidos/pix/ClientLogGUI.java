package br.com.sisdistribuidos.pix;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Interface Gráfica (em uma janela separada) para os logs do Cliente.
 * Mostra a comunicação JSON (envio e recebimento).
 */
public class ClientLogGUI extends JFrame {

    private final JTextArea logArea;

    public ClientLogGUI() {
        setTitle("Cliente PIX - Logs de Rede");
        setSize(700, 300); // Tamanho
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Tenta posicionar ao lado da janela principal (estimativa)
        setLocationRelativeTo(null); 
        Point p = getLocation();
        setLocation(p.x - 210, p.y + 360); // Ajuste para baixo e para a esquerda

        // --- Painel de Log ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(new TitledBorder("Logs Cliente-Servidor"));

        getContentPane().add(logScrollPane, BorderLayout.CENTER);
    }

    /**
     * Adiciona uma mensagem ao log da GUI.
     * Este método é thread-safe.
     */
    public void log(String message) {
        // Usa SwingUtilities.invokeLater para garantir que a GUI
        // seja atualizada na Thread de Eventos do Swing.
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            // Auto-scroll
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}