# Visão Geral

Sistema local para registro, classificação e análise de gastos pessoais a partir de **QR codes de notas fiscais** e **inserções manuais**, com **SQLite como fonte da verdade** e **relatórios em Excel (.xlsx)** como camada de visualização inicial.

O projeto prioriza **engenharia, modelagem de domínio e decisões explícitas**, não dependência de UI ou linguagem específica.

---

# Objetivos

- Registrar compras a partir de QR codes e inserções manuais
    
- Classificar gastos automaticamente (com possibilidade de edição pelo usuário)
    
- Armazenar dados de forma consistente e auditável
    
- Gerar relatórios em Excel espelhando o estado atual do banco
    
- Manter histórico controlado por janela temporal configurável
    

---

# Escopo Funcional

## Origens de Entrada

### 1. QR Code

- Origem automática
    
- Pode retornar:
    
    - Compra detalhada (produtos, valores unitários, totais)
        
    - Compra resumida (apenas local, data e valor total)
        
- Dados esperados (a confirmar em _Descobertas de Campo_):
    
    - Data da compra
        
    - Local
        
    - Itens (quando disponíveis)
        
    - Valor total
        

>  O comportamento exato do QR code depende do emissor da nota fiscal. Este projeto assume **variabilidade de retorno**.

### 2. Inserção Manual

- Origem controlada pelo usuário
    
- Modos possíveis:
    
    - Inserção detalhada (produtos + valores)
        
    - Inserção resumida (local + valor total)
        
- Data da compra:
    
    - Informada manualmente ou
        
    - Data atual, por escolha do usuário
        

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
        

## Regras

- Compra detalhada → classificação por produto
    
- Compra resumida → classificação por local
    
- Classificação padrão inicial (hardcoded ou tabela base)
    
- Usuário pode:
    
    - Editar classificação
        
    - Corrigir erros
         
    - Estado anterior à alteração do usuário permance salva
        

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

# Relatórios (.xlsx)

## Características

- Gerado no primeiro registro de compra
    
- Atualizado a cada nova inserção
    
- Reflete apenas os dados ativos no banco
    

## Conteúdo

- Classificação da compra
    
- Valor total
    
- Data
    
- Detalhamento expansível por produtos (quando disponíveis)
    

> Relatórios não são fonte da verdade e podem ser regenerados a qualquer momento.

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
        
>A ausência de criptografia no MVP é uma decisão consciente para reduzir complexidade inicial, dado o caráter local e offline do sistema.

---

# Descobertas de Campo (A Fazer)

- Escaneamento real de QR codes
    
- Análise dos dados retornados
    
- Validação das suposições atuais

> Caso não exista identificador único confiável, o sistema **não garante deduplicação automática**.  
  Cabe ao usuário decidir se uma compra duplicada deve ser mantida ou removida.

---

# Stack

- C++
    
- SQLite
    

(UI e mobile serão tratados em fase posterior)