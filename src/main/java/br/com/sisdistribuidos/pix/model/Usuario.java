package br.com.sisdistribuidos.pix.model;

import com.fasterxml.jackson.annotation.JsonInclude;

// A anotação @JsonInclude é usada para não incluir campos nulos na serialização JSON.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Usuario {
    private String cpf;
    private String nome;
    private String senha;
    private double saldo;

    // Construtor vazio, necessário para o Jackson
    public Usuario() {}

    // Construtor completo para facilitar a criação de objetos
    public Usuario(String cpf, String nome, String senha, double saldo) {
        this.cpf = cpf;
        this.nome = nome;
        this.senha = senha;
        this.saldo = saldo;
    }
    
    // Getters e Setters para todos os campos
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }

    @Override
    public String toString() {
        return "Usuario{" +
                "cpf='" + cpf + '\'' +
                ", nome='" + nome + '\'' +
                ", saldo=" + saldo +
                '}';
    }
}