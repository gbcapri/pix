package br.com.sisdistribuidos.pix.model;

import java.util.UUID;
import java.time.LocalDateTime; // Importação para o tipo de dado de data
import java.time.format.DateTimeFormatter; // Importação para o formatador de data
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

// A anotação @JsonInclude é usada para não incluir campos nulos na serialização JSON.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transacao {
    private String id;
    @JsonProperty("valor_enviado") // Corrigido para o nome do campo no protocolo
    private double valor;
    @JsonProperty("usuario_enviador") // Corrigido para o nome do campo no protocolo
    private Usuario usuarioEnviador; // Agora a transação armazena o objeto completo para a resposta
    @JsonProperty("usuario_recebedor") // Corrigido para o nome do campo no protocolo
    private Usuario usuarioRecebedor; // Agora a transação armazena o objeto completo para a resposta
    @JsonProperty("cpf_enviador") // Campo interno para o banco de dados
    private String cpfEnviador;
    @JsonProperty("cpf_recebedor") // Campo interno para o banco de dados
    private String cpfRecebedor;
    @JsonProperty("criado_em")
    private String criadoEm; // Armazenado como String no formato ISO 8601
    @JsonProperty("atualizado_em")
    private String atualizadoEm; // Armazenado como String no formato ISO 8601

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // Construtor vazio
    public Transacao() {
        this.id = UUID.randomUUID().toString();
        String agora = LocalDateTime.now().format(ISO_FORMATTER);
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    // Construtor completo
    public Transacao(double valor, String cpfEnviador, String cpfRecebedor) {
        this(); // Chama o construtor vazio para gerar o ID e as datas
        this.valor = valor;
        this.cpfEnviador = cpfEnviador;
        this.cpfRecebedor = cpfRecebedor;
    }

    // Getters e Setters para todos os campos
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    // Getters e Setters para os objetos de usuário (usados na resposta do servidor)
    public Usuario getUsuarioEnviador() { return usuarioEnviador; }
    public void setUsuarioEnviador(Usuario usuarioEnviador) { this.usuarioEnviador = usuarioEnviador; }

    public Usuario getUsuarioRecebedor() { return usuarioRecebedor; }
    public void setUsuarioRecebedor(Usuario usuarioRecebedor) { this.usuarioRecebedor = usuarioRecebedor; }

    // Getters e Setters para os CPFs (usados nos DAOs)
    public String getCpfEnviador() { return cpfEnviador; }
    public void setCpfEnviador(String cpfEnviador) { this.cpfEnviador = cpfEnviador; }

    public String getCpfRecebedor() { return cpfRecebedor; }
    public void setCpfRecebedor(String cpfRecebedor) { this.cpfRecebedor = cpfRecebedor; }

    // Getters e Setters para as datas
    public String getCriadoEm() { return criadoEm; }
    public void setCriadoEm(String criadoEm) { this.criadoEm = criadoEm; }

    public String getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(String atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    @Override
    public String toString() {
        return "Transacao{" +
                "id='" + id + '\'' +
                ", valor=" + valor +
                ", cpfEnviador='" + cpfEnviador + '\'' +
                ", cpfRecebedor='" + cpfRecebedor + '\'' +
                '}';
    }
}