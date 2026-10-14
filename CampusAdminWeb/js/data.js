// Campus Admin Console - Unified Mock Data & State Schema
// Matches AdminMainActivity.kt exactly with 100% feature parity

const ADMIN_ROLES = {
    SUPER_ADMIN: {
        id: "SUPER_ADMIN",
        name: "Super Administrator",
        badgeColor: "bg-purple-600 text-white",
        description: "Full unrestricted access to all operations, accounts, and audits.",
        allowedTabs: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    },
    FINANCIAL_AUDITOR: {
        id: "FINANCIAL_AUDITOR",
        name: "Financial Auditor",
        badgeColor: "bg-sky-500 text-white",
        description: "Access limited to Financial Ledger, Wallet balances, Escrow, and Reports.",
        allowedTabs: [0, 1, 2, 5, 8, 9]
    },
    OPERATIONS_OFFICER: {
        id: "OPERATIONS_OFFICER",
        name: "Operations Officer",
        badgeColor: "bg-emerald-500 text-white",
        description: "Access limited to Live Orders, Runners, Cab Pools, Meal Circles, and Rentals.",
        allowedTabs: [0, 1, 4, 6, 10]
    },
    SAFETY_DISPUTE_OFFICER: {
        id: "SAFETY_DISPUTE_OFFICER",
        name: "Safety & Dispute Officer",
        badgeColor: "bg-rose-500 text-white",
        description: "Access limited to Disputes, Lost & Found, Fraud Scanner, and Moderation.",
        allowedTabs: [0, 2, 5, 6, 7, 8]
    }
};

const TAB_DEFINITIONS = [
    { id: 0, label: "Overview", icon: "activity" },
    { id: 1, label: "Orders", icon: "shopping-bag" },
    { id: 2, label: "Students", icon: "users" },
    { id: 3, label: "Riders", icon: "bike" },
    { id: 4, label: "Services", icon: "grid" },
    { id: 5, label: "Disputes", icon: "shield-alert" },
    { id: 6, label: "Lost & Found", icon: "search" },
    { id: 7, label: "Fraud Scanner", icon: "zap" },
    { id: 8, label: "Audit Trail", icon: "file-text" },
    { id: 9, label: "Reports", icon: "download" },
    { id: 10, label: "Feedback", icon: "message-square" }
];

const DEFAULT_SYSTEM_CONFIG = {
    minAppVersion: "2.4.0",
    forceUpdateEnabled: false,
    maintenanceMode: false,
    serverStatus: "OPTIMAL (18ms)",
    activeSessions: 142,
    errorRate: "0.02%"
};

const SEED_ORDERS = [
    {
        id: "ORD-4029",
        customerId: "student@college.edu",
        customerName: "Arjun Patel",
        customerPhone: "+91 9876543210",
        deliveryOtp: "4912",
        items: [
            { name: "Bourbon Biscuit", qty: 2 },
            { name: "Coca Cola 300ml", qty: 1 }
        ],
        pickPoint: "Main Canteen Counter 2",
        dropPoint: "Hostel 3, Room 204",
        totalMin: 85,
        totalMax: 110,
        deliveryFee: 20,
        gatePremium: 0,
        status: "RUNNER_ASSIGNED",
        actualBill: 95,
        refundAmount: 0,
        runnerName: "Vikram Singh",
        runnerPhone: "+91 9822334455",
        runnerRating: "4.8 *",
        timestamp: "Today, 1:15 PM"
    },
    {
        id: "ORD-4030",
        customerId: "rahul.k@college.edu",
        customerName: "Rahul Kumar",
        customerPhone: "+91 9123456789",
        deliveryOtp: "8821",
        items: [
            { name: "Lays Classic Salted", qty: 3 },
            { name: "Amul Butter", qty: 1 }
        ],
        pickPoint: "Night Canteen Tuck Shop",
        dropPoint: "Girls Hostel 1 Gate",
        totalMin: 140,
        totalMax: 160,
        deliveryFee: 25,
        gatePremium: 0,
        status: "PENDING_ACCEPTANCE",
        actualBill: 0,
        refundAmount: 0,
        runnerName: null,
        runnerPhone: null,
        runnerRating: null,
        timestamp: "Today, 2:00 PM"
    },
    {
        id: "ORD-4031",
        customerId: "priya.s@college.edu",
        customerName: "Priya Sharma",
        customerPhone: "+91 9811223344",
        deliveryOtp: "1142",
        items: [
            { name: "Nescafe Hot Coffee", qty: 2 },
            { name: "Grilled Sandwich", qty: 1 }
        ],
        pickPoint: "Nescafe Kiosk Block B",
        dropPoint: "Girls Hostel 2, Room 102",
        totalMin: 110,
        totalMax: 130,
        deliveryFee: 15,
        gatePremium: 0,
        status: "DELIVERED",
        actualBill: 120,
        refundAmount: 0,
        runnerName: "Amit Kumar",
        runnerPhone: "+91 9811998877",
        runnerRating: "4.7 *",
        timestamp: "Today, 12:45 PM"
    }
];

const SEED_STUDENTS = [
    {
        email: "student@college.edu",
        name: "Arjun Patel",
        phone: "+91 9876543210",
        rollNo: "22CS042",
        hostel: "Hostel Block 3",
        stream: "Computer Science",
        className: "B.Tech Year 3",
        upiId: "arjun@upi",
        walletBalance: 450,
        isSignedUp: true,
        isVerified: true,
        studentStatus: "ACTIVE",
        studentBlockReason: null,
        studentBlockUntil: 0,
        warningMessage: null,
        riderStatus: "ACTIVE",
        riderBlockReason: null,
        riderBlockUntil: 0
    },
    {
        email: "rohit.v@college.edu",
        name: "Rohit Verma",
        phone: "+91 9876500011",
        rollNo: "21ME102",
        hostel: "Hostel Block 1",
        stream: "Mechanical",
        className: "B.Tech Year 4",
        upiId: "rohit@axis",
        walletBalance: 80,
        isSignedUp: true,
        isVerified: true,
        studentStatus: "ACTIVE",
        studentBlockReason: null,
        studentBlockUntil: 0,
        warningMessage: null,
        riderStatus: "TEMP_BLOCKED",
        riderBlockReason: "Rider Malpractice: Repetitive Fake Pairing Velocity Spike",
        riderBlockUntil: Date.now() + 10 * 3600 * 1000
    },
    {
        email: "priya.s@college.edu",
        name: "Priya Sharma",
        phone: "+91 9811223344",
        rollNo: "23EC019",
        hostel: "Girls Hostel 2",
        stream: "Electronics",
        className: "B.Tech Year 2",
        upiId: "priya@okhdfc",
        walletBalance: 320,
        isSignedUp: true,
        isVerified: true,
        studentStatus: "ACTIVE",
        studentBlockReason: null,
        studentBlockUntil: 0,
        warningMessage: null,
        riderStatus: "ACTIVE",
        riderBlockReason: null,
        riderBlockUntil: 0
    }
];

const SEED_RIDERS = [
    {
        email: "student@college.edu",
        name: "Arjun Patel (You)",
        phone: "+91 9876543210",
        rollNo: "22CS042",
        department: "Computer Science",
        isOnline: true,
        activeJobId: "ORD-4029",
        walletBalance: 850,
        totalDeliveries: 42,
        rating: "4.9 *",
        verificationStatus: "VERIFIED",
        riderStatus: "ACTIVE"
    },
    {
        email: "vikram.s@college.edu",
        name: "Vikram Singh",
        phone: "+91 9822334455",
        rollNo: "21EE088",
        department: "Electrical Engg",
        isOnline: true,
        activeJobId: null,
        walletBalance: 1240,
        totalDeliveries: 118,
        rating: "4.8 *",
        verificationStatus: "VERIFIED",
        riderStatus: "ACTIVE"
    },
    {
        email: "amit.k@college.edu",
        name: "Amit Kumar",
        phone: "+91 9811998877",
        rollNo: "23CE014",
        department: "Civil Engg",
        isOnline: false,
        activeJobId: null,
        walletBalance: 420,
        totalDeliveries: 31,
        rating: "4.7 *",
        verificationStatus: "VERIFIED",
        riderStatus: "ACTIVE"
    }
];

const SEED_CAB_POOLS = [
    {
        id: "CAB-901",
        hostEmail: "ananya.s@college.edu",
        hostName: "Ananya Sharma",
        hostPhone: "+91 9988776655",
        pickupPoint: "Main Gate Pick-Up Zone",
        dropDestination: "Airport Terminal 2",
        departureDate: "Tomorrow",
        departureTime: "06:00 AM",
        totalSeats: 4,
        bookedSeats: 3,
        perPersonFare: 280,
        status: "CONFIRMED_RIDE",
        isNightRide: true
    },
    {
        id: "CAB-902",
        hostEmail: "karan.m@college.edu",
        hostName: "Karan Mehta",
        hostPhone: "+91 9822114477",
        pickupPoint: "Hostel 4 Parking",
        dropDestination: "City Railway Station",
        departureDate: "Today",
        departureTime: "08:30 PM",
        totalSeats: 3,
        bookedSeats: 2,
        perPersonFare: 140,
        status: "OPEN",
        isNightRide: false
    }
];

const SEED_MEAL_CIRCLES = [
    {
        id: "MEAL-501",
        hostEmail: "siddharth.r@college.edu",
        hostName: "Siddharth Roy",
        hostPhone: "+91 9845012345",
        restaurantSpot: "Dominos Pizza Outpost",
        mealCategory: "Italian & Fast Food",
        meetingPoint: "Food Court Center Table",
        targetTime: "Tonight, 8:00 PM",
        totalBuddies: 4,
        joinedBuddies: 3,
        status: "OPEN"
    }
];

const SEED_RENTALS = [
    {
        id: "RNT-301",
        renterEmail: "student@college.edu",
        productName: "Engineering Drawing Set + Drafter",
        durationDays: 7,
        totalPaid: 140,
        handoverOtp: "6190",
        returnOtp: "4401",
        status: "ACTIVE_BORROWED"
    }
];

const SEED_DISPUTES = [
    {
        id: "DSP-1001",
        orderId: "ORD-4029",
        serviceType: "Delivery",
        complainantName: "Rahul Sharma",
        complainantEmail: "student@college.edu",
        complainantPhone: "+91 9876543210",
        respondentName: "Vikram Singh",
        respondentEmail: "vikram.s@college.edu",
        respondentRole: "Rider",
        issueCategory: "Order Not Delivered",
        description: "Rider marked delivery as completed but items were never handed over at Hostel 3.",
        escrowAmount: 110,
        chatLogs: [
            { sender: "Complainant", senderName: "Rahul Sharma", message: "Where are you? I am waiting downstairs.", time: "1:20 PM" },
            { sender: "Respondent", senderName: "Vikram Singh", message: "I left the packet with security guard at Gate 1.", time: "1:22 PM" },
            { sender: "Complainant", senderName: "Rahul Sharma", message: "My address clearly says Hostel 3 Room 204. Please bring it here.", time: "1:25 PM" }
        ],
        evidenceNotes: "Attached Photo: Empty reception desk at Hostel 3. Attached Call Log: 2 unanswered calls.",
        filedTimestamp: "Today, 1:30 PM",
        status: "OPEN_INVESTIGATION",
        adminResolutionNote: null,
        refundAmount: 0,
        resolvedBy: null
    }
];

const SEED_LOST_FOUND = [
    {
        id: "LNF-201",
        type: "LOST",
        title: "Calculus Engineering Textbook & Spiral Notes",
        category: "Books & Stationery",
        description: "Black spiral notebook left in Lecture Hall 104 back row.",
        location: "Academic Block 1, LH 104",
        date: "Today, 11:30 AM",
        reporterName: "Sneha Kapoor",
        reporterEmail: "sneha.k@college.edu",
        reporterPhone: "+91 9871122334",
        reporterRollNo: "23CS089",
        status: "ACTIVE",
        securityDeskNote: null,
        claimedByRoll: null,
        isFlagged: false,
        adminTakedownReason: null
    },
    {
        id: "LNF-202",
        type: "FOUND",
        title: "Boat Airdopes Earbuds Case (Blue)",
        category: "Electronics",
        description: "Found on concrete bench outside Library canteen.",
        location: "Central Library Garden",
        date: "Yesterday, 5:45 PM",
        reporterName: "Tanmay Sen",
        reporterEmail: "tanmay.s@college.edu",
        reporterPhone: "+91 9833445566",
        reporterRollNo: "22ME034",
        status: "SECURITY_DEPOSITED",
        securityDeskNote: "Held at Main Gate Security Desk under Guard In-Charge Rajesh.",
        claimedByRoll: null,
        isFlagged: false,
        adminTakedownReason: null
    }
];

const SEED_FRAUD_ANOMALIES = [
    {
        id: "ANM-501",
        userId: "rohit.v@college.edu",
        userName: "Rohit Verma",
        userRole: "Rider & Student",
        malpracticeType: "RIDER_MALPRACTICE",
        riskLevel: "CRITICAL",
        riskScore: 94,
        anomalyType: "Repetitive Fake Delivery Pairing",
        description: "Rider accepted 5 orders within 15 minutes from the same hostel room with instant OTP completions under 30s.",
        telemetryDetail: "5 Orders / 15m | Paired Account: student2@college.edu | GPS Match Delta: 0m | Risk Matrix: High Collusion",
        detectedAt: "Today, 11:15 AM",
        status: "FLAGGED_PENDING",
        blockTarget: null,
        blockUntil: 0
    },
    {
        id: "ANM-502",
        userId: "student@college.edu",
        userName: "Arjun Patel",
        userRole: "Student (Customer)",
        malpracticeType: "CUSTOMER_MALPRACTICE",
        riskLevel: "MEDIUM",
        riskScore: 52,
        anomalyType: "Rapid Order Cancellation Loop",
        description: "Customer created and cancelled 3 snack orders within 10 minutes.",
        telemetryDetail: "3 Cancellations / 10m | Escrow Cycle: Rs. 270 | Device IP: 10.0.2.16",
        detectedAt: "Today, 12:40 PM",
        status: "FLAGGED_PENDING",
        blockTarget: null,
        blockUntil: 0
    },
    {
        id: "ANM-503",
        userId: "vikram.s@college.edu",
        userName: "Vikram Singh",
        userRole: "Rider",
        malpracticeType: "RIDER_MALPRACTICE",
        riskLevel: "HIGH",
        riskScore: 81,
        anomalyType: "GPS Velocity Spike / Mock Movement",
        description: "Rider location jumped 3.2 km in 45 seconds between North Gate and South Hostel canteen.",
        telemetryDetail: "Speed: 256 km/h recorded | Location Spoofing suspected | Mock GPS Flagged",
        detectedAt: "Today, 01:05 PM",
        status: "FLAGGED_PENDING",
        blockTarget: null,
        blockUntil: 0
    },
    {
        id: "ANM-504",
        userId: "priya.s@college.edu",
        userName: "Priya Sharma",
        userRole: "Student (Customer)",
        malpracticeType: "ESCROW_ABUSE",
        riskLevel: "CRITICAL",
        riskScore: 88,
        anomalyType: "Multi-Account Wallet Referral Farming",
        description: "4 new accounts registered with virtual phone numbers linked to single UPI ID for promotional balance abuse.",
        telemetryDetail: "Android Device ID: 88f2-10a4 | UPI: priya@okhdfc | 4 Accounts created within 30 mins",
        detectedAt: "Today, 01:25 PM",
        status: "FLAGGED_PENDING",
        blockTarget: null,
        blockUntil: 0
    }
];

const SEED_AUDIT_LOGS = [
    {
        id: "AUD-1001",
        timestamp: "11 Oct 2026, 10:15:20 AM",
        adminRole: "SUPER_ADMIN",
        actionType: "SYSTEM_INITIALIZE",
        targetEntityId: "SYSTEM",
        details: "Admin Master Web Operations Console initialized with secure client state.",
        severity: "INFO"
    },
    {
        id: "AUD-1002",
        timestamp: "11 Oct 2026, 12:45:10 PM",
        adminRole: "FINANCIAL_AUDITOR",
        actionType: "REPORT_EXPORT",
        targetEntityId: "FIN-CSV",
        details: "Generated RFC-compliant CSV Financial and Settlement Report for current cycle.",
        severity: "INFO"
    }
];

const SEED_FEEDBACK = [
    {
        id: "FB-101",
        userEmail: "ananya.s@college.edu",
        userName: "Ananya Sharma",
        userRole: "Customer",
        category: "Delivery Speed",
        rating: 5,
        message: "Delivery from Night Canteen was under 15 minutes! The OTP security feature is great.",
        timestamp: "Today, 1:40 PM",
        isAcceptedAsTask: false,
        taskStatus: "PLANNED",
        adminNote: null
    },
    {
        id: "FB-102",
        userEmail: "vikram.s@college.edu",
        userName: "Vikram Singh",
        userRole: "Rider",
        category: "App UI & Speed",
        rating: 4,
        message: "Need option to see hostel delivery notes directly in notification bar.",
        timestamp: "Yesterday, 9:20 PM",
        isAcceptedAsTask: true,
        taskStatus: "IN_PROGRESS",
        adminNote: "Accepted for next release."
    }
];
