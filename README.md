# HopeConnect — Finding Missing Persons 🛡️ 🇮🇳
 
**AI-Powered Social Impact Platform for Locating Missing Persons in India**
 
> "Every year, over 100,000+ people go missing in India. HopeConnect is built to ensure that no family has to wait in silence during those critical first 72 hours."
 
HopeConnect is a high-performance Android application that combines **on-device AI face recognition** with community-driven reporting to transform the missing persons search process into a smart, collaborative network.
 
---
 
## 🚀 Download & Try the App
 
[**📥 Download HopeConnect APK (v1.0.1)**](https://github.com/Ritesh-Shinde45/HopeConnect-App/releases/tag/v1.0.1)

[**📥 HopeConnect App Repo**](https://github.com/Ritesh-Shinde45/HopeConnect-App.git)

*Optimized for Android 7.0+.*
 
---
 
## ✨ Key Features
 
### 🤖 AI-Powered Face Matching
- Uses **MobileFaceNet via TensorFlow Lite** for on-device facial recognition
- Automatically scans the database to surface potential matches
- No internet required for face comparison — fully on-device
 
### 📋 Smart Reporting & GPS Integration
- File detailed reports with photos, real-time GPS coordinates, age, gender, last seen location
- Reports go through admin verification before going live
 
### 💬 Witness Collaboration Hub
- Secure built-in chat for volunteers and families
- Personal phone numbers stay hidden for privacy
 
### 🛡️ Admin Moderation Panel
- Full admin dashboard to review and verify reports before publishing
- Prevents spam and false entries from polluting the database
 
### 🔔 Smart Notifications
- Case match alerts, new report updates, admin announcements
 
### 🏆 Achievement System
- Volunteers earn recognition for verified leads and community contributions
 
---
 
## ⚙️ Tech Stack
 
| Layer          | Technology                                      |
|----------------|-------------------------------------------------|
| Mobile         | Android (Kotlin & Java), XML, Material Design 3 |
| Backend        | Appwrite (Database & Storage)                   |
| AI / ML        | TensorFlow Lite, MobileFaceNet                  |
| Architecture   | Modular Clean Architecture                      |
| Build System   | Gradle (Kotlin DSL)                             |
 
---
 
## 🏗 Project Architecture
```
HopeConnect
│
├── Activities
│   ├── LoginActivity
│   ├── RegisterActivity
│   ├── MainActivity
│   ├── ExploreActivity
│   ├── ProfileActivity
│   ├── NewReportActivity
│   ├── MissedPersonDetailActivity
│   ├── ChatsActivity / ChatRoomActivity
│   └── AdminDashboardActivity
│
├── Fragments
│   ├── MatchFragment       ← Face recognition matching
│   ├── MissedFragment      ← Browse missing persons
│   ├── HelpFragment        ← Community help logs
│   └── AchievementFragment ← Volunteer achievements
│
├── Models
│   ├── ReportModel
│   ├── UserModel
│   ├── HelpModel
│   └── NotificationModel
│
├── Adapters
│   ├── ReportAdapter
│   ├── ChatAdapter
│   └── FaceMatchResultAdapter
│
├── Services
│   ├── AppwriteService.kt  ← Backend singleton
│   └── AppwriteHelper.kt   ← Database utilities
│
└── ML Model
    └── assets/MobileFaceNet.tflite
```
 
## 🚀 Setup & Installation
 
### Prerequisites
- Android Studio Hedgehog or newer
- Android device or emulator (API 24 / Android 7.0+)
- Appwrite account at https://cloud.appwrite.io
 
### Steps
 
1. Clone the repository
 
   git clone https://github.com/Ritesh-Shinde45/HopeConnect.git
 
2. Open in Android Studio
 
3. Set up local configuration
 
   Copy the example file:
   cp local.properties.example local.properties
 
   Fill in your own Appwrite credentials in local.properties:
 
   APPWRITE_PROJECT_ID=your_project_id
   
   APPWRITE_DB_ID=your_database_id

   APPWRITE_USERS_BUCKET_ID=your_users_bucket_id

   APPWRITE_REPORT_BUCKET_ID=your_report_bucket_id

   APPWRITE_CHAT_BUCKET_ID=your_chat_bucket_id

   ADMIN_EMAIL=your_admin_email
 
5. Sync Gradle
   File → Sync Project with Gradle Files
 
6. Run the app
   Hit the ▶ Run button on your connected device or emulator.
 
⚠️ local.properties is gitignored and must never be committed.
   It contains your private backend credentials.
 
---
 
## 🔧 Engineering Challenges & Solutions
 
Appwrite + Java Bridge
The latest Appwrite SDK is Kotlin-first and incompatible with Java out of the box.
Solved this by building a Kotlin-based service layer (AppwriteService.kt + AppwriteHelper.kt)
that bridges async Kotlin coroutines to synchronous Java calls — allowing the existing
Java codebase to consume the backend seamlessly.
 
On-Device ML Performance
Chose MobileFaceNet specifically for its lightweight architecture,
enabling real-time facial embedding comparisons on mid-range Android devices without
noticeable battery drain or lag.
 
Data Integrity & Spam Prevention
Built a complete admin moderation workflow where every report requires approval before
appearing publicly. This keeps the platform reliable and trustworthy for families and authorities.
 
Secrets Management
All backend credentials are loaded at build time via BuildConfig from a local
local.properties file — never hardcoded in source.
Git history was scrubbed clean before open-sourcing.
 
---
 
## 📂 Project Structure

```text id="psx92k"
app/
│
├── src/main/
│   │
│   ├── java/com/ritesh/hoppeconnect/
│   │   ├── activities/
│   │   ├── fragments/
│   │   ├── adapters/
│   │   ├── models/
│   │   ├── AppwriteService.kt
│   │   └── AppwriteHelper.kt
│   │
│   ├── res/
│   │   ├── layout/
│   │   ├── drawable/
│   │   ├── values/
│   │   └── raw/
│   │       └── loading.gif
│   │
│   └── assets/
│       └── MobileFaceNet.tflite
│
├── build.gradle.kts
└── local.properties.example
```

 
## 🚀 Roadmap

```text
Phase 1 — Cloud Migration (Appwrite → AWS)
│
├── Migrate database → AWS DynamoDB
├── Move file storage → AWS S3
└── Implement authentication → AWS Cognito

Phase 2 — Cloud-Based AI Processing
│
├── Shift face recognition → AWS Lambda + Rekognition
├── Build REST APIs → AWS API Gateway
└── Deploy backend services → AWS EC2 / ECS

Phase 3 — Scale & Expansion
│
├── Web dashboard for authorities → AWS Amplify
├── Real-time alerts → AWS SNS
├── Multilingual support (Marathi, Hindi)
└── NGO & system integration
```

## 🤝 Contributing
 
Contributions are welcome!
 
1. Fork the repository
2. Create a new branch
   git checkout -b feature/your-feature-name
3. Make your changes
4. Commit with a clear message
   git commit -m "feat: describe your change"
5. Push and submit a Pull Request
 
Please make sure you have set up local.properties correctly before running locally.
 
---
 
## 👨‍💻 Developers
 
Ritesh Shinde — Cloud Security Learner
Passionate about building secure and reliable technology for social good.
 
Kishan Alladwar — Data Science Learner
Special thanks to Kishan for his support throughout this journey. 🤝
 
LinkedIn : https://www.linkedin.com/in/ritesh--shinde/
GitHub   : https://github.com/Ritesh-Shinde45
 
---
 
## ⭐ Support
 
If HopeConnect resonates with you, please give it a star on GitHub ⭐
Every star helps more people discover this project and contributes to
the mission of reuniting families.
 
