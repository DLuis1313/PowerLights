# PowerGrid Lights Addon

Addon para [Create: Power Grid](https://github.com/patryk3211/PowerGrid) que adiciona novas
lâmpadas elétricas ao jogo.  
Plataforma: **NeoForge 1.21.1**

---

## 📦 Conteúdo atual

| Bloco | Descrição |
|-------|-----------|
| **Refletor Elétrico** (`floodlight`) | Projeta um cone de luz de até 16 blocos na direção que está apontando. Similar ao refletor do Immersive Engineering. |
| `floodlight_light` *(interno)* | Bloco de luz invisível colocado dinamicamente pelo refletor. Não está disponível no inventário. |

---

## ⚡ Especificações Elétricas do Refletor

| Parâmetro | Valor |
|-----------|-------|
| Tensão nominal | **120 V** |
| Resistência | **1440 Ω** |
| Potência (@120V) | **~10 W** |
| Tensão mínima | **30 V** (luz fraca) |
| Tensão máxima recomendada | **240 V** |
| Nível de luz máximo | **15** (a 120 V) |
| Alcance do cone | **16 blocos** |
| Largura do cone | **±4 blocos** na extremidade |

### Como conectar

O refletor tem dois terminais elétricos:
- **Terminal A** – na face **traseira** (oposta ao FACING). Conecte o fio positivo/quente aqui.
- **Terminal B** – na face **inferior**. Conecte o neutro/retorno aqui.

---

## 🔦 Comportamento da Iluminação

Quando energizado com tensão suficiente, o refletor:
1. Emite um brilho suave (nível 4) na própria face frontal.
2. Coloca **blocos de luz invisíveis** em uma grade cônica de até 16 blocos de distância.
3. O nível de luz diminui progressivamente da base do cone até a ponta.
4. Blocos sólidos no caminho **não são iluminados** (a luz para nas paredes).
5. Ao ser desligado ou quebrado, **todos os blocos de luz são removidos** automaticamente.

### Escalonamento de brilho com tensão

| Tensão | Nível de luz máx. |
|--------|-------------------|
| 30 V   | 1 |
| 60 V   | 8 |
| 120 V  | 15 |
| >120 V | 15 (sem dano) |

---

## ✨ Compatibilidade com Shine

O [Shine](https://modrinth.com/mod/shine) adiciona bloom visual às fontes de luz.  
A compatibilidade funciona através de **textura emissiva** no modelo do refletor:

- Crie o arquivo: `assets/powergridlights/textures/block/floodlight_lens_on_e.png`
- Este arquivo é uma versão do padrão da lente com áreas mais brilhantes em branco/amarelo
- O Shine detecta a sufixo `_e` automaticamente e aplica o bloom

> **Nota:** O Shine v1.0.0 não tem API Java pública para registro programático.
> Toda a integração é feita via textura emissiva no resource pack.

---

## 🔨 Crafting

```
I G I
G C G
I G I
```

- `I` = Iron Ingot
- `G` = Glass Pane
- `C` = Redstone (representando o filamento/circuito)

*(Altere a receita em `data/powergridlights/recipe/floodlight.json` conforme desejado)*

---

## 🛠️ Como Compilar

### Pré-requisitos

- Java 21+
- Gradle 8.x (incluído no wrapper)
- NeoForge MDK para 1.21.1
- Jar do **Create: Power Grid** (NeoForge 1.21.1) disponível localmente

### Adicionando o PowerGrid como dependência local

Coloque o jar do PowerGrid na pasta `libs/` do projeto e ajuste o `build.gradle`:

```groovy
dependencies {
    implementation files('libs/powergrid-1.21.1-X.X.X-neoforge.jar')
}
```

### Build

```bash
./gradlew build
```

O jar ficará em `build/libs/`.

---

## 📝 O que precisa ser feito ainda

### Texturas (obrigatório para o mod parecer bonito)

Você precisa criar os seguintes arquivos PNG 16x16:

| Arquivo | Descrição |
|---------|-----------|
| `textures/block/floodlight_body.png` | Corpo metálico do refletor |
| `textures/block/floodlight_lens_off.png` | Lente escura (desligado) |
| `textures/block/floodlight_lens_on.png` | Lente brilhante (ligado) |
| `textures/block/floodlight_lens_on_e.png` | Textura emissiva para Shine (bloom) |

### Modelo 3D (recomendado)

O modelo atual em JSON usa uma caixa simples. Para parecer com o refletor do
Immersive Engineering, crie um modelo em BlockBench com:
- Cabeça cilíndrica / parabólica na frente (a lente)
- Suporte/haste atrás
- Cabo elétrico saindo do terminal

### API do PowerGrid

> ⚠️ **Atenção:** Os imports e métodos do PowerGrid no `FloodlightBlockEntity.java`
> foram escritos com base na API do branch `architectury-1.20.1/dev`.
> O **port NeoForge 1.21.1** pode ter nomes de pacotes/métodos diferentes.
>
> Você precisa:
> 1. Obter o jar do PowerGrid NeoForge 1.21.1 (port unofficial)
> 2. Inspecionar a API pública (interfaces como `IElectricalBlockEntity`, `CircuitNode`, `ElectricNetwork`)
> 3. Ajustar os imports em `FloodlightBlockEntity.java` conforme necessário

### Creative Tab

Adicione o item a uma aba criativa (ou crie a sua própria):

```java
// No seu mod principal ou em uma classe separada
@SubscribeEvent
public static void buildContents(BuildCreativeModeTabContentsEvent event) {
    // Adicione à aba do PowerGrid se ela existir
    // event.accept(PGLItems.FLOODLIGHT);
}
```

### Futuras lâmpadas

A estrutura está pronta para adicionar mais lâmpadas. Para cada nova lâmpada:
1. Crie uma classe `XyzBlock extends BaseEntityBlock`
2. Crie `XyzBlockEntity` implementando `IElectricalBlockEntity`
3. Registre em `PGLBlocks` e `PGLBlockEntities`
4. Adicione texturas, modelos, blockstates e loot table

---

## 📄 Licença

MIT – use, modifique e distribua livremente com crédito.
