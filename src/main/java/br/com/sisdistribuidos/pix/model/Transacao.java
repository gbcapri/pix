package br.com.sisdistribuidos.pix.model;

import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

// A anotação @JsonInclude é usada para não incluir campos nulos na serialização JSON.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Transacao {
    private String id;
    @JsonProperty("valor_enviado")
    private double valor;
    @JsonProperty("usuario_enviador")
    private Usuario usuarioEnviador;
    @JsonProperty("usuario_recebedor") 
    private Usuario usuarioRecebedor; 
    @JsonProperty("cpf_enviador") 
    private String cpfEnviador;
    @JsonProperty("cpf_recebedor") 
    private String cpfRecebedor;
    @JsonProperty("criado_em")
    private String criadoEm; 
    @JsonProperty("atualizado_em")
    private String atualizadoEm; 

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public Transacao() {
        this.id = UUID.randomUUID().toString();
        String agora = LocalDateTime.now().format(ISO_FORMATTER);
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public Transacao(double valor, String cpfEnviador, String cpfRecebedor) {
        this();
        this.valor = valor;
        this.cpfEnviador = cpfEnviador;
        this.cpfRecebedor = cpfRecebedor;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    
    
    public Usuario getUsuarioEnviador() { return usuarioEnviador; }
    public void setUsuarioEnviador(Usuario usuarioEnviador) { this.usuarioEnviador = usuarioEnviador; }

    public Usuario getUsuarioRecebedor() { return usuarioRecebedor; }
    public void setUsuarioRecebedor(Usuario usuarioRecebedor) { this.usuarioRecebedor = usuarioRecebedor; }

    
    
    public String getCpfEnviador() { return cpfEnviador; }
    public void setCpfEnviador(String cpfEnviador) { this.cpfEnviador = cpfEnviador; }

    public String getCpfRecebedor() { return cpfRecebedor; }
    public void setCpfRecebedor(String cpfRecebedor) { this.cpfRecebedor = cpfRecebedor; }

    
    
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