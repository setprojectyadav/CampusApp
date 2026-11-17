// Campus Admin Console - Live Realtime Cloud Sync Engine
// Unified Multi-Section Realtime Cloud Bridge (ntfy.sh) + Firebase RTDB (campusapp-e8435)
// Fully Synchronizes: Orders, Store, Courier Transfers, Students, Riders, Cab Pools, Meal Circles, Rentals, Lost & Found, Disputes, and Feedback

const FIREBASE_CONFIG = {
    apiKey: "AIzaSyDoRPnj_t42bxqoEQG-XtGV6l6R_r9e9hs",
    authDomain: "campusapp-e8435.firebaseapp.com",
    databaseURL: "https://campusapp-e8435-default-rtdb.firebaseio.com",
    projectId: "campusapp-e8435",
    storageBucket: "campusapp-e8435.firebasestorage.app",
    messagingSenderId: "697758899175",
    appId: "1:697758899175:web:campusapp-admin"
};

const NTFY_TOPIC = "campusapp_orders_e8435_sync";
const NTFY_URL = `https://ntfy.sh/${NTFY_TOPIC}`;

class CampusFirebaseSync {
    constructor() {
        this.isInitialized = false;
        this.isConnected = false;
        this.lastSyncTime = null;
        this.db = null;
        this.ntfyEventSource = null;
        this.ntfyTopic = NTFY_TOPIC;
        this.ntfyUrl = NTFY_URL;
        this.processedMessageIds = new Set();
        this.pendingQueue = [];
        this.isPolling = false;

        // 1. Initialize Realtime Bridge (ntfy.sh - Accountless & Instant bi-directional sync across all sections)
        this.initNtfyBridge();

        // 2. Initialize Firebase Realtime Cloud (if available)
        this.initFirebase();

        // 3. Initial catch-up poll to ingest messages cached before connection
        this.pollRecentMessages(true);

        // 4. Background safety catch-up interval (every 7 seconds)
        this.pollInterval = setInterval(() => {
            this.pollRecentMessages(true);
        }, 7000);
    }

    initNtfyBridge() {
        try {
            const sseUrl = `${this.ntfyUrl}/sse`;
            console.log(`[RealtimeBridge] Connecting to live all-services SSE stream: ${sseUrl}`);

            if (this.ntfyEventSource) {
                try { this.ntfyEventSource.close(); } catch (e) {}
            }

            this.ntfyEventSource = new EventSource(sseUrl);

            this.ntfyEventSource.onopen = () => {
                this.isConnected = true;
                this.lastSyncTime = new Date().toLocaleTimeString();
                this.updateStatusBadge("Cloud Sync: Live", true);
                console.log("[RealtimeBridge] Connected to live channel:", this.ntfyTopic);
                // Ingest any messages cached prior to SSE open
                this.pollRecentMessages(true);
            };

            this.ntfyEventSource.onmessage = (event) => {
                try {
                    const raw = JSON.parse(event.data);
                    if (raw.event === "message" && raw.message) {
                        if (raw.id && this.processedMessageIds.has(raw.id)) {
                            return;
                        }
                        if (raw.id) this.processedMessageIds.add(raw.id);
                        const payload = JSON.parse(raw.message);
                        this.handleBridgeMessage(payload, raw.id, false);
                    }
                } catch (parseErr) {
                    console.debug("[RealtimeBridge] Non-JSON message:", event.data);
                }
            };

            this.ntfyEventSource.onerror = (err) => {
                console.warn("[RealtimeBridge] SSE connection interrupted, reconnecting...", err);
                if (!this.isConnectedViaFirebase) {
                    this.updateStatusBadge("Cloud Sync: Reconnecting...", false);
                }
            };
        } catch (e) {
            console.error("[RealtimeBridge] Init failed:", e);
        }
    }

    async pollRecentMessages(isBackground = false) {
        if (this.isPolling) return 0;
        this.isPolling = true;
        let count = 0;
        try {
            const url = `${this.ntfyUrl}/json?poll=1&since=10m`;
            const response = await fetch(url, { cache: "no-store" });
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const text = await response.text();
            if (text) {
                const lines = text.trim().split("\n");
                for (const line of lines) {
                    if (!line.trim()) continue;
                    try {
                        const raw = JSON.parse(line.trim());
                        if (raw.event === "message" && raw.message) {
                            if (raw.id && this.processedMessageIds.has(raw.id)) {
                                continue;
                            }
                            if (raw.id) this.processedMessageIds.add(raw.id);
                            const payload = JSON.parse(raw.message);
                            this.handleBridgeMessage(payload, raw.id, isBackground);
                            count++;
                        }
                    } catch (lineErr) {}
                }
            }
            if (count > 0) {
                console.log(`[RealtimeBridge] Polled and ingested ${count} cloud messages.`);
                this.refreshAdminViews();
            }
        } catch (e) {
            console.debug("[RealtimeBridge] Catch-up poll:", e.message || e);
        } finally {
            this.isPolling = false;
        }
        return count;
    }

    flushPendingQueue() {
        if (!window.app || !this.pendingQueue.length) return;
        console.log(`[RealtimeBridge] Flushing ${this.pendingQueue.length} queued messages to window.app...`);
        const queue = [...this.pendingQueue];
        this.pendingQueue = [];
        queue.forEach(item => {
            this.handleBridgeMessage(item.payload, item.msgId, item.isBackground);
        });
        this.refreshAdminViews();
    }

    refreshAdminViews() {
        if (!window.app) return;
        try {
            if (typeof window.app.renderCurrentTab === "function") {
                if ([0, 7, 8, 9].includes(window.app.selectedTab)) {
                    window.app.renderCurrentTab();
                } else if (window.app.selectedTab === 1) {
                    window.app.updateSectionGrid("orders");
                } else if (window.app.selectedTab === 2) {
                    window.app.updateSectionGrid("students");
                } else if (window.app.selectedTab === 3) {
                    window.app.updateSectionGrid("riders");
                } else if (window.app.selectedTab === 4) {
                    window.app.updateSectionGrid("services");
                } else if (window.app.selectedTab === 5) {
                    window.app.updateSectionGrid("disputes");
                } else if (window.app.selectedTab === 6) {
                    window.app.updateSectionGrid("lostFound");
                } else if (window.app.selectedTab === 10) {
                    window.app.updateSectionGrid("feedback");
                }
            }
            if (window.app.activeDossierEmail && typeof window.app.openUserActivityDossier === "function") {
                const modal = document.getElementById("modal-generic");
                if (modal && !modal.classList.contains("hidden")) {
                    window.app.openUserActivityDossier(window.app.activeDossierEmail, window.app.activeDossierSubTab);
                }
            }
            if (typeof lucide !== "undefined" && lucide.createIcons) {
                lucide.createIcons();
            }
        } catch (e) {
            console.warn("[RealtimeBridge] Refresh views error:", e);
        }
    }

    handleBridgeMessage(payload, msgId = null, isBackground = false) {
        if (!payload) return;
        if (typeof window.app === "undefined" || !window.app) {
            this.pendingQueue.push({ payload, msgId, isBackground });
            return;
        }
        this.lastSyncTime = new Date().toLocaleTimeString();

        // Prevent echoing our own Admin Web messages
        if (payload.source === "ADMIN_WEB") return;

        // 1. Order payload (Store Canteen orders & Courier Package Transfers)
        if (payload.type === "ORDER" || (payload.id && (payload.id.toUpperCase().startsWith("ORD") || payload.id.toUpperCase().startsWith("ORDER-") || payload.id.toUpperCase().startsWith("ORDERNO-") || payload.items || (payload.totalMax !== undefined && !payload.itemType && payload.type !== "RENTAL")))) {
            const incomingOrder = payload;
            const idx = window.app.orders.findIndex(o => o.id === incomingOrder.id);
            if (idx >= 0) {
                const prev = window.app.orders[idx];
                const prevStatus = prev ? prev.status : null;
                window.app.orders[idx] = { ...window.app.orders[idx], ...incomingOrder };
                if (incomingOrder.status && incomingOrder.status !== prevStatus) {
                    if (!isBackground) this.playOrderChime();
                    if (!isBackground && window.app.isUnlocked) {
                        const statusBadge = incomingOrder.status === "CANCELLED" ? "❌ CANCELLED" : incomingOrder.status;
                        window.app.showToast(`⚠️ Order/Courier ${incomingOrder.id} status updated: ${statusBadge}`, incomingOrder.status === "CANCELLED" ? "warning" : "info");
                    }
                }
            } else {
                window.app.orders.unshift(incomingOrder);
                if (!isBackground) this.playOrderChime();
                if (!isBackground && window.app.isUnlocked) {
                    const label = incomingOrder.pickPoint && incomingOrder.dropPoint ? `Order/Courier: ${incomingOrder.id}` : `New Live Order: ${incomingOrder.id}`;
                    window.app.showToast(`🔔 ${label} received from CampusApp`, "success");
                }
            }
            window.app.save("app_orders_list", window.app.orders);
            const pending = window.app.orders.filter(o => o.status !== "DELIVERED" && o.status !== "CANCELLED");
            const completed = window.app.orders.filter(o => o.status === "DELIVERED");
            const cancelled = window.app.orders.filter(o => o.status === "CANCELLED");
            window.app.save("db_pending_orders", pending);
            window.app.save("db_completed_orders", completed);
            window.app.save("db_cancelled_orders", cancelled);
            if (typeof window.app.recordCustomerActivity === "function") {
                const isCourier = !!(incomingOrder.pickPoint && incomingOrder.dropPoint && (incomingOrder.id.startsWith("OrderNo-") || incomingOrder.id.startsWith("CSV-")));
                window.app.recordCustomerActivity({
                    userEmail: incomingOrder.customerId || "student@college.edu",
                    userName: incomingOrder.customerName || "Campus Student",
                    section: isCourier ? "COURIER" : "ORDERS",
                    action: incomingOrder.status === "CANCELLED" ? (isCourier ? "COURIER_CANCELLED" : "ORDER_CANCELLED") : (incomingOrder.status === "DELIVERED" ? (isCourier ? "COURIER_DELIVERED" : "ORDER_DELIVERED") : (isCourier ? "COURIER_PLACED" : "ORDER_PLACED")),
                    title: (isCourier ? "Courier Package: " : "Store Order: ") + incomingOrder.id,
                    details: `${incomingOrder.pickPoint || 'Canteen'} ➔ ${incomingOrder.dropPoint || 'Hostel'} | Total: Rs. ${incomingOrder.totalMin || 0}-${incomingOrder.totalMax || 0} | Status: ${incomingOrder.status}`,
                    status: incomingOrder.status
                });
            }
            this.refreshAdminViews();
        }

        // 2. Student Profile payload
        else if (payload.type === "STUDENT" || (payload.email && payload.rollNo && !payload.itemType && payload.isOnline === undefined)) {
            const student = payload;
            const idx = window.app.students.findIndex(s => s.email === student.email || s.rollNo === student.rollNo);
            if (idx >= 0) {
                window.app.students[idx] = { ...window.app.students[idx], ...student };
            } else {
                window.app.students.unshift(student);
                if (!isBackground) this.playOrderChime();
                if (!isBackground && window.app.isUnlocked) {
                    window.app.showToast(`👤 Student Profile Synced: ${student.name || student.email}`, "success");
                }
            }
            window.app.save("campus_students_list", window.app.students);
            if (typeof window.app.recordCustomerActivity === "function") {
                window.app.recordCustomerActivity({
                    userEmail: student.email,
                    userName: student.name || student.email.split("@")[0],
                    section: "PROFILE",
                    action: "PROFILE_SYNC",
                    title: "Student Profile Synced: " + (student.name || student.email),
                    details: `Roll No: ${student.rollNo || 'N/A'} | Hostel: ${student.hostel || 'Hostel'} | Stream: ${student.stream || 'CS'} | Wallet: Rs. ${student.walletBalance || 0}`,
                    status: "ACTIVE"
                });
            }
            this.refreshAdminViews();
        }

        // 3. Rider Status payload
        else if (payload.type === "RIDER" || (payload.email && payload.isOnline !== undefined)) {
            const rider = payload;
            const idx = window.app.riders.findIndex(r => r.email === rider.email || (r.id && r.id === rider.id));
            if (idx >= 0) {
                window.app.riders[idx] = { ...window.app.riders[idx], ...rider };
            } else {
                window.app.riders.unshift(rider);
                if (!isBackground) this.playOrderChime();
                if (!isBackground && window.app.isUnlocked) {
                    window.app.showToast(`🛵 Rider Status Synced: ${rider.name} (${rider.isOnline ? "Online" : "Offline"})`, "info");
                }
            }
            window.app.save("campus_riders_list", window.app.riders);
            if (typeof window.app.recordCustomerActivity === "function") {
                window.app.recordCustomerActivity({
                    userEmail: rider.email,
                    userName: rider.name || rider.email.split("@")[0],
                    userRole: "Rider",
                    section: "RIDER",
                    action: rider.isOnline ? "RIDER_ONLINE" : "RIDER_OFFLINE",
                    title: `Rider Duty: ${rider.name} (${rider.isOnline ? 'Online' : 'Offline'})`,
                    details: `Active Job: ${rider.activeJobId || 'None'} | Deliveries: ${rider.totalDeliveries || 0} | Rating: ${rider.rating || '5.0'}`,
                    status: rider.isOnline ? "ONLINE" : "OFFLINE"
                });
            }
            this.refreshAdminViews();
        }

        // 3b. Account Deleted payload (DPDPA 2023 compliance)
        else if (payload.type === "ACCOUNT_DELETED" || payload.type === "USER_ACCOUNT_DELETED") {
            const userEmail = (payload.userEmail || payload.email || "").toLowerCase().trim();
            if (userEmail) {
                window.app.students = window.app.students.filter(s => (s.email || "").toLowerCase() !== userEmail);
                window.app.riders = window.app.riders.filter(r => (r.email || "").toLowerCase() !== userEmail);
                window.app.save("campus_students_list", window.app.students);
                window.app.save("campus_riders_list", window.app.riders);
                const prevDel = parseInt(window.app.load("campus_total_deleted_accounts", "0"), 10) || 0;
                window.app.save("campus_total_deleted_accounts", prevDel + 1);
                window.app.recordAudit("ACCOUNT_DELETED", userEmail, `User ${userEmail} executed permanent account erasure under DPDPA 2023.`, "WARNING");
                if (typeof window.app.recordCustomerActivity === "function") {
                    window.app.recordCustomerActivity({
                        userEmail: userEmail,
                        userName: payload.userName || userEmail.split("@")[0],
                        section: "PROFILE",
                        action: "ACCOUNT_DELETED",
                        title: "Account Permanently Deleted",
                        details: `User account deleted from campus directory under DPDPA erasure rights.`,
                        status: "DELETED"
                    });
                }
                this.refreshAdminViews();
            }
        }

        // 4. Campus Cab Pool payload
        else if (payload.type === "CAB_POOL" || (payload.id && payload.id.toUpperCase().startsWith("CAB-"))) {
            const cab = payload;
            const idx = window.app.cabPools.findIndex(c => c.id === cab.id);
            if (idx >= 0) {
                const prevStatus = window.app.cabPools[idx] ? window.app.cabPools[idx].status : null;
                window.app.cabPools[idx] = { ...window.app.cabPools[idx], ...cab };
                if (cab.status && cab.status !== prevStatus) {
                    if (!isBackground) this.playOrderChime();
                    if (!isBackground && window.app.isUnlocked) {
                        const statusBadge = cab.status === "CANCELLED" ? "❌ CANCELLED" : cab.status;
                        window.app.showToast(`⚠️ Cab Pool ${cab.id} status: ${statusBadge}`, cab.status === "CANCELLED" ? "warning" : "info");
                    }
                }
            } else {
                window.app.cabPools.unshift(cab);
                if (!isBackground) this.playOrderChime();
                if (!isBackground && window.app.isUnlocked) {
                    window.app.showToast(`🚗 New Cab Pool Created: ${cab.dropDestination || cab.id}`, "info");
                }
            }
            window.app.save("campus_cab_pools", window.app.cabPools);
            if (typeof window.app.recordCustomerActivity === "function") {
                window.app.recordCustomerActivity({
                    userEmail: cab.hostEmail || "host@college.edu",
                    userName: cab.hostName || "Cab Host",
                    section: "CAB_POOL",
                    action: cab.status === "CANCELLED" ? "CAB_CANCELLED" : "CAB_UPDATE",
                    title: `Cab Pool: ${cab.pickupPoint || 'Campus'} ➔ ${cab.dropDestination || cab.id}`,
                    details: `Departure: ${cab.departureDate || 'Today'} ${cab.departureTime || ''} | Seats: ${cab.bookedSeats || 1}/${cab.totalSeats || 4} | Fare: Rs. ${cab.perPersonFare || 0}/seat | Status: ${cab.status}`,
                    status: cab.status
                });
            }
            this.refreshAdminViews();
        }

        // 5. Meal Circle payload
        else if (payload.type === "MEAL_CIRCLE" || (payload.id && payload.id.toUpperCase().startsWith("MEAL-"))) {
            const meal = payload;
            const idx = window.app.mealCircles.findIndex(m => m.id === meal.id);
            if (idx >= 0) {
                const prevStatus = window.app.mealCircles[idx] ? window.app.mealCircles[idx].status : null;
                window.app.mealCircles[idx] = { ...window.app.mealCircles[idx], ...meal };
                if (meal.status && meal.status !== prevStatus) {
                    if (!isBackground) this.playOrderChime();
                    if (!isBackground && window.app.isUnlocked) {
                        const statusBadge = meal.status === "CANCELLED" ? "❌ CANCELLED" : meal.status;
                        window.app.showToast(`⚠️ Meal Circle ${meal.id} status: ${statusBadge}`, meal.status === "CANCELLED" ? "warning" : "info");
                    }
                }
            } else {
                window.app.mealCircles.unshift(meal);
                if (!isBackground) this.playOrderChime();
                if (!isBackground && window.app.isUnlocked) {
                    window.app.showToast(`🍕 New Meal Gathering Created: ${meal.restaurantSpot || meal.id}`, "info");
                }
            }
            window.app.save("campus_meal_gatherings", window.app.mealCircles);
            if (typeof window.app.recordCustomerActivity === "function") {
                window.app.recordCustomerActivity({
                    userEmail: meal.hostEmail || "host@college.edu",
                    userName: meal.hostName || "Meal Host",
                    section: "MEAL_CIRCLE",
                    action: meal.status === "CANCELLED" ? "MEAL_CANCELLED" : "MEAL_UPDATE",
                    title: `Meal Gathering: ${meal.restaurantSpot || meal.id}`,
                    details: `Category: ${meal.mealCategory || 'Fast Food'} | Meet: ${meal.meetingPoint || 'Campus'} @ ${meal.targetTime || ''} | Buddies: ${meal.joinedBuddies || 1}/${meal.totalBuddies || 4} | Status: ${meal.status}`,
                    status: meal.status
                });
            }
            this.refreshAdminViews();
        }

        // 6. Rental Item payload
        else if (payload.type === "RENTAL" || (payload.id && payload.id.toUpperCase().startsWith("RNT-"))) {
            const rnt = payload;
            const idx = window.app.rentals.findIndex(r => r.id === rnt.id);
            if (idx >= 0) {
                const prevStatus = window.app.rentals[idx] ? window.app.rentals[idx].status : null;
                window.app.rentals[idx] = { ...window.app.rentals[idx], ...rnt };
                if (rnt.status && rnt.status !== prevStatus) {
                    if (!isBackground) this.playOrderChime();
                    if (!isBackground && window.app.isUnlocked) {
                        const statusBadge = rnt.status === "CANCELLED" ? "❌ CANCELLED" : rnt.status;
                        window.app.showToast(`⚠️ Rental Item ${rnt.id} status: ${statusBadge}`, rnt.status === "CANCELLED" ? "warning" : "info");
                    }
                }
            } else {
                window.app.rentals.unshift(rnt);
                if (!isBackground) this.playOrderChime();
                if (!isBackground && window.app.isUnlocked) {
                    window.app.showToast(`📦 New Rental Item Listed: ${rnt.productName || rnt.id}`, "info");
                }
            }
            window.app.save("campus_rentals_list", window.app.rentals);
            if (typeof window.app.recordCustomerActivity === "function") {
                window.app.recordCustomerActivity({
                    userEmail: rnt.renterEmail || "renter@college.edu",
                    section: "RENTAL",
                    action: rnt.status === "CANCELLED" ? "RENTAL_CANCELLED" : (rnt.status === "RETURNED" ? "RENTAL_RETURNED" : "RENTAL_UPDATE"),
                    title: `Item Rental: ${rnt.productName || rnt.id}`,
                    details: `Duration: ${rnt.durationDays || 1} Days | Paid: Rs. ${rnt.totalPaid || 0} | Handover OTP: ${rnt.handoverOtp || '----'} | Status: ${rnt.status}`,
                    status: rnt.status
                });
            }
            this.refreshAdminViews();
        }

        // 7. Lost & Found payload
        else if (payload.type === "LOST_FOUND" || (payload.id && (payload.id.toUpperCase().startsWith("LF-") || payload.id.toUpperCase().startsWith("LNF-")))) {
            const item = {
                id: payload.id,
                type: payload.itemType || payload.type || "LOST",
                title: payload.title,
                category: payload.category || "Others",
                description: payload.description || "",
                location: payload.location || "Campus",
                date: payload.date || "Today",
                reporterName: payload.reporterName || "Anonymous Student",
                reporterEmail: payload.reporterEmail || "",
                reporterPhone: payload.reporterPhone || "",
                reporterRollNo: payload.reporterRollNo || "",
                status: payload.status || "ACTIVE",
                imageUrl: payload.imageUrl || payload.imageUri || null,
                photoBase64: payload.photoBase64 || payload.photo || null,
                images: payload.images || (payload.imageUrl ? [payload.imageUrl] : (payload.photoBase64 ? [payload.photoBase64] : [])),
                securityDeskNote: payload.securityDeskNote || null,
                claimedByRoll: payload.claimedByRoll || null,
                isFlagged: !!payload.isFlagged,
                adminTakedownReason: payload.adminTakedownReason || null
            };
            const idx = window.app.lostFound.findIndex(l => l.id === item.id);
            if (idx >= 0) {
                const prevStatus = window.app.lostFound[idx] ? window.app.lostFound[idx].status : null;
                window.app.lostFound[idx] = { ...window.app.lostFound[idx], ...item };
                if (item.status && item.status !== prevStatus) {
                    if (!isBackground) this.playOrderChime();
                    if (!isBackground && window.app.isUnlocked) {
                        window.app.showToast(`⚠️ Lost & Found ${item.id} status: ${item.status}`, "info");
                    }
                }
            } else {
                window.app.lostFound.unshift(item);
                if (!isBackground) this.playOrderChime();
                if (!isBackground && window.app.isUnlocked) {
                    window.app.showToast(`🔍 Lost & Found Item Reported: ${item.title}`, "warning");
                }
            }
            window.app.save("campus_lost_found_items", window.app.lostFound);
            if (typeof window.app.recordCustomerActivity === "function") {
                window.app.recordCustomerActivity({
                    userEmail: item.reporterEmail || "reporter@college.edu",
                    userName: item.reporterName || "Anonymous Student",
                    section: "LOST_FOUND",
                    action: item.type === "LOST" ? "LOST_REPORTED" : "FOUND_REPORTED",
                    title: `Lost & Found: ${item.title}`,
                    details: `Type: ${item.type} | Category: ${item.category} | Location: ${item.location} | Date: ${item.date} | Status: ${item.status}`,
                    status: item.status
                });
            }
            this.refreshAdminViews();
        }

        // 8. Dispute payload
        else if (payload.type === "DISPUTE" || (payload.id && payload.id.startsWith("DSP-"))) {
            const dispute = payload;
            const idx = window.app.disputes.findIndex(d => d.id === dispute.id);
            if (idx >= 0) {
                window.app.disputes[idx] = { ...window.app.disputes[idx], ...dispute };
            } else {
                window.app.disputes.unshift(dispute);
                if (!isBackground) this.playOrderChime();
                if (!isBackground && window.app.isUnlocked) {
                    window.app.showToast(`⚠️ Dispute Ticket Filed: ${dispute.id}`, "warning");
                }
            }
            window.app.save("admin_dispute_cases", window.app.disputes);
            if (typeof window.app.recordCustomerActivity === "function") {
                window.app.recordCustomerActivity({
                    userEmail: dispute.complainantEmail || "student@college.edu",
                    userName: dispute.complainantName || "Student Complainant",
                    section: "DISPUTE",
                    action: "DISPUTE_FILED",
                    title: `Dispute Case: ${dispute.id} (Order ${dispute.orderId || 'N/A'})`,
                    details: `Issue: ${dispute.issueCategory || 'Delivery'} | Description: ${dispute.description || ''} | Status: ${dispute.status}`,
                    status: dispute.status
                });
            }
            this.refreshAdminViews();
        }

        // 9. Feedback payload
        else if (payload.type === "FEEDBACK" || (payload.id && payload.id.startsWith("FB-"))) {
            const fb = payload;
            const idx = window.app.feedback.findIndex(f => f.id === fb.id);
            if (idx >= 0) {
                window.app.feedback[idx] = { ...window.app.feedback[idx], ...fb };
            } else {
                window.app.feedback.unshift(fb);
                if (!isBackground) this.playOrderChime();
                if (!isBackground && window.app.isUnlocked) {
                    window.app.showToast(`💬 Feedback Received from ${fb.userName || "Student"}`, "info");
                }
            }
            window.app.save("campus_user_feedback_list", window.app.feedback);
            if (typeof window.app.recordCustomerActivity === "function") {
                window.app.recordCustomerActivity({
                    userEmail: fb.userEmail || "user@college.edu",
                    userName: fb.userName || "Student",
                    section: "FEEDBACK",
                    action: "FEEDBACK_SUBMITTED",
                    title: `Feedback Submitted: ${fb.category || 'App Experience'} (${fb.rating || 5} Stars)`,
                    details: `Review: "${fb.message || ''}"`,
                    status: "SUBMITTED"
                });
            }
            this.refreshAdminViews();
        }
    }
    }

    initFirebase() {
        try {
            if (typeof firebase !== "undefined" && !firebase.apps.length) {
                firebase.initializeApp(FIREBASE_CONFIG);
                this.db = firebase.database();
                this.isInitialized = true;
                this.initConnectionMonitor();
                this.attachLiveListeners();
                console.log("[FirebaseSync] Attempting Firebase RTDB: campusapp-e8435");
            } else if (typeof firebase !== "undefined" && firebase.apps.length) {
                this.db = firebase.database();
                this.isInitialized = true;
                this.initConnectionMonitor();
                this.attachLiveListeners();
            }
        } catch (err) {
            console.warn("[FirebaseSync] Firebase initialization note:", err);
        }
    }

    initConnectionMonitor() {
        if (!this.db) return;
        try {
            const connectedRef = this.db.ref(".info/connected");
            connectedRef.on("value", (snap) => {
                this.isConnectedViaFirebase = snap.val() === true;
                if (this.isConnectedViaFirebase) {
                    this.isConnected = true;
                    this.lastSyncTime = new Date().toLocaleTimeString();
                    this.updateStatusBadge("Cloud Sync: Live (Firebase)", true);
                }
            });
        } catch (e) {
            console.warn("[FirebaseSync] Connection monitor error:", e);
        }
    }

    attachLiveListeners() {
        if (!this.db) return;

        try {
            this.db.ref("orders").on("value", (snapshot) => {
                const data = snapshot.val();
                if (data && typeof window.app !== "undefined") {
                    const cloudOrders = Object.values(data);
                    if (cloudOrders.length > 0) {
                        cloudOrders.forEach(co => {
                            const idx = window.app.orders.findIndex(o => o.id === co.id);
                            if (idx >= 0) {
                                window.app.orders[idx] = { ...window.app.orders[idx], ...co };
                            } else {
                                window.app.orders.unshift(co);
                                if (window.app.isUnlocked) {
                                    window.app.showToast(`New Live Order received from CampusApp: ${co.id}`, "success");
                                }
                            }
                        });
                        window.app.save("app_orders_list", window.app.orders, true);
                        const pending = window.app.orders.filter(o => o.status !== "DELIVERED" && o.status !== "CANCELLED");
                        const completed = window.app.orders.filter(o => o.status === "DELIVERED");
                        const cancelled = window.app.orders.filter(o => o.status === "CANCELLED");
                        window.app.save("db_pending_orders", pending, true);
                        window.app.save("db_completed_orders", completed, true);
                        window.app.save("db_cancelled_orders", cancelled, true);
                        if (typeof window.app.recordCustomerActivity === "function") {
                            cloudOrders.forEach(co => {
                                const isCourier = !!(co.pickPoint && co.dropPoint && (co.id.startsWith("OrderNo-") || co.id.startsWith("CSV-")));
                                window.app.recordCustomerActivity({
                                    userEmail: co.customerId || "student@college.edu",
                                    userName: co.customerName || "Campus Student",
                                    section: isCourier ? "COURIER" : "ORDERS",
                                    action: co.status === "CANCELLED" ? (isCourier ? "COURIER_CANCELLED" : "ORDER_CANCELLED") : (co.status === "DELIVERED" ? (isCourier ? "COURIER_DELIVERED" : "ORDER_DELIVERED") : (isCourier ? "COURIER_PLACED" : "ORDER_PLACED")),
                                    title: (isCourier ? "Courier Package: " : "Store Order: ") + co.id,
                                    details: `${co.pickPoint || 'Canteen'} ➔ ${co.dropPoint || 'Hostel'} | Total: Rs. ${co.totalMin || 0}-${co.totalMax || 0} | Status: ${co.status}`,
                                    status: co.status
                                });
                            });
                        }
                        this.refreshAdminViews();
                    }
                }
            });

            this.db.ref("students").on("value", (snapshot) => {
                const data = snapshot.val();
                if (data && typeof window.app !== "undefined") {
                    const cloudStudents = Object.values(data);
                    if (cloudStudents.length > 0) {
                        cloudStudents.forEach(cs => {
                            const csEmail = (cs.email || cs.id || "").toLowerCase().trim();
                            const idx = window.app.students.findIndex(s => {
                                const sEmail = (s.email || s.id || "").toLowerCase().trim();
                                return (sEmail && csEmail && sEmail === csEmail) || (s.rollNo && cs.rollNo && s.rollNo === cs.rollNo);
                            });
                            if (idx >= 0) {
                                window.app.students[idx] = { ...window.app.students[idx], ...cs };
                            } else {
                                window.app.students.unshift(cs);
                            }
                        });
                        window.app.save("campus_students_list", window.app.students, true);
                        if (typeof window.app.recordCustomerActivity === "function") {
                            cloudStudents.forEach(cs => {
                                window.app.recordCustomerActivity({
                                    userEmail: cs.email,
                                    userName: cs.name || cs.email.split("@")[0],
                                    section: "PROFILE",
                                    action: "PROFILE_SYNC",
                                    title: "Student Profile Synced: " + (cs.name || cs.email),
                                    details: `Roll No: ${cs.rollNo || 'N/A'} | Hostel: ${cs.hostel || 'Hostel'} | Stream: ${cs.stream || 'CS'} | Wallet: Rs. ${cs.walletBalance || 0}`,
                                    status: "ACTIVE"
                                });
                            });
                        }
                        this.refreshAdminViews();
                    }
                }
            });

            this.db.ref("riders").on("value", (snapshot) => {
                const data = snapshot.val();
                if (data && typeof window.app !== "undefined") {
                    const cloudRiders = Object.values(data);
                    if (cloudRiders.length > 0) {
                        cloudRiders.forEach(cr => {
                            const crEmail = (cr.email || cr.id || "").toLowerCase().trim();
                            const idx = window.app.riders.findIndex(r => {
                                const rEmail = (r.email || r.id || "").toLowerCase().trim();
                                return (rEmail && crEmail && rEmail === crEmail) || (r.id && cr.id && r.id === cr.id);
                            });
                            if (idx >= 0) {
                                window.app.riders[idx] = { ...window.app.riders[idx], ...cr };
                            } else {
                                window.app.riders.unshift(cr);
                            }
                        });
                        window.app.save("campus_riders_list", window.app.riders, true);
                        if (typeof window.app.recordCustomerActivity === "function") {
                            cloudRiders.forEach(cr => {
                                window.app.recordCustomerActivity({
                                    userEmail: cr.email,
                                    userName: cr.name || cr.email.split("@")[0],
                                    userRole: "Rider",
                                    section: "RIDER",
                                    action: cr.isOnline ? "RIDER_ONLINE" : "RIDER_OFFLINE",
                                    title: `Rider Duty: ${cr.name} (${cr.isOnline ? 'Online' : 'Offline'})`,
                                    details: `Active Job: ${cr.activeJobId || 'None'} | Deliveries: ${cr.totalDeliveries || 0} | Rating: ${cr.rating || '5.0'}`,
                                    status: cr.isOnline ? "ONLINE" : "OFFLINE"
                                });
                            });
                        }
                        this.refreshAdminViews();
                    }
                }
            });

            this.db.ref("disputes").on("value", (snapshot) => {
                const data = snapshot.val();
                if (data && typeof window.app !== "undefined") {
                    const cloudDisputes = Object.values(data);
                    if (cloudDisputes.length > 0) {
                        cloudDisputes.forEach(cd => {
                            const idx = window.app.disputes.findIndex(d => d.id === cd.id);
                            if (idx >= 0) {
                                window.app.disputes[idx] = { ...window.app.disputes[idx], ...cd };
                            } else {
                                window.app.disputes.unshift(cd);
                            }
                        });
                        window.app.save("admin_dispute_cases", window.app.disputes);
                        if (typeof window.app.recordCustomerActivity === "function") {
                            cloudDisputes.forEach(cd => {
                                window.app.recordCustomerActivity({
                                    userEmail: cd.complainantEmail || "student@college.edu",
                                    userName: cd.complainantName || "Student Complainant",
                                    section: "DISPUTE",
                                    action: "DISPUTE_FILED",
                                    title: `Dispute Case: ${cd.id} (Order ${cd.orderId || 'N/A'})`,
                                    details: `Issue: ${cd.issueCategory || 'Delivery'} | Description: ${cd.description || ''} | Status: ${cd.status}`,
                                    status: cd.status
                                });
                            });
                        }
                        if (window.app.selectedTab === 5) window.app.updateSectionGrid("disputes");
                    }
                }
            });
        } catch (e) {
            console.warn("[FirebaseSync] Listeners attach error:", e);
        }
    }

    // Outbound Cloud Sync: pushes update to ntfy Realtime Bridge & Firebase
    syncOrder(order) {
        if (!order || !order.id) return;
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify({ ...order, source: "ADMIN_WEB" })
            }).catch(e => console.debug("[RealtimeBridge] Post error:", e));
        } catch (e) {}

        if (this.db) {
            try {
                this.db.ref(`orders/${order.id.replace(/[.#$[\]]/g, "_")}`).set(order);
            } catch (e) {
                console.warn("[FirebaseSync] Outbound sync buffered:", e);
            }
        }
    }

    syncStudent(student) {
        if (!student) return;
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify({ ...student, type: "STUDENT", source: "ADMIN_WEB" })
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = (student.email || student.id || "student").replace(/[.#$[\]]/g, "_");
                this.db.ref(`students/${idKey}`).set(student);
            } catch (e) {
                console.warn("[FirebaseSync] Student sync buffered:", e);
            }
        }
    }

    syncRider(rider) {
        if (!rider) return;
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify({ ...rider, type: "RIDER", source: "ADMIN_WEB" })
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = (rider.email || rider.id || "rider").replace(/[.#$[\]]/g, "_");
                this.db.ref(`riders/${idKey}`).set(rider);
            } catch (e) {
                console.warn("[FirebaseSync] Rider sync buffered:", e);
            }
        }
    }

    syncLostFound(item) {
        if (!item || !item.id) return;
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify({ ...item, type: "LOST_FOUND", source: "ADMIN_WEB" })
            }).catch(() => {});
        } catch (e) {}
    }

    syncDispute(dispute) {
        if (!dispute || !dispute.id) return;
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify({ ...dispute, type: "DISPUTE", source: "ADMIN_WEB" })
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                this.db.ref(`disputes/${dispute.id.replace(/[.#$[\]]/g, "_")}`).set(dispute);
            } catch (e) {
                console.warn("[FirebaseSync] Dispute sync buffered:", e);
            }
        }
    }

    syncWarning({ userEmail, warningMessage }) {
        if (!userEmail) return;
        const payload = {
            type: "ACCOUNT_WARNING",
            source: "ADMIN_WEB",
            userEmail: userEmail.toLowerCase().trim(),
            warningMessage: warningMessage,
            timestamp: new Date().toLocaleTimeString()
        };
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify(payload)
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = userEmail.replace(/[.#$[\]]/g, "_");
                this.db.ref(`warnings/${idKey}`).set(payload);
            } catch (e) {}
        }
    }

    syncStudentBlock({ userEmail, blockDurationHours = 12, blockReason = "Administrative Lock", studentStatus = "TEMP_BLOCKED" }) {
        if (!userEmail) return;
        const payload = {
            type: "STUDENT_BLOCKED",
            source: "ADMIN_WEB",
            userEmail: userEmail.toLowerCase().trim(),
            studentStatus: studentStatus,
            blockHours: blockDurationHours,
            blockUntil: Date.now() + (blockDurationHours * 3600 * 1000),
            blockReason: blockReason,
            timestamp: new Date().toLocaleTimeString()
        };
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify(payload)
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = userEmail.replace(/[.#$[\]]/g, "_");
                this.db.ref(`student_locks/${idKey}`).set(payload);
            } catch (e) {}
        }
    }

    syncStudentUnblock({ userEmail }) {
        if (!userEmail) return;
        const payload = {
            type: "STUDENT_UNBLOCKED",
            source: "ADMIN_WEB",
            userEmail: userEmail.toLowerCase().trim(),
            studentStatus: "ACTIVE",
            timestamp: new Date().toLocaleTimeString()
        };
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify(payload)
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = userEmail.replace(/[.#$[\]]/g, "_");
                this.db.ref(`student_locks/${idKey}`).remove();
            } catch (e) {}
        }
    }

    syncRiderBlock({ userEmail, blockDurationHours = 12, blockReason = "Rider Duty Suspension", riderStatus = "TEMP_BLOCKED" }) {
        if (!userEmail) return;
        const payload = {
            type: "RIDER_BLOCKED",
            source: "ADMIN_WEB",
            userEmail: userEmail.toLowerCase().trim(),
            riderStatus: riderStatus,
            blockHours: blockDurationHours,
            blockUntil: Date.now() + (blockDurationHours * 3600 * 1000),
            blockReason: blockReason,
            timestamp: new Date().toLocaleTimeString()
        };
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify(payload)
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = userEmail.replace(/[.#$[\]]/g, "_");
                this.db.ref(`rider_locks/${idKey}`).set(payload);
            } catch (e) {}
        }
    }

    syncRiderUnblock({ userEmail }) {
        if (!userEmail) return;
        const payload = {
            type: "RIDER_UNBLOCKED",
            source: "ADMIN_WEB",
            userEmail: userEmail.toLowerCase().trim(),
            riderStatus: "ACTIVE",
            timestamp: new Date().toLocaleTimeString()
        };
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify(payload)
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = userEmail.replace(/[.#$[\]]/g, "_");
                this.db.ref(`rider_locks/${idKey}`).remove();
            } catch (e) {}
        }
    }

    syncWallet({ userEmail, walletBalance }) {
        if (!userEmail) return;
        const payload = {
            type: "WALLET_UPDATE",
            source: "ADMIN_WEB",
            userEmail: userEmail.toLowerCase().trim(),
            walletBalance: walletBalance,
            timestamp: new Date().toLocaleTimeString()
        };
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify(payload)
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = userEmail.replace(/[.#$[\]]/g, "_");
                this.db.ref(`wallets/${idKey}`).set(payload);
            } catch (e) {}
        }
    }

    syncProfileUpdate(payload) {
        if (!payload || !payload.userEmail) return;
        const fullPayload = {
            ...payload,
            type: "ACCOUNT_OVERRIDE",
            source: "ADMIN_WEB",
            userEmail: payload.userEmail.toLowerCase().trim(),
            timestamp: new Date().toLocaleTimeString()
        };
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify(fullPayload)
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = payload.userEmail.replace(/[.#$[\]]/g, "_");
                this.db.ref(`profiles/${idKey}`).update(fullPayload);
                this.db.ref(`account_overrides/${idKey}`).set(fullPayload);
            } catch (e) {}
        }
    }

    syncAccountOverride(payload) {
        this.syncProfileUpdate(payload);
    }

    syncForceSessionReset({ userEmail, reason = "Administrative Master Reset" }) {
        if (!userEmail) return;
        const payload = {
            type: "FORCE_SESSION_RESET",
            source: "ADMIN_WEB",
            userEmail: userEmail.toLowerCase().trim(),
            reason: reason,
            timestamp: new Date().toLocaleTimeString()
        };
        try {
            fetch(this.ntfyUrl, {
                method: "POST",
                headers: { "Content-Type": "text/plain" },
                body: JSON.stringify(payload)
            }).catch(() => {});
        } catch (e) {}

        if (this.db) {
            try {
                const idKey = userEmail.replace(/[.#$[\]]/g, "_");
                this.db.ref(`session_resets/${idKey}`).set(payload);
            } catch (e) {}
        }
    }

    // Push all current local seeds into Realtime Bridge & Firebase Cloud
    pushAllLocalToCloud() {
        if (!window.app) return false;
        try {
            // Push active orders
            if (window.app.orders && window.app.orders.length) {
                window.app.orders.slice(0, 10).forEach(o => {
                    fetch(this.ntfyUrl, {
                        method: "POST",
                        headers: { "Content-Type": "text/plain" },
                        body: JSON.stringify({ ...o, source: "ADMIN_WEB" })
                    }).catch(() => {});
                });
            }

            // Push students
            if (window.app.students && window.app.students.length) {
                window.app.students.slice(0, 5).forEach(s => {
                    fetch(this.ntfyUrl, {
                        method: "POST",
                        headers: { "Content-Type": "text/plain" },
                        body: JSON.stringify({ ...s, type: "STUDENT", source: "ADMIN_WEB" })
                    }).catch(() => {});
                });
            }

            // Push riders
            if (window.app.riders && window.app.riders.length) {
                window.app.riders.slice(0, 5).forEach(r => {
                    fetch(this.ntfyUrl, {
                        method: "POST",
                        headers: { "Content-Type": "text/plain" },
                        body: JSON.stringify({ ...r, type: "RIDER", source: "ADMIN_WEB" })
                    }).catch(() => {});
                });
            }

            // Push cab pools
            if (window.app.cabPools && window.app.cabPools.length) {
                window.app.cabPools.slice(0, 5).forEach(c => {
                    fetch(this.ntfyUrl, {
                        method: "POST",
                        headers: { "Content-Type": "text/plain" },
                        body: JSON.stringify({ ...c, type: "CAB_POOL", source: "ADMIN_WEB" })
                    }).catch(() => {});
                });
            }

            // Push meal circles
            if (window.app.mealCircles && window.app.mealCircles.length) {
                window.app.mealCircles.slice(0, 5).forEach(m => {
                    fetch(this.ntfyUrl, {
                        method: "POST",
                        headers: { "Content-Type": "text/plain" },
                        body: JSON.stringify({ ...m, type: "MEAL_CIRCLE", source: "ADMIN_WEB" })
                    }).catch(() => {});
                });
            }

            // Push rentals
            if (window.app.rentals && window.app.rentals.length) {
                window.app.rentals.slice(0, 5).forEach(rnt => {
                    fetch(this.ntfyUrl, {
                        method: "POST",
                        headers: { "Content-Type": "text/plain" },
                        body: JSON.stringify({ ...rnt, type: "RENTAL", source: "ADMIN_WEB" })
                    }).catch(() => {});
                });
            }

            // Push Lost & Found
            if (window.app.lostFound && window.app.lostFound.length) {
                window.app.lostFound.slice(0, 5).forEach(lf => {
                    fetch(this.ntfyUrl, {
                        method: "POST",
                        headers: { "Content-Type": "text/plain" },
                        body: JSON.stringify({ ...lf, type: "LOST_FOUND", source: "ADMIN_WEB" })
                    }).catch(() => {});
                });
            }

            // Push to Firebase if db available
            if (this.db) {
                const updates = {};
                window.app.orders.forEach(o => { updates[`orders/${o.id.replace(/[.#$[\]]/g, "_")}`] = o; });
                window.app.students.forEach(s => { updates[`students/${(s.email || s.id).replace(/[.#$[\]]/g, "_")}`] = s; });
                window.app.riders.forEach(r => { updates[`riders/${(r.email || r.id).replace(/[.#$[\]]/g, "_")}`] = r; });
                window.app.disputes.forEach(d => { updates[`disputes/${d.id.replace(/[.#$[\]]/g, "_")}`] = d; });
                this.db.ref().update(updates);
            }
            return true;
        } catch (e) {
            console.error("[RealtimeBridge] Push all error:", e);
            return false;
        }
    }

    updateStatusBadge(text, isLive) {
        const textEl = document.getElementById("cloud-sync-status-text");
        const container = document.getElementById("topbar-cloud-sync");
        if (textEl) textEl.textContent = text;
        if (container) {
            if (isLive) {
                container.className = "hidden sm:inline-flex items-center gap-1.5 h-8 px-2.5 rounded-xl bg-emerald-950/60 border border-emerald-500/30 text-emerald-300 text-xs font-mono font-medium cursor-pointer hover:bg-emerald-900/60 transition shadow-xs shrink-0 whitespace-nowrap";
            } else {
                container.className = "hidden sm:inline-flex items-center gap-1.5 h-8 px-2.5 rounded-xl bg-amber-950/60 border border-amber-500/30 text-amber-300 text-xs font-mono font-medium cursor-pointer hover:bg-amber-900/60 transition shadow-xs shrink-0 whitespace-nowrap";
            }
        }
    }

    playOrderChime() {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.type = "sine";
            osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
            osc.frequency.setValueAtTime(880, ctx.currentTime + 0.08); // A5
            gain.gain.setValueAtTime(0.2, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.35);
            osc.start();
            osc.stop(ctx.currentTime + 0.35);
        } catch (e) {}
    }
}

// Global Cloud Sync instance
window.cloudSync = new CampusFirebaseSync();
