# Visão Geral

Sistema local para registro, classificação e análise de gastos pessoais a partir de fontes reais, incluindo **QR-codes**, **documentos fiscais digitais (ex: NFC-e, NF-e e PDF)** e **inserções manuais**, com **SQLite como fonte da verdade** e **relatórios em Excel (.xlsx)** como camada de visualização inicial.

O projeto prioriza **engenharia, modelagem de domínio e decisões explícitas**, não dependência de UI ou linguagem específica.

---

# Objetivos

- Registrar compras a partir de fontes fiscais reais (QR codes, documentos digitais e inserções manuais)
    
- Classificar gastos automaticamente (com possibilidade de edição pelo usuário)
    
- Armazenar dados de forma consistente e auditável
    
- Gerar relatórios em Excel espelhando o estado atual do banco
    
- Manter histórico controlado por janela temporal configurável
    

---

# Escopo Funcional

## Origens de Entrada

### 1. QR Code

- Origem assistida
- Utilizado como meio de acesso a documentos fiscais oficiais
- Redireciona, em geral, para portais governamentais (ex: SEFAZ), podendo exigir validações adicionais (captcha)
- Pode resultar na visualização da nota fiscal em HTML ou no download de documento digital (ex: PDF)

Dados obtidos indiretamente a partir do documento fiscal acessado:
- Data da compra
- Local / emissor
- Itens (quando disponíveis)
- Valor total
- Identificadores fiscais (quando presentes)

> QR codes fiscais **não fornecem dados estruturados diretamente ao sistema**.
> Eles atuam como ponte para obtenção de documentos fiscais confiáveis, cujo conteúdo é interpretado pelo usuário no MVP.

### 2. Inserção Manual

- Origem controlada pelo usuário
    
- Modos possíveis:
    
    - Inserção detalhada (produtos + valores)
        
    - Inserção resumida (local + valor total)
        
- Data da compra:
    
    - Informada manualmente ou
        
    - Data atual, por escolha do usuário

### 3. Documento Fiscal Digital (PDF / HTML)

- Origem assistida (não automatizada no MVP)
- Documento serve como referência confiável de dados
- Extração de informações é realizada manualmente pelo usuário
- Pode conter:
    - Identificadores fiscais (número da nota, código de verificação, município emissor)
    - Dados sensíveis do tomador
- O sistema pode registrar metadados do documento para rastreabilidade

> A importação automática de documentos fiscais **não faz parte do MVP** e é considerada uma evolução futura.
> Documentos fiscais digitais representam atualmente a fonte mais confiável de dados detalhados no domínio do projeto.

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

- Documentos fiscais podem conter dados pessoais identificáveis (ex: CPF, nome)
        
>A ausência de criptografia no MVP é uma decisão consciente para reduzir complexidade inicial, dado o caráter local e offline do sistema.

---

# Descobertas de Campo (A Fazer)

- Escaneamento real de QR codes e análise do fluxo governamental (redirecionamento, captcha, acesso à NFC-e)
    
- Análise dos dados retornados
    
- Validação das suposições atuais

- Documentos fiscais digitais (ex: NFS-e em PDF) apresentam estrutura rica de dados,
  porém não são projetados para consumo automatizado direto
  
- QR codes fiscais atuam como identificadores de acesso, não como interfaces de ingestão automática

> Caso não exista identificador único confiável, o sistema **não garante deduplicação automática**.  
  Cabe ao usuário decidir se uma compra duplicada deve ser mantida ou removida.

---

# Stack

- C++
    
- SQLite
    

(UI e mobile serão tratados em fase posterior)