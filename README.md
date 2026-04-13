# 🎬 Gerador Plus

Aplicativo Android para geração de banners e vídeos promocionais de IPTV.

---

## 📱 Funcionalidades

- **Sistema de Login Master/Usuário** com controle de licenças
- **Gerador de Banners** com 6 templates profissionais
- **Gerador de Vídeos** animados em MP4
- **Integração TMDB** — busca filmes e séries com pôster, sinopse, gênero e ano
- **Licença com prazo** — 30 dias (renovável) ou teste de 1 hora
- **Logo personalizada** com fundo transparente por usuário
- **Contato no rodapé** de cada banner/vídeo
- **Compartilhar ou salvar** banners diretamente da galeria

---

## 🔑 Configuração da Chave TMDB

1. Acesse [https://www.themoviedb.org/settings/api](https://www.themoviedb.org/settings/api)
2. Crie uma conta gratuita e solicite uma API Key
3. Abra o arquivo `app/build.gradle`
4. Substitua `SUA_CHAVE_TMDB_AQUI` pela sua chave:

```groovy
buildConfigField "String", "TMDB_API_KEY", "\"cole_sua_chave_aqui\""
```

---

## 🚀 Como usar no GitHub / Android Studio

### 1. Clone ou crie o repositório no GitHub

```bash
git init
git add .
git commit -m "Gerador Plus - versão inicial"
git remote add origin https://github.com/SEU_USUARIO/geradorplus.git
git push -u origin main
```

### 2. Abra no Android Studio

- File → Open → selecione a pasta `GeradorPlus`
- Aguarde o Gradle sync
- Conecte um celular Android (API 24+) ou use um emulador
- Clique em ▶ Run

---

## 📂 Estrutura de Arquivos

```
GeradorPlus/
├── app/
│   ├── build.gradle                          ← dependências e chave TMDB
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/geradorplus/
│       │   ├── GeradorPlusApp.kt             ← Application (Hilt)
│       │   ├── data/
│       │   │   ├── models/Models.kt          ← User, Banner, TMDB models
│       │   │   ├── database/AppDatabase.kt   ← Room DB + DAOs
│       │   │   ├── api/TmdbApiService.kt     ← Retrofit TMDB
│       │   │   └── repository/
│       │   │       ├── UserRepository.kt
│       │   │       ├── BannerRepository.kt
│       │   │       └── TmdbRepository.kt
│       │   ├── di/AppModule.kt               ← Hilt DI
│       │   ├── utils/
│       │   │   ├── SessionManager.kt         ← Login persistente
│       │   │   └── BannerGenerator.kt        ← Canvas rendering
│       │   └── ui/
│       │       ├── activities/
│       │       │   ├── SplashActivity.kt
│       │       │   ├── LoginActivity.kt
│       │       │   ├── MainActivity.kt       ← App de usuário
│       │       │   ├── AdminActivity.kt      ← App de master
│       │       │   ├── BannerEditorActivity.kt
│       │       │   └── VideoEditorActivity.kt
│       │       ├── fragments/Fragments.kt    ← Home, Banners, Search, Profile, Users, Dashboard
│       │       ├── adapters/Adapters.kt      ← RecyclerView adapters
│       │       └── viewmodels/ViewModels.kt  ← Auth, Admin, Banner, Profile
│       └── res/
│           ├── layout/                       ← Todos os XMLs de tela
│           ├── drawable/                     ← Ícones vetoriais + fundos
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   ├── themes.xml
│           │   └── dimens.xml
│           ├── menu/                         ← Menus de navegação
│           └── xml/                          ← file_paths, network_security, backup
├── build.gradle
├── settings.gradle
├── gradle.properties
└── .gitignore
```

---

## 🎨 Templates de Banner

| Template | Descrição |
|----------|-----------|
| **Cinema Dark** | Fundo escuro, pôster em destaque, estilo cinema |
| **Neon Glow** | Efeito neon vibrante, fundo roxo/preto |
| **Minimal Elegant** | Fundo branco, design limpo e moderno |
| **Explosive Action** | Imagem de fundo total, texto impactante |
| **Series Binge** | Tema azul navy, ideal para séries |
| **Promotion Sale** | Vermelho intenso, foco em promoção |

---

## 👤 Sistema de Usuários

### Login Master
- Criado no primeiro acesso ao app
- Acessa o painel Admin completo
- Pode criar outros logins Master e logins de Usuário
- Pode criar, renovar e suspender licenças

### Login Usuário
- Criado pelo Master
- Acessa apenas a área de criação de banners
- Licença com prazo (30 dias padrão ou 1h de teste)
- Possui logo e contato personalizados (aparece em todos os banners)

### Licença de Teste
- Duração: 1 hora
- Após vencer, exibe mensagem de expiração no login
- Master pode renovar convertendo para licença mensal

---

## 🛠️ Tecnologias

- **Kotlin** + Coroutines
- **Hilt** — injeção de dependência
- **Room** — banco de dados local
- **Retrofit** — requisições TMDB
- **Glide** — carregamento de imagens
- **EncryptedSharedPreferences** — sessão segura
- **MediaCodec/MediaMuxer** — geração de vídeo MP4
- **Canvas API** — renderização de banners

---

## ⚠️ Importante

- A chave TMDB **nunca deve ser comitada em texto** em repositórios públicos.
  Para projetos públicos, use variáveis de ambiente ou `local.properties`:
  ```
  tmdb_api_key=SUA_CHAVE
  ```
  E no `build.gradle`:
  ```groovy
  def tmdbKey = project.findProperty("tmdb_api_key") ?: ""
  buildConfigField "String", "TMDB_API_KEY", "\"${tmdbKey}\""
  ```

---

## 📄 Licença

Projeto privado — todos os direitos reservados.
