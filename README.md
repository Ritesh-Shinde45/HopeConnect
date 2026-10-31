# HopeConnect
 
**An Android app that helps families, volunteers and police find missing persons in India.**
 
> Over 100,000 people go missing in India every year. HopeConnect exists so that no family has to wait in silence during the first 72 hours.
 
HopeConnect combines on-device face recognition with community reporting. Anyone can file a report with a photo and location. The app compares faces on the phone and shows possible matches. Every report is checked by an admin before it goes public.
 
Next, we are adding a police portal and a system that scans publicly available CCTV cameras. Both are on the roadmap below.
 
## Download
 
- [HopeConnect APK (v1.0.3)](https://github.com/Ritesh-Shinde45/HopeConnect-App/releases/download/v1.0.3/HopeConnect.apk)
- [HopeConnect app repository](https://github.com/Ritesh-Shinde45/HopeConnect-App.git)
Works on Android 7.0 and above.
 
## Features
 
**Face matching**
The app uses MobileFaceNet through TensorFlow Lite. It scans stored reports and shows likely matches. Face comparison runs fully on the device, so it works without internet.
 
**Reporting with GPS**
Users file reports with photos, live GPS location, age, gender and the place where the person was last seen.
 
**Admin moderation**
Admins review every report before it is published. This keeps spam and false entries out of the database.
 
**Witness chat**
Families and volunteers can talk inside the app. Personal phone numbers stay hidden.
 
**Notifications**
Users get alerts for case matches, report updates and admin announcements.
 
**Achievements**
Volunteers earn recognition for verified leads and community help.
 
## Tech stack
 
| Layer        | Technology                                      |
|--------------|-------------------------------------------------|
| Mobile       | Android (Kotlin and Java), XML, Material Design 3 |
| Backend      | Appwrite (database and storage)                 |
| AI and ML    | TensorFlow Lite, MobileFaceNet                  |
| Architecture | Modular clean architecture                      |
| Build system | Gradle (Kotlin DSL)                             |
 
## Architecture
 
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
│   ├── ChatsActivity and ChatRoomActivity
│   └── AdminDashboardActivity
│
├── Fragments
│   ├── MatchFragment         (face matching)
│   ├── MissedFragment        (browse missing persons)
│   ├── HelpFragment          (community help logs)
│   └── AchievementFragment   (volunteer achievements)
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
│   ├── AppwriteService.kt    (backend singleton)
│   └── AppwriteHelper.kt     (database utilities)
│
└── ML model
    └── assets/MobileFaceNet.tflite
```
 
## Project structure
 
```
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
 
## Setup
 
### Requirements
 
- Android Studio Hedgehog or newer
- An Android device or emulator (API 24 or higher)
- An Appwrite account at https://cloud.appwrite.io
### Steps
 
1. Clone the repository:
```
   git clone https://github.com/Ritesh-Shinde45/HopeConnect.git
```
 
2. Open the project in Android Studio.
3. Copy the example config file:
```
   cp local.properties.example local.properties
```
 
4. Add your own Appwrite details to `local.properties`:
```
   APPWRITE_PROJECT_ID=your_project_id
   APPWRITE_DB_ID=your_database_id
   APPWRITE_USERS_BUCKET_ID=your_users_bucket_id
   APPWRITE_REPORT_BUCKET_ID=your_report_bucket_id
   APPWRITE_CHAT_BUCKET_ID=your_chat_bucket_id
   ADMIN_EMAIL=your_admin_email
```
 
5. Sync Gradle: **File > Sync Project with Gradle Files**.
6. Press **Run** on your device or emulator.
`local.properties` is listed in `.gitignore`. It holds private credentials, so never commit it.
 
## Engineering notes
 
**Using Appwrite from Java**
The latest Appwrite SDK is built for Kotlin and does not work with Java directly. We wrote a Kotlin service layer (`AppwriteService.kt` and `AppwriteHelper.kt`). It turns async coroutines into synchronous calls, so the existing Java code can use the backend without changes.
 
**Fast face matching on mid-range phones**
We chose MobileFaceNet because it is small. It compares face embeddings in real time without noticeable lag or battery drain.
 
**Keeping data clean**
Every report needs admin approval before it appears publicly. Families and authorities can trust what they see.
 
**Handling secrets**
Backend credentials are read at build time through `BuildConfig` from `local.properties`. Nothing is hardcoded. We also cleaned the git history before making the repository public.
 
## Roadmap
 
The plan has four phases. Each phase builds on the one before it.
 
### Phase 1: Move to AWS
 
Appwrite works for the prototype. For police and large-scale use we need a stronger backend.
 
```
Appwrite                    AWS
─────────────────────────────────────────────
Database            ->      DynamoDB
File storage        ->      S3
Authentication      ->      Cognito
```
 
Result: one cloud backend that the app, the police portal and the CCTV system can all share.
 
### Phase 2: Police portal and cloud AI
 
Police stations get a web portal to manage reports and act on them.
 
**What police can do**
- Sign in with a verified police account (Cognito with role-based access)
- Upload missing person reports, including the FIR number
- Review reports filed by families and volunteers
- Update case status (open, under investigation, found, closed)
- Add notes and record the action taken
- Request more details from the family
- Get an alert when a face match or CCTV match is found
**What families see**
- The same case, with status updates from the police
- Their uploaded report linked to the official record
**Cloud AI and APIs**
```
Face matching on the cloud   ->   AWS Lambda + Amazon Rekognition
REST APIs                    ->   Amazon API Gateway
Backend services             ->   AWS EC2 or ECS
```
 
On-device matching stays in the app. The cloud adds matching across all cases and districts.
 
### Phase 3: Public CCTV scanning
 
This is the main new idea. The system will keep scanning publicly available CCTV cameras and look for people on the active missing persons list.
 
```
Public CCTV feeds
      │
      ▼
Frame sampling service (ECS)
      │   picks frames at set intervals
      ▼
Face detection and matching (Rekognition)
      │   compares against active reports
      ▼
Confidence check
      │   low scores are dropped
      ▼
Police review
      │   a human confirms the match
      ▼
Alerts (Amazon SNS)
      ├── Concerned police authority
      └── Family that uploaded the report
```
 
**Steps to build it**
1. Keep a registry of public camera sources, with location and owner details.
2. Build a service that reads each feed and samples frames.
3. Detect faces and compare them with active missing person reports.
4. Set a confidence threshold and send likely matches to a police review queue.
5. After police confirm, send alerts through SNS with the camera location and time.
6. Log every match and every action for audit.
**Responsible use**
- Only publicly available and legally accessible feeds are used.
- A human at the police end confirms each match before any alert goes out.
- Only the matched snapshot and its details are stored. Other footage is not kept.
- Matches are shared only with the concerned authority and the family. They are never posted publicly.
- Access and actions are logged.
We will take legal and privacy advice before the pilot.
 
### Phase 4: Scale and expansion
 
- Web dashboard for authorities (AWS Amplify)
- Real-time alerts (Amazon SNS)
- Marathi and Hindi support
- Integration with NGOs and existing police systems
- Pilot in one district, then repeat in other districts and states
## Contributing
 
Contributions are welcome.
 
1. Fork the repository.
2. Create a branch:
```
   git checkout -b feature/your-feature-name
```
3. Make your changes.
4. Commit with a clear message:
```
   git commit -m "feat: describe your change"
```
5. Push and open a pull request.
Please set up `local.properties` before you run the app locally.
 
## Developers
 
**Ritesh Shinde**, Cloud Security Learner. Building secure and reliable technology for social good.
[LinkedIn](https://www.linkedin.com/in/ritesh--shinde/) | [GitHub](https://github.com/Ritesh-Shinde45)

**Kishan Alladwar**, Data Science Learner. Thank you for the support throughout this project.
 

 
## Support
 
If HopeConnect is useful to you, please star the repository. It helps more people find the project and supports the goal of reuniting families.
 
