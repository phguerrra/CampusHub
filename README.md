# CampusHub 🎓

**CampusHub** é um aplicativo Android desenvolvido para ajudar alunos a encontrar, organizar, inscrever-se e favoritar eventos acadêmicos e universitários.

---

## 🚀 Funcionalidades Principal

### 🔐 Autenticação & Usuários
- **Criar Conta:** Cadastro com nome, e-mail e senha.
- **Login Seguro:** Autenticação via Firebase Auth com validação de dados.
- **Recuperação de Senha:** Envio de e-mail de redefinição de senha com um clique.
- **Perfil do Usuário:** Consulta do e-mail cadastrado e atualização do nome de exibição.

### 📅 Gestão de Eventos
- **Listagem de Eventos:** Exibição de eventos disponíveis ordenados por data.
- **Detalhes do Evento:** Consulta das informações completas (nome, data, horário, local, vagas e descrição).
- **Criação de Eventos:** Cadastro de novos eventos informando vagas e detalhes.

### 🎟️ Inscrições em Eventos
- **Inscrição:** Inscrição com atualização atômica de vagas no Cloud Firestore.
- **Cancelamento de Inscrição:** Possibilidade de desmarcar presença, liberando a vaga para outros alunos.
- **Filtro "Minhas Inscrições":** Aba dedicada para listar eventos em que o aluno está inscrito.

### ⭐ Favoritos
- **Favoritar/Desfavoritar:** Marque ou desmarque qualquer evento como favorito (independente de estar inscrito).
- **Filtro "Favoritos":** Aba dedicada para visualizar rapidamente seus eventos favoritados.

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java / Kotlin
- **SDK Android:** Min SDK 31 | Target SDK 37
- **Arquitetura & UI:** Material Design 3, RecyclerView, ConstraintLayout/LinearLayout, Vector Drawables
- **Backend & Database:** Google Firebase
  - **Firebase Authentication:** Autenticação por e-mail e senha.
  - **Cloud Firestore:** Banco de dados NoSQL em tempo real.
- **Build System:** Gradle (Kotlin DSL `.kts`)

---

## 🗄️ Estrutura do Banco de Dados (Cloud Firestore)

### Coleção `events`
| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `name` | String | Nome do evento |
| `description` | String | Descrição detalhada |
| `date` | Timestamp | Data do evento |
| `time` | String | Horário (ex: "19:00") |
| `location` | String | Local do evento |
| `availableSlots` | Number | Quantidade de vagas disponíveis |
| `createdBy` | String | UID do organizador/criador |
| `createdAt` | Timestamp | Data de criação no servidor |

### Coleção `subscriptions` (ID do Documento: `${eventId}_${userId}`)
| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `eventId` | String | ID do documento do evento |
| `userId` | String | UID do usuário inscrito |
| `subscribedAt` | Timestamp | Data e hora da inscrição |

### Coleção `favorites` (ID do Documento: `${eventId}_${userId}`)
| Campo | Tipo | Descrição |
| :--- | :--- | :--- |
| `eventId` | String | ID do documento do evento |
| `userId` | String | UID do usuário que favoritou |
| `favoritedAt` | Timestamp | Data e hora da inclusão nos favoritos |

---

## ⚙️ Configuração do Ambiente

1. **Clonar o Repositório:**
   ```bash
   git clone <URL_DO_REPOSITORIO>
   ```

2. **Configuração do Firebase:**
   - Acesse o [Firebase Console](https://console.firebase.google.com/).
   - Crie um projeto Firebase e adicione um aplicativo Android com o pacote `com.project.application`.
   - Baixe o arquivo `google-services.json` e coloque na pasta `app/` do projeto.
   - Ative o serviço **Authentication** (método E-mail/Senha).
   - Ative o **Firestore Database** e configure as regras de segurança:

   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

3. **Execução no Android Studio:**
   - Abra o projeto no **Android Studio**.
   - Sincronize o Gradle (`Sync Project with Gradle Files`).
   - Execute o projeto em um emulador ou dispositivo físico com **Android 12 (API 31)** ou superior.

---

## 📱 Telas do Aplicativo

- `MainActivity` - Tela de Login e Recuperação de Senha
- `RegisterActivity` - Tela de Cadastro de Usuário
- `ProfileActivity` - Tela de Edição do Perfil
- `EventsActivity` - Lista de Eventos com Alternância de Filtros (*Todos | Minhas Inscrições | Favoritos*)
- `EventDetailsActivity` - Detalhes do Evento com botões de Inscrição e Favorito
- `CreateEventActivity` - Formulário para Criar Novo Evento
