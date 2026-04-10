# Stipz - Backend

Sistema de gerenciamento de **reservas de salas e recursos**, desenvolvido com **Spring Boot**.

---

## Sobre o projeto

O Stipz é uma aplicação que permite:

-  Cadastro de usuários
-  Gerenciamento de salas
-  Controle de recursos (tecnológicos e alimentícios)
-  Criação e gerenciamento de reservas
-  Regras de negócio (conflito de horário, aprovação, limite, etc.)

---

##  Tecnologias utilizadas

- Java 17+
- Spring Boot
- Spring Data JPA
- Spring Security
- PostgreSQL
- Maven

---

##  Estrutura do projeto

src/main/java/br/com/stipz
-  config # Configurações (CORS, Security)
-  controller # Endpoints da API
-  domain # Entidades
-  DTO # Objetos de transferência
-  nums # Enumerações
-  exception # Tratamento de erros
-  repository # Acesso ao banco
-  service # Regras de negócio
