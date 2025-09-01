package br.com.sisdistribuidos.pix.model;

import java.util.UUID;

// A classe Transacao representa uma transação PIX.
public class Transacao {
    private String id;
    private double valor;
    private String cpfEnviador; // CPF do usuário que enviou a transação
    private String cpfRecebedor; // CPF do usuário que recebeu a transação

    // Construtor vazio
    public Transacao() {
        this.id = UUID.randomUUID().toString(); // Gerar um ID único para a transação
    }

    // Construtor completo
    public Transacao(double valor, String cpfEnviador, String cpfRecebedor) {
        this(); // Chama o construtor vazio para gerar o ID
        this.valor = valor;
        this.cpfEnviador = cpfEnviador;
        this.cpfRecebedor = cpfRecebedor;
    }
    
    // Getters e Setters para todos os campos
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getCpfEnviador() { return cpfEnviador; }
    public void setCpfEnviador(String cpfEnviador) { this.cpfEnviador = cpfEnviador; }

    public String getCpfRecebedor() { return cpfRecebedor; }
    public void setCpfRecebedor(String cpfRecebedor) { this.cpfRecebedor = cpfRecebedor; }

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