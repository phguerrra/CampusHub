# CampusHub 🎓

**CampusHub** is an Android application designed to help university students discover, organize, enroll in, comment on, and bookmark academic and campus events.

---

## 🚀 Key Features

### 🔐 Authentication & User Management
- **Create Account:** Register with name, email, and password.
- **Secure Login:** Firebase Authentication with input validation.
- **Password Reset:** One-tap password reset email request.
- **User Profile:** View registered email and update display name.

### 📅 Event Management
- **Event Listings:** Browse upcoming events ordered by date.
- **Event Details:** View comprehensive event details (title, category, date, time, location, available slots, and description).
- **Event Creation:** Create new campus events with custom categories (Academic, Sports, Technology, Cultural, Other) and seat capacities.

### 🎟️ Event Enrollments
- **Enroll / Unenroll:** Register or cancel enrollment with atomic seat capacity updates in Cloud Firestore.
- **"My Enrollments" Filter:** Dedicated view listing events the student is currently enrolled in.

### ⭐ Favorites
- **Favorite / Unfavorite:** Bookmark or unbookmark any event (independent of enrollment).
- **"Favorites" Filter:** Dedicated tab to quickly view bookmarked events.

### 💬 Event Comments
- **Post Comments:** Authenticated users can post comments on event details pages.
- **Author & Timestamp:** Comments display the author's name and publication timestamp.
- **Edit & Delete Controls:** Users can edit or delete **only their own** comments.

---

## 🛠️ Tech Stack

- **Language:** Java / Kotlin
- **Android SDK:** Min SDK 31 | Target SDK 37
- **Architecture & UI:** Material Design 3, RecyclerView, ConstraintLayout/LinearLayout, Vector Drawables
- **Backend & Database:** Google Firebase
  - **Firebase Authentication:** Email and password authentication.
  - **Cloud Firestore:** Real-time NoSQL database.
- **Build System:** Gradle (Kotlin DSL `.kts`)

---

## 🗄️ Database Structure (Cloud Firestore)

### Collection `events`
| Field | Type | Description |
| :--- | :--- | :--- |
| `name` | String | Name/Title of the event |
| `category` | String | Event category (Academic, Sports, Technology, Cultural, Other) |
| `description` | String | Detailed event description |
| `date` | Timestamp | Scheduled event date |
| `time` | String | Scheduled event time (e.g. "19:00") |
| `location` | String | Event venue/location |
| `availableSlots` | Number | Available seat capacity |
| `createdBy` | String | UID of the event organizer |
| `createdAt` | Timestamp | Server timestamp when created |

### Subcollection `events/{eventId}/comments`
| Field | Type | Description |
| :--- | :--- | :--- |
| `eventId` | String | Associated event document ID |
| `userId` | String | UID of the comment author |
| `authorName` | String | Display name of the comment author |
| `text` | String | Comment text body |
| `createdAt` | Timestamp | Server timestamp when published |

### Collection `subscriptions` (Document ID: `${eventId}_${userId}`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `eventId` | String | Associated event document ID |
| `userId` | String | UID of the enrolled user |
| `subscribedAt` | Timestamp | Server timestamp when enrolled |

### Collection `favorites` (Document ID: `${eventId}_${userId}`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `eventId` | String | Associated event document ID |
| `userId` | String | UID of the user who bookmarked the event |
| `favoritedAt` | Timestamp | Server timestamp when bookmarked |

---

## ⚙️ Environment Setup

1. **Clone the Repository:**
   ```bash
   git clone <REPOSITORY_URL>
   ```

2. **Firebase Setup:**
   - Go to the [Firebase Console](https://console.firebase.google.com/).
   - Create a Firebase project and add an Android app with package name `com.project.application`.
   - Download `google-services.json` and place it inside the `app/` directory of the project.
   - Enable **Authentication** (Email/Password sign-in method).
   - Enable **Firestore Database** and configure the security rules:

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

3. **Running in Android Studio:**
   - Open the project in **Android Studio**.
   - Sync Gradle files (`Sync Project with Gradle Files`).
   - Run the application on an emulator or physical device running **Android 12 (API 31)** or higher.

---

## 📱 Application Screens

- `MainActivity` - Login & Password Reset Screen
- `RegisterActivity` - User Account Registration Screen
- `ProfileActivity` - User Profile Editing Screen
- `EventsActivity` - Events List with Filter Toggle (*All | My Enrollments | Favorites*)
- `EventDetailsActivity` - Event Details with Enrollment, Favoriting, and Comments Section
- `CreateEventActivity` - New Event Creation Form with Category Dropdown
