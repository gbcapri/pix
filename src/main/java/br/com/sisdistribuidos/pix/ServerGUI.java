package br.com.sisdistribuidos.pix;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Set;

/**
 * Interface Gráfica para o Servidor.
 * Mostra logs e a lista de usuários conectados.
 */
public class ServerGUI extends JFrame {

    private final JTextArea logArea;
    private final DefaultListModel<String> userListModel;
    private final JList<String> userList;

    public ServerGUI() {
        setTitle("Servidor PIX - Painel de Controle");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centraliza

        // --- Painel de Log ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(new TitledBorder("Logs do Servidor"));

        // --- Painel de Usuários ---
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setBorder(new TitledBorder("Usuários Logados (0)"));
        userScrollPane.setPreferredSize(new Dimension(250, 0));

        // --- Layout ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userScrollPane, logScrollPane);
        splitPane.setDividerLocation(250);

        getContentPane().add(splitPane, BorderLayout.CENTER);
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

    /**
     * Atualiza a lista de usuários logados na GUI.
     * Este método é thread-safe.
     */
    public void updateUserList(Set<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            if (users.isEmpty()) {
                userListModel.addElement("Nenhum usuário logado.");
            } else {
                users.forEach(userListModel::addElement);
            }
            // Atualiza o título do painel com a contagem
            ((TitledBorder) userList.getParent().getParent().getBorder()).setTitle("Usuários Logados (" + users.size() + ")");
            userList.getParent().getParent().repaint();
        });
    }

    /**
     * Torna a GUI visível. Deve ser chamado na thread principal.
     */
    public void start() {
        setVisible(true);
        log("GUI do Servidor iniciada.");
    }
}