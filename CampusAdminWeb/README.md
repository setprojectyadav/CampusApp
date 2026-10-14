# CampusAdminWeb - Campus Administration & Operations Console

A centralized, responsive, high-performance web dashboard for managing campus delivery operations, student accounts, rider fleets, disputes, fraud detection, and system telemetry.

Adapted with 100% feature parity from the Android `CampusAdminApp`.

---

## 🚀 Features

- **Master Security & RBAC**: Dual default Master PIN unlock (`4656` / `1234`), 2-step PIN reset workflow, and real-time session management.
- **Operations Overview**: Real-time KPI telemetry (Live Orders, Active Fleet, Open Disputes, Blocked Users), database latency status, connected user counters, and system-wide maintenance mode controls.
- **Order Management & Live Tracking**: Full order lifecycle controls (Assign, Dispatch, Deliver, Cancel, Refund), live tracking timeline modal, OTP verification, and runner assignment.
- **Universal Case-Insensitive Search**: Non-blocking instantaneous search bar across every section (Orders, Students, Fleet, Services, Disputes, Lost & Found, Fraud, Audit Logs).
- **Student & Fleet Management**: Profile review, KYC verification, one-click block/unblock, warning issuance, and student rating history.
- **Campus Services Moderation**: Moderate cab pooling, group meals, and campus product rentals with deposit and return validations.
- **Dispute Resolution Center**: Interactive evidence viewer, chat logs, refund arbitration, and resolution audit notes.
- **Lost & Found Vault**: Claim verification, security hand-off token generation, item return confirmation.
- **AI Fraud Scanner**: Suspicious pattern alerts, rapid multi-order anomaly detection, account flag toggles.
- **Immutable Audit Trail & Telemetry**: Full historical logs with severity filters, exportable compliance trail.
- **Reports & Feedback**: Exportable operational statistics, revenue breakdowns, and user feedback ratings.

---

## 📂 Project Structure

```text
CampusAdminWeb/
│
├── index.html          # Main HTML structure with Tailwind CSS & Lucide Icons
├── css/
│   └── custom.css      # Custom styles, responsive tweaks, and theme tokens
├── js/
│   ├── data.js         # Initial mock datasets, schemas, and persistence handlers
│   └── app.js          # Core OOP application controller and event logic
└── README.md           # Documentation
```

---

## 💻 How to Run

Because this project is built using modern standard web technologies with **zero build step requirements**:

1. **Option 1: Direct File Launch**
   - Double-click `index.html` to open it directly in Google Chrome, Microsoft Edge, Mozilla Firefox, or Brave.

2. **Option 2: Local HTTP Server**
   - Using Python:
     ```bash
     python -m http.server 8080
     ```
     Then open `http://localhost:8080` in your browser.
   - Using VS Code: Right-click `index.html` and click **Open with Live Server**.

3. **Option 3: GitHub Pages Hosting**
   - Push this repository to GitHub.
   - Go to **Repository Settings** > **Pages**.
   - Under **Build and deployment**, select `Deploy from a branch` and choose `main` / `/ (root)`.
   - Your dashboard will be live instantly with a free HTTPS URL!

---

## 🔑 Default Credentials

- **Master PIN**: `4656` *(matching original app)* or `1234`
- You can change the Master PIN anytime directly inside the dashboard settings modal.
