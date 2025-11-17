package br.com.sisdistribuidos.pix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class ExtratoPanel extends JPanel {

    private PixClientManager clientManager;
    private ObjectMapper objectMapper;

    private JTextField tfDataInicial, tfDataFinal;
    private JButton btnBuscar;
    private JTextArea textAreaExtrato;
    private JScrollPane scrollPane;

    public ExtratoPanel(PixClientManager clientManager) {
        this.clientManager = clientManager;
        this.objectMapper = clientManager.getObjectMapper();

        setLayout(new BorderLayout(10, 10));

        JPanel panelFiltro = new JPanel(new GridBagLayout());
        panelFiltro.setBorder(BorderFactory.createTitledBorder("Período da Busca"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        panelFiltro.add(new JLabel("Data Inicial (yyyy-MM-dd):"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;
        tfDataInicial = new JTextField(hoje, 10);
        panelFiltro.add(tfDataInicial, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        panelFiltro.add(new JLabel("Data Final (yyyy-MM-dd):"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        tfDataFinal = new JTextField(hoje, 10);
        panelFiltro.add(tfDataFinal, gbc);

        gbc.gridx = 2; gbc.gridy = 0; 
        gbc.gridheight = 2;
        gbc.fill = GridBagConstraints.VERTICAL;
        btnBuscar = new JButton("Buscar Extrato");
        panelFiltro.add(btnBuscar, gbc);
        
        gbc.gridx = 3; gbc.weightx = 1.0; 
        panelFiltro.add(new JPanel(), gbc);

        textAreaExtrato = new JTextArea("Selecione um período e clique em 'Buscar Extrato'.\nO período máximo é de 31 dias.");
        textAreaExtrato.setEditable(false);
        textAreaExtrato.setFont(new Font("Monospaced", Font.PLAIN, 12));
        scrollPane = new JScrollPane(textAreaExtrato);

        add(panelFiltro, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        btnBuscar.addActionListener(e -> buscarExtrato());
    }

    private void buscarExtrato() {
        String dataInicial = tfDataInicial.getText() + "T00:00:00Z";
        String dataFinal = tfDataFinal.getText() + "T23:59:59Z";

        ObjectNode request = objectMapper.createObjectNode();
        request.put("operacao", "transacao_ler");
        request.put("data_inicial", dataInicial);
        request.put("data_final", dataFinal);

        try {
            String responseStr = clientManager.sendRequest(request);
            JsonNode responseNode = objectMapper.readTree(responseStr);

            if (responseNode.get("status").asBoolean()) {
                formatarExtrato(responseNode.get("transacoes"), dataInicial, dataFinal);
            } else {
                String info = responseNode.get("info").asText();
                JOptionPane.showMessageDialog(this,
                        "Erro ao buscar extrato: " + info, "Falha", JOptionPane.WARNING_MESSAGE);
                textAreaExtrato.setText("Erro ao buscar extrato: " + info);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro de comunicação: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            textAreaExtrato.setText("Erro de comunicação: " + ex.getMessage());
        }
    }

    private void formatarExtrato(JsonNode transacoesArray, String dataInicial, String dataFinal) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("--- EXTRATO DE %s até %s ---\n", dataInicial.substring(0, 10), dataFinal.substring(0, 10)));
        sb.append("----------------------------------------------------------------------\n\n");

        if (transacoesArray == null || transacoesArray.isEmpty()) {
            sb.append("Nenhuma transação encontrada neste período.");
            textAreaExtrato.setText(sb.toString());
            return;
        }

        String loggedInUserName = clientManager.getLoggedInUserName();
        if (loggedInUserName == null) {
            sb.append("Erro: Não foi possível identificar o usuário logado para formatar o extrato.");
            textAreaExtrato.setText(sb.toString());
            return;
        }

        for (JsonNode t : transacoesArray) {
            String data = t.path("criado_em").asText("N/A");
            double valor = t.path("valor_enviado").asDouble(0.0);
            String enviadorNome = t.path("usuario_enviador").path("nome").asText("N/A");
            String recebedorNome = t.path("usuario_recebedor").path("nome").asText("N/A");

            String tipo;
            String valorDisplay;

            if (enviadorNome.equals(recebedorNome)) {
                tipo = "DEPÓSITO";
                valorDisplay = String.format("+ R$ %.2f", valor);
            } else if (enviadorNome.equals(loggedInUserName)) {
                tipo = "ENVIO (PIX)";
                valorDisplay = String.format("- R$ %.2f", valor);
            } else {
                tipo = "RECEBIMENTO (PIX)";
                valorDisplay = String.format("+ R$ %.2f", valor);
            }

            sb.append("Data: ").append(data).append("\n");
            sb.append("Tipo: ").append(tipo).append("\n");
            sb.append("Valor: ").append(valorDisplay).append("\n");

            if (tipo.equals("ENVIO (PIX)")) {
                sb.append("Para: ").append(recebedorNome).append("\n");
            } else if (tipo.equals("RECEBIMENTO (PIX)")) {
                sb.append("De: ").append(enviadorNome).append("\n");
            }
            sb.append("--------------------------------------------------\n");
        }

        textAreaExtrato.setText(sb.toString());
        textAreaExtrato.setCaretPosition(0); // Rola para o topo
    }
}