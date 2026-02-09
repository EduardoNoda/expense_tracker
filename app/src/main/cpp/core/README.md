# Visão Geral

Sistema local para registro, classificação e análise de gastos pessoais, com foco em **inserção manual rápida**, organização por usuário e múltiplas fontes de renda, utilizando **SQLite como fonte da verdade**.

O projeto é orientado a um **usuário final real**, priorizando simplicidade de uso, clareza das regras de negócio e persistência confiável, antes de qualquer automação avançada.

---

# Objetivos

- Registrar gastos de forma rápida e direta (modelo "inserir e confirmar")

- Classificar despesas automaticamente, com possibilidade de edição

- Gerenciar múltiplas fontes de renda por usuário

- Associar despesas a meios de pagamento (ex: dinheiro, cartão de crédito)

- Armazenar dados de forma consistente, auditável e offline

- Manter histórico controlado por janela temporal configurável    

---

# Escopo Funcional

## Entrada de Dados

### Inserção Manual (MVP)

- Origem principal do sistema
- Fluxo simplificado: inserir → confirmar
- Dados mínimos:
  - Data da compra
  - Valor
  - Categoria
  - Meio de pagamento
- Dados opcionais:
  - Descrição
  - Observações

---

## Granularidade dos Dados

- **Compra**: unidade principal do sistema
    
- **Produto**: opcional, pertence a exatamente uma compra
    
- Compras são sempre distintas por evento de entrada (QR ou manual), mesmo que ocorram no mesmo local
    

---

# Classificação de Gastos

## Estratégia Inicial

- Classificação automática baseada prioritariamente em:
    
    - Nome do produto (quando disponível)
        
    - Local da compra (fallback)

# Meios de Pagamento

- Gastos podem ser associados a um meio de pagamento

- Exemplo:
 
  - Dinheiro
 
  - Cartão de crédito

- Cartões de crédito:
  
  - Não armazenam dados sensíveis
  
  - São tratados apenas como identificadores lógicos
  
  - Permitem agrupamento e análise de despesas por cartão
        
# Receitas

- Cada usuário pode possuir uma ou mais fontes de renda

- O sistema permite:

  - Cadastro de múltiplas receitas

  - Frequências distintas (ex: quinzenal, mensal)

- O número de receitas é configurável, com padrão inicial de uma por usuário


## Regras

- Compra detalhada → classificação por produto
    
- Compra resumida → classificação por local
    
- Classificação padrão inicial (hardcoded ou tabela base)
    
- Usuário pode:
    
    - Editar classificação
        
    - Corrigir erros
         
    - Estado original (automático) e estado corrigido pelo usuário são ambos persistidos para fins de auditoria e análise futura
        

> Alterações feitas pelo usuário afetam os dados persistidos e, consequentemente, os relatórios futuros.

---

# Persistência de Dados (SQLite)

## Princípios

- SQLite é a **fonte da verdade**
    
- Relatórios são derivados dos dados persistidos
    
- Falha no Excel não compromete os dados
    

## Dados Persistidos

- Compras
    
- Produtos (quando existentes)
    
- Classificações
    
- Datas (data da compra e data de inserção)

- Identificadores fiscais (quando disponíveis), como número da nota e código de verificação
    

> Invariantes mais rígidas serão definidas após validação do domínio real (QR code).

---

# Histórico e Janela Temporal

## Janela Deslizante

- Período padrão: 12 meses ativos
    
- Dados além do período ativo:
    
    - Arquivados por um período adicional (ex: +12 meses)
        
    - Após limite máximo configurável (ex: 36 ou 48 meses), dados antigos podem ser removidos
        

## Estratégia Adaptativa

- O sistema pode ajustar o período de retenção com base no comportamento do usuário
    
- Exemplo:
    
    - Usuário usa 12 meses → mantém 24 (12 ativos + 12 arquivados)
        
    - Usuário usa 36 meses → mantém 48     

---

# Versionamento

- README versionado
    
- Relatórios versionados (opcional)
    
- Histórico do software considerado parte do projeto
    

---

# Privacidade e Segurança (Em Aberto)

- Dados tratados como sensíveis
    
- Sistema local, sem sincronização externa
    
- Estratégias de proteção ainda a definir:
    
    - Criptografia local
        
    - Proteção por usuário

- Documentos fiscais podem conter dados pessoais identificáveis (ex: CPF, nome)
        
>A ausência de criptografia no MVP é uma decisão consciente para reduzir complexidade inicial, dado o caráter local e offline do sistema.

---

# Stack

- C++
    
- SQLite
    

(UI e mobile serão tratados em fase posterior)