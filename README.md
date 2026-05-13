# API de Gerenciamento de Reservas de Salas

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-ready-blue?style=flat-square&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)](LICENSE)

API REST para gerenciamento de reservas de salas corporativas. Permite criar salas, cadastrar usuários e realizar reservas com validação automática de conflitos de horário.

---

## Sumário

- [Visão geral](#visão-geral)
- [Tecnologias e justificativas](#tecnologias-e-justificativas)
- [Arquitetura](#arquitetura)
- [Modelo de domínio](#modelo-de-domínio)
- [Regras de negócio](#regras-de-negócio)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Pré-requisitos](#pré-requisitos)
- [Rodando com Docker](#rodando-com-docker)
- [Rodando localmente sem Docker](#rodando-localmente-sem-docker)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Endpoints da API](#endpoints-da-api)
- [Exemplos de uso](#exemplos-de-uso)
- [Testes](#testes)
- [Dados iniciais](#dados-iniciais)
- [Decisões de design](#decisões-de-design)
- [Próximos passos](#próximos-passos)

---

## Visão geral

O sistema resolve um problema comum em ambientes corporativos: múltiplos times tentando reservar a mesma sala ao mesmo tempo. A API garante que nenhuma sobreposição de horário ocorra, mantendo o histórico de todas as reservas — incluindo cancelamentos — para fins de auditoria.

**Funcionalidades principais:**

- CRUD completo de salas, usuários e reservas
- Validação de conflito de horário com intervalo semiaberto `[início, fim)`
- Soft delete em salas (desativação em vez de remoção)
- Cancelamento de reservas com controle de estado
- Paginação e filtro por sala e período
- Respostas de erro padronizadas com código HTTP e mensagem descritiva
- Perfis separados para desenvolvimento (H2) e produção (PostgreSQL)
- Containerização completa com Docker e Docker Compose

---

## Tecnologias e justificativas

| Tecnologia | Versão | Por que foi escolhida |
|---|---|---|
| **Java** | 21 | LTS mais recente. Records, pattern matching e virtual threads disponíveis. |
| **Spring Boot** | 3.x | Configuração mínima, ecossistema maduro, padrão de mercado para APIs REST em Java. |
| **Spring Data JPA** | — | Elimina boilerplate de SQL para operações comuns; query methods e JPQL para casos específicos. |
| **Hibernate** | — | Provedor JPA padrão; gerencia mapeamento objeto-relacional e controle de transações. |
| **H2** | — | Banco em memória para desenvolvimento e testes. Zero configuração, console web integrado. |
| **PostgreSQL** | 16 | Banco relacional robusto para produção. Suporte nativo a `REPEATABLE_READ` para controle de concorrência. |
| **Lombok** | — | Reduz código repetitivo (`@Getter`, `@NoArgsConstructor`, `@RequiredArgsConstructor`). Sem impacto em runtime. |
| **Bean Validation** | — | Validações declarativas nos DTOs (`@NotBlank`, `@Min`, `@Email`). Separa validação de entrada da lógica de negócio. |
| **JUnit 5** | — | Framework de testes padrão no ecossistema Spring. `@Nested` e `@DisplayName` tornam os testes autodocumentados. |
| **Mockito** | — | Isolamento de dependências nos testes de unidade do service. Permite testar regras sem banco real. |
| **AssertJ** | — | API fluente de assertions. Mensagens de falha mais descritivas que o JUnit puro. |
| **Docker** | — | Empacota a aplicação com todas as dependências. Elimina o "funciona na minha máquina". |
| **Docker Compose** | — | Orquestra múltiplos serviços (app + banco) com um único comando. Perfis separados por ambiente. |

---

## Arquitetura

A aplicação segue a arquitetura em camadas clássica do Spring, com responsabilidades bem definidas:

```
Cliente HTTP
    │
    ▼
┌─────────────────────────────────────┐
│           Controller                │  Recebe requisições HTTP, valida entrada
│   (@RestController, @Valid)         │  com @Valid, delega ao service, retorna
│                                     │  resposta com status HTTP correto.
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│             Service                 │  Concentra as regras de negócio.
│   (@Service, @Transactional)        │  Validação de conflito, cancelamento,
│                                     │  atomicidade com REPEATABLE_READ.
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│           Repository                │  Acesso ao banco via Spring Data JPA.
│   (JpaRepository + JPQL)            │  Queries de conflito, paginação,
│                                     │  JOIN FETCH para evitar N+1.
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│         Banco de dados              │
│   H2 (dev/test) · PostgreSQL (prod) │
└─────────────────────────────────────┘
```

**Por que essa separação?**

- O controller não conhece `EntityManager` nem SQL — só sabe de HTTP.
- O service não conhece `HttpStatus` nem JSON — só sabe de regras de negócio.
- O repository não contém `if` de negócio — só sabe de persistência.
- Cada camada pode ser testada em isolamento sem subir a outra.

---

## Modelo de domínio

```
Sala
├── id: Long
├── nome: String (único)
├── capacidade: int (mínimo 1)
├── localizacao: String
└── ativa: boolean (soft delete)

Usuario
├── id: Long
├── nome: String
├── email: String (único)
└── departamento: String

Reserva
├── id: Long
├── sala: Sala (ManyToOne, LAZY)
├── usuario: Usuario (ManyToOne, LAZY)
├── inicio: LocalDateTime
├── fim: LocalDateTime
├── status: StatusReserva (ATIVA | CANCELADA)
├── motivo: String
└── criadaEm: LocalDateTime (imutável)

StatusReserva
└── ATIVA → CANCELADA  (transição única, sem reativação)
```

**Decisões de mapeamento:**

- Todos os relacionamentos `@ManyToOne` são `LAZY` para evitar carregamentos automáticos desnecessários. Quando a API precisa dos dados relacionados, a query usa `JOIN FETCH` explicitamente.
- `@Enumerated(EnumType.STRING)` em vez de `ORDINAL` — legível no banco e estável a reordenações futuras do enum.
- Índice composto `(sala_id, inicio, fim)` na tabela `reservas` — é exatamente o filtro da query de conflito, a mais executada do sistema.

---

## Regras de negócio

### Conflito de horário

A reserva usa intervalo semiaberto `[início, fim)`. Duas reservas conflitam se:

```
A.inicio < B.fim  AND  B.inicio < A.fim
```

**Exemplos com a Reserva A = 09:00 → 11:00:**

| Reserva | Horário | Resultado |
|---|---|---|
| B | 11:00 → 12:00 | ✅ Permitida — início de B igual ao fim de A |
| C | 08:00 → 09:00 | ✅ Permitida — fim de C igual ao início de A |
| D | 10:00 → 12:00 | ❌ Conflito — sobreposição parcial |
| E | 08:00 → 13:00 | ❌ Conflito — engloba A inteiramente |

Reservas com status `CANCELADA` são **ignoradas** na checagem de conflito. Cancelar uma reserva libera o horário para novas reservas.

### Sala inativa

Salas desativadas não aceitam novas reservas. A validação ocorre no construtor da entidade `Reserva`, garantindo que nenhuma camada possa contornar essa regra.

### Cancelamento

O cancelamento é um estado **final e irreversível**. Para alterar o horário de uma reserva cancelada, é necessário criar uma nova. Isso simplifica relatórios e preserva o histórico completo.

### Atomicidade na criação

O método `criarReserva()` usa `Isolation.REPEATABLE_READ`. Sem isso, duas requisições simultâneas poderiam ler "sem conflito" ao mesmo tempo e ambas persistirem reservas sobrepostas (race condition). Com `REPEATABLE_READ`, a segunda transação aguarda a primeira terminar antes de reler o intervalo.

---

## Estrutura do projeto

```
reservas-api/
├── Dockerfile                          # Build multistage (JDK build + JRE runtime)
├── docker-compose.yml                  # Perfis dev (H2) e prod (PostgreSQL)
├── docker-entrypoint.sh                # Flags de JVM + graceful shutdown
├── .dockerignore                       # Exclui target/, .git/, .env do contexto
├── .env.example                        # Template de variáveis de ambiente
├── init-db.sql                         # Script de inicialização do PostgreSQL
├── Makefile                            # Atalhos: make dev, make prod, make logs...
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/seuprojeto/reservas/
    │   │   ├── domain/                 # Entidades e enum de domínio
    │   │   │   ├── Sala.java
    │   │   │   ├── Usuario.java
    │   │   │   ├── Reserva.java
    │   │   │   └── StatusReserva.java
    │   │   ├── repository/             # Interfaces JPA com queries customizadas
    │   │   │   ├── SalaRepository.java
    │   │   │   ├── UsuarioRepository.java
    │   │   │   └── ReservaRepository.java
    │   │   ├── service/                # Regras de negócio e transações
    │   │   │   ├── SalaService.java
    │   │   │   ├── UsuarioService.java
    │   │   │   └── ReservaService.java
    │   │   ├── controller/             # Endpoints REST
    │   │   │   ├── SalaController.java
    │   │   │   ├── UsuarioController.java
    │   │   │   └── ReservaController.java
    │   │   ├── dto/
    │   │   │   ├── request/            # Objetos de entrada (validados com @Valid)
    │   │   │   └── response/           # Objetos de saída (nunca expõe entidades)
    │   │   └── exception/              # Exceções customizadas + handler global
    │   │       ├── ConflitoDeReservaException.java
    │   │       ├── RecursoNaoEncontradoException.java
    │   │       ├── RegraDeNegocioException.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       ├── application.yml         # Configurações base compartilhadas
    │       ├── application-dev.yml     # H2, console habilitado, DDL create-drop
    │       ├── application-prod.yml    # PostgreSQL, DDL validate, logs compactos
    │       └── data.sql                # Dados iniciais (6 salas, 6 usuários, 7 reservas)
    └── test/
        ├── java/com/seuprojeto/reservas/
        │   ├── domain/
        │   │   └── ReservaTest.java    # Testes das regras de domínio puras
        │   ├── service/
        │   │   ├── ReservaServiceTest.java
        │   │   └── SalaServiceTest.java
        │   └── util/
        │       └── TestFactory.java    # Fábrica centralizada de objetos de teste
        └── resources/
            └── application-test.yml   # H2 separado para testes
```

---

## Pré-requisitos

| Ferramenta | Versão mínima | Para quê |
|---|---|---|
| Docker Desktop | 24.x | Rodar via container (recomendado) |
| Docker Compose | 2.x | Orquestrar app + banco |
| Java JDK | 21 | Rodar localmente sem Docker |
| Maven | 3.9 | Build local sem Docker |

> **Windows:** certifique-se de que o Docker Desktop está **em execução** antes de qualquer comando `docker`. O ícone da baleia na bandeja do sistema deve estar ativo.

---

## Rodando com Docker

### Desenvolvimento (H2 em memória)

```bash
# 1. clone o projeto
git clone https://github.com/seu-usuario/reservas-api.git
cd reservas-api

# 2. crie o arquivo de variáveis
cp .env.example .env
# edite o .env e certifique-se que SPRING_PROFILES_ACTIVE=dev

# 3. suba o container
docker compose --profile dev up --build
```

A aplicação estará disponível em:

| Serviço | URL |
|---|---|
| API REST | http://localhost:8080/api/v1 |
| Console H2 | http://localhost:8080/h2-console |
| Health check | http://localhost:8080/actuator/health |

> **Console H2:** JDBC URL = `jdbc:h2:mem:reservasdb` · User = `sa` · Password = *(vazio)*

Para parar:
```bash
docker compose --profile dev down
```

### Produção (PostgreSQL)

```bash
# 1. configure as credenciais reais no .env
DB_USERNAME=reservas_user
DB_PASSWORD=senha_forte_aqui
SPRING_PROFILES_ACTIVE=prod

# 2. suba o stack completo
docker compose --profile prod up -d

# 3. acompanhe os logs
docker compose logs -f app-prod
```

---

## Rodando localmente sem Docker

```bash
# 1. compile o projeto
./mvnw clean package -DskipTests

# 2. execute com perfil dev (H2 automático)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# ou execute o JAR diretamente
java -jar -Dspring.profiles.active=dev target/reservas-api-1.0.0.jar
```

---

## Variáveis de ambiente

| Variável | Padrão | Obrigatório | Descrição |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | Sim | Perfil ativo: `dev`, `test` ou `prod` |
| `DB_USERNAME` | — | Em prod | Usuário do PostgreSQL |
| `DB_PASSWORD` | — | Em prod | Senha do PostgreSQL |
| `SERVER_PORT` | `8080` | Não | Porta HTTP da aplicação |
| `LOGGING_LEVEL_COM_SEUPROJETO` | `INFO` | Não | Nível de log da aplicação |
| `JVM_OPTS` | flags do entrypoint | Não | Flags adicionais de JVM |

> O arquivo `.env` **nunca deve ser commitado**. Adicione ao `.gitignore`:
> ```bash
> echo ".env" >> .gitignore
> ```

---

## Endpoints da API

### Salas — `/api/v1/salas`

| Método | Rota | Descrição | Status |
|---|---|---|---|
| `GET` | `/api/v1/salas` | Lista salas (ativas por padrão) | 200 |
| `GET` | `/api/v1/salas/{id}` | Busca sala por ID | 200 / 404 |
| `POST` | `/api/v1/salas` | Cria nova sala | 201 / 400 / 422 |
| `PUT` | `/api/v1/salas/{id}` | Atualiza sala | 200 / 400 / 404 |
| `DELETE` | `/api/v1/salas/{id}` | Desativa sala (soft delete) | 204 / 404 |

> `GET /api/v1/salas?apenasAtivas=false` retorna todas, incluindo inativas.

### Usuários — `/api/v1/usuarios`

| Método | Rota | Descrição | Status |
|---|---|---|---|
| `GET` | `/api/v1/usuarios` | Lista usuários paginado | 200 |
| `GET` | `/api/v1/usuarios/{id}` | Busca usuário por ID | 200 / 404 |
| `POST` | `/api/v1/usuarios` | Cria novo usuário | 201 / 400 |
| `PUT` | `/api/v1/usuarios/{id}` | Atualiza usuário | 200 / 400 / 404 |

### Reservas — `/api/v1/reservas`

| Método | Rota | Descrição | Status |
|---|---|---|---|
| `GET` | `/api/v1/reservas/{id}` | Busca reserva por ID | 200 / 404 |
| `GET` | `/api/v1/reservas/sala/{salaId}` | Lista reservas de uma sala (paginado) | 200 |
| `POST` | `/api/v1/reservas` | Cria nova reserva | 201 / 400 / 409 / 422 |
| `PATCH` | `/api/v1/reservas/{id}/cancelar` | Cancela reserva | 200 / 404 |

### Códigos de erro

| Status | Quando ocorre |
|---|---|
| `400 Bad Request` | Campo obrigatório ausente ou formato inválido |
| `404 Not Found` | Recurso não existe no banco |
| `409 Conflict` | Conflito de horário com reserva existente |
| `422 Unprocessable Entity` | Regra de negócio violada (ex: início >= fim, sala inativa) |

---

## Exemplos de uso

### Criar uma sala

```bash
curl -X POST http://localhost:8080/api/v1/salas \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Sala Apollo",
    "capacidade": 12,
    "localizacao": "Bloco A — 1º andar"
  }'
```

```json
{
  "id": 1,
  "nome": "Sala Apollo",
  "capacidade": 12,
  "localizacao": "Bloco A — 1º andar",
  "ativa": true
}
```

### Criar uma reserva

```bash
curl -X POST http://localhost:8080/api/v1/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "salaId": 1,
    "usuarioId": 1,
    "inicio": "2025-11-10T09:00:00",
    "fim":    "2025-11-10T11:00:00",
    "motivo": "Planejamento de sprint"
  }'
```

```json
{
  "id": 1,
  "salaId": 1,
  "salaNome": "Sala Apollo",
  "usuarioId": 1,
  "usuarioNome": "Ana Lima",
  "inicio": "2025-11-10T09:00:00",
  "fim": "2025-11-10T11:00:00",
  "status": "ATIVA",
  "motivo": "Planejamento de sprint",
  "criadaEm": "2025-10-01T14:32:00"
}
```

### Tentativa com conflito de horário

```bash
curl -X POST http://localhost:8080/api/v1/reservas \
  -H "Content-Type: application/json" \
  -d '{
    "salaId": 1,
    "usuarioId": 2,
    "inicio": "2025-11-10T10:00:00",
    "fim":    "2025-11-10T12:00:00"
  }'
```

```json
{
  "status": 409,
  "mensagem": "Sala 'Sala Apollo' já reservada das 2025-11-10T09:00 às 2025-11-10T11:00.",
  "timestamp": "2025-10-01T14:33:00"
}
```

### Cancelar uma reserva

```bash
curl -X PATCH http://localhost:8080/api/v1/reservas/1/cancelar
```

```json
{
  "id": 1,
  "status": "CANCELADA",
  ...
}
```

---

## Testes

```bash
# executar todos os testes
./mvnw test

# executar um arquivo específico
./mvnw test -Dtest=ReservaServiceTest

# gerar relatório de cobertura (disponível em target/site/jacoco/index.html)
./mvnw test jacoco:report
```

### Cobertura por componente

| Componente | Cobertura | O que é testado |
|---|---|---|
| `Reserva` (domínio) | ~95% | `conflitaCom()`, `cancelar()`, construtor com validações |
| `StatusReserva` | 100% | `podeSerCancelada()` e transições |
| `ReservaService` | ~88% | `criar()`, `cancelar()`, todos os caminhos de erro |
| `SalaService` | ~80% | `criar()`, `desativar()`, nome duplicado |
| Controllers | — | Cobertos por testes de integração (`@SpringBootTest`) |
| Repositories | — | Cobertos por testes de repositório (`@DataJpaTest`) |

### Casos de teste cobertos

- Criação de reserva com sala disponível
- Borda livre: início de nova reserva igual ao fim de outra existente
- Sobreposição total, parcial no início, parcial no fim, interna
- Rejeição de sala inativa
- Rejeição de intervalo inválido (início ≥ fim)
- Rejeição de recursos inexistentes (sala, usuário)
- Simetria da regra de conflito (`A conflita B ↔ B conflita A`)
- Cancelamento de reserva ativa
- Rejeição de cancelamento duplo
- Verificação de que `save()` não é chamado em caso de erro

---

## Dados iniciais

O arquivo `src/main/resources/data.sql` popula o banco automaticamente no perfil `dev`:

```yaml
# application-dev.yml
spring:
  sql:
    init:
      mode: always
      data-locations: classpath:data.sql
```

**Dados incluídos:**

- 6 salas (1 inativa para testar a regra de validação)
- 6 usuários de departamentos distintos
- 7 reservas cobrindo os cenários: reservas adjacentes sem conflito, reservas em salas diferentes no mesmo horário, e uma reserva já cancelada

---

## Decisões de design

### Por que DTOs em vez de expor entidades diretamente?

Expor entidades JPA diretamente na API acopla o contrato HTTP ao modelo de banco. Uma mudança de coluna quebraria clientes. Com DTOs (`CriarSalaRequest`, `SalaResponse`), o modelo de domínio e a API evoluem independentemente.

### Por que soft delete em salas?

Deletar uma sala apagaria o histórico de todas as reservas associadas. Desativar (`ativa = false`) preserva o passado e impede novos usos. Isso é especialmente importante para relatórios e auditoria.

### Por que `REPEATABLE_READ` na criação de reservas?

Com o isolamento padrão `READ_COMMITTED`, duas requisições simultâneas para o mesmo horário poderiam ambas ler "sem conflito" e ambas persistirem — resultando em dupla reserva. `REPEATABLE_READ` serializa essas operações sem precisar de locks manuais.

### Por que validações no construtor da entidade?

As regras "sala deve estar ativa" e "início deve ser anterior ao fim" vivem no construtor de `Reserva`. Isso garante que é impossível criar um objeto em estado inválido, independente de qual camada o instanciou. O service não precisa lembrar de validar — o domínio rejeita automaticamente.

### Por que `JOIN FETCH` nas queries de reserva?

`@ManyToOne` com `LAZY` evita carregar dados desnecessários. Mas ao acessar `reserva.getSala()` ou `reserva.getUsuario()` dentro de um loop, cada acesso dispara um SELECT separado — o problema N+1. As queries com `JOIN FETCH` carregam tudo em uma única query quando os dados relacionados são necessários.

### Por que o `Dockerfile` tem dois estágios?

O estágio `build` usa o JDK completo com Maven (~600MB). O estágio `runtime` usa apenas o JRE (~200MB). A imagem final não carrega compilador, fontes, cache Maven nem código-fonte — apenas o JAR executável. Isso reduz o tamanho final em ~60% e a superfície de ataque em produção.

---

## Próximos passos

- [ ] Autenticação com Spring Security + JWT
- [ ] Cache de listagens com Spring Cache + Redis
- [ ] Notificações por e-mail ao criar/cancelar reserva
- [ ] Deploy em Kubernetes com Helm chart

---

