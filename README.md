# Modular Life Tracker Engine ⚡

Um ecossistema minimalista de produtividade e *Quantified Self* projetado para rodar com altíssima performance em aparelhos Android modestos ou antigos (como o Samsung Galaxy A12 com chip Helio P35).

Inspirado na **filosofia UNIX** e na **arquitetura de plugins do Neovim**, a Engine substitui a Home tradicional do Android por um chassi escuro (100% AMOLED Black), um feed dinâmico de cartões e uma linha de comando CLI rápida e reativa.

---

## 🎯 Arquitetura: Host + Plugins (Estilo Neovim)

Em vez de sobrecarregar o hardware com múltiplos processos e arquivos soltos em disco, a Engine funciona como um host ultraleve onde cada rastreador é um **Módulo** independente que implementa uma interface simples:

```
┌──────────────────────────────────────────────┐
│  ZONA 1: CONTEXTO (Relógio, Bateria, Modo)   │
├──────────────────────────────────────────────┤
│  ZONA 2: FEED DINÂMICO (LazyColumn)          │
│  - [Módulo Água]    Barra de Progresso (ml)  │
│  - [Módulo Treino]  Status da Série / Check  │
│  - [Módulo GitHub]  Streak & PRs             │
├──────────────────────────────────────────────┤
│  ZONA 3: TERMINAL CLI                        │
│  > a 500       (Adiciona 500ml de água)      │
│  > g check     (Conclui exercício na academia)│
│  > help        (Lista comandos disponíveis)  │
└──────────────────────────────────────────────┘
```

---

## 🧩 Como criar um novo Módulo em 3 passos

Adicionar um novo módulo (ex: para rastrear café, finanças, hábitos ou sono) é tão simples quanto criar um plugin do Neovim:

### 1. Implemente o contrato `EngineModule`

```kotlin
class CoffeeTrackerModule : EngineModule {
    override val id = "coffee"
    override val name = "Café"
    override val commandPrefix = "c"
    override val helpText = "c <qtd | reset> - Registra doses de cafeína"

    private var cups = 0
    private val _blockFlow = MutableStateFlow<BlockUiModel?>(render())
    override val blockFlow = _blockFlow.asStateFlow()

    private fun render() = BlockUiModel.Info(
        moduleId = id,
        title = "Cafeína Hoje",
        description = "$cups xícaras consumidas",
        tag = if (cups > 3) "ALERTA" else "OK"
    )

    override suspend fun executeCommand(args: String): CommandResult {
        cups += args.toIntOrNull() ?: 1
        _blockFlow.value = render()
        return CommandResult.Success("Total de café hoje: $cups xícaras")
    }
}
```

### 2. Registre na Engine (`MainActivity.kt`)

```kotlin
moduleRegistry.register(CoffeeTrackerModule())
```

Pronto! O bloco aparecerá na tela inicial e o comando `c` estará ativo no terminal.

---

## 🛠️ Stack Tecnológica

- **Linguagem:** Kotlin 2.0
- **UI:** Jetpack Compose (Material 3 Dark Theme puro, sem dependências infladas)
- **Design:** 100% Background Black `#000000` (economia de bateria e leitura limpa)
- **Launcher:** Configurado no `AndroidManifest.xml` como `android.intent.category.HOME`

---

## 🚀 Como Executar

1. Abra o projeto no **Android Studio Hedgehog** (ou superior).
2. Conecte seu dispositivo Android com depuração USB ativada (ex: Samsung A12).
3. Execute o app (`Run 'app'`).
4. Pressione o botão Home no celular e selecione o **Life Tracker Engine** como seu inicializador padrão.
