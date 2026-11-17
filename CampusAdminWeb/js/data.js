// Campus Admin Console - Unified Mock Data & State Schema
// Matches AdminMainActivity.kt exactly with 100% feature parity

const ADMIN_ROLES = {
    SUPER_ADMIN: {
        id: "SUPER_ADMIN",
        name: "Superior Administrator",
        badgeColor: "bg-purple-600 text-white",
        description: "Full master access to all operations, policies, and controls.",
        defaultPin: "4656",
        allowedTabs: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
    },
    OPERATIONS_OFFICER: {
        id: "OPERATIONS_OFFICER",
        name: "Operations Officer",
        badgeColor: "bg-emerald-500 text-white",
        description: "Live order dispatch, rider fleet, and services.",
        defaultPin: "1234",
        allowedTabs: [1, 3, 4, 6, 10] // Orders, Riders, Services, Lost & Found, Feedback
    },
    FINANCIAL_AUDITOR: {
        id: "FINANCIAL_AUDITOR",
        name: "Financial Auditor",
        badgeColor: "bg-sky-500 text-white",
        description: "Financial oversight, escrow balances, and reports.",
        defaultPin: "5678",
        allowedTabs: [0, 1, 5, 8, 9] // Overview (Financial), Orders, Disputes (Refunds), Audit Trail, Reports
    },
    SAFETY_DISPUTE_OFFICER: {
        id: "SAFETY_DISPUTE_OFFICER",
        name: "Safety & Dispute Officer",
        badgeColor: "bg-rose-500 text-white",
        description: "Dispute resolution, safety conduct, and fraud scanner.",
        defaultPin: "1011",
        allowedTabs: [2, 5, 6, 7] // Students, Disputes, Lost & Found, Fraud Scanner
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
        timestamp: "Today, 1:15 PM",
        chatLogs: [
            { sender: "Customer", senderName: "Arjun Patel", message: "Hi Vikram, please get chilled Coca Cola if available at Counter 2.", time: "Today, 1:16 PM" },
            { sender: "Rider", senderName: "Vikram Singh", message: "Yes Arjun, got cold Coke and 2 packets of Bourbon. Total came to Rs. 95.", time: "Today, 1:20 PM" },
            { sender: "Rider", senderName: "Vikram Singh", message: "Reaching Hostel 3 staircase now. Please keep delivery OTP handy.", time: "Today, 1:26 PM" }
        ]
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
        timestamp: "Today, 2:00 PM",
        chatLogs: [
            { sender: "Customer", senderName: "Rahul Kumar", message: "Order placed for delivery at Girls Hostel 1 Gate. Looking for runner.", time: "Today, 2:00 PM" }
        ]
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
        timestamp: "Today, 12:45 PM",
        chatLogs: [
            { sender: "Customer", senderName: "Priya Sharma", message: "Please make the coffee extra hot, thanks!", time: "Today, 12:46 PM" },
            { sender: "Rider", senderName: "Amit Kumar", message: "Sure Priya, just picked up fresh grilled sandwich and two piping hot coffees.", time: "Today, 12:54 PM" },
            { sender: "Rider", senderName: "Amit Kumar", message: "Handed over at Room 102 door. OTP 1142 verified.", time: "Today, 1:05 PM" }
        ]
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
        isNightRide: true,
        chatLogs: [
            { sender: "Host", senderName: "Ananya Sharma", message: "Hey everyone! Booking an Uber XL. Please be at Main Gate by 5:50 AM sharp.", time: "Yesterday, 8:15 PM" },
            { sender: "Rider", senderName: "Rohan Verma", message: "Noted Ananya! I have one medium suitcase, will that fit in the boot?", time: "Yesterday, 8:20 PM" },
            { sender: "Host", senderName: "Ananya Sharma", message: "Yes plenty of boot space. Total fare is ~1120 so 280 each via UPI.", time: "Yesterday, 8:22 PM" }
        ]
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
        isNightRide: false,
        chatLogs: [
            { sender: "Host", senderName: "Karan Mehta", message: "Departing for City Railway Station to catch the 10:15 PM Shatabdi. 1 seat left!", time: "Today, 5:10 PM" },
            { sender: "Rider", senderName: "Pooja Hegde", message: "I want to join! Meeting directly at Hostel 4 parking.", time: "Today, 5:30 PM" }
        ]
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
        status: "OPEN",
        chatLogs: [
            { sender: "Host", senderName: "Siddharth Roy", message: "Dominos 40% off coupon code active! Looking for 3 buddies to share 2 Large Peppy Paneer + Cheese Garlic Bread.", time: "Today, 6:30 PM" },
            { sender: "Buddy", senderName: "Tanmay Bhat", message: "Count me in! I will bring Coke 750ml from tuck shop.", time: "Today, 6:45 PM" },
            { sender: "Buddy", senderName: "Divya Nair", message: "Joining! I will meet you guys at the food court center table at 8 sharp.", time: "Today, 7:00 PM" }
        ]
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
        status: "ACTIVE_BORROWED",
        chatLogs: [
            { sender: "Borrower", senderName: "Rahul Sharma", message: "Hi, I need the mini-drafter for ED submission on Monday.", time: "2 days ago, 3:15 PM" },
            { sender: "Owner", senderName: "Sneha Kapoor", message: "Sure, mini drafter and sheet clips are in pristine condition. Handover OTP is 6190.", time: "2 days ago, 3:30 PM" },
            { sender: "Borrower", senderName: "Rahul Sharma", message: "Collected from Hostel 2 reception. Thank you!", time: "2 days ago, 4:00 PM" }
        ]
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
        description: "Black spiral notebook left in Lecture Hall 104 back row. Has handwritten formulas on cover.",
        location: "Academic Block 1, LH 104",
        date: "Today, 11:30 AM",
        reporterName: "Sneha Kapoor",
        reporterEmail: "sneha.k@college.edu",
        reporterPhone: "+91 9871122334",
        reporterRollNo: "23CS089",
        status: "ACTIVE",
        imageUrl: "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=900&q=80",
        images: [
            "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=900&q=80",
            "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?auto=format&fit=crop&w=900&q=80"
        ],
        photoBase64: null,
        securityDeskNote: null,
        claimedByRoll: null,
        isFlagged: false,
        adminTakedownReason: null,
        chatLogs: [
            { sender: "Reporter", senderName: "Sneha Kapoor", message: "Left my spiral notes during period 3. It contains all derivation solutions.", time: "Today, 11:35 AM" },
            { sender: "Security", senderName: "Academic Block Security", message: "We have checked LH 104 and kept it safe at Block 1 Guard desk.", time: "Today, 11:55 AM" }
        ]
    },
    {
        id: "LNF-202",
        type: "FOUND",
        title: "Boat Airdopes Earbuds Case (Blue)",
        category: "Electronics",
        description: "Found on concrete bench outside Library canteen. Verified serial indicator active.",
        location: "Central Library Garden",
        date: "Yesterday, 5:45 PM",
        reporterName: "Tanmay Sen",
        reporterEmail: "tanmay.s@college.edu",
        reporterPhone: "+91 9833445566",
        reporterRollNo: "22ME034",
        status: "ACTIVE",
        imageUrl: "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&w=900&q=80",
        images: [
            "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?auto=format&fit=crop&w=900&q=80"
        ],
        photoBase64: null,
        securityDeskNote: null,
        claimedByRoll: null,
        isFlagged: false,
        adminTakedownReason: null,
        chatLogs: [
            { sender: "Finder", senderName: "Tanmay Sen", message: "Found this blue Boat case on the concrete bench. Handing over to Central Library desk.", time: "Yesterday, 5:50 PM" }
        ]
    },
    {
        id: "LNF-203",
        type: "FOUND",
        title: "Casio FX-991CW Scientific Calculator",
        category: "Electronics",
        description: "Found in Physics Lab Desk 12. Has neon yellow barcode sticker on back.",
        location: "Science Block 2, Lab 12",
        date: "Today, 09:15 AM",
        reporterName: "Kavita Reddy",
        reporterEmail: "kavita.r@college.edu",
        reporterPhone: "+91 9822114477",
        reporterRollNo: "23EC015",
        status: "ACTIVE",
        imageUrl: "https://images.unsplash.com/photo-1587145820266-a5951ee6f620?auto=format&fit=crop&w=900&q=80",
        images: [
            "https://images.unsplash.com/photo-1587145820266-a5951ee6f620?auto=format&fit=crop&w=900&q=80"
        ],
        photoBase64: null,
        securityDeskNote: null,
        claimedByRoll: null,
        isFlagged: false,
        adminTakedownReason: null
    },
    {
        id: "LNF-204",
        type: "LOST",
        title: "Titan Analog Watch with Brown Leather Strap",
        category: "Others",
        description: "Left near badminton court bench during evening practice. Dial has slight scuff mark.",
        location: "Indoor Sports Complex",
        date: "2 days ago, 7:20 PM",
        reporterName: "Rohan Verma",
        reporterEmail: "rohan.v@college.edu",
        reporterPhone: "+91 9811442200",
        reporterRollNo: "22EE061",
        status: "ACTIVE",
        imageUrl: "https://images.unsplash.com/photo-1524805444758-089113d48a6d?auto=format&fit=crop&w=900&q=80",
        images: [
            "https://images.unsplash.com/photo-1524805444758-089113d48a6d?auto=format&fit=crop&w=900&q=80"
        ],
        photoBase64: null,
        securityDeskNote: null,
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
        timestamp: "11 Oct 2026, 10:15:20 AM IST",
        adminRole: "SUPER_ADMIN",
        ipAddress: "103.187.228.184",
        actionType: "SYSTEM_INITIALIZE",
        targetEntityId: "SYSTEM",
        details: "Admin Master Web Operations Console initialized with secure client state.",
        severity: "INFO"
    },
    {
        id: "AUD-1002",
        timestamp: "11 Oct 2026, 12:45:10 PM IST",
        adminRole: "FINANCIAL_AUDITOR",
        ipAddress: "192.168.1.105",
        actionType: "REPORT_EXPORT",
        targetEntityId: "FIN-CSV",
        details: "Generated RFC-compliant CSV Financial and Settlement Report for current cycle.",
        severity: "INFO"
    }
];

const SEED_SESSION_LOGS = [
    {
        id: "SESS-1001",
        role: "SUPER_ADMIN",
        roleName: "Superior Administrator",
        eventType: "LOGIN",
        timestamp: "18 Oct 2026, 09:30:15 AM IST",
        rawTimestamp: Date.now() - (7 * 3600 * 1000),
        ipAddress: "103.187.228.184",
        details: "Authenticated with Superior Master Security PIN.",
        duration: "Active Session"
    },
    {
        id: "SESS-1002",
        role: "OPERATIONS_OFFICER",
        roleName: "Operations Officer",
        eventType: "LOGOUT",
        timestamp: "18 Oct 2026, 11:45:20 AM IST",
        rawTimestamp: Date.now() - (5 * 3600 * 1000),
        ipAddress: "192.168.1.108",
        details: "Manual console lock executed by operator.",
        duration: "1h 15m 05s"
    },
    {
        id: "SESS-1003",
        role: "FINANCIAL_AUDITOR",
        roleName: "Financial Auditor",
        eventType: "AUTO_EXPIRED",
        timestamp: "18 Oct 2026, 01:20:44 PM IST",
        rawTimestamp: Date.now() - (3 * 3600 * 1000),
        ipAddress: "192.168.1.115",
        details: "Console session automatically expired due to 5 minutes of inactivity.",
        duration: "5m 00s"
    },
    {
        id: "SESS-1004",
        role: "SAFETY_DISPUTE_OFFICER",
        roleName: "Safety & Dispute Officer",
        eventType: "LOGIN",
        timestamp: "18 Oct 2026, 02:10:00 PM IST",
        rawTimestamp: Date.now() - (2 * 3600 * 1000),
        ipAddress: "103.187.228.184",
        details: "Authenticated with Safety Security PIN.",
        duration: "Active Session"
    },
    {
        id: "SESS-1005",
        role: "SAFETY_DISPUTE_OFFICER",
        roleName: "Safety & Dispute Officer",
        eventType: "LOGOUT",
        timestamp: "18 Oct 2026, 03:25:10 PM IST",
        rawTimestamp: Date.now() - (1 * 3600 * 1000),
        ipAddress: "103.187.228.184",
        details: "Manual console lock executed by Safety Officer.",
        duration: "1h 15m 10s"
    },
    {
        id: "SESS-1006",
        role: "SUPER_ADMIN",
        roleName: "Superior Administrator",
        eventType: "LOGIN",
        timestamp: "18 Oct 2026, 04:00:00 PM IST",
        rawTimestamp: Date.now() - (35 * 60 * 1000),
        ipAddress: "103.187.228.184",
        details: "Authenticated with Superior Master Security PIN.",
        duration: "Active Session"
    },
    {
        id: "SESS-1007",
        role: "SUPER_ADMIN",
        roleName: "Superior Administrator",
        eventType: "AUTO_EXPIRED",
        timestamp: "18 Oct 2026, 04:05:00 PM IST",
        rawTimestamp: Date.now() - (30 * 60 * 1000),
        ipAddress: "103.187.228.184",
        details: "Console session automatically expired due to 5 minutes of inactivity.",
        duration: "5m 00s"
    },
    {
        id: "SESS-1008",
        role: "OPERATIONS_OFFICER",
        roleName: "Operations Officer",
        eventType: "LOGIN",
        timestamp: "18 Oct 2026, 04:30:12 PM IST",
        rawTimestamp: Date.now() - (10 * 60 * 1000),
        ipAddress: "192.168.1.108",
        details: "Operational team member authenticated with Staff PIN.",
        duration: "Active Session"
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

const SEED_CUSTOMER_ACTIVITIES = [
    {
        id: "ACT-1001",
        timestamp: "Today, 1:40 PM",
        rawTimestamp: Date.now() - 1000 * 60 * 20,
        userEmail: "ananya.s@college.edu",
        userName: "Ananya Sharma",
        userRole: "Customer",
        section: "FEEDBACK",
        action: "FEEDBACK_SUBMITTED",
        title: "Submitted 5-Star Feedback",
        details: "Night Canteen Delivery feedback: 'Delivery from Night Canteen was under 15 minutes! The OTP security feature is great.'",
        status: "SUBMITTED",
        badgeColor: "bg-amber-100 text-amber-800"
    },
    {
        id: "ACT-1002",
        timestamp: "Today, 1:30 PM",
        rawTimestamp: Date.now() - 1000 * 60 * 30,
        userEmail: "student@college.edu",
        userName: "Arjun Patel",
        userRole: "Customer & Rider",
        section: "DISPUTE",
        action: "DISPUTE_FILED",
        title: "Dispute Case Filed: ORD-4029",
        details: "Filed dispute against Rider Vikram Singh. Reason: Item not delivered to Hostel 3 Room 204. Escrow: Rs. 110.",
        status: "OPEN_INVESTIGATION",
        badgeColor: "bg-rose-100 text-rose-800"
    },
    {
        id: "ACT-1003",
        timestamp: "Today, 1:15 PM",
        rawTimestamp: Date.now() - 1000 * 60 * 45,
        userEmail: "student@college.edu",
        userName: "Arjun Patel",
        userRole: "Customer & Rider",
        section: "ORDERS",
        action: "ORDER_PLACED",
        title: "Store Order Placed: ORD-4029",
        details: "Bourbon Biscuit x2, Coca Cola 300ml x1 | From Main Canteen Counter 2 to Hostel 3 | Bill: Rs. 95 | Delivery OTP: 4912",
        status: "RUNNER_ASSIGNED",
        badgeColor: "bg-sky-100 text-sky-800"
    },
    {
        id: "ACT-1004",
        timestamp: "Today, 1:05 PM",
        rawTimestamp: Date.now() - 1000 * 60 * 55,
        userEmail: "rahul.k@college.edu",
        userName: "Rahul Kumar",
        userRole: "Customer",
        section: "ORDERS",
        action: "ORDER_PLACED",
        title: "Store Order Placed: ORD-4030",
        details: "Lays Classic Salted x3, Amul Butter x1 | Night Canteen Tuck Shop to Girls Hostel 1 Gate | Delivery OTP: 8821",
        status: "PENDING_ACCEPTANCE",
        badgeColor: "bg-amber-100 text-amber-800"
    },
    {
        id: "ACT-1005",
        timestamp: "Today, 12:45 PM",
        rawTimestamp: Date.now() - 1000 * 60 * 75,
        userEmail: "student@college.edu",
        userName: "Arjun Patel",
        userRole: "Customer & Rider",
        section: "COURIER",
        action: "COURIER_CANCELLED",
        title: "Courier Transfer Cancelled: OrderNo-88392104",
        details: "Engineering Books Package from Campus Gate to Hostel 3 | Rs. 40 refunded to App Wallet",
        status: "CANCELLED",
        badgeColor: "bg-rose-100 text-rose-800"
    },
    {
        id: "ACT-1006",
        timestamp: "Today, 12:30 PM",
        rawTimestamp: Date.now() - 1000 * 60 * 90,
        userEmail: "ananya.s@college.edu",
        userName: "Ananya Sharma",
        userRole: "Customer",
        section: "CAB_POOL",
        action: "CAB_POOL_HOSTED",
        title: "Cab Pool Created: Main Gate ➔ Airport Terminal 2",
        details: "Departure: Tomorrow 06:00 AM | Total Seats: 4 (3 Booked) | Fare: Rs. 280/seat | Night Ride Protection Active",
        status: "CONFIRMED_RIDE",
        badgeColor: "bg-sky-100 text-sky-800"
    },
    {
        id: "ACT-1007",
        timestamp: "Today, 11:45 AM",
        rawTimestamp: Date.now() - 1000 * 60 * 135,
        userEmail: "aarav.sharma@college.edu",
        userName: "Aarav Sharma",
        userRole: "Customer",
        section: "CAB_POOL",
        action: "CAB_CANCELLED",
        title: "Cab Pool Cancelled: CAB-POOL-101",
        details: "Main Gate to Kempegowda Airport Terminal 1 & 2 pool cancelled by Host. All riders notified.",
        status: "CANCELLED",
        badgeColor: "bg-rose-100 text-rose-800"
    },
    {
        id: "ACT-1008",
        timestamp: "Today, 11:30 AM",
        rawTimestamp: Date.now() - 1000 * 60 * 150,
        userEmail: "sneha.k@college.edu",
        userName: "Sneha Kapoor",
        userRole: "Customer",
        section: "LOST_FOUND",
        action: "LOST_REPORTED",
        title: "Lost Item Reported: Calculus Engineering Textbook",
        details: "Black spiral notebook left in Academic Block 1, LH 104 back row. Contact: +91 9871122334",
        status: "ACTIVE",
        badgeColor: "bg-amber-100 text-amber-800"
    },
    {
        id: "ACT-1009",
        timestamp: "Today, 11:20 AM",
        rawTimestamp: Date.now() - 1000 * 60 * 160,
        userEmail: "siddharth.r@college.edu",
        userName: "Siddharth Roy",
        userRole: "Customer",
        section: "MEAL_CIRCLE",
        action: "MEAL_CIRCLE_HOSTED",
        title: "Meal Gathering Created: Dominos Pizza Outpost",
        details: "Meeting Point: Food Court Center Table @ Tonight, 8:00 PM | Buddies: 3/4 Joined",
        status: "OPEN",
        badgeColor: "bg-amber-100 text-amber-800"
    },
    {
        id: "ACT-1010",
        timestamp: "Today, 10:50 AM",
        rawTimestamp: Date.now() - 1000 * 60 * 190,
        userEmail: "student@college.edu",
        userName: "Arjun Patel",
        userRole: "Customer & Rider",
        section: "RENTAL",
        action: "ITEM_RENTED",
        title: "Rented: Engineering Drawing Set + Drafter",
        details: "Duration: 7 Days | Total Paid: Rs. 140 | Handover OTP: 6190 | Return OTP: 4401",
        status: "ACTIVE_BORROWED",
        badgeColor: "bg-emerald-100 text-emerald-800"
    },
    {
        id: "ACT-1011",
        timestamp: "Yesterday, 5:45 PM",
        rawTimestamp: Date.now() - 1000 * 60 * 60 * 20,
        userEmail: "tanmay.s@college.edu",
        userName: "Tanmay Sen",
        userRole: "Customer",
        section: "LOST_FOUND",
        action: "FOUND_REPORTED",
        title: "Found Item Deposited: Boat Airdopes Earbuds Case",
        details: "Found on concrete bench outside Library canteen. Deposited at Main Gate Security Desk.",
        status: "SECURITY_DEPOSITED",
        badgeColor: "bg-emerald-100 text-emerald-800"
    },
    {
        id: "ACT-1012",
        timestamp: "Yesterday, 2:15 PM",
        rawTimestamp: Date.now() - 1000 * 60 * 60 * 23,
        userEmail: "vikram.s@college.edu",
        userName: "Vikram Singh",
        userRole: "Rider",
        section: "RIDER",
        action: "DELIVERY_COMPLETED",
        title: "Order Delivered: ORD-4028",
        details: "Completed snack delivery from Day Canteen to Hostel 4 | Delivery fee earned: Rs. 25 credited to Runner Wallet",
        status: "DELIVERED",
        badgeColor: "bg-emerald-100 text-emerald-800"
    }
];
