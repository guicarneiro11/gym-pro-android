# GymPro

Aplicativo Android nativo para gerenciamento de treinos de musculação com sincronização em tempo real.

## Tecnologias

### Stack Principal
- **Kotlin** - Linguagem principal
- **Jetpack Compose** - UI moderna com Material Design 3
- **Firebase**
  - Authentication (login/registro)
  - Cloud Firestore (banco de dados)
  - Cloud Storage (imagens)
- **MVVM + Clean Architecture**
- **Koin** - Injeção de dependência
- **Room** - Cache local para funcionamento offline
- **Coroutines + Flow** - Programação reativa

## Funcionalidades

### Treinos (CRUD Completo)
- ✅ Criar treino com nome e descrição
- ✅ Listar treinos do usuário
- ✅ Editar informações
- ✅ Excluir com confirmação
- ✅ Relação 1:N com exercícios

### Exercícios (CRUD Completo)
- ✅ Criar exercício com nome, observações e imagem
- ✅ Upload de imagem (câmera ou galeria)
- ✅ Suporte a GIFs
- ✅ Editar informações
- ✅ Excluir com swipe gesture
- ✅ Reordenação manual

### Diferenciais Implementados
- ✅ **Funcionamento 100% offline** com sincronização automática
- ✅ **Autenticação completa** com Firebase Auth
- ✅ **Internacionalização** (PT-BR, EN, ES)
- ✅ **Tema claro/escuro** com persistência
- ✅ **Testes** unitários e de integração

## Arquitetura

```
app/
├── data/           # Repositories, Room, Firebase
├── domain/         # Models e interfaces
├── presentation/   # UI (Compose) e ViewModels
└── di/             # Módulos Koin
```

### Padrão de Projeto
- **Repository Pattern** para abstração de dados
- **MVVM** para separação UI/Lógica
- **Clean Architecture** em 3 camadas
- **SOLID** principles aplicados

## Firebase Integration

### Firestore - Estrutura
```
workouts/
  └── {workoutId}
      ├── name: String
      ├── description: String
      ├── date: Timestamp
      └── userId: String

exercises/
  └── {exerciseId}
      ├── workoutId: String
      ├── name: String
      ├── imageUrl: String?
      ├── observations: String
      └── position: Int
```

### Storage
- Upload de imagens/GIFs dos exercícios
- Organização por userId
- Compressão automática

## Offline First

O app funciona **100% offline** através de:
- **Room Database** para cache local
- **NetworkMonitor** para detecção de conectividade
- **SyncManager** para sincronização inteligente
- Indicador visual de status de sincronização

## Testes

```bash
# Rodar testes unitários
./gradlew test

# Rodar testes instrumentados
./gradlew connectedAndroidTest
```

### Cobertura
- ViewModels (Login, Register, Workout, Exercise)
- Repositories com mocks
- Fluxo de autenticação
- CRUD operations

## Setup

### Opção 1: Usar configuração existente (Recomendado para teste rápido)
O projeto já inclui `google-services.json` configurado com ambiente de desenvolvimento pronto para uso.

### Opção 2: Configurar projeto próprio
1. Crie um novo projeto no [Firebase Console](https://console.firebase.google.com)
2. Adicione um app Android com package: `com.guicarneirodev.gympro`
3. Baixe o `google-services.json` e substitua em `/app`
4. Habilite:
   - Authentication (Email/Password)
   - Cloud Firestore
   - Cloud Storage
5. Configure as Security Rules conforme documentação

## 🎨 UI/UX

- **Material Design 3** com Material You
- **Compose** para UI declarativa
- Animações e transições suaves
- Loading states com Shimmer Effect
- Empty states ilustrados
- Swipe gestures para ações rápidas
- Pull-to-refresh
