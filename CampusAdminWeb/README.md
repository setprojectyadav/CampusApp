# CampusAdminWeb - Campus Administration & Operations Console

A centralized, responsive, high-performance web dashboard for managing campus delivery operations, student accounts, rider fleets, disputes, fraud detection, and system telemetry.

Adapted with 100% feature parity from the Android `CampusAdminApp`.

---

## 🚀 Features

- **Master Security & RBAC**: Master Security PIN authentication, 2-step PIN reset workflow, and real-time session management.
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

## 🔑 Security, Credentials & Role-Based Access Architecture

Each role has an independent security PIN and designated operational workspace:

| Role | PIN | Accessible Tabs / Modules | Operational Scope |
| :--- | :--- | :--- | :--- |
| **Superior Administrator** | `4656` | All 11 Tabs (0–10) | Master control: System config, App version enforcement, PIN management, Factory resets, Full Telemetry |
| **Operations Officer** | `1234` | Orders (1), Riders (3), Services (4), Lost & Found (6), Feedback (10) | Campus operations: Live order dispatch, Rider fleet management, Service approvals, Lost & Found vault |
| **Financial Auditor** | `5678` | Overview (0), Orders (1), Disputes (5), Audit Trail (8), Reports (9) | Financial oversight: Escrow balances, Platform commission fees, Dispute refunds, Immutable audit trail & RFC CSV exports |
| **Safety & Dispute Officer** | `1011` | Students (2), Disputes (5), Lost & Found (6), Fraud Scanner (7) | Discipline & Safety: Student warnings/suspensions, Dispute arbitration, AI fraud detection & anomaly mitigation |

### 🛡️ Privilege Escalation & Switch Guard
- When logged in, any operator attempting to switch to another role is prompted to enter that target role's designated PIN.
- Superior Administrator has master authority to freely preview all workspaces without re-authentication.
- All switch attempts, whether approved or rejected, are immutably logged with the operator's real-time system IP address.

### 🌐 System IP Detection & Audit Logging
- Automatically captures the operator's active system/public IP address dynamically at the moment any change is committed.
- Records the IP address alongside the role badge in the immutable Audit Trail and includes it in all exported CSV logs.

### 🔒 3-Attempt Security Lockout & 24-Hour Emergency Rescue
- Enforces a maximum limit of **3 consecutive failed PIN attempts**.
- After 3 failed attempts, the console is locked for **24 hours**.
- Dispatches a security warning notification modal addressed to **`vishalsharma782635@gmail.com`** containing:
  - Exact UTC & IST timestamp of the security breach.
  - Detected IP address and device telemetry.
  - A secure **"Immediate Emergency Account Rescue"** button allowing the legitimate owner to immediately reset attempts and recover console access without waiting 24 hours.

---

## ⚡ Live Firebase Cloud Sync (CampusApp & CampusAdminWeb)

- **Firebase Project**: `campusapp-e8435`
- **Realtime Database**: `https://campusapp-e8435-default-rtdb.firebaseio.com`
- **Live Bi-Directional Nodes**:
  - `orders/` — Synchronizes order placement, ETA, OTP verification, runner assignment, billing corrections, and cancellations in real time.
  - `students/` — Synchronizes student profile states, disciplinary warnings, and temporary or permanent blocks.
  - `riders/` — Synchronizes runner active duties, live ratings, verification statuses, and fleet health.
  - `disputes/` — Synchronizes student dispute claims, proof attachments, and administrative refund payouts.
  - `audit_trail/` — Synchronizes immutable administrative action entries with detected client IP addresses.
