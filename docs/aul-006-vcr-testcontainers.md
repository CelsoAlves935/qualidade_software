# Aula 006 - Testes com VCR e TestContainers

## Visão Geral

Esta aula demonstra como usar dois padrões importantes para testes de software:

1. **VCR (Video Cassette Recorder)** - Para mockar chamadas HTTP externas
2. **TestContainers** - Para testes de integração com banco de dados real

## Conceitos Fundamentais

### Padrão VCR

O padrão VCR grava chamadas HTTP reais uma vez e depois reproduz essas gravadas durante os testes, eliminando a dependência de serviços externos.

**Vantagens:**
- ✅ Testes rápidos (sem latência de rede)
- ✅ Confiáveis (sem dependência de serviços externos)
- ✅ Determinísticos (mesma resposta sempre)
- ✅ Funcionam offline
- ✅ Ideais para CI/CD

**Quando usar:**
- APIs de terceiros (Correios, gateways de pagamento)
- Serviços web que você não controla
- Quando a velocidade do teste é crítica

### TestContainers

TestContainers fornece instâncias descartáveis de bancos de dados em containers Docker para testes.

**Vantagens:**
- ✅ Testes com banco de dados real
- ✅ Ambiente realista localmente
- ✅ Validação completa de queries

**Quando usar:**
- Testes de integração com banco de dados
- Desenvolvimento local com validação completa
- Testando migrações de schema

## Estrutura do Projeto

### Arquivos Principais

```
src/
├── main/
│   ├── java/.../
│   │   ├── entity/
│   │   │   └── Address.java                    # Entity para endereço CEP
│   │   ├── service/
│   │   │   └── CorreiosService.java            # Serviço de consulta CEP
│   │   └── controller/
│   │       └── CorreiosController.java         # Controller web para consulta
│   └── resources/
│       └── templates/correios/
│           └── cep-lookup.html                 # Interface web
│
└── test/
    ├── java/.../
    │   ├── util/
    │   │   └── VcrHelper.java                  # Utilitário VCR
    │   ├── vcr/
    │   │   └── CorreiosVcrTest.java            # Testes VCR (para CI)
    │   └── integration/
    │       └── CorreiosIntegrationTest.java    # Testes com TestContainers (local)
    └── resources/
        └── vcr-cassettes/
            ├── cep_01001000.json               # Cassette: São Paulo
            ├── cep_20040020.json               # Cassette: Rio de Janeiro
            └── cep_invalid.json                # Cassette: CEP inválido
```

## Como Usar

### Executando Testes VCR (Rápido - Para CI)

```bash
# Executar apenas testes VCR
./mvnw test -Dtest=CorreiosVcrTest

# Executar todos os testes do CI
./mvnw test -Dtest="**/vcr/*Test,**/service/*Test,**/controller/*Test"
```

**Características:**
- ⚡ Muito rápido (~2 segundos)
- 🚫 Sem dependência de Docker
- ✅ Perfeito para CI/CD
- 📦 Usa cassetes gravados

### Executando Testes com TestContainers (Completo - Para Local)

```bash
# Executar testes de integração com TestContainers
./mvnw test -Dtest=CorreiosIntegrationTest
```

**Requisitos:**
- Docker instalado e rodando
- Java 21

**Características:**
- 🐳 Requer Docker
- 🔄 Mais lento (sobe container MongoDB)
- ✅ Teste completo com banco real
- 💻 Ideal para desenvolvimento local

## Gravando Novos Cassetes

Para gravar um novo cassette (fazer chamada real à API):

```java
// 1. Criar VCRHelper em modo de gravação
VcrHelper recorder = new VcrHelper(
    "src/test/resources/vcr-cassettes",
    "novo_cassette",
    true  // recordMode = true
);

// 2. Iniciar gravação
recorder.start();

// 3. Fazer chamada real
CorreiosService service = new CorreiosService(recorder.getOkHttpClient());
Address address = service.findAddressByCep("70002900");

// 4. Parar e salvar
recorder.stop();
```

O arquivo cassette será criado em `src/test/resources/vcr-cassettes/novo_cassette.json`.

## Exemplo Prático: Consulta CEP

Este projeto demonstra o padrão VCR com um caso de uso real: consulta de endereço por CEP usando a API aberta do ViaCEP.

### API Utilizada

**ViaCEP** - https://viacep.com.br/

```
GET https://viacep.com.br/ws/{cep}/json/
```

**Exemplo de resposta:**
```json
{
  "cep": "01001-000",
  "logradouro": "Praça da Sé",
  "complemento": "lado ímpar",
  "bairro": "Sé",
  "localidade": "São Paulo",
  "uf": "SP",
  "ibge": "3550308",
  "gia": "1004",
  "ddd": "11",
  "siafi": "7107"
}
```

### Fluxo de Teste

1. **Gravação (uma vez)**:
   ```
   Teste → API Real → Cassette JSON
   ```

2. **Playback (sempre)**:
   ```
   Teste → Cassette JSON → Resposta Mockada
   ```

## CI/CD Configuration

O arquivo `.github/workflows/ci.yml` está configurado para usar apenas testes VCR:

```yaml
test:
  runs-on: ubuntu-latest
  steps:
    - name: Run VCR-based tests (no TestContainers in CI)
      run: ./mvnw -B -ntp clean test \
        -Dtest="**/vcr/*Test,**/service/*Test,**/controller/*Test"
```

**Por que não TestContainers no CI?**
- ❌ Requer Docker (nem sempre disponível)
- ❌ Mais lento (sobe containers)
- ❌ Consome mais recursos
- ✅ Use apenas VCR no CI

## Comparação de Performance

| Tipo de Teste | Tempo | Requer Docker | Uso de Rede |
|---------------|-------|---------------|-------------|
| VCR Tests     | ~2s   | ❌ Não        | ❌ Não       |
| TestContainers| ~30s  | ✅ Sim        | ❌ Não       |
| Real API      | ~5s   | ❌ Não        | ✅ Sim       |

## Competências Desenvolvidas

Após esta aula, você será capaz de:

- ✅ Compreender o padrão VCR e quando aplicá-lo
- ✅ Usar TestContainers para testes de integração local
- ✅ Mockar APIs externas com OkHttp MockWebServer
- ✅ Combinar VCR + TestContainers em testes completos
- ✅ Configurar perfis diferentes para local vs CI
- ✅ Criar e manter cassetes VCR
- ✅ Testar integrações com serviços externos (Correios/CEP)

## Troubleshooting

### Erro: "Cassette file not found"
**Solução:** Verifique se o arquivo existe em `src/test/resources/vcr-cassettes/`

### Erro: "TestContainers requires Docker"
**Solução:** Instale e inicie o Docker Desktop

### Erro: "CEP not found"
**Solução:** Verifique se o CEP tem 8 dígitos e existe na API ViaCEP

## Recursos Adicionais

- [Documentação OkHttp MockWebServer](https://square.github.io/okhttp/mockwebserver/)
- [Documentação TestContainers](https://www.testcontainers.org/)
- [API ViaCEP](https://viacep.com.br/)
- [Padrão VCR (Ruby)](https://github.com/vcr/vcr) - inspiração original

## Próximos Passos

1. Experimente gravar um novo cassette com um CEP diferente
2. Adicione mais testes para cobrir casos de erro
3. Combine VCR com outros tipos de testes
4. Explore TestContainers com outros bancos (PostgreSQL, MySQL, etc.)

---

**Nota:** Esta aula faz parte da série de aulas sobre qualidade de software e testes automatizados.
