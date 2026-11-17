// Campus Operations Console - Core Web Application Engine
// Complete 1:1 Implementation of AdminMainActivity.kt with 100% Feature Parity & Non-Blocking Universal Search

class CampusAdminApp {
    constructor() {
        this.activeRole = localStorage.getItem("admin_active_role") || "SUPER_ADMIN";
        this.selectedTab = 0;
        this.isUnlocked = sessionStorage.getItem("admin_is_unlocked") === "true";
        this.searchQueries = {
            orders: "",
            students: "",
            riders: "",
            services: "",
            servicesType: "ALL", // ALL, CABS, MEALS, RENTALS
            disputes: "",
            disputesTab: "OPEN", // OPEN, RESOLVED, ALL
            lostFound: "",
            fraud: "",
            audit: "",
            sessions: "",
            reports: "",
            feedback: "",
            feedbackSubTab: 0, // 0: Suggestions, 1: Roadmap
            customerActivities: "",
            overviewActivities: ""
        };
        this.customerActivitiesFilter = "ALL";
        this.activeDossierEmail = null;
        this.activeDossierSubTab = 0;
        this.severityFilter = "ALL";
        this.auditFilters = {
            dateFrom: "",
            dateTo: "",
            severity: "ALL",
            role: "ALL"
        };
        this.reportsFilters = {
            dateFrom: "",
            dateTo: "",
            status: "ALL"
        };
        this.ordersFilters = {
            dateFrom: "",
            dateTo: "",
            status: "ALL",
            orderType: "ALL"
        };
        this.studentsFilters = {
            status: "ALL",
            hostel: "ALL",
            stream: "ALL"
        };
        this.ridersFilters = {
            duty: "ALL",
            verification: "ALL",
            status: "ALL"
        };
        this.servicesFilters = {
            type: "ALL",
            status: "ALL"
        };
        this.disputesFilters = {
            tab: "OPEN",
            value: "ALL"
        };
        this.lostFoundFilters = {
            type: "ALL",
            category: "ALL",
            status: "ALL",
            media: "ALL"
        };
        this.fraudFilters = {
            risk: "ALL",
            type: "ALL",
            status: "ALL"
        };
        this.feedbackFilters = {
            category: "ALL",
            taskStatus: "ALL"
        };
        this.sessionTimeoutMs = 5 * 60 * 1000; // 5 Minutes Inactivity Timeout
        this.lastUserActivity = Date.now();
        this.currentSessionStartTime = Date.now();
        this.auditSubTab = 0; // 0: General Audit Trail, 1: Login & Logout Data, 2: Customer Activities Stream
        this.sessionFilters = {
            dateFrom: "",
            dateTo: "",
            eventType: "ALL",
            role: "ALL"
        };
        this.superAdminPin = localStorage.getItem("admin_security_pin") || "4656"; // Superior Admin Master PIN (4656)
        this.opsStaffPin = localStorage.getItem("admin_ops_pin") || "1234"; // Operational Team PIN (1234)
        this.financialAuditorPin = localStorage.getItem("admin_fin_pin") || "5678"; // Financial Auditor PIN (5678)
        this.safetyDisputePin = localStorage.getItem("admin_safety_pin") || "1011"; // Safety & Dispute PIN (1011)
        this.currentPin = this.superAdminPin;
        this.failedAttempts = parseInt(localStorage.getItem("admin_failed_attempts") || "0", 10);
        this.lockoutUntil = parseInt(localStorage.getItem("admin_lockout_until") || "0", 10);
        this.alertEmail = "vishalsharma782635@gmail.com";
        this.clientIp = "Detecting IP...";

        this.initData();
        this.initDOM();
        this.fetchClientIp();
        this.startTimerLoop();
        this.render();
    }

    initData() {
        const getOrSet = (key, fallback) => {
            const stored = localStorage.getItem(key);
            if (stored) {
                try { return JSON.parse(stored); } catch (e) { return fallback; }
            }
            localStorage.setItem(key, JSON.stringify(fallback));
            return fallback;
        };

        this.orders = getOrSet("app_orders_list", SEED_ORDERS);
        this.students = getOrSet("campus_students_list", SEED_STUDENTS);
        this.riders = getOrSet("campus_riders_list", SEED_RIDERS);
        this.cabPools = getOrSet("campus_cab_pools", SEED_CAB_POOLS);
        this.mealCircles = getOrSet("campus_meal_gatherings", SEED_MEAL_CIRCLES);
        this.rentals = getOrSet("campus_rentals_list", SEED_RENTALS);
        this.disputes = getOrSet("admin_dispute_cases", SEED_DISPUTES);
        this.lostFound = getOrSet("campus_lost_found_items", SEED_LOST_FOUND);
        if (Array.isArray(this.lostFound)) {
            let updatedLf = false;
            this.lostFound.forEach(item => {
                const seedMatch = typeof SEED_LOST_FOUND !== "undefined" ? SEED_LOST_FOUND.find(s => s.id === item.id) : null;
                if (seedMatch) {
                    if (!item.imageUrl && seedMatch.imageUrl) { item.imageUrl = seedMatch.imageUrl; updatedLf = true; }
                    if ((!item.images || item.images.length === 0) && seedMatch.images) { item.images = seedMatch.images; updatedLf = true; }
                }
            });
            if (typeof SEED_LOST_FOUND !== "undefined") {
                SEED_LOST_FOUND.forEach(s => {
                    if (!this.lostFound.some(l => l.id === s.id)) {
                        this.lostFound.push(s);
                        updatedLf = true;
                    }
                });
            }
            if (updatedLf) {
                localStorage.setItem("campus_lost_found_items", JSON.stringify(this.lostFound));
            }
        }
        this.fraudAnomalies = getOrSet("admin_fraud_anomalies", SEED_FRAUD_ANOMALIES);
        this.auditLogs = getOrSet("admin_audit_trail_log", SEED_AUDIT_LOGS);
        this.sessionLogs = getOrSet("admin_session_access_logs", SEED_SESSION_LOGS);
        this.feedback = getOrSet("campus_user_feedback_list", SEED_FEEDBACK);
        this.systemConfig = getOrSet("campus_system_config", DEFAULT_SYSTEM_CONFIG);
        this.customerActivities = getOrSet("campus_customer_activities", typeof SEED_CUSTOMER_ACTIVITIES !== "undefined" ? SEED_CUSTOMER_ACTIVITIES : []);

        // Synchronize and initialize persistent chat transcripts across all sections
        const syncCollectionChatLogs = (collection, seedCollection, storageKey) => {
            if (!Array.isArray(collection) || !Array.isArray(seedCollection)) return;
            let updated = false;
            collection.forEach(item => {
                if (!Array.isArray(item.chatLogs) || item.chatLogs.length === 0) {
                    const seedMatch = seedCollection.find(s => s.id === item.id);
                    if (seedMatch && Array.isArray(seedMatch.chatLogs) && seedMatch.chatLogs.length > 0) {
                        item.chatLogs = JSON.parse(JSON.stringify(seedMatch.chatLogs));
                        updated = true;
                    } else if (!Array.isArray(item.chatLogs)) {
                        item.chatLogs = [];
                        updated = true;
                    }
                }
            });
            if (updated) {
                localStorage.setItem(storageKey, JSON.stringify(collection));
            }
        };

        syncCollectionChatLogs(this.orders, typeof SEED_ORDERS !== "undefined" ? SEED_ORDERS : [], "app_orders_list");
        syncCollectionChatLogs(this.cabPools, typeof SEED_CAB_POOLS !== "undefined" ? SEED_CAB_POOLS : [], "campus_cab_pools");
        syncCollectionChatLogs(this.mealCircles, typeof SEED_MEAL_CIRCLES !== "undefined" ? SEED_MEAL_CIRCLES : [], "campus_meal_gatherings");
        syncCollectionChatLogs(this.rentals, typeof SEED_RENTALS !== "undefined" ? SEED_RENTALS : [], "campus_rentals_list");
        syncCollectionChatLogs(this.disputes, typeof SEED_DISPUTES !== "undefined" ? SEED_DISPUTES : [], "admin_dispute_cases");
        syncCollectionChatLogs(this.lostFound, typeof SEED_LOST_FOUND !== "undefined" ? SEED_LOST_FOUND : [], "campus_lost_found_items");
    }

    save(key, data, skipCloud = false) {
        localStorage.setItem(key, JSON.stringify(data));
        // Automatic Realtime Cloud Synchronization with Firebase campusapp-e8435
        if (!skipCloud && window.cloudSync && window.cloudSync.db) {
            try {
                if (key === "app_orders_list") {
                    const updates = {};
                    (data || []).forEach(o => {
                        const rawId = (o && (o.id || o.orderId)) || '';
                        if (rawId) updates[rawId.replace(/[.#$[\]]/g, "_")] = o;
                    });
                    if (Object.keys(updates).length > 0) {
                        window.cloudSync.db.ref("orders").update(updates);
                    }
                } else if (key === "campus_students_list") {
                    const updates = {};
                    (data || []).forEach(s => {
                        const rawId = (s && (s.id || s.email || s.rollNo)) || '';
                        if (rawId) updates[rawId.replace(/[.#$[\]]/g, "_")] = s;
                    });
                    if (Object.keys(updates).length > 0) {
                        window.cloudSync.db.ref("students").update(updates);
                    }
                } else if (key === "campus_riders_list") {
                    const updates = {};
                    (data || []).forEach(r => {
                        const rawId = (r && (r.id || r.email || r.riderId)) || '';
                        if (rawId) updates[rawId.replace(/[.#$[\]]/g, "_")] = r;
                    });
                    if (Object.keys(updates).length > 0) {
                        window.cloudSync.db.ref("riders").update(updates);
                    }
                } else if (key === "admin_dispute_cases") {
                    const updates = {};
                    (data || []).forEach(d => {
                        const rawId = (d && (d.id || d.caseId)) || '';
                        if (rawId) updates[rawId.replace(/[.#$[\]]/g, "_")] = d;
                    });
                    if (Object.keys(updates).length > 0) {
                        window.cloudSync.db.ref("disputes").update(updates);
                    }
                } else if (key === "admin_audit_trail_log") {
                    const latest = (data || [])[0];
                    if (latest && latest.id) {
                        window.cloudSync.db.ref(`audit_trail/${latest.id.replace(/[.#$[\]]/g, "_")}`).set(latest);
                    }
                } else if (key === "campus_customer_activities") {
                    const latest = (data || [])[0];
                    if (latest && latest.id) {
                        window.cloudSync.db.ref(`customer_activities/${latest.id.replace(/[.#$[\]]/g, "_")}`).set(latest);
                    }
                } else if (key === "admin_session_access_logs") {
                    const latest = (data || [])[0];
                    if (latest && latest.id) {
                        window.cloudSync.db.ref(`session_logs/${latest.id.replace(/[.#$[\]]/g, "_")}`).set(latest);
                    }
                }
            } catch (e) {
                console.warn("[CloudSync] Background sync exception:", e);
            }
        }
    }

    load(key, fallback = null) {
        try {
            const v = localStorage.getItem(key);
            return v !== null ? v : fallback;
        } catch (e) {
            return fallback;
        }
    }

    async fetchClientIp() {
        try {
            const res = await fetch("https://api.ipify.org?format=json", { cache: "no-store" });
            if (res.ok) {
                const data = await res.json();
                if (data && data.ip) {
                    this.clientIp = data.ip;
                    this.updateIpDisplays();
                    return;
                }
            }
        } catch (e) {}
        try {
            const res2 = await fetch("https://ipapi.co/json/");
            if (res2.ok) {
                const data2 = await res2.json();
                if (data2 && data2.ip) {
                    this.clientIp = data2.ip;
                    this.updateIpDisplays();
                    return;
                }
            }
        } catch (e) {}
        this.clientIp = (window.location.hostname && window.location.hostname !== "localhost")
            ? window.location.hostname
            : "127.0.0.1";
        this.updateIpDisplays();
    }

    updateIpDisplays() {
        const clientEl = document.getElementById("client-ip-display");
        if (clientEl) clientEl.textContent = this.clientIp;
        const topEl = document.getElementById("topbar-ip-display");
        if (topEl) topEl.textContent = this.clientIp;
    }

    getTodayDateString() {
        const d = new Date();
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
    }

    getDateStringDaysAgo(days) {
        const d = new Date(Date.now() - days * 24 * 60 * 60 * 1000);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${y}-${m}-${day}`;
    }

    getMonthStartDateString() {
        const d = new Date();
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, '0');
        return `${y}-${m}-01`;
    }

    getIstTimestamp(date = new Date()) {
        const d = (date instanceof Date) ? date : new Date(date);
        return d.toLocaleString('en-IN', {
            timeZone: 'Asia/Kolkata',
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: true
        }) + ' IST';
    }

    normalizeRecordDate(record) {
        if (!record) return null;
        if (record.rawTimestamp && typeof record.rawTimestamp === 'number') {
            const d = new Date(record.rawTimestamp);
            if (!isNaN(d.getTime())) {
                const y = d.getFullYear();
                const m = String(d.getMonth() + 1).padStart(2, '0');
                const day = String(d.getDate()).padStart(2, '0');
                return `${y}-${m}-${day}`;
            }
        }
        const val = record.timestamp || record.createdAt || record.time || record.date;
        if (!val) return null;
        if (typeof val === 'number') {
            const d = new Date(val);
            if (isNaN(d.getTime())) return null;
            return d.toISOString().split('T')[0];
        }
        const str = val.toString().replace(/ IST$/i, '').trim();
        if (str.startsWith("Today")) {
            return this.getTodayDateString();
        }
        if (str.startsWith("Yesterday")) {
            return this.getDateStringDaysAgo(1);
        }
        const parsed = Date.parse(str);
        if (!isNaN(parsed)) {
            const d = new Date(parsed);
            const y = d.getFullYear();
            const m = String(d.getMonth() + 1).padStart(2, '0');
            const day = String(d.getDate()).padStart(2, '0');
            return `${y}-${m}-${day}`;
        }
        const match = str.match(/(\d{1,2})\s+([A-Za-z]{3})\s+(\d{4})/);
        if (match) {
            const d = new Date(`${match[2]} ${match[1]}, ${match[3]}`);
            if (!isNaN(d.getTime())) {
                const y = d.getFullYear();
                const m = String(d.getMonth() + 1).padStart(2, '0');
                const day = String(d.getDate()).padStart(2, '0');
                return `${y}-${m}-${day}`;
            }
        }
        return null;
    }

    setDateFilter(section, field, value) {
        if (section === "audit") {
            this.auditFilters[field] = value;
        } else if (section === "sessions") {
            this.sessionFilters[field] = value;
        } else if (section === "reports") {
            this.reportsFilters[field] = value;
        } else if (section === "orders") {
            this.ordersFilters[field] = value;
        }
        this.renderCurrentTab();
    }

    applyDatePreset(section, preset) {
        let from = "";
        let to = "";
        const today = this.getTodayDateString();

        if (preset === "today") {
            from = today;
            to = today;
        } else if (preset === "7days") {
            from = this.getDateStringDaysAgo(7);
            to = today;
        } else if (preset === "this_month") {
            from = this.getMonthStartDateString();
            to = today;
        }

        if (section === "audit") {
            this.auditFilters.dateFrom = from;
            this.auditFilters.dateTo = to;
        } else if (section === "sessions") {
            this.sessionFilters.dateFrom = from;
            this.sessionFilters.dateTo = to;
        } else if (section === "reports") {
            this.reportsFilters.dateFrom = from;
            this.reportsFilters.dateTo = to;
        } else if (section === "orders") {
            this.ordersFilters.dateFrom = from;
            this.ordersFilters.dateTo = to;
        }
        this.renderCurrentTab();
    }

    setAuditSeverity(sev) {
        this.auditFilters.severity = sev;
        this.severityFilter = sev;
        this.renderCurrentTab();
    }

    setAuditRole(role) {
        this.auditFilters.role = role;
        this.renderCurrentTab();
    }

    setSessionEventType(type) {
        this.sessionFilters.eventType = type;
        this.renderCurrentTab();
    }

    setSessionRole(role) {
        this.sessionFilters.role = role;
        this.renderCurrentTab();
    }

    setOrderStatusFilter(section, status) {
        if (section === "reports") {
            this.reportsFilters.status = status;
        } else {
            this.ordersFilters.status = status;
        }
        this.renderCurrentTab();
    }

    setOrderTypeFilter(type) {
        this.ordersFilters.orderType = type;
        this.renderCurrentTab();
    }

    setStudentFilter(key, value) {
        this.studentsFilters[key] = value;
        this.renderCurrentTab();
    }

    setRiderFilter(key, value) {
        this.ridersFilters[key] = value;
        this.renderCurrentTab();
    }

    setServicesSpecificFilter(key, value) {
        this.servicesFilters[key] = value;
        if (key === 'type') {
            this.searchQueries.servicesType = value;
        }
        this.renderCurrentTab();
    }

    setDisputesSpecificFilter(key, value) {
        this.disputesFilters[key] = value;
        if (key === 'tab') {
            this.searchQueries.disputesTab = value;
        }
        this.renderCurrentTab();
    }

    setLostFoundFilter(key, value) {
        this.lostFoundFilters[key] = value;
        this.renderCurrentTab();
    }

    setFraudFilter(key, value) {
        this.fraudFilters[key] = value;
        this.renderCurrentTab();
    }

    setFeedbackFilter(key, value) {
        this.feedbackFilters[key] = value;
        this.renderCurrentTab();
    }

    resetFilters(section) {
        if (this.searchQueries && this.searchQueries[section] !== undefined) {
            this.searchQueries[section] = "";
            const input = document.getElementById(`search-input-${section}`);
            if (input) input.value = "";
            const clearBtn = document.getElementById(`${section}-clear-btn`);
            if (clearBtn) clearBtn.classList.add('hidden');
        }

        if (section === "audit") {
            this.auditFilters = { dateFrom: "", dateTo: "", severity: "ALL", role: "ALL" };
            this.severityFilter = "ALL";
        } else if (section === "sessions") {
            this.sessionFilters = { dateFrom: "", dateTo: "", eventType: "ALL", role: "ALL" };
        } else if (section === "reports") {
            this.reportsFilters = { dateFrom: "", dateTo: "", status: "ALL" };
        } else if (section === "orders") {
            this.ordersFilters = { dateFrom: "", dateTo: "", status: "ALL", orderType: "ALL" };
        } else if (section === "students") {
            this.studentsFilters = { status: "ALL", hostel: "ALL", stream: "ALL" };
        } else if (section === "riders") {
            this.ridersFilters = { duty: "ALL", verification: "ALL", status: "ALL" };
        } else if (section === "services") {
            this.servicesFilters = { type: "ALL", status: "ALL" };
            this.searchQueries.servicesType = "ALL";
        } else if (section === "disputes") {
            this.disputesFilters = { tab: "OPEN", value: "ALL" };
            this.searchQueries.disputesTab = "OPEN";
        } else if (section === "lostFound") {
            this.lostFoundFilters = { type: "ALL", category: "ALL", status: "ALL", media: "ALL" };
        } else if (section === "fraud") {
            this.fraudFilters = { risk: "ALL", type: "ALL", status: "ALL" };
        } else if (section === "feedback") {
            this.feedbackFilters = { category: "ALL", taskStatus: "ALL" };
        } else if (section === "customerActivities") {
            this.customerActivitiesFilter = "ALL";
        }

        this.renderCurrentTab();
        if (window.lucide) {
            lucide.createIcons();
        }
    }

    renderFilterBar(section) {
        if (section === "audit") {
            const f = this.auditFilters;
            const severities = ["ALL", "INFO", "WARNING", "CRITICAL"];
            const roles = [
                { id: "ALL", name: "All Roles" },
                { id: "SUPER_ADMIN", name: "Super Admin" },
                { id: "OPERATIONS_OFFICER", name: "Operations" },
                { id: "FINANCIAL_AUDITOR", name: "Financial" },
                { id: "SAFETY_DISPUTE_OFFICER", name: "Safety & Dispute" }
            ];

            const isToday = f.dateFrom === this.getTodayDateString() && f.dateTo === this.getTodayDateString();
            const is7Days = f.dateFrom === this.getDateStringDaysAgo(7) && f.dateTo === this.getTodayDateString();
            const isThisMonth = f.dateFrom === this.getMonthStartDateString() && f.dateTo === this.getTodayDateString();
            const isAll = !f.dateFrom && !f.dateTo;
            const filteredCount = this.getFilteredAudit().length;

            return `
                <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm space-y-3.5">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                                <i data-lucide="filter" class="w-4 h-4"></i>
                            </span>
                            <div>
                                <h3 class="text-xs font-black text-slate-900 uppercase tracking-wide">Filter & Export Conditions</h3>
                            </div>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.resetFilters('audit')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                                <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                                <span>Reset Filters</span>
                            </button>
                            <span class="text-xs font-black text-purple-800 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                                ${filteredCount} Logs Matching Filter
                            </span>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-3 text-xs">
                        <div class="lg:col-span-6 flex items-center gap-2">
                            <div class="flex-1">
                                <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">From Date</label>
                                <input type="date" value="${f.dateFrom || ''}" onchange="app.setDateFilter('audit', 'dateFrom', this.value)" class="w-full px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold focus:ring-2 focus:ring-purple-600 focus:outline-none bg-slate-50" />
                            </div>
                            <div class="flex-1">
                                <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">To Date</label>
                                <input type="date" value="${f.dateTo || ''}" onchange="app.setDateFilter('audit', 'dateTo', this.value)" class="w-full px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold focus:ring-2 focus:ring-purple-600 focus:outline-none bg-slate-50" />
                            </div>
                        </div>

                        <div class="lg:col-span-6 flex flex-col justify-end">
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Quick Date Range</label>
                            <div class="flex items-center gap-1.5">
                                <button onclick="app.applyDatePreset('audit', 'all')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${isAll ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">All</button>
                                <button onclick="app.applyDatePreset('audit', 'today')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${isToday ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">Today</button>
                                <button onclick="app.applyDatePreset('audit', '7days')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${is7Days ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">7 Days</button>
                                <button onclick="app.applyDatePreset('audit', 'this_month')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${isThisMonth ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">This Month</button>
                            </div>
                        </div>
                    </div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs">
                        <div class="flex flex-wrap items-center gap-1.5">
                            <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Condition (Severity):</span>
                            ${severities.map(s => `
                                <button onclick="app.setAuditSeverity('${s}')" class="px-3 py-1 rounded-xl text-xs transition ${f.severity === s ? 'bg-purple-700 text-white font-black font-extrabold uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                    ${s}
                                </button>
                            `).join("")}
                        </div>

                        <div class="flex items-center gap-2">
                            <span class="text-[10px] font-bold text-slate-400 uppercase">Actor Role:</span>
                            <select onchange="app.setAuditRole(this.value)" class="px-3 py-1 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${roles.map(r => `<option value="${r.id}" ${f.role === r.id ? 'selected' : ''}>${r.name}</option>`).join("")}
                            </select>
                        </div>
                    </div>
                </div>
            `;
        }

        if (section === "sessions") {
            const f = this.sessionFilters;
            const eventTypes = ["ALL", "LOGIN", "LOGOUT", "AUTO_EXPIRED"];
            const roles = [
                { id: "ALL", name: "All Roles" },
                { id: "SUPER_ADMIN", name: "Superior Admin" },
                { id: "OPERATIONS_OFFICER", name: "Operations Officer" },
                { id: "FINANCIAL_AUDITOR", name: "Financial Auditor" },
                { id: "SAFETY_DISPUTE_OFFICER", name: "Safety & Dispute Officer" }
            ];

            const isToday = f.dateFrom === this.getTodayDateString() && f.dateTo === this.getTodayDateString();
            const is7Days = f.dateFrom === this.getDateStringDaysAgo(7) && f.dateTo === this.getTodayDateString();
            const isThisMonth = f.dateFrom === this.getMonthStartDateString() && f.dateTo === this.getTodayDateString();
            const isAll = !f.dateFrom && !f.dateTo;
            const filteredCount = this.getFilteredSessionLogs().length;

            return `
                <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm space-y-3.5">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                                <i data-lucide="filter" class="w-4 h-4"></i>
                            </span>
                            <div>
                                <h3 class="text-xs font-black text-slate-900 uppercase tracking-wide">Filter & Export Conditions</h3>
                                <p class="text-[11px] text-slate-500">Filter access telemetry by event type, administrative role, and IST date range</p>
                            </div>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.resetFilters('sessions')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                                <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                                <span>Reset Filters</span>
                            </button>
                            <span class="text-xs font-black text-purple-800 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                                ${filteredCount} Sessions Matching Filter
                            </span>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-3 text-xs">
                        <div class="lg:col-span-6 flex items-center gap-2">
                            <div class="flex-1">
                                <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">From Date</label>
                                <input type="date" value="${f.dateFrom || ''}" onchange="app.setDateFilter('sessions', 'dateFrom', this.value)" class="w-full px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold focus:ring-2 focus:ring-purple-600 focus:outline-none bg-slate-50" />
                            </div>
                            <div class="flex-1">
                                <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">To Date</label>
                                <input type="date" value="${f.dateTo || ''}" onchange="app.setDateFilter('sessions', 'dateTo', this.value)" class="w-full px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold focus:ring-2 focus:ring-purple-600 focus:outline-none bg-slate-50" />
                            </div>
                        </div>

                        <div class="lg:col-span-6 flex flex-col justify-end">
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Quick Date Range</label>
                            <div class="flex items-center gap-1.5">
                                <button onclick="app.applyDatePreset('sessions', 'all')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${isAll ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">All</button>
                                <button onclick="app.applyDatePreset('sessions', 'today')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${isToday ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">Today</button>
                                <button onclick="app.applyDatePreset('sessions', '7days')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${is7Days ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">7 Days</button>
                                <button onclick="app.applyDatePreset('sessions', 'this_month')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${isThisMonth ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">This Month</button>
                            </div>
                        </div>
                    </div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs">
                        <div class="flex flex-wrap items-center gap-1.5">
                            <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Event Type:</span>
                            ${eventTypes.map(ev => `
                                <button onclick="app.setSessionEventType('${ev}')" class="px-3 py-1 rounded-xl text-xs transition ${f.eventType === ev ? 'bg-purple-700 text-white font-black font-extrabold uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                    ${ev.replace(/_/g, ' ')}
                                </button>
                            `).join("")}
                        </div>

                        <div class="flex items-center gap-2">
                            <span class="text-[10px] font-bold text-slate-400 uppercase">Role:</span>
                            <select onchange="app.setSessionRole(this.value)" class="px-3 py-1 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${roles.map(r => `<option value="${r.id}" ${f.role === r.id ? 'selected' : ''}>${r.name}</option>`).join("")}
                            </select>
                        </div>
                    </div>
                </div>
            `;
        }

        if (section === "students") {
            const f = this.studentsFilters;
            const filteredCount = this.getFilteredStudents().length;
            const statuses = [
                { id: "ALL", name: "All Statuses" },
                { id: "ACTIVE", name: "Active" },
                { id: "SUSPENDED", name: "Suspended" }
            ];
            const hostels = [
                { id: "ALL", name: "All Hostels" },
                { id: "HOSTEL 1", name: "Hostel 1" },
                { id: "HOSTEL 2", name: "Hostel 2" },
                { id: "HOSTEL 3", name: "Hostel 3" },
                { id: "GIRLS HOSTEL 1", name: "Girls Hostel 1" },
                { id: "GIRLS HOSTEL 2", name: "Girls Hostel 2" }
            ];
            const streams = [
                { id: "ALL", name: "All Streams" },
                { id: "COMPUTER SCIENCE", name: "CSE / IT" },
                { id: "ELECTRONICS", name: "ECE / EE" },
                { id: "MECHANICAL", name: "Mechanical" },
                { id: "CIVIL", name: "Civil" },
                { id: "MANAGEMENT", name: "Management" }
            ];

            return `
                <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm space-y-3.5">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                                <i data-lucide="filter" class="w-4 h-4"></i>
                            </span>
                            <div>
                                <h3 class="text-xs font-black text-slate-900 uppercase tracking-wide">Student Directory Filters</h3>
                                <p class="text-[11px] text-slate-500">Filter students by moderation status, campus residence, and academic branch</p>
                            </div>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.resetFilters('students')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                                <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                                <span>Reset Filters</span>
                            </button>
                            <span class="text-xs font-black text-purple-800 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                                ${filteredCount} Students Matching
                            </span>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                        <div>
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Campus Hostel</label>
                            <select onchange="app.setStudentFilter('hostel', this.value)" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${hostels.map(h => `<option value="${h.id}" ${f.hostel === h.id ? 'selected' : ''}>${h.name}</option>`).join("")}
                            </select>
                        </div>

                        <div>
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Academic Stream</label>
                            <select onchange="app.setStudentFilter('stream', this.value)" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${streams.map(s => `<option value="${s.id}" ${f.stream === s.id ? 'selected' : ''}>${s.name}</option>`).join("")}
                            </select>
                        </div>
                    </div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center gap-1.5 text-xs">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Account Status:</span>
                        ${statuses.map(s => `
                            <button onclick="app.setStudentFilter('status', '${s.id}')" class="px-3 py-1 rounded-xl text-xs transition ${f.status === s.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${s.name}
                            </button>
                        `).join("")}
                    </div>
                </div>
            `;
        }

        if (section === "riders") {
            const f = this.ridersFilters;
            const filteredCount = this.getFilteredRiders().length;
            const dutyOptions = [
                { id: "ALL", name: "All Duty" },
                { id: "ONLINE", name: "Online (Active Duty)" },
                { id: "OFFLINE", name: "Offline" }
            ];
            const verifOptions = [
                { id: "ALL", name: "All KYC" },
                { id: "VERIFIED", name: "Verified" },
                { id: "PENDING", name: "Pending KYC" },
                { id: "REJECTED", name: "Rejected" }
            ];
            const statusOptions = [
                { id: "ALL", name: "All Status" },
                { id: "ACTIVE", name: "Active" },
                { id: "SUSPENDED", name: "Suspended" }
            ];

            return `
                <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm space-y-3.5">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                                <i data-lucide="filter" class="w-4 h-4"></i>
                            </span>
                            <div>
                                <h3 class="text-xs font-black text-slate-900 uppercase tracking-wide">Courier Fleet Filters</h3>
                                <p class="text-[11px] text-slate-500">Filter runners by real-time duty status, KYC verification tier, and operational standing</p>
                            </div>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.resetFilters('riders')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                                <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                                <span>Reset Filters</span>
                            </button>
                            <span class="text-xs font-black text-purple-800 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                                ${filteredCount} Riders Matching
                            </span>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                        <div>
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">KYC Verification</label>
                            <select onchange="app.setRiderFilter('verification', this.value)" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${verifOptions.map(v => `<option value="${v.id}" ${f.verification === v.id ? 'selected' : ''}>${v.name}</option>`).join("")}
                            </select>
                        </div>

                        <div>
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Fleet Standing</label>
                            <select onchange="app.setRiderFilter('status', this.value)" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${statusOptions.map(s => `<option value="${s.id}" ${f.status === s.id ? 'selected' : ''}>${s.name}</option>`).join("")}
                            </select>
                        </div>
                    </div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center gap-1.5 text-xs">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Duty Status:</span>
                        ${dutyOptions.map(d => `
                            <button onclick="app.setRiderFilter('duty', '${d.id}')" class="px-3 py-1 rounded-xl text-xs transition ${f.duty === d.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${d.name}
                            </button>
                        `).join("")}
                    </div>
                </div>
            `;
        }

        if (section === "services") {
            const f = this.servicesFilters;
            const data = this.getFilteredServices();
            const types = [
                { id: "ALL", name: `All Services (${this.cabPools.length + this.mealCircles.length + this.rentals.length})` },
                { id: "CABS", name: `Cab Pools (${this.cabPools.length})` },
                { id: "MEALS", name: `Meal Circles (${this.mealCircles.length})` },
                { id: "RENTALS", name: `Rentals (${this.rentals.length})` }
            ];
            const statuses = [
                { id: "ALL", name: "All Statuses" },
                { id: "ACTIVE", name: "Active / Open" },
                { id: "COMPLETED", name: "Completed" },
                { id: "CANCELLED", name: "Cancelled" }
            ];

            return `
                <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm space-y-3.5">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                                <i data-lucide="filter" class="w-4 h-4"></i>
                            </span>
                            <div>
                                <h3 class="text-xs font-black text-slate-900 uppercase tracking-wide">Multi-Service Hub Filters</h3>
                                <p class="text-[11px] text-slate-500">Filter campus rides, shared meal orders, and peer-to-peer item rentals</p>
                            </div>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.resetFilters('services')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                                <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                                <span>Reset Filters</span>
                            </button>
                            <span class="text-xs font-black text-purple-800 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                                ${data.totalCount} Listings Matching
                            </span>
                        </div>
                    </div>

                    <div class="flex flex-wrap items-center gap-1.5 text-xs">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Service Type:</span>
                        ${types.map(t => `
                            <button onclick="app.setServicesSpecificFilter('type', '${t.id}')" class="px-3 py-1.5 rounded-xl text-xs transition ${f.type === t.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${t.name}
                            </button>
                        `).join("")}
                    </div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center gap-1.5 text-xs">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Lifecycle Status:</span>
                        ${statuses.map(s => `
                            <button onclick="app.setServicesSpecificFilter('status', '${s.id}')" class="px-2.5 py-1 rounded-xl text-xs transition ${f.status === s.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${s.name}
                            </button>
                        `).join("")}
                    </div>
                </div>
            `;
        }

        if (section === "disputes") {
            const f = this.disputesFilters;
            const filteredCount = this.getFilteredDisputes().length;
            const openCount = this.disputes.filter(d => d.status.startsWith("OPEN") || d.status.startsWith("UNDER")).length;
            const resolvedCount = this.disputes.filter(d => d.status.startsWith("RESOLVED")).length;

            const tabs = [
                { id: "OPEN", name: `Open Cases (${openCount})` },
                { id: "RESOLVED", name: `Resolved (${resolvedCount})` },
                { id: "ALL", name: `All Cases (${this.disputes.length})` }
            ];
            const tiers = [
                { id: "ALL", name: "All Escrow Amounts" },
                { id: "UNDER_500", name: "Micro Escrow (< Rs. 500)" },
                { id: "ABOVE_500", name: "Major Escrow (>= Rs. 500)" }
            ];

            return `
                <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm space-y-3.5">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                                <i data-lucide="filter" class="w-4 h-4"></i>
                            </span>
                            <div>
                                <h3 class="text-xs font-black text-slate-900 uppercase tracking-wide">Dispute & Escrow Filters</h3>
                                <p class="text-[11px] text-slate-500">Filter complaints by case resolution stage and escrow refund threshold</p>
                            </div>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.resetFilters('disputes')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                                <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                                <span>Reset Filters</span>
                            </button>
                            <span class="text-xs font-black text-purple-800 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                                ${filteredCount} Cases Matching
                            </span>
                        </div>
                    </div>

                    <div class="flex flex-wrap items-center gap-1.5 text-xs">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Case State:</span>
                        ${tabs.map(t => `
                            <button onclick="app.setDisputesSpecificFilter('tab', '${t.id}')" class="px-3 py-1.5 rounded-xl text-xs transition ${f.tab === t.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${t.name}
                            </button>
                        `).join("")}
                    </div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center gap-1.5 text-xs">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Escrow Threshold:</span>
                        ${tiers.map(t => `
                            <button onclick="app.setDisputesSpecificFilter('value', '${t.id}')" class="px-2.5 py-1 rounded-xl text-xs transition ${f.value === t.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${t.name}
                            </button>
                        `).join("")}
                    </div>
                </div>
            `;
        }

        if (section === "lostFound") {
            const f = this.lostFoundFilters;
            const filteredCount = this.getFilteredLostFound().length;
            const types = [
                { id: "ALL", name: "All Types" },
                { id: "LOST", name: "Lost Items" },
                { id: "FOUND", name: "Found Items" }
            ];
            const categories = [
                { id: "ALL", name: "All Categories" },
                { id: "ELECTRONICS", name: "Electronics" },
                { id: "DOCUMENTS", name: "Documents / IDs" },
                { id: "ACCESSORIES", name: "Accessories" },
                { id: "KEYS_CARDS", name: "Keys / Cards" },
                { id: "CLOTHING", name: "Clothing / Bags" },
                { id: "OTHER", name: "Other" }
            ];
            const statuses = [
                { id: "ALL", name: "All Status" },
                { id: "OPEN", name: "Open / Pending" },
                { id: "VERIFIED", name: "Verified" },
                { id: "CLAIMED", name: "Claimed / Resolved" },
                { id: "TAKEN_DOWN", name: "Taken Down" }
            ];
            const mediaOptions = [
                { id: "ALL", name: "All Media" },
                { id: "WITH_PHOTOS", name: "With Photos Only" },
                { id: "NO_PHOTOS", name: "Without Photos" }
            ];

            return `
                <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm space-y-3.5">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                                <i data-lucide="filter" class="w-4 h-4"></i>
                            </span>
                            <div>
                                <h3 class="text-xs font-black text-slate-900 uppercase tracking-wide">Lost & Found Vault Filters</h3>
                                <p class="text-[11px] text-slate-500">Filter notices by report type, item category, verification status, and media uploads</p>
                            </div>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.resetFilters('lostFound')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                                <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                                <span>Reset Filters</span>
                            </button>
                            <span class="text-xs font-black text-purple-800 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                                ${filteredCount} Listings Matching
                            </span>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                        <div>
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Item Category</label>
                            <select onchange="app.setLostFoundFilter('category', this.value)" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${categories.map(c => `<option value="${c.id}" ${f.category === c.id ? 'selected' : ''}>${c.name}</option>`).join("")}
                            </select>
                        </div>

                        <div>
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Media Presence</label>
                            <select onchange="app.setLostFoundFilter('media', this.value)" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${mediaOptions.map(m => `<option value="${m.id}" ${f.media === m.id ? 'selected' : ''}>${m.name}</option>`).join("")}
                            </select>
                        </div>
                    </div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs">
                        <div class="flex flex-wrap items-center gap-1.5">
                            <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Report Type:</span>
                            ${types.map(t => `
                                <button onclick="app.setLostFoundFilter('type', '${t.id}')" class="px-3 py-1 rounded-xl text-xs transition ${f.type === t.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                    ${t.name}
                                </button>
                            `).join("")}
                        </div>

                        <div class="flex flex-wrap items-center gap-1.5">
                            <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Status:</span>
                            ${statuses.map(s => `
                                <button onclick="app.setLostFoundFilter('status', '${s.id}')" class="px-2.5 py-1 rounded-xl text-xs transition ${f.status === s.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                    ${s.name}
                                </button>
                            `).join("")}
                        </div>
                    </div>
                </div>
            `;
        }

        if (section === "fraud") {
            const f = this.fraudFilters;
            const filteredCount = this.getFilteredFraud().length;
            const riskLevels = [
                { id: "ALL", name: "All Risk Levels" },
                { id: "CRITICAL", name: "Critical" },
                { id: "HIGH", name: "High" },
                { id: "MEDIUM", name: "Medium" },
                { id: "LOW", name: "Low" }
            ];
            const types = [
                { id: "ALL", name: "All Anomalies" },
                { id: "BILL", name: "Bill Manipulation" },
                { id: "GEO", name: "Geo-Spoofing" },
                { id: "CANCEL", name: "Cancellation Exploits" },
                { id: "MULTI", name: "Multi-Account Rings" }
            ];
            const statuses = [
                { id: "ALL", name: "All Statuses" },
                { id: "FLAGGED", name: "Flagged" },
                { id: "UNDER_REVIEW", name: "Under Review" },
                { id: "RESOLVED", name: "Resolved" },
                { id: "ACTIONED", name: "Actioned" }
            ];

            return `
                <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm space-y-3.5">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                                <i data-lucide="filter" class="w-4 h-4"></i>
                            </span>
                            <div>
                                <h3 class="text-xs font-black text-slate-900 uppercase tracking-wide">Fraud & Anomaly Scanner Filters</h3>
                                <p class="text-[11px] text-slate-500">Filter risk indicators by severity grade, malpractice archetype, and review status</p>
                            </div>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.resetFilters('fraud')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                                <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                                <span>Reset Filters</span>
                            </button>
                            <span class="text-xs font-black text-purple-800 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                                ${filteredCount} Anomalies Matching
                            </span>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                        <div>
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Anomaly Type</label>
                            <select onchange="app.setFraudFilter('type', this.value)" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${types.map(t => `<option value="${t.id}" ${f.type === t.id ? 'selected' : ''}>${t.name}</option>`).join("")}
                            </select>
                        </div>

                        <div>
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Investigation Status</label>
                            <select onchange="app.setFraudFilter('status', this.value)" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-bold text-slate-700 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                ${statuses.map(s => `<option value="${s.id}" ${f.status === s.id ? 'selected' : ''}>${s.name}</option>`).join("")}
                            </select>
                        </div>
                    </div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center gap-1.5 text-xs">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Risk Severity:</span>
                        ${riskLevels.map(r => `
                            <button onclick="app.setFraudFilter('risk', '${r.id}')" class="px-3 py-1 rounded-xl text-xs transition ${f.risk === r.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${r.name}
                            </button>
                        `).join("")}
                    </div>
                </div>
            `;
        }

        // For "reports" or "orders"
        const f = section === "reports" ? this.reportsFilters : this.ordersFilters;
        const statuses = ["ALL", "PENDING_ACCEPTANCE", "RUNNER_ASSIGNED", "BILL_UPLOADED", "DELIVERED", "CANCELLED"];

        const isToday = f.dateFrom === this.getTodayDateString() && f.dateTo === this.getTodayDateString();
        const is7Days = f.dateFrom === this.getDateStringDaysAgo(7) && f.dateTo === this.getTodayDateString();
        const isThisMonth = f.dateFrom === this.getMonthStartDateString() && f.dateTo === this.getTodayDateString();
        const isAll = !f.dateFrom && !f.dateTo;
        const filteredCount = section === "reports" ? this.getFilteredReportsOrders().length : this.getFilteredOrders().length;

        return `
            <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm space-y-3.5">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                    <div class="flex items-center gap-2">
                        <span class="p-1.5 rounded-xl bg-purple-100 text-purple-700">
                            <i data-lucide="filter" class="w-4 h-4"></i>
                        </span>
                        <div>
                            <h3 class="text-xs font-black text-slate-900 uppercase tracking-wide">Filter & Export Conditions</h3>
                            <p class="text-[11px] text-slate-500">Refine order records by status, service type, and IST delivery timeline</p>
                        </div>
                    </div>
                    <div class="flex items-center gap-2">
                        <button onclick="app.resetFilters('${section}')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                            <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                            <span>Reset Filters</span>
                        </button>
                        <span class="text-xs font-black text-purple-800 bg-purple-50 px-3 py-1 rounded-full border border-purple-200">
                            ${filteredCount} Orders Matching Filter
                        </span>
                    </div>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-3 text-xs">
                    <div class="lg:col-span-6 flex items-center gap-2">
                        <div class="flex-1">
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">From Date</label>
                            <input type="date" value="${f.dateFrom || ''}" onchange="app.setDateFilter('${section}', 'dateFrom', this.value)" class="w-full px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold focus:ring-2 focus:ring-purple-600 focus:outline-none bg-slate-50" />
                        </div>
                        <div class="flex-1">
                            <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">To Date</label>
                            <input type="date" value="${f.dateTo || ''}" onchange="app.setDateFilter('${section}', 'dateTo', this.value)" class="w-full px-3 py-1.5 rounded-xl border border-slate-200 text-xs font-semibold focus:ring-2 focus:ring-purple-600 focus:outline-none bg-slate-50" />
                        </div>
                    </div>

                    <div class="lg:col-span-6 flex flex-col justify-end">
                        <label class="block text-[10px] font-bold text-slate-500 uppercase mb-1">Quick Date Range</label>
                        <div class="flex items-center gap-1.5">
                            <button onclick="app.applyDatePreset('${section}', 'all')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${isAll ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">All</button>
                            <button onclick="app.applyDatePreset('${section}', 'today')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${isToday ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">Today</button>
                            <button onclick="app.applyDatePreset('${section}', '7days')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${is7Days ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">7 Days</button>
                            <button onclick="app.applyDatePreset('${section}', 'this_month')" class="px-2.5 py-1.5 rounded-xl text-[11px] font-bold transition ${isThisMonth ? 'bg-purple-700 text-white shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">This Month</button>
                        </div>
                    </div>
                </div>

                <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs">
                    <div class="flex flex-wrap items-center gap-1.5">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Condition (Status):</span>
                        ${statuses.map(s => `
                            <button onclick="app.setOrderStatusFilter('${section}', '${s}')" class="px-2.5 py-1 rounded-xl text-xs transition ${f.status === s ? 'bg-purple-700 text-white font-black font-extrabold uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${s.replace(/_/g, ' ')}
                            </button>
                        `).join("")}
                    </div>
                    ${section === "orders" ? `
                    <div class="flex items-center gap-1.5">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Order Type:</span>
                        ${[
                            { id: "ALL", name: "All Types" },
                            { id: "STORE_ORDERS", name: "Store Orders" },
                            { id: "COURIER_TRANSFERS", name: "Courier Transfers" }
                        ].map(t => `
                            <button onclick="app.setOrderTypeFilter('${t.id}')" class="px-2.5 py-1 rounded-xl text-xs transition ${(f.orderType || 'ALL') === t.id ? 'bg-purple-700 text-white font-black uppercase shadow-md ring-2 ring-purple-400' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${t.name}
                            </button>
                        `).join("")}
                    </div>
                    ` : ''}
                </div>
            </div>
        `;
    }

    recordAudit(actionType, targetId, details, severity = "INFO") {
        const now = new Date();
        const formatted = this.getIstTimestamp(now);

        const newLog = {
            id: `AUD-${Date.now() % 100000}`,
            timestamp: formatted,
            adminRole: this.activeRole,
            ipAddress: this.clientIp || "127.0.0.1",
            actionType: actionType,
            targetEntityId: targetId,
            details: details,
            severity: severity
        };

        this.auditLogs.unshift(newLog);
        this.save("admin_audit_trail_log", this.auditLogs);
    }

    formatDuration(ms) {
        if (!ms || ms <= 0) return "0s";
        const sec = Math.floor(ms / 1000);
        const h = Math.floor(sec / 3600);
        const m = Math.floor((sec % 3600) / 60);
        const s = sec % 60;
        if (h > 0) return `${h}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`;
        if (m > 0) return `${m}m ${String(s).padStart(2, '0')}s`;
        return `${s}s`;
    }

    recordSessionEvent(eventType, details, duration = null) {
        const now = new Date();
        const raw = Date.now();
        const id = `SESS-${raw % 1000000}`;
        const roleMeta = ADMIN_ROLES[this.activeRole] || { name: this.activeRole };

        let calculatedDuration = duration;
        if (!calculatedDuration) {
            if (eventType === "LOGIN") {
                calculatedDuration = "Active Session";
            } else {
                const diffMs = Math.max(0, raw - (this.currentSessionStartTime || raw));
                calculatedDuration = this.formatDuration(diffMs);
            }
        }

        const newSessionLog = {
            id: id,
            role: this.activeRole,
            roleName: roleMeta.name,
            eventType: eventType,
            timestamp: this.getIstTimestamp(now),
            rawTimestamp: raw,
            ipAddress: this.clientIp || "127.0.0.1",
            details: details,
            duration: calculatedDuration
        };

        this.sessionLogs.unshift(newSessionLog);
        this.save("admin_session_access_logs", this.sessionLogs);
    }

    handleSessionInactivityExpiry() {
        if (!this.isUnlocked) return;
        const expiredRole = this.activeRole;
        const roleMeta = ADMIN_ROLES[expiredRole] || { name: expiredRole };
        const sessionDuration = this.formatDuration(Math.max(0, Date.now() - (this.currentSessionStartTime || Date.now())));

        // 1. Record AUTO_EXPIRED in Session Access Logs (IST)
        this.recordSessionEvent("AUTO_EXPIRED", `Session automatically expired after 5 minutes of inactivity for ${roleMeta.name}.`, sessionDuration);

        // 2. Record immutable Audit Trail entry (IST)
        this.recordAudit("AUTO_SESSION_EXPIRED", "GATEWAY", `Console session for ${roleMeta.name} automatically expired and locked due to 5 minutes of inactivity.`, "WARNING");

        // 3. Lock console back to PIN gate
        this.isUnlocked = false;
        sessionStorage.setItem("admin_is_unlocked", "false");
        this.lastUserActivity = Date.now();

        // 4. Close any open modal
        this.closeModal();

        // 5. Re-render UI to show locked PIN screen
        this.render();

        // 6. Show alert toast
        this.showToast("Session expired due to 5 minutes of inactivity. Please re-enter PIN.", "warning");
    }

    showToast(msg, type = "success") {
        const container = document.getElementById("toast-container");
        if (!container) return;

        const toast = document.createElement("div");
        const bgColor = type === "error" ? "bg-rose-600" : type === "warning" ? "bg-amber-600" : "bg-emerald-600";
        const icon = type === "error" ? "alert-circle" : type === "warning" ? "alert-triangle" : "check-circle";

        toast.className = `toast flex items-center justify-between gap-3 px-4 py-3 rounded-2xl text-white text-xs font-semibold shadow-2xl ${bgColor} cursor-pointer`;
        toast.innerHTML = `
            <div class="flex items-center gap-2.5">
                <i data-lucide="${icon}" class="w-4 h-4 shrink-0"></i>
                <span class="leading-snug">${msg}</span>
            </div>
            <button class="ml-2 text-white/80 hover:text-white shrink-0 text-base font-bold leading-none">&times;</button>
        `;
        container.appendChild(toast);
        if (window.lucide) lucide.createIcons();

        let timer = null;
        let isHovered = false;

        const removeToast = () => {
            if (toast.classList.contains("toast-fade-out")) return;
            toast.classList.add("toast-fade-out");
            setTimeout(() => {
                if (toast.parentElement) toast.remove();
            }, 200);
        };

        const startTimer = () => {
            if (timer) clearTimeout(timer);
            timer = setTimeout(() => {
                if (!isHovered) removeToast();
            }, 3000); // Disappear after 3 seconds
        };

        startTimer();

        // Stays if cursor moves to popup, disappears as cursor leaves
        toast.addEventListener("mouseenter", () => {
            isHovered = true;
            if (timer) {
                clearTimeout(timer);
                timer = null;
            }
        });

        toast.addEventListener("mouseleave", () => {
            isHovered = false;
            removeToast();
        });

        const closeBtn = toast.querySelector("button");
        if (closeBtn) {
            closeBtn.addEventListener("click", (e) => {
                e.stopPropagation();
                if (timer) clearTimeout(timer);
                removeToast();
            });
        }
    }

    startTimerLoop() {
        setInterval(() => {
            const now = Date.now();
            let stateChanged = false;

            // Inactivity Session Auto-Expiry Check (5 Minutes)
            if (this.isUnlocked && (now - this.lastUserActivity >= this.sessionTimeoutMs)) {
                this.handleSessionInactivityExpiry();
            }

            this.students.forEach(st => {
                if (st.studentStatus === "TEMP_BLOCKED" && st.studentBlockUntil > 0 && now >= st.studentBlockUntil) {
                    st.studentStatus = "ACTIVE";
                    st.studentBlockUntil = 0;
                    stateChanged = true;
                }
                if (st.riderStatus === "TEMP_BLOCKED" && st.riderBlockUntil > 0 && now >= st.riderBlockUntil) {
                    st.riderStatus = "ACTIVE";
                    st.riderBlockUntil = 0;
                    stateChanged = true;
                }
            });

            this.fraudAnomalies.forEach(anm => {
                if (anm.status === "TEMP_BLOCKED" && anm.blockUntil > 0 && now >= anm.blockUntil) {
                    anm.status = "FLAGGED_PENDING";
                    anm.blockUntil = 0;
                    stateChanged = true;
                }
            });

            if (stateChanged) {
                this.save("campus_students_list", this.students);
                this.save("admin_fraud_anomalies", this.fraudAnomalies);
                this.render();
            } else {
                this.updateLiveTimers();
            }
        }, 1000);
    }

    updateLiveTimers() {
        document.querySelectorAll("[data-countdown]").forEach(el => {
            const until = parseInt(el.getAttribute("data-countdown"), 10);
            if (!until || until <= Date.now()) {
                el.textContent = "00h 00m 00s (Expired)";
                return;
            }
            const rem = until - Date.now();
            const h = Math.floor(rem / (1000 * 60 * 60)) % 24;
            const m = Math.floor(rem / (1000 * 60)) % 60;
            const s = Math.floor(rem / 1000) % 60;
            el.textContent = `${String(h).padStart(2, '0')}h ${String(m).padStart(2, '0')}m ${String(s).padStart(2, '0')}s`;
        });
    }

    initDOM() {
        // Activity Tracking for Inactivity Timeout (5 Minutes)
        const registerActivity = () => {
            this.lastUserActivity = Date.now();
        };
        ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll', 'click'].forEach(evt => {
            window.addEventListener(evt, registerActivity, { passive: true });
        });

        // PIN Unlock
        document.getElementById("btn-unlock")?.addEventListener("click", () => this.handleUnlock());
        document.getElementById("pin-input")?.addEventListener("keyup", (e) => {
            if (e.key === "Enter") this.handleUnlock();
        });

        // Top bar buttons
        document.getElementById("btn-switch-role")?.addEventListener("click", () => this.openRoleSwitcherModal());
        document.getElementById("btn-change-pin")?.addEventListener("click", () => this.openChangePinModal());
        document.getElementById("btn-lock-console")?.addEventListener("click", () => this.lockConsole());
        document.getElementById("btn-refresh-state")?.addEventListener("click", () => {
            this.initData();
            this.showToast("Telemetry synced with storage!");
            this.render();
        });
        document.getElementById("btn-reset-db")?.addEventListener("click", () => this.resetToDefaults());
    }

    isAccountLocked() {
        if (!this.lockoutUntil) return false;
        if (Date.now() < this.lockoutUntil) return true;
        // Lockout expired
        this.lockoutUntil = 0;
        this.failedAttempts = 0;
        localStorage.removeItem("admin_lockout_until");
        localStorage.setItem("admin_failed_attempts", "0");
        return false;
    }

    updateLockoutCountdown() {
        const el = document.getElementById("lockout-countdown");
        if (!el || !this.lockoutUntil) return;
        const diff = this.lockoutUntil - Date.now();
        if (diff <= 0) {
            this.isAccountLocked();
            this.render();
            return;
        }
        const hours = Math.floor(diff / (1000 * 60 * 60));
        const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((diff % (1000 * 60)) / 1000);
        el.textContent = `${hours.toString().padStart(2, '0')}h ${minutes.toString().padStart(2, '0')}m ${seconds.toString().padStart(2, '0')}s remaining`;
    }

    handleUnlock() {
        if (this.isAccountLocked()) {
            this.openEmergencyEmailModal();
            return;
        }

        const input = document.getElementById("pin-input");
        const errorEl = document.getElementById("pin-error");
        const entered = input.value.trim();

        // 1. Check Superior Administrator PIN (Default 4656)
        if (entered === this.superAdminPin) {
            this.activeRole = "SUPER_ADMIN";
            this.selectedTab = 0;
            this.failedAttempts = 0;
            localStorage.setItem("admin_failed_attempts", "0");
            localStorage.setItem("admin_active_role", "SUPER_ADMIN");
            this.isUnlocked = true;
            this.currentSessionStartTime = Date.now();
            this.lastUserActivity = Date.now();
            sessionStorage.setItem("admin_is_unlocked", "true");
            errorEl.classList.add("hidden");
            this.recordAudit("AUTH_LOGIN_SUCCESS", "GATEWAY", "Superior Administrator unlocked console with Master PIN.", "INFO");
            this.recordSessionEvent("LOGIN", "Superior Administrator authenticated with Master PIN.");
            this.render();
            if (window.cloudSync) { window.cloudSync.flushPendingQueue(); window.cloudSync.pollRecentMessages(true); }
            this.showToast("Welcome back, Superior Administrator!", "success");
            return;
        }

        // 2. Check Operational Team PIN (Default 1234)
        if (entered === this.opsStaffPin) {
            this.activeRole = "OPERATIONS_OFFICER";
            this.selectedTab = 1; // Direct route to Orders
            this.failedAttempts = 0;
            localStorage.setItem("admin_failed_attempts", "0");
            localStorage.setItem("admin_active_role", "OPERATIONS_OFFICER");
            this.isUnlocked = true;
            this.currentSessionStartTime = Date.now();
            this.lastUserActivity = Date.now();
            sessionStorage.setItem("admin_is_unlocked", "true");
            errorEl.classList.add("hidden");
            this.recordAudit("AUTH_LOGIN_SUCCESS", "GATEWAY", "Operational team member unlocked console with Staff PIN.", "INFO");
            this.recordSessionEvent("LOGIN", "Operations Officer authenticated with Staff PIN.");
            this.render();
            if (window.cloudSync) { window.cloudSync.flushPendingQueue(); window.cloudSync.pollRecentMessages(true); }
            this.showToast("Logged in as Operations Officer (Operations Tabs Access).", "success");
            return;
        }

        // 3. Check Financial Auditor PIN (Default 5678)
        if (entered === this.financialAuditorPin) {
            this.activeRole = "FINANCIAL_AUDITOR";
            this.selectedTab = 0; // Overview & Financial Ledgers
            this.failedAttempts = 0;
            localStorage.setItem("admin_failed_attempts", "0");
            localStorage.setItem("admin_active_role", "FINANCIAL_AUDITOR");
            this.isUnlocked = true;
            this.currentSessionStartTime = Date.now();
            this.lastUserActivity = Date.now();
            sessionStorage.setItem("admin_is_unlocked", "true");
            errorEl.classList.add("hidden");
            this.recordAudit("AUTH_LOGIN_SUCCESS", "GATEWAY", "Financial Auditor unlocked console with Auditor PIN.", "INFO");
            this.recordSessionEvent("LOGIN", "Financial Auditor authenticated with Auditor PIN.");
            this.render();
            if (window.cloudSync) { window.cloudSync.flushPendingQueue(); window.cloudSync.pollRecentMessages(true); }
            this.showToast("Logged in as Financial Auditor (Ledger & Reports Access).", "success");
            return;
        }

        // 4. Check Safety & Dispute Officer PIN (Default 1011)
        if (entered === this.safetyDisputePin) {
            this.activeRole = "SAFETY_DISPUTE_OFFICER";
            this.selectedTab = 2; // Direct route to Students & Safety
            this.failedAttempts = 0;
            localStorage.setItem("admin_failed_attempts", "0");
            localStorage.setItem("admin_active_role", "SAFETY_DISPUTE_OFFICER");
            this.isUnlocked = true;
            this.currentSessionStartTime = Date.now();
            this.lastUserActivity = Date.now();
            sessionStorage.setItem("admin_is_unlocked", "true");
            errorEl.classList.add("hidden");
            this.recordAudit("AUTH_LOGIN_SUCCESS", "GATEWAY", "Safety & Dispute Officer unlocked console with Security PIN.", "INFO");
            this.recordSessionEvent("LOGIN", "Safety & Dispute Officer authenticated with Security PIN.");
            this.render();
            if (window.cloudSync) { window.cloudSync.flushPendingQueue(); window.cloudSync.pollRecentMessages(true); }
            this.showToast("Logged in as Safety & Dispute Officer (Disputes & Fraud Scanner Access).", "success");
            return;
        }

        // 3. Failed attempt handling (Max 3 attempts, then 24-hr lockout)
        this.failedAttempts++;
        localStorage.setItem("admin_failed_attempts", this.failedAttempts.toString());

        if (this.failedAttempts >= 3) {
            this.lockoutUntil = Date.now() + (24 * 60 * 60 * 1000); // 24 Hours
            localStorage.setItem("admin_lockout_until", this.lockoutUntil.toString());
            this.recordAudit("SECURITY_LOCKOUT_24H", "GATEWAY", `System locked for 24 hours after 3 consecutive failed login attempts. Emergency rescue warning dispatched to ${this.alertEmail}.`, "CRITICAL");
            this.render();
            this.openEmergencyEmailModal();
        } else {
            const remaining = 3 - this.failedAttempts;
            errorEl.textContent = `Incorrect PIN. Access denied. ${remaining} attempt${remaining === 1 ? '' : 's'} remaining before 24-hour lockout.`;
            errorEl.classList.remove("hidden");
            this.recordAudit("AUTH_FAILED_ATTEMPT", "GATEWAY", `Failed unlock attempt (${this.failedAttempts}/3) entered.`, "WARNING");
            input.value = "";
            input.focus();
        }
    }

    rescueAccountNow() {
        this.lockoutUntil = 0;
        this.failedAttempts = 0;
        localStorage.removeItem("admin_lockout_until");
        localStorage.setItem("admin_failed_attempts", "0");
        this.recordAudit("ACCOUNT_RESCUED", "SECURITY_GATEWAY", `24-hour security lockout cleared via immediate emergency rescue authorization for ${this.alertEmail}.`, "WARNING");
        this.closeModal();
        this.showToast("Account successfully rescued! You may now log in.", "success");
        this.render();
    }

    openEmergencyEmailModal() {
        const modal = document.getElementById("modal-generic");
        const now = new Date();
        const dateStr = now.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) +
            ', ' + now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true });

        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-lg rounded-3xl overflow-hidden shadow-2xl border border-slate-200 animate-in fade-in zoom-in-95 duration-200">
                    <!-- Email Client Header -->
                    <div class="bg-gradient-to-r from-slate-900 to-slate-800 text-white p-5 border-b border-slate-700">
                        <div class="flex items-center justify-between pb-3 border-b border-slate-700/60">
                            <div class="flex items-center gap-2">
                                <span class="w-3 h-3 rounded-full bg-rose-500"></span>
                                <span class="w-3 h-3 rounded-full bg-amber-500"></span>
                                <span class="w-3 h-3 rounded-full bg-emerald-500"></span>
                                <span class="text-xs text-slate-300 font-mono ml-2">Security Dispatch Gateway</span>
                            </div>
                            <button onclick="app.closeModal()" class="text-slate-400 hover:text-white font-bold text-lg">&times;</button>
                        </div>
                        <div class="mt-3 flex items-start justify-between">
                            <div>
                                <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 font-bold text-[10px] uppercase tracking-wide">
                                    🚨 High Priority Security Alert
                                </span>
                                <h3 class="text-sm font-bold text-white mt-1.5">[URGENT] Campus Admin Console Locked — 3 Failed Login Attempts</h3>
                            </div>
                            <span class="text-[11px] text-slate-400 font-mono">${dateStr}</span>
                        </div>
                        <div class="mt-3 text-[11px] text-slate-300 space-y-0.5 bg-slate-950/40 p-2.5 rounded-xl border border-slate-700/50">
                            <div><span class="text-slate-400">To:</span> <strong class="text-amber-300 font-mono">${this.alertEmail}</strong></div>
                            <div><span class="text-slate-400">From:</span> <span class="text-slate-200 font-mono">security-shield@campusapp.admin</span></div>
                        </div>
                    </div>

                    <!-- Email Body -->
                    <div class="p-6 space-y-4 text-xs text-slate-700 max-h-[70vh] overflow-y-auto">
                        <div class="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 space-y-2">
                            <div class="font-extrabold flex items-center gap-2 text-amber-950">
                                <i data-lucide="alert-triangle" class="w-4 h-4 text-amber-600"></i>
                                <span>Security Incident Warning</span>
                            </div>
                            <p class="leading-relaxed">
                                Our security monitoring layer has flagged <strong>3 consecutive incorrect PIN attempts</strong> on the <strong>Campus Admin Operations Console</strong>.
                                In compliance with master security protocols, the console has been placed under a <strong>strict 24-hour security lockout</strong>.
                            </p>
                        </div>

                        <!-- Incident Telemetry -->
                        <div class="bg-slate-50 rounded-2xl p-4 border border-slate-200/80 space-y-2">
                            <div class="text-[11px] font-black uppercase text-slate-400 tracking-wider">Incident Telemetry Log</div>
                            <div class="grid grid-cols-2 gap-2 text-[11px]">
                                <div><span class="text-slate-400">Detected Client IP:</span><br/><strong class="text-slate-900 font-mono">${this.clientIp}</strong></div>
                                <div><span class="text-slate-400">Lockout Duration:</span><br/><strong class="text-rose-600 font-mono">24 Hours (Active)</strong></div>
                                <div><span class="text-slate-400">Target Role:</span><br/><strong class="text-purple-700 font-bold">Administrative Gateway</strong></div>
                                <div><span class="text-slate-400">Incident Severity:</span><br/><strong class="text-rose-600 font-mono">CRITICAL (AUTH-LOCK)</strong></div>
                            </div>
                        </div>

                        <p class="text-slate-600 leading-relaxed">
                            Click below to immediately clear the 24-hour lockout and restore console access.
                        </p>

                        <!-- Rescue Action Button -->
                        <div class="pt-2">
                            <button 
                                onclick="app.rescueAccountNow()" 
                                class="w-full py-4 px-6 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-700 hover:from-emerald-700 hover:to-teal-800 text-white font-extrabold text-sm shadow-xl shadow-emerald-700/25 transition-all transform active:scale-95 flex items-center justify-center gap-2.5">
                                <i data-lucide="key-round" class="w-5 h-5"></i>
                                <span>Rescue & Unlock Console Immediately</span>
                            </button>
                            <p class="text-[10px] text-center text-slate-400 mt-2">Authorized emergency override for ${this.alertEmail}</p>
                        </div>
                    </div>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) lucide.createIcons();
    }

    lockConsole() {
        const roleMeta = ADMIN_ROLES[this.activeRole] || { name: this.activeRole };
        const duration = this.formatDuration(Math.max(0, Date.now() - (this.currentSessionStartTime || Date.now())));
        this.recordSessionEvent("LOGOUT", `Manual console lock executed by ${roleMeta.name}.`, duration);
        this.recordAudit("AUTH_LOGOUT", "GATEWAY", `User manually locked console session for ${roleMeta.name}.`, "INFO");
        this.isUnlocked = false;
        sessionStorage.setItem("admin_is_unlocked", "false");
        this.render();
        this.showToast("Console locked successfully.");
    }

    resetToDefaults() {
        if (!confirm("Are you sure you want to reset all data and logs back to default seeds?")) return;
        localStorage.clear();
        this.superAdminPin = "4656";
        this.opsStaffPin = "1234";
        this.financialAuditorPin = "5678";
        this.safetyDisputePin = "1011";
        this.currentPin = "4656";
        this.failedAttempts = 0;
        this.lockoutUntil = 0;
        this.initData();
        this.showToast("Database reset to factory seeds successfully.");
        this.render();
    }

    render() {
        const pinScreen = document.getElementById("screen-pin-lock");
        const dashboardScreen = document.getElementById("screen-dashboard");

        if (!this.isUnlocked) {
            pinScreen.classList.remove("hidden");
            dashboardScreen.classList.add("hidden");

            const pinEntry = document.getElementById("pin-entry-container");
            const lockoutEntry = document.getElementById("lockout-container");
            const clientIpEl = document.getElementById("client-ip-display");
            if (clientIpEl) clientIpEl.textContent = this.clientIp;

            if (this.isAccountLocked()) {
                if (pinEntry) pinEntry.classList.add("hidden");
                if (lockoutEntry) lockoutEntry.classList.remove("hidden");
                this.updateLockoutCountdown();
            } else {
                if (pinEntry) pinEntry.classList.remove("hidden");
                if (lockoutEntry) lockoutEntry.classList.add("hidden");
                const pinInput = document.getElementById("pin-input");
                if (pinInput) { pinInput.value = ""; pinInput.focus(); }
            }
            if (window.lucide) lucide.createIcons();
            return;
        }

        pinScreen.classList.add("hidden");
        dashboardScreen.classList.remove("hidden");

        this.renderTopBar();
        this.renderNavigationTabs();
        this.renderCurrentTab();

        if (window.lucide) lucide.createIcons();
    }

    renderTopBar() {
        const roleShortNames = {
            SUPER_ADMIN: "Superior Admin",
            OPERATIONS_OFFICER: "Operations Officer",
            FINANCIAL_AUDITOR: "Financial Auditor",
            SAFETY_DISPUTE_OFFICER: "Safety & Dispute"
        };
        const roleObj = ADMIN_ROLES[this.activeRole] || ADMIN_ROLES.SUPER_ADMIN;
        const roleLabel = document.getElementById("topbar-role-label");
        if (roleLabel) {
            roleLabel.textContent = roleShortNames[this.activeRole] || roleObj.name;
        }
        const topIp = document.getElementById("topbar-ip-display");
        if (topIp) {
            topIp.textContent = this.clientIp;
        }

        // Hide Master Administrative settings for non-Superior Admin roles
        const btnChangePin = document.getElementById("btn-change-pin");
        if (btnChangePin) {
            btnChangePin.style.display = this.activeRole === "SUPER_ADMIN" ? "inline-flex" : "none";
        }
        const btnResetDb = document.getElementById("btn-reset-db");
        if (btnResetDb) {
            btnResetDb.style.display = this.activeRole === "SUPER_ADMIN" ? "inline-flex" : "none";
        }
    }

    renderNavigationTabs() {
        const container = document.getElementById("nav-tabs-container");
        if (!container) return;

        const roleObj = ADMIN_ROLES[this.activeRole] || ADMIN_ROLES.SUPER_ADMIN;
        const allowed = roleObj.allowedTabs;

        if (!allowed.includes(this.selectedTab)) {
            this.selectedTab = allowed[0] || 0;
        }

        container.innerHTML = allowed.map(tabIdx => {
            const tab = TAB_DEFINITIONS.find(t => t.id === tabIdx);
            if (!tab) return "";
            const isSelected = this.selectedTab === tabIdx;
            return `
                <button 
                    onclick="app.setTab(${tabIdx})" 
                    class="nav-tab-btn flex items-center gap-2 px-3 py-1.5 rounded-xl whitespace-nowrap text-xs transition-colors duration-150 ${
                        isSelected 
                            ? 'active-nav-tab bg-white text-purple-950 font-black shadow-md' 
                            : 'text-purple-200/80 hover:bg-white/10 hover:text-white font-semibold'
                    }"
                    title="${tab.label}">
                    <span class="nav-tab-icon-wrap flex items-center justify-center w-6 h-6 rounded-lg ${isSelected ? 'bg-purple-700 text-white shadow-xs ring-1 ring-purple-800' : 'text-purple-300'} transition-all shrink-0">
                        <i data-lucide="${tab.icon}" data-stroke-width="${isSelected ? '2.8' : '1.8'}" stroke-width="${isSelected ? '2.8' : '1.8'}" class="w-3.5 h-3.5 ${isSelected ? 'stroke-[2.8] text-white' : ''}"></i>
                    </span>
                    <span class="${isSelected ? 'font-black tracking-wide text-purple-950' : 'font-semibold'}">${tab.label}</span>
                </button>
            `;
        }).join("");

        if (window.lucide) {
            try {
                lucide.createIcons({
                    nameAttr: 'data-lucide',
                    root: container
                });
            } catch (e) {
                console.warn("[CampusAdmin] Tab icon render error:", e);
            }
        }

        try {
            const activeBtn = container.querySelector('.active-nav-tab');
            if (activeBtn) {
                activeBtn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
            }
        } catch (_) {}
    }

    setTab(idx) {
        const roleObj = ADMIN_ROLES[this.activeRole] || ADMIN_ROLES.SUPER_ADMIN;
        if (!roleObj.allowedTabs.includes(idx)) {
            this.showToast(`Access Restricted: Your active role (${roleObj.name}) does not have permission for this section.`, "error");
            this.selectedTab = roleObj.allowedTabs[0] || 0;
            this.render();
            return;
        }
        this.selectedTab = idx;
        this.render();
    }

    renderCurrentTab() {
        const main = document.getElementById("tab-content-area");
        if (!main) return;

        let content = "";
        try {
            switch (this.selectedTab) {
                case 0: content = this.renderOverviewTab(); break;
                case 1: content = this.renderOrdersTab(); break;
                case 2: content = this.renderStudentsTab(); break;
                case 3: content = this.renderRidersTab(); break;
                case 4: content = this.renderServicesTab(); break;
                case 5: content = this.renderDisputesTab(); break;
                case 6: content = this.renderLostFoundTab(); break;
                case 7: content = this.renderFraudScannerTab(); break;
                case 8: content = this.renderAuditTrailTab(); break;
                case 9: content = this.renderReportsTab(); break;
                case 10: content = this.renderFeedbackTab(); break;
                default: content = `<div class="p-8 text-center text-slate-500">Select a valid tab</div>`;
            }
        } catch (tabErr) {
            console.error("[CampusAdmin] Exception caught while rendering tab " + this.selectedTab + ":", tabErr);
            content = `
                <div class="p-8 bg-rose-50 border border-rose-200 rounded-3xl text-center space-y-3">
                    <div class="w-12 h-12 rounded-2xl bg-rose-600 text-white flex items-center justify-center mx-auto shadow-md">
                        <i data-lucide="alert-triangle" class="w-6 h-6"></i>
                    </div>
                    <h3 class="text-base font-black text-rose-950">Section Temporarily Unavailable</h3>
                    <p class="text-xs text-rose-700 max-w-md mx-auto">${tabErr.message || "An unexpected error occurred while loading this section."}</p>
                    <button onclick="app.setTab(0)" class="px-4 py-2 bg-purple-700 text-white rounded-xl text-xs font-bold hover:bg-purple-800 transition">Return to Overview</button>
                </div>
            `;
        }

        main.innerHTML = content;
        if (window.lucide) {
            try {
                lucide.createIcons();
            } catch (e) {
                console.warn("[CampusAdmin] Lucide icons creation warning:", e);
            }
        }
    }

    // ==========================================
    // REUSABLE SEARCH BOX COMPONENT
    // ==========================================
    renderSearchBox(section, placeholder) {
        const query = (this.searchQueries[section] || "").replace(/"/g, '&quot;');
        return `
            <div class="relative">
                <i data-lucide="search" class="w-4 h-4 text-purple-700 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none"></i>
                <input 
                    type="text" 
                    id="search-input-${section}"
                    value="${query}" 
                    oninput="app.handleSearch('${section}', this.value)"
                    placeholder="${placeholder}" 
                    class="w-full pl-11 pr-10 py-3 bg-white border border-slate-200 rounded-2xl text-xs focus:ring-2 focus:ring-purple-600 focus:outline-none transition shadow-sm"
                    autocomplete="off"
                />
                <button 
                    id="${section}-clear-btn" 
                    onclick="app.clearSearch('${section}')" 
                    class="${this.searchQueries[section] ? '' : 'hidden'} absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1 font-bold text-base leading-none">
                    &times;
                </button>
            </div>
        `;
    }

    handleSearch(section, value) {
        this.searchQueries[section] = value;
        const clearBtn = document.getElementById(`${section}-clear-btn`);
        if (clearBtn) {
            clearBtn.classList.toggle('hidden', !value);
        }
        this.updateSectionGrid(section);
    }

    clearSearch(section) {
        this.searchQueries[section] = "";
        const input = document.getElementById(`search-input-${section}`);
        if (input) {
            input.value = "";
            input.focus();
        }
        const clearBtn = document.getElementById(`${section}-clear-btn`);
        if (clearBtn) {
            clearBtn.classList.add('hidden');
        }
        this.updateSectionGrid(section);
    }

    updateSectionGrid(section) {
        switch (section) {
            case "orders": {
                const container = document.getElementById("orders-grid");
                const badge = document.getElementById("orders-count-badge");
                if (container) {
                    const filtered = this.getFilteredOrders();
                    container.innerHTML = this.renderOrderCards(filtered);
                    if (badge) badge.textContent = `${filtered.length} Orders Listed`;
                }
                break;
            }
            case "students": {
                const container = document.getElementById("students-grid");
                const badge = document.getElementById("students-count-badge");
                if (container) {
                    const filtered = this.getFilteredStudents();
                    container.innerHTML = this.renderStudentCards(filtered);
                    if (badge) badge.textContent = `${filtered.length} Students Listed`;
                }
                break;
            }
            case "riders": {
                const container = document.getElementById("riders-grid");
                const badge = document.getElementById("riders-count-badge");
                if (container) {
                    const filtered = this.getFilteredRiders();
                    container.innerHTML = this.renderRiderCards(filtered);
                    if (badge) badge.textContent = `${filtered.length} Riders Listed`;
                }
                break;
            }
            case "services": {
                const container = document.getElementById("services-grid");
                const badge = document.getElementById("services-count-badge");
                if (container) {
                    const data = this.getFilteredServices();
                    container.innerHTML = this.renderServicesCards(data);
                    if (badge) badge.textContent = `${data.totalCount} Services Found`;
                }
                break;
            }
            case "disputes": {
                const container = document.getElementById("disputes-grid");
                const badge = document.getElementById("disputes-count-badge");
                if (container) {
                    const filtered = this.getFilteredDisputes();
                    container.innerHTML = this.renderDisputeCards(filtered);
                    if (badge) badge.textContent = `${filtered.length} Cases in View`;
                }
                break;
            }
            case "lostFound": {
                const container = document.getElementById("lostfound-grid");
                const badge = document.getElementById("lostfound-count-badge");
                if (container) {
                    const filtered = this.getFilteredLostFound();
                    container.innerHTML = this.renderLostFoundCards(filtered);
                    if (badge) badge.textContent = `${filtered.length} Listings Found`;
                }
                break;
            }
            case "fraud": {
                const container = document.getElementById("fraud-grid");
                const badge = document.getElementById("fraud-count-badge");
                if (container) {
                    const filtered = this.getFilteredFraud();
                    container.innerHTML = this.renderFraudCards(filtered);
                    if (badge) badge.textContent = `${filtered.length} Anomalies Tracked`;
                }
                break;
            }
            case "audit": {
                if (this.auditSubTab === 1) {
                    const container = document.getElementById("sessions-grid");
                    const badge = document.getElementById("sessions-count-badge");
                    if (container) {
                        const filtered = this.getFilteredSessionLogs();
                        container.innerHTML = this.renderSessionCards(filtered);
                        if (badge) badge.textContent = `${filtered.length} Sessions Listed`;
                    }
                } else if (this.auditSubTab === 2) {
                    const container = document.getElementById("customer-activities-grid");
                    const badge = document.getElementById("customer-activities-count-badge");
                    if (container) {
                        const filtered = this.getFilteredCustomerActivities();
                        container.innerHTML = this.renderCustomerActivitiesCards(filtered);
                        if (badge) badge.textContent = `${filtered.length} Activities Listed`;
                    }
                } else {
                    const container = document.getElementById("audit-grid");
                    const badge = document.getElementById("audit-count-badge");
                    if (container) {
                        const filtered = this.getFilteredAudit();
                        container.innerHTML = this.renderAuditCards(filtered);
                        if (badge) badge.textContent = `${filtered.length} Records Logged`;
                    }
                }
                break;
            }
            case "customerActivities": {
                const container = document.getElementById("customer-activities-grid");
                const badge = document.getElementById("customer-activities-count-badge");
                if (container) {
                    const filtered = this.getFilteredCustomerActivities();
                    container.innerHTML = this.renderCustomerActivitiesCards(filtered);
                    if (badge) badge.textContent = `${filtered.length} Activities Listed`;
                }
                break;
            }
            case "sessions": {
                const container = document.getElementById("sessions-grid");
                const badge = document.getElementById("sessions-count-badge");
                if (container) {
                    const filtered = this.getFilteredSessionLogs();
                    container.innerHTML = this.renderSessionCards(filtered);
                    if (badge) badge.textContent = `${filtered.length} Sessions Listed`;
                }
                break;
            }
            case "reports": {
                const container = document.getElementById("reports-orders-grid");
                const badge = document.getElementById("reports-count-badge");
                if (container) {
                    const filtered = this.getFilteredReportsOrders();
                    container.innerHTML = this.renderReportsOrderRows(filtered);
                    if (badge) badge.textContent = `${filtered.length} Orders in Report`;
                }
                break;
            }
            case "feedback": {
                const container = document.getElementById("feedback-grid");
                const badge = document.getElementById("feedback-count-badge");
                if (container) {
                    const data = this.getFilteredFeedback();
                    container.innerHTML = this.renderFeedbackCards(data);
                    if (badge) badge.textContent = `${data.length} Items in Queue`;
                }
                break;
            }
        }

        if (window.lucide) lucide.createIcons();
        this.updateLiveTimers();
    }

    // ==========================================
    // TAB 0: OVERVIEW & SYSTEM TELEMETRY
    // ==========================================
    renderOverviewTab() {
        const activeOrders = this.orders.filter(o => o.status !== "DELIVERED" && o.status !== "CANCELLED").length;
        const openDisputes = this.disputes.filter(d => d.status.startsWith("OPEN") || d.status.startsWith("UNDER")).length;
        const onlineRiders = this.riders.filter(r => r.isOnline).length;
        const blockedStudents = this.students.filter(s => s.studentStatus === "TEMP_BLOCKED" || s.riderStatus === "TEMP_BLOCKED").length;

        return `
            <div class="space-y-6">
                <div>
                    <h2 class="text-xl font-extrabold text-slate-900">Operations Command Center</h2>
                </div>

                <!-- 4 KPI Metrics matching AdminOverviewAndHealthTab -->
                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                    <div onclick="app.setTab(1)" class="card-interactive cursor-pointer p-5 rounded-2xl bg-indigo-50 border border-indigo-100 shadow-sm">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-semibold text-indigo-700">Live Orders</span>
                            <i data-lucide="shopping-bag" class="w-4 h-4 text-indigo-500"></i>
                        </div>
                        <div class="text-2xl font-black text-indigo-900 mt-2">${activeOrders} Active</div>
                        <div class="text-[11px] text-indigo-600 mt-1">${this.orders.length} Total Today</div>
                    </div>

                    <div onclick="app.setTab(5)" class="card-interactive cursor-pointer p-5 rounded-2xl bg-amber-50 border border-amber-100 shadow-sm">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-semibold text-amber-700">Open Disputes</span>
                            <i data-lucide="shield-alert" class="w-4 h-4 text-amber-500"></i>
                        </div>
                        <div class="text-2xl font-black text-amber-900 mt-2">${openDisputes} Cases</div>
                        <div class="text-[11px] text-amber-600 mt-1">Manual Review Required</div>
                    </div>

                    <div onclick="app.setTab(3)" class="card-interactive cursor-pointer p-5 rounded-2xl bg-emerald-50 border border-emerald-100 shadow-sm">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-semibold text-emerald-700">Active Fleet</span>
                            <i data-lucide="bike" class="w-4 h-4 text-emerald-500"></i>
                        </div>
                        <div class="text-2xl font-black text-emerald-900 mt-2">${onlineRiders} Online</div>
                        <div class="text-[11px] text-emerald-600 mt-1">${this.riders.length} Verified Riders</div>
                    </div>

                    <div onclick="app.setTab(2)" class="card-interactive cursor-pointer p-5 rounded-2xl bg-rose-50 border border-rose-100 shadow-sm">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-semibold text-rose-700">Temp Blocked IDs</span>
                            <i data-lucide="clock" class="w-4 h-4 text-rose-500"></i>
                        </div>
                        <div class="text-2xl font-black text-rose-900 mt-2">${blockedStudents} Locked</div>
                        <div class="text-[11px] text-rose-600 mt-1">12h Timer Active</div>
                    </div>
                </div>

                <!-- System Telemetry & Client App Version Enforcement -->
                <div class="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm space-y-4">
                    <div class="flex items-center justify-between">
                        <div>
                            <h3 class="text-sm font-bold text-slate-900">Client App Version Enforcement</h3>
                            <p class="text-xs text-slate-500">Minimum Required: v${this.systemConfig.minAppVersion} ${this.systemConfig.forceUpdateEnabled ? '<span class="text-rose-600 font-bold">(Force Enforced)</span>' : '<span class="text-slate-400">(Optional)</span>'}</p>
                        </div>
                        <button onclick="app.openConfigModal()" class="px-3.5 py-1.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-bold transition shadow-sm">
                            Configure Policy
                        </button>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-3 border-t border-slate-100">
                        <div class="p-4 rounded-xl bg-slate-50 border border-slate-100">
                            <div class="text-[11px] text-slate-400 font-medium">Database Latency</div>
                            <div class="text-sm font-bold text-emerald-600 mt-0.5">${this.systemConfig.serverStatus}</div>
                        </div>
                        <div class="p-4 rounded-xl bg-slate-50 border border-slate-100">
                            <div class="text-[11px] text-slate-400 font-medium">Active Connected Users</div>
                            <div class="text-sm font-bold text-purple-900 mt-0.5">${this.systemConfig.activeSessions} Users</div>
                        </div>
                        <div class="p-4 rounded-xl bg-slate-50 border border-slate-100">
                            <div class="text-[11px] text-slate-400 font-medium">System Error Rate</div>
                            <div class="text-sm font-bold text-emerald-600 mt-0.5">${this.systemConfig.errorRate}</div>
                        </div>
                    </div>
                </div>

                <!-- Live Customer & Student Activities Feed (All Modules) -->
                <div class="bg-white rounded-2xl p-6 border border-slate-200/80 shadow-sm space-y-4">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100">
                        <div>
                            <div class="flex items-center gap-2">
                                <span class="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                                <h3 class="text-sm font-extrabold text-slate-900">Live Customer Activities Stream (All Sections)</h3>
                            </div>
                            <p class="text-xs text-slate-500 mt-0.5">Real-time actions across Store Orders, Couriers, Cab Pools, Meals, Rentals, Lost & Found, and Disputes.</p>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.setTab(8); app.setAuditSubTab(2);" class="text-xs font-bold text-purple-700 hover:text-purple-900 transition flex items-center gap-1">
                                <span>View Full Activities Audit</span>
                                <i data-lucide="chevron-right" class="w-3.5 h-3.5"></i>
                            </button>
                        </div>
                    </div>

                    <!-- Filter pills -->
                    <div class="flex flex-wrap items-center gap-1.5 text-xs">
                        <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Section:</span>
                        ${['ALL', 'ORDERS', 'COURIER', 'CAB_POOL', 'MEAL_CIRCLE', 'RENTAL', 'LOST_FOUND', 'DISPUTE', 'FEEDBACK'].map(sec => `
                            <button onclick="app.setOverviewActivitiesFilter('${sec}')" class="px-2.5 py-1 rounded-xl text-xs transition ${this.customerActivitiesFilter === sec ? 'bg-purple-700 text-white font-black font-extrabold uppercase shadow-sm ring-2 ring-purple-300' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                ${sec.replace(/_/g, ' ')}
                            </button>
                        `).join("")}
                    </div>

                    <!-- Activity List Container -->
                    <div id="overview-activities-container" class="space-y-2.5 max-h-[460px] overflow-y-auto pr-1">
                        ${this.renderOverviewActivitiesList()}
                    </div>
                </div>
            </div>
        `;
    }

    // ==========================================
    // TAB 1: ORDERS TAB
    // ==========================================
    isCourierOrder(o) {
        if (!o) return false;
        const id = (o.id || "").toUpperCase();
        if (id.startsWith("ORDERNO-") || id.startsWith("CSV-") || id.startsWith("TRN-") || id.startsWith("TRANSFER-")) return true;
        if (o.transferMode || o.serviceType === "TRANSFER") return true;
        if (Array.isArray(o.items) && o.items.some(it => {
            const n = (typeof it === 'string' ? it : (it.name || it.first || "")).toLowerCase();
            return n.includes("transfer") || n.includes("courier") || n.includes("package") || n.includes("send:") || n.includes("bring:");
        })) return true;
        return false;
    }

    getFilteredOrders() {
        const q = (this.searchQueries.orders || "").toLowerCase().trim();
        const status = (this.ordersFilters.status || "ALL").toUpperCase();
        const orderType = (this.ordersFilters.orderType || "ALL").toUpperCase();
        const from = this.ordersFilters.dateFrom;
        const to = this.ordersFilters.dateTo;

        return this.orders.filter(o => {
            if (status !== "ALL" && (o.status || "").toUpperCase() !== status) return false;

            if (orderType === "STORE_ORDERS" && this.isCourierOrder(o)) return false;
            if (orderType === "COURIER_TRANSFERS" && !this.isCourierOrder(o)) return false;

            if (from || to) {
                const oDate = this.normalizeRecordDate(o);
                if (oDate) {
                    if (from && oDate < from) return false;
                    if (to && oDate > to) return false;
                }
            }

            if (!q) return true;
            const itemsText = (o.items || []).map(it => `${it.name} ${it.qty}`).join(" ");
            const chatText = (o.chatLogs || []).map(c => `${c.senderName} ${c.sender} ${c.message}`).join(" ");
            const haystack = [
                o.id,
                o.customerName,
                o.customerId,
                o.customerPhone,
                o.runnerName,
                o.runnerPhone,
                o.pickPoint,
                o.dropPoint,
                o.status,
                o.deliveryOtp,
                itemsText,
                chatText,
                `Rs. ${o.actualBill}`,
                `Rs. ${o.deliveryFee}`
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderOrdersTab() {
        const filtered = this.getFilteredOrders();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Live Orders Queue</h2>
                    </div>
                    <div class="flex items-center gap-2 self-start sm:self-auto">
                        <button onclick="app.downloadOrdersCsv('orders')" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs flex items-center gap-2 transition shadow-sm">
                            <i data-lucide="file-spreadsheet" class="w-4 h-4"></i>
                            <span>Download Filtered Excel/CSV</span>
                        </button>
                        <span id="orders-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800">
                            ${filtered.length} Orders Listed
                        </span>
                    </div>
                </div>

                <!-- Date & Condition Filter Bar -->
                ${this.renderFilterBar("orders")}

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("orders", "Search by Order ID, Customer, Runner, Location, Status, Items...")}

                <!-- Orders Grid -->
                <div id="orders-grid" class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    ${this.renderOrderCards(filtered)}
                </div>
            </div>
        `;
    }

    renderOrderCards(ordersList) {
        if (ordersList.length === 0) {
            return `
                <div class="col-span-full py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="package-x" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No orders matching your search query.</p>
                </div>
            `;
        }

        const statusColors = {
            DELIVERED: "bg-emerald-100 text-emerald-800",
            CANCELLED: "bg-rose-100 text-rose-800",
            RUNNER_ASSIGNED: "bg-sky-100 text-sky-800",
            BILL_UPLOADED: "bg-purple-100 text-purple-800",
            PENDING_ACCEPTANCE: "bg-amber-100 text-amber-800"
        };

        return ordersList.map(order => {
            const statusClass = statusColors[order.status] || "bg-slate-100 text-slate-700";
            const isRiderAssigned = order.runnerName && order.status !== "PENDING_ACCEPTANCE";

            return `
                <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive flex flex-col justify-between">
                    <div>
                        <!-- Header -->
                        <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                            <div class="flex items-center gap-2">
                                <span class="font-black text-sm text-purple-900">${order.id}</span>
                                <span class="text-[11px] text-slate-400">${order.timestamp || "Today"}</span>
                            </div>
                            <span class="pill-status ${statusClass}">${order.status}</span>
                        </div>

                        <!-- Customer Details -->
                        <div class="mt-3 p-3 rounded-xl bg-slate-50 border border-slate-100 text-xs space-y-1">
                            <div class="flex items-center justify-between">
                                <span class="font-bold text-purple-900">Customer Details</span>
                                <button onclick="app.openUserActivityDossier('${order.customerId}')" class="text-[10px] font-bold text-purple-700 hover:text-purple-900 underline flex items-center gap-1 cursor-pointer">
                                    <span>360° Dossier</span>
                                    <i data-lucide="chevron-right" class="w-3 h-3"></i>
                                </button>
                            </div>
                            <div class="text-slate-800 font-semibold cursor-pointer hover:text-purple-700 flex items-center gap-1.5" onclick="app.openUserActivityDossier('${order.customerId}')" title="Click to view full 360° Activity Dossier">
                                <span>${order.customerName || "Customer"}</span>
                                <span class="text-slate-400 font-normal">(${order.customerId})</span>
                            </div>
                            <div class="text-slate-500">Contact Phone: ${order.customerPhone || "+91 9876543210"}</div>
                            <div class="text-slate-600 font-medium">Delivery Location: ${order.dropPoint}</div>
                        </div>

                        <!-- Order Breakdown -->
                        <div class="mt-3 p-3 rounded-xl bg-slate-50 border border-slate-100 text-xs space-y-1.5">
                            <div class="font-bold text-sky-700">Pickup Spot: ${order.pickPoint}</div>
                            ${order.items && order.items.length ? `
                                <div class="text-slate-600">Items: ${order.items.map(it => `${it.name} (x${it.qty})`).join(", ")}</div>
                            ` : ''}
                            <div class="flex items-center justify-between pt-1 border-t border-slate-200/60 font-semibold">
                                <span class="text-slate-500">Bill Range: Rs. ${order.totalMin} - ${order.totalMax}</span>
                                <span class="text-emerald-700">Delivery Fee: Rs. ${order.deliveryFee}</span>
                            </div>
                            <div class="text-[11px] font-bold text-purple-700">Delivery OTP: ${order.deliveryOtp}</div>
                        </div>

                        <!-- Assigned Rider Details -->
                        ${isRiderAssigned ? `
                            <div class="mt-3 p-3 rounded-xl bg-emerald-50 border border-emerald-100 text-xs space-y-1">
                                <div class="flex items-center justify-between">
                                    <span class="font-bold text-emerald-800">Assigned Rider Details</span>
                                    <button onclick="app.openRiderActivityDossier('${order.runnerEmail || order.customerId}')" class="text-[10px] font-bold text-emerald-700 hover:text-emerald-900 underline flex items-center gap-1 cursor-pointer">
                                        <span>Rider 360°</span>
                                        <i data-lucide="chevron-right" class="w-3 h-3"></i>
                                    </button>
                                </div>
                                <div class="font-bold text-slate-800 cursor-pointer hover:text-purple-700 flex items-center gap-1.5" onclick="app.openRiderActivityDossier('${order.runnerEmail || order.customerId}')" title="Click to view full 360° Rider Dossier">
                                    <span>Rider: ${order.runnerName}</span>
                                </div>
                                <div class="text-emerald-800">Phone: ${order.runnerPhone || "+91 9822334455"}</div>
                                ${order.actualBill > 0 ? `
                                    <div class="font-bold text-emerald-700">Uploaded Actual Bill: Rs. ${order.actualBill}</div>
                                ` : ''}
                            </div>
                        ` : ''}
                    </div>

                    <!-- Footer Action -->
                    <div class="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                        <button onclick="app.openSectionChatModal('orders', '${order.id}')" class="px-3.5 py-2 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold text-xs transition flex items-center gap-1.5 shadow-2xs cursor-pointer">
                            <i data-lucide="message-square" class="w-3.5 h-3.5 text-purple-700"></i>
                            <span>Chat (${(order.chatLogs || []).length})</span>
                        </button>
                        <button onclick="app.openOrderInterventionModal('${order.id}')" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-bold transition shadow-sm cursor-pointer">
                            Intervene & Edit Status
                        </button>
                    </div>
                </div>
            `;
        }).join("");
    }

    openOrderInterventionModal(orderId) {
        const order = this.orders.find(o => o.id === orderId);
        if (!order) return;

        const statuses = ["PENDING_ACCEPTANCE", "RUNNER_ASSIGNED", "BILL_UPLOADED", "DELIVERED", "CANCELLED"];
        const modal = document.getElementById("modal-generic");

        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-lg rounded-3xl p-6 shadow-2xl border border-slate-200 max-h-[90vh] overflow-y-auto">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <h3 class="text-base font-extrabold text-slate-900">Intervene: ${order.id}</h3>
                        <button onclick="app.closeModal()" class="text-slate-400 hover:text-slate-600 text-xl font-bold">&times;</button>
                    </div>

                    <form onsubmit="app.submitOrderIntervention(event, '${order.id}')" class="space-y-4 mt-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-2">Select Lifecycle Status:</label>
                            <div class="flex flex-wrap gap-2">
                                ${statuses.map(st => `
                                    <label class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 cursor-pointer hover:bg-slate-50 has-[:checked]:border-purple-600 has-[:checked]:bg-purple-50">
                                        <input type="radio" name="order_status" value="${st}" ${order.status === st ? 'checked' : ''} class="text-purple-600" />
                                        <span class="font-semibold text-[11px]">${st}</span>
                                    </label>
                                `).join("")}
                            </div>
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Assigned Runner Name:</label>
                            <input type="text" id="int-runner-name" value="${order.runnerName || ''}" placeholder="e.g. Vikram Singh" class="w-full px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600 focus:outline-none" />
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Rider Contact Phone:</label>
                            <input type="text" id="int-runner-phone" value="${order.runnerPhone || '+91 9822334455'}" class="w-full px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600 focus:outline-none" />
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Actual Item Bill (Rs.):</label>
                            <input type="number" id="int-actual-bill" value="${order.actualBill || 0}" class="w-full px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600 focus:outline-none" />
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Delivery OTP Code:</label>
                            <input type="text" id="int-delivery-otp" value="${order.deliveryOtp || '4912'}" class="w-full px-3 py-2 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600 focus:outline-none font-mono font-bold" />
                        </div>

                        <div class="flex items-center justify-end gap-2 pt-4 border-t border-slate-100">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50 transition">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold transition shadow-sm">Apply Intervention</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
    }

    submitOrderIntervention(e, orderId) {
        e.preventDefault();
        const order = this.orders.find(o => o.id === orderId);
        if (!order) return;

        const statusRadio = document.querySelector('input[name="order_status"]:checked');
        const runnerName = document.getElementById("int-runner-name").value.trim();
        const runnerPhone = document.getElementById("int-runner-phone").value.trim();
        const actualBill = parseInt(document.getElementById("int-actual-bill").value, 10) || 0;
        const otp = document.getElementById("int-delivery-otp").value.trim();

        order.status = statusRadio ? statusRadio.value : order.status;
        order.runnerName = runnerName || null;
        order.runnerPhone = runnerPhone || null;
        order.actualBill = actualBill;
        if (otp) order.deliveryOtp = otp;

        this.save("app_orders_list", this.orders);

        // Synchronize all lifecycle state repositories simultaneously for 100% Android app parity
        const pending = this.orders.filter(o => o.status !== "DELIVERED" && o.status !== "CANCELLED");
        const completed = this.orders.filter(o => o.status === "DELIVERED");
        const cancelled = this.orders.filter(o => o.status === "CANCELLED");

        this.save("db_pending_orders", pending);
        this.save("db_completed_orders", completed);
        this.save("db_cancelled_orders", cancelled);

        this.recordAudit(
            "ORDER_INTERVENTION",
            order.id,
            `Order ${order.id} status modified to ${order.status} (Runner: ${order.runnerName || "Unassigned"}, Bill: Rs. ${order.actualBill}).`,
            "INFO"
        );

        this.closeModal();
        this.showToast(`Order ${order.id} updated and synced across all repositories!`);
        this.updateSectionGrid("orders");
    }

    // ==========================================
    // TAB 2: STUDENTS 360 & MODERATION
    // ==========================================
    getFilteredStudents() {
        const q = (this.searchQueries.students || "").toLowerCase().trim();
        const fStatus = (this.studentsFilters?.status || "ALL").toUpperCase();
        const fHostel = (this.studentsFilters?.hostel || "ALL").toUpperCase();
        const fStream = (this.studentsFilters?.stream || "ALL").toUpperCase();

        return this.students.filter(st => {
            if (fStatus !== "ALL" && (st.studentStatus || "").toUpperCase() !== fStatus) return false;
            if (fHostel !== "ALL" && !(st.hostel || "").toUpperCase().includes(fHostel)) return false;
            if (fStream !== "ALL" && !(st.stream || st.department || "").toUpperCase().includes(fStream)) return false;

            if (!q) return true;
            const haystack = [
                st.name,
                st.email,
                st.phone,
                st.rollNo,
                st.hostel,
                st.stream,
                st.className,
                st.studentStatus,
                st.riderStatus,
                st.studentBlockReason,
                st.riderBlockReason,
                st.warningMessage,
                st.upiId,
                `Rs. ${st.walletBalance}`
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderStudentsTab() {
        const filtered = this.getFilteredStudents();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Student Profiles & Moderation</h2>
                    </div>
                    <span id="students-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${filtered.length} Students Listed
                    </span>
                </div>

                <!-- Directory Filter Bar -->
                ${this.renderFilterBar("students")}

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("students", "Search by Student Name, Email, Phone, Roll No, Hostel, Department, Status...")}

                <div id="students-grid" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    ${this.renderStudentCards(filtered)}
                </div>
            </div>
        `;
    }

    renderStudentCards(studentsList) {
        if (studentsList.length === 0) {
            return `
                <div class="col-span-full py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="users-2" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No students matching your search query.</p>
                </div>
            `;
        }

        return studentsList.map(st => {
            const isStudentBlocked = st.studentStatus === "TEMP_BLOCKED";
            const isRiderBlocked = st.riderStatus === "TEMP_BLOCKED";
            const hasActiveLock = isStudentBlocked || isRiderBlocked;

            return `
                <div class="bg-white rounded-2xl p-5 border ${hasActiveLock ? 'border-rose-300' : 'border-slate-200/80'} shadow-sm card-interactive flex flex-col justify-between">
                    <div class="space-y-3">
                        <div class="flex items-start justify-between">
                            <div onclick="app.openUserActivityDossier('${st.email}')" class="cursor-pointer group">
                                <div class="font-extrabold text-sm text-slate-900 group-hover:text-purple-700 flex items-center gap-1.5 transition">
                                    <span>${st.name}</span>
                                    <span class="text-[10px] px-1.5 py-0.5 rounded-md bg-purple-100 text-purple-800 font-bold opacity-0 group-hover:opacity-100 transition">360° Dossier &gt;</span>
                                </div>
                                <div class="text-[11px] text-purple-700 font-semibold">${st.email}</div>
                                <div class="text-[11px] text-slate-400">Roll: ${st.rollNo} | ${st.hostel}</div>
                                <div class="text-[11px] text-slate-400">Stream: ${st.stream || 'Engineering'}</div>
                            </div>
                            <div class="text-right cursor-pointer hover:opacity-80 transition" onclick="app.quickAdjustWalletBalance('${st.email}')" title="Click to adjust wallet balance">
                                <div class="text-xs text-slate-400 font-medium">Wallet</div>
                                <div class="text-base font-black text-emerald-700">Rs. ${st.walletBalance}</div>
                            </div>
                        </div>

                        <!-- Status Badges (Decoupled) -->
                        <div class="flex flex-wrap gap-2 pt-2 border-t border-slate-100">
                            <span class="pill-status ${isStudentBlocked ? 'bg-rose-100 text-rose-800' : 'bg-emerald-100 text-emerald-800'}">
                                Student Console: ${st.studentStatus}
                            </span>
                            <span class="pill-status ${isRiderBlocked ? 'bg-rose-100 text-rose-800' : 'bg-indigo-100 text-indigo-800'}">
                                Rider Console: ${st.riderStatus}
                            </span>
                        </div>

                        <!-- 12-Hour Reverse Countdown Timer Banners -->
                        ${isStudentBlocked && st.studentBlockUntil > 0 ? `
                            <div class="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs space-y-1">
                                <div class="font-bold text-rose-800 flex items-center justify-between">
                                    <span>Student 12h Block Remaining:</span>
                                    <span class="font-mono text-rose-700 font-black" data-countdown="${st.studentBlockUntil}">Calculating...</span>
                                </div>
                                ${st.studentBlockReason ? `<div class="text-[10px] text-rose-600">Reason: ${st.studentBlockReason}</div>` : ''}
                            </div>
                        ` : ''}

                        ${isRiderBlocked && st.riderBlockUntil > 0 ? `
                            <div class="p-3 rounded-xl bg-rose-50 border border-rose-200 text-xs space-y-1">
                                <div class="font-bold text-rose-800 flex items-center justify-between">
                                    <span>Rider 12h Block Remaining:</span>
                                    <span class="font-mono text-rose-700 font-black" data-countdown="${st.riderBlockUntil}">Calculating...</span>
                                </div>
                                ${st.riderBlockReason ? `<div class="text-[10px] text-rose-600">Reason: ${st.riderBlockReason}</div>` : ''}
                            </div>
                        ` : ''}

                        ${st.warningMessage ? `
                            <div class="p-2.5 rounded-xl bg-amber-50 border border-amber-200 text-[11px] text-amber-800 font-medium">
                                <strong>Active Warning:</strong> "${st.warningMessage}"
                            </div>
                        ` : ''}
                    </div>

                    <!-- Action Buttons -->
                    <div class="mt-4 pt-3 border-t border-slate-100 flex flex-wrap gap-2 justify-end text-xs">
                        <button onclick="app.openUserActivityDossier('${st.email}')" class="px-3 py-1.5 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-900 font-bold border border-purple-200 transition flex items-center gap-1.5 shadow-2xs">
                            <i data-lucide="file-text" class="w-3.5 h-3.5 text-purple-700"></i>
                            <span>360° Dossier</span>
                        </button>
                        ${hasActiveLock ? `
                            <button onclick="app.immediateUnblockStudent('${st.email}')" class="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold transition">
                                Unblock All
                            </button>
                        ` : ''}
                        <button onclick="app.openWarningModal('${st.email}')" class="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold transition">
                            Warn ID
                        </button>
                        <button onclick="app.openEditAccountModal('${st.email}')" class="px-3 py-1.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold transition flex items-center gap-1">
                            <i data-lucide="edit-3" class="w-3.5 h-3.5"></i>
                            <span>Edit Account</span>
                        </button>
                        <button onclick="app.openModerateStudentModal('${st.email}')" class="px-3 py-1.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold transition">
                            Moderate
                        </button>
                    </div>
                </div>
            `;
        }).join("");
    }

    immediateUnblockStudent(email) {
        const st = this.students.find(s => s.email === email);
        if (!st) return;
        st.studentStatus = "ACTIVE";
        st.riderStatus = "ACTIVE";
        st.studentBlockUntil = 0;
        st.riderBlockUntil = 0;
        this.save("campus_students_list", this.students);
        this.recordAudit("STUDENT_UNBLOCK", st.email, `Admin lifted 12h lockout for ${st.name} on all consoles.`, "INFO");
        this.showToast(`Lifted temporary lockout for ${st.name}!`);
        this.updateSectionGrid("students");
    }

    openWarningModal(email) {
        const st = this.students.find(s => s.email === email);
        const rd = this.riders.find(r => r.email === email);
        const name = (st && st.name) || (rd && rd.name) || "User";
        const targetEmail = (st && st.email) || (rd && rd.email) || email;

        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <div class="flex items-center gap-2.5 pb-2 border-b border-slate-100">
                        <div class="w-9 h-9 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
                            <i data-lucide="alert-triangle" class="w-5 h-5"></i>
                        </div>
                        <div>
                            <h3 class="text-sm font-extrabold text-slate-900">Dispatch In-App Warning</h3>
                            <p class="text-[11px] text-slate-500">Target User: <strong class="text-purple-700">${name}</strong> (${targetEmail})</p>
                        </div>
                    </div>

                    <div class="mt-3 p-3 rounded-xl bg-amber-50/80 border border-amber-200/80 text-[11px] text-amber-900 flex items-start gap-2">
                        <i data-lucide="smartphone" class="w-4 h-4 text-amber-700 shrink-0 mt-0.5"></i>
                        <span>This warning delivers <strong>live directly onto the student/rider's mobile screen</strong> inside CampusApp and their phone notification drawer. Zero email sent.</span>
                    </div>

                    <form onsubmit="app.submitWarning(event, '${targetEmail}')" class="mt-4 space-y-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">In-App Warning Message Content:</label>
                            <textarea id="warning-text" rows="3" class="w-full p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-amber-500 focus:outline-none">Official Warning: Policy violation detected. Future infractions will trigger a 12-hour temporary account lock.</textarea>
                        </div>
                        
                        <div class="flex justify-end gap-2">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold flex items-center gap-1.5 shadow-sm">
                                <i data-lucide="send" class="w-3.5 h-3.5"></i>
                                <span>Dispatch In-App Warning</span>
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) lucide.createIcons();
    }

    submitWarning(e, email) {
        e.preventDefault();
        const st = this.students.find(s => s.email === email);
        const rd = this.riders.find(r => r.email === email);
        const msg = document.getElementById("warning-text").value.trim();
        if (st) {
            st.warningMessage = msg;
            this.save("campus_students_list", this.students);
        }
        if (rd) {
            rd.warningMessage = msg;
            this.save("campus_riders_list", this.riders);
        }
        if (window.cloudSync) {
            window.cloudSync.syncWarning({ userEmail: email, warningMessage: msg });
        }
        this.recordAudit("DISPATCH_WARNING", email, `Dispatched in-app warning to ${email}: "${msg}"`, "WARNING");
        this.closeModal();
        this.showToast(`In-app warning delivered to ${email}'s mobile screen!`);
        this.updateSectionGrid("students");
        this.updateSectionGrid("riders");
    }

    openModerateStudentModal(email) {
        const st = this.students.find(s => s.email === email);
        if (!st) return;

        const options = ["ACTIVE", "TEMP_BLOCKED", "PERM_BLOCKED"];
        const modal = document.getElementById("modal-generic");

        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-lg rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <h3 class="text-base font-extrabold text-slate-900">Moderate: ${st.name}</h3>
                        <button onclick="app.closeModal()" class="font-bold text-slate-400 text-xl">&times;</button>
                    </div>

                    <form onsubmit="app.submitModerateStudent(event, '${st.email}')" class="space-y-4 mt-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Wallet Balance (Rs.):</label>
                            <input type="number" id="mod-wallet" value="${st.walletBalance}" class="w-full px-3 py-2 border border-slate-200 rounded-xl" />
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Student Console Status:</label>
                            <div class="flex gap-2">
                                ${options.map(opt => `
                                    <label class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 cursor-pointer hover:bg-slate-50 has-[:checked]:border-purple-600 has-[:checked]:bg-purple-50">
                                        <input type="radio" name="student_status_select" value="${opt}" ${st.studentStatus === opt ? 'checked' : ''} class="text-purple-600" />
                                        <span class="font-semibold text-[11px]">${opt}</span>
                                    </label>
                                `).join("")}
                            </div>
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Rider Console Status:</label>
                            <div class="flex gap-2">
                                ${options.map(opt => `
                                    <label class="flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 cursor-pointer hover:bg-slate-50 has-[:checked]:border-purple-600 has-[:checked]:bg-purple-50">
                                        <input type="radio" name="rider_status_select" value="${opt}" ${st.riderStatus === opt ? 'checked' : ''} class="text-purple-600" />
                                        <span class="font-semibold text-[11px]">${opt}</span>
                                    </label>
                                `).join("")}
                            </div>
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Reason for Lockout / Moderation Note:</label>
                            <input type="text" id="mod-reason" value="${st.riderBlockReason || st.studentBlockReason || ''}" placeholder="e.g. Heuristic Malpractice Spike" class="w-full px-3 py-2 border border-slate-200 rounded-xl" />
                        </div>

                        <div class="flex justify-end gap-2 pt-4 border-t border-slate-100">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold">Save Changes</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
    }

    submitModerateStudent(e, email) {
        e.preventDefault();
        const st = this.students.find(s => s.email === email);
        if (!st) return;

        const wallet = parseInt(document.getElementById("mod-wallet").value, 10) || 0;
        const sSelect = document.querySelector('input[name="student_status_select"]:checked')?.value || st.studentStatus;
        const rSelect = document.querySelector('input[name="rider_status_select"]:checked')?.value || st.riderStatus;
        const reason = document.getElementById("mod-reason").value.trim();

        st.walletBalance = wallet;
        const now = Date.now();

        st.studentStatus = sSelect;
        st.studentBlockUntil = sSelect === "TEMP_BLOCKED" ? (now + 12 * 3600 * 1000) : 0;
        st.studentBlockReason = sSelect === "TEMP_BLOCKED" ? (reason || "12-Hour Administrative Lock") : null;

        st.riderStatus = rSelect;
        st.riderBlockUntil = rSelect === "TEMP_BLOCKED" ? (now + 12 * 3600 * 1000) : 0;
        st.riderBlockReason = rSelect === "TEMP_BLOCKED" ? (reason || "12-Hour Administrative Lock") : null;

        this.save("campus_students_list", this.students);

        // Realtime outbound broadcast to Android user app
        if (window.cloudSync) {
            window.cloudSync.syncWallet({ userEmail: st.email, walletBalance: wallet });
            if (sSelect === "TEMP_BLOCKED") {
                window.cloudSync.syncStudentBlock({ userEmail: st.email, blockDurationHours: 12, blockReason: reason || "12-Hour Administrative Lock", studentStatus: "TEMP_BLOCKED" });
            } else if (sSelect === "ACTIVE") {
                window.cloudSync.syncStudentUnblock({ userEmail: st.email });
            }
            if (rSelect === "TEMP_BLOCKED") {
                window.cloudSync.syncRiderBlock({ userEmail: st.email, blockDurationHours: 12, blockReason: reason || "12-Hour Administrative Lock", riderStatus: "TEMP_BLOCKED" });
            } else if (rSelect === "ACTIVE") {
                window.cloudSync.syncRiderUnblock({ userEmail: st.email });
            }
        }

        this.recordAudit("STUDENT_ACCOUNT_UPDATE", st.email, `Student ${st.name}: StudentStatus=${st.studentStatus}, RiderStatus=${st.riderStatus}, Bal=Rs.${st.walletBalance}.`, "WARNING");
        this.closeModal();
        this.showToast(`Updated moderation parameters for ${st.name}!`);
        this.updateSectionGrid("students");
    }

    // ==========================================
    // TAB 3: RIDERS FLEET (Vehicle Mode Removed)
    // ==========================================
    getFilteredRiders() {
        const q = (this.searchQueries.riders || "").toLowerCase().trim();
        const fDuty = (this.ridersFilters?.duty || "ALL").toUpperCase();
        const fVerif = (this.ridersFilters?.verification || "ALL").toUpperCase();
        const fStatus = (this.ridersFilters?.status || "ALL").toUpperCase();

        return this.riders.filter(r => {
            if (fDuty === "ONLINE" && !r.isOnline) return false;
            if (fDuty === "OFFLINE" && r.isOnline) return false;
            if (fVerif !== "ALL" && (r.verificationStatus || "").toUpperCase() !== fVerif) return false;
            if (fStatus !== "ALL" && (r.riderStatus || "").toUpperCase() !== fStatus) return false;

            if (!q) return true;
            const onlineText = r.isOnline ? "online" : "offline";
            const haystack = [
                r.name,
                r.email,
                r.phone,
                r.rollNo,
                r.department,
                r.verificationStatus,
                r.activeJobId,
                r.rating,
                onlineText,
                `Rs. ${r.walletBalance}`,
                `${r.totalDeliveries} deliveries`
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderRidersTab() {
        const filtered = this.getFilteredRiders();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Courier Fleet Management</h2>
                    </div>
                    <span id="riders-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${filtered.length} Riders Listed
                    </span>
                </div>

                <!-- Courier Fleet Filter Bar -->
                ${this.renderFilterBar("riders")}

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("riders", "Search by Rider Name, Email, Phone, Roll No, KYC Status, Online/Offline...")}

                <div id="riders-grid" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    ${this.renderRiderCards(filtered)}
                </div>
            </div>
        `;
    }

    renderRiderCards(ridersList) {
        if (ridersList.length === 0) {
            return `
                <div class="col-span-full py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="bike" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No riders matching your search query.</p>
                </div>
            `;
        }

        return ridersList.map(r => {
            const isBlocked = r.riderStatus === "TEMP_BLOCKED" || (this.students.find(s => (s.email || "").toLowerCase() === (r.email || "").toLowerCase())?.riderStatus === "TEMP_BLOCKED");

            return `
            <div onclick="app.openRiderActivityDossier('${r.email}')" class="bg-white rounded-2xl p-5 border ${isBlocked ? 'border-rose-300 ring-1 ring-rose-200' : 'border-slate-200/80'} shadow-sm card-interactive flex flex-col justify-between cursor-pointer group">
                <div class="space-y-3">
                    <div class="flex items-start justify-between">
                        <div>
                            <div class="font-extrabold text-sm text-slate-900 group-hover:text-purple-700 flex items-center gap-1.5 transition">
                                <span>${r.name}</span>
                                <span class="text-[10px] px-1.5 py-0.5 rounded-md bg-purple-100 text-purple-800 font-bold opacity-0 group-hover:opacity-100 transition">360° Dossier &gt;</span>
                            </div>
                            <div class="text-[11px] text-purple-700 font-semibold">${r.email} | Roll: ${r.rollNo}</div>
                            <div class="text-[11px] text-slate-400">${r.department}</div>
                        </div>
                        <div class="flex flex-col items-end gap-1">
                            <span class="pill-status ${r.isOnline ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}">
                                ${r.isOnline ? 'ONLINE' : 'OFFLINE'}
                            </span>
                            ${isBlocked ? `
                                <span class="px-2 py-0.5 rounded-full text-[10px] font-black bg-rose-100 text-rose-800 border border-rose-200">
                                    Rider Locked
                                </span>
                            ` : ''}
                        </div>
                    </div>

                    <div class="grid grid-cols-3 gap-2 p-3 rounded-xl bg-slate-50 border border-slate-100 text-center">
                        <div>
                            <div class="text-[10px] text-slate-400 font-bold uppercase">Deliveries</div>
                            <div class="text-xs font-black text-slate-900 mt-0.5">${r.totalDeliveries}</div>
                        </div>
                        <div>
                            <div class="text-[10px] text-slate-400 font-bold uppercase">Rating</div>
                            <div class="text-xs font-black text-purple-700 mt-0.5">${r.rating}</div>
                        </div>
                        <div>
                            <div class="text-[10px] text-slate-400 font-bold uppercase">Earnings</div>
                            <div class="text-xs font-black text-emerald-700 mt-0.5">Rs. ${r.walletBalance}</div>
                        </div>
                    </div>

                    <div class="text-xs text-sky-700 font-semibold flex items-center justify-between">
                        <span>KYC: <strong class="font-bold">${r.verificationStatus}</strong></span>
                        ${r.warningMessage ? `<span class="text-[10px] px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 font-bold">⚠️ Warning Sent</span>` : ''}
                    </div>

                    ${r.activeJobId ? `
                        <div class="p-2.5 rounded-xl bg-sky-50 border border-sky-200 text-xs font-bold text-sky-800 flex items-center justify-between">
                            <span>Active Job: ${r.activeJobId}</span>
                            <span class="text-[10px] bg-sky-600 text-white px-2 py-0.5 rounded-full">In Transit</span>
                        </div>
                    ` : ''}
                </div>

                <div class="mt-4 pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-1.5 text-xs">
                    <span class="text-[11px] font-bold text-purple-700 flex items-center gap-1">
                        <i data-lucide="user-check" class="w-3.5 h-3.5"></i>
                        <span>360° Dossier</span>
                    </span>
                    <div class="flex flex-wrap items-center gap-1">
                        <button onclick="event.stopPropagation(); app.openWarningModal('${r.email}')" class="px-2 py-1 rounded-xl bg-amber-50 text-amber-800 font-bold hover:bg-amber-100 transition text-[11px] border border-amber-200" title="Dispatch warning to rider">
                            Warn
                        </button>
                        ${isBlocked ? `
                            <button onclick="event.stopPropagation(); app.unblockRiderDuty('${r.email}')" class="px-2 py-1 rounded-xl bg-emerald-600 text-white font-bold hover:bg-emerald-700 transition text-[11px]">
                                Lift Lock
                            </button>
                        ` : `
                            <button onclick="event.stopPropagation(); app.openRiderBlockModal('${r.email}')" class="px-2 py-1 rounded-xl bg-rose-600 text-white font-bold hover:bg-rose-700 transition text-[11px]" title="Temporarily block rider console">
                                Block Rider
                            </button>
                        `}
                        <button onclick="event.stopPropagation(); app.toggleRiderOnline('${r.email}')" class="px-2 py-1 rounded-xl border border-slate-200 text-slate-700 font-bold hover:bg-slate-50 transition text-[11px]">
                            ${r.isOnline ? 'Offline' : 'Online'}
                        </button>
                        <button onclick="event.stopPropagation(); app.openRiderPreviewModal('${r.email}')" class="px-2 py-1 rounded-xl bg-purple-50 text-purple-700 font-bold hover:bg-purple-100 transition text-[11px]">
                            Preview
                        </button>
                    </div>
                </div>
            </div>
            `;
        }).join("");
    }

    openRiderPreviewModal(email) {
        const rider = this.riders.find(r => r.email === email);
        if (!rider) return;

        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200 max-h-[90vh] overflow-y-auto">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <h3 class="text-base font-extrabold text-slate-900">Rider 360 Preview: ${rider.name}</h3>
                        <button onclick="app.closeModal()" class="text-slate-400 hover:text-slate-600 font-bold text-xl">&times;</button>
                    </div>

                    <div class="mt-4 space-y-3 text-xs">
                        <!-- Personal & Academic Info -->
                        <div class="p-3 rounded-xl bg-slate-50 border border-slate-100 space-y-1">
                            <div class="font-bold text-purple-900">Personal & Academic Info</div>
                            <div class="text-slate-900 font-bold text-sm">${rider.name}</div>
                            <div class="text-slate-600">Email: ${rider.email}</div>
                            <div class="text-slate-600">Phone: ${rider.phone}</div>
                            <div class="text-slate-600">Roll No: ${rider.rollNo} (${rider.department})</div>
                        </div>

                        <!-- Performance & Earnings -->
                        <div class="p-3 rounded-xl bg-emerald-50 border border-emerald-100 space-y-1">
                            <div class="font-bold text-emerald-800">Performance & Earnings</div>
                            <div class="text-slate-800">Total Completed Deliveries: <strong class="text-slate-900">${rider.totalDeliveries}</strong></div>
                            <div class="text-slate-800">Overall Rating: <strong class="text-amber-600">${rider.rating}</strong></div>
                            <div class="text-slate-800">Wallet Balance: <strong class="text-emerald-700">Rs. ${rider.walletBalance}</strong></div>
                            <div class="text-slate-800">Live Duty Status: <strong class="${rider.isOnline ? 'text-emerald-600' : 'text-slate-500'}">${rider.isOnline ? 'ONLINE' : 'OFFLINE'}</strong></div>
                        </div>

                        <!-- Full 360 Dossier CTA Button -->
                        <button onclick="app.openRiderActivityDossier('${rider.email}')" class="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-purple-700 to-indigo-800 hover:from-purple-800 hover:to-indigo-900 text-white font-black text-xs shadow-md transition flex items-center justify-center gap-2">
                            <i data-lucide="file-text" class="w-4 h-4"></i>
                            <span>Open Complete 360° Activity Dossier</span>
                        </button>

                        <!-- Action Buttons -->
                        <div class="grid grid-cols-2 gap-2 pt-2">
                            <a href="tel:${rider.phone}" class="py-2 px-3 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-center transition flex items-center justify-center gap-1.5 text-xs">
                                <i data-lucide="phone" class="w-3.5 h-3.5"></i>
                                <span>Call Rider</span>
                            </a>
                            <button onclick="app.toggleRiderVerification('${rider.email}')" class="py-2 px-3 rounded-xl ${rider.verificationStatus === 'VERIFIED' ? 'bg-amber-600 hover:bg-amber-700' : 'bg-emerald-600 hover:bg-emerald-700'} text-white font-bold text-center transition text-xs">
                                ${rider.verificationStatus === 'VERIFIED' ? 'Suspend KYC' : 'Verify KYC'}
                            </button>
                            <button onclick="app.openWarningModal('${rider.email}')" class="py-2 px-3 rounded-xl bg-amber-500 hover:bg-amber-600 text-white font-bold text-center transition flex items-center justify-center gap-1.5 text-xs">
                                <i data-lucide="alert-triangle" class="w-3.5 h-3.5"></i>
                                <span>Send Warning</span>
                            </button>
                            ${(rider.riderStatus === 'TEMP_BLOCKED' || (this.students.find(s => (s.email || '').toLowerCase() === (rider.email || '').toLowerCase())?.riderStatus === 'TEMP_BLOCKED')) ? `
                                <button onclick="app.unblockRiderDuty('${rider.email}')" class="py-2 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-center transition flex items-center justify-center gap-1.5 text-xs">
                                    <i data-lucide="unlock" class="w-3.5 h-3.5"></i>
                                    <span>Lift Rider Block</span>
                                </button>
                            ` : `
                                <button onclick="app.openRiderBlockModal('${rider.email}')" class="py-2 px-3 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-center transition flex items-center justify-center gap-1.5 text-xs">
                                    <i data-lucide="lock" class="w-3.5 h-3.5"></i>
                                    <span>Temp Block Rider</span>
                                </button>
                            `}
                            <button onclick="app.openEditAccountModal('${rider.email}')" class="py-2 px-3 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-center transition flex items-center justify-center gap-1.5 text-xs col-span-2">
                                <i data-lucide="edit-3" class="w-3.5 h-3.5"></i>
                                <span>Edit Account Attributes & PIN</span>
                            </button>
                        </div>
                    </div>

                    <div class="mt-4 pt-3 border-t border-slate-100 flex justify-end">
                        <button onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Close</button>
                    </div>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) lucide.createIcons();
    }

    openRiderBlockModal(email) {
        const rider = this.riders.find(r => (r.email || "").toLowerCase() === (email || "").toLowerCase());
        const student = this.students.find(s => (s.email || "").toLowerCase() === (email || "").toLowerCase());
        const name = (rider && rider.name) || (student && student.name) || email;
        const modal = document.getElementById("modal-generic");
        if (!modal) return;

        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-lg rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <div>
                            <h3 class="text-base font-extrabold text-slate-900 flex items-center gap-2">
                                <i data-lucide="shield-alert" class="w-5 h-5 text-rose-600"></i>
                                <span>Suspend Rider Console Only: ${name}</span>
                            </h3>
                            <p class="text-xs text-slate-500 mt-0.5">Student shopping console will remain active. Only the Rider Duty Console will be temporarily locked.</p>
                        </div>
                        <button onclick="app.closeModal()" class="font-bold text-slate-400 text-xl">&times;</button>
                    </div>

                    <form onsubmit="app.submitRiderBlock(event, '${email}')" class="space-y-4 mt-4 text-xs">
                        <div class="p-3.5 rounded-2xl bg-amber-50/80 border border-amber-200/90 space-y-2">
                            <label class="flex items-center gap-2 cursor-pointer font-bold text-amber-900">
                                <input type="checkbox" id="rider-send-warning-check" checked class="rounded text-amber-600 focus:ring-amber-500" />
                                <span>Dispatch Official In-App Warning to Rider immediately</span>
                            </label>
                            <textarea id="rider-warning-msg" rows="2" class="w-full p-2.5 bg-white border border-amber-200 rounded-xl text-slate-700 text-xs focus:ring-2 focus:ring-amber-500 focus:outline-none">Notice: Your Rider Console access has been temporarily suspended due to delivery fulfillment compliance issues.</textarea>
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1.5">Temporary Rider Lockout Duration:</label>
                            <div class="grid grid-cols-4 gap-2">
                                ${[
                                    { hours: 2, label: "2 Hours" },
                                    { hours: 6, label: "6 Hours" },
                                    { hours: 12, label: "12 Hours" },
                                    { hours: 24, label: "24 Hours" }
                                ].map((d, i) => `
                                    <label class="flex flex-col items-center justify-center p-2.5 rounded-xl border border-slate-200 cursor-pointer hover:bg-slate-50 has-[:checked]:border-rose-600 has-[:checked]:bg-rose-50 text-center">
                                        <input type="radio" name="rider_block_duration" value="${d.hours}" ${d.hours === 12 ? 'checked' : ''} class="text-rose-600" />
                                        <span class="font-black text-slate-900 mt-1">${d.label}</span>
                                    </label>
                                `).join("")}
                            </div>
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Reason for Lockout / Incident Reference:</label>
                            <input type="text" id="rider-block-reason" value="Delivery Delay & Acceptance Rate Non-Compliance" class="w-full px-3 py-2 border border-slate-200 rounded-xl font-medium focus:ring-2 focus:ring-rose-500 focus:outline-none" />
                        </div>

                        <div class="flex justify-end gap-2 pt-3 border-t border-slate-100">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-black shadow-md flex items-center gap-1.5">
                                <i data-lucide="lock" class="w-3.5 h-3.5"></i>
                                <span>Lock Rider Console</span>
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) lucide.createIcons();
    }

    submitRiderBlock(e, email) {
        e.preventDefault();
        const sendWarning = document.getElementById("rider-send-warning-check")?.checked;
        const warningMsg = document.getElementById("rider-warning-msg")?.value.trim() || "";
        const hours = parseInt(document.querySelector('input[name="rider_block_duration"]:checked')?.value || "12", 10);
        const reason = document.getElementById("rider-block-reason")?.value.trim() || "Rider Duty Suspension";
        const blockUntil = Date.now() + (hours * 3600 * 1000);

        if (sendWarning && warningMsg) {
            if (window.cloudSync) {
                window.cloudSync.syncWarning({ userEmail: email, warningMessage: warningMsg });
            }
            this.recordAudit("DISPATCH_WARNING", email, `Dispatched pre-suspension warning to rider ${email}: "${warningMsg}"`, "WARNING");
        }

        const st = this.students.find(s => (s.email || "").toLowerCase() === (email || "").toLowerCase());
        if (st) {
            st.riderStatus = "TEMP_BLOCKED";
            st.riderBlockUntil = blockUntil;
            st.riderBlockReason = reason;
            this.save("campus_students_list", this.students);
        }

        const rd = this.riders.find(r => (r.email || "").toLowerCase() === (email || "").toLowerCase());
        if (rd) {
            rd.riderStatus = "TEMP_BLOCKED";
            rd.riderBlockUntil = blockUntil;
            rd.riderBlockReason = reason;
            rd.isOnline = false;
            this.save("campus_riders_list", this.riders);
        }

        if (window.cloudSync) {
            window.cloudSync.syncRiderBlock({
                userEmail: email,
                blockDurationHours: hours,
                blockReason: reason,
                riderStatus: "TEMP_BLOCKED"
            });
        }

        this.recordCustomerActivity({
            userEmail: email,
            userName: (rd && rd.name) || (st && st.name) || email,
            userRole: "Rider",
            section: "MODERATION",
            action: "RIDER_TEMP_BLOCKED",
            title: `Rider Console Suspended (${hours} Hours)`,
            details: `Reason: ${reason} | Duration: ${hours}h | Warning Dispatched: ${sendWarning ? 'Yes' : 'No'}`,
            status: "TEMP_BLOCKED",
            badgeColor: "bg-rose-100 text-rose-800"
        });

        this.recordAudit("RIDER_TEMP_BLOCK", email, `Admin suspended Rider Console for ${email} (${hours}h): ${reason}`, "CRITICAL");
        this.closeModal();
        this.showToast(`Rider Console for ${email} locked for ${hours} hours!`, "warning");
        this.updateSectionGrid("riders");
        this.updateSectionGrid("students");
    }

    unblockRiderDuty(email) {
        const st = this.students.find(s => (s.email || "").toLowerCase() === (email || "").toLowerCase());
        if (st) {
            st.riderStatus = "ACTIVE";
            st.riderBlockUntil = 0;
            this.save("campus_students_list", this.students);
        }
        const rd = this.riders.find(r => (r.email || "").toLowerCase() === (email || "").toLowerCase());
        if (rd) {
            rd.riderStatus = "ACTIVE";
            rd.riderBlockUntil = 0;
            this.save("campus_riders_list", this.riders);
        }
        if (window.cloudSync) {
            window.cloudSync.syncRiderUnblock({ userEmail: email });
        }
        this.recordCustomerActivity({
            userEmail: email,
            userName: (rd && rd.name) || (st && st.name) || email,
            userRole: "Rider",
            section: "MODERATION",
            action: "RIDER_UNBLOCKED",
            title: "Rider Console Unblocked",
            details: "Rider duty console restored to ACTIVE status by Admin",
            status: "ACTIVE",
            badgeColor: "bg-emerald-100 text-emerald-800"
        });
        this.recordAudit("RIDER_UNBLOCK", email, `Admin unblocked Rider Console for ${email}.`, "INFO");
        this.showToast(`Rider Console for ${email} restored to ACTIVE!`);
        this.updateSectionGrid("riders");
        this.updateSectionGrid("students");
    }

    toggleRiderVerification(email) {
        const rider = this.riders.find(r => r.email === email);
        if (!rider) return;
        rider.verificationStatus = rider.verificationStatus === "VERIFIED" ? "SUSPENDED" : "VERIFIED";
        this.save("campus_riders_list", this.riders);
        this.recordAudit("RIDER_STATUS_UPDATE", rider.email, `Rider ${rider.name}: verification=${rider.verificationStatus}`, "INFO");
        this.closeModal();
        this.showToast(`Rider ${rider.name} status updated to ${rider.verificationStatus}!`);
        this.updateSectionGrid("riders");
    }

    toggleRiderOnline(email) {
        const r = this.riders.find(rd => rd.email === email);
        if (!r) return;
        r.isOnline = !r.isOnline;
        this.save("campus_riders_list", this.riders);
        this.recordAudit("RIDER_STATUS_UPDATE", r.email, `Toggled online status for rider ${r.name} to ${r.isOnline}.`, "INFO");
        this.showToast(`Rider ${r.name} is now ${r.isOnline ? 'Online' : 'Offline'}!`);
        this.updateSectionGrid("riders");
    }

    // ==========================================
    // TAB 4: SERVICES (CABS, MEALS, RENTALS)
    // ==========================================
    getFilteredServices() {
        const q = (this.searchQueries.services || "").toLowerCase().trim();
        const type = this.servicesFilters?.type || this.searchQueries.servicesType || "ALL";
        const fStatus = (this.servicesFilters?.status || "ALL").toUpperCase();

        const matchStatus = (status) => {
            if (fStatus === "ALL") return true;
            const s = (status || "").toUpperCase();
            if (fStatus === "ACTIVE") return s === "OPEN" || s === "ACTIVE" || s === "IN_PROGRESS" || s === "ORDERED";
            if (fStatus === "COMPLETED") return s === "COMPLETED" || s === "RETURNED" || s === "DELIVERED";
            if (fStatus === "CANCELLED") return s === "CANCELLED" || s === "TERMINATED";
            return s === fStatus;
        };

        const cabs = this.cabPools.filter(cab => {
            if (type !== "ALL" && type !== "CABS") return false;
            if (!matchStatus(cab.status)) return false;
            if (!q) return true;
            const chatText = (cab.chatLogs || []).map(c => `${c.senderName} ${c.sender} ${c.message}`).join(" ");
            const haystack = [
                cab.id,
                cab.pickupPoint,
                cab.dropDestination,
                cab.hostName,
                cab.hostPhone,
                cab.hostEmail,
                cab.departureDate,
                cab.departureTime,
                cab.status,
                cab.isNightRide ? "night ride" : "",
                `Rs. ${cab.perPersonFare}`,
                chatText
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });

        const meals = this.mealCircles.filter(meal => {
            if (type !== "ALL" && type !== "MEALS") return false;
            if (!matchStatus(meal.status)) return false;
            if (!q) return true;
            const chatText = (meal.chatLogs || []).map(c => `${c.senderName} ${c.sender} ${c.message}`).join(" ");
            const haystack = [
                meal.id,
                meal.restaurantSpot,
                meal.mealCategory,
                meal.hostName,
                meal.hostPhone,
                meal.meetingPoint,
                meal.targetTime,
                meal.status,
                chatText
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });

        const rentals = this.rentals.filter(rnt => {
            if (type !== "ALL" && type !== "RENTALS") return false;
            if (!matchStatus(rnt.status)) return false;
            if (!q) return true;
            const chatText = (rnt.chatLogs || []).map(c => `${c.senderName} ${c.sender} ${c.message}`).join(" ");
            const haystack = [
                rnt.id,
                rnt.productName,
                rnt.renterEmail,
                rnt.handoverOtp,
                rnt.returnOtp,
                rnt.status,
                `Rs. ${rnt.totalPaid}`,
                chatText
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });

        return { cabs, meals, rentals, totalCount: cabs.length + meals.length + rentals.length };
    }

    renderServicesTab() {
        const data = this.getFilteredServices();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Multi-Service Hub</h2>
                    </div>
                    <span id="services-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${data.totalCount} Services Found
                    </span>
                </div>

                <!-- Multi-Service Filter Bar -->
                ${this.renderFilterBar("services")}

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("services", "Search across Cabs (destination, host, chat), Meals (spot, category, chat), Rentals (product, renter, chat)...")}

                <div id="services-grid" class="space-y-6">
                    ${this.renderServicesCards(data)}
                </div>
            </div>
        `;
    }

    renderServicesCards(data) {
        const { cabs, meals, rentals, totalCount } = data;
        const type = this.searchQueries.servicesType;

        if (totalCount === 0) {
            return `
                <div class="py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="grid" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No services matching your search query.</p>
                </div>
            `;
        }

        return `
            <!-- Cab Pools -->
            ${(type === 'ALL' || type === 'CABS') && cabs.length > 0 ? `
                <div>
                    <h3 class="text-sm font-extrabold text-slate-900 mb-3 flex items-center gap-2">
                        <i data-lucide="car" class="w-4 h-4 text-purple-700"></i>
                        <span>Active Cab Pools (${cabs.length})</span>
                    </h3>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        ${cabs.map(cab => `
                            <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive text-xs space-y-3 flex flex-col justify-between">
                                <div class="space-y-2">
                                    <div class="flex items-center justify-between">
                                        <span class="font-bold text-slate-900 text-sm">${cab.id}: ${cab.pickupPoint} &gt; ${cab.dropDestination}</span>
                                        <span class="pill-status ${cab.status === 'CANCELLED' ? 'bg-rose-100 text-rose-800 font-bold' : 'bg-sky-100 text-sky-800'}">${cab.status}</span>
                                    </div>
                                    <div class="text-slate-500">Host: <span class="cursor-pointer font-bold text-slate-800 hover:text-purple-700 underline decoration-dotted" onclick="app.openUserActivityDossier('${cab.hostEmail}')">${cab.hostName}</span> (${cab.hostPhone}) | Time: ${cab.departureDate} ${cab.departureTime}</div>
                                    <div class="text-purple-700 font-bold">Fare: Rs. ${cab.perPersonFare}/seat | Seats: ${cab.bookedSeats}/${cab.totalSeats} Booked</div>
                                    ${cab.isNightRide ? `<span class="inline-block px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 font-bold text-[10px]">Night Ride Protection</span>` : ''}
                                </div>
                                <div class="pt-2.5 border-t border-slate-100 flex items-center justify-between">
                                    <span class="text-[11px] text-slate-400 flex items-center gap-1 font-medium">
                                        <i data-lucide="message-square" class="w-3.5 h-3.5 text-purple-600"></i>
                                        <span>${(cab.chatLogs || []).length} message${(cab.chatLogs || []).length === 1 ? '' : 's'}</span>
                                    </span>
                                    <button onclick="app.openSectionChatModal('cabs', '${cab.id}')" class="px-3 py-1.5 rounded-xl border border-purple-200 bg-purple-50/70 hover:bg-purple-100 text-purple-800 font-bold text-xs transition flex items-center gap-1.5 shadow-2xs cursor-pointer">
                                        <i data-lucide="message-square" class="w-3.5 h-3.5 text-purple-700"></i>
                                        <span>Ride Chat (${(cab.chatLogs || []).length})</span>
                                    </button>
                                </div>
                            </div>
                        `).join("")}
                    </div>
                </div>
            ` : ''}

            <!-- Meal Circles -->
            ${(type === 'ALL' || type === 'MEALS') && meals.length > 0 ? `
                <div>
                    <h3 class="text-sm font-extrabold text-slate-900 mb-3 flex items-center gap-2">
                        <i data-lucide="utensils" class="w-4 h-4 text-amber-600"></i>
                        <span>Meal Circles (${meals.length})</span>
                    </h3>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        ${meals.map(meal => `
                            <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive text-xs space-y-3 flex flex-col justify-between">
                                <div class="space-y-2">
                                    <div class="flex items-center justify-between">
                                        <span class="font-bold text-slate-900 text-sm">${meal.id}: ${meal.restaurantSpot}</span>
                                        <span class="pill-status ${meal.status === 'CANCELLED' ? 'bg-rose-100 text-rose-800 font-bold' : 'bg-amber-100 text-amber-800'}">${meal.status}</span>
                                    </div>
                                    <div class="text-slate-500">Category: ${meal.mealCategory} | Host: <span class="cursor-pointer font-bold text-slate-800 hover:text-purple-700 underline decoration-dotted" onclick="app.openUserActivityDossier('${meal.hostEmail}')">${meal.hostName}</span></div>
                                    <div class="text-slate-700">Buddies: ${meal.joinedBuddies}/${meal.totalBuddies} Joined | Meet: ${meal.meetingPoint} @ ${meal.targetTime}</div>
                                </div>
                                <div class="pt-2.5 border-t border-slate-100 flex items-center justify-between">
                                    <span class="text-[11px] text-slate-400 flex items-center gap-1 font-medium">
                                        <i data-lucide="message-square" class="w-3.5 h-3.5 text-amber-600"></i>
                                        <span>${(meal.chatLogs || []).length} message${(meal.chatLogs || []).length === 1 ? '' : 's'}</span>
                                    </span>
                                    <button onclick="app.openSectionChatModal('meals', '${meal.id}')" class="px-3 py-1.5 rounded-xl border border-amber-200 bg-amber-50/70 hover:bg-amber-100 text-amber-800 font-bold text-xs transition flex items-center gap-1.5 shadow-2xs cursor-pointer">
                                        <i data-lucide="message-square" class="w-3.5 h-3.5 text-amber-700"></i>
                                        <span>Meal Chat (${(meal.chatLogs || []).length})</span>
                                    </button>
                                </div>
                            </div>
                        `).join("")}
                    </div>
                </div>
            ` : ''}

            <!-- Item Rentals -->
            ${(type === 'ALL' || type === 'RENTALS') && rentals.length > 0 ? `
                <div>
                    <h3 class="text-sm font-extrabold text-slate-900 mb-3 flex items-center gap-2">
                        <i data-lucide="package" class="w-4 h-4 text-emerald-600"></i>
                        <span>Item Rentals (${rentals.length})</span>
                    </h3>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        ${rentals.map(rnt => `
                            <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive text-xs space-y-3 flex flex-col justify-between">
                                <div class="space-y-2">
                                    <div class="flex items-center justify-between">
                                        <span class="font-bold text-slate-900 text-sm">${rnt.id}: ${rnt.productName}</span>
                                        <span class="pill-status ${rnt.status === 'CANCELLED' ? 'bg-rose-100 text-rose-800 font-bold' : 'bg-emerald-100 text-emerald-800'}">${rnt.status}</span>
                                    </div>
                                    <div class="text-slate-500">Renter: <span class="cursor-pointer font-bold text-purple-700 hover:text-purple-900 underline decoration-dotted" onclick="app.openUserActivityDossier('${rnt.renterEmail}')">${rnt.renterEmail}</span> | Duration: ${rnt.durationDays} Days</div>
                                    <div class="text-emerald-700 font-bold">Total Paid: Rs. ${rnt.totalPaid} | Handover OTP: ${rnt.handoverOtp} | Return OTP: ${rnt.returnOtp}</div>
                                </div>
                                <div class="pt-2.5 border-t border-slate-100 flex items-center justify-between">
                                    <span class="text-[11px] text-slate-400 flex items-center gap-1 font-medium">
                                        <i data-lucide="message-square" class="w-3.5 h-3.5 text-emerald-600"></i>
                                        <span>${(rnt.chatLogs || []).length} message${(rnt.chatLogs || []).length === 1 ? '' : 's'}</span>
                                    </span>
                                    <button onclick="app.openSectionChatModal('rentals', '${rnt.id}')" class="px-3 py-1.5 rounded-xl border border-emerald-200 bg-emerald-50/70 hover:bg-emerald-100 text-emerald-800 font-bold text-xs transition flex items-center gap-1.5 shadow-2xs cursor-pointer">
                                        <i data-lucide="message-square" class="w-3.5 h-3.5 text-emerald-700"></i>
                                        <span>Rental Chat (${(rnt.chatLogs || []).length})</span>
                                    </button>
                                </div>
                            </div>
                        `).join("")}
                    </div>
                </div>
            ` : ''}
        `;
    }

    setServicesFilter(type) {
        this.servicesFilters.type = type;
        this.searchQueries.servicesType = type;
        this.renderCurrentTab();
    }

    // ==========================================
    // TAB 5: DISPUTES & ESCROW DESK
    // ==========================================
    getFilteredDisputes() {
        const q = (this.searchQueries.disputes || "").toLowerCase().trim();
        const filter = this.disputesFilters?.tab || this.searchQueries.disputesTab || "OPEN";
        const valTier = this.disputesFilters?.value || "ALL";

        return this.disputes.filter(d => {
            if (filter === "OPEN" && !d.status.startsWith("OPEN") && !d.status.startsWith("UNDER")) return false;
            if (filter === "RESOLVED" && !d.status.startsWith("RESOLVED")) return false;

            if (valTier === "UNDER_500" && (Number(d.escrowAmount) || 0) >= 500) return false;
            if (valTier === "ABOVE_500" && (Number(d.escrowAmount) || 0) < 500) return false;

            if (!q) return true;
            const chatText = (d.chatLogs || []).map(c => `${c.senderName} ${c.sender} ${c.message}`).join(" ");
            const haystack = [
                d.id,
                d.orderId,
                d.serviceType,
                d.complainantName,
                d.complainantEmail,
                d.complainantPhone,
                d.respondentName,
                d.respondentEmail,
                d.respondentRole,
                d.issueCategory,
                d.description,
                d.status,
                d.adminResolutionNote,
                chatText,
                `Rs. ${d.escrowAmount}`
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderDisputesTab() {
        const filtered = this.getFilteredDisputes();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Disputes & Arbitration Desk</h2>
                        <p class="text-xs text-slate-500 mt-1">Review student claims, evidence, and issue refund payouts.</p>
                    </div>
                    <span id="disputes-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${filtered.length} Cases in View
                    </span>
                </div>

                <!-- Disputes Filter Bar -->
                ${this.renderFilterBar("disputes")}

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("disputes", "Search by Dispute ID, Order ID, Complainant, Respondent, Issue, Transcript...")}

                <div id="disputes-grid" class="grid grid-cols-1 lg:grid-cols-2 gap-4">
                    ${this.renderDisputeCards(filtered)}
                </div>
            </div>
        `;
    }

    renderDisputeCards(disputesList) {
        if (disputesList.length === 0) {
            return `
                <div class="col-span-full py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="shield-check" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No dispute cases matching criteria.</p>
                </div>
            `;
        }

        return disputesList.map(d => {
            const isResolved = d.status.startsWith("RESOLVED");

            return `
                <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive space-y-3">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <div>
                            <span class="font-black text-sm text-purple-900">${d.id} (${d.serviceType})</span>
                            <span class="text-xs text-slate-400 ml-2">Order: ${d.orderId}</span>
                        </div>
                        <span class="pill-status ${isResolved ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}">${d.status}</span>
                    </div>

                    <div class="text-xs space-y-1">
                        <div class="font-bold text-slate-900">Issue: ${d.issueCategory}</div>
                        <div class="text-slate-500">
                            Complainant: <span class="font-bold text-slate-800 hover:text-purple-700 cursor-pointer underline decoration-dotted" onclick="app.openUserActivityDossier('${d.complainantEmail}')">${d.complainantName}</span> 
                            vs 
                            <span class="font-bold text-slate-800 hover:text-purple-700 cursor-pointer underline decoration-dotted" onclick="app.openUserActivityDossier('${d.respondentEmail}')">${d.respondentName} (${d.respondentRole})</span>
                        </div>
                        <div class="text-slate-700">Statement: "${d.description}"</div>
                        <div class="text-sky-700 font-bold">Escrow At Stake: Rs. ${d.escrowAmount} | Filed: ${d.filedTimestamp}</div>
                    </div>

                    <div class="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                        <button onclick="app.openSectionChatModal('disputes', '${d.id}')" class="px-3.5 py-2 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-700 font-bold text-xs transition flex items-center gap-1.5 shadow-2xs cursor-pointer">
                            <i data-lucide="message-square" class="w-3.5 h-3.5 text-purple-700"></i>
                            <span>Chat (${(d.chatLogs || []).length})</span>
                        </button>
                        <button onclick="app.openDisputeReviewModal('${d.id}')" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs transition shadow-sm cursor-pointer">
                            Review Evidence & Arbitrate
                        </button>
                    </div>
                </div>
            `;
        }).join("");
    }

    setDisputesFilter(filter) {
        this.disputesFilters.tab = filter;
        this.searchQueries.disputesTab = filter;
        this.renderCurrentTab();
    }

    openDisputeReviewModal(disputeId) {
        const dispute = this.disputes.find(d => d.id === disputeId);
        if (!dispute) return;

        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-xl rounded-3xl p-6 shadow-2xl border border-slate-200 max-h-[90vh] overflow-y-auto">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <h3 class="text-base font-extrabold text-slate-900">Dispute Arbitration: ${dispute.id}</h3>
                        <button onclick="app.closeModal()" class="text-slate-400 hover:text-slate-600 font-bold text-xl">&times;</button>
                    </div>

                    <div class="mt-4 space-y-4 text-xs">
                        <!-- Parties Info -->
                        <div class="p-3 rounded-xl bg-slate-50 border border-slate-100 space-y-1">
                            <div><strong>Complainant:</strong> ${dispute.complainantName} (${dispute.complainantEmail}) - ${dispute.complainantPhone}</div>
                            <div><strong>Respondent:</strong> ${dispute.respondentName} (${dispute.respondentEmail}) - ${dispute.respondentRole} | Order Ref: ${dispute.orderId}</div>
                        </div>

                        <!-- Chat Logs & Message Transcript -->
                        <div>
                            <div class="flex items-center justify-between mb-2">
                                <div class="font-bold text-slate-900">Chat Logs & Message Transcript (${(dispute.chatLogs || []).length}):</div>
                                <button type="button" onclick="app.openSectionChatModal('disputes', '${dispute.id}')" class="px-2.5 py-1 rounded-lg border border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-800 font-bold text-[11px] transition flex items-center gap-1 shadow-2xs">
                                    <i data-lucide="message-square-plus" class="w-3 h-3 text-purple-700"></i>
                                    <span>Manage & Save Chat</span>
                                </button>
                            </div>
                            <div class="p-3 rounded-xl bg-slate-50 border border-slate-100 space-y-2 max-h-48 overflow-y-auto">
                                ${dispute.chatLogs.map(c => {
                                    const isComplainant = c.sender === "Complainant";
                                    const isSystem = c.sender === "System";
                                    const bubbleClass = isSystem ? 'bg-amber-50 border-amber-200' : isComplainant ? 'bg-purple-50 border-purple-200' : 'bg-emerald-50 border-emerald-200';
                                    const senderColor = isSystem ? 'text-amber-800' : isComplainant ? 'text-purple-800' : 'text-emerald-800';

                                    return `
                                        <div class="p-2.5 rounded-xl border ${bubbleClass} text-xs">
                                            <div class="flex items-center justify-between mb-1">
                                                <span class="font-bold ${senderColor}">${c.senderName} (${c.sender})</span>
                                                <span class="text-[10px] text-slate-400">${c.time}</span>
                                            </div>
                                            <div class="text-slate-800">${c.message}</div>
                                        </div>
                                    `;
                                }).join("")}
                            </div>
                        </div>

                        <!-- Attached Evidence -->
                        <div>
                            <div class="font-bold text-slate-900 mb-1">Attached Evidence & Receipts:</div>
                            <div class="p-3 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 font-medium">
                                ${dispute.evidenceNotes || "Attached Photos and Communication Timestamps"}
                            </div>
                        </div>

                        <!-- Manual Verification & Refund Control -->
                        <div class="space-y-3 pt-2 border-t border-slate-100">
                            <div>
                                <label class="block font-bold text-slate-700 mb-1">Admin Findings & Notes *</label>
                                <textarea id="dsp-notes" rows="2" placeholder="Document evidence verification details..." class="w-full p-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-purple-600 focus:outline-none">${dispute.adminResolutionNote || ''}</textarea>
                            </div>

                            <div>
                                <label class="block font-bold text-slate-700 mb-1">Manual Refund Amount (Rs.):</label>
                                <input type="number" id="dsp-refund-amt" value="${dispute.escrowAmount}" class="w-full px-3 py-2 border border-slate-200 rounded-xl" />
                            </div>

                            <div class="grid grid-cols-2 gap-3 pt-2">
                                <button onclick="app.executeDisputeArbitration('${dispute.id}', 'MANUAL_FULL_REFUND')" class="py-2.5 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold transition">
                                    Approve Refund
                                </button>
                                <button onclick="app.executeDisputeArbitration('${dispute.id}', 'DISMISS_DISPUTE_NO_REFUND')" class="py-2.5 px-4 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold transition">
                                    Dismiss Claim
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
    }

    executeDisputeArbitration(disputeId, actionType) {
        const dispute = this.disputes.find(d => d.id === disputeId);
        if (!dispute) return;

        const notes = document.getElementById("dsp-notes")?.value.trim() || "Manually verified by admin arbitration desk.";
        const refundAmt = parseInt(document.getElementById("dsp-refund-amt")?.value, 10) || 0;

        if (actionType === "MANUAL_FULL_REFUND") {
            if (!confirm(`Are you sure you have manually verified the evidence and want to refund Rs. ${refundAmt} to student ${dispute.complainantName}?`)) return;
            dispute.status = "RESOLVED_MANUAL_REFUND";
            dispute.adminResolutionNote = notes;
            dispute.refundAmount = refundAmt;
            dispute.resolvedBy = this.activeRole;

            // Credit student wallet
            const st = this.students.find(s => s.email === dispute.complainantEmail);
            if (st) {
                st.walletBalance += refundAmt;
                this.save("campus_students_list", this.students);
            }

            this.recordAudit("DISPUTE_MANUAL_REFUND", dispute.id, `Dispute ${dispute.id} resolved: Full refund of Rs. ${refundAmt} issued to ${dispute.complainantName}. Note: ${notes}`, "WARNING");
            this.showToast(`Refund of Rs. ${refundAmt} issued to ${dispute.complainantName}!`);
        } else {
            if (!confirm(`Are you sure you want to dismiss this dispute without refunding? Escrow will be released to respondent ${dispute.respondentName}.`)) return;
            dispute.status = "RESOLVED_DISMISSED";
            dispute.adminResolutionNote = notes;
            dispute.refundAmount = 0;
            dispute.resolvedBy = this.activeRole;

            this.recordAudit("DISPUTE_DISMISSED", dispute.id, `Dispute ${dispute.id} dismissed without refund. Escrow released to ${dispute.respondentName}. Note: ${notes}`, "INFO");
            this.showToast(`Dispute dismissed. Escrow released to ${dispute.respondentName}.`);
        }

        this.save("admin_dispute_cases", this.disputes);
        this.closeModal();
        this.updateSectionGrid("disputes");
    }

    // ==========================================
    // UNIFIED CHAT TRANSCRIPT & PERSISTENCE SUBSYSTEM
    // ==========================================
    getItemForChat(section, itemId) {
        let item = null;
        let sectionLabel = "";
        let title = "";
        let subtitle = "";
        let status = "";
        let storageKey = "";

        const sec = (section || "").toLowerCase();

        if (sec === "cabs" || sec === "cab_pools" || sec === "cabpools") {
            item = this.cabPools.find(c => c.id === itemId);
            sectionLabel = "Ride / Cab Pool";
            title = item ? `${item.pickupPoint} ➔ ${item.dropDestination}` : itemId;
            subtitle = item ? `Host: ${item.hostName} (${item.hostEmail}) • Departure: ${item.departureDate} ${item.departureTime}` : "";
            status = item?.status || "";
            storageKey = "campus_cab_pools";
        } else if (sec === "meals" || sec === "meal_circles" || sec === "mealcircles") {
            item = this.mealCircles.find(m => m.id === itemId);
            sectionLabel = "Meal Circle Gathering";
            title = item ? `${item.restaurantSpot} (${item.mealCategory})` : itemId;
            subtitle = item ? `Host: ${item.hostName} (${item.hostEmail}) • Meeting: ${item.meetingPoint} @ ${item.targetTime}` : "";
            status = item?.status || "";
            storageKey = "campus_meal_gatherings";
        } else if (sec === "rentals" || sec === "rental") {
            item = this.rentals.find(r => r.id === itemId);
            sectionLabel = "Item Rental";
            title = item ? `${item.productName}` : itemId;
            subtitle = item ? `Renter: ${item.renterEmail} • Duration: ${item.durationDays} Days • Total Paid: Rs. ${item.totalPaid}` : "";
            status = item?.status || "";
            storageKey = "campus_rentals_list";
        } else if (sec === "orders" || sec === "order") {
            item = this.orders.find(o => o.id === itemId);
            sectionLabel = "Delivery Order";
            title = item ? `${item.pickPoint} ➔ ${item.dropPoint}` : itemId;
            subtitle = item ? `Customer: ${item.customerName || item.customerId} • Runner: ${item.runnerName || "Unassigned"}` : "";
            status = item?.status || "";
            storageKey = "app_orders_list";
        } else if (sec === "disputes" || sec === "dispute") {
            item = this.disputes.find(d => d.id === itemId);
            sectionLabel = "Dispute & Mediation";
            title = item ? `${item.issueCategory} (Order: ${item.orderId})` : itemId;
            subtitle = item ? `Complainant: ${item.complainantName} vs ${item.respondentName} (${item.respondentRole})` : "";
            status = item?.status || "";
            storageKey = "admin_dispute_cases";
        } else if (sec === "lostfound" || sec === "lost_found") {
            item = this.lostFound.find(l => l.id === itemId);
            sectionLabel = "Lost & Found Vault";
            title = item ? `${item.title} (${item.type})` : itemId;
            subtitle = item ? `Reporter: ${item.reporterName} • Location: ${item.location}` : "";
            status = item?.status || "";
            storageKey = "campus_lost_found_items";
        }

        if (item && !Array.isArray(item.chatLogs)) {
            item.chatLogs = [];
        }

        return { item, section: sec, sectionLabel, title, subtitle, status, storageKey };
    }

    openSectionChatModal(section, itemId) {
        const info = this.getItemForChat(section, itemId);
        if (!info || !info.item) {
            this.showToast(`Record ${itemId} not found for chat.`);
            return;
        }

        const { item, sectionLabel, title, subtitle, status } = info;
        const chatLogs = item.chatLogs || [];
        const modal = document.getElementById("modal-generic");
        if (!modal) return;

        const roleColorMap = {
            "Host": "text-purple-700 bg-purple-50 border-purple-200",
            "Rider": "text-emerald-700 bg-emerald-50 border-emerald-200",
            "Buddy": "text-amber-700 bg-amber-50 border-amber-200",
            "Customer": "text-sky-700 bg-sky-50 border-sky-200",
            "Borrower": "text-indigo-700 bg-indigo-50 border-indigo-200",
            "Owner": "text-teal-700 bg-teal-50 border-teal-200",
            "Complainant": "text-purple-700 bg-purple-50 border-purple-200",
            "Respondent": "text-emerald-700 bg-emerald-50 border-emerald-200",
            "Reporter": "text-blue-700 bg-blue-50 border-blue-200",
            "Security": "text-amber-800 bg-amber-50 border-amber-200",
            "Finder": "text-emerald-800 bg-emerald-50 border-emerald-200",
            "Admin": "text-purple-900 bg-purple-100 border-purple-300 font-bold",
            "Admin Notice": "text-rose-900 bg-rose-50 border-rose-300 font-bold"
        };

        const activeAdminRoleTitle = (this.activeRole || "SUPER_ADMIN").replace(/_/g, " ");

        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-3 sm:p-4 z-50">
                <div class="modal-content bg-white w-full max-w-2xl rounded-3xl p-5 sm:p-6 shadow-2xl border border-slate-200 max-h-[92vh] flex flex-col justify-between overflow-hidden">
                    
                    <!-- Header -->
                    <div class="flex items-start justify-between pb-3.5 border-b border-slate-100 shrink-0">
                        <div class="flex items-center gap-2.5">
                            <span class="p-2.5 rounded-2xl bg-purple-100 text-purple-700 shrink-0">
                                <i data-lucide="message-square" class="w-5 h-5"></i>
                            </span>
                            <div>
                                <div class="flex flex-wrap items-center gap-2">
                                    <span class="text-[10px] font-black uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700">${sectionLabel}</span>
                                    <span class="text-xs font-black text-purple-900">${itemId}</span>
                                    ${status ? `<span class="pill-status bg-slate-100 text-slate-700 text-[10px]">${status}</span>` : ''}
                                </div>
                                <h3 class="text-sm sm:text-base font-extrabold text-slate-900 mt-1 line-clamp-1">${title}</h3>
                                <p class="text-[11px] text-slate-500 line-clamp-1">${subtitle}</p>
                            </div>
                        </div>
                        <button onclick="app.closeModal()" class="text-slate-400 hover:text-slate-600 font-bold text-2xl leading-none p-1 transition">&times;</button>
                    </div>

                    <!-- Action Bar: Export & Copy & Count -->
                    <div class="py-2.5 px-3 bg-slate-50 border-b border-slate-100 flex items-center justify-between text-xs shrink-0 rounded-xl my-2">
                        <div class="flex items-center gap-2 text-slate-600 font-medium text-[11px]">
                            <i data-lucide="archive" class="w-3.5 h-3.5 text-purple-600"></i>
                            <span id="chat-modal-count" class="font-bold">${chatLogs.length} Saved Transcript Entr${chatLogs.length === 1 ? 'y' : 'ies'}</span>
                        </div>
                        <div class="flex items-center gap-2">
                            <button onclick="app.copyChatTranscript('${section}', '${itemId}')" class="px-2.5 py-1 rounded-lg border border-slate-200 hover:bg-white text-slate-600 font-semibold text-[11px] transition flex items-center gap-1 shadow-2xs" title="Copy entire chat text to clipboard">
                                <i data-lucide="copy" class="w-3 h-3 text-slate-500"></i>
                                <span>Copy</span>
                            </button>
                            <button onclick="app.exportChatTranscript('${section}', '${itemId}')" class="px-2.5 py-1 rounded-lg border border-slate-200 hover:bg-white text-slate-600 font-semibold text-[11px] transition flex items-center gap-1 shadow-2xs" title="Download text transcript for legal or academic reference">
                                <i data-lucide="download" class="w-3 h-3 text-purple-700"></i>
                                <span>Export (.txt)</span>
                            </button>
                        </div>
                    </div>

                    <!-- Transcript Stream -->
                    <div id="chat-transcript-scroll" class="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50/60 rounded-2xl border border-slate-100 my-1 min-h-[200px] max-h-[320px]">
                        ${chatLogs.length === 0 ? `
                            <div class="py-12 text-center text-slate-400">
                                <i data-lucide="messages-square" class="w-8 h-8 mx-auto mb-2 opacity-40"></i>
                                <p class="text-xs font-semibold text-slate-600">No chat transcript entries recorded yet.</p>
                                <p class="text-[11px] text-slate-400 mt-1">Use the admin composer below to record participant communications or post an official admin notice.</p>
                            </div>
                        ` : chatLogs.map((msg, idx) => {
                            const isAdmin = Boolean(msg.isAdmin || (msg.sender && msg.sender.toLowerCase().includes("admin")) || (msg.senderRole && msg.senderRole.toLowerCase().includes("admin")));
                            const senderTag = msg.sender || (isAdmin ? "Admin" : "User");
                            const badgeStyle = roleColorMap[senderTag] || (isAdmin ? roleColorMap["Admin"] : "text-slate-700 bg-slate-100 border-slate-200");

                            return `
                                <div class="p-3 rounded-2xl border ${isAdmin ? 'bg-purple-50/90 border-purple-200 shadow-2xs ring-1 ring-purple-300/40' : 'bg-white border-slate-200/80 shadow-2xs'} text-xs space-y-1.5 transition">
                                    <div class="flex items-center justify-between pb-1 border-b ${isAdmin ? 'border-purple-100' : 'border-slate-100'}">
                                        <div class="flex items-center gap-2">
                                            <span class="font-extrabold ${isAdmin ? 'text-purple-950 font-black' : 'text-slate-900'}">${msg.senderName || msg.sender}</span>
                                            <span class="text-[10px] font-bold px-2 py-0.5 rounded-full border ${badgeStyle}">
                                                ${isAdmin ? '🛡️ ' : ''}${senderTag}
                                            </span>
                                        </div>
                                        <div class="flex items-center gap-2">
                                            <span class="text-[10px] text-slate-400 font-medium">${msg.time || "Just now"}</span>
                                            <button onclick="app.deleteSectionChatMessage('${section}', '${itemId}', ${idx})" class="text-slate-300 hover:text-rose-600 transition p-0.5" title="Remove message entry">
                                                <i data-lucide="trash-2" class="w-3 h-3"></i>
                                            </button>
                                        </div>
                                    </div>
                                    <p class="text-slate-800 leading-relaxed break-words whitespace-pre-wrap">${msg.message}</p>
                                </div>
                            `;
                        }).join("")}
                    </div>

                    <!-- Admin Message Input & Save Form -->
                    <div class="shrink-0 pt-3 border-t border-slate-100 space-y-2.5">
                        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                            <div class="flex items-center gap-2 text-xs">
                                <label for="chat-admin-sender" class="text-[10px] font-black text-slate-500 uppercase tracking-wider">Save Message As:</label>
                                <select id="chat-admin-sender" class="px-2.5 py-1 bg-slate-100 border border-slate-200 rounded-xl text-xs font-bold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600">
                                    <option value="Admin">Admin (${activeAdminRoleTitle})</option>
                                    <option value="Admin Notice">Official Administrative Notice</option>
                                    <option value="Host">Host / Service Initiator</option>
                                    <option value="Participant">Participant / Co-rider / Buddy</option>
                                    <option value="Security">Security Desk Dispatch</option>
                                </select>
                            </div>

                            <!-- Quick preset chips -->
                            <div class="flex flex-wrap items-center gap-1 text-[10px]">
                                <span class="text-slate-400 font-bold uppercase mr-0.5">Quick:</span>
                                <button type="button" onclick="document.getElementById('chat-admin-input').value = 'Booking details and campus safety protocols verified by admin.'" class="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 transition">Verified</button>
                                <button type="button" onclick="document.getElementById('chat-admin-input').value = 'Admin Notice: Please ensure OTP handover and follow campus security rules.'" class="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 transition">Notice</button>
                                <button type="button" onclick="document.getElementById('chat-admin-input').value = 'Item handover completed and confirmed at designated spot.'" class="px-2 py-0.5 rounded-md bg-slate-100 hover:bg-slate-200 text-slate-700 transition">Handover</button>
                            </div>
                        </div>

                        <div class="flex items-center gap-2">
                            <textarea id="chat-admin-input" rows="2" placeholder="Write admin intervention, chat transcript note, or message..." class="flex-1 p-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs font-medium focus:ring-2 focus:ring-purple-600 focus:outline-none resize-none shadow-inner" onkeydown="if(event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); app.saveSectionChatMessage('${section}', '${itemId}'); }"></textarea>
                            <button onclick="app.saveSectionChatMessage('${section}', '${itemId}')" class="px-4 py-3 rounded-2xl bg-purple-700 hover:bg-purple-800 active:scale-95 text-white font-extrabold text-xs transition flex items-center justify-center gap-1.5 shadow-md shadow-purple-900/20 shrink-0 self-stretch sm:self-auto">
                                <i data-lucide="send" class="w-4 h-4"></i>
                                <span class="hidden sm:inline">Save to Chat</span>
                            </button>
                        </div>
                        <p class="text-[10px] text-slate-400">All chat entries are permanently archived to the database and can be referred to at any time. Press <kbd class="px-1 py-0.5 bg-slate-100 border border-slate-200 rounded text-[9px] font-mono">Enter</kbd> to save.</p>
                    </div>

                </div>
            </div>
        `;

        modal.classList.remove("hidden");
        if (window.lucide) lucide.createIcons();

        // Scroll transcript to bottom
        setTimeout(() => {
            const container = document.getElementById("chat-transcript-scroll");
            if (container) container.scrollTop = container.scrollHeight;
            const input = document.getElementById("chat-admin-input");
            if (input) input.focus();
        }, 50);
    }

    saveSectionChatMessage(section, itemId) {
        const input = document.getElementById("chat-admin-input");
        const senderSelect = document.getElementById("chat-admin-sender");
        if (!input) return;

        const text = input.value.trim();
        if (!text) {
            this.showToast("Please enter a chat message or note.");
            return;
        }

        const info = this.getItemForChat(section, itemId);
        if (!info || !info.item) {
            this.showToast("Record not found.");
            return;
        }

        const senderTag = senderSelect ? senderSelect.value : "Admin";
        const isAdmin = senderTag.toLowerCase().includes("admin");
        const activeRoleName = (this.activeRole || "SUPER_ADMIN").replace(/_/g, " ");

        const senderName = isAdmin 
            ? `${activeRoleName} (Admin)`
            : senderTag === "Security" 
            ? "Campus Security Officer" 
            : `${senderTag} Participant`;

        const now = new Date();
        const timeFormatted = now.toLocaleTimeString("en-IN", { hour: "numeric", minute: "2-digit", hour12: true }) + ", Today";

        const newMsg = {
            id: "MSG-" + Date.now(),
            sender: senderTag,
            senderName: senderName,
            senderRole: isAdmin ? this.activeRole : senderTag.toUpperCase(),
            message: text,
            time: timeFormatted,
            timestamp: Date.now(),
            isAdmin: isAdmin
        };

        if (!Array.isArray(info.item.chatLogs)) {
            info.item.chatLogs = [];
        }
        info.item.chatLogs.push(newMsg);

        // Save to respective localStorage key
        if (info.storageKey) {
            const sec = (section || "").toLowerCase();
            if (sec === "cabs" || sec === "cab_pools" || sec === "cabpools") this.save(info.storageKey, this.cabPools);
            else if (sec === "meals" || sec === "meal_circles" || sec === "mealcircles") this.save(info.storageKey, this.mealCircles);
            else if (sec === "rentals" || sec === "rental") this.save(info.storageKey, this.rentals);
            else if (sec === "orders" || sec === "order") this.save(info.storageKey, this.orders);
            else if (sec === "disputes" || sec === "dispute") this.save(info.storageKey, this.disputes);
            else if (sec === "lostfound" || sec === "lost_found") this.save(info.storageKey, this.lostFound);
        }

        // Record audit trail entry
        this.recordAudit(
            "CHAT_INTERVENTION_SAVED",
            itemId,
            `Admin saved chat entry in ${info.sectionLabel} (${itemId}): "${text.length > 50 ? text.slice(0, 50) + '...' : text}"`,
            "INFO"
        );

        this.showToast(`Chat entry saved for ${itemId}!`);

        // Refresh modal to show updated chat history
        this.openSectionChatModal(section, itemId);

        // Refresh underlying background section grid so the chat counter updates live
        const sec = (section || "").toLowerCase();
        if (sec === "cabs" || sec === "meals" || sec === "rentals") {
            this.updateSectionGrid("services");
        } else if (sec === "orders" || sec === "order") {
            this.updateSectionGrid("orders");
        } else if (sec === "disputes" || sec === "dispute") {
            this.updateSectionGrid("disputes");
        } else if (sec === "lostfound" || sec === "lost_found") {
            this.updateSectionGrid("lostFound");
        }
    }

    deleteSectionChatMessage(section, itemId, msgIndex) {
        if (!confirm("Are you sure you want to remove this chat log entry?")) return;
        const info = this.getItemForChat(section, itemId);
        if (!info || !info.item || !Array.isArray(info.item.chatLogs)) return;

        info.item.chatLogs.splice(msgIndex, 1);

        if (info.storageKey) {
            const sec = (section || "").toLowerCase();
            if (sec === "cabs" || sec === "cab_pools" || sec === "cabpools") this.save(info.storageKey, this.cabPools);
            else if (sec === "meals" || sec === "meal_circles" || sec === "mealcircles") this.save(info.storageKey, this.mealCircles);
            else if (sec === "rentals" || sec === "rental") this.save(info.storageKey, this.rentals);
            else if (sec === "orders" || sec === "order") this.save(info.storageKey, this.orders);
            else if (sec === "disputes" || sec === "dispute") this.save(info.storageKey, this.disputes);
            else if (sec === "lostfound" || sec === "lost_found") this.save(info.storageKey, this.lostFound);
        }

        this.recordAudit("CHAT_ENTRY_DELETED", itemId, `Admin deleted message index ${msgIndex} from ${info.sectionLabel} (${itemId})`, "WARNING");
        this.showToast("Message entry removed.");
        this.openSectionChatModal(section, itemId);

        const sec = (section || "").toLowerCase();
        if (sec === "cabs" || sec === "meals" || sec === "rentals") {
            this.updateSectionGrid("services");
        } else if (sec === "orders" || sec === "order") {
            this.updateSectionGrid("orders");
        } else if (sec === "disputes" || sec === "dispute") {
            this.updateSectionGrid("disputes");
        } else if (sec === "lostfound" || sec === "lost_found") {
            this.updateSectionGrid("lostFound");
        }
    }

    exportChatTranscript(section, itemId) {
        const info = this.getItemForChat(section, itemId);
        if (!info || !info.item) return;

        const chatLogs = info.item.chatLogs || [];
        const lines = [
            "================================================================================",
            `CAMPUS ADMIN CONSOLE - ARCHIVED CHAT TRANSCRIPT`,
            "================================================================================",
            `Section:         ${info.sectionLabel}`,
            `Record ID:       ${itemId}`,
            `Context / Title: ${info.title}`,
            `Subtitle:        ${info.subtitle}`,
            `Current Status:  ${info.status}`,
            `Exported By:     ${this.activeRole} (Admin)`,
            `Exported At:     ${new Date().toLocaleString("en-IN")}`,
            `Total Messages:  ${chatLogs.length}`,
            "================================================================================",
            ""
        ];

        if (chatLogs.length === 0) {
            lines.push("No chat transcript messages recorded.");
        } else {
            chatLogs.forEach((msg, i) => {
                lines.push(`[${i + 1}] ${msg.time || "Time Unknown"} | ${msg.senderName || msg.sender} (${msg.sender || "User"}):`);
                lines.push(`    ${msg.message}`);
                lines.push("");
            });
        }

        lines.push("================================================================================");
        lines.push("END OF ARCHIVE TRANSCRIPT");
        lines.push("================================================================================");

        this.downloadTextFile(`chat_${itemId}_transcript.txt`, lines.join("\n"));
    }

    copyChatTranscript(section, itemId) {
        const info = this.getItemForChat(section, itemId);
        if (!info || !info.item) return;

        const chatLogs = info.item.chatLogs || [];
        const formatted = chatLogs.map((msg, i) => `[${msg.time || "Time Unknown"}] ${msg.senderName || msg.sender} (${msg.sender || "User"}): ${msg.message}`).join("\n");
        const fullText = `--- CHAT TRANSCRIPT: ${itemId} (${info.sectionLabel}) ---\n${formatted || "No messages recorded."}`;

        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(fullText).then(() => {
                this.showToast(`Chat transcript for ${itemId} copied to clipboard!`);
            }).catch(() => {
                this.showToast(`Unable to copy to clipboard.`);
            });
        } else {
            this.showToast(`Clipboard API not supported in browser.`);
        }
    }

    // ==========================================
    // TAB 6: LOST & FOUND
    // ==========================================
    getFilteredLostFound() {
        const q = (this.searchQueries.lostFound || "").toLowerCase().trim();
        const fType = (this.lostFoundFilters?.type || "ALL").toUpperCase();
        const fCat = (this.lostFoundFilters?.category || "ALL").toUpperCase();
        const fStatus = (this.lostFoundFilters?.status || "ALL").toUpperCase();
        const fMedia = (this.lostFoundFilters?.media || "ALL").toUpperCase();

        return this.lostFound.filter(item => {
            if (fType !== "ALL" && (item.type || "").toUpperCase() !== fType) return false;
            if (fCat !== "ALL" && (item.category || "").toUpperCase() !== fCat) return false;
            if (fStatus !== "ALL") {
                const s = (item.status || "").toUpperCase();
                if (fStatus === "VERIFIED" && !item.isVerified && s !== "VERIFIED") return false;
                if (fStatus === "OPEN" && (item.isVerified || s === "CLAIMED" || s === "TAKEN_DOWN")) return false;
                if (fStatus === "CLAIMED" && s !== "CLAIMED" && s !== "RESOLVED") return false;
                if (fStatus === "TAKEN_DOWN" && s !== "TAKEN_DOWN") return false;
            }
            if (fMedia === "WITH_PHOTOS") {
                const hasPhoto = Boolean(item.photoBase64 || item.imageUrl || (Array.isArray(item.images) && item.images.length > 0));
                if (!hasPhoto) return false;
            }
            if (fMedia === "NO_PHOTOS") {
                const hasPhoto = Boolean(item.photoBase64 || item.imageUrl || (Array.isArray(item.images) && item.images.length > 0));
                if (hasPhoto) return false;
            }

            if (!q) return true;
            const chatText = (item.chatLogs || []).map(c => `${c.senderName} ${c.sender} ${c.message}`).join(" ");
            const haystack = [
                item.id,
                item.title,
                item.type,
                item.category,
                item.description,
                item.location,
                item.date,
                item.reporterName,
                item.reporterEmail,
                item.reporterPhone,
                item.reporterRollNo,
                item.status,
                item.securityDeskNote,
                item.adminTakedownReason,
                chatText
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderLostFoundTab() {
        const filtered = this.getFilteredLostFound();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Lost & Found Vault</h2>
                        <p class="text-xs text-slate-500 mt-1">Campus item listings and security desk deposits.</p>
                    </div>
                    <span id="lostfound-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${filtered.length} Listings Found
                    </span>
                </div>

                <!-- Lost & Found Filter Bar -->
                ${this.renderFilterBar("lostFound")}

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("lostFound", "Search by Title, Category, Location, Reporter Name, Roll No, Phone, Security Token...")}

                <div id="lostfound-grid" class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    ${this.renderLostFoundCards(filtered)}
                </div>
            </div>
        `;
    }

    renderLostFoundCards(itemsList) {
        if (itemsList.length === 0) {
            return `
                <div class="col-span-full py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="search-x" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No Lost & Found listings matching query.</p>
                </div>
            `;
        }

        return itemsList.map(item => {
            const isLost = item.type === "LOST";
            const isDeposited = item.status === "SECURITY_DEPOSITED";
            const isVerified = item.status === "VERIFIED_APPROVED";
            const isClaimed = item.status === "CLAIMED";
            
            // Resolve media
            const mediaSrc = item.photoBase64 
                ? (item.photoBase64.startsWith("data:") ? item.photoBase64 : `data:image/jpeg;base64,${item.photoBase64}`) 
                : (item.imageUrl || (item.images && item.images[0]) || null);

            return `
                <div class="bg-white rounded-2xl p-5 border ${item.isFlagged ? 'border-rose-300 ring-2 ring-rose-100' : isVerified ? 'border-emerald-300' : 'border-slate-200/80'} shadow-sm card-interactive space-y-3.5 text-xs flex flex-col justify-between">
                    <div class="space-y-3">
                        
                        <!-- Top Badges -->
                        <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                            <div class="flex items-center gap-2">
                                <span class="pill-status ${isLost ? 'bg-rose-100 text-rose-800' : 'bg-emerald-100 text-emerald-800'}">${item.type}</span>
                                <span class="font-bold text-slate-500">${item.category}</span>
                            </div>
                            <span class="pill-status ${isClaimed ? 'bg-blue-100 text-blue-800' : isVerified ? 'bg-emerald-100 text-emerald-800' : isDeposited ? 'bg-sky-100 text-sky-800' : item.isFlagged ? 'bg-rose-100 text-rose-800' : 'bg-slate-100 text-slate-700'}">
                                ${isVerified ? 'VERIFIED ✓' : item.status}
                            </span>
                        </div>

                        <!-- Uploaded Image Banner (Clickable to preview) -->
                        ${mediaSrc ? `
                            <div class="relative w-full h-44 rounded-xl overflow-hidden bg-slate-950 border border-slate-200 cursor-pointer group shadow-2xs" onclick="app.openLostFoundPreviewModal('${item.id}')" title="Click to inspect uploaded ad and full-size photos">
                                <img src="${mediaSrc}" 
                                     alt="${item.title}" 
                                     class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" 
                                     onerror="this.parentElement.style.display='none'" />
                                <div class="absolute inset-0 bg-gradient-to-t from-slate-950/80 via-transparent to-transparent flex items-end justify-between p-3">
                                    <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-black/60 backdrop-blur-xs text-white text-[11px] font-semibold border border-white/10">
                                        <i data-lucide="camera" class="w-3.5 h-3.5 text-emerald-400"></i>
                                        <span>${item.photoBase64 ? 'Live Camera Photo' : 'Photo Uploaded'}</span>
                                    </span>
                                    <span class="text-white text-[11px] font-bold flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-purple-600/80 hover:bg-purple-600 backdrop-blur-xs transition">
                                        <i data-lucide="eye" class="w-3.5 h-3.5"></i> Inspect Ad
                                    </span>
                                </div>
                            </div>
                        ` : `
                            <div class="p-3 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between text-slate-400 cursor-pointer hover:bg-purple-50/50 transition" onclick="app.openLostFoundPreviewModal('${item.id}')">
                                <span class="flex items-center gap-1.5 text-[11px]">
                                    <i data-lucide="image-off" class="w-3.5 h-3.5 text-slate-400"></i> No Photo Uploaded
                                </span>
                                <span class="text-purple-700 font-bold text-[11px] flex items-center gap-0.5">
                                    <span>Preview Ad</span>
                                    <i data-lucide="chevron-right" class="w-3 h-3"></i>
                                </span>
                            </div>
                        `}

                        <!-- Text Details -->
                        <div>
                            <h3 class="font-extrabold text-slate-900 text-sm leading-snug">${item.title}</h3>
                            <p class="text-slate-600 mt-1 line-clamp-2 leading-relaxed">${item.description}</p>
                            <div class="text-slate-400 text-[11px] mt-1.5 flex items-center gap-1.5">
                                <i data-lucide="map-pin" class="w-3.5 h-3.5 text-purple-600 shrink-0"></i>
                                <span>Location: ${item.location} | Date: ${item.date}</span>
                            </div>
                            <div class="text-purple-700 text-[11px] font-semibold mt-1 flex items-center gap-1.5">
                                <i data-lucide="user" class="w-3.5 h-3.5 text-purple-500 shrink-0"></i>
                                <span>Reporter: ${item.reporterName} (${item.reporterRollNo})</span>
                            </div>
                        </div>

                        ${item.securityDeskNote ? `
                            <div class="p-2.5 rounded-xl bg-sky-50 border border-sky-200 text-sky-800 text-[11px] font-semibold flex items-center gap-1.5">
                                <i data-lucide="shield" class="w-3.5 h-3.5 text-sky-600 shrink-0"></i>
                                <span>Security Desk: ${item.securityDeskNote}</span>
                            </div>
                        ` : ''}

                        ${item.claimedByRoll ? `
                            <div class="p-2.5 rounded-xl bg-blue-50 border border-blue-200 text-blue-800 text-[11px] font-semibold flex items-center gap-1.5">
                                <i data-lucide="check-circle" class="w-3.5 h-3.5 text-blue-600 shrink-0"></i>
                                <span>Claimed by: ${item.claimedByName ? item.claimedByName + ' (' + item.claimedByRoll + ')' : item.claimedByRoll}</span>
                            </div>
                        ` : ''}
                    </div>

                    <!-- Action Buttons: "Security Deposit" REMOVED, "Preview Ad & Photos" ADDED -->
                    <div class="pt-3 border-t border-slate-100 space-y-2">
                        <div class="grid grid-cols-2 gap-2">
                            <button onclick="app.openLostFoundPreviewModal('${item.id}')" class="py-2.5 px-3 rounded-xl bg-gradient-to-r from-purple-700 to-indigo-800 hover:from-purple-800 hover:to-indigo-900 text-white font-extrabold text-center transition flex items-center justify-center gap-1.5 shadow-sm shadow-purple-900/15 active:scale-95">
                                <i data-lucide="eye" class="w-3.5 h-3.5"></i>
                                <span>Preview Ad</span>
                            </button>
                            <a href="tel:${item.reporterPhone}" class="py-2.5 px-3 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-50 font-bold text-center transition flex items-center justify-center gap-1.5">
                                <i data-lucide="phone" class="w-3.5 h-3.5 text-slate-500"></i>
                                <span>Call Reporter</span>
                            </a>
                        </div>
                        <button onclick="app.openSectionChatModal('lostFound', '${item.id}')" class="w-full py-2 px-3 rounded-xl border border-purple-200 bg-purple-50/70 hover:bg-purple-100 text-purple-800 font-bold text-center transition flex items-center justify-center gap-1.5 shadow-2xs cursor-pointer">
                            <i data-lucide="message-square" class="w-3.5 h-3.5 text-purple-700"></i>
                            <span>Chat (${(item.chatLogs || []).length})</span>
                        </button>
                        <button onclick="app.openTakedownModal('${item.id}')" class="w-full py-2 px-3 rounded-xl ${item.isFlagged ? 'bg-slate-100 text-slate-500 border border-slate-200' : 'bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200'} font-bold text-center transition flex items-center justify-center gap-1.5">
                            <i data-lucide="flag" class="w-3.5 h-3.5"></i>
                            <span>${item.isFlagged ? 'Post Flagged / Taken Down' : 'Takedown Post & Penalize ID'}</span>
                        </button>
                    </div>
                </div>
            `;
        }).join("");
    }

    openLostFoundPreviewModal(itemId) {
        const item = this.lostFound.find(i => i.id === itemId);
        if (!item) return;

        const isLost = item.type === "LOST";
        const isFlagged = !!item.isFlagged;
        const isClaimed = item.status === "CLAIMED";
        const isVerified = item.status === "VERIFIED_APPROVED";

        // Collect all images
        const imagesList = [];
        if (item.photoBase64) {
            imagesList.push(item.photoBase64.startsWith("data:") ? item.photoBase64 : `data:image/jpeg;base64,${item.photoBase64}`);
        }
        if (item.imageUrl) {
            imagesList.push(item.imageUrl);
        }
        if (Array.isArray(item.images)) {
            item.images.forEach(img => {
                if (img && !imagesList.includes(img)) imagesList.push(img);
            });
        }

        const primaryImg = imagesList.length > 0 ? imagesList[0] : null;

        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-3 sm:p-5 z-50 overflow-y-auto">
                <div class="modal-content bg-white w-full max-w-4xl rounded-3xl p-6 sm:p-8 shadow-2xl border border-slate-200 max-h-[92vh] overflow-y-auto custom-scrollbar-dark my-auto">
                    
                    <!-- Header -->
                    <div class="flex items-center justify-between pb-4 border-b border-slate-100">
                        <div class="flex items-center gap-3">
                            <div class="w-10 h-10 rounded-2xl ${isLost ? 'bg-rose-100 text-rose-700' : 'bg-emerald-100 text-emerald-700'} flex items-center justify-center font-bold shadow-xs">
                                <i data-lucide="${isLost ? 'search' : 'gift'}" class="w-5 h-5"></i>
                            </div>
                            <div>
                                <div class="flex items-center gap-2">
                                    <h3 class="text-lg font-extrabold text-slate-900 tracking-tight">Ad Inspection & Image Review</h3>
                                    <span class="text-xs font-mono font-bold px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 border border-slate-200">#${item.id}</span>
                                </div>
                                <p class="text-xs text-slate-500 mt-0.5">Review submitted images and post details to verify authenticity and campus guidelines.</p>
                            </div>
                        </div>
                        <button onclick="app.closeModal()" class="w-8 h-8 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-700 flex items-center justify-center font-bold text-lg transition">&times;</button>
                    </div>

                    <!-- Main Two-Column Layout -->
                    <div class="grid grid-cols-1 md:grid-cols-12 gap-6 mt-6">
                        
                        <!-- Left Column: Photo / Image Viewer (7 cols on md) -->
                        <div class="md:col-span-7 space-y-3">
                            <div class="flex items-center justify-between text-xs font-bold text-slate-700">
                                <span class="flex items-center gap-1.5">
                                    <i data-lucide="image" class="w-4 h-4 text-purple-600"></i>
                                    <span>Uploaded Media (${imagesList.length} Photo${imagesList.length === 1 ? '' : 's'})</span>
                                </span>
                                ${primaryImg ? `
                                    <a id="lostfound-preview-full-link" href="${primaryImg}" target="_blank" class="text-purple-700 hover:text-purple-800 font-bold text-[11px] flex items-center gap-1 hover:underline">
                                        <i data-lucide="external-link" class="w-3.5 h-3.5"></i> Full Size
                                    </a>
                                ` : ''}
                            </div>

                            ${primaryImg ? `
                                <div class="relative w-full bg-slate-950 rounded-2xl overflow-hidden border border-slate-800 shadow-md flex items-center justify-center min-h-[260px] max-h-[360px]">
                                    <img id="lostfound-preview-main-img" src="${primaryImg}" alt="${item.title}" class="w-full h-full max-h-[360px] object-contain transition-all duration-200" />
                                    
                                    <div class="absolute top-3 left-3 flex items-center gap-1.5">
                                        <span class="px-2.5 py-1 rounded-xl bg-black/70 backdrop-blur-md text-white text-[10px] font-bold tracking-wide flex items-center gap-1 border border-white/10">
                                            <i data-lucide="camera" class="w-3 h-3 text-emerald-400"></i>
                                            <span>${item.photoBase64 ? 'LIVE CAMERA CAPTURE' : 'STUDENT UPLOAD'}</span>
                                        </span>
                                    </div>
                                </div>

                                ${imagesList.length > 1 ? `
                                    <div class="flex items-center gap-2 overflow-x-auto py-1">
                                        ${imagesList.map((img, idx) => `
                                            <button type="button" onclick="app.switchPreviewImage('${img}')" class="w-16 h-16 rounded-xl overflow-hidden border-2 border-slate-200 hover:border-purple-600 shrink-0 transition focus:outline-none focus:border-purple-600">
                                                <img src="${img}" alt="Thumbnail ${idx + 1}" class="w-full h-full object-cover" />
                                            </button>
                                        `).join("")}
                                    </div>
                                ` : ''}
                            ` : `
                                <div class="w-full h-64 rounded-2xl bg-amber-50/70 border-2 border-dashed border-amber-300 flex flex-col items-center justify-center p-6 text-center space-y-2">
                                    <div class="w-12 h-12 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center">
                                        <i data-lucide="image-off" class="w-6 h-6"></i>
                                    </div>
                                    <h4 class="text-sm font-bold text-amber-900">No Image Uploaded</h4>
                                    <p class="text-xs text-amber-700 max-w-xs">
                                        This listing was posted as text-only without live camera photo verification. Pay special attention to item descriptions before approving.
                                    </p>
                                </div>
                            `}

                            <!-- Quick Image Guidelines Tip -->
                            <div class="p-3 rounded-xl bg-slate-50 border border-slate-200/80 text-[11px] text-slate-600 space-y-1">
                                <div class="font-bold text-slate-800 flex items-center gap-1.5">
                                    <i data-lucide="shield-check" class="w-3.5 h-3.5 text-purple-600"></i>
                                    <span>Verification Checklist:</span>
                                </div>
                                <ul class="list-disc list-inside space-y-0.5 text-slate-500 pl-1">
                                    <li>Check that the image clearly shows the actual item, not stock or meme images.</li>
                                    <li>Confirm item matches the reported location and category.</li>
                                    <li>Ensure no private or sensitive information (PINs, passwords) is exposed in photo.</li>
                                </ul>
                            </div>
                        </div>

                        <!-- Right Column: Item Information & Moderation Decision (5 cols on md) -->
                        <div class="md:col-span-5 space-y-4 flex flex-col justify-between">
                            <div class="space-y-3.5 text-xs">
                                
                                <!-- Status Badges -->
                                <div class="flex flex-wrap items-center gap-2">
                                    <span class="px-2.5 py-1 rounded-full font-bold text-xs ${isLost ? 'bg-rose-100 text-rose-800' : 'bg-emerald-100 text-emerald-800'}">
                                        ${item.type}
                                    </span>
                                    <span class="px-2.5 py-1 rounded-full font-semibold text-xs bg-slate-100 text-slate-700 border border-slate-200">
                                        ${item.category}
                                    </span>
                                    <span class="px-2.5 py-1 rounded-full font-bold text-xs ${isClaimed ? 'bg-blue-100 text-blue-800' : isVerified ? 'bg-emerald-100 text-emerald-800' : isFlagged ? 'bg-rose-100 text-rose-800' : 'bg-amber-100 text-amber-800'}">
                                        ${isVerified ? 'VERIFIED ✓' : item.status}
                                    </span>
                                </div>

                                <!-- Item Description Box -->
                                <div>
                                    <h4 class="text-base font-extrabold text-slate-900">${item.title}</h4>
                                    <div class="mt-2 p-3 rounded-xl bg-slate-50 border border-slate-100 text-slate-700 leading-relaxed">
                                        ${item.description || '<em class="text-slate-400">No additional description provided.</em>'}
                                    </div>
                                </div>

                                <!-- Metadata: Location & Date -->
                                <div class="space-y-1.5 pt-1 text-slate-600">
                                    <div class="flex items-center gap-2">
                                        <i data-lucide="map-pin" class="w-3.5 h-3.5 text-purple-600 shrink-0"></i>
                                        <span><strong>Location:</strong> ${item.location}</span>
                                    </div>
                                    <div class="flex items-center gap-2">
                                        <i data-lucide="clock" class="w-3.5 h-3.5 text-purple-600 shrink-0"></i>
                                        <span><strong>Reported Date:</strong> ${item.date}</span>
                                    </div>
                                    ${item.securityDeskNote ? `
                                        <div class="flex items-start gap-2 text-sky-800 bg-sky-50 p-2 rounded-lg border border-sky-200">
                                            <i data-lucide="shield" class="w-3.5 h-3.5 text-sky-600 mt-0.5 shrink-0"></i>
                                            <span><strong>Security Holding:</strong> ${item.securityDeskNote}</span>
                                        </div>
                                    ` : ''}
                                    ${item.claimedByRoll ? `
                                        <div class="flex items-start gap-2 text-blue-800 bg-blue-50 p-2 rounded-lg border border-blue-200">
                                            <i data-lucide="user-check" class="w-3.5 h-3.5 text-blue-600 mt-0.5 shrink-0"></i>
                                            <span><strong>Claimed by:</strong> ${item.claimedByName ? item.claimedByName + ' (' + item.claimedByRoll + ')' : item.claimedByRoll}</span>
                                        </div>
                                    ` : ''}
                                    ${item.isFlagged ? `
                                        <div class="flex items-start gap-2 text-rose-800 bg-rose-50 p-2 rounded-lg border border-rose-200">
                                            <i data-lucide="alert-triangle" class="w-3.5 h-3.5 text-rose-600 mt-0.5 shrink-0"></i>
                                            <span><strong>Taken Down:</strong> ${item.adminTakedownReason || 'Inappropriate campus post'}</span>
                                        </div>
                                    ` : ''}
                                </div>

                                <!-- Student Reporter Credentials -->
                                <div class="p-3.5 rounded-2xl bg-purple-50/70 border border-purple-200/60 space-y-2">
                                    <div class="flex items-center justify-between">
                                        <span class="font-extrabold text-purple-900 flex items-center gap-1.5">
                                            <i data-lucide="user" class="w-3.5 h-3.5 text-purple-700"></i> Reporter Profile
                                        </span>
                                        <span class="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-purple-200/70 text-purple-900">${item.reporterRollNo}</span>
                                    </div>
                                    <div class="text-slate-800 font-semibold">${item.reporterName}</div>
                                    <div class="text-slate-500 text-[11px] font-mono break-all">${item.reporterEmail}</div>
                                    <div class="pt-1 flex items-center gap-2">
                                        <a href="tel:${item.reporterPhone}" class="flex-1 py-1.5 px-3 rounded-xl bg-white border border-purple-200 text-purple-800 font-bold text-center hover:bg-purple-100 transition flex items-center justify-center gap-1.5 shadow-2xs">
                                            <i data-lucide="phone" class="w-3 h-3"></i>
                                            <span>${item.reporterPhone || 'Call Student'}</span>
                                        </a>
                                        <a href="mailto:${item.reporterEmail}" class="py-1.5 px-3 rounded-xl bg-white border border-purple-200 text-purple-800 font-bold hover:bg-purple-100 transition flex items-center justify-center shadow-2xs" title="Send Email">
                                            <i data-lucide="mail" class="w-3 h-3"></i>
                                        </a>
                                    </div>
                                </div>
                            </div>

                            <!-- Moderation Action Panel (Judge and Act Forward) -->
                            <div class="pt-4 border-t border-slate-100 space-y-2.5">
                                <div class="text-xs font-extrabold text-slate-800 flex items-center justify-between">
                                    <span>Moderation Actions:</span>
                                    <span class="text-[10px] font-semibold text-slate-400">Real-time Cloud Broadcast</span>
                                </div>

                                <div class="grid grid-cols-2 gap-2">
                                    <button 
                                        onclick="app.verifyLostFoundPost('${item.id}')" 
                                        class="py-2.5 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs transition flex items-center justify-center gap-1.5 shadow-sm shadow-emerald-900/10 active:scale-95">
                                        <i data-lucide="check-circle" class="w-3.5 h-3.5"></i>
                                        <span>${isVerified ? 'Verified ✓' : 'Approve Post'}</span>
                                    </button>

                                    <button 
                                        onclick="app.openClaimLostFoundModal('${item.id}')" 
                                        class="py-2.5 px-3 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-xs transition flex items-center justify-center gap-1.5 shadow-sm shadow-sky-900/10 active:scale-95">
                                        <i data-lucide="user-check" class="w-3.5 h-3.5"></i>
                                        <span>${isClaimed ? 'Claimed ✓' : 'Mark Claimed'}</span>
                                    </button>
                                </div>

                                <button 
                                    type="button"
                                    onclick="app.openSectionChatModal('lostFound', '${item.id}')" 
                                    class="w-full py-2.5 px-3 rounded-xl border border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-800 font-bold text-xs transition flex items-center justify-center gap-1.5 shadow-2xs">
                                    <i data-lucide="message-square" class="w-3.5 h-3.5 text-purple-700"></i>
                                    <span>Manage & Save Item Chat (${(item.chatLogs || []).length})</span>
                                </button>

                                <button 
                                    onclick="app.openTakedownModal('${item.id}')" 
                                    class="w-full py-2.5 px-3 rounded-xl ${isFlagged ? 'bg-rose-100 text-rose-800 border border-rose-300' : 'bg-rose-600 hover:bg-rose-700 text-white shadow-sm shadow-rose-900/10'} font-bold text-xs transition flex items-center justify-center gap-1.5 active:scale-95">
                                    <i data-lucide="flag" class="w-3.5 h-3.5"></i>
                                    <span>${isFlagged ? 'Post Taken Down (Click to Re-penalize)' : 'Takedown Improper / Fake Post'}</span>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        modal.classList.remove("hidden");
        if (window.lucide) window.lucide.createIcons();
    }

    switchPreviewImage(imgUrl) {
        const mainImg = document.getElementById("lostfound-preview-main-img");
        const openLink = document.getElementById("lostfound-preview-full-link");
        if (mainImg) mainImg.src = imgUrl;
        if (openLink) openLink.href = imgUrl;
    }

    verifyLostFoundPost(itemId) {
        const item = this.lostFound.find(i => i.id === itemId);
        if (!item) return;

        item.status = "VERIFIED_APPROVED";
        item.isFlagged = false;
        item.adminVerifiedAt = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        this.save("campus_lost_found_items", this.lostFound);
        if (window.cloudSync) window.cloudSync.syncLostFound(item);

        this.recordAudit("LOST_FOUND_VERIFIED", item.id, `Admin verified listing ${item.id} ("${item.title}") submitted by ${item.reporterName}. Images and content confirmed proper.`, "INFO");
        this.closeModal();
        this.showToast(`Listing "${item.title}" verified and approved as proper!`, "success");
        this.updateSectionGrid("lostFound");
    }

    openClaimLostFoundModal(itemId) {
        const item = this.lostFound.find(i => i.id === itemId);
        if (!item) return;

        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <div class="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                                <i data-lucide="check-circle" class="w-4 h-4"></i>
                            </div>
                            <h3 class="text-base font-extrabold text-slate-900">Mark Item as Claimed</h3>
                        </div>
                        <button onclick="app.closeModal()" class="text-slate-400 hover:text-slate-600 font-bold text-xl">&times;</button>
                    </div>
                    <p class="text-xs text-slate-500 mt-2">Confirm that "${item.title}" has been successfully verified and handed over to the rightful student owner.</p>

                    <form onsubmit="app.submitClaimLostFound(event, '${item.id}')" class="space-y-4 mt-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Claimant Student Roll Number / ID *</label>
                            <input type="text" id="claimant-roll" placeholder="e.g. 23CS042" required class="w-full px-3 py-2 border border-slate-200 rounded-xl focus:border-purple-600 focus:outline-none" />
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Claimant Student Name *</label>
                            <input type="text" id="claimant-name" placeholder="Student Full Name" required class="w-full px-3 py-2 border border-slate-200 rounded-xl focus:border-purple-600 focus:outline-none" />
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Hand-off Notes / Proof Checked</label>
                            <input type="text" id="claimant-notes" value="Identity and item ownership verified by Admin" class="w-full px-3 py-2 border border-slate-200 rounded-xl focus:border-purple-600 focus:outline-none" />
                        </div>

                        <div class="flex justify-end gap-2 pt-2 border-t border-slate-100">
                            <button type="button" onclick="app.openLostFoundPreviewModal('${item.id}')" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50">Back</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold flex items-center gap-1.5 shadow-sm">
                                <i data-lucide="check" class="w-3.5 h-3.5"></i>
                                <span>Confirm Hand-off & Claim</span>
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) window.lucide.createIcons();
    }

    submitClaimLostFound(e, itemId) {
        e.preventDefault();
        const item = this.lostFound.find(i => i.id === itemId);
        if (!item) return;

        const roll = document.getElementById("claimant-roll").value.trim();
        const name = document.getElementById("claimant-name").value.trim();
        const notes = document.getElementById("claimant-notes").value.trim();

        item.status = "CLAIMED";
        item.claimedByRoll = roll;
        item.claimedByName = name;
        item.claimHandOffNotes = notes;

        this.save("campus_lost_found_items", this.lostFound);
        if (window.cloudSync) window.cloudSync.syncLostFound(item);

        this.recordAudit("LOST_FOUND_CLAIMED", item.id, `Item ${item.id} (${item.title}) claimed by student ${name} (${roll}). Hand-off note: ${notes}`, "INFO");
        this.closeModal();
        this.showToast(`Item successfully marked as CLAIMED by ${name} (${roll})!`, "success");
        this.updateSectionGrid("lostFound");
    }

    openSecurityDepositModal(itemId) {
        const item = this.lostFound.find(i => i.id === itemId);
        if (!item) return;

        const defaultToken = `SEC-${Date.now() % 1000}`;
        const modal = document.getElementById("modal-generic");

        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <h3 class="text-base font-extrabold text-slate-900">Deposit at Security Desk</h3>
                    <p class="text-xs text-slate-500 mt-1">Mark item "${item.title}" as turned into campus security proctor desk so the student owner can safely collect it.</p>

                    <form onsubmit="app.submitSecurityDeposit(event, '${item.id}')" class="space-y-4 mt-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Security Token / Receipt Number:</label>
                            <input type="text" id="sec-token" value="${defaultToken}" class="w-full px-3 py-2 border border-slate-200 rounded-xl" />
                        </div>

                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Holding Location / Locker Reference:</label>
                            <input type="text" id="sec-location" value="Main Gate Security Desk (Locker 4)" class="w-full px-3 py-2 border border-slate-200 rounded-xl" />
                        </div>

                        <div class="flex justify-end gap-2 pt-2">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-sky-700 hover:bg-sky-800 text-white font-bold">Confirm Security Deposit</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) window.lucide.createIcons();
    }

    submitSecurityDeposit(e, itemId) {
        e.preventDefault();
        const item = this.lostFound.find(i => i.id === itemId);
        if (!item) return;

        const token = document.getElementById("sec-token").value.trim();
        const loc = document.getElementById("sec-location").value.trim();
        const note = `Deposited at ${loc} (Token: #${token})`;

        item.status = "SECURITY_DEPOSITED";
        item.securityDeskNote = note;
        this.save("campus_lost_found_items", this.lostFound);
        if (window.cloudSync) window.cloudSync.syncLostFound(item);
        this.recordAudit("LOST_FOUND_SECURITY_DEPOSIT", item.id, `Deposited item ${item.id} (${item.title}) at security desk. Token: #${token}`, "INFO");
        this.closeModal();
        this.showToast(`Item marked as deposited at Security Desk!`);
        this.updateSectionGrid("lostFound");
    }

    openTakedownModal(itemId) {
        const item = this.lostFound.find(i => i.id === itemId);
        if (!item) return;

        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <h3 class="text-base font-extrabold text-slate-900">Takedown Post & Penalize ID</h3>
                    <p class="text-xs text-slate-500 mt-1">Remove "${item.title}" from student feed and log moderation warning against student ${item.reporterName} (${item.reporterRollNo}).</p>

                    <form onsubmit="app.submitTakedown(event, '${item.id}')" class="space-y-4 mt-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Takedown Reason:</label>
                            <input type="text" id="takedown-reason" value="Spam or inappropriate campus posting" class="w-full px-3 py-2 border border-slate-200 rounded-xl" />
                        </div>

                        <div class="flex justify-end gap-2 pt-2">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold">Confirm Takedown</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) window.lucide.createIcons();
    }

    submitTakedown(e, itemId) {
        e.preventDefault();
        const item = this.lostFound.find(i => i.id === itemId);
        if (!item) return;

        const reason = document.getElementById("takedown-reason").value.trim();
        item.status = "FLAGGED_TAKEDOWN";
        item.isFlagged = true;
        item.adminTakedownReason = reason;

        this.save("campus_lost_found_items", this.lostFound);
        if (window.cloudSync) window.cloudSync.syncLostFound(item);
        this.recordAudit("LOST_FOUND_TAKEDOWN", item.id, `Admin took down post ${item.id} by student ${item.reporterEmail}. Reason: ${reason}`, "WARNING");
        this.closeModal();
        this.showToast(`Listing taken down and warning logged!`);
        this.updateSectionGrid("lostFound");
    }

    // ==========================================
    // TAB 7: FRAUD & ANOMALY SCANNER
    // ==========================================
    getFilteredFraud() {
        const q = (this.searchQueries.fraud || "").toLowerCase().trim();
        const fRisk = (this.fraudFilters?.risk || "ALL").toUpperCase();
        const fType = (this.fraudFilters?.type || "ALL").toUpperCase();
        const fStatus = (this.fraudFilters?.status || "ALL").toUpperCase();

        return this.fraudAnomalies.filter(anm => {
            if (fRisk !== "ALL" && (anm.riskLevel || "").toUpperCase() !== fRisk) return false;
            if (fType !== "ALL") {
                const mal = (anm.malpracticeType || anm.anomalyType || "").toUpperCase();
                if (!mal.includes(fType)) return false;
            }
            if (fStatus !== "ALL" && (anm.status || "").toUpperCase() !== fStatus) return false;

            if (!q) return true;
            const haystack = [
                anm.id,
                anm.userId,
                anm.userName,
                anm.userRole,
                anm.malpracticeType,
                anm.riskLevel,
                anm.anomalyType,
                anm.description,
                anm.telemetryDetail,
                anm.status,
                anm.blockTarget,
                `score ${anm.riskScore}`
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderFraudScannerTab() {
        const filtered = this.getFilteredFraud();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Fraud Scanner</h2>
                    </div>
                    <span id="fraud-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${filtered.length} Anomalies Tracked
                    </span>
                </div>

                <!-- Fraud Scanner Filter Bar -->
                ${this.renderFilterBar("fraud")}

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("fraud", "Search by Anomaly Type, User Name, Email/ID, Risk Level, Telemetry...")}

                <div id="fraud-grid" class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    ${this.renderFraudCards(filtered)}
                </div>
            </div>
        `;
    }

    renderFraudCards(anomaliesList) {
        if (anomaliesList.length === 0) {
            return `
                <div class="col-span-full py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="shield-check" class="w-10 h-10 mx-auto mb-2 opacity-50 text-emerald-500"></i>
                    <p class="text-xs">No fraud anomalies matching criteria.</p>
                </div>
            `;
        }

        return anomaliesList.map(anm => {
            const isCritical = anm.riskLevel === "CRITICAL";
            const isHigh = anm.riskLevel === "HIGH";
            const isBlocked = anm.status === "TEMP_BLOCKED";
            const malpracticeType = anm.malpracticeType || "RIDER_MALPRACTICE";

            return `
                <div class="bg-white rounded-2xl p-5 border ${isCritical ? 'border-rose-300' : 'border-slate-200/80'} shadow-sm card-interactive space-y-3 text-xs flex flex-col justify-between">
                    <div class="space-y-2">
                        <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                            <span class="pill-status ${malpracticeType === 'RIDER_MALPRACTICE' ? 'bg-amber-100 text-amber-800' : 'bg-indigo-100 text-indigo-800'}">
                                ${malpracticeType.replace(/_/g, ' ')}
                            </span>
                            <span class="pill-status ${isCritical ? 'bg-rose-100 text-rose-800' : isHigh ? 'bg-amber-100 text-amber-800' : 'bg-indigo-100 text-indigo-800'} font-black">
                                RISK SCORE: ${anm.riskScore}/100
                            </span>
                        </div>

                        <div>
                            <h3 class="font-black text-slate-900 text-sm">${anm.anomalyType}</h3>
                            <div class="text-slate-500 font-semibold mt-0.5">Target: ${anm.userName} (${anm.userId})</div>
                            <div class="text-slate-700 mt-1">${anm.description}</div>
                        </div>

                        <div class="p-3 rounded-xl bg-slate-50 border border-slate-100 text-sky-800 font-mono text-[11px]">
                            Telemetry: ${anm.telemetryDetail}
                        </div>

                        ${isBlocked && anm.blockUntil > 0 ? `
                            <div class="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 flex items-center justify-between font-bold">
                                <span>12-Hour Temp Block Active (${anm.blockTarget || 'Console'}):</span>
                                <span class="font-mono text-rose-700 font-black" data-countdown="${anm.blockUntil}">Calculating...</span>
                            </div>
                        ` : ''}
                    </div>

                    <!-- Decoupled Action Buttons matching Android Point 5 & 7 -->
                    <div class="pt-3 border-t border-slate-100 flex flex-wrap gap-2 justify-end">
                        ${!isBlocked ? `
                            <button onclick="app.blockAnomalyConsole('${anm.id}', 'RIDER_ONLY')" class="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-black text-xs transition shadow-xs flex items-center gap-1.5" title="Block rider duty console for 12 hours">
                                <i data-lucide="bike" class="w-3.5 h-3.5"></i>
                                <span>12h Block Rider Only</span>
                            </button>
                            <button onclick="app.blockAnomalyConsole('${anm.id}', 'STUDENT_ONLY')" class="px-3 py-1.5 rounded-xl bg-rose-700 hover:bg-rose-800 text-white font-black text-xs transition shadow-xs flex items-center gap-1.5" title="Block student shopping console for 12 hours">
                                <i data-lucide="user-x" class="w-3.5 h-3.5"></i>
                                <span>12h Block Student Only</span>
                            </button>
                        ` : `
                            <button onclick="app.unblockAnomalyConsole('${anm.id}')" class="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs transition shadow-xs flex items-center gap-1.5">
                                <i data-lucide="unlock" class="w-3.5 h-3.5"></i>
                                <span>Immediate Unblock</span>
                            </button>
                        `}
                        <button onclick="app.openWarningModal('${anm.userId}')" class="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs transition shadow-xs flex items-center gap-1.5">
                            <i data-lucide="alert-triangle" class="w-3.5 h-3.5"></i>
                            <span>Warn User</span>
                        </button>
                    </div>
                </div>
            `;
        }).join("");
    }

    blockAnomalyConsole(id, targetConsole) {
        const anm = this.fraudAnomalies.find(a => a.id === id);
        if (!anm) return;
        const blockUntil = Date.now() + 12 * 3600 * 1000;
        anm.status = "TEMP_BLOCKED";
        anm.blockTarget = targetConsole;
        anm.blockUntil = blockUntil;
        this.save("admin_fraud_anomalies", this.fraudAnomalies);

        // Update student record decoupled
        const st = this.students.find(s => (s.email || "").toLowerCase() === (anm.userId || "").toLowerCase());
        const rd = this.riders.find(r => (r.email || "").toLowerCase() === (anm.userId || "").toLowerCase());

        if (targetConsole === "RIDER_ONLY") {
            if (st) {
                st.riderStatus = "TEMP_BLOCKED";
                st.riderBlockUntil = blockUntil;
                st.riderBlockReason = `Fraud Scanner: ${anm.anomalyType}`;
                this.save("campus_students_list", this.students);
            }
            if (rd) {
                rd.riderStatus = "TEMP_BLOCKED";
                rd.riderBlockUntil = blockUntil;
                rd.riderBlockReason = `Fraud Scanner: ${anm.anomalyType}`;
                rd.isOnline = false;
                this.save("campus_riders_list", this.riders);
            }
            if (window.cloudSync) {
                window.cloudSync.syncRiderBlock({
                    userEmail: anm.userId,
                    blockDurationHours: 12,
                    blockReason: `Fraud Scanner: ${anm.anomalyType}`,
                    riderStatus: "TEMP_BLOCKED"
                });
            }
        } else {
            if (st) {
                st.studentStatus = "TEMP_BLOCKED";
                st.studentBlockUntil = blockUntil;
                st.studentBlockReason = `Fraud Scanner: ${anm.anomalyType}`;
                this.save("campus_students_list", this.students);
            }
            if (window.cloudSync) {
                window.cloudSync.syncStudentBlock({
                    userEmail: anm.userId,
                    blockDurationHours: 12,
                    blockReason: `Fraud Scanner: ${anm.anomalyType}`,
                    studentStatus: "TEMP_BLOCKED"
                });
            }
        }

        this.recordAudit("FRAUD_BLOCK_TRIGGERED", anm.userId, `12-Hour Lockout applied to ${targetConsole} for ${anm.userName} (${anm.anomalyType}).`, "CRITICAL");
        this.showToast(`Applied 12-Hour Lockout (${targetConsole}) on ${anm.userName}!`);
        this.updateSectionGrid("fraud");
        this.updateSectionGrid("students");
        this.updateSectionGrid("riders");
    }

    unblockAnomalyConsole(id) {
        const anm = this.fraudAnomalies.find(a => a.id === id);
        if (!anm) return;
        anm.status = "FLAGGED_PENDING";
        anm.blockUntil = 0;
        this.save("admin_fraud_anomalies", this.fraudAnomalies);

        const st = this.students.find(s => (s.email || "").toLowerCase() === (anm.userId || "").toLowerCase());
        if (st) {
            st.studentStatus = "ACTIVE";
            st.riderStatus = "ACTIVE";
            st.studentBlockUntil = 0;
            st.riderBlockUntil = 0;
            this.save("campus_students_list", this.students);
        }

        const rd = this.riders.find(r => (r.email || "").toLowerCase() === (anm.userId || "").toLowerCase());
        if (rd) {
            rd.riderStatus = "ACTIVE";
            rd.riderBlockUntil = 0;
            this.save("campus_riders_list", this.riders);
        }

        if (window.cloudSync) {
            window.cloudSync.syncStudentUnblock({ userEmail: anm.userId });
            window.cloudSync.syncRiderUnblock({ userEmail: anm.userId });
        }

        this.recordAudit("FRAUD_UNBLOCK", anm.userId, `Admin lifted fraud lockout on ${anm.userName}.`, "INFO");
        this.showToast(`Lifted lockout for ${anm.userName}!`);
        this.updateSectionGrid("fraud");
        this.updateSectionGrid("students");
        this.updateSectionGrid("riders");
    }

    // ==========================================
    // TAB 8: AUDIT TRAIL
    // ==========================================
    getFilteredAudit() {
        const q = (this.searchQueries.audit || "").toLowerCase().trim();
        const sev = (this.auditFilters.severity || "ALL").toUpperCase();
        const role = (this.auditFilters.role || "ALL").toUpperCase();
        const from = this.auditFilters.dateFrom;
        const to = this.auditFilters.dateTo;

        return this.auditLogs.filter(log => {
            if (sev !== "ALL" && (log.severity || "").toUpperCase() !== sev) return false;
            if (role !== "ALL" && (log.adminRole || "").toUpperCase() !== role) return false;

            if (from || to) {
                const logDate = this.normalizeRecordDate(log);
                if (logDate) {
                    if (from && logDate < from) return false;
                    if (to && logDate > to) return false;
                }
            }

            if (!q) return true;
            const haystack = [
                log.id,
                log.actionType,
                log.targetEntityId,
                log.details,
                log.adminRole,
                log.ipAddress,
                log.severity,
                log.timestamp
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    setAuditSubTab(idx) {
        this.auditSubTab = idx;
        this.renderCurrentTab();
    }

    renderAuditTrailTab() {
        const auditCount = this.auditLogs.length;
        const sessionsCount = this.sessionLogs.length;
        const sub = this.auditSubTab;

        return `
            <div class="space-y-6">
                <!-- Sub-tab Navigation Header -->
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-2 border-b border-slate-200">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Audit & Access Control Center</h2>
                    </div>

                    <!-- Sub-tab Toggle Pills -->
                    <div class="flex items-center p-1 bg-slate-100 rounded-2xl border border-slate-200 self-start sm:self-auto">
                        <button onclick="app.setAuditSubTab(0)" class="px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${sub === 0 ? 'bg-purple-700 text-white shadow-md' : 'text-slate-600 hover:text-slate-900'}">
                            <i data-lucide="shield-check" data-stroke-width="${sub === 0 ? '2.8' : '1.8'}" stroke-width="${sub === 0 ? '2.8' : '1.8'}" class="w-4 h-4 ${sub === 0 ? 'stroke-[2.8]' : ''}"></i>
                            <span>General Audit Trail</span>
                            <span class="text-[10px] px-2 py-0.5 rounded-full ${sub === 0 ? 'bg-purple-800 text-purple-200' : 'bg-slate-200 text-slate-700'}">${auditCount}</span>
                        </button>
                        <button onclick="app.setAuditSubTab(1)" class="px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${sub === 1 ? 'bg-purple-700 text-white shadow-md' : 'text-slate-600 hover:text-slate-900'}">
                            <i data-lucide="clock" data-stroke-width="${sub === 1 ? '2.8' : '1.8'}" stroke-width="${sub === 1 ? '2.8' : '1.8'}" class="w-4 h-4 ${sub === 1 ? 'stroke-[2.8]' : ''}"></i>
                            <span>Login & Logout Data (IST)</span>
                            <span class="text-[10px] px-2 py-0.5 rounded-full ${sub === 1 ? 'bg-purple-800 text-purple-200' : 'bg-slate-200 text-slate-700'}">${sessionsCount}</span>
                        </button>
                        <button onclick="app.setAuditSubTab(2)" class="px-4 py-2 rounded-xl text-xs font-bold transition flex items-center gap-2 ${sub === 2 ? 'bg-purple-700 text-white shadow-md' : 'text-slate-600 hover:text-slate-900'}">
                            <i data-lucide="zap" data-stroke-width="${sub === 2 ? '2.8' : '1.8'}" stroke-width="${sub === 2 ? '2.8' : '1.8'}" class="w-4 h-4 ${sub === 2 ? 'stroke-[2.8]' : ''}"></i>
                            <span>Customer Activities Stream</span>
                            <span class="text-[10px] px-2 py-0.5 rounded-full ${sub === 2 ? 'bg-purple-800 text-purple-200' : 'bg-slate-200 text-slate-700'}">${this.customerActivities.length}</span>
                        </button>
                    </div>
                </div>

                ${sub === 0 ? this.renderGeneralAuditSection() : (sub === 1 ? this.renderSessionAuditSection() : this.renderCustomerActivitiesSection())}
            </div>
        `;
    }

    renderGeneralAuditSection() {
        const filtered = this.getFilteredAudit();
        const totalDeletedAccounts = parseInt((typeof this.load === 'function' ? this.load("campus_total_deleted_accounts", "0") : (typeof localStorage !== 'undefined' ? localStorage.getItem("campus_total_deleted_accounts") : "0")) || "0", 10) || 0;
        const uniqueEmails = new Set([
            ...this.students.map(s => (s.email || "").toLowerCase()),
            ...this.riders.map(r => (r.email || "").toLowerCase())
        ].filter(Boolean));
        const activeAccountsCount = uniqueEmails.size;
        const totalCreatedAccounts = activeAccountsCount + totalDeletedAccounts;

        return `
            <div class="space-y-6">
                <!-- Account Lifecycle Metrics matching Request 7 -->
                <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                    <div class="p-5 rounded-2xl bg-emerald-50/80 border border-emerald-200/90 shadow-sm flex flex-col justify-between">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-bold text-emerald-800 uppercase tracking-wide">Total Users Created Account</span>
                            <div class="w-8 h-8 rounded-xl bg-emerald-600 text-white flex items-center justify-center shadow-xs">
                                <i data-lucide="user-plus" class="w-4 h-4"></i>
                            </div>
                        </div>
                        <div class="mt-2">
                            <div class="text-2xl font-black text-emerald-950">${totalCreatedAccounts} Accounts</div>
                            <div class="text-[11px] text-emerald-700 font-medium mt-0.5">Total accounts registered in CampusApp</div>
                        </div>
                    </div>

                    <div class="p-5 rounded-2xl bg-rose-50/80 border border-rose-200/90 shadow-sm flex flex-col justify-between">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-bold text-rose-800 uppercase tracking-wide">Total Users Deleted Their Account</span>
                            <div class="w-8 h-8 rounded-xl bg-rose-600 text-white flex items-center justify-center shadow-xs">
                                <i data-lucide="user-x" class="w-4 h-4"></i>
                            </div>
                        </div>
                        <div class="mt-2">
                            <div class="text-2xl font-black text-rose-950">${totalDeletedAccounts} Closed</div>
                            <div class="text-[11px] text-rose-700 font-medium mt-0.5">Account erasure under DPDPA Data Rights</div>
                        </div>
                    </div>

                    <div class="p-5 rounded-2xl bg-purple-50/80 border border-purple-200/90 shadow-sm flex flex-col justify-between">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-bold text-purple-800 uppercase tracking-wide">Active Student Accounts</span>
                            <div class="w-8 h-8 rounded-xl bg-purple-600 text-white flex items-center justify-center shadow-xs">
                                <i data-lucide="users" class="w-4 h-4"></i>
                            </div>
                        </div>
                        <div class="mt-2">
                            <div class="text-2xl font-black text-purple-950">${this.students.length} Active</div>
                            <div class="text-[11px] text-purple-700 font-medium mt-0.5">Currently registered active students</div>
                        </div>
                    </div>

                    <div class="p-5 rounded-2xl bg-sky-50/80 border border-sky-200/90 shadow-sm flex flex-col justify-between">
                        <div class="flex items-center justify-between">
                            <span class="text-xs font-bold text-sky-800 uppercase tracking-wide">Active Rider Fleet</span>
                            <div class="w-8 h-8 rounded-xl bg-sky-600 text-white flex items-center justify-center shadow-xs">
                                <i data-lucide="bike" class="w-4 h-4"></i>
                            </div>
                        </div>
                        <div class="mt-2">
                            <div class="text-2xl font-black text-sky-950">${this.riders.length} Runners</div>
                            <div class="text-[11px] text-sky-700 font-medium mt-0.5">Campus delivery runners on active roster</div>
                        </div>
                    </div>
                </div>

                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h3 class="text-lg font-extrabold text-slate-900">Immutable Audit Trail</h3>
                    </div>

                    <div class="flex items-center gap-2 self-start sm:self-auto">
                        <button onclick="app.downloadAuditCsv()" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs flex items-center gap-2 transition shadow-sm">
                            <i data-lucide="cloud-download" class="w-4 h-4"></i>
                            <span>Download Filtered Excel/CSV</span>
                        </button>
                        <span id="audit-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800">
                            ${filtered.length} Logs Listed
                        </span>
                    </div>
                </div>

                <!-- Date & Condition Filter Bar -->
                ${this.renderFilterBar("audit")}

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("audit", "Search actions, targets, admin roles, details, timestamps...")}

                <!-- Audit Cards List with Balanced Spacing -->
                <div id="audit-grid" class="space-y-3">
                    ${this.renderAuditCards(filtered)}
                </div>
            </div>
        `;
    }

    renderSessionAuditSection() {
        const filtered = this.getFilteredSessionLogs();
        const totalSessions = this.sessionLogs.length;
        const totalLogins = this.sessionLogs.filter(s => s.eventType === "LOGIN").length;
        const totalLogouts = this.sessionLogs.filter(s => s.eventType === "LOGOUT").length;
        const totalAutoExpired = this.sessionLogs.filter(s => s.eventType === "AUTO_EXPIRED").length;

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h3 class="text-lg font-extrabold text-slate-900">Role Login & Logout Telemetry (IST)</h3>
                        <p class="text-xs text-slate-500 mt-0.5">Chronological access log of role logins, logouts, and automatic inactivity expiries in Indian Standard Time.</p>
                    </div>

                    <div class="flex items-center gap-2 self-start sm:self-auto">
                        <button onclick="app.downloadSessionsCsv()" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs flex items-center gap-2 transition shadow-sm">
                            <i data-lucide="cloud-download" class="w-4 h-4"></i>
                            <span>Download Filtered Excel/CSV</span>
                        </button>
                        <span id="sessions-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800">
                            ${filtered.length} Sessions Listed
                        </span>
                    </div>
                </div>

                <!-- KPI Metric Cards for Sessions -->
                <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
                    <div class="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-sm flex items-center justify-between">
                        <div>
                            <div class="text-[10px] font-bold uppercase text-slate-400">Total Access Events</div>
                            <div class="text-xl font-black text-slate-900 mt-0.5">${totalSessions}</div>
                        </div>
                        <div class="p-2.5 rounded-xl bg-purple-50 text-purple-600">
                            <i data-lucide="history" class="w-5 h-5"></i>
                        </div>
                    </div>
                    <div class="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-sm flex items-center justify-between">
                        <div>
                            <div class="text-[10px] font-bold uppercase text-emerald-600">Total Logins</div>
                            <div class="text-xl font-black text-emerald-600 mt-0.5">${totalLogins}</div>
                        </div>
                        <div class="p-2.5 rounded-xl bg-emerald-50 text-emerald-600">
                            <i data-lucide="log-in" class="w-5 h-5"></i>
                        </div>
                    </div>
                    <div class="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-sm flex items-center justify-between">
                        <div>
                            <div class="text-[10px] font-bold uppercase text-amber-600">Manual Logouts</div>
                            <div class="text-xl font-black text-amber-600 mt-0.5">${totalLogouts}</div>
                        </div>
                        <div class="p-2.5 rounded-xl bg-amber-50 text-amber-600">
                            <i data-lucide="log-out" class="w-5 h-5"></i>
                        </div>
                    </div>
                    <div class="bg-white rounded-2xl p-4 border border-slate-200/80 shadow-sm flex items-center justify-between">
                        <div>
                            <div class="text-[10px] font-bold uppercase text-rose-600">Inactivity Expirations</div>
                            <div class="text-xl font-black text-rose-600 mt-0.5">${totalAutoExpired}</div>
                        </div>
                        <div class="p-2.5 rounded-xl bg-rose-50 text-rose-600">
                            <i data-lucide="clock" class="w-5 h-5"></i>
                        </div>
                    </div>
                </div>

                <!-- Session Date & Condition Filter Bar -->
                ${this.renderFilterBar("sessions")}

                <!-- Non-blocking Session Search Box -->
                ${this.renderSearchBox("sessions", "Search session ID, role, event type, IST timestamp, IP address, details...")}

                <!-- Session Cards List -->
                <div id="sessions-grid" class="space-y-3">
                    ${this.renderSessionCards(filtered)}
                </div>
            </div>
        `;
    }

    getFilteredSessionLogs() {
        const q = (this.searchQueries.sessions || "").toLowerCase().trim();
        const evType = (this.sessionFilters.eventType || "ALL").toUpperCase();
        const role = (this.sessionFilters.role || "ALL").toUpperCase();
        const from = this.sessionFilters.dateFrom;
        const to = this.sessionFilters.dateTo;

        return this.sessionLogs.filter(log => {
            if (evType !== "ALL" && (log.eventType || "").toUpperCase() !== evType) return false;
            if (role !== "ALL" && (log.role || "").toUpperCase() !== role) return false;

            if (from || to) {
                const logDate = this.normalizeRecordDate(log);
                if (logDate) {
                    if (from && logDate < from) return false;
                    if (to && logDate > to) return false;
                }
            }

            if (!q) return true;
            const haystack = [
                log.id,
                log.role,
                log.roleName,
                log.eventType,
                log.timestamp,
                log.ipAddress,
                log.details,
                log.duration
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderSessionCards(sessionsList) {
        if (sessionsList.length === 0) {
            return `
                <div class="py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="history" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No login/logout records matching criteria.</p>
                </div>
            `;
        }

        return sessionsList.map(session => {
            const isLogin = session.eventType === 'LOGIN';
            const isAutoExpired = session.eventType === 'AUTO_EXPIRED';

            const badgeColor = isLogin 
                ? 'bg-emerald-100 text-emerald-800 border-emerald-200' 
                : isAutoExpired 
                    ? 'bg-rose-100 text-rose-800 border-rose-200' 
                    : 'bg-amber-100 text-amber-800 border-amber-200';

            const iconName = isLogin ? 'log-in' : isAutoExpired ? 'clock' : 'log-out';
            const eventLabel = isLogin ? 'LOGGED IN' : isAutoExpired ? 'AUTO-EXPIRED (INACTIVITY)' : 'LOGGED OUT';

            return `
                <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive space-y-3 text-xs">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-2.5 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-black uppercase border ${badgeColor}">
                                <i data-lucide="${iconName}" class="w-3.5 h-3.5"></i>
                                <span>${eventLabel}</span>
                            </span>
                            <span class="font-bold text-slate-500 font-mono text-[11px]">${session.id}</span>
                        </div>
                        <div class="flex items-center gap-2 text-slate-600 font-bold text-[11px] bg-slate-50 px-3 py-1 rounded-xl border border-slate-100">
                            <i data-lucide="clock" class="w-3.5 h-3.5 text-purple-600"></i>
                            <span class="font-mono text-purple-950 font-extrabold">${session.timestamp}</span>
                        </div>
                    </div>

                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                        <div class="text-slate-800 text-sm leading-relaxed">
                            ${session.details}
                        </div>
                        <div class="shrink-0 flex items-center gap-1.5 text-xs font-bold text-slate-600 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-100">
                            <span class="text-[10px] text-slate-400 uppercase">Duration:</span>
                            <span class="font-mono ${isLogin ? 'text-emerald-700' : isAutoExpired ? 'text-rose-700' : 'text-slate-800'}">${session.duration || '-'}</span>
                        </div>
                    </div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500 font-semibold">
                        <div class="flex items-center gap-2">
                            <span>Role: <strong class="text-purple-700 font-bold">${session.roleName || session.role}</strong></span>
                            <span class="text-[10px] text-slate-400 font-mono uppercase">(${session.role})</span>
                        </div>
                        <div class="flex items-center gap-2">
                            <span class="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md bg-purple-50 text-purple-700 font-mono text-[10px] border border-purple-100">
                                <i data-lucide="globe" class="w-3 h-3 text-purple-400"></i> ${session.ipAddress || '127.0.0.1'}
                            </span>
                        </div>
                    </div>
                </div>
            `;
        }).join("");
    }

    renderAuditCards(logsList) {
        if (logsList.length === 0) {
            return `
                <div class="py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="file-text" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No audit records matching criteria.</p>
                </div>
            `;
        }

        return logsList.map(log => {
            const isCrit = (log.severity || '').toUpperCase() === 'CRITICAL';
            const isWarn = (log.severity || '').toUpperCase() === 'WARNING';
            const isRefund = (log.actionType || '').includes('REFUND');

            const badgeColor = isCrit ? 'bg-rose-100 text-rose-800' : isWarn ? 'bg-amber-100 text-amber-800' : isRefund ? 'bg-emerald-100 text-emerald-800' : 'bg-indigo-100 text-indigo-800';

            return `
                <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive space-y-3 text-xs">
                    <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="pill-status ${badgeColor}">${(log.actionType || 'ACTION').replace(/_/g, ' ')}</span>
                            <span class="font-bold text-slate-400 text-[10px] uppercase">${log.severity || 'INFO'}</span>
                        </div>
                        <span class="text-slate-400 text-[11px] font-medium">${log.timestamp}</span>
                    </div>

                    <div class="text-slate-800 text-sm leading-relaxed">${log.details}</div>

                    <div class="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500 font-semibold">
                        <div class="flex items-center gap-2">
                            <span>Actor: <strong class="text-purple-700">${log.adminRole || 'ADMIN'}</strong></span>
                            <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-purple-50 text-purple-700 font-mono text-[10px] border border-purple-100">
                                <i data-lucide="globe" class="w-3 h-3 text-purple-400"></i> ${log.ipAddress || '127.0.0.1'}
                            </span>
                        </div>
                        <div>Target: <span class="text-slate-800 font-bold">${log.targetEntityId || '-'}</span></div>
                    </div>
                </div>
            `;
        }).join("");
    }

    // ==========================================
    // TAB 9: REPORTS
    // ==========================================
    getFilteredReportsOrders() {
        const q = (this.searchQueries.reports || "").toLowerCase().trim();
        const status = (this.reportsFilters.status || "ALL").toUpperCase();
        const from = this.reportsFilters.dateFrom;
        const to = this.reportsFilters.dateTo;

        return this.orders.filter(o => {
            if (status !== "ALL" && (o.status || "").toUpperCase() !== status) return false;

            if (from || to) {
                const oDate = this.normalizeRecordDate(o);
                if (oDate) {
                    if (from && oDate < from) return false;
                    if (to && oDate > to) return false;
                }
            }

            if (!q) return true;
            const haystack = [
                o.id,
                o.customerName,
                o.customerId,
                o.runnerName,
                o.pickPoint,
                o.dropPoint,
                o.status,
                `Rs. ${o.totalMax}`,
                `Rs. ${o.actualBill}`,
                `Rs. ${o.deliveryFee}`
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderReportsTab() {
        const filtered = this.getFilteredReportsOrders();
        const filteredAudit = this.getFilteredAudit();
        const totalGMV = filtered.reduce((acc, o) => acc + (o.totalMax || 0), 0);
        const totalPlatformFees = filtered.reduce((acc, o) => acc + (o.deliveryFee || 0), 0);
        const totalEscrow = this.disputes.reduce((acc, d) => acc + (d.escrowAmount || 0), 0);
        const totalWallets = this.students.reduce((acc, s) => acc + (s.walletBalance || 0), 0);

        return `
            <div class="space-y-6">
                <div>
                    <h2 class="text-xl font-extrabold text-slate-900">Financial Reports & Ledgers</h2>
                    <p class="text-xs text-slate-500 mt-1">Financial ledgers and CSV export generation.</p>
                </div>

                <!-- Date & Condition Filter Bar for Reports -->
                ${this.renderFilterBar("reports")}

                <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4">
                    <div class="flex items-center justify-between">
                        <h3 class="text-sm font-bold text-slate-900">Platform Financial Summary</h3>
                        <span class="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-purple-100 text-purple-800">
                            Based on ${filtered.length} Filtered Orders
                        </span>
                    </div>

                    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 pt-2">
                        <div class="p-4 rounded-xl bg-purple-50 border border-purple-100">
                            <div class="text-[11px] text-purple-700 font-semibold">Gross Merchandise Value (GMV)</div>
                            <div class="text-xl font-black text-purple-900 mt-1">Rs. ${totalGMV}</div>
                        </div>
                        <div class="p-4 rounded-xl bg-emerald-50 border border-emerald-100">
                            <div class="text-[11px] text-emerald-700 font-semibold">Commissions Collected</div>
                            <div class="text-xl font-black text-emerald-900 mt-1">Rs. ${totalPlatformFees}</div>
                        </div>
                        <div class="p-4 rounded-xl bg-amber-50 border border-amber-100">
                            <div class="text-[11px] text-amber-700 font-semibold">Dispute Escrow Reserved</div>
                            <div class="text-xl font-black text-amber-900 mt-1">Rs. ${totalEscrow}</div>
                        </div>
                        <div class="p-4 rounded-xl bg-sky-50 border border-sky-100">
                            <div class="text-[11px] text-sky-700 font-semibold">Total Student Wallet Liability</div>
                            <div class="text-xl font-black text-sky-900 mt-1">Rs. ${totalWallets}</div>
                        </div>
                    </div>

                    <div class="pt-6 border-t border-slate-100 grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <button onclick="app.downloadOrdersCsv('reports')" class="p-4 rounded-2xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs flex items-center justify-center gap-3 transition shadow-sm">
                            <i data-lucide="file-spreadsheet" class="w-5 h-5"></i>
                            <div class="text-left">
                                <div class="font-black text-sm">Download Orders Financial CSV</div>
                                <div class="text-[11px] font-normal text-purple-200">Export ${filtered.length} orders matching active date & condition filter</div>
                            </div>
                        </button>

                        <button onclick="app.downloadAuditCsv()" class="p-4 rounded-2xl bg-slate-800 hover:bg-slate-900 text-white font-bold text-xs flex items-center justify-center gap-3 transition shadow-sm">
                            <i data-lucide="cloud-download" class="w-5 h-5"></i>
                            <div class="text-left">
                                <div class="font-black text-sm">Download Filtered Audit Trail CSV Log</div>
                                <div class="text-[11px] font-normal text-slate-300">Export ${filteredAudit.length} audit records matching active date & condition filter</div>
                            </div>
                        </button>
                    </div>
                </div>

                <!-- Financial Orders Search & Table -->
                <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4">
                    <div class="flex items-center justify-between">
                        <h3 class="text-sm font-bold text-slate-900">Financial Order Ledger</h3>
                        <span id="reports-count-badge" class="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700">
                            ${filtered.length} Orders in Report
                        </span>
                    </div>

                    <!-- Search Box -->
                    ${this.renderSearchBox("reports", "Search ledger by Order ID, Customer, Runner, Pickup, Drop, Amount...")}

                    <div class="overflow-x-auto custom-scrollbar-light">
                        <table class="w-full text-xs text-left">
                            <thead class="text-slate-400 font-bold uppercase border-b border-slate-100 text-[10px]">
                                <tr>
                                    <th class="py-2.5 px-3">Order ID</th>
                                    <th class="py-2.5 px-3">Customer</th>
                                    <th class="py-2.5 px-3">Runner</th>
                                    <th class="py-2.5 px-3">Status</th>
                                    <th class="py-2.5 px-3 text-right">Estimated</th>
                                    <th class="py-2.5 px-3 text-right">Actual Bill</th>
                                    <th class="py-2.5 px-3 text-right">Delivery Fee</th>
                                </tr>
                            </thead>
                            <tbody id="reports-orders-grid" class="divide-y divide-slate-100">
                                ${this.renderReportsOrderRows(filtered)}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    }

    renderReportsOrderRows(ordersList) {
        if (ordersList.length === 0) {
            return `
                <tr>
                    <td colspan="7" class="py-8 text-center text-slate-400">No orders matching ledger search query.</td>
                </tr>
            `;
        }

        return ordersList.map(o => `
            <tr class="hover:bg-slate-50 transition">
                <td class="py-3 px-3 font-bold text-purple-900">${o.id}</td>
                <td class="py-3 px-3">${o.customerName} <span class="text-slate-400 font-normal">(${o.customerId})</span></td>
                <td class="py-3 px-3">${o.runnerName || '<span class="text-slate-400 italic">Unassigned</span>'}</td>
                <td class="py-3 px-3"><span class="pill-status bg-slate-100 text-slate-700 text-[10px]">${o.status}</span></td>
                <td class="py-3 px-3 text-right font-medium text-slate-600">Rs. ${o.totalMax}</td>
                <td class="py-3 px-3 text-right font-bold ${o.actualBill > 0 ? 'text-emerald-700' : 'text-slate-400'}">Rs. ${o.actualBill}</td>
                <td class="py-3 px-3 text-right font-bold text-emerald-700">Rs. ${o.deliveryFee}</td>
            </tr>
        `).join("");
    }

    downloadOrdersCsv(source = "reports") {
        const ordersList = source === "reports" ? this.getFilteredReportsOrders() : this.getFilteredOrders();
        const filters = source === "reports" ? this.reportsFilters : this.ordersFilters;
        
        if (ordersList.length === 0) {
            this.showToast("No orders match the selected condition or date filters to download.", "warning");
            return;
        }

        const rows = [["OrderID", "Timestamp", "CustomerEmail", "CustomerName", "Runner", "PickPoint", "DropPoint", "Status", "EstimatedBill", "ActualBill", "DeliveryFee"]];
        ordersList.forEach(o => {
            rows.push([
                o.id,
                `"${o.timestamp || 'N/A'}"`,
                `"${o.customerId}"`,
                `"${o.customerName || 'Customer'}"`,
                `"${o.runnerName || 'Unassigned'}"`,
                `"${o.pickPoint}"`,
                `"${o.dropPoint}"`,
                o.status,
                o.totalMax,
                o.actualBill,
                o.deliveryFee
            ]);
        });

        let suffix = "";
        if (filters.status && filters.status !== "ALL") suffix += `_${filters.status}`;
        if (filters.dateFrom) suffix += `_from_${filters.dateFrom}`;
        if (filters.dateTo) suffix += `_to_${filters.dateTo}`;
        if (!suffix) suffix = `_${this.getTodayDateString()}`;

        const filename = `CampusAdmin_Orders_Report${suffix}.csv`;
        this.downloadCsvFile(filename, rows);
    }

    downloadAuditCsv() {
        const logsList = this.getFilteredAudit();
        const filters = this.auditFilters;

        if (logsList.length === 0) {
            this.showToast("No audit records match the selected condition or date filters to download.", "warning");
            return;
        }

        const rows = [["ID", "Timestamp", "AdminRole", "IP_Address", "ActionType", "TargetID", "Severity", "Details"]];
        logsList.forEach(l => {
            const escapedDetails = (l.details || "").replace(/"/g, '""');
            rows.push([
                l.id,
                `"${l.timestamp}"`,
                l.adminRole,
                `"${l.ipAddress || '127.0.0.1'}"`,
                l.actionType,
                `"${l.targetEntityId}"`,
                l.severity,
                `"${escapedDetails}"`
            ]);
        });

        let suffix = "";
        if (filters.severity && filters.severity !== "ALL") suffix += `_${filters.severity}`;
        if (filters.role && filters.role !== "ALL") suffix += `_${filters.role}`;
        if (filters.dateFrom) suffix += `_from_${filters.dateFrom}`;
        if (filters.dateTo) suffix += `_to_${filters.dateTo}`;
        if (!suffix) suffix = `_${this.getTodayDateString()}`;

        const filename = `CampusAdmin_Audit_Trail${suffix}.csv`;
        this.downloadCsvFile(filename, rows);
    }

    downloadSessionsCsv() {
        const logsList = this.getFilteredSessionLogs();
        const filters = this.sessionFilters;

        if (logsList.length === 0) {
            this.showToast("No session records match the selected condition or date filters to download.", "warning");
            return;
        }

        const rows = [["Session_ID", "Role_ID", "Role_Name", "Event_Type", "Timestamp_IST", "IP_Address", "Duration", "Details"]];
        logsList.forEach(l => {
            const escapedDetails = (l.details || "").replace(/"/g, '""');
            rows.push([
                l.id,
                l.role,
                `"${l.roleName || l.role}"`,
                l.eventType,
                `"${l.timestamp}"`,
                `"${l.ipAddress || '127.0.0.1'}"`,
                `"${l.duration || ''}"`,
                `"${escapedDetails}"`
            ]);
        });

        let suffix = "";
        if (filters.eventType && filters.eventType !== "ALL") suffix += `_${filters.eventType}`;
        if (filters.role && filters.role !== "ALL") suffix += `_${filters.role}`;
        if (filters.dateFrom) suffix += `_from_${filters.dateFrom}`;
        if (filters.dateTo) suffix += `_to_${filters.dateTo}`;
        if (!suffix) suffix = `_${this.getTodayDateString()}`;

        const filename = `CampusAdmin_Login_Logout_Access_IST${suffix}.csv`;
        this.downloadCsvFile(filename, rows);
    }

    downloadCsvFile(filename, rows) {
        const content = rows.map(r => r.join(",")).join("\n");
        const blob = new Blob([content], { type: "text/csv;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.showToast(`Downloaded RFC CSV: ${filename}!`);
    }

    downloadTextFile(filename, textContent) {
        const blob = new Blob([textContent], { type: "text/plain;charset=utf-8;" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.showToast(`Downloaded Text File: ${filename}!`);
    }

    // ==========================================
    // TAB 10: FEEDBACK & ROADMAP
    // ==========================================
    getFilteredFeedback() {
        const q = (this.searchQueries.feedback || "").toLowerCase().trim();
        const subTab = this.searchQueries.feedbackSubTab || 0;

        const list = subTab === 0 ? this.feedback.filter(f => !f.isAcceptedAsTask) : this.feedback.filter(f => f.isAcceptedAsTask);

        return list.filter(fb => {
            if (!q) return true;
            const haystack = [
                fb.id,
                fb.userName,
                fb.userEmail,
                fb.userRole,
                fb.category,
                fb.message,
                fb.timestamp,
                fb.adminNote,
                fb.taskStatus,
                `${fb.rating} star`
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderFeedbackTab() {
        const subTab = this.searchQueries.feedbackSubTab;
        const unaccepted = this.feedback.filter(fb => !fb.isAcceptedAsTask);
        const planned = this.feedback.filter(fb => fb.isAcceptedAsTask);
        const data = this.getFilteredFeedback();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Feedback & Roadmap</h2>
                        <p class="text-xs text-slate-500 mt-1">Student suggestions, bug reports, and product roadmap.</p>
                    </div>

                    <!-- Sub Tab Selector -->
                    <div class="flex items-center gap-1.5 p-1 bg-slate-100 rounded-xl text-xs font-semibold self-start sm:self-auto">
                        <button onclick="app.setFeedbackSubTab(0)" class="px-3.5 py-1.5 rounded-lg transition ${subTab === 0 ? 'bg-white shadow text-purple-900 font-bold' : 'text-slate-600 hover:text-slate-900'}">User Feedbacks (${unaccepted.length})</button>
                        <button onclick="app.setFeedbackSubTab(1)" class="px-3.5 py-1.5 rounded-lg transition ${subTab === 1 ? 'bg-white shadow text-purple-900 font-bold' : 'text-slate-600 hover:text-slate-900'}">Roadmap Tasks (${planned.length})</button>
                    </div>
                </div>

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("feedback", "Search suggestions, submitters, categories, roadmap items, notes...")}

                <div id="feedback-grid" class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    ${this.renderFeedbackCards(data)}
                </div>
            </div>
        `;
    }

    renderFeedbackCards(itemsList) {
        const subTab = this.searchQueries.feedbackSubTab;

        if (itemsList.length === 0) {
            return `
                <div class="col-span-full py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="message-square" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No feedback or roadmap items matching criteria.</p>
                </div>
            `;
        }

        if (subTab === 0) {
            return itemsList.map(fb => `
                <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive space-y-3 text-xs flex flex-col justify-between">
                    <div class="space-y-2">
                        <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                            <div class="font-bold text-slate-900">${fb.userName} <span class="text-purple-700">(${fb.userRole})</span></div>
                            <div class="text-amber-500 font-black text-sm">${'&#9733;'.repeat(fb.rating)}</div>
                        </div>
                        <div class="text-slate-500">Category: ${fb.category} | ${fb.timestamp}</div>
                        <div class="text-slate-800 text-sm font-medium">"${fb.message}"</div>
                    </div>

                    <div class="pt-3 border-t border-slate-100 grid grid-cols-2 gap-2">
                        <button onclick="app.acceptFeedbackTask('${fb.id}')" class="py-2 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-center transition">
                            Accept as Task
                        </button>
                        <button onclick="app.ignoreFeedback('${fb.id}')" class="py-2 px-3 rounded-xl bg-slate-400 hover:bg-slate-500 text-white font-bold text-center transition">
                            Ignore
                        </button>
                    </div>
                </div>
            `).join("");
        } else {
            return itemsList.map(task => {
                const isDone = task.taskStatus === "COMPLETED";

                return `
                    <div class="bg-white rounded-2xl p-5 border border-emerald-200/80 shadow-sm card-interactive space-y-3 text-xs flex flex-col justify-between bg-emerald-50/20">
                        <div class="space-y-2">
                            <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                <span class="font-bold text-emerald-800 text-sm">Roadmap Item: ${task.category}</span>
                                <span class="pill-status ${isDone ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}">${task.taskStatus}</span>
                            </div>
                            <div class="text-slate-900 font-semibold text-sm">Feature: "${task.message}"</div>
                            <div class="text-slate-500">Submitted by: ${task.userName} (${task.userRole})</div>
                            ${task.adminNote ? `<div class="p-2 rounded-xl bg-purple-50 text-purple-800 font-semibold text-[11px]">Note: ${task.adminNote}</div>` : ''}
                        </div>

                        <div class="pt-3 border-t border-slate-100 flex justify-end">
                            ${!isDone ? `
                                <button onclick="app.completeRoadmapTask('${task.id}')" class="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold transition">
                                    Mark Completed
                                </button>
                            ` : `
                                <span class="text-emerald-700 font-black text-xs">&check; Completed in Production</span>
                            `}
                        </div>
                    </div>
                `;
            }).join("");
        }
    }

    setFeedbackSubTab(sub) {
        this.searchQueries.feedbackSubTab = sub;
        this.renderCurrentTab();
    }

    acceptFeedbackTask(id) {
        const fb = this.feedback.find(f => f.id === id);
        if (!fb) return;
        fb.isAcceptedAsTask = true;
        fb.taskStatus = "PLANNED";
        fb.adminNote = "Accepted into Roadmap";
        this.save("campus_user_feedback_list", this.feedback);
        this.recordAudit("FEEDBACK_TASK_ACCEPTED", fb.id, `Accepted suggestion from ${fb.userName} into Roadmap.`, "INFO");
        this.showToast("Feedback accepted and moved to Roadmap Tasks!");
        this.updateSectionGrid("feedback");
    }

    ignoreFeedback(id) {
        const idx = this.feedback.findIndex(f => f.id === id);
        if (idx === -1) return;
        const removed = this.feedback.splice(idx, 1)[0];
        this.save("campus_user_feedback_list", this.feedback);
        this.recordAudit("FEEDBACK_IGNORED", id, `Removed feedback suggestion from ${removed.userName}.`, "INFO");
        this.showToast("Suggestion ignored and removed.");
        this.updateSectionGrid("feedback");
    }

    completeRoadmapTask(id) {
        const task = this.feedback.find(f => f.id === id);
        if (!task) return;
        task.taskStatus = "COMPLETED";
        this.save("campus_user_feedback_list", this.feedback);
        this.recordAudit("ROADMAP_TASK_COMPLETED", task.id, `Marked feature "${task.message}" as completed in release.`, "INFO");
        this.showToast("Task marked as completed!");
        this.updateSectionGrid("feedback");
    }

    // ==========================================
    // MODALS: RBAC, SYSTEM CONFIG, 2-STEP PIN
    // ==========================================
    openRoleSwitcherModal() {
        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <h3 class="text-base font-extrabold text-slate-900">Switch Active Admin Role</h3>
                        <button onclick="app.closeModal()" class="font-bold text-slate-400 text-xl">&times;</button>
                    </div>

                    <p class="text-xs text-slate-500 mt-2">Select an administrative role to switch workspace.</p>

                    <div class="space-y-2.5 mt-4 text-xs">
                        ${Object.values(ADMIN_ROLES).map(role => `
                            <button onclick="app.switchRole('${role.id}')" class="w-full text-left p-3.5 rounded-2xl border transition ${this.activeRole === role.id ? 'border-purple-600 bg-purple-50 shadow-sm' : 'border-slate-200 hover:bg-slate-50'}">
                                <div class="font-bold text-slate-900 flex items-center justify-between">
                                    <span>${role.name}</span>
                                    ${this.activeRole === role.id ? '<span class="text-purple-700 font-black">&check; Active</span>' : ''}
                                </div>
                                <div class="text-[11px] text-slate-500 mt-1">${role.description}</div>
                            </button>
                        `).join("")}
                    </div>

                    <div class="mt-4 pt-3 border-t border-slate-100 flex justify-end">
                        <button onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold text-xs">Close</button>
                    </div>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
    }

    getRolePin(roleId) {
        switch (roleId) {
            case "SUPER_ADMIN": return this.superAdminPin;
            case "OPERATIONS_OFFICER": return this.opsStaffPin;
            case "FINANCIAL_AUDITOR": return this.financialAuditorPin;
            case "SAFETY_DISPUTE_OFFICER": return this.safetyDisputePin;
            default: return null;
        }
    }

    switchRole(roleId) {
        if (roleId === this.activeRole) {
            this.closeModal();
            return;
        }

        // Always ask and verify the target role's designated PIN on every role switch
        this.openRoleVerificationModal(roleId);
    }

    openRoleVerificationModal(targetRoleId) {
        const targetRole = ADMIN_ROLES[targetRoleId];
        if (!targetRole) return;
        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-sm rounded-3xl p-6 shadow-2xl border border-slate-200 animate-in fade-in zoom-in-95 duration-150">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="w-8 h-8 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center">
                                <i data-lucide="shield-check" class="w-4 h-4"></i>
                            </span>
                            <h3 class="text-sm font-extrabold text-slate-900">Verify Role PIN</h3>
                        </div>
                        <button onclick="app.closeModal()" class="font-bold text-slate-400 text-xl hover:text-slate-600">&times;</button>
                    </div>

                    <div class="mt-3 p-3 rounded-2xl bg-slate-50 border border-slate-200">
                        <div class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Target Workspace</div>
                        <div class="text-sm font-black text-slate-900 mt-0.5">${targetRole.name}</div>
                    </div>

                    <p class="text-xs text-slate-500 mt-3">
                        Enter the security PIN for <strong>${targetRole.name}</strong> to switch:
                    </p>

                    <form onsubmit="app.verifyRolePinAndSwitch(event, '${targetRoleId}')" class="space-y-4 mt-3 text-xs">
                        <div>
                            <input 
                                type="password" 
                                maxlength="6" 
                                id="role-pin-input" 
                                required 
                                class="w-full px-4 py-2.5 border-2 border-slate-200 focus:border-purple-600 rounded-xl text-center text-xl tracking-widest font-mono font-bold outline-none" 
                                placeholder="****"
                                autocomplete="off"
                            />
                            <p id="role-pin-error" class="text-rose-600 font-bold mt-1.5 hidden"></p>
                        </div>
                        <div class="flex justify-end gap-2 pt-1">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold hover:bg-slate-50">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold transition shadow-sm">Verify & Unlock</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) lucide.createIcons();
        const inp = document.getElementById("role-pin-input");
        if (inp) inp.focus();
    }

    verifyRolePinAndSwitch(e, targetRoleId) {
        e.preventDefault();
        const entered = document.getElementById("role-pin-input").value.trim();
        const err = document.getElementById("role-pin-error");
        const targetRole = ADMIN_ROLES[targetRoleId];
        const correctPin = this.getRolePin(targetRoleId);

        if (entered === correctPin) {
            this.roleSwitchFailedAttempts = 0;
            this.failedAttempts = 0;
            localStorage.setItem("admin_failed_attempts", "0");

            const prevRole = this.activeRole;
            const prevRoleMeta = ADMIN_ROLES[prevRole] || { name: prevRole };
            const duration = this.formatDuration(Math.max(0, Date.now() - (this.currentSessionStartTime || Date.now())));
            this.recordSessionEvent("LOGOUT", `Role switched from ${prevRoleMeta.name} to ${targetRole.name}.`, duration);

            this.activeRole = targetRoleId;
            localStorage.setItem("admin_active_role", targetRoleId);
            this.currentSessionStartTime = Date.now();
            this.lastUserActivity = Date.now();
            this.selectedTab = targetRole.allowedTabs[0] || 0;
            this.recordSessionEvent("LOGIN", `Role switch authenticated for ${targetRole.name}.`);
            this.recordAudit("ROLE_SWITCH_SUCCESS", targetRoleId, `Switched active role from ${prevRoleMeta.name} to ${targetRole.name} upon successful PIN authentication.`, "INFO");
            this.closeModal();
            this.showToast(`Switched active role to ${targetRole.name}`);
            this.render();
        } else {
            this.roleSwitchFailedAttempts = (this.roleSwitchFailedAttempts || 0) + 1;
            this.failedAttempts = this.roleSwitchFailedAttempts;
            localStorage.setItem("admin_failed_attempts", this.failedAttempts.toString());

            if (this.roleSwitchFailedAttempts >= 3) {
                // 3 Wrong PIN attempts: Lock console for 24 hours & trigger rescue email procedure!
                this.lockoutUntil = Date.now() + (24 * 60 * 60 * 1000); // 24 Hours
                localStorage.setItem("admin_lockout_until", this.lockoutUntil.toString());
                this.isUnlocked = false;
                sessionStorage.setItem("admin_is_unlocked", "false");
                this.roleSwitchFailedAttempts = 0;

                this.recordAudit("SECURITY_LOCKOUT_24H", targetRoleId, `Console locked for 24 hours after 3 failed PIN attempts during role switch to ${targetRole.name}. Emergency warning alert dispatched to ${this.alertEmail}.`, "CRITICAL");
                this.closeModal();
                this.render();
                this.openEmergencyEmailModal();
                this.showToast("3 failed attempts. Console locked for 24 hours.", "error");
            } else {
                const remaining = 3 - this.roleSwitchFailedAttempts;
                this.recordAudit("AUTH_FAILED_ATTEMPT", targetRoleId, `Incorrect PIN for ${targetRole.name} (${this.roleSwitchFailedAttempts}/3).`, "WARNING");
                err.textContent = `Incorrect PIN. ${remaining} attempt${remaining === 1 ? '' : 's'} remaining before 24-hour lockout.`;
                err.classList.remove("hidden");
                const inp = document.getElementById("role-pin-input");
                if (inp) {
                    inp.value = "";
                    inp.focus();
                }
            }
        }
    }

    // Cloud Telemetry & Sync Inspection Modal
    openCloudSyncModal() {
        const isLive = window.cloudSync && window.cloudSync.isConnected;
        const lastSync = (window.cloudSync && window.cloudSync.lastSyncTime) ? window.cloudSync.lastSyncTime : "Just now";
        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200 animate-in fade-in zoom-in-95 duration-150">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <div class="flex items-center gap-2">
                            <span class="w-8 h-8 rounded-xl ${isLive ? 'bg-emerald-100 text-emerald-600' : 'bg-amber-100 text-amber-600'} flex items-center justify-center">
                                <i data-lucide="cloud-lightning" class="w-4 h-4"></i>
                            </span>
                            <div>
                                <h3 class="text-sm font-extrabold text-slate-900">Realtime Cloud Sync Bridge</h3>
                                <div class="text-[10px] text-slate-400 font-mono">ntfy.sh &bull; Topic: campusapp_orders_e8435_sync</div>
                            </div>
                        </div>
                        <button onclick="app.closeModal()" class="font-bold text-slate-400 text-xl">&times;</button>
                    </div>

                    <div class="space-y-3 mt-4 text-xs">
                        <div class="p-3 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
                            <div>
                                <div class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Bridge Engine Status</div>
                                <div class="text-xs font-black ${isLive ? 'text-emerald-700' : 'text-amber-700'} flex items-center gap-1.5 mt-0.5">
                                    <span class="w-2 h-2 rounded-full ${isLive ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'}"></span>
                                    <span>${isLive ? 'Active & Synchronized (Zero Login)' : 'Connecting / Local Buffer Active'}</span>
                                </div>
                            </div>
                            <div class="text-right">
                                <div class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Last Heartbeat</div>
                                <div class="text-xs font-mono font-bold text-slate-700 mt-0.5">${lastSync}</div>
                            </div>
                        </div>

                        <div class="p-2.5 rounded-xl bg-purple-50 border border-purple-100 text-purple-900 flex items-center gap-2 text-xs font-semibold">
                            <i data-lucide="arrow-left-right" class="w-3.5 h-3.5 text-purple-700 shrink-0"></i>
                            <span>Bi-directional real-time sync with CampusApp (Android)</span>
                        </div>

                        <div class="grid grid-cols-2 gap-2 text-[11px]">
                            <div class="p-2.5 rounded-xl border border-slate-100 bg-white">
                                <div class="text-slate-400 font-semibold">Orders Node</div>
                                <div class="font-bold text-slate-800 font-mono mt-0.5">${this.orders.length} Synced</div>
                            </div>
                            <div class="p-2.5 rounded-xl border border-slate-100 bg-white">
                                <div class="text-slate-400 font-semibold">Students Node</div>
                                <div class="font-bold text-slate-800 font-mono mt-0.5">${this.students.length} Synced</div>
                            </div>
                            <div class="p-2.5 rounded-xl border border-slate-100 bg-white">
                                <div class="text-slate-400 font-semibold">Riders Node</div>
                                <div class="font-bold text-slate-800 font-mono mt-0.5">${this.riders.length} Synced</div>
                            </div>
                            <div class="p-2.5 rounded-xl border border-slate-100 bg-white">
                                <div class="text-slate-400 font-semibold">Disputes Node</div>
                                <div class="font-bold text-slate-800 font-mono mt-0.5">${this.disputes.length} Synced</div>
                            </div>
                        </div>
                    </div>

                    <div class="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                        <button onclick="app.triggerPushAllCloud()" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs flex items-center gap-1.5 transition">
                            <i data-lucide="upload-cloud" class="w-3.5 h-3.5"></i>
                            <span>Push All to Cloud</span>
                        </button>
                        <button onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold text-xs">Close</button>
                    </div>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) lucide.createIcons();
    }

    triggerPushAllCloud() {
        if (window.cloudSync) {
            const success = window.cloudSync.pushAllLocalToCloud();
            if (success) {
                this.recordAudit("CLOUD_SYNC_PUSH_ALL", "FIREBASE_SYNC", "Pushed all current local orders, students, riders, and disputes to Firebase Cloud database campusapp-e8435.", "INFO");
                this.showToast("All data successfully pushed to Firebase Cloud!", "success");
                this.closeModal();
            } else {
                this.showToast("Cloud sync buffered. Will sync when connection is live.", "warning");
            }
        }
    }

    // 2-Step Change Security PIN Modal matching ChangePinDialog in AdminMainActivity.kt
    openChangePinModal() {
        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-sm rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                        <h3 class="text-base font-extrabold text-slate-900">Step 1: Verify Current PIN</h3>
                        <button onclick="app.closeModal()" class="font-bold text-slate-400 text-xl">&times;</button>
                    </div>
                    <p class="text-xs text-slate-500 mt-2">Enter your current active PIN to authenticate.</p>

                    <form onsubmit="app.verifyCurrentPinStep(event)" class="space-y-4 mt-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Current PIN:</label>
                            <input type="password" maxlength="6" id="curr-pin-val" required class="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-center text-lg tracking-widest font-mono font-bold" autofocus />
                            <p id="curr-pin-error" class="text-rose-600 font-bold mt-1.5 hidden"></p>
                        </div>
                        <div class="flex justify-end gap-2">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold">Verify Existing PIN</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
    }

    verifyCurrentPinStep(e) {
        e.preventDefault();
        const input = document.getElementById("curr-pin-val");
        const err = document.getElementById("curr-pin-error");
        const val = input.value.trim();

        if (val === this.currentPin) {
            this.openSetNewPinModal();
        } else {
            err.textContent = "Incorrect current PIN. Verification failed.";
            err.classList.remove("hidden");
            input.value = "";
            input.focus();
        }
    }

    openSetNewPinModal() {
        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-sm rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                        <h3 class="text-base font-extrabold text-slate-900">Step 2: Set New Master PIN</h3>
                        <button onclick="app.closeModal()" class="font-bold text-slate-400 text-xl">&times;</button>
                    </div>
                    <p class="text-xs text-slate-500 mt-2">Enter a new 4 to 6 digit master PIN. Once saved, only the new PIN can unlock.</p>

                    <form onsubmit="app.submitNewPinConfirm(event)" class="space-y-4 mt-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">New PIN (4-6 Digits):</label>
                            <input type="password" maxlength="6" id="new-pin-1" required class="w-full px-4 py-2 border border-slate-200 rounded-xl text-center text-lg tracking-widest font-mono font-bold" />
                        </div>
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Confirm New PIN:</label>
                            <input type="password" maxlength="6" id="new-pin-2" required class="w-full px-4 py-2 border border-slate-200 rounded-xl text-center text-lg tracking-widest font-mono font-bold" />
                            <p id="new-pin-error" class="text-rose-600 font-bold mt-1.5 hidden"></p>
                        </div>
                        <div class="flex justify-end gap-2">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold">Save New PIN</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
    }

    submitNewPinConfirm(e) {
        e.preventDefault();
        const p1 = document.getElementById("new-pin-1").value.trim();
        const p2 = document.getElementById("new-pin-2").value.trim();
        const err = document.getElementById("new-pin-error");

        if (p1.length < 4) {
            err.textContent = "PIN must be at least 4 digits.";
            err.classList.remove("hidden");
            return;
        }

        if (p1 !== p2) {
            err.textContent = "New PIN and Confirm PIN do not match.";
            err.classList.remove("hidden");
            return;
        }

        this.currentPin = p1;
        this.superAdminPin = p1;
        localStorage.setItem("admin_security_pin", p1);
        localStorage.setItem("admin_master_pin", p1);

        this.recordAudit("PIN_CHANGED", "SECURITY_PIN", "Master Admin PIN successfully updated and verified.", "WARNING");
        this.closeModal();
        this.showToast("Security PIN updated successfully!");
    }

    openConfigModal() {
        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100">
                        <h3 class="text-base font-extrabold text-slate-900">Configure Client App Version Policy</h3>
                        <button onclick="app.closeModal()" class="font-bold text-slate-400 text-xl">&times;</button>
                    </div>

                    <form onsubmit="app.submitConfig(event)" class="space-y-4 mt-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Minimum Required Version:</label>
                            <input type="text" id="cfg-version" value="${this.systemConfig.minAppVersion}" placeholder="e.g. 2.4.0" class="w-full px-3 py-2 border border-slate-200 rounded-xl" />
                        </div>

                        <label class="flex items-start gap-2.5 font-bold text-slate-800 p-3 rounded-xl bg-slate-50 border border-slate-100">
                            <input type="checkbox" id="cfg-force" ${this.systemConfig.forceUpdateEnabled ? 'checked' : ''} class="mt-0.5 rounded text-purple-700" />
                            <div>
                                <span>Enforce Immediate Client App Updates</span>
                                <div class="text-[11px] text-slate-500 font-normal">Blocks older versions with update prompt on launch.</div>
                            </div>
                        </label>

                        <label class="flex items-start gap-2.5 font-bold text-slate-800 p-3 rounded-xl bg-slate-50 border border-slate-100">
                            <input type="checkbox" id="cfg-maint" ${this.systemConfig.maintenanceMode ? 'checked' : ''} class="mt-0.5 rounded text-purple-700" />
                            <div>
                                <span>Activate Global Maintenance Mode</span>
                                <div class="text-[11px] text-slate-500 font-normal">Displays campus maintenance banner and pauses requests.</div>
                            </div>
                        </label>

                        <div class="flex justify-end gap-2 pt-4 border-t border-slate-100">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold">Save Policy</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
    }

    submitConfig(e) {
        e.preventDefault();
        const ver = document.getElementById("cfg-version").value.trim();
        const force = document.getElementById("cfg-force").checked;
        const maint = document.getElementById("cfg-maint").checked;

        this.systemConfig.minAppVersion = ver;
        this.systemConfig.forceUpdateEnabled = force;
        this.systemConfig.maintenanceMode = maint;
        this.save("campus_system_config", this.systemConfig);

        this.recordAudit("SYSTEM_CONFIG", "SYS_VER", `Updated minimum app version requirement to ${ver} (Force=${force}, Maint=${maint}).`, "WARNING");
        this.closeModal();
        this.showToast("System configuration updated!");
        this.renderCurrentTab();
    }

    // ==========================================
    // REFRESH WHOLE PAGE ENGINE
    // ==========================================
    async refreshWholePage() {
        const icon = document.getElementById("topbar-refresh-icon");
        if (icon) {
            icon.classList.add("animate-spin");
        }
        this.showToast("Pulling live data from CampusApp & Cloud Bridge...", "info");

        try {
            if (window.cloudSync) {
                const count = await window.cloudSync.pollRecentMessages(false);
                if (count > 0) {
                    this.showToast(`✨ Received ${count} new update${count > 1 ? 's' : ''} from CampusApp!`, "success");
                } else {
                    this.showToast("Dashboard refreshed. All services are up-to-date.", "success");
                }
            } else {
                this.showToast("Dashboard refreshed.", "info");
            }
        } catch (e) {
            console.warn("Refresh sync error:", e);
        }

        // Re-read storage and re-render current view cleanly
        this.initData();
        this.renderCurrentTab();
        if (typeof lucide !== "undefined" && lucide.createIcons) {
            lucide.createIcons();
        }

        setTimeout(() => {
            if (icon) {
                icon.classList.remove("animate-spin");
            }
        }, 600);
    }

    // ==========================================
    // CUSTOMER ACTIVITIES INGESTION & BROADCAST
    // ==========================================
    recordCustomerActivity(activity) {
        if (!activity) return;
        const now = new Date();
        const rawTime = activity.rawTimestamp || Date.now();
        const formattedTime = activity.timestamp || `Today, ${now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true })}`;
        const userEmail = (activity.userEmail || "student@college.edu").toLowerCase().trim();

        const badgeColors = {
            ORDERS: "bg-purple-100 text-purple-800",
            COURIER: "bg-indigo-100 text-indigo-800",
            CAB_POOL: "bg-sky-100 text-sky-800",
            MEAL_CIRCLE: "bg-amber-100 text-amber-800",
            RENTAL: "bg-emerald-100 text-emerald-800",
            LOST_FOUND: "bg-teal-100 text-teal-800",
            DISPUTE: "bg-rose-100 text-rose-800",
            FEEDBACK: "bg-blue-100 text-blue-800",
            WALLET: "bg-emerald-100 text-emerald-800",
            RIDER: "bg-sky-100 text-sky-800",
            MODERATION: "bg-rose-100 text-rose-800"
        };

        const newEntry = {
            id: activity.id || `ACT-${Date.now() % 1000000}`,
            timestamp: formattedTime,
            rawTimestamp: rawTime,
            userEmail: userEmail,
            userName: activity.userName || "Campus Student",
            userRole: activity.userRole || "Customer",
            section: (activity.section || "ORDERS").toUpperCase(),
            action: activity.action || "ACTIVITY_RECORDED",
            title: activity.title || "Customer Action",
            details: activity.details || "Details recorded",
            status: activity.status || "COMPLETED",
            badgeColor: activity.badgeColor || badgeColors[(activity.section || "ORDERS").toUpperCase()] || "bg-purple-100 text-purple-800",
            amount: activity.amount !== undefined ? activity.amount : null
        };

        this.customerActivities.unshift(newEntry);
        if (this.customerActivities.length > 500) {
            this.customerActivities.length = 500;
        }
        this.save("campus_customer_activities", this.customerActivities);

        this.updateOverviewActivities();

        if (this.selectedTab === 8 && this.auditSubTab === 2) {
            this.updateSectionGrid("customerActivities");
        }

        if (this.activeDossierEmail && this.activeDossierEmail.toLowerCase() === userEmail) {
            const profile = this.getUserCompleteProfileAndActivities(this.activeDossierEmail);
            if (profile) {
                const body = document.getElementById("dossier-subtab-body");
                if (body) {
                    body.innerHTML = this.renderDossierSubTabContent(profile, this.activeDossierSubTab);
                    if (window.lucide) lucide.createIcons();
                }
            }
        }
    }

    // ==========================================
    // OVERVIEW LIVE ACTIVITIES WIDGET METHODS
    // ==========================================
    setOverviewActivitiesFilter(filter) {
        this.customerActivitiesFilter = filter;
        this.updateOverviewActivities();
    }

    updateOverviewActivities() {
        const container = document.getElementById("overview-activities-container");
        if (container) {
            container.innerHTML = this.renderOverviewActivitiesList();
            if (window.lucide) lucide.createIcons();
        }
    }

    renderOverviewActivitiesList() {
        const secFilter = (this.customerActivitiesFilter || "ALL").toUpperCase();
        const list = this.customerActivities.filter(a => secFilter === "ALL" || (a.section || "").toUpperCase() === secFilter);

        if (list.length === 0) {
            return `
                <div class="py-8 text-center text-slate-400 bg-slate-50 rounded-xl border border-slate-100">
                    <i data-lucide="activity" class="w-8 h-8 mx-auto mb-1 opacity-40"></i>
                    <p class="text-xs">No customer activities in this section.</p>
                </div>
            `;
        }

        const sectionIcons = {
            ORDERS: "shopping-bag",
            COURIER: "package",
            CAB_POOL: "car",
            MEAL_CIRCLE: "utensils",
            RENTAL: "tag",
            LOST_FOUND: "search",
            DISPUTE: "shield-alert",
            FEEDBACK: "message-square",
            WALLET: "credit-card",
            MODERATION: "shield-check",
            RIDER: "bike"
        };

        return list.slice(0, 15).map(act => {
            const iconName = sectionIcons[act.section] || "activity";
            const badgeClass = act.badgeColor || "bg-purple-100 text-purple-800";

            return `
                <div class="p-3 rounded-xl bg-slate-50 hover:bg-slate-100/80 border border-slate-100 transition flex items-start justify-between gap-3 text-xs">
                    <div class="flex items-start gap-2.5 min-w-0">
                        <div class="w-8 h-8 rounded-lg bg-white border border-slate-200 text-purple-700 flex items-center justify-center shrink-0 shadow-2xs mt-0.5">
                            <i data-lucide="${iconName}" class="w-4 h-4"></i>
                        </div>
                        <div class="min-w-0">
                            <div class="flex flex-wrap items-center gap-1.5">
                                <span class="font-bold text-slate-900 cursor-pointer hover:text-purple-700 hover:underline" onclick="app.openUserActivityDossier('${act.userEmail}')">
                                    ${act.userName || 'Student'}
                                </span>
                                <span class="text-[10px] px-1.5 py-0.2 rounded-md bg-purple-50 text-purple-700 font-semibold border border-purple-100">
                                    ${act.userRole || 'Customer'}
                                </span>
                                <span class="text-[10px] text-slate-400 font-medium">
                                    • ${act.timestamp || 'Today'}
                                </span>
                            </div>
                            <div class="font-semibold text-slate-800 mt-0.5 truncate">${act.title}</div>
                            <div class="text-[11px] text-slate-500 mt-0.5 leading-snug line-clamp-1">${act.details}</div>
                        </div>
                    </div>

                    <div class="flex flex-col items-end gap-1.5 shrink-0">
                        <span class="pill-status ${badgeClass} text-[10px] font-bold">
                            ${act.status || act.action}
                        </span>
                        <button onclick="app.openUserActivityDossier('${act.userEmail}')" class="px-2 py-0.5 rounded-lg bg-white border border-slate-200 hover:border-purple-300 text-purple-700 font-bold text-[10px] transition shadow-2xs flex items-center gap-1">
                            <i data-lucide="user" class="w-3 h-3"></i>
                            <span>Dossier</span>
                        </button>
                    </div>
                </div>
            `;
        }).join("");
    }

    // ==========================================
    // AUDIT SUB-TAB 2: CUSTOMER ACTIVITIES STREAM
    // ==========================================
    setCustomerActivitiesFilter(filter) {
        this.customerActivitiesFilter = filter;
        this.renderCurrentTab();
    }

    getFilteredCustomerActivities() {
        const q = (this.searchQueries.customerActivities || "").toLowerCase().trim();
        const secFilter = (this.customerActivitiesFilter || "ALL").toUpperCase();

        return this.customerActivities.filter(act => {
            if (secFilter !== "ALL" && (act.section || "").toUpperCase() !== secFilter) return false;

            if (!q) return true;
            const haystack = [
                act.id,
                act.userName,
                act.userEmail,
                act.userRole,
                act.section,
                act.action,
                act.title,
                act.details,
                act.status,
                act.timestamp
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });
    }

    renderCustomerActivitiesSection() {
        const filtered = this.getFilteredCustomerActivities();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h3 class="text-lg font-extrabold text-slate-900">Customer & Student Activities Stream</h3>
                        <p class="text-xs text-slate-500 mt-0.5">Chronological record of every action performed and saved by students and customers across all modules.</p>
                    </div>

                    <div class="flex items-center gap-2 self-start sm:self-auto">
                        <button onclick="app.downloadCustomerActivitiesCsv()" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs flex items-center gap-2 transition shadow-sm">
                            <i data-lucide="cloud-download" class="w-4 h-4"></i>
                            <span>Download Activities CSV</span>
                        </button>
                        <span id="customer-activities-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800">
                            ${filtered.length} Activities Listed
                        </span>
                    </div>
                </div>

                <!-- Section Filter Pills with Reset -->
                <div class="bg-white rounded-2xl p-4 border border-slate-200/90 shadow-sm space-y-3">
                    <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                        <div class="flex flex-wrap items-center gap-1.5">
                            <span class="text-[10px] font-bold text-slate-400 uppercase mr-1">Filter by Module:</span>
                            ${['ALL', 'ORDERS', 'COURIER', 'CAB_POOL', 'MEAL_CIRCLE', 'RENTAL', 'LOST_FOUND', 'DISPUTE', 'FEEDBACK', 'WALLET', 'RIDER'].map(sec => `
                                <button onclick="app.setCustomerActivitiesFilter('${sec}')" class="px-3 py-1 rounded-xl text-xs transition ${this.customerActivitiesFilter === sec ? 'bg-purple-700 text-white font-black font-extrabold uppercase shadow-md ring-2 ring-purple-300' : 'bg-slate-100 text-slate-600 font-normal hover:bg-slate-200'}">
                                    ${sec.replace(/_/g, ' ')}
                                </button>
                            `).join("")}
                        </div>
                        <button onclick="app.resetFilters('customerActivities')" class="py-1.5 px-3 rounded-xl border border-slate-200 hover:bg-slate-100 text-slate-600 font-bold text-xs transition flex items-center justify-center gap-1.5 self-start sm:self-auto">
                            <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                            <span>Reset Filters</span>
                        </button>
                    </div>
                </div>

                <!-- Search Box -->
                ${this.renderSearchBox("customerActivities", "Search by User Name, Email, Section, Action, Title, Details, or Status...")}

                <!-- Customer Activities Cards -->
                <div id="customer-activities-grid" class="space-y-3">
                    ${this.renderCustomerActivitiesCards(filtered)}
                </div>
            </div>
        `;
    }

    renderCustomerActivitiesCards(activitiesList) {
        if (activitiesList.length === 0) {
            return `
                <div class="py-16 text-center text-slate-400 bg-white rounded-2xl border border-slate-200">
                    <i data-lucide="zap" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                    <p class="text-xs">No customer activities matching search or filter criteria.</p>
                </div>
            `;
        }

        const sectionIcons = {
            ORDERS: "shopping-bag",
            COURIER: "package",
            CAB_POOL: "car",
            MEAL_CIRCLE: "utensils",
            RENTAL: "tag",
            LOST_FOUND: "search",
            DISPUTE: "shield-alert",
            FEEDBACK: "message-square",
            WALLET: "credit-card",
            MODERATION: "shield-check",
            RIDER: "bike"
        };

        return activitiesList.map(act => {
            const iconName = sectionIcons[act.section] || "activity";
            const badgeClass = act.badgeColor || "bg-purple-100 text-purple-800";

            return `
                <div class="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/90 shadow-sm card-interactive text-xs flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div class="flex items-start gap-3 min-w-0">
                        <div class="w-10 h-10 rounded-xl bg-purple-50 text-purple-700 border border-purple-100 flex items-center justify-center shrink-0 shadow-2xs mt-0.5">
                            <i data-lucide="${iconName}" class="w-5 h-5"></i>
                        </div>
                        <div class="min-w-0 space-y-1">
                            <div class="flex flex-wrap items-center gap-2">
                                <span class="font-extrabold text-sm text-slate-900 cursor-pointer hover:text-purple-700" onclick="app.openUserActivityDossier('${act.userEmail}')">
                                    ${act.userName}
                                </span>
                                <span class="text-purple-700 font-semibold font-mono text-[11px]">${act.userEmail}</span>
                                <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-700">
                                    ${act.section}
                                </span>
                                <span class="text-[11px] text-slate-400 font-medium">
                                    ${act.timestamp}
                                </span>
                            </div>
                            <div class="font-bold text-slate-800 text-xs">${act.title}</div>
                            <div class="text-slate-500 text-xs">${act.details}</div>
                        </div>
                    </div>

                    <div class="flex sm:flex-col items-center sm:items-end justify-between sm:justify-center gap-2 shrink-0 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                        <span class="pill-status ${badgeClass} font-black text-xs">
                            ${act.status || act.action}
                        </span>
                        <button onclick="app.openUserActivityDossier('${act.userEmail}')" class="px-3 py-1.5 rounded-xl bg-purple-50 hover:bg-purple-100 text-purple-800 font-bold border border-purple-200 transition flex items-center gap-1.5 shadow-2xs">
                            <i data-lucide="file-text" class="w-3.5 h-3.5"></i>
                            <span>360° Dossier</span>
                        </button>
                    </div>
                </div>
            `;
        }).join("");
    }

    downloadCustomerActivitiesCsv() {
        const list = this.getFilteredCustomerActivities();
        if (!list || !list.length) {
            this.showToast("No activities to export.", "warning");
            return;
        }
        const headers = ["Activity ID", "Timestamp", "User Email", "User Name", "Role", "Section", "Action", "Title", "Details", "Status"];
        const rows = list.map(a => [
            `"${a.id || ''}"`,
            `"${a.timestamp || ''}"`,
            `"${a.userEmail || ''}"`,
            `"${(a.userName || '').replace(/"/g, '""')}"`,
            `"${a.userRole || ''}"`,
            `"${a.section || ''}"`,
            `"${a.action || ''}"`,
            `"${(a.title || '').replace(/"/g, '""')}"`,
            `"${(a.details || '').replace(/"/g, '""')}"`,
            `"${a.status || ''}"`
        ]);
        const csvContent = "data:text/csv;charset=utf-8," + [headers.join(","), ...rows.map(r => r.join(","))].join("\n");
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", `customer_activities_${this.getTodayDateString()}.csv`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        this.showToast(`Exported ${list.length} activities to CSV!`);
    }

    // ==========================================
    // 360° UNIFIED CUSTOMER & RIDER DOSSIER ENGINE
    // ==========================================
    getUserCompleteProfileAndActivities(email) {
        if (!email) return null;
        const cleanEmail = email.toLowerCase().trim();

        const student = this.students.find(s => (s.email || "").toLowerCase() === cleanEmail);
        const rider = this.riders.find(r => (r.email || "").toLowerCase() === cleanEmail);

        const name = (student && student.name) || (rider && rider.name) || cleanEmail.split("@")[0].replace(/[._]/g, " ").replace(/\b\w/g, l => l.toUpperCase());
        const phone = (student && student.phone) || (rider && rider.phone) || "+91 9876543210";
        const rollNo = (student && student.rollNo) || (rider && rider.rollNo) || "23CS101";
        const hostel = (student && student.hostel) || "Hostel 3";
        const stream = (student && student.stream) || (rider && rider.department) || "Computer Science";

        const isStudent = !!student || !rider;
        const isRider = !!rider || (student && student.riderStatus && student.riderStatus !== "NONE");
        const primaryRole = (isStudent && isRider) ? "Customer & Rider" : (isRider ? "Rider" : "Customer / Student");

        const walletBalance = (student ? student.walletBalance : (rider ? rider.walletBalance : 100));

        let activities = this.customerActivities.filter(a => (a.userEmail || "").toLowerCase() === cleanEmail);

        const customerOrders = this.orders.filter(o => (o.customerId || "").toLowerCase() === cleanEmail);

        const runnerOrders = this.orders.filter(o => 
            (o.runnerEmail && o.runnerEmail.toLowerCase() === cleanEmail) || 
            (rider && o.runnerName && o.runnerName.toLowerCase() === rider.name.toLowerCase()) ||
            (student && o.runnerName && o.runnerName.toLowerCase() === student.name.toLowerCase())
        );

        const isCourierOrder = (o) => {
            if (!o) return false;
            const id = (o.id || "").toUpperCase();
            if (id.startsWith("ORDERNO-") || id.startsWith("CSV-") || id.startsWith("TRN-") || id.startsWith("TRANSFER-")) return true;
            if (o.transferMode || o.serviceType === "TRANSFER") return true;
            if (Array.isArray(o.items) && o.items.some(it => {
                const n = (typeof it === 'string' ? it : (it.name || it.first || "")).toLowerCase();
                return n.includes("transfer") || n.includes("courier") || n.includes("package") || n.includes("send:") || n.includes("bring:");
            })) return true;
            return false;
        };

        const customerStoreOrders = customerOrders.filter(o => !isCourierOrder(o));
        const customerCouriers = customerOrders.filter(o => isCourierOrder(o));
        const runnerStoreOrders = runnerOrders.filter(o => !isCourierOrder(o));
        const runnerCouriers = runnerOrders.filter(o => isCourierOrder(o));

        const cabPools = this.cabPools.filter(c => 
            (c.hostEmail || "").toLowerCase() === cleanEmail ||
            (c.participants || []).some(p => (p.email || p.id || "").toLowerCase() === cleanEmail)
        );

        const mealCircles = this.mealCircles.filter(m => 
            (m.hostEmail || "").toLowerCase() === cleanEmail ||
            (m.participants || []).some(p => (p.email || p.id || "").toLowerCase() === cleanEmail)
        );

        const rentals = this.rentals.filter(r => 
            (r.renterEmail || "").toLowerCase() === cleanEmail ||
            (r.ownerEmail || "").toLowerCase() === cleanEmail
        );

        const lostFound = this.lostFound.filter(l => (l.reporterEmail || "").toLowerCase() === cleanEmail);

        const disputes = this.disputes.filter(d => 
            (d.complainantEmail || "").toLowerCase() === cleanEmail ||
            (d.respondentEmail || "").toLowerCase() === cleanEmail ||
            (d.targetUserEmail || "").toLowerCase() === cleanEmail
        );

        const feedback = this.feedback.filter(f => (f.userEmail || "").toLowerCase() === cleanEmail);

        if (activities.length === 0) {
            customerOrders.forEach(o => {
                activities.push({
                    id: `ACT-SYN-${o.id}`,
                    timestamp: o.timestamp || "Past Activity",
                    rawTimestamp: Date.now() - 3600000,
                    userEmail: cleanEmail,
                    userName: name,
                    userRole: "Customer",
                    section: "ORDERS",
                    action: o.status === "DELIVERED" ? "ORDER_DELIVERED" : (o.status === "CANCELLED" ? "ORDER_CANCELLED" : "ORDER_PLACED"),
                    title: `Store Order: ${o.id}`,
                    details: `${(o.items || []).map(it => `${it.name} x${it.qty}`).join(", ") || 'Items'} | Drop: ${o.dropPoint}`,
                    status: o.status,
                    badgeColor: o.status === "CANCELLED" ? "bg-rose-100 text-rose-800" : "bg-purple-100 text-purple-800"
                });
            });

            runnerOrders.forEach(r => {
                activities.push({
                    id: `ACT-SYN-RUN-${r.id}`,
                    timestamp: r.timestamp || "Past Delivery",
                    rawTimestamp: Date.now() - 3600000,
                    userEmail: cleanEmail,
                    userName: name,
                    userRole: "Rider",
                    section: "RIDER",
                    action: "DELIVERY_COMPLETED",
                    title: `Courier Run: ${r.id}`,
                    details: `Delivered to ${r.dropPoint} | Earned: Rs. ${r.deliveryFee}`,
                    status: r.status,
                    badgeColor: "bg-emerald-100 text-emerald-800"
                });
            });

            cabPools.forEach(c => {
                activities.push({
                    id: `ACT-SYN-CAB-${c.id}`,
                    timestamp: `${c.departureDate}, ${c.departureTime}`,
                    rawTimestamp: Date.now() - 7200000,
                    userEmail: cleanEmail,
                    userName: name,
                    userRole: "Customer",
                    section: "CAB_POOL",
                    action: "CAB_POOL_HOSTED",
                    title: `Cab Pool: ${c.pickupPoint} ➔ ${c.dropDestination}`,
                    details: `Seats: ${c.bookedSeats}/${c.totalSeats} Booked | Fare: Rs. ${c.perPersonFare}/seat`,
                    status: c.status,
                    badgeColor: "bg-sky-100 text-sky-800"
                });
            });

            mealCircles.forEach(m => {
                activities.push({
                    id: `ACT-SYN-MEAL-${m.id}`,
                    timestamp: m.targetTime || "Today",
                    rawTimestamp: Date.now() - 5400000,
                    userEmail: cleanEmail,
                    userName: name,
                    userRole: "Customer",
                    section: "MEAL_CIRCLE",
                    action: "MEAL_GATHERING_HOSTED",
                    title: `Meal Gathering: ${m.restaurantSpot}`,
                    details: `Buddies: ${m.joinedBuddies}/${m.totalBuddies} Joined | Meet: ${m.meetingPoint} @ ${m.targetTime}`,
                    status: m.status,
                    badgeColor: "bg-amber-100 text-amber-800"
                });
            });

            rentals.forEach(rnt => {
                activities.push({
                    id: `ACT-SYN-RNT-${rnt.id}`,
                    timestamp: "Past Week",
                    rawTimestamp: Date.now() - 86400000,
                    userEmail: cleanEmail,
                    userName: name,
                    userRole: "Customer",
                    section: "RENTAL",
                    action: "ITEM_RENTED",
                    title: `Rented: ${rnt.productName}`,
                    details: `Duration: ${rnt.durationDays} Days | Total: Rs. ${rnt.totalPaid} | Handover OTP: ${rnt.handoverOtp}`,
                    status: rnt.status,
                    badgeColor: "bg-emerald-100 text-emerald-800"
                });
            });

            lostFound.forEach(l => {
                activities.push({
                    id: `ACT-SYN-LNF-${l.id}`,
                    timestamp: l.date || "Past Notice",
                    rawTimestamp: Date.now() - 10000000,
                    userEmail: cleanEmail,
                    userName: name,
                    userRole: "Customer",
                    section: "LOST_FOUND",
                    action: l.type === "LOST" ? "LOST_REPORTED" : "FOUND_REPORTED",
                    title: `${l.type} Item: ${l.title}`,
                    details: `${l.description} | Location: ${l.location}`,
                    status: l.status,
                    badgeColor: "bg-amber-100 text-amber-800"
                });
            });
        }

        return {
            email: cleanEmail,
            name: name,
            phone: phone,
            rollNo: rollNo,
            hostel: hostel,
            stream: stream,
            isStudent: isStudent,
            isRider: isRider,
            primaryRole: primaryRole,
            student: student,
            rider: rider,
            walletBalance: walletBalance,
            activities: activities,
            customerOrders: customerOrders,
            runnerOrders: runnerOrders,
            customerStoreOrders: customerStoreOrders,
            customerCouriers: customerCouriers,
            runnerStoreOrders: runnerStoreOrders,
            runnerCouriers: runnerCouriers,
            cabPools: cabPools,
            mealCircles: mealCircles,
            rentals: rentals,
            lostFound: lostFound,
            disputes: disputes,
            feedback: feedback
        };
    }

    openRiderActivityDossier(email, subTabIdx = 0) {
        this.openUserActivityDossier(email, subTabIdx);
    }

    openUserActivityDossier(email, subTabIdx = 0) {
        this.activeDossierEmail = email;
        this.activeDossierSubTab = subTabIdx;
        const profile = this.getUserCompleteProfileAndActivities(email);
        if (!profile) {
            this.showToast("User record not found for " + email, "error");
            return;
        }

        const modal = document.getElementById("modal-generic");
        if (!modal) return;

        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-2 sm:p-4 z-50 overflow-y-auto">
                <div class="modal-content bg-white w-full max-w-5xl rounded-3xl p-5 sm:p-6 shadow-2xl border border-slate-200 max-h-[92vh] flex flex-col my-auto">
                    <!-- Dossier Header -->
                    <div class="flex items-start justify-between pb-4 border-b border-slate-100 shrink-0">
                        <div class="flex items-center gap-3.5">
                            <div class="w-12 h-12 rounded-2xl bg-gradient-to-br from-purple-700 to-indigo-800 text-white flex items-center justify-center font-black text-lg shadow-md shadow-purple-900/20 shrink-0">
                                ${profile.name.charAt(0)}
                            </div>
                            <div>
                                <div class="flex flex-wrap items-center gap-2">
                                    <h3 class="text-base sm:text-lg font-black text-slate-900">${profile.name}</h3>
                                    <span class="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wide ${
                                        profile.primaryRole.includes('&') ? 'bg-purple-100 text-purple-900 border border-purple-200' :
                                        profile.isRider ? 'bg-sky-100 text-sky-900 border border-sky-200' : 'bg-emerald-100 text-emerald-900 border border-emerald-200'
                                    }">
                                        ${profile.primaryRole}
                                    </span>
                                    ${profile.student && profile.student.studentStatus === 'TEMP_BLOCKED' ? `
                                        <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800 border border-rose-200">
                                            Student Locked (12h)
                                        </span>
                                    ` : ''}
                                    ${profile.rider && profile.rider.isOnline ? `
                                        <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                                            Rider Online
                                        </span>
                                    ` : ''}
                                </div>
                                <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500 mt-0.5 font-medium">
                                    <span class="text-purple-700 font-semibold font-mono">${profile.email}</span>
                                    <span>•</span>
                                    <span>${profile.phone}</span>
                                    <span>•</span>
                                    <span>Roll: <strong class="text-slate-700">${profile.rollNo}</strong></span>
                                    <span>•</span>
                                    <span>Hostel: <strong class="text-slate-700">${profile.hostel}</strong></span>
                                    <span>•</span>
                                    <span>Stream: <strong class="text-slate-700">${profile.stream}</strong></span>
                                </div>
                            </div>
                        </div>
                        <div class="flex items-center gap-2 shrink-0">
                            <button onclick="app.openEditAccountModal('${profile.email}')" class="px-3 py-1.5 rounded-xl bg-purple-100 hover:bg-purple-200 text-purple-900 font-extrabold text-xs transition flex items-center gap-1.5 border border-purple-200 shadow-2xs">
                                <i data-lucide="edit-3" class="w-3.5 h-3.5 text-purple-700"></i>
                                <span>Edit Account</span>
                            </button>
                            <button onclick="app.closeModal()" class="text-slate-400 hover:text-slate-700 font-black text-2xl p-1 leading-none shrink-0">&times;</button>
                        </div>
                    </div>

                    <!-- 11 Sub-Tab Navigation Bar (Separated Orders & Couriers) -->
                    <div class="flex items-center gap-1.5 overflow-x-auto py-2.5 border-b border-slate-100 shrink-0 text-xs font-bold scrollbar-thin custom-scrollbar-light">
                        ${[
                            { id: 0, label: "Unified Timeline", icon: "activity", count: profile.activities.length },
                            { id: 1, label: "Store Orders", icon: "shopping-bag", count: profile.customerStoreOrders.length + profile.runnerStoreOrders.length },
                            { id: 2, label: "Courier Transfers", icon: "package", count: profile.customerCouriers.length + profile.runnerCouriers.length },
                            { id: 3, label: "Cab Pools", icon: "car", count: profile.cabPools.length },
                            { id: 4, label: "Meal Circles", icon: "utensils", count: profile.mealCircles.length },
                            { id: 5, label: "Rentals", icon: "tag", count: profile.rentals.length },
                            { id: 6, label: "Lost & Found", icon: "search", count: profile.lostFound.length },
                            { id: 7, label: "Disputes", icon: "shield-alert", count: profile.disputes.length },
                            { id: 8, label: "Feedback", icon: "message-square", count: profile.feedback.length },
                            { id: 9, label: "Wallet Ledger", icon: "credit-card", count: `Rs.${profile.walletBalance}` },
                            { id: 10, label: "Roles & Controls", icon: "sliders", count: null }
                        ].map(tab => `
                            <button onclick="app.setDossierSubTab(${tab.id})" class="dossier-tab-btn px-3 py-1.5 rounded-xl whitespace-nowrap transition flex items-center gap-1.5 ${
                                this.activeDossierSubTab === tab.id
                                    ? 'bg-purple-700 text-white font-extrabold shadow-sm'
                                    : 'text-slate-600 hover:bg-slate-100 font-semibold'
                            }">
                                <i data-lucide="${tab.icon}" class="w-3.5 h-3.5"></i>
                                <span>${tab.label}</span>
                                ${tab.count !== null ? `<span class="text-[10px] px-1.5 py-0.2 rounded-full ${this.activeDossierSubTab === tab.id ? 'bg-purple-900 text-purple-200' : 'bg-slate-200 text-slate-700'}">${tab.count}</span>` : ''}
                            </button>
                        `).join("")}
                    </div>

                    <!-- Dynamic Dossier Sub-Tab View Container -->
                    <div id="dossier-subtab-body" class="flex-1 overflow-y-auto pt-4 pr-1 text-xs">
                        ${this.renderDossierSubTabContent(profile, this.activeDossierSubTab)}
                    </div>

                    <!-- Footer -->
                    <div class="pt-3 border-t border-slate-100 flex items-center justify-between shrink-0 text-xs">
                        <span class="text-slate-400 font-medium">360° Real-Time Customer Dossier • Synchronized across all modules</span>
                        <button onclick="app.closeModal()" class="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold transition">Close Dossier</button>
                    </div>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) lucide.createIcons();
    }

    setDossierSubTab(subTabIdx) {
        this.activeDossierSubTab = subTabIdx;
        const profile = this.getUserCompleteProfileAndActivities(this.activeDossierEmail);
        if (!profile) return;

        const btns = document.querySelectorAll(".dossier-tab-btn");
        btns.forEach((btn, idx) => {
            if (idx === subTabIdx) {
                btn.className = "dossier-tab-btn px-3 py-1.5 rounded-xl whitespace-nowrap transition flex items-center gap-1.5 bg-purple-700 text-white font-extrabold shadow-sm";
                try {
                    btn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
                } catch (_) {}
            } else {
                btn.className = "dossier-tab-btn px-3 py-1.5 rounded-xl whitespace-nowrap transition flex items-center gap-1.5 text-slate-600 hover:bg-slate-100 font-semibold";
            }
        });

        const body = document.getElementById("dossier-subtab-body");
        if (body) {
            body.innerHTML = this.renderDossierSubTabContent(profile, subTabIdx);
            if (window.lucide) lucide.createIcons();
        }
    }

    renderDossierSubTabContent(profile, subTabIdx) {
        switch (subTabIdx) {
            case 0: {
                if (profile.activities.length === 0) {
                    return `
                        <div class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
                            <i data-lucide="activity" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                            <p class="text-xs">No activity records logged for this user yet.</p>
                        </div>
                    `;
                }
                return `
                    <div class="space-y-3">
                        <div class="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Chronological Activity Stream (Past & Present)</div>
                        ${profile.activities.map(act => `
                            <div class="p-3.5 rounded-2xl bg-slate-50 border border-slate-200/80 hover:bg-white transition flex items-start justify-between gap-3">
                                <div class="space-y-1 min-w-0">
                                    <div class="flex flex-wrap items-center gap-2">
                                        <span class="pill-status ${act.badgeColor || 'bg-purple-100 text-purple-800'} text-[10px]">
                                            ${act.section}
                                        </span>
                                        <span class="font-extrabold text-slate-900 text-xs">${act.title}</span>
                                        <span class="text-[10px] text-slate-400 font-medium">• ${act.timestamp}</span>
                                    </div>
                                    <div class="text-slate-600 text-xs">${act.details}</div>
                                </div>
                                <span class="pill-status ${act.badgeColor || 'bg-slate-100 text-slate-700'} text-[10px] font-bold shrink-0">
                                    ${act.status || act.action}
                                </span>
                            </div>
                        `).join("")}
                    </div>
                `;
            }

            case 1: {
                const totalStore = profile.customerStoreOrders.length + profile.runnerStoreOrders.length;
                if (totalStore === 0) {
                    return `
                        <div class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
                            <i data-lucide="shopping-bag" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                            <p class="text-xs">No store or canteen orders found for this user.</p>
                        </div>
                    `;
                }
                return `
                    <div class="space-y-4">
                        ${profile.customerStoreOrders.length > 0 ? `
                            <div class="space-y-2.5">
                                <h4 class="text-xs font-black text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                                    <i data-lucide="shopping-bag" class="w-4 h-4 text-purple-700"></i>
                                    <span>Placed Store & Canteen Orders (${profile.customerStoreOrders.length})</span>
                                </h4>
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                                    ${profile.customerStoreOrders.map(o => `
                                        <div class="p-3.5 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-2">
                                            <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                                <span class="font-black text-purple-900">${o.id}</span>
                                                <span class="pill-status ${o.status === 'DELIVERED' ? 'bg-emerald-100 text-emerald-800' : (o.status === 'CANCELLED' ? 'bg-rose-100 text-rose-800' : 'bg-sky-100 text-sky-800')}">${o.status}</span>
                                            </div>
                                            <div class="text-slate-600 text-xs">
                                                <div><strong>Drop:</strong> ${o.dropPoint}</div>
                                                <div><strong>Pick:</strong> ${o.pickPoint}</div>
                                                ${o.items && o.items.length ? `<div><strong>Items:</strong> ${o.items.map(it => `${it.name || it.first || ''} (x${it.qty || it.second || 1})`).join(', ')}</div>` : ''}
                                                <div class="text-emerald-700 font-bold mt-1">Bill: Rs. ${o.actualBill || o.totalMax} | Delivery Fee: Rs. ${o.deliveryFee}</div>
                                                <div class="text-purple-700 font-mono text-[11px]">Delivery OTP: ${o.deliveryOtp}</div>
                                            </div>
                                        </div>
                                    `).join("")}
                                </div>
                            </div>
                        ` : ''}

                        ${profile.runnerStoreOrders.length > 0 ? `
                            <div class="space-y-2.5 pt-2">
                                <h4 class="text-xs font-black text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                                    <i data-lucide="bike" class="w-4 h-4 text-emerald-600"></i>
                                    <span>Fulfilled Store Deliveries as Runner (${profile.runnerStoreOrders.length})</span>
                                </h4>
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                                    ${profile.runnerStoreOrders.map(r => `
                                        <div class="p-3.5 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-2">
                                            <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                                <span class="font-black text-emerald-900">${r.id}</span>
                                                <span class="pill-status ${r.status === 'DELIVERED' ? 'bg-emerald-100 text-emerald-800' : 'bg-sky-100 text-sky-800'}">${r.status}</span>
                                            </div>
                                            <div class="text-slate-600 text-xs">
                                                <div><strong>Customer:</strong> ${r.customerName} (${r.customerId})</div>
                                                <div><strong>Route:</strong> ${r.pickPoint} â†’ ${r.dropPoint}</div>
                                                <div class="text-emerald-700 font-bold mt-1">Runner Fee Earned: Rs. ${r.deliveryFee}</div>
                                            </div>
                                        </div>
                                    `).join("")}
                                </div>
                            </div>
                        ` : ''}
                    </div>
                `;
            }

            case 2: {
                const totalCouriers = profile.customerCouriers.length + profile.runnerCouriers.length;
                if (totalCouriers === 0) {
                    return `
                        <div class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
                            <i data-lucide="package" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                            <p class="text-xs">No peer-to-peer courier transfers (Send/Bring items) found for this user.</p>
                        </div>
                    `;
                }
                return `
                    <div class="space-y-4">
                        ${profile.customerCouriers.length > 0 ? `
                            <div class="space-y-2.5">
                                <h4 class="text-xs font-black text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                                    <i data-lucide="package" class="w-4 h-4 text-indigo-700"></i>
                                    <span>Requested Courier Transfers (${profile.customerCouriers.length})</span>
                                </h4>
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                                    ${profile.customerCouriers.map(o => `
                                        <div class="p-3.5 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-2">
                                            <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                                <span class="font-black text-indigo-900">${o.id}</span>
                                                <span class="pill-status ${o.status === 'DELIVERED' ? 'bg-emerald-100 text-emerald-800' : (o.status === 'CANCELLED' ? 'bg-rose-100 text-rose-800' : 'bg-sky-100 text-sky-800')}">${o.status}</span>
                                            </div>
                                            <div class="text-slate-600 text-xs">
                                                <div><strong>Pickup Point:</strong> ${o.pickPoint}</div>
                                                <div><strong>Drop-off Point:</strong> ${o.dropPoint}</div>
                                                ${o.items && o.items.length ? `<div><strong>Item Details:</strong> ${o.items.map(it => `${it.name || it.first || ''}`).join(', ')}</div>` : ''}
                                                <div class="text-emerald-700 font-bold mt-1">Delivery Fee: Rs. ${o.deliveryFee}</div>
                                                <div class="text-purple-700 font-mono text-[11px]">Handover OTP: ${o.deliveryOtp}</div>
                                            </div>
                                        </div>
                                    `).join("")}
                                </div>
                            </div>
                        ` : ''}

                        ${profile.runnerCouriers.length > 0 ? `
                            <div class="space-y-2.5 pt-2">
                                <h4 class="text-xs font-black text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                                    <i data-lucide="bike" class="w-4 h-4 text-emerald-600"></i>
                                    <span>Fulfilled Courier Transfers as Runner (${profile.runnerCouriers.length})</span>
                                </h4>
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                                    ${profile.runnerCouriers.map(r => `
                                        <div class="p-3.5 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-2">
                                            <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                                <span class="font-black text-emerald-900">${r.id}</span>
                                                <span class="pill-status ${r.status === 'DELIVERED' ? 'bg-emerald-100 text-emerald-800' : 'bg-sky-100 text-sky-800'}">${r.status}</span>
                                            </div>
                                            <div class="text-slate-600 text-xs">
                                                <div><strong>Sender/Customer:</strong> ${r.customerName} (${r.customerId})</div>
                                                <div><strong>Route:</strong> ${r.pickPoint} â†’ ${r.dropPoint}</div>
                                                <div class="text-emerald-700 font-bold mt-1">Courier Fee Earned: Rs. ${r.deliveryFee}</div>
                                            </div>
                                        </div>
                                    `).join("")}
                                </div>
                            </div>
                        ` : ''}
                    </div>
                `;
            }

            case 3: {
                if (profile.cabPools.length === 0) {
                    return `
                        <div class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
                            <i data-lucide="car" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                            <p class="text-xs">No Cab Pool records associated with this student.</p>
                        </div>
                    `;
                }
                return `
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                        ${profile.cabPools.map(c => `
                            <div class="p-4 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-2 flex flex-col justify-between">
                                <div class="space-y-1.5">
                                    <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                        <span class="font-black text-sky-900">${c.id}</span>
                                        <span class="pill-status ${c.status === 'CANCELLED' ? 'bg-rose-100 text-rose-800 font-bold' : 'bg-sky-100 text-sky-800'}">${c.status}</span>
                                    </div>
                                    <div class="space-y-1 text-xs text-slate-600">
                                        <div class="font-bold text-slate-800">${c.pickupPoint} ➔ ${c.dropDestination}</div>
                                        <div><strong>Departure:</strong> ${c.departureDate} at ${c.departureTime}</div>
                                        <div><strong>Seats Booked:</strong> ${c.bookedSeats}/${c.totalSeats} | <strong>Fare:</strong> Rs. ${c.perPersonFare}/seat</div>
                                        ${c.isNightRide ? `<div class="text-amber-700 font-bold">🌙 Night Ride Protection Enabled</div>` : ''}
                                    </div>
                                </div>
                                <div class="pt-2 border-t border-slate-100 flex items-center justify-between">
                                    <span class="text-[11px] text-slate-400 font-medium">${(c.chatLogs || []).length} message${(c.chatLogs || []).length === 1 ? '' : 's'}</span>
                                    <button onclick="app.openSectionChatModal('cabs', '${c.id}')" class="px-2.5 py-1 rounded-lg border border-sky-200 bg-sky-50 hover:bg-sky-100 text-sky-800 font-bold text-[11px] transition flex items-center gap-1 shadow-2xs cursor-pointer">
                                        <i data-lucide="message-square" class="w-3 h-3 text-sky-700"></i>
                                        <span>Ride Chat (${(c.chatLogs || []).length})</span>
                                    </button>
                                </div>
                            </div>
                        `).join("")}
                    </div>
                `;
            }

            case 4: {
                if (profile.mealCircles.length === 0) {
                    return `
                        <div class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
                            <i data-lucide="utensils" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                            <p class="text-xs">No Meal Circles hosted or joined by this user.</p>
                        </div>
                    `;
                }
                return `
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                        ${profile.mealCircles.map(m => `
                            <div class="p-4 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-2 flex flex-col justify-between">
                                <div class="space-y-1.5">
                                    <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                        <span class="font-black text-amber-900">${m.id}: ${m.restaurantSpot}</span>
                                        <span class="pill-status ${m.status === 'CANCELLED' ? 'bg-rose-100 text-rose-800 font-bold' : 'bg-amber-100 text-amber-800'}">${m.status}</span>
                                    </div>
                                    <div class="space-y-1 text-xs text-slate-600">
                                        <div><strong>Category:</strong> ${m.mealCategory}</div>
                                        <div><strong>Meeting Spot:</strong> ${m.meetingPoint} @ ${m.targetTime}</div>
                                        <div><strong>Buddies:</strong> ${m.joinedBuddies}/${m.totalBuddies} Joined</div>
                                    </div>
                                </div>
                                <div class="pt-2 border-t border-slate-100 flex items-center justify-between">
                                    <span class="text-[11px] text-slate-400 font-medium">${(m.chatLogs || []).length} message${(m.chatLogs || []).length === 1 ? '' : 's'}</span>
                                    <button onclick="app.openSectionChatModal('meals', '${m.id}')" class="px-2.5 py-1 rounded-lg border border-amber-200 bg-amber-50 hover:bg-amber-100 text-amber-800 font-bold text-[11px] transition flex items-center gap-1 shadow-2xs cursor-pointer">
                                        <i data-lucide="message-square" class="w-3 h-3 text-amber-700"></i>
                                        <span>Meal Chat (${(m.chatLogs || []).length})</span>
                                    </button>
                                </div>
                            </div>
                        `).join("")}
                    </div>
                `;
            }

            case 5: {
                if (profile.rentals.length === 0) {
                    return `
                        <div class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
                            <i data-lucide="tag" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                            <p class="text-xs">No equipment rentals recorded for this user.</p>
                        </div>
                    `;
                }
                return `
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                        ${profile.rentals.map(rnt => `
                            <div class="p-4 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-2 flex flex-col justify-between">
                                <div class="space-y-1.5">
                                    <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                        <span class="font-black text-emerald-900">${rnt.id}: ${rnt.productName}</span>
                                        <span class="pill-status ${rnt.status === 'CANCELLED' ? 'bg-rose-100 text-rose-800 font-bold' : 'bg-emerald-100 text-emerald-800'}">${rnt.status}</span>
                                    </div>
                                    <div class="space-y-1 text-xs text-slate-600">
                                        <div><strong>Duration:</strong> ${rnt.durationDays} Days | <strong>Total Paid:</strong> Rs. ${rnt.totalPaid}</div>
                                        <div class="font-mono text-purple-700"><strong>Handover OTP:</strong> ${rnt.handoverOtp} | <strong>Return OTP:</strong> ${rnt.returnOtp}</div>
                                    </div>
                                </div>
                                <div class="pt-2 border-t border-slate-100 flex items-center justify-between">
                                    <span class="text-[11px] text-slate-400 font-medium">${(rnt.chatLogs || []).length} message${(rnt.chatLogs || []).length === 1 ? '' : 's'}</span>
                                    <button onclick="app.openSectionChatModal('rentals', '${rnt.id}')" class="px-2.5 py-1 rounded-lg border border-emerald-200 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 font-bold text-[11px] transition flex items-center gap-1 shadow-2xs cursor-pointer">
                                        <i data-lucide="message-square" class="w-3 h-3 text-emerald-700"></i>
                                        <span>Rental Chat (${(rnt.chatLogs || []).length})</span>
                                    </button>
                                </div>
                            </div>
                        `).join("")}
                    </div>
                `;
            }

            case 6: {
                if (profile.lostFound.length === 0) {
                    return `
                        <div class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
                            <i data-lucide="search" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                            <p class="text-xs">No Lost & Found notices posted by this user.</p>
                        </div>
                    `;
                }
                return `
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                        ${profile.lostFound.map(l => `
                            <div class="p-4 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-2 flex flex-col justify-between">
                                <div class="space-y-1.5">
                                    <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                        <span class="pill-status ${l.type === 'LOST' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'} font-black">${l.type}</span>
                                        <span class="pill-status bg-slate-100 text-slate-700">${l.status}</span>
                                    </div>
                                    <div class="font-bold text-slate-900 text-xs">${l.title}</div>
                                    <div class="text-slate-600 text-xs">${l.description}</div>
                                    <div class="text-[11px] text-slate-400">Location: ${l.location} • ${l.date}</div>
                                </div>
                                <div class="pt-2 border-t border-slate-100 flex items-center justify-between">
                                    <span class="text-[11px] text-slate-400 font-medium">${(l.chatLogs || []).length} message${(l.chatLogs || []).length === 1 ? '' : 's'}</span>
                                    <button onclick="app.openSectionChatModal('lostFound', '${l.id}')" class="px-2.5 py-1 rounded-lg border border-purple-200 bg-purple-50 hover:bg-purple-100 text-purple-800 font-bold text-[11px] transition flex items-center gap-1 shadow-2xs cursor-pointer">
                                        <i data-lucide="message-square" class="w-3 h-3 text-purple-700"></i>
                                        <span>Item Chat (${(l.chatLogs || []).length})</span>
                                    </button>
                                </div>
                            </div>
                        `).join("")}
                    </div>
                `;
            }

            case 7: {
                if (profile.disputes.length === 0) {
                    return `
                        <div class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
                            <i data-lucide="shield-check" class="w-10 h-10 mx-auto mb-2 opacity-50 text-emerald-500"></i>
                            <p class="text-xs">No dispute or fraud arbitration claims on record for this user.</p>
                        </div>
                    `;
                }
                return `
                    <div class="space-y-3">
                        ${profile.disputes.map(d => `
                            <div class="p-4 rounded-2xl bg-white border border-rose-200/90 shadow-2xs space-y-2">
                                <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                    <div>
                                        <span class="font-black text-rose-900">${d.id}</span>
                                        <span class="text-slate-400 text-xs ml-2">Order: ${d.orderId}</span>
                                    </div>
                                    <span class="pill-status ${d.status.startsWith('RESOLVED') ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}">${d.status}</span>
                                </div>
                                <div class="text-xs text-slate-700 space-y-1">
                                    <div><strong>Issue Category:</strong> ${d.issueCategory}</div>
                                    <div><strong>Statement:</strong> "${d.description}"</div>
                                    <div><strong>Parties:</strong> ${d.complainantName} vs ${d.respondentName} (${d.respondentRole})</div>
                                    <div class="text-sky-700 font-bold">Escrow Amount: Rs. ${d.escrowAmount}</div>
                                </div>
                            </div>
                        `).join("")}
                    </div>
                `;
            }

            case 8: {
                if (profile.feedback.length === 0) {
                    return `
                        <div class="py-12 text-center text-slate-400 bg-slate-50 rounded-2xl border border-slate-100">
                            <i data-lucide="message-square" class="w-10 h-10 mx-auto mb-2 opacity-50"></i>
                            <p class="text-xs">No user feedback or suggestions submitted yet.</p>
                        </div>
                    `;
                }
                return `
                    <div class="space-y-3">
                        ${profile.feedback.map(fb => `
                            <div class="p-4 rounded-2xl bg-white border border-slate-200/90 shadow-2xs space-y-2">
                                <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                                    <div class="flex items-center gap-2">
                                        <span class="text-amber-500 font-bold text-sm">★ ${fb.rating}/5</span>
                                        <span class="pill-status bg-purple-50 text-purple-700">${fb.category}</span>
                                    </div>
                                    <span class="text-[11px] text-slate-400 font-medium">${fb.timestamp}</span>
                                </div>
                                <div class="text-xs text-slate-700">"${fb.message}"</div>
                                ${fb.isAcceptedAsTask ? `<div class="text-[11px] text-purple-700 font-bold">Roadmap Status: ${fb.taskStatus}</div>` : ''}
                            </div>
                        `).join("")}
                    </div>
                `;
            }

            case 9: {
                return `
                    <div class="space-y-4">
                        <div class="p-5 rounded-2xl bg-gradient-to-r from-emerald-900 to-teal-900 text-white flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-md">
                            <div>
                                <div class="text-xs text-emerald-200 font-bold uppercase tracking-wider">Current Consolidated Wallet Balance</div>
                                <div class="text-3xl font-black mt-1">Rs. ${profile.walletBalance}</div>
                                <p class="text-[11px] text-emerald-200/80 mt-1">Real-time escrow & runner balance linked to student profile</p>
                            </div>
                            <div class="flex items-center gap-2">
                                <button onclick="app.quickAdjustWalletBalance('${profile.email}')" class="px-4 py-2.5 rounded-xl bg-white text-emerald-950 font-extrabold text-xs hover:bg-emerald-50 transition shadow-sm flex items-center gap-1.5">
                                    <i data-lucide="sliders" class="w-3.5 h-3.5"></i>
                                    <span>Adjust Balance</span>
                                </button>
                            </div>
                        </div>

                        <div class="space-y-2">
                            <h4 class="text-xs font-black text-slate-900 uppercase tracking-wider">Wallet Transaction & Activity History</h4>
                            ${profile.activities.filter(a => a.section === 'WALLET' || a.section === 'ORDERS' || a.section === 'RIDER' || a.action.includes('CANCELLED')).length > 0 ? 
                                profile.activities.filter(a => a.section === 'WALLET' || a.section === 'ORDERS' || a.section === 'RIDER' || a.action.includes('CANCELLED')).map(tx => `
                                    <div class="p-3 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-between gap-3 text-xs">
                                        <div>
                                            <div class="font-bold text-slate-800">${tx.title}</div>
                                            <div class="text-slate-500 text-[11px]">${tx.details}</div>
                                            <div class="text-[10px] text-slate-400 mt-0.5">${tx.timestamp}</div>
                                        </div>
                                        <span class="pill-status ${tx.badgeColor || 'bg-slate-200 text-slate-700'} font-black text-[11px]">
                                            ${tx.status}
                                        </span>
                                    </div>
                                `).join("") : `
                                    <div class="p-4 text-center text-slate-400 text-xs bg-slate-50 rounded-xl">
                                        No recent wallet transactions on record.
                                    </div>
                                `}
                        </div>
                    </div>
                `;
            }

            case 10: {
                const isStudentBlocked = profile.student && profile.student.studentStatus === 'TEMP_BLOCKED';
                const isRiderBlocked = profile.rider && profile.rider.riderStatus === 'TEMP_BLOCKED';
                const hasBlock = isStudentBlocked || isRiderBlocked;

                return `
                    <div class="space-y-5">
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <!-- Student Console Role -->
                            <div class="p-5 rounded-2xl bg-slate-50 border border-slate-200/90 space-y-3">
                                <div class="flex items-center justify-between pb-2 border-b border-slate-200/60">
                                    <h4 class="font-black text-slate-900 flex items-center gap-2">
                                        <i data-lucide="user" class="w-4 h-4 text-purple-700"></i>
                                        <span>Student Role Console</span>
                                    </h4>
                                    <span class="pill-status ${isStudentBlocked ? 'bg-rose-100 text-rose-800' : 'bg-emerald-100 text-emerald-800'}">
                                        ${profile.student ? profile.student.studentStatus : 'ACTIVE'}
                                    </span>
                                </div>
                                <div class="text-xs text-slate-600 space-y-1">
                                    <div>Roll No: <strong>${profile.rollNo}</strong> | Hostel: <strong>${profile.hostel}</strong></div>
                                    <div>Academic Stream: <strong>${profile.stream}</strong></div>
                                    ${isStudentBlocked ? `
                                        <div class="p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-[11px] font-bold">
                                            12-Hour Lockout Active: ${profile.student.studentBlockReason || 'Moderation Policy Violation'}
                                        </div>
                                    ` : ''}
                                </div>
                                <div class="pt-2 flex flex-wrap gap-2">
                                    ${!isStudentBlocked ? `
                                        <button onclick="app.blockDossierConsole('${profile.email}', 'STUDENT_ONLY')" class="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold transition text-xs">
                                            12h Block Student Only
                                        </button>
                                    ` : `
                                        <button onclick="app.unblockDossierStudent('${profile.email}')" class="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold transition text-xs">
                                            Lift Student Lock
                                        </button>
                                    `}
                                </div>
                            </div>

                            <!-- Rider Console Role -->
                            <div class="p-5 rounded-2xl bg-slate-50 border border-slate-200/90 space-y-3">
                                <div class="flex items-center justify-between pb-2 border-b border-slate-200/60">
                                    <h4 class="font-black text-slate-900 flex items-center gap-2">
                                        <i data-lucide="bike" class="w-4 h-4 text-sky-700"></i>
                                        <span>Rider Fleet Role Console</span>
                                    </h4>
                                    <span class="pill-status ${profile.isRider ? (isRiderBlocked ? 'bg-rose-100 text-rose-800' : 'bg-sky-100 text-sky-800') : 'bg-slate-200 text-slate-600'}">
                                        ${profile.isRider ? (profile.rider ? profile.rider.verificationStatus : 'REGISTERED') : 'NOT ENROLLED'}
                                    </span>
                                </div>
                                <div class="text-xs text-slate-600 space-y-1">
                                    <div>Duty Status: <strong>${profile.rider && profile.rider.isOnline ? '🟢 ONLINE (Ready for deliveries)' : '⚪ OFFLINE'}</strong></div>
                                    <div>Total Completed Runs: <strong>${profile.rider ? profile.rider.totalDeliveries : profile.runnerOrders.length}</strong></div>
                                    <div>Customer Rating: <strong>${profile.rider ? profile.rider.rating : '5.0 *'}</strong></div>
                                    ${isRiderBlocked ? `
                                        <div class="p-2.5 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-[11px] font-bold">
                                            12-Hour Rider Lockout Active
                                        </div>
                                    ` : ''}
                                </div>
                                <div class="pt-2 flex flex-wrap gap-2">
                                    ${profile.rider ? `
                                        <button onclick="app.toggleDossierRiderDuty('${profile.email}')" class="px-3 py-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-100 text-slate-700 font-bold transition text-xs">
                                            Toggle ${profile.rider.isOnline ? 'Offline' : 'Online'}
                                        </button>
                                        <button onclick="app.toggleDossierRiderVerification('${profile.email}')" class="px-3 py-1.5 rounded-xl ${profile.rider.verificationStatus === 'VERIFIED' ? 'bg-rose-600 text-white' : 'bg-emerald-600 text-white'} font-bold transition text-xs">
                                            ${profile.rider.verificationStatus === 'VERIFIED' ? 'Suspend Rider' : 'Verify Rider'}
                                        </button>
                                    ` : ''}
                                    ${!isRiderBlocked && profile.isRider ? `
                                        <button onclick="app.blockDossierConsole('${profile.email}', 'RIDER_ONLY')" class="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold transition text-xs">
                                            12h Block Rider Only
                                        </button>
                                    ` : ''}
                                </div>
                            </div>
                        </div>

                        <!-- Global Master Actions -->
                        <div class="p-4 rounded-2xl bg-purple-50/70 border border-purple-200/80 space-y-2">
                            <h4 class="text-xs font-black text-purple-950 uppercase tracking-wider">Global Account Administrative Overrides</h4>
                            <div class="flex flex-wrap gap-2 pt-1">
                                ${hasBlock ? `
                                    <button onclick="app.unblockDossierStudent('${profile.email}')" class="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold text-xs transition shadow-sm flex items-center gap-1.5">
                                        <i data-lucide="unlock" class="w-3.5 h-3.5"></i>
                                        <span>Immediate Unblock Both Consoles</span>
                                    </button>
                                ` : `
                                    <button onclick="app.blockDossierConsole('${profile.email}', 'BOTH')" class="px-4 py-2 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-extrabold text-xs transition shadow-sm flex items-center gap-1.5">
                                        <i data-lucide="lock" class="w-3.5 h-3.5"></i>
                                        <span>12-Hour Lock All Consoles</span>
                                    </button>
                                `}
                                <button onclick="app.openWarningModal('${profile.email}')" class="px-4 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-extrabold text-xs transition shadow-sm flex items-center gap-1.5">
                                    <i data-lucide="alert-triangle" class="w-3.5 h-3.5"></i>
                                    <span>Dispatch In-App Warning</span>
                                </button>
                                <button onclick="app.quickAdjustWalletBalance('${profile.email}')" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-extrabold text-xs transition shadow-sm flex items-center gap-1.5">
                                    <i data-lucide="credit-card" class="w-3.5 h-3.5"></i>
                                    <span>Adjust Wallet Balance</span>
                                </button>
                                <button onclick="app.openEditAccountModal('${profile.email}')" class="px-4 py-2 rounded-xl bg-gradient-to-r from-purple-700 to-indigo-800 hover:from-purple-800 hover:to-indigo-900 text-white font-extrabold text-xs transition shadow-sm flex items-center gap-1.5">
                                    <i data-lucide="user-cog" class="w-3.5 h-3.5"></i>
                                    <span>Edit Account Attributes & PIN</span>
                                </button>
                                <button onclick="app.forceDeviceSessionReset('${profile.email}')" class="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-900 text-white font-extrabold text-xs transition shadow-sm flex items-center gap-1.5" title="Remotely reset device cached state & require re-login">
                                    <i data-lucide="refresh-cw" class="w-3.5 h-3.5"></i>
                                    <span>Force Session Reset</span>
                                </button>
                            </div>
                        </div>
                    </div>
                `;
            }

            default:
                return `<div class="p-8 text-center text-slate-400">Select a sub-tab to view details.</div>`;
        }
    }

    quickAdjustWalletBalance(email) {
        const profile = this.getUserCompleteProfileAndActivities(email);
        if (!profile) return;
        const currentBal = profile.walletBalance;
        const promptVal = prompt(`Adjust Wallet Balance for ${profile.name} (${email}):\nCurrent Balance: Rs. ${currentBal}\n\nEnter new wallet balance (Rs.):`, currentBal);
        if (promptVal === null) return;
        const newBal = parseFloat(promptVal);
        if (isNaN(newBal) || newBal < 0) {
            alert("Please enter a valid positive number.");
            return;
        }
        const diff = newBal - currentBal;

        if (profile.student) {
            profile.student.walletBalance = newBal;
            this.save("campus_students_list", this.students);
        }
        if (profile.rider) {
            profile.rider.walletBalance = newBal;
            this.save("campus_riders_list", this.riders);
        }

        if (window.cloudSync) {
            window.cloudSync.syncWallet({ userEmail: email, walletBalance: newBal });
        }

        this.recordCustomerActivity({
            userEmail: email,
            userName: profile.name,
            userRole: profile.primaryRole,
            section: "WALLET",
            action: diff >= 0 ? "WALLET_CREDITED" : "WALLET_DEBITED",
            title: `Wallet Adjustment: Rs. ${Math.abs(diff)} ${diff >= 0 ? 'Credited' : 'Debited'}`,
            details: `Balance updated from Rs. ${currentBal} to Rs. ${newBal} by Admin`,
            status: "COMPLETED",
            badgeColor: diff >= 0 ? "bg-emerald-100 text-emerald-800" : "bg-rose-100 text-rose-800",
            amount: Math.abs(diff)
        });

        this.recordAudit("WALLET_MANUAL_ADJUST", email, `Admin adjusted wallet balance for ${profile.name} from Rs. ${currentBal} to Rs. ${newBal}.`, "WARNING");
        this.showToast(`Updated wallet balance to Rs. ${newBal} for ${profile.name}!`);

        if (this.activeDossierEmail && this.activeDossierEmail.toLowerCase() === email.toLowerCase()) {
            this.openUserActivityDossier(email, this.activeDossierSubTab);
        }
        this.updateSectionGrid("students");
        this.updateSectionGrid("riders");
    }

    blockDossierConsole(email, targetConsole) {
        const profile = this.getUserCompleteProfileAndActivities(email);
        if (!profile) return;
        const reason = prompt(`Enter reason for 12-hour temporary block (${targetConsole}) on ${profile.name}:`, "Administrative Policy Compliance Review");
        if (reason === null) return;
        const blockUntil = Date.now() + 12 * 3600 * 1000;

        if (profile.student) {
            if (targetConsole === "STUDENT_ONLY" || targetConsole === "BOTH") {
                profile.student.studentStatus = "TEMP_BLOCKED";
                profile.student.studentBlockUntil = blockUntil;
                profile.student.studentBlockReason = reason;
            }
            if (targetConsole === "RIDER_ONLY" || targetConsole === "BOTH") {
                profile.student.riderStatus = "TEMP_BLOCKED";
                profile.student.riderBlockUntil = blockUntil;
                profile.student.riderBlockReason = reason;
            }
            this.save("campus_students_list", this.students);
        }
        if (profile.rider && (targetConsole === "RIDER_ONLY" || targetConsole === "BOTH")) {
            profile.rider.riderStatus = "TEMP_BLOCKED";
            this.save("campus_riders_list", this.riders);
        }

        if (window.cloudSync) {
            if (targetConsole === "STUDENT_ONLY" || targetConsole === "BOTH") {
                window.cloudSync.syncStudentBlock({ userEmail: email, blockDurationHours: 12, blockReason: reason, studentStatus: "TEMP_BLOCKED" });
            }
            if (targetConsole === "RIDER_ONLY" || targetConsole === "BOTH") {
                window.cloudSync.syncRiderBlock({ userEmail: email, blockDurationHours: 12, blockReason: reason, riderStatus: "TEMP_BLOCKED" });
            }
        }

        this.recordCustomerActivity({
            userEmail: email,
            userName: profile.name,
            userRole: profile.primaryRole,
            section: "MODERATION",
            action: "ACCOUNT_TEMP_BLOCKED",
            title: `12-Hour Lockout Applied (${targetConsole})`,
            details: `Reason: ${reason} | Lockout active until 12 hours from now`,
            status: "TEMP_BLOCKED",
            badgeColor: "bg-rose-100 text-rose-800"
        });

        this.recordAudit("ACCOUNT_TEMP_LOCK", email, `12-Hour Lockout applied to ${targetConsole} for ${profile.name}: ${reason}`, "CRITICAL");
        this.showToast(`Applied 12-hour temporary lock on ${targetConsole} for ${profile.name}!`);

        if (this.activeDossierEmail && this.activeDossierEmail.toLowerCase() === email.toLowerCase()) {
            this.openUserActivityDossier(email, this.activeDossierSubTab);
        }
        this.updateSectionGrid("students");
        this.updateSectionGrid("riders");
    }

    unblockDossierStudent(email) {
        const profile = this.getUserCompleteProfileAndActivities(email);
        if (!profile) return;

        if (profile.student) {
            profile.student.studentStatus = "ACTIVE";
            profile.student.riderStatus = "ACTIVE";
            profile.student.studentBlockUntil = 0;
            profile.student.riderBlockUntil = 0;
            this.save("campus_students_list", this.students);
        }
        if (profile.rider) {
            profile.rider.riderStatus = "ACTIVE";
            this.save("campus_riders_list", this.riders);
        }

        if (window.cloudSync) {
            window.cloudSync.syncStudentUnblock({ userEmail: email });
            window.cloudSync.syncRiderUnblock({ userEmail: email });
        }

        this.recordCustomerActivity({
            userEmail: email,
            userName: profile.name,
            userRole: profile.primaryRole,
            section: "MODERATION",
            action: "ACCOUNT_UNBLOCKED",
            title: "All Lockouts Cleared (Active Status Restored)",
            details: "Student and Rider consoles restored to ACTIVE status by Admin",
            status: "ACTIVE",
            badgeColor: "bg-emerald-100 text-emerald-800"
        });

        this.recordAudit("ACCOUNT_UNBLOCK", email, `Admin lifted all lockouts for ${profile.name} restoring ACTIVE status.`, "INFO");
        this.showToast(`Restored ACTIVE status for ${profile.name}!`);

        if (this.activeDossierEmail && this.activeDossierEmail.toLowerCase() === email.toLowerCase()) {
            this.openUserActivityDossier(email, this.activeDossierSubTab);
        }
        this.updateSectionGrid("students");
        this.updateSectionGrid("riders");
    }

    toggleDossierRiderDuty(email) {
        const rider = this.riders.find(r => (r.email || "").toLowerCase() === email.toLowerCase());
        if (!rider) {
            this.showToast("Rider record not found.", "warning");
            return;
        }
        rider.isOnline = !rider.isOnline;
        this.save("campus_riders_list", this.riders);

        this.recordCustomerActivity({
            userEmail: email,
            userName: rider.name,
            userRole: "Rider",
            section: "RIDER",
            action: rider.isOnline ? "RIDER_ONLINE" : "RIDER_OFFLINE",
            title: `Rider Shift: ${rider.isOnline ? 'Online (Accepting Deliveries)' : 'Offline (Shift Paused)'}`,
            details: `Duty status updated to ${rider.isOnline ? 'ONLINE' : 'OFFLINE'}`,
            status: rider.isOnline ? "ONLINE" : "OFFLINE",
            badgeColor: rider.isOnline ? "bg-emerald-100 text-emerald-800" : "bg-slate-100 text-slate-700"
        });

        this.recordAudit("RIDER_DUTY_TOGGLE", email, `Admin set rider duty for ${rider.name} to ${rider.isOnline ? 'ONLINE' : 'OFFLINE'}.`, "INFO");
        this.showToast(`Set rider duty to ${rider.isOnline ? 'ONLINE' : 'OFFLINE'} for ${rider.name}!`);

        if (this.activeDossierEmail && this.activeDossierEmail.toLowerCase() === email.toLowerCase()) {
            this.openUserActivityDossier(email, this.activeDossierSubTab);
        }
        this.updateSectionGrid("riders");
    }

    toggleDossierRiderVerification(email) {
        const rider = this.riders.find(r => (r.email || "").toLowerCase() === email.toLowerCase());
        if (!rider) {
            this.showToast("Rider record not found.", "warning");
            return;
        }
        rider.verificationStatus = rider.verificationStatus === "VERIFIED" ? "SUSPENDED" : "VERIFIED";
        this.save("campus_riders_list", this.riders);

        this.recordCustomerActivity({
            userEmail: email,
            userName: rider.name,
            userRole: "Rider",
            section: "RIDER",
            action: "RIDER_KYC_UPDATED",
            title: `Rider KYC Status: ${rider.verificationStatus}`,
            details: `Verification status updated to ${rider.verificationStatus}`,
            status: rider.verificationStatus,
            badgeColor: rider.verificationStatus === "VERIFIED" ? "bg-emerald-100 text-emerald-800" : "bg-rose-100 text-rose-800"
        });

        this.recordAudit("RIDER_KYC_TOGGLE", email, `Admin set KYC verification for ${rider.name} to ${rider.verificationStatus}.`, "WARNING");
        this.showToast(`Set rider verification to ${rider.verificationStatus} for ${rider.name}!`);

        if (this.activeDossierEmail && this.activeDossierEmail.toLowerCase() === email.toLowerCase()) {
            this.openUserActivityDossier(email, this.activeDossierSubTab);
        }
        this.updateSectionGrid("riders");
    }

    openEditAccountModal(email) {
        const profile = this.getUserCompleteProfileAndActivities(email);
        if (!profile) {
            this.showToast("User profile not found for " + email, "error");
            return;
        }

        const modal = document.getElementById("modal-generic");
        if (!modal) return;

        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-3 sm:p-4 z-50 overflow-y-auto">
                <div class="modal-content bg-white w-full max-w-2xl rounded-3xl p-6 shadow-2xl border border-slate-200 max-h-[92vh] flex flex-col my-auto custom-scrollbar-light">
                    <!-- Header -->
                    <div class="flex items-center justify-between pb-3 border-b border-slate-100 shrink-0">
                        <div class="flex items-center gap-2.5">
                            <div class="w-10 h-10 rounded-2xl bg-purple-100 text-purple-700 flex items-center justify-center shadow-xs">
                                <i data-lucide="user-cog" class="w-5 h-5"></i>
                            </div>
                            <div>
                                <h3 class="text-base font-black text-slate-900">Edit Account Attributes & Remote Sync</h3>
                                <p class="text-[11px] text-slate-500 font-mono">${profile.email}</p>
                            </div>
                        </div>
                        <button onclick="app.closeModal()" class="text-slate-400 hover:text-slate-700 font-black text-2xl p-1 leading-none">&times;</button>
                    </div>

                    <!-- Form Body with Clean Scrolling -->
                    <form onsubmit="app.saveAccountAttributes(event, '${profile.email}')" class="flex-1 overflow-y-auto pt-4 pr-1 space-y-4 text-xs custom-scrollbar-light">
                        <!-- Alert banner -->
                        <div class="p-3 rounded-2xl bg-purple-50 border border-purple-200/80 text-purple-900 text-[11px] flex items-start gap-2">
                            <i data-lucide="zap" class="w-4 h-4 text-purple-700 shrink-0 mt-0.5"></i>
                            <span>Changes saved here persist in the database and <strong>synchronize live to the user's mobile app screen</strong> in real-time.</span>
                        </div>

                        <!-- Personal & Contact Information -->
                        <div class="space-y-3">
                            <h4 class="font-extrabold text-slate-800 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                                <i data-lucide="user" class="w-3.5 h-3.5 text-purple-700"></i>
                                <span>Personal & Contact Identity</span>
                            </h4>
                            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Full Name</label>
                                    <input type="text" id="edit-account-name" value="${profile.name}" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white" required />
                                </div>
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Phone Number</label>
                                    <input type="text" id="edit-account-phone" value="${profile.phone}" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white" required />
                                </div>
                            </div>
                        </div>

                        <!-- College & Academic Records -->
                        <div class="space-y-3 pt-2 border-t border-slate-100">
                            <h4 class="font-extrabold text-slate-800 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                                <i data-lucide="graduation-cap" class="w-3.5 h-3.5 text-purple-700"></i>
                                <span>Campus & Academic Roster</span>
                            </h4>
                            <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Roll Number</label>
                                    <input type="text" id="edit-account-roll" value="${profile.rollNo}" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white" required />
                                </div>
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Hostel & Room</label>
                                    <input type="text" id="edit-account-hostel" value="${profile.hostel}" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white" required />
                                </div>
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Academic Department</label>
                                    <input type="text" id="edit-account-stream" value="${profile.stream}" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white" required />
                                </div>
                            </div>
                        </div>

                        <!-- Financial & Wallet Balance -->
                        <div class="space-y-3 pt-2 border-t border-slate-100">
                            <h4 class="font-extrabold text-slate-800 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                                <i data-lucide="credit-card" class="w-3.5 h-3.5 text-purple-700"></i>
                                <span>Financials & Wallet Ledger</span>
                            </h4>
                            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Current Wallet Balance (Rs.)</label>
                                    <input type="number" step="1" id="edit-account-wallet" value="${profile.walletBalance}" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-bold text-emerald-700 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white" required />
                                </div>
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Registered UPI ID (Payouts / Refunds)</label>
                                    <input type="text" id="edit-account-upi" value="${profile.student?.upiId || profile.rider?.upiId || (profile.email.split('@')[0] + '@upi')}" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white" />
                                </div>
                            </div>
                        </div>

                        <!-- Role Permissions & Moderation Status -->
                        <div class="space-y-3 pt-2 border-t border-slate-100">
                            <h4 class="font-extrabold text-slate-800 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                                <i data-lucide="shield" class="w-3.5 h-3.5 text-purple-700"></i>
                                <span>Console Permissions & Lock Status</span>
                            </h4>
                            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Student Console Status</label>
                                    <select id="edit-account-student-status" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white">
                                        <option value="ACTIVE" ${(profile.student?.studentStatus || 'ACTIVE') === 'ACTIVE' ? 'selected' : ''}>ACTIVE (Full Access)</option>
                                        <option value="TEMP_BLOCKED" ${profile.student?.studentStatus === 'TEMP_BLOCKED' ? 'selected' : ''}>TEMP_BLOCKED (12h Policy Lock)</option>
                                        <option value="PERM_BLOCKED" ${profile.student?.studentStatus === 'PERM_BLOCKED' ? 'selected' : ''}>PERM_BLOCKED (Administrative Blacklist)</option>
                                    </select>
                                </div>
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Rider Fleet Console Status</label>
                                    <select id="edit-account-rider-status" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-semibold text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white">
                                        <option value="NONE" ${!profile.isRider ? 'selected' : ''}>NOT ENROLLED (Student Only)</option>
                                        <option value="REGISTERED" ${profile.rider && profile.rider.verificationStatus === 'REGISTERED' ? 'selected' : ''}>REGISTERED (Pending KYC)</option>
                                        <option value="VERIFIED" ${profile.rider && profile.rider.verificationStatus === 'VERIFIED' ? 'selected' : ''}>VERIFIED (Active Fleet Runner)</option>
                                        <option value="TEMP_BLOCKED" ${profile.rider && profile.rider.riderStatus === 'TEMP_BLOCKED' ? 'selected' : ''}>TEMP_BLOCKED (Suspended Duty)</option>
                                    </select>
                                </div>
                            </div>
                        </div>

                        <!-- Client Security PIN Override -->
                        <div class="space-y-3 pt-2 border-t border-slate-100">
                            <h4 class="font-extrabold text-slate-800 uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                                <i data-lucide="key" class="w-3.5 h-3.5 text-purple-700"></i>
                                <span>Client Security PIN Management</span>
                            </h4>
                            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                <div>
                                    <label class="block font-bold text-slate-700 mb-1">Assign New Client PIN (Optional)</label>
                                    <input type="text" id="edit-account-pin" placeholder="Leave blank to keep existing PIN" maxlength="6" class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-mono text-slate-800 focus:outline-none focus:ring-2 focus:ring-purple-600 focus:bg-white" />
                                    <span class="text-[10px] text-slate-400">e.g. 1234 or 2026. If provided, overrides client login PIN immediately.</span>
                                </div>
                                <div class="flex flex-col justify-center space-y-2 pt-2 sm:pt-0">
                                    <label class="inline-flex items-center gap-2 cursor-pointer">
                                        <input type="checkbox" id="edit-account-force-reset" class="w-4 h-4 text-purple-600 rounded border-slate-300 focus:ring-purple-500" />
                                        <span class="font-bold text-slate-700 text-xs">Force Remote Device Session Re-login</span>
                                    </label>
                                    <span class="text-[10px] text-slate-400">Forces the user's mobile app to invalidate local cached session and re-verify.</span>
                                </div>
                            </div>
                        </div>

                        <!-- Footer Buttons -->
                        <div class="pt-4 border-t border-slate-100 flex items-center justify-end gap-2.5">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-600 font-bold transition">Cancel</button>
                            <button type="submit" class="px-5 py-2.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-black transition flex items-center gap-2 shadow-sm">
                                <i data-lucide="save" class="w-4 h-4"></i>
                                <span>Save & Push Changes Live</span>
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
        if (window.lucide) lucide.createIcons();
    }

    saveAccountAttributes(e, email) {
        e.preventDefault();
        const cleanEmail = email.toLowerCase().trim();
        const student = this.students.find(s => (s.email || '').toLowerCase() === cleanEmail);
        const rider = this.riders.find(r => (r.email || '').toLowerCase() === cleanEmail);

        const name = document.getElementById("edit-account-name").value.trim();
        const phone = document.getElementById("edit-account-phone").value.trim();
        const rollNo = document.getElementById("edit-account-roll").value.trim();
        const hostel = document.getElementById("edit-account-hostel").value.trim();
        const stream = document.getElementById("edit-account-stream").value.trim();
        const wallet = parseFloat(document.getElementById("edit-account-wallet").value) || 0;
        const upiId = document.getElementById("edit-account-upi").value.trim();
        const studentStatus = document.getElementById("edit-account-student-status").value;
        const riderStatus = document.getElementById("edit-account-rider-status").value;
        const pin = document.getElementById("edit-account-pin").value.trim();
        const forceReset = document.getElementById("edit-account-force-reset").checked;

        if (student) {
            student.name = name;
            student.phone = phone;
            student.rollNo = rollNo;
            student.hostel = hostel;
            student.stream = stream;
            student.walletBalance = wallet;
            student.upiId = upiId;
            student.studentStatus = studentStatus;
            if (studentStatus === "ACTIVE") {
                student.studentBlockUntil = 0;
                student.studentBlockReason = "";
            }
            if (pin) {
                student.clientPin = pin;
            }
            this.save("campus_students_list", this.students);
        }

        if (rider || riderStatus !== "NONE") {
            let activeRider = rider;
            if (!activeRider && student) {
                activeRider = {
                    id: cleanEmail,
                    email: cleanEmail,
                    name: `${name} (Rider)`,
                    phone: phone,
                    rollNo: rollNo,
                    department: stream,
                    isOnline: false,
                    activeJobId: "",
                    walletBalance: wallet,
                    totalDeliveries: 0,
                    rating: "5.0 ★",
                    verificationStatus: riderStatus === "VERIFIED" ? "VERIFIED" : "REGISTERED",
                    riderStatus: riderStatus === "TEMP_BLOCKED" ? "TEMP_BLOCKED" : "ACTIVE"
                };
                this.riders.push(activeRider);
            } else if (activeRider) {
                activeRider.name = name;
                activeRider.phone = phone;
                activeRider.rollNo = rollNo;
                activeRider.department = stream;
                activeRider.walletBalance = wallet;
                activeRider.verificationStatus = riderStatus === "NONE" ? "UNVERIFIED" : (riderStatus === "REGISTERED" ? "REGISTERED" : "VERIFIED");
                activeRider.riderStatus = riderStatus === "TEMP_BLOCKED" ? "TEMP_BLOCKED" : "ACTIVE";
                if (riderStatus === "TEMP_BLOCKED") {
                    activeRider.isOnline = false;
                }
            }
            this.save("campus_riders_list", this.riders);
        }

        // Realtime bridge payload
        if (window.cloudSync) {
            window.cloudSync.syncAccountOverride({
                userEmail: cleanEmail,
                name: name,
                phone: phone,
                rollNo: rollNo,
                hostel: hostel,
                stream: stream,
                walletBalance: wallet,
                upiId: upiId,
                studentStatus: studentStatus,
                riderStatus: riderStatus,
                pin: pin,
                forceSessionReset: forceReset
            });
        }

        this.recordAudit("ACCOUNT_OVERRIDE", cleanEmail, `Admin updated account properties for ${name} (${cleanEmail}): Roll=${rollNo}, Phone=${phone}, Wallet=Rs.${wallet}, Status=${studentStatus}${pin ? ', PIN reset' : ''}${forceReset ? ', Force Reset sent' : ''}`, "WARNING");
        this.recordCustomerActivity({
            userEmail: cleanEmail,
            userName: name,
            userRole: "Customer",
            section: "PROFILE",
            action: "PROFILE_UPDATED",
            title: "Account Properties Admin Override",
            details: `Administrative override: Contact & credentials updated and pushed to device.`,
            status: "UPDATED"
        });

        this.closeModal();
        this.showToast(`Account properties updated & pushed live to ${name}'s device!`);
        this.updateSectionGrid("students");
        this.updateSectionGrid("riders");

        if (this.activeDossierEmail && this.activeDossierEmail.toLowerCase() === cleanEmail) {
            this.openUserActivityDossier(cleanEmail, this.activeDossierSubTab);
        }
    }

    forceDeviceSessionReset(email) {
        const cleanEmail = email.toLowerCase().trim();
        if (!confirm(`Are you sure you want to force remote session reset for ${cleanEmail}?\n\nThis will instantly invalidate their device session and require re-login on their phone.`)) {
            return;
        }
        if (window.cloudSync) {
            window.cloudSync.syncForceSessionReset({ userEmail: cleanEmail, reason: "Administrative Master Reset" });
        }
        this.recordAudit("SESSION_FORCE_RESET", cleanEmail, `Admin executed remote session invalidation for user ${cleanEmail}.`, "CRITICAL");
        this.showToast(`Remote session reset dispatched to ${cleanEmail}'s device!`);
    }

    closeModal() {
        const modal = document.getElementById("modal-generic");
        if (modal) modal.classList.add("hidden");
    }
}

// Global initialization on DOM ready
document.addEventListener("DOMContentLoaded", () => {
    window.app = new CampusAdminApp();
    if (window.cloudSync) {
        window.cloudSync.flushPendingQueue();
        window.cloudSync.pollRecentMessages(true);
    }
});
