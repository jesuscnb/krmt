# RemoteFamily — Arquitetura, Roadmap e Instruções de Desenvolvimento

> Documento mestre para desenvolvimento assistido por Codex.
>
> **Objetivo:** construir um aplicativo Android simples de suporte remoto para uso familiar, com **um único APK capaz de controlar ou ser controlado**, utilizando um servidor próprio para descoberta, autenticação e signaling.
>
> **Princípio principal:** o usuário que recebe suporte deve realizar o menor número possível de ações. A interface deve ser simples, direta, com botões grandes e sem opções técnicas desnecessárias.

---

## 1. Visão do projeto

O **RemoteFamily** é uma ferramenta privada de suporte remoto destinada a poucos dispositivos de familiares autorizados.

Não é um produto comercial e não deve tentar competir em funcionalidades com AnyDesk, TeamViewer ou RustDesk.

O MVP precisa resolver apenas o fluxo essencial:

```text
Controlador
    │
    │ escolhe dispositivo
    ▼
Servidor
    │
    │ solicita sessão
    ▼
Dispositivo remoto
    │
    │ usuário permite
    │ usuário confirma MediaProjection
    ▼
WebRTC
    │
    ├── vídeo da tela
    └── comandos de controle
```

O mesmo APK pode assumir dois papéis diferentes dependendo da sessão:

```text
RemoteFamily.apk
       │
       ├── CONTROLLER
       │     controla outro aparelho
       │
       └── HOST
             recebe suporte
```

Não devem existir APKs separados para controlador e controlado.

---

# 2. Restrições obrigatórias

Estas regras devem ser consideradas requisitos de arquitetura.

## 2.1 Desenvolvimento

A workstation do desenvolvedor **não terá ambiente Android instalado**.

Não depender de:

- Android Studio;
- Android SDK local;
- Android NDK local;
- emulador Android;
- Gradle instalado globalmente;
- Kotlin instalado globalmente.

A workstation precisa apenas de:

```text
Git
Editor / Codex
Navegador
```

Todo build Android deve ser realizado pelo **GitHub Actions**.

O projeto deve conter o **Gradle Wrapper** versionado no repositório.

---

## 2.2 Interface

A interface não precisa ser visualmente sofisticada.

Prioridades:

1. clareza;
2. botões grandes;
3. textos curtos;
4. poucas opções;
5. nenhuma configuração técnica na interface normal;
6. menor quantidade possível de ações do familiar.

Não adicionar dashboards, animações, menus complexos, temas elaborados ou elementos puramente decorativos no MVP.

---

## 2.3 Segurança e consentimento

O projeto **não deve tentar esconder o acesso remoto**.

Obrigatório:

- mostrar que uma sessão remota está ativa;
- oferecer botão claro para encerrar;
- respeitar o consentimento do Android para `MediaProjection`;
- não tentar reutilizar uma autorização de captura entre sessões;
- não tentar contornar `FLAG_SECURE`;
- não tentar esconder foreground services;
- não remover notificações obrigatórias;
- não tentar burlar permissões do sistema;
- não aceitar comandos remotos fora de uma sessão autenticada.

Android 14+ exige consentimento do usuário para cada nova sessão de `MediaProjection`.

Esse comportamento deve ser tratado como requisito do sistema operacional, e não como algo a ser contornado.

---

# 3. Arquitetura geral

```text
                         INTERNET
                            │
                            │ HTTPS / WSS
                            ▼
                ┌─────────────────────────┐
                │       VPS / SERVER      │
                │                         │
                │  Traefik                │
                │  ├─ TLS / HTTPS         │
                │  └─ Reverse Proxy       │
                │                         │
                │  RemoteFamily Server    │
                │  Java + Javalin         │
                │  ├─ Autenticação        │
                │  ├─ Dispositivos        │
                │  ├─ Sessões             │
                │  ├─ WebSocket signaling │
                │  └─ Credenciais TURN    │
                │                         │
                │  SQLite                 │
                │                         │
                │  coturn                 │
                │  ├─ STUN                │
                │  └─ TURN                │
                └───────────┬─────────────┘
                            │
                   signaling│
                   WSS      │
            ┌───────────────┴───────────────┐
            │                               │
            ▼                               ▼
┌────────────────────────┐       ┌────────────────────────┐
│       ANDROID A        │       │       ANDROID B        │
│                        │       │                        │
│ RemoteFamily.apk       │       │ RemoteFamily.apk       │
│                        │       │                        │
│ Controller             │       │ Host                   │
│ nesta sessão           │       │ nesta sessão           │
└────────────┬───────────┘       └────────────┬───────────┘
             │                                │
             └──────────── WebRTC ────────────┘
                     vídeo + comandos
```

O servidor participa da descoberta e negociação.

Sempre que possível, o tráfego pesado deve ser P2P:

```text
Android A ═════════════════════════ Android B
                     WebRTC
```

Se a conexão direta falhar:

```text
Android A
    │
    ▼
  coturn
    │
    ▼
Android B
```

---

# 4. Stack tecnológica

## Android

- Kotlin;
- Jetpack Compose;
- Gradle Kotlin DSL;
- MediaProjection;
- VirtualDisplay;
- AccessibilityService;
- WebRTC;
- WebRTC DataChannel;
- HTTPS / WebSocket;
- Android Keystore quando necessário.

### Baseline inicial

No início do projeto, usar versões explicitamente fixadas e compatíveis.

Baseline validado em agosto de 2026:

```text
Android Gradle Plugin: 9.3.x
Gradle: 9.5+
JDK do build Android: 17
compileSdk: 36
targetSdk: 36
minSdk: 24
```

Antes de alterar qualquer versão principal, Codex deve:

1. consultar documentação oficial;
2. verificar matriz de compatibilidade;
3. alterar versões explicitamente;
4. executar CI;
5. somente depois manter a alteração.

Nunca usar versões dinâmicas como:

```text
9.3.+
latest.release
+
```

---

## Servidor

- Java 25;
- Javalin;
- WebSocket;
- REST somente onde fizer sentido;
- SQLite;
- HikariCP ou acesso simples compatível com SQLite;
- Jackson;
- SLF4J/Logback;
- Maven ou Gradle Java.

Preferência: **Maven** para o servidor, para manter a parte backend extremamente simples.

---

## Infraestrutura

- Linux VPS;
- Docker;
- Docker Compose;
- Traefik;
- Let's Encrypt;
- coturn.

Não adicionar Kubernetes, Redis, RabbitMQ, Kafka ou outros componentes no MVP.

---

# 5. Estrutura do monorepo

Estrutura desejada:

```text
remote-family/
│
├── AGENTS.md
├── README.md
├── REMOTEFAMILY_ROADMAP.md
│
├── android/
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/
│   │       ├── test/
│   │       └── androidTest/
│   │
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── gradle/
│       └── wrapper/
│
├── server/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       └── test/
│
├── infra/
│   ├── docker-compose.yml
│   ├── traefik/
│   └── coturn/
│
├── docs/
│   ├── architecture.md
│   ├── protocol.md
│   ├── security.md
│   └── testing.md
│
└── .github/
    └── workflows/
        ├── android-ci.yml
        ├── android-release.yml
        ├── server-ci.yml
        └── server-release.yml
```

---

# 6. Arquitetura interna do Android

Não criar uma arquitetura excessivamente abstrata.

Separar apenas responsabilidades reais.

Estrutura sugerida:

```text
android/app/src/main/java/.../remotefamily/
│
├── app/
│   ├── MainActivity.kt
│   └── RemoteFamilyApplication.kt
│
├── ui/
│   ├── home/
│   ├── setup/
│   ├── devices/
│   ├── request/
│   └── remote/
│
├── device/
│   ├── DeviceIdentity.kt
│   └── DeviceRepository.kt
│
├── server/
│   ├── ServerClient.kt
│   ├── SignalingClient.kt
│   └── ServerMessage.kt
│
├── session/
│   ├── RemoteSession.kt
│   ├── SessionState.kt
│   └── SessionManager.kt
│
├── webrtc/
│   ├── WebRtcManager.kt
│   ├── PeerConnectionFactoryProvider.kt
│   ├── ScreenVideoSource.kt
│   └── ControlDataChannel.kt
│
├── capture/
│   ├── ScreenCaptureManager.kt
│   └── MediaProjectionService.kt
│
├── control/
│   ├── RemoteAccessibilityService.kt
│   ├── RemoteCommand.kt
│   └── GestureExecutor.kt
│
└── security/
    ├── TokenStore.kt
    └── DeviceKeyStore.kt
```

Regra:

> Cada classe deve ter uma responsabilidade clara. Não criar uma `RemoteManager.kt` gigantesca que implemente captura, signaling, sessão e controle ao mesmo tempo.

---

# 7. Estados de sessão

Utilizar uma máquina de estados simples.

```text
IDLE
  │
  ▼
REQUESTED
  │
  ├── RECUSAR ───────────────► CLOSED
  │
  ▼
ACCEPTED
  │
  ▼
AWAITING_SCREEN_PERMISSION
  │
  ▼
CONNECTING
  │
  ▼
CONNECTED
  │
  ▼
CLOSED
```

Representação sugerida:

```kotlin
enum class SessionState {
    IDLE,
    REQUESTED,
    ACCEPTED,
    AWAITING_SCREEN_PERMISSION,
    CONNECTING,
    CONNECTED,
    CLOSED,
    FAILED
}
```

Nenhum comando remoto pode ser executado se a sessão não estiver em:

```text
CONNECTED
```

---

# 8. Fluxo UX

## 8.1 Primeira instalação

A configuração inicial deve ser semelhante a:

```text
Instalar APK
     │
     ▼
Abrir
     │
     ▼
Registrar dispositivo automaticamente
     │
     ▼
Ativar AccessibilityService
     │
     ▼
Permitir notificações
     │
     ▼
Pronto
```

Não pedir ao familiar:

- IP;
- hostname;
- porta;
- servidor;
- STUN;
- TURN;
- token;
- código técnico;
- codec;
- resolução;
- FPS.

O endereço do servidor deve estar configurado no build.

---

## 8.2 Tela principal

Exemplo:

```text
┌──────────────────────────────┐
│        REMOTE FAMILY         │
│                              │
│              ✓               │
│                              │
│     PRONTO PARA SUPORTE      │
│                              │
│ Dispositivo conectado.       │
│                              │
│ Você será avisado quando     │
│ houver uma solicitação.      │
│                              │
│ ┌──────────────────────────┐ │
│ │ CONTROLAR OUTRO APARELHO │ │
│ └──────────────────────────┘ │
│                              │
│ ⚙ Configurações             │
└──────────────────────────────┘
```

---

## 8.3 Solicitação

```text
┌──────────────────────────────┐
│ SOLICITAÇÃO DE SUPORTE       │
│                              │
│ Adriano quer acessar         │
│ este aparelho.               │
│                              │
│ ┌──────────────────────────┐ │
│ │         PERMITIR         │ │
│ └──────────────────────────┘ │
│                              │
│          RECUSAR             │
└──────────────────────────────┘
```

Depois de `PERMITIR`, abrir imediatamente o diálogo oficial do Android para MediaProjection.

Meta de UX depois da configuração inicial:

```text
1. PERMITIR
2. INICIAR AGORA
```

Essas devem ser, idealmente, as únicas ações do familiar por atendimento.

---

# 9. Tela do controlador

Lista simples:

```text
┌──────────────────────────────┐
│ CONTROLAR DISPOSITIVO        │
│                              │
│ ● Celular Mãe                │
│   Samsung A55                │
│                              │
│       [ ACESSAR ]            │
│                              │
│ ● Celular Pai                │
│   Moto G84                   │
│                              │
│       [ ACESSAR ]            │
│                              │
│ ○ TV Sala                    │
│   Offline                    │
└──────────────────────────────┘
```

Durante uma sessão:

```text
┌──────────────────────────────┐
│ Celular Mãe          ● ONLINE│
├──────────────────────────────┤
│                              │
│                              │
│         TELA REMOTA          │
│                              │
│                              │
├──────────────────────────────┤
│   ◀      ●      □      ⌨    │
│                              │
│          ENCERRAR            │
└──────────────────────────────┘
```

Comandos inicialmente suportados:

- TAP;
- LONG_PRESS;
- SWIPE;
- BACK;
- HOME;
- RECENTS.

Teclado remoto deve ser implementado somente após o controle básico estar estável.

---

# 10. Identidade do dispositivo

Na primeira execução:

1. gerar `deviceId` aleatório;
2. gerar segredo/chave do dispositivo;
3. salvar localmente;
4. registrar no servidor;
5. receber token de dispositivo;
6. manter credenciais de forma segura.

Não usar:

- IMEI;
- serial de hardware;
- número do telefone;
- advertising ID;
- identificadores invasivos.

Exemplo conceitual:

```json
{
  "deviceId": "uuid",
  "name": "SM-A556E",
  "manufacturer": "Samsung",
  "model": "SM-A556E",
  "appVersion": "0.2.0"
}
```

O nome amigável pode ser alterado posteriormente pelo administrador.

Exemplo:

```text
SM-A556E
    ↓
Celular Mãe
```

---

# 11. Servidor

Responsabilidades:

```text
RemoteFamily Server
│
├── autenticação
├── registro de dispositivos
├── lista de dispositivos
├── estado online/offline
├── solicitação de sessão
├── autorização da sessão
├── signaling WebRTC
├── emissão de credenciais TURN temporárias
└── encerramento da sessão
```

Não transmitir vídeo pelo Javalin.

Não implementar proxy de vídeo por WebSocket.

---

# 12. Modelo de dados inicial

SQLite.

Tabelas mínimas:

```text
users
devices
device_authorizations
sessions
```

Exemplo conceitual:

```sql
CREATE TABLE devices (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    manufacturer TEXT,
    model TEXT,
    app_version TEXT,
    owner_user_id TEXT,
    created_at TEXT NOT NULL,
    last_seen_at TEXT
);
```

Sessões:

```sql
CREATE TABLE sessions (
    id TEXT PRIMARY KEY,
    controller_device_id TEXT NOT NULL,
    host_device_id TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    accepted_at TEXT,
    closed_at TEXT
);
```

Não persistir SDP ou ICE candidates após a sessão.

---

# 13. Protocolo de signaling

Preferência por mensagens JSON pequenas.

Envelope:

```json
{
  "type": "MESSAGE_TYPE",
  "requestId": "uuid",
  "sessionId": "uuid",
  "payload": {}
}
```

Tipos iniciais:

```text
AUTH
AUTH_OK
AUTH_ERROR

DEVICE_ONLINE
DEVICE_OFFLINE
DEVICE_LIST
DEVICE_LIST_UPDATE

REQUEST_SESSION
SESSION_REQUESTED
SESSION_ACCEPT
SESSION_REJECT
SESSION_CANCEL
SESSION_CLOSE
SESSION_ERROR

WEBRTC_OFFER
WEBRTC_ANSWER
ICE_CANDIDATE

TURN_CREDENTIALS

PING
PONG
```

Documentar os schemas definitivos em:

```text
docs/protocol.md
```

---

# 14. WebRTC

WebRTC será responsável por:

- transporte de vídeo;
- negociação de mídia;
- NAT traversal;
- DTLS/SRTP;
- DataChannel.

Fluxo:

```text
HOST
MediaProjection
      │
      ▼
VirtualDisplay
      │
      ▼
VideoSource
      │
      ▼
VideoTrack
      │
      ▼
PeerConnection
      ║
      ║ WebRTC
      ║
      ▼
CONTROLLER
VideoTrack
      │
      ▼
Renderer
```

Começar sem áudio.

Baseline de vídeo:

```text
720p
15 FPS
bitrate adaptativo
```

Não tentar otimizar qualidade antes de obter estabilidade.

---

# 15. DataChannel de controle

Criar um DataChannel:

```text
control
```

Mensagens:

```json
{
  "type": "POINTER_TAP",
  "x": 0.42,
  "y": 0.71
}
```

Coordenadas devem ser normalizadas entre:

```text
0.0 e 1.0
```

Nunca enviar coordenadas absolutas do aparelho controlador.

Swipe:

```json
{
  "type": "POINTER_SWIPE",
  "fromX": 0.5,
  "fromY": 0.8,
  "toX": 0.5,
  "toY": 0.2,
  "durationMs": 350
}
```

Comandos de sistema:

```json
{
  "type": "SYSTEM_BACK"
}
```

```json
{
  "type": "SYSTEM_HOME"
}
```

```json
{
  "type": "SYSTEM_RECENTS"
}
```

Todo payload deve possuir validação.

Valores de coordenada fora de `0..1` devem ser rejeitados.

---

# 16. AccessibilityService

Responsável somente por executar comandos de controle.

Implementar:

```text
tap
long press
swipe
back
home
recents
```

Usar `dispatchGesture()` para gestos.

Declarar explicitamente a capacidade necessária para realizar gestos.

O serviço só deve aceitar comandos provenientes do `SessionManager` quando houver sessão autenticada em estado `CONNECTED`.

Não abrir socket diretamente dentro do AccessibilityService.

Fluxo correto:

```text
WebRTC DataChannel
       │
       ▼
ControlDataChannel
       │
       ▼
SessionManager
       │
       ▼
GestureExecutor
       │
       ▼
AccessibilityService
```

---

# 17. MediaProjection

Responsabilidade:

```text
capturar tela
    ↓
produzir VideoTrack
    ↓
WebRTC
```

Regras:

- pedir consentimento em cada nova sessão;
- nunca armazenar um `MediaProjection` para reutilizar depois;
- registrar callback de parada;
- liberar `VirtualDisplay`;
- liberar Surface;
- liberar tracks;
- encerrar sessão quando captura for interrompida;
- tratar mudança de orientação sem iniciar nova sessão de captura.

Quando a resolução mudar:

```text
VirtualDisplay.resize(...)
VirtualDisplay.setSurface(...)
```

Evitar recriar uma nova captura apenas por rotação.

---

# 18. Foreground Service

Durante captura ativa haverá Foreground Service visível.

A notificação deve ser clara.

Exemplo:

```text
RemoteFamily
Suporte remoto em andamento
[ ENCERRAR ]
```

Não tentar esconder a notificação.

Para disponibilidade em background, implementar somente mecanismos permitidos pela versão alvo do Android.

Não assumir que um processo pode permanecer vivo indefinidamente.

O servidor e o aplicativo devem tolerar:

- processo morto;
- perda de rede;
- suspensão;
- reinício do aplicativo;
- reconnect.

---

# 19. Reconexão

O signaling deve possuir reconnect com backoff.

Exemplo:

```text
1 s
2 s
5 s
10 s
30 s
60 s
```

Depois manter máximo de aproximadamente 60 segundos entre tentativas.

Ao reconectar:

1. autenticar novamente;
2. informar presença;
3. atualizar `lastSeen`;
4. não restaurar automaticamente uma sessão de captura antiga;
5. se uma sessão estava ativa, tratá-la como interrompida.

---

# 20. TURN

Usar coturn.

Nunca colocar usuário e senha TURN permanentes dentro do APK.

O servidor deve gerar credenciais temporárias.

Fluxo:

```text
Android
   │
   │ solicita configuração ICE
   ▼
Javalin
   │
   ├── STUN URLs
   └── TURN credentials temporárias
   │
   ▼
Android
```

A duração das credenciais deve ser curta.

---

# 21. Infraestrutura Docker

Estrutura inicial:

```yaml
services:

  traefik:
    image: traefik:<versao-fixada>

  remote-family-server:
    image: ghcr.io/<owner>/remote-family-server:<versao>

  coturn:
    image: coturn/coturn:<versao-fixada>
```

Volumes:

```text
server-data
traefik-data
```

Portas necessárias devem ser documentadas.

Evitar expor diretamente a porta interna do Javalin se Traefik estiver na frente.

---

# 22. GitHub Actions

A CI é parte essencial do projeto.

## Regra

Um commit que quebre o build Android não pode ser considerado concluído.

---

## 22.1 android-ci.yml

Executar em:

```text
pull_request
push para main
```

Fluxo:

```text
checkout
    ↓
JDK 17
    ↓
setup-gradle
    ↓
chmod +x gradlew
    ↓
./gradlew test
    ↓
./gradlew lint
    ↓
./gradlew assembleDebug
    ↓
upload APK artifact
```

Exemplo base:

```yaml
name: Android CI

on:
  push:
    branches: [main]
    paths:
      - "android/**"
      - ".github/workflows/android-ci.yml"

  pull_request:
    paths:
      - "android/**"
      - ".github/workflows/android-ci.yml"

jobs:
  build:
    runs-on: ubuntu-latest

    defaults:
      run:
        working-directory: android

    steps:
      - name: Checkout
        uses: actions/checkout@v6

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Make Gradle executable
        run: chmod +x gradlew

      - name: Test
        run: ./gradlew test

      - name: Lint
        run: ./gradlew lint

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: remote-family-debug
          path: android/app/build/outputs/apk/debug/*.apk
```

Ao implementar, preferir fixar GitHub Actions em SHA ou em versões principais cuidadosamente controladas.

---

# 23. Release Android

Tags:

```text
v0.1.0
v0.2.0
v0.3.0
...
```

A tag deve disparar:

```text
testes
   ↓
lint
   ↓
assembleRelease
   ↓
assinatura
   ↓
GitHub Release
   ↓
APK
```

Nome esperado:

```text
remote-family-0.6.0.apk
```

---

# 24. Assinatura do APK

Criar um keystore definitivo.

Armazenar no GitHub Secrets:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

O workflow:

1. recria o `.jks`;
2. executa build;
3. assina;
4. publica APK;
5. remove arquivo temporário.

Também manter backup criptografado do keystore fora do GitHub.

**Não perder o keystore.**

A mesma chave deve assinar todas as versões futuras para permitir atualização normal do APK instalado.

---

# 25. Server CI

`server-ci.yml`:

```text
checkout
   ↓
setup Java 25
   ↓
mvn test
   ↓
mvn package
   ↓
docker build
```

Executar em:

```text
push
pull_request
```

---

# 26. Server release

Quando uma release for criada:

```text
mvn test
   ↓
docker build
   ↓
push GHCR
```

Imagem:

```text
ghcr.io/<owner>/remote-family-server:<version>
```

Deploy pode inicialmente ser manual:

```bash
docker compose pull
docker compose up -d
```

Automatizar SSH/deploy somente depois que o MVP estiver funcional.

---

# 27. Roadmap

---

## Milestone 0.1.0 — APK criado pelo GitHub

### Objetivo

Provar que todo desenvolvimento Android pode ocorrer sem SDK local.

### Implementar

- estrutura Gradle;
- app Kotlin;
- Compose;
- tela "RemoteFamily";
- Gradle Wrapper;
- Android CI;
- geração do APK;
- upload como artifact.

### Critério de aceite

Um `git push` deve resultar em:

```text
GitHub Actions
    ↓
BUILD SUCCESS
    ↓
remote-family-debug.apk
```

O APK deve instalar e abrir em um telefone real.

### Não implementar ainda

- WebRTC;
- MediaProjection;
- Accessibility;
- servidor.

---

## Milestone 0.2.0 — Servidor e registro de dispositivo

### Objetivo

Fazer o APK registrar-se no servidor e permanecer identificável.

### Implementar servidor

- projeto Java 25;
- Javalin;
- `/health`;
- WebSocket;
- SQLite;
- tabela `devices`;
- autenticação de dispositivo;
- presença online.

### Implementar Android

- gerar `deviceId`;
- registrar dispositivo;
- salvar token;
- abrir signaling WebSocket;
- mostrar status:

```text
● Conectado
```

ou:

```text
○ Desconectado
```

### Critério de aceite

Dois APKs instalados devem aparecer registrados no servidor.

---

## Milestone 0.3.0 — Controle local

### Objetivo

Provar que o aparelho consegue executar gestos.

### Implementar

- AccessibilityService;
- configuração XML;
- permissão/configuração;
- GestureExecutor;
- TAP;
- SWIPE;
- BACK;
- HOME;
- RECENTS;
- tela de diagnóstico temporária.

### Teste

A tela de diagnóstico manda executar um gesto local.

### Critério de aceite

O app consegue executar toque e swipe através do AccessibilityService depois da autorização do usuário.

Depois desta fase, remover ou esconder a tela de diagnóstico da interface normal.

---

## Milestone 0.4.0 — Captura de tela local

### Objetivo

Provar MediaProjection separadamente de WebRTC.

### Implementar

- solicitação de autorização;
- foreground service;
- MediaProjection;
- VirtualDisplay;
- callback de parada;
- lifecycle correto.

### Critério de aceite

O aplicativo consegue iniciar e encerrar uma sessão de captura sem crash e liberar corretamente os recursos.

---

## Milestone 0.5.0 — WebRTC vídeo Android → Android

### Objetivo

Um aparelho visualizar a tela do outro.

### Implementar

- dependência WebRTC;
- PeerConnectionFactory;
- VideoTrack;
- signaling SDP;
- ICE candidates;
- render remoto;
- MediaProjection → WebRTC.

### Inicialmente testar

```text
mesma rede Wi-Fi
```

Depois:

```text
redes diferentes
```

### Critério de aceite

Android A mostra em tempo real a tela de Android B.

Meta inicial:

```text
720p
15 FPS
```

---

## Milestone 0.6.0 — Controle remoto

### Objetivo

Integrar vídeo e gestos.

### Implementar

- DataChannel;
- protocolo de comandos;
- coordenadas normalizadas;
- TAP;
- LONG_PRESS;
- SWIPE;
- BACK;
- HOME;
- RECENTS.

### Critério de aceite

No Android A:

```text
tocar na imagem remota
```

deve produzir:

```text
toque equivalente no Android B
```

---

## Milestone 0.7.0 — TURN e funcionamento pela internet

### Objetivo

Funcionar mesmo quando conexão P2P direta não for possível.

### Implementar

- coturn;
- STUN;
- TURN;
- credenciais temporárias;
- ICE server configuration;
- métricas/logs básicos para saber se conexão foi P2P ou TURN.

### Critério de aceite

Conectar:

```text
telefone A: Wi-Fi residencial
telefone B: 4G/5G
```

e realizar vídeo + controle.

---

## Milestone 0.8.0 — UX familiar

### Objetivo

Reduzir ações do usuário remoto ao mínimo.

### Implementar

- onboarding simples;
- lista de permissões necessárias;
- botão grande para ativar Accessibility;
- status "Pronto para suporte";
- solicitação em tela;
- botão grande `PERMITIR`;
- abertura imediata da MediaProjection;
- botão claro `ENCERRAR`.

### Critério

Depois da configuração inicial:

```text
1. PERMITIR
2. INICIAR AGORA
```

deve ser suficiente para receber suporte.

---

## Milestone 0.9.0 — Administração e dispositivos

### Objetivo

Permitir que somente administradores iniciem sessões.

### Implementar

- usuário administrador;
- login;
- relação usuário/dispositivo;
- lista de dispositivos;
- renomear dispositivo;
- online/offline;
- solicitar acesso.

### Critério de aceite

Um dispositivo comum não pode iniciar acesso a outro sem autorização administrativa.

---

## Milestone 1.0.0 — Release familiar

### Objetivo

Versão estável para uso real.

### Requisitos

- APK release assinado;
- GitHub Release;
- servidor Docker;
- TLS;
- TURN;
- autenticação;
- reconexão;
- logs;
- mensagens de erro simples;
- encerramento correto da sessão;
- teste em pelo menos dois fabricantes Android;
- teste via Wi-Fi;
- teste via rede móvel;
- documentação de instalação.

---

# 28. Ordem obrigatória de desenvolvimento

Não começar por interface.

A ordem deve ser:

```text
1. CI gera APK
2. App conecta no servidor
3. Accessibility executa gesto
4. MediaProjection captura tela
5. WebRTC transmite vídeo
6. DataChannel controla
7. TURN
8. Segurança/admin
9. UX final
10. Android TV
```

A regra principal:

> Provar primeiro as partes tecnicamente arriscadas.

As duas provas essenciais são:

```text
MediaProjection → WebRTC → outro Android
```

e:

```text
DataChannel → AccessibilityService → gesto
```

---

# 29. Android TV

Não faz parte do primeiro MVP.

Somente iniciar Android TV quando `1.0.0` em smartphone estiver estável.

O mesmo APK deve ser reaproveitado se possível.

Para TV, priorizar:

```text
DPAD_UP
DPAD_DOWN
DPAD_LEFT
DPAD_RIGHT
ENTER / OK
BACK
HOME
```

Não assumir que Samsung Tizen e LG webOS sejam Android.

Alvos inicialmente compatíveis:

```text
Android TV
Google TV
Android boxes
```

---

# 30. Tratamento de erros

Mensagens para o familiar devem ser humanas.

Não mostrar:

```text
ICE_CONNECTION_FAILED
HTTP 401
WebSocket close 1006
MediaProjection callback
TURN allocation failed
```

Mostrar:

```text
Não foi possível conectar.
Tente novamente.
```

Logs técnicos ficam no log do aplicativo/servidor.

Estados importantes:

- servidor indisponível;
- dispositivo offline;
- solicitação recusada;
- autorização de tela cancelada;
- Accessibility desabilitado;
- WebRTC falhou;
- TURN falhou;
- conexão perdida;
- host encerrou;
- controller encerrou.

---

# 31. Logs

Servidor deve registrar:

```text
timestamp
event
deviceId mascarado/parcial quando adequado
sessionId
connection mode
result
```

Não registrar:

- token;
- secret;
- conteúdo de tela;
- SDP completo em produção;
- credencial TURN;
- senha.

Eventos úteis:

```text
DEVICE_CONNECTED
DEVICE_DISCONNECTED
SESSION_REQUESTED
SESSION_ACCEPTED
SESSION_REJECTED
SESSION_CONNECTED
SESSION_CLOSED
WEBRTC_CONNECTED
WEBRTC_FAILED
TURN_USED
```

---

# 32. Segurança

## Obrigatório

- HTTPS;
- WSS;
- tokens de dispositivo;
- tokens de usuário;
- sessão com UUID criptograficamente aleatório;
- autorização controller → host;
- expiração de sessão;
- rate limiting de login;
- credenciais TURN temporárias;
- secrets apenas no servidor/GitHub Secrets;
- nenhuma chave administrativa hardcoded no APK.

## Sessão

Somente dispositivos pertencentes à sessão podem trocar signaling.

Exemplo:

```text
session 123

controller = device A
host       = device B
```

Device C não pode enviar:

```text
WEBRTC_OFFER
ICE_CANDIDATE
CONTROL
```

para essa sessão.

---

# 33. Testes

## Android unit tests

Cobrir:

- SessionState;
- transições de estado;
- parser do protocolo;
- validação de comandos;
- normalização de coordenadas;
- reconnect backoff;
- token repository.

Exemplo:

```text
x < 0       → rejeitar
x > 1       → rejeitar
y < 0       → rejeitar
y > 1       → rejeitar
```

---

## Server tests

Cobrir:

- autenticação;
- autorização;
- criação de sessão;
- aceite;
- rejeição;
- device offline;
- signaling somente entre participantes;
- expiração;
- TURN credentials;
- rate limiting.

---

## Teste físico

Algumas funcionalidades exigem dispositivo Android real:

- Accessibility;
- MediaProjection;
- comportamento de background;
- WebRTC;
- orientação;
- bateria;
- fabricantes.

Registrar testes manuais em:

```text
docs/testing.md
```

---

# 34. Política de commits

Commits pequenos.

Exemplos:

```text
chore(android): scaffold application
ci(android): build debug apk
feat(server): register device
feat(android): add accessibility service
feat(android): add screen capture
feat(webrtc): establish peer connection
feat(control): send tap over data channel
feat(turn): add coturn fallback
```

Não misturar várias milestones em um único commit gigante.

---

# 35. Política para o Codex

Ao iniciar uma sessão de Codex, este arquivo deve ser lido primeiro.

Também criar `AGENTS.md`.

Conteúdo conceitual obrigatório do `AGENTS.md`:

```text
1. Leia REMOTEFAMILY_ROADMAP.md antes de alterar o projeto.

2. Trabalhe apenas na milestone solicitada.

3. Não antecipe funcionalidades de milestones futuras sem necessidade técnica.

4. Não adicione dependências sem justificar.

5. Não instalar ou exigir Android SDK na workstation.

6. Todo build Android deve ser reproduzível no GitHub Actions.

7. Não declarar uma tarefa Android concluída se o GitHub Actions estiver falhando.

8. Interface simples:
   - botões grandes;
   - poucos elementos;
   - textos claros;
   - sem decoração desnecessária.

9. Não tentar contornar proteções do Android.

10. Não remover consentimento, foreground notification ou controles de segurança.

11. Antes de mudanças arquiteturais:
    - explicar;
    - documentar;
    - aguardar aprovação quando a mudança for relevante.

12. Escrever testes para regras e protocolo.

13. Nunca colocar secrets no repositório.

14. Não implementar sistema próprio de streaming por JPEG/WebSocket.
    Usar WebRTC.

15. Não transformar o servidor Javalin em relay de vídeo.
    TURN é responsabilidade do coturn.

16. Manter classes pequenas e focadas.

17. Não criar abstrações sem uso real.

18. Preferir soluções simples e explícitas.
```

---

# 36. Prompt inicial para o Codex

Usar algo semelhante:

```text
Leia primeiro o arquivo REMOTEFAMILY_ROADMAP.md inteiro.

Este arquivo é a especificação principal do projeto.

Também leia AGENTS.md e respeite suas regras.

Vamos desenvolver o RemoteFamily incrementalmente.

Não implemente o roadmap inteiro de uma vez.

Comece somente pela Milestone 0.1.0.

Objetivo da Milestone 0.1.0:
- criar o projeto Android;
- Kotlin + Jetpack Compose;
- Gradle Wrapper versionado;
- sem necessidade de Android SDK local;
- configurar GitHub Actions;
- executar testes/lint;
- gerar app-debug.apk como artifact;
- mostrar uma única tela simples com o texto "RemoteFamily".

Antes de implementar:
1. apresente os arquivos que pretende criar;
2. confirme as versões das dependências;
3. verifique compatibilidade entre AGP, Gradle e JDK;
4. implemente;
5. revise o código;
6. verifique o workflow;
7. não avance para a Milestone 0.2.0.

Considere a milestone concluída somente quando o GitHub Actions gerar o APK com sucesso.
```

---

# 37. Fluxo de trabalho com Codex

Para cada milestone:

```text
1. selecionar milestone
2. pedir plano curto ao Codex
3. Codex altera código
4. commit
5. push
6. GitHub Actions
7. corrigir erros
8. baixar APK quando aplicável
9. testar em Android real
10. registrar resultado
11. só então avançar
```

Não deixar Codex implementar várias milestones de uma vez.

Isso reduz drasticamente o risco de acumular código não testado.

---

# 38. Definition of Done

Uma milestone só está concluída quando:

- código compilando;
- testes passando;
- CI verde;
- nenhuma secret commitada;
- documentação relevante atualizada;
- teste físico realizado quando necessário;
- critério de aceite da milestone cumprido;
- nenhuma funcionalidade futura implementada sem necessidade.

---

# 39. O que NÃO fazer

Não implementar no MVP:

- áudio remoto;
- transferência de arquivos;
- chat;
- gravação de sessão;
- clipboard sincronizado;
- múltiplos monitores;
- gerenciamento de arquivos;
- shell remoto;
- terminal;
- câmera;
- microfone;
- automação silenciosa;
- bypass de tela bloqueada;
- bypass de FLAG_SECURE;
- Android TV antes do smartphone;
- Tizen;
- webOS;
- Windows;
- iOS;
- Kubernetes;
- Redis;
- Kafka;
- RabbitMQ.

Essas funcionalidades só podem ser discutidas depois da versão 1.0.

---

# 40. Critério de sucesso do projeto

O projeto estará cumprindo seu objetivo quando este cenário funcionar:

```text
ADRIANO                             FAMILIAR

abre RemoteFamily
      │
      ▼
"Controlar dispositivo"
      │
      ▼
"Celular Mãe"
      │
      ▼
[ ACESSAR ]
      │
      └────────────────────────────►

                              🔔 Solicitação

                               [ PERMITIR ]
                                    │
                                    ▼
                              Android solicita
                              compartilhamento
                                    │
                              [ INICIAR AGORA ]
                                    │
                                    ▼

          ◀════════════ WebRTC ════════════▶

     vê a tela                    tela transmitida

     toca na tela
          │
          └──── DataChannel ───────────────►
                                       Accessibility
                                            │
                                            ▼
                                      Android executa
                                          o toque
```

Após a configuração inicial, o familiar não deve precisar entender nenhuma tecnologia.

Para ele, a experiência precisa ser simplesmente:

```text
PERMITIR
   ↓
INICIAR AGORA
   ↓
SUPORTE
```

---

# 41. Decisões arquiteturais consolidadas

| Item | Decisão |
|---|---|
| Aplicativos Android | 1 APK |
| Papéis | Controller ou Host por sessão |
| Linguagem Android | Kotlin |
| UI | Jetpack Compose |
| Build Android local | Não |
| Build Android | GitHub Actions |
| Captura | MediaProjection |
| Controle | AccessibilityService |
| Streaming | WebRTC |
| Comandos | WebRTC DataChannel |
| Descoberta | Servidor próprio |
| Signaling | Javalin WebSocket |
| Backend | Java 25 + Javalin |
| Banco | SQLite |
| NAT traversal | STUN/TURN |
| TURN | coturn |
| Proxy/TLS | Traefik |
| Infra | Docker Compose |
| Áudio MVP | Não |
| Android TV MVP | Não |
| Interface sofisticada | Não |
| Consentimento por sessão | Sim |
| Uso comercial | Não é objetivo |

---

# 42. Prioridade técnica

Quando houver dúvida entre uma solução simples e uma solução arquiteturalmente sofisticada:

> escolher a solução simples, desde que seja segura e mantenha as fronteiras principais do projeto.

Ordem de prioridade:

```text
funcionar
  ↓
ser seguro
  ↓
ser estável
  ↓
ser simples de usar
  ↓
ser simples de manter
  ↓
otimizar
  ↓
embelezar
```

---

# 43. Referências técnicas principais

Consultar sempre documentação oficial antes de alterações importantes:

- Android MediaProjection:
  https://developer.android.com/media/grow/media-projection

- Android 14 MediaProjection behavior:
  https://developer.android.com/about/versions/14/behavior-changes-14

- AccessibilityService:
  https://developer.android.com/reference/android/accessibilityservice/AccessibilityService

- Foreground Services:
  https://developer.android.com/develop/background-work/services/fgs

- Android Gradle Plugin:
  https://developer.android.com/build/releases/about-agp

- GitHub Actions + Gradle:
  https://docs.github.com/actions/tutorials/build-and-test-code/java-with-gradle

- Javalin:
  https://javalin.io/documentation

- WebRTC:
  https://webrtc.org/

- coturn:
  https://github.com/coturn/coturn

---

# 44. Primeira tarefa

A primeira execução do Codex deve trabalhar exclusivamente em:

```text
Milestone 0.1.0
```

Não criar servidor antes do primeiro APK ser gerado corretamente pelo GitHub Actions.

Primeiro resultado concreto esperado do projeto:

```text
remote-family-debug.apk
```

gerado remotamente pelo GitHub Actions, instalável em um telefone Android real.

---

**Fim do documento mestre.**
