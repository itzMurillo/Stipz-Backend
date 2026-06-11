# Relatorio de Validacao e Ajustes dos Documentos Stipz

Data da validacao: 11/06/2026

## 1. Resultado dos testes do backend

Comando executado:

```powershell
.\mvnw.cmd test
```

Resultado:

- 21 testes executados
- 0 falhas
- 0 erros
- 0 testes ignorados
- Build finalizado com sucesso

### 1.1 Validacoes verificadas

- Campos obrigatorios enviados como `null`, vazios ou ausentes.
- Corpo JSON ausente ou malformado.
- Parametros obrigatorios ausentes.
- Datas no passado e periodo com fim anterior ao inicio.
- Numeros negativos ou iguais a zero onde nao sao permitidos.
- Capacidade da sala excedida.
- Quantidade de cadeiras extras invalida.
- Recursos repetidos, inexistentes ou indisponiveis.
- Sala duplicada, inclusive com diferenca apenas entre maiusculas e minusculas.
- Conflito de horario.
- Limite de cinco salas por semana.
- Justificativa obrigatoria para evento com mais de uma sala.
- Cancelamento por outro usuario, fora do prazo ou em status invalido.
- Exclusao do administrador inicial.
- Login com email inexistente ou senha incorreta.

### 1.2 Seguranca verificada

- Requisicao sem token retorna `401`.
- Token JWT invalido retorna `401`.
- Usuario COMUM nao pode consultar usuarios.
- Usuario COMUM nao pode cadastrar salas.
- Usuario COMUM nao pode listar todas as reservas.
- Usuario COMUM nao pode aprovar ou rejeitar reservas.
- Usuario COMUM nao pode gerar backup de auditoria.
- Usuario COMUM pode consultar apenas `GET /reservas/minhas`.
- Usuario ADMIN pode consultar usuarios e executar funcoes administrativas.
- A senha do usuario nao aparece nas respostas JSON.
- Login invalido usa uma mensagem generica, sem revelar se o email existe.
- Duas tentativas simultaneas para a mesma sala e horario nao geram duas reservas.

### 1.3 Correcoes aplicadas durante a validacao

- Ordem das regras do Spring Security corrigida.
- `GET /usuarios` passou a ser exclusivo do ADMIN.
- `GET /reservas/minhas` passou a ser permitido aos dois perfis.
- Demais rotas de `/reservas` ficaram administrativas quando aplicavel.
- Requisicoes nao autenticadas passaram a retornar `401`, em vez de `403`.
- Erros internos deixaram de devolver detalhes tecnicos da excecao.
- Falha de concorrencia passou a retornar `409`.
- Validacoes numericas e de datas foram adicionadas aos DTOs.
- Justificativa de evento passou a ser obrigatoria somente quando houver mais de uma sala.
- Reservas com cadeiras extras exigem quantidade maior que zero.
- Nome de sala duplicado passou a ser rejeitado.
- O administrador inicial nao pode ser excluido.

### 1.4 Pontos que ainda merecem atencao

- O Hibernate Envers exibe o aviso `HHH015007` durante a inicializacao. Ele nao impediu os testes nem a auditoria, mas convem revisar a compatibilidade de versoes futuramente.
- O backup depende do executavel `pg_dump` e da configuracao correta de `PG_DUMP_PATH`.
- O armazenamento de backup e local. Em producao, seria recomendado tambem copiar os arquivos para outro servidor ou servico de armazenamento.
- O token do SSE e enviado na URL. Funciona, mas uma evolucao futura mais segura seria usar um token temporario exclusivo para a conexao.
- Os testes executados cobrem o backend. O frontend e os fluxos visuais precisam de uma bateria propria.
- A auditoria atual registra alteracoes persistidas nas entidades. Ela nao registra toda consulta, tentativa de login ou simples visualizacao de tela.
- Nao existem rotas completas de edicao para todas as entidades. Os documentos nao devem afirmar CRUD completo onde ele nao existe.

---

# 2. Alteracoes no Documento de Especificacao de Casos de Uso

Documento atual: `Stipz-Documento de Especificacao de Casos de Uso.pdf`

## 2.1 CONTROLE DE VERSAO

Adicionar:

| Versao | Data | Descricao |
|---|---|---|
| 1.3 | 11/06/2026 | Atualizacao de autenticacao, permissoes, validacoes, eventos, auditoria e concorrencia |

## 2.2 DESCRICAO DO SISTEMA/FUNCIONALIDADE

Adicionar:

> O sistema utiliza autenticacao por email e senha, com emissao de token JWT. Existe apenas um administrador inicial, configurado na implantacao. O administrador cadastra os usuarios comuns, salas e recursos, alem de aprovar ou rejeitar reservas. As senhas sao armazenadas com hash BCrypt e nunca sao retornadas pela API. O sistema registra alteracoes das entidades por meio do Hibernate Envers e possui sincronizacao de alteracoes entre telas por SSE.

Remover ou corrigir qualquer trecho que diga:

- Que qualquer usuario pode cadastrar outro usuario.
- Que existem varios administradores cadastrados normalmente.
- Que a aplicacao registra todas as consultas e visualizacoes em auditoria.

## 2.3 DIAGRAMA DE CLASSES

Atualizar as classes:

- `Usuario`: `id`, `nome`, `email`, `senha`, `perfil`.
- `Sala`: `id`, `nome`, `capacidade`.
- `Reserva`: incluir `status`, `motivoRejeicao`, `cadeirasExtras`, `quantidadeCadeiras`, datas, usuario, sala e evento.
- `Evento`: incluir `nome`, `descricao`, `justificativa`, usuario e datas.
- `ReservaRecurso`: manter recurso e quantidade solicitada.

Adicionar as relacoes:

- Um usuario possui varias reservas.
- Um evento possui varias reservas, uma para cada sala.
- Uma reserva pode possuir zero ou varios recursos.
- Uma sala pode possuir varios recursos.

## 2.4 DIAGRAMA DE CASO DE USO

Adicionar:

- Consultar minhas reservas, para ADMIN e COMUM.
- Substituir sala de uma reserva rejeitada de evento.
- Gerar backup de auditoria, somente ADMIN.
- Receber atualizacoes do sistema em tempo real.

Remover:

- Casos de uso duplicados para "Reservas pendentes" e "Aprovar/Recusar". Eles representam o mesmo fluxo administrativo.

## 2.5 ATORES

### Administrador

Adicionar:

- Existe somente um administrador inicial.
- Pode cadastrar e excluir usuarios comuns.
- Pode cadastrar salas, tipos de recurso e recursos.
- Pode consultar todas as reservas.
- Pode aprovar ou rejeitar reservas pendentes.
- Pode gerar backup de auditoria.
- Em "Minhas reservas", visualiza apenas as reservas feitas por ele.

### Usuario comum

Adicionar:

- Pode criar reserva de sala e evento.
- Pode consultar somente as proprias reservas.
- Pode cancelar somente a propria reserva pendente e dentro do prazo permitido.

## 2.6 UC.001 - LOGIN

Adicionar ao fluxo principal:

1. O usuario informa email e senha.
2. O backend valida a senha usando BCrypt.
3. O backend retorna um token JWT com identificacao e perfil.
4. O frontend envia o token nas proximas requisicoes.

Adicionar aos fluxos alternativos:

- Email ou senha invalidos: retornar `401` com a mensagem "Email ou senha invalidos".
- Token ausente, invalido ou expirado: retornar `401`.

Remover:

- Qualquer afirmacao de que a senha e enviada ou armazenada criptografada pelo frontend.
- Referencia a servico externo de autenticacao, caso ainda exista. A autenticacao e realizada pelo proprio backend.

## 2.7 UC.002 - CADASTRAR USUARIO

Alterar:

- Somente o administrador pode executar o caso de uso.
- Depois da criacao do administrador inicial, os novos usuarios devem possuir perfil COMUM.
- A senha recebida e convertida em hash antes de ser salva.
- A senha nunca e retornada em consultas.
- O administrador inicial nao pode ser excluido.

Remover:

- Edicao de usuario, caso esteja descrita como funcionalidade pronta.
- Criacao comum de novos administradores.

## 2.8 UC.003 - CADASTRAR SALA

Adicionar:

- Nome e capacidade sao obrigatorios.
- A capacidade deve ser maior que zero.
- Nao podem existir duas salas com o mesmo nome, ignorando maiusculas e minusculas.
- Somente ADMIN pode cadastrar.

## 2.9 UC.004 - CADASTRAR RECURSO

Adicionar:

- Quantidade deve ser maior que zero.
- O recurso deve estar associado a um tipo e, quando aplicavel, a uma sala.
- Somente ADMIN pode cadastrar.

## 2.10 UC.006 - CONSULTAR DISPONIBILIDADE/RESERVAR SALA/RECURSO

Adicionar ao fluxo:

- A quantidade de participantes pode ser informada.
- Se informada, nao pode ultrapassar a capacidade da sala.
- Cadeiras extras e quantidade de participantes sao informacoes diferentes.
- Quando `cadeirasExtras` for verdadeiro, `quantidadeCadeiras` deve ser maior que zero.
- Recursos auxiliares sao opcionais.
- A reserva e criada inicialmente com status PENDENTE.
- A verificacao e o salvamento sao protegidos contra duas reservas simultaneas.

Adicionar mensagens:

- "Capacidade da sala excedida. Sala {nome} comporta {capacidade} pessoas, solicitado {quantidade}."
- "Ja existe uma reserva para esta sala no periodo informado."
- "Outro usuario alterou estes dados ao mesmo tempo. Atualize e tente novamente."

Remover:

- RN-12, caso ela diga que recursos auxiliares sao sempre obrigatorios.

## 2.11 UC.007 - RESERVAR SALAS/RECURSOS PARA EVENTO

Adicionar:

- Cada sala possui seu proprio periodo, quantidade de participantes, cadeiras extras e recursos.
- A capacidade e validada individualmente para cada sala.
- A justificativa e obrigatoria apenas quando o evento possuir mais de uma sala.
- Os recursos de cada sala sao opcionais.
- A criacao do evento e transacional: se uma sala falhar, nenhuma reserva do evento e salva.
- Uma sala rejeitada do evento pode ser substituida, mantendo o evento.

Remover:

- Obrigatoriedade de recurso em todas as salas.
- Obrigatoriedade de justificativa para evento com apenas uma sala.

## 2.12 UC.008 - CANCELAR

Alterar:

- ADMIN e COMUM podem cancelar uma reserva criada pelo proprio usuario.
- A reserva deve estar PENDENTE.
- O cancelamento deve ocorrer com no minimo tres horas de antecedencia.
- O cancelamento libera sala e recursos.

## 2.13 UC.009 - APROVAR/RECUSAR

Adicionar:

- Somente ADMIN pode aprovar ou rejeitar.
- Apenas reservas PENDENTES podem ser processadas.
- A rejeicao pode registrar um motivo.
- A alteracao e enviada para as telas conectadas.

## 2.14 UC.010 - RELATORIOS

Alterar para refletir a implementacao:

- O relatorio de utilizacao e montado pelo frontend com os dados autenticados de reservas.
- O backup de auditoria e uma funcao administrativa separada.
- O backup e gerado diariamente e os sete arquivos mais recentes sao mantidos.

Remover:

- Afirmacao de que existe um endpoint completo de relatorios no backend, caso isso ainda nao tenha sido implementado.

## 2.15 UC.011 - VISUALIZAR AGENDA

Alterar:

- Atores: ADMIN e COMUM.
- Cada usuario visualiza somente as proprias reservas.
- A consulta usa `GET /reservas/minhas`.
- Devem ser exibidos os status PENDENTE, APROVADA, REJEITADA e CANCELADA.

## 2.16 REGRAS DE NEGOCIO

Alterar:

- RN-14 e RN-15: substituir "todas as operacoes e consultas" por "criacao, alteracao e exclusao das entidades auditadas".

Adicionar:

- RN-17: O sistema possui somente um administrador inicial, definido na implantacao.
- RN-18: Senhas devem ser armazenadas com BCrypt e nunca retornadas pela API.
- RN-19: Rotas administrativas exigem perfil ADMIN.
- RN-20: A capacidade e validada pela quantidade de participantes, nunca pela quantidade de recursos ou cadeiras extras.
- RN-21: A criacao de evento com varias salas e atomica.
- RN-22: Tentativas simultaneas nao podem produzir sobreposicao de reservas.
- RN-23: A justificativa e obrigatoria somente para evento com mais de uma sala.
- RN-24: Recursos auxiliares sao opcionais.
- RN-25: "Minhas reservas" retorna somente registros do usuario autenticado.
- RN-26: Cadeiras extras exigem quantidade maior que zero.
- RN-27: Alteracoes nas entidades sao auditadas, com backup diario e retencao dos sete arquivos mais recentes.
- RN-28: Alteracoes de reservas, salas e recursos sao notificadas por SSE.

## 2.17 ESTRUTURA DE DADOS

Adicionar em Reserva:

- `cadeirasExtras`: booleano, padrao `false`.
- `quantidadeCadeiras`: inteiro, padrao `0`.
- `motivoRejeicao`: texto opcional.
- `status`: PENDENTE, APROVADA, REJEITADA ou CANCELADA.

Adicionar nas entradas de Reserva e Evento:

- `quantidadeParticipantes`: inteiro opcional, usado para validar a capacidade.

Registrar que:

- `quantidadeParticipantes` nao deve ser confundida com `quantidadeCadeiras`.
- Campos opcionais ausentes usam valores padrao quando aplicavel.

---

# 3. Alteracoes no SRS Mocker Sistemas

Documento atual: `SRS_MockerSistemas.pdf`

## 3.1 CONTROLE DE VERSAO

Adicionar:

| Versao | Data | Descricao |
|---|---|---|
| 1.2 | 11/06/2026 | Atualizacao dos requisitos de seguranca, reserva, evento, auditoria e sincronizacao |

## 3.2 1. INTRODUCAO

### 1.2 Escopo

Adicionar:

- Autenticacao JWT e controle de acesso por perfil.
- Administrador inicial unico.
- Reserva comum e reserva de evento com multiplas salas.
- Auditoria por Hibernate Envers e backup diario.
- Sincronizacao de dados por SSE.
- Validacao de capacidade e protecao contra concorrencia.

### 1.3 Definicoes, siglas e abreviacoes

Adicionar:

- JWT: JSON Web Token.
- BCrypt: algoritmo de hash utilizado para senhas.
- SSE: Server-Sent Events.
- Envers: mecanismo de auditoria de entidades do Hibernate.
- RBAC: controle de acesso baseado em perfis.

## 3.3 2. DESCRICAO GERAL

### 2.2 Funcoes do produto

Adicionar:

- Login com emissao de token.
- Consulta isolada das reservas do usuario autenticado.
- Validacao de capacidade.
- Substituicao de sala rejeitada em evento.
- Backup de auditoria.
- Atualizacao em tempo real das telas.

### 2.3 Caracteristicas dos usuarios

Alterar:

- Existe um ADMIN inicial.
- Os demais usuarios cadastrados pelo ADMIN possuem perfil COMUM.
- O ADMIN tambem pode realizar reservas, mas "Minhas reservas" continua isolado pelo usuario autenticado.

### 2.4 Restricoes

Adicionar:

- Java 17, Spring Boot, PostgreSQL e Angular.
- Segredos obrigatorios em variaveis de ambiente.
- `pg_dump` necessario para o backup SQL.
- Datas e horarios seguem o fuso configurado no servidor.

## 3.4 3.1 REQUISITOS FUNCIONAIS

### RF001 - Gerenciamento de usuarios

Substituir por:

> O sistema deve criar um unico administrador inicial a partir da configuracao de implantacao. O ADMIN deve poder cadastrar, listar e excluir usuarios COMUNS. O administrador inicial nao pode ser excluido.

Remover:

- Edicao de usuario, enquanto nao existir rota implementada.
- CRUD completo, se o texto atual usar esse termo.

### RF002 - Autenticacao

Adicionar:

- Login por email e senha.
- Senha armazenada com BCrypt.
- Emissao de JWT.
- Resposta generica para credenciais invalidas.
- Senha ausente nas respostas.

### RF003 - Gerenciamento de salas e recursos

Adicionar:

- Operacoes restritas ao ADMIN.
- Nome de sala unico.
- Capacidade maior que zero.

Remover:

- Edicao completa, se nao houver endpoint correspondente.

### RF004 - Criacao de reservas

Adicionar:

- Quantidade de participantes opcional.
- Cadeiras extras e quantidade de cadeiras.
- Recursos opcionais.
- Status inicial PENDENTE.

### RF005 - Conflitos de horario

Adicionar:

- A validacao deve ocorrer dentro de transacao.
- A sala deve ser bloqueada durante a verificacao e gravacao.
- Em concorrencia, somente uma tentativa pode ser confirmada.

### RF006 - Aprovacao e rejeicao

Adicionar:

- Exclusivo do ADMIN.
- Somente reserva PENDENTE.
- Motivo da rejeicao opcional.

### RF007 - Cancelamento

Alterar:

- Somente o proprietario pode cancelar.
- Somente reserva PENDENTE.
- Antecedencia minima de tres horas.

### RF008 - Relatorios

Alterar:

> O frontend gera o relatorio de utilizacao a partir dos dados autenticados de reservas. O backend disponibiliza separadamente a geracao do backup de auditoria.

### RF009 - Visualizacao de agenda

Alterar:

- Disponivel para ADMIN e COMUM.
- Deve mostrar apenas as reservas do usuario autenticado.

### RF010 - Consulta de disponibilidade

Adicionar:

- Inicio e fim devem ser informados em conjunto.
- O fim deve ser posterior ao inicio.

### RF011 - Reserva de evento

Adicionar:

- Varias salas em uma unica solicitacao.
- Justificativa obrigatoria quando houver mais de uma sala.
- Capacidade validada individualmente.
- Operacao totalmente transacional.
- Possibilidade de substituir uma sala rejeitada.

### RF013 - Auditoria

Substituir por:

> O sistema deve auditar criacao, alteracao e exclusao das entidades configuradas no Hibernate Envers. Deve gerar backup diario da auditoria e manter os sete arquivos mais recentes.

Remover:

- Obrigacao de auditar toda leitura, login ou visualizacao, pois isso nao esta implementado.

### RF016 - Recursos especiais

Remover ou substituir:

- Remover a regra que afirma que somente alimentos ou recursos nao cadastrados exigem aprovacao.
- Substituir por: "Toda reserva criada deve iniciar com status PENDENTE e ser processada pelo ADMIN."

### Novos requisitos

- RF017: Validar a capacidade de cada sala pela quantidade de participantes.
- RF018: Persistir cadeiras extras e quantidade de cadeiras.
- RF019: Isolar as reservas pelo usuario autenticado em `GET /reservas/minhas`.
- RF020: Notificar alteracoes por SSE.
- RF021: Padronizar respostas de erro para validacao, autenticacao e regra de negocio.
- RF022: Retornar conflito quando uma alteracao concorrente impedir a operacao.
- RF023: Carregar administrador inicial e segredos por variaveis de ambiente.

## 3.5 3.2 REQUISITOS DE USABILIDADE

Adicionar:

- Mensagens devem identificar o campo invalido.
- Mensagens de autenticacao nao devem revelar se o email existe.
- A interface deve atualizar listas ao receber eventos SSE.

## 3.6 3.3 REQUISITOS DE CONFIABILIDADE

Adicionar:

- Reservas e eventos devem ser transacionais.
- Uma falha durante evento nao pode deixar reservas parciais.
- Concorrencia nao pode gerar sobreposicao.
- Backups devem manter retencao de sete dias.

Remover ou marcar como meta futura, caso nao existam medicoes:

- Disponibilidade garantida de 99%.
- MTTR menor ou igual a uma hora.
- Numero maximo de falhas por mil transacoes.

## 3.7 3.4 REQUISITOS DE DESEMPENHO

Adicionar:

- Calculos de disponibilidade devem ser delegados ao banco de dados quando possivel.
- Consultas de listagem devem evitar carregamentos repetidos desnecessarios.

Remover:

- Tempos de resposta absolutos que nao tenham sido medidos por teste de carga.

## 3.8 3.5 REQUISITOS DE SUPORTABILIDADE

Adicionar:

- Configuracao externa por variaveis de ambiente.
- Logs sem senhas, tokens ou detalhes internos enviados ao cliente.
- Caminho do `pg_dump` configuravel.

## 3.9 3.6 RESTRICOES DE DESIGN

Adicionar:

- API REST JSON.
- Spring Security stateless.
- JWT no cabecalho `Authorization: Bearer`.
- PostgreSQL como banco.
- Angular como frontend.

## 3.10 3.9 INTERFACES EXTERNAS

Adicionar:

- `POST /auth/login` para autenticacao.
- Cabecalho Bearer nas rotas protegidas.
- `GET /notificacoes/stream?token=...` para SSE.
- Respostas de erro em JSON com data, status, mensagem e caminho.

## 3.11 REQUISITOS DE SEGURANCA

Adicionar uma secao especifica, caso ainda nao exista:

- Apenas `/auth/login` e documentacao publica podem ser acessadas sem autenticacao.
- Rotas administrativas exigem perfil ADMIN.
- Senhas devem usar BCrypt.
- Segredos devem vir de variaveis de ambiente e arquivos locais de ambiente devem ficar no `.gitignore`.
- Token invalido ou ausente retorna `401`.
- Perfil insuficiente retorna `403`.
- O backend nao deve retornar stack trace ou mensagem tecnica interna ao cliente.
- CSRF permanece desabilitado porque a API e stateless e utiliza token Bearer, devendo essa decisao ser registrada no documento.

Remover ou corrigir:

- Afirmacoes absolutas de conformidade total com OWASP sem auditoria especifica.
- Qualquer exemplo contendo senha real, segredo JWT ou token valido.
