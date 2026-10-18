// Campus Admin Console - Live Firebase Cloud Sync Engine
// Connects CampusAdminWeb with CampusApp (Android) via project campusapp-e8435

const FIREBASE_CONFIG = {
    apiKey: "AIzaSyDoRPnj_t42bxqoEQG-XtGV6l6R_r9e9hs",
    authDomain: "campusapp-e8435.firebaseapp.com",
    databaseURL: "https://campusapp-e8435-default-rtdb.firebaseio.com",
    projectId: "campusapp-e8435",
    storageBucket: "campusapp-e8435.firebasestorage.app",
    messagingSenderId: "697758899175",
    appId: "1:697758899175:web:campusapp-admin"
};

class CampusFirebaseSync {
    constructor() {
        this.isInitialized = false;
        this.isConnected = false;
        this.lastSyncTime = null;
        this.db = null;
        this.initFirebase();
    }

    initFirebase() {
        try {
            if (typeof firebase !== "undefined" && !firebase.apps.length) {
                firebase.initializeApp(FIREBASE_CONFIG);
                this.db = firebase.database();
                this.isInitialized = true;
                this.initConnectionMonitor();
                this.attachLiveListeners();
                console.log("[FirebaseSync] Connected to Firebase Realtime Cloud: campusapp-e8435");
            } else if (typeof firebase !== "undefined" && firebase.apps.length) {
                this.db = firebase.database();
                this.isInitialized = true;
                this.initConnectionMonitor();
                this.attachLiveListeners();
            } else {
                console.warn("[FirebaseSync] Firebase SDK not loaded, operating in local-persistence buffer.");
            }
        } catch (err) {
            console.error("[FirebaseSync] Initialization error:", err);
            this.updateStatusBadge("Cloud Offline (Local Buffer)", false);
        }
    }

    initConnectionMonitor() {
        if (!this.db) return;
        const connectedRef = this.db.ref(".info/connected");
        connectedRef.on("value", (snap) => {
            this.isConnected = snap.val() === true;
            this.lastSyncTime = new Date().toLocaleTimeString();
            if (this.isConnected) {
                this.updateStatusBadge("Cloud Sync: Live (campusapp-e8435)", true);
            } else {
                this.updateStatusBadge("Cloud Sync: Connecting...", false);
            }
        });
    }

    attachLiveListeners() {
        if (!this.db) return;

        // 1. Live Orders sync from Android App
        this.db.ref("orders").on("value", (snapshot) => {
            const data = snapshot.val();
            if (data && typeof window.app !== "undefined") {
                const cloudOrders = Object.values(data);
                if (cloudOrders.length > 0) {
                    // Merge cloud orders with local orders without duplicates
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
                    window.app.save("app_orders_list", window.app.orders);
                    if (window.app.selectedTab === 1) window.app.updateSectionGrid("orders");
                }
            }
        });

        // 2. Live Student Status sync
        this.db.ref("students").on("value", (snapshot) => {
            const data = snapshot.val();
            if (data && typeof window.app !== "undefined") {
                const cloudStudents = Object.values(data);
                if (cloudStudents.length > 0) {
                    cloudStudents.forEach(cs => {
                        const idx = window.app.students.findIndex(s => s.email === cs.email || s.id === cs.id);
                        if (idx >= 0) {
                            window.app.students[idx] = { ...window.app.students[idx], ...cs };
                        }
                    });
                    window.app.save("campus_students_list", window.app.students);
                    if (window.app.selectedTab === 2) window.app.updateSectionGrid("students");
                }
            }
        });

        // 3. Live Rider Status sync
        this.db.ref("riders").on("value", (snapshot) => {
            const data = snapshot.val();
            if (data && typeof window.app !== "undefined") {
                const cloudRiders = Object.values(data);
                if (cloudRiders.length > 0) {
                    cloudRiders.forEach(cr => {
                        const idx = window.app.riders.findIndex(r => r.id === cr.id);
                        if (idx >= 0) {
                            window.app.riders[idx] = { ...window.app.riders[idx], ...cr };
                        }
                    });
                    window.app.save("campus_riders_list", window.app.riders);
                    if (window.app.selectedTab === 3) window.app.updateSectionGrid("riders");
                }
            }
        });

        // 4. Live Disputes sync
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
                    if (window.app.selectedTab === 5) window.app.updateSectionGrid("disputes");
                }
            }
        });
    }

    // Outbound Cloud Sync: pushes update to Firebase so Android app gets it in real-time
    syncOrder(order) {
        if (!this.db || !order || !order.id) return;
        try {
            this.db.ref(`orders/${order.id.replace(/[.#$[\]]/g, "_")}`).set(order);
        } catch (e) {
            console.warn("[FirebaseSync] Outbound sync buffered:", e);
        }
    }

    syncStudent(student) {
        if (!this.db || !student || !student.id) return;
        try {
            this.db.ref(`students/${student.id.replace(/[.#$[\]]/g, "_")}`).set(student);
        } catch (e) {
            console.warn("[FirebaseSync] Student sync buffered:", e);
        }
    }

    syncRider(rider) {
        if (!this.db || !rider || !rider.id) return;
        try {
            this.db.ref(`riders/${rider.id.replace(/[.#$[\]]/g, "_")}`).set(rider);
        } catch (e) {
            console.warn("[FirebaseSync] Rider sync buffered:", e);
        }
    }

    syncDispute(dispute) {
        if (!this.db || !dispute || !dispute.id) return;
        try {
            this.db.ref(`disputes/${dispute.id.replace(/[.#$[\]]/g, "_")}`).set(dispute);
        } catch (e) {
            console.warn("[FirebaseSync] Dispute sync buffered:", e);
        }
    }

    // Push all current local seeds into Firebase Cloud so cloud database is populated
    pushAllLocalToCloud() {
        if (!this.db || !window.app) return false;
        try {
            const updates = {};
            window.app.orders.forEach(o => { updates[`orders/${o.id.replace(/[.#$[\]]/g, "_")}`] = o; });
            window.app.students.forEach(s => { updates[`students/${s.id.replace(/[.#$[\]]/g, "_")}`] = s; });
            window.app.riders.forEach(r => { updates[`riders/${r.id.replace(/[.#$[\]]/g, "_")}`] = r; });
            window.app.disputes.forEach(d => { updates[`disputes/${d.id.replace(/[.#$[\]]/g, "_")}`] = d; });
            this.db.ref().update(updates);
            return true;
        } catch (e) {
            console.error("[FirebaseSync] Push all error:", e);
            return false;
        }
    }

    updateStatusBadge(text, isLive) {
        const textEl = document.getElementById("cloud-sync-status-text");
        const container = document.getElementById("topbar-cloud-sync");
        if (textEl) textEl.textContent = text;
        if (container) {
            if (isLive) {
                container.className = "hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-950/60 border border-emerald-500/30 text-emerald-300 text-xs font-mono font-medium cursor-pointer hover:bg-emerald-900/60 transition shadow-sm";
            } else {
                container.className = "hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-amber-950/60 border border-amber-500/30 text-amber-300 text-xs font-mono font-medium cursor-pointer hover:bg-amber-900/60 transition shadow-sm";
            }
        }
    }
}

// Global Cloud Sync instance
window.cloudSync = new CampusFirebaseSync();
