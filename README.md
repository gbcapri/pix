# pix
Requisitos Funcionais e Não Funcionais do Projeto Final (Transferência de Pix)

Requisitos Funcionais:
CRUD - Usuários
Cadastro de usuário
Leitura dos DADOS do usuário
Atualizar os DADOS do usuário
Apagar o usuário
Login de usuário
Logout de usuário
CRUD - Transações
Cadastro de transação
Leitura dos DADOS de uma transação
E/S - Registro de saldo (retornado pela leitura dos dados do usuário)
Requisitos Não Funcionais:
Só pode interagir no sistema LOGADO
Transações atômicas
Transações alteram E/S - Registro de saldo
Saída do saldo deve verificar quantia suficiente na conta
Pesquisa de extrato tem limite de 31 dias entre inicio e fim
Um saldo por usuário
Token gerado pelo servidor deve ser seguro e aleatório
Interações entre servidor e cliente devem seguir o protocolo de mensagens: link para o protocolo
