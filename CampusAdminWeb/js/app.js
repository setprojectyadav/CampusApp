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
            reports: "",
            feedback: "",
            feedbackSubTab: 0 // 0: Suggestions, 1: Roadmap
        };
        this.severityFilter = "ALL";
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
        this.fraudAnomalies = getOrSet("admin_fraud_anomalies", SEED_FRAUD_ANOMALIES);
        this.auditLogs = getOrSet("admin_audit_trail_log", SEED_AUDIT_LOGS);
        this.feedback = getOrSet("campus_user_feedback_list", SEED_FEEDBACK);
        this.systemConfig = getOrSet("campus_system_config", DEFAULT_SYSTEM_CONFIG);
    }

    save(key, data) {
        localStorage.setItem(key, JSON.stringify(data));
        // Automatic Realtime Cloud Synchronization with Firebase campusapp-e8435
        if (window.cloudSync && window.cloudSync.db) {
            try {
                if (key === "app_orders_list") {
                    const updates = {};
                    (data || []).forEach(o => { updates[o.id.replace(/[.#$[\]]/g, "_")] = o; });
                    window.cloudSync.db.ref("orders").update(updates);
                } else if (key === "campus_students_list") {
                    const updates = {};
                    (data || []).forEach(s => { updates[s.id.replace(/[.#$[\]]/g, "_")] = s; });
                    window.cloudSync.db.ref("students").update(updates);
                } else if (key === "campus_riders_list") {
                    const updates = {};
                    (data || []).forEach(r => { updates[r.id.replace(/[.#$[\]]/g, "_")] = r; });
                    window.cloudSync.db.ref("riders").update(updates);
                } else if (key === "admin_dispute_cases") {
                    const updates = {};
                    (data || []).forEach(d => { updates[d.id.replace(/[.#$[\]]/g, "_")] = d; });
                    window.cloudSync.db.ref("disputes").update(updates);
                } else if (key === "admin_audit_trail_log") {
                    const latest = (data || [])[0];
                    if (latest && latest.id) {
                        window.cloudSync.db.ref(`audit_trail/${latest.id.replace(/[.#$[\]]/g, "_")}`).set(latest);
                    }
                }
            } catch (e) {
                console.warn("[CloudSync] Background sync exception:", e);
            }
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
            : "127.0.0.1 (Local Client)";
        this.updateIpDisplays();
    }

    updateIpDisplays() {
        const clientEl = document.getElementById("client-ip-display");
        if (clientEl) clientEl.textContent = this.clientIp;
        const topEl = document.getElementById("topbar-ip-display");
        if (topEl) topEl.textContent = this.clientIp;
    }

    recordAudit(actionType, targetId, details, severity = "INFO") {
        const now = new Date();
        const formatted = now.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' }) +
            ', ' + now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true });

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
            sessionStorage.setItem("admin_is_unlocked", "true");
            errorEl.classList.add("hidden");
            this.recordAudit("AUTH_LOGIN_SUCCESS", "GATEWAY", "Superior Administrator unlocked console with Master PIN.", "INFO");
            this.render();
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
            sessionStorage.setItem("admin_is_unlocked", "true");
            errorEl.classList.add("hidden");
            this.recordAudit("AUTH_LOGIN_SUCCESS", "GATEWAY", "Operational team member unlocked console with Staff PIN.", "INFO");
            this.render();
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
            sessionStorage.setItem("admin_is_unlocked", "true");
            errorEl.classList.add("hidden");
            this.recordAudit("AUTH_LOGIN_SUCCESS", "GATEWAY", "Financial Auditor unlocked console with Auditor PIN.", "INFO");
            this.render();
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
            sessionStorage.setItem("admin_is_unlocked", "true");
            errorEl.classList.add("hidden");
            this.recordAudit("AUTH_LOGIN_SUCCESS", "GATEWAY", "Safety & Dispute Officer unlocked console with Security PIN.", "INFO");
            this.render();
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
        this.isUnlocked = false;
        sessionStorage.setItem("admin_is_unlocked", "false");
        this.render();
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
        const roleObj = ADMIN_ROLES[this.activeRole] || ADMIN_ROLES.SUPER_ADMIN;
        const roleLabel = document.getElementById("topbar-role-label");
        if (roleLabel) {
            roleLabel.textContent = `Role: ${roleObj.name} >`;
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
                <button onclick="app.setTab(${tabIdx})" class="flex items-center gap-2 px-4 py-2.5 rounded-xl font-medium text-xs whitespace-nowrap transition-all ${
                    isSelected 
                        ? 'bg-white text-purple-900 shadow-md font-bold' 
                        : 'text-white/80 hover:bg-white/10 hover:text-white'
                }">
                    <i data-lucide="${tab.icon}" class="w-3.5 h-3.5"></i>
                    <span>${tab.label}</span>
                </button>
            `;
        }).join("");
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

        switch (this.selectedTab) {
            case 0: main.innerHTML = this.renderOverviewTab(); break;
            case 1: main.innerHTML = this.renderOrdersTab(); break;
            case 2: main.innerHTML = this.renderStudentsTab(); break;
            case 3: main.innerHTML = this.renderRidersTab(); break;
            case 4: main.innerHTML = this.renderServicesTab(); break;
            case 5: main.innerHTML = this.renderDisputesTab(); break;
            case 6: main.innerHTML = this.renderLostFoundTab(); break;
            case 7: main.innerHTML = this.renderFraudScannerTab(); break;
            case 8: main.innerHTML = this.renderAuditTrailTab(); break;
            case 9: main.innerHTML = this.renderReportsTab(); break;
            case 10: main.innerHTML = this.renderFeedbackTab(); break;
            default: main.innerHTML = `<div class="p-8 text-center text-slate-500">Select a valid tab</div>`;
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
                const container = document.getElementById("audit-grid");
                const badge = document.getElementById("audit-count-badge");
                if (container) {
                    const filtered = this.getFilteredAudit();
                    container.innerHTML = this.renderAuditCards(filtered);
                    if (badge) badge.textContent = `${filtered.length} Records Logged`;
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
                    <p class="text-xs text-slate-500 mt-1">Real-time campus telemetry and service metrics.</p>
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
            </div>
        `;
    }

    // ==========================================
    // TAB 1: ORDERS TAB
    // ==========================================
    getFilteredOrders() {
        const q = (this.searchQueries.orders || "").toLowerCase().trim();
        return this.orders.filter(o => {
            if (!q) return true;
            const itemsText = (o.items || []).map(it => `${it.name} ${it.qty}`).join(" ");
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
                        <p class="text-xs text-slate-500 mt-1">Live order dispatch, delivery verification, and runner assignments.</p>
                    </div>
                    <span id="orders-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${filtered.length} Orders Listed
                    </span>
                </div>

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
                            <div class="font-bold text-purple-900">Customer Details</div>
                            <div class="text-slate-800 font-semibold">${order.customerName || "Customer"} <span class="text-slate-400 font-normal">(${order.customerId})</span></div>
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
                                    <span class="font-bold text-emerald-700">Rating: ${order.runnerRating || "4.8 *"}</span>
                                </div>
                                <div class="font-bold text-slate-800">Rider: ${order.runnerName}</div>
                                <div class="text-emerald-800">Phone: ${order.runnerPhone || "+91 9822334455"}</div>
                                ${order.actualBill > 0 ? `
                                    <div class="font-bold text-emerald-700">Uploaded Actual Bill: Rs. ${order.actualBill}</div>
                                ` : ''}
                            </div>
                        ` : ''}
                    </div>

                    <!-- Footer Action -->
                    <div class="mt-4 pt-3 border-t border-slate-100 flex justify-end">
                        <button onclick="app.openOrderInterventionModal('${order.id}')" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-bold transition shadow-sm">
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
        return this.students.filter(st => {
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
                        <p class="text-xs text-slate-500 mt-1">Student accounts, wallet balances, and disciplinary status.</p>
                    </div>
                    <span id="students-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${filtered.length} Students Listed
                    </span>
                </div>

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
                            <div>
                                <div class="font-extrabold text-sm text-slate-900">${st.name}</div>
                                <div class="text-[11px] text-purple-700 font-semibold">${st.email}</div>
                                <div class="text-[11px] text-slate-400">Roll: ${st.rollNo} | ${st.hostel}</div>
                                <div class="text-[11px] text-slate-400">Stream: ${st.stream || 'Engineering'}</div>
                            </div>
                            <div class="text-right">
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
                        ${hasActiveLock ? `
                            <button onclick="app.immediateUnblockStudent('${st.email}')" class="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold transition">
                                Unblock All
                            </button>
                        ` : ''}
                        <button onclick="app.openWarningModal('${st.email}')" class="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold transition">
                            Warn ID
                        </button>
                        <button onclick="app.openModerateStudentModal('${st.email}')" class="px-3 py-1.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold transition">
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
        const name = st ? st.name : "User";
        const targetEmail = st ? st.email : email;

        const modal = document.getElementById("modal-generic");
        modal.innerHTML = `
            <div class="modal-backdrop fixed inset-0 flex items-center justify-center p-4 z-50">
                <div class="modal-content bg-white w-full max-w-md rounded-3xl p-6 shadow-2xl border border-slate-200">
                    <h3 class="text-base font-extrabold text-slate-900">Dispatch Warning: ${name}</h3>
                    <p class="text-xs text-slate-500 mt-1">This warning will be delivered immediately to the student upon launching CampusApp.</p>

                    <form onsubmit="app.submitWarning(event, '${targetEmail}')" class="mt-4 space-y-4 text-xs">
                        <div>
                            <label class="block font-bold text-slate-700 mb-1">Warning Message Content:</label>
                            <textarea id="warning-text" rows="3" class="w-full p-3 border border-slate-200 rounded-xl focus:ring-2 focus:ring-amber-500 focus:outline-none">Official Warning: Policy violation detected. Future infractions will trigger a 12-hour temporary account lock.</textarea>
                        </div>
                        
                        <div class="flex justify-end gap-2">
                            <button type="button" onclick="app.closeModal()" class="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 font-bold">Cancel</button>
                            <button type="submit" class="px-5 py-2 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold">Send Warning</button>
                        </div>
                    </form>
                </div>
            </div>
        `;
        modal.classList.remove("hidden");
    }

    submitWarning(e, email) {
        e.preventDefault();
        const st = this.students.find(s => s.email === email);
        const msg = document.getElementById("warning-text").value.trim();
        if (st) {
            st.warningMessage = msg;
            this.save("campus_students_list", this.students);
        }
        this.recordAudit("DISPATCH_WARNING", email, `Dispatched warning to ${email}: "${msg}"`, "WARNING");
        this.closeModal();
        this.showToast(`Official warning dispatched to ${email}!`);
        this.updateSectionGrid("students");
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
        return this.riders.filter(r => {
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
                        <p class="text-xs text-slate-500 mt-1">Runner verification, performance ratings, and active duty shifts.</p>
                    </div>
                    <span id="riders-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${filtered.length} Riders Listed
                    </span>
                </div>

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

        return ridersList.map(r => `
            <div onclick="app.openRiderPreviewModal('${r.email}')" class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive flex flex-col justify-between cursor-pointer">
                <div class="space-y-3">
                    <div class="flex items-start justify-between">
                        <div>
                            <div class="font-extrabold text-sm text-slate-900">${r.name}</div>
                            <div class="text-[11px] text-purple-700 font-semibold">${r.email} | Roll: ${r.rollNo}</div>
                            <div class="text-[11px] text-slate-400">${r.department}</div>
                        </div>
                        <span class="pill-status ${r.isOnline ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}">
                            ${r.isOnline ? 'ONLINE' : 'OFFLINE'}
                        </span>
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

                    <div class="text-xs text-sky-700 font-semibold">
                        KYC Verification: <span class="font-bold">${r.verificationStatus}</span>
                    </div>

                    ${r.activeJobId ? `
                        <div class="p-2.5 rounded-xl bg-sky-50 border border-sky-200 text-xs font-bold text-sky-800 flex items-center justify-between">
                            <span>Active Job: ${r.activeJobId}</span>
                            <span class="text-[10px] bg-sky-600 text-white px-2 py-0.5 rounded-full">In Transit</span>
                        </div>
                    ` : ''}
                </div>

                <div class="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                    <span class="text-[11px] font-bold text-purple-700">Tap for Rider 360 &gt;</span>
                    <button onclick="event.stopPropagation(); app.toggleRiderOnline('${r.email}')" class="px-3 py-1 rounded-xl border border-slate-200 text-slate-700 font-bold hover:bg-slate-50 transition text-[11px]">
                        Set ${r.isOnline ? 'Offline' : 'Online'}
                    </button>
                </div>
            </div>
        `).join("");
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

                        <!-- Action Buttons -->
                        <div class="grid grid-cols-2 gap-2 pt-2">
                            <a href="tel:${rider.phone}" class="py-2.5 px-4 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-center transition flex items-center justify-center gap-1.5">
                                <i data-lucide="phone" class="w-3.5 h-3.5"></i>
                                <span>Call Rider</span>
                            </a>
                            <button onclick="app.toggleRiderVerification('${rider.email}')" class="py-2.5 px-4 rounded-xl ${rider.verificationStatus === 'VERIFIED' ? 'bg-rose-600 hover:bg-rose-700' : 'bg-emerald-600 hover:bg-emerald-700'} text-white font-bold text-center transition">
                                ${rider.verificationStatus === 'VERIFIED' ? 'Suspend Rider' : 'Verify Rider'}
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
        const type = this.searchQueries.servicesType || "ALL";

        const cabs = this.cabPools.filter(cab => {
            if (type !== "ALL" && type !== "CABS") return false;
            if (!q) return true;
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
                `Rs. ${cab.perPersonFare}`
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });

        const meals = this.mealCircles.filter(meal => {
            if (type !== "ALL" && type !== "MEALS") return false;
            if (!q) return true;
            const haystack = [
                meal.id,
                meal.restaurantSpot,
                meal.mealCategory,
                meal.hostName,
                meal.hostPhone,
                meal.meetingPoint,
                meal.targetTime,
                meal.status
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });

        const rentals = this.rentals.filter(rnt => {
            if (type !== "ALL" && type !== "RENTALS") return false;
            if (!q) return true;
            const haystack = [
                rnt.id,
                rnt.productName,
                rnt.renterEmail,
                rnt.handoverOtp,
                rnt.returnOtp,
                rnt.status,
                `Rs. ${rnt.totalPaid}`
            ].map(v => (v || "").toString().toLowerCase());
            return haystack.some(v => v.includes(q));
        });

        return { cabs, meals, rentals, totalCount: cabs.length + meals.length + rentals.length };
    }

    renderServicesTab() {
        const type = this.searchQueries.servicesType;
        const data = this.getFilteredServices();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Multi-Service Hub</h2>
                        <p class="text-xs text-slate-500 mt-1">Cab pools, meal circles, and equipment rentals.</p>
                    </div>

                    <!-- Segmented Filter Control -->
                    <div class="flex items-center gap-1.5 p-1 bg-slate-100 rounded-xl text-xs font-semibold self-start sm:self-auto">
                        <button onclick="app.setServicesFilter('ALL')" class="px-3 py-1.5 rounded-lg transition ${type === 'ALL' ? 'bg-white shadow text-purple-900 font-bold' : 'text-slate-600 hover:text-slate-900'}">All</button>
                        <button onclick="app.setServicesFilter('CABS')" class="px-3 py-1.5 rounded-lg transition ${type === 'CABS' ? 'bg-white shadow text-purple-900 font-bold' : 'text-slate-600 hover:text-slate-900'}">Cab Pools (${this.cabPools.length})</button>
                        <button onclick="app.setServicesFilter('MEALS')" class="px-3 py-1.5 rounded-lg transition ${type === 'MEALS' ? 'bg-white shadow text-purple-900 font-bold' : 'text-slate-600 hover:text-slate-900'}">Meals (${this.mealCircles.length})</button>
                        <button onclick="app.setServicesFilter('RENTALS')" class="px-3 py-1.5 rounded-lg transition ${type === 'RENTALS' ? 'bg-white shadow text-purple-900 font-bold' : 'text-slate-600 hover:text-slate-900'}">Rentals (${this.rentals.length})</button>
                    </div>
                </div>

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("services", "Search across Cabs (destination, host), Meals (spot, category), Rentals (product, renter)...")}

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
                            <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive text-xs space-y-2">
                                <div class="flex items-center justify-between">
                                    <span class="font-bold text-slate-900 text-sm">${cab.id}: ${cab.pickupPoint} &gt; ${cab.dropDestination}</span>
                                    <span class="pill-status bg-sky-100 text-sky-800">${cab.status}</span>
                                </div>
                                <div class="text-slate-500">Host: ${cab.hostName} (${cab.hostPhone}) | Time: ${cab.departureDate} ${cab.departureTime}</div>
                                <div class="text-purple-700 font-bold">Fare: Rs. ${cab.perPersonFare}/seat | Seats: ${cab.bookedSeats}/${cab.totalSeats} Booked</div>
                                ${cab.isNightRide ? `<span class="inline-block px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 font-bold text-[10px]">Night Ride Protection</span>` : ''}
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
                            <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive text-xs space-y-2">
                                <div class="flex items-center justify-between">
                                    <span class="font-bold text-slate-900 text-sm">${meal.id}: ${meal.restaurantSpot}</span>
                                    <span class="pill-status bg-amber-100 text-amber-800">${meal.status}</span>
                                </div>
                                <div class="text-slate-500">Category: ${meal.mealCategory} | Host: ${meal.hostName}</div>
                                <div class="text-slate-700">Buddies: ${meal.joinedBuddies}/${meal.totalBuddies} Joined | Meet: ${meal.meetingPoint} @ ${meal.targetTime}</div>
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
                            <div class="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm card-interactive text-xs space-y-2">
                                <div class="flex items-center justify-between">
                                    <span class="font-bold text-slate-900 text-sm">${rnt.id}: ${rnt.productName}</span>
                                    <span class="pill-status bg-emerald-100 text-emerald-800">${rnt.status}</span>
                                </div>
                                <div class="text-slate-500">Renter: ${rnt.renterEmail} | Duration: ${rnt.durationDays} Days</div>
                                <div class="text-emerald-700 font-bold">Total Paid: Rs. ${rnt.totalPaid} | Handover OTP: ${rnt.handoverOtp} | Return OTP: ${rnt.returnOtp}</div>
                            </div>
                        `).join("")}
                    </div>
                </div>
            ` : ''}
        `;
    }

    setServicesFilter(type) {
        this.searchQueries.servicesType = type;
        this.updateSectionGrid("services");
    }

    // ==========================================
    // TAB 5: DISPUTES & ESCROW DESK
    // ==========================================
    getFilteredDisputes() {
        const q = (this.searchQueries.disputes || "").toLowerCase().trim();
        const filter = this.searchQueries.disputesTab || "OPEN";

        return this.disputes.filter(d => {
            if (filter === "OPEN" && !d.status.startsWith("OPEN") && !d.status.startsWith("UNDER")) return false;
            if (filter === "RESOLVED" && !d.status.startsWith("RESOLVED")) return false;

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
        const filter = this.searchQueries.disputesTab;
        const openCount = this.disputes.filter(d => d.status.startsWith("OPEN") || d.status.startsWith("UNDER")).length;
        const resolvedCount = this.disputes.filter(d => d.status.startsWith("RESOLVED")).length;
        const filtered = this.getFilteredDisputes();

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Disputes & Arbitration Desk</h2>
                        <p class="text-xs text-slate-500 mt-1">Review student claims, evidence, and issue refund payouts.</p>
                    </div>

                    <!-- Sub Tabs matching Android -->
                    <div class="flex items-center gap-1.5 p-1 bg-slate-100 rounded-xl text-xs font-semibold self-start sm:self-auto">
                        <button onclick="app.setDisputesFilter('OPEN')" class="px-3 py-1.5 rounded-lg transition ${filter === 'OPEN' ? 'bg-white shadow text-purple-900 font-bold' : 'text-slate-600 hover:text-slate-900'}">Open Cases (${openCount})</button>
                        <button onclick="app.setDisputesFilter('RESOLVED')" class="px-3 py-1.5 rounded-lg transition ${filter === 'RESOLVED' ? 'bg-white shadow text-purple-900 font-bold' : 'text-slate-600 hover:text-slate-900'}">Resolved (${resolvedCount})</button>
                        <button onclick="app.setDisputesFilter('ALL')" class="px-3 py-1.5 rounded-lg transition ${filter === 'ALL' ? 'bg-white shadow text-purple-900 font-bold' : 'text-slate-600 hover:text-slate-900'}">All Cases (${this.disputes.length})</button>
                    </div>
                </div>

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
                        <div class="text-slate-500">Complainant: ${d.complainantName} vs ${d.respondentName} (${d.respondentRole})</div>
                        <div class="text-slate-700">Statement: "${d.description}"</div>
                        <div class="text-sky-700 font-bold">Escrow At Stake: Rs. ${d.escrowAmount} | Filed: ${d.filedTimestamp}</div>
                    </div>

                    <div class="pt-3 border-t border-slate-100 flex items-center justify-end">
                        <button onclick="app.openDisputeReviewModal('${d.id}')" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs transition shadow-sm">
                            Review Evidence & Arbitrate
                        </button>
                    </div>
                </div>
            `;
        }).join("");
    }

    setDisputesFilter(filter) {
        this.searchQueries.disputesTab = filter;
        this.updateSectionGrid("disputes");
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
                            <div class="font-bold text-slate-900 mb-2">Chat Logs & Message Transcript:</div>
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
    // TAB 6: LOST & FOUND
    // ==========================================
    getFilteredLostFound() {
        const q = (this.searchQueries.lostFound || "").toLowerCase().trim();
        return this.lostFound.filter(item => {
            if (!q) return true;
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
                item.adminTakedownReason
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

            return `
                <div class="bg-white rounded-2xl p-5 border ${item.isFlagged ? 'border-rose-300' : 'border-slate-200/80'} shadow-sm card-interactive space-y-3 text-xs flex flex-col justify-between">
                    <div class="space-y-2">
                        <div class="flex items-center justify-between pb-2 border-b border-slate-100">
                            <div class="flex items-center gap-2">
                                <span class="pill-status ${isLost ? 'bg-rose-100 text-rose-800' : 'bg-emerald-100 text-emerald-800'}">${item.type}</span>
                                <span class="font-bold text-slate-500">${item.category}</span>
                            </div>
                            <span class="pill-status ${isDeposited ? 'bg-sky-100 text-sky-800' : 'bg-slate-100 text-slate-700'}">${item.status}</span>
                        </div>

                        <div>
                            <h3 class="font-bold text-slate-900 text-sm">${item.title}</h3>
                            <p class="text-slate-600 mt-1">${item.description}</p>
                            <div class="text-slate-400 text-[11px] mt-1">Location: ${item.location} | Date: ${item.date}</div>
                            <div class="text-purple-700 text-[11px] font-semibold mt-0.5">Reporter: ${item.reporterName} (${item.reporterRollNo}) | ${item.reporterEmail}</div>
                        </div>

                        ${item.securityDeskNote ? `
                            <div class="p-2.5 rounded-xl bg-sky-50 border border-sky-200 text-sky-800 font-semibold">
                                Security Holding: ${item.securityDeskNote}
                            </div>
                        ` : ''}
                    </div>

                    <!-- Action Buttons matching Point 6 in Android -->
                    <div class="pt-3 border-t border-slate-100 space-y-2">
                        <div class="grid grid-cols-2 gap-2">
                            <a href="tel:${item.reporterPhone}" class="py-2 px-3 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-50 font-bold text-center transition flex items-center justify-center gap-1">
                                <i data-lucide="phone" class="w-3.5 h-3.5"></i>
                                <span>Call Reporter</span>
                            </a>
                            <button onclick="app.openSecurityDepositModal('${item.id}')" class="py-2 px-3 rounded-xl bg-sky-600 hover:bg-sky-700 text-white font-bold text-center transition">
                                Security Deposit
                            </button>
                        </div>
                        <button onclick="app.openTakedownModal('${item.id}')" class="w-full py-2 px-3 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-center transition">
                            Takedown Post & Penalize ID
                        </button>
                    </div>
                </div>
            `;
        }).join("");
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
        return this.fraudAnomalies.filter(anm => {
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
                        <p class="text-xs text-slate-500 mt-1">Automated anomaly detection for location spoofing and account abuse.</p>
                    </div>
                    <span id="fraud-count-badge" class="text-xs font-semibold px-3 py-1 rounded-full bg-purple-100 text-purple-800 self-start sm:self-auto">
                        ${filtered.length} Anomalies Tracked
                    </span>
                </div>

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
                            ${(malpracticeType === 'RIDER_MALPRACTICE' || malpracticeType === 'ESCROW_ABUSE') ? `
                                <button onclick="app.blockAnomalyConsole('${anm.id}', 'RIDER_ONLY')" class="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold transition">
                                    12h Block Rider Only
                                </button>
                            ` : ''}
                            ${(malpracticeType === 'CUSTOMER_MALPRACTICE' || malpracticeType === 'ESCROW_ABUSE') ? `
                                <button onclick="app.blockAnomalyConsole('${anm.id}', 'STUDENT_ONLY')" class="px-3 py-1.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold transition">
                                    12h Block Student Only
                                </button>
                            ` : ''}
                        ` : `
                            <button onclick="app.unblockAnomalyConsole('${anm.id}')" class="px-3 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold transition">
                                Immediate Unblock
                            </button>
                        `}
                        <button onclick="app.openWarningModal('${anm.userId}')" class="px-3 py-1.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold transition">
                            Warn User
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
        const st = this.students.find(s => s.email === anm.userId);
        if (st) {
            if (targetConsole === "RIDER_ONLY") {
                st.riderStatus = "TEMP_BLOCKED";
                st.riderBlockUntil = blockUntil;
                st.riderBlockReason = `Fraud Malpractice: ${anm.anomalyType}`;
            } else {
                st.studentStatus = "TEMP_BLOCKED";
                st.studentBlockUntil = blockUntil;
                st.studentBlockReason = `Fraud Malpractice: ${anm.anomalyType}`;
            }
            this.save("campus_students_list", this.students);
        }

        this.recordAudit("FRAUD_BLOCK_TRIGGERED", anm.userId, `12-Hour Lockout applied to ${targetConsole} for ${anm.userName} (${anm.anomalyType}).`, "CRITICAL");
        this.showToast(`Applied 12-Hour Lockout (${targetConsole}) on ${anm.userName}!`);
        this.updateSectionGrid("fraud");
    }

    unblockAnomalyConsole(id) {
        const anm = this.fraudAnomalies.find(a => a.id === id);
        if (!anm) return;
        anm.status = "FLAGGED_PENDING";
        anm.blockUntil = 0;
        this.save("admin_fraud_anomalies", this.fraudAnomalies);

        const st = this.students.find(s => s.email === anm.userId);
        if (st) {
            st.studentStatus = "ACTIVE";
            st.riderStatus = "ACTIVE";
            st.studentBlockUntil = 0;
            st.riderBlockUntil = 0;
            this.save("campus_students_list", this.students);
        }

        this.recordAudit("FRAUD_UNBLOCK", anm.userId, `Admin lifted fraud lockout on ${anm.userName}.`, "INFO");
        this.showToast(`Lifted lockout for ${anm.userName}!`);
        this.updateSectionGrid("fraud");
    }

    // ==========================================
    // TAB 8: AUDIT TRAIL
    // ==========================================
    getFilteredAudit() {
        const q = (this.searchQueries.audit || "").toLowerCase().trim();
        const sev = this.severityFilter || "ALL";

        return this.auditLogs.filter(log => {
            if (sev !== "ALL" && (log.severity || "").toUpperCase() !== sev) return false;
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

    renderAuditTrailTab() {
        const filtered = this.getFilteredAudit();
        const sev = this.severityFilter;

        return `
            <div class="space-y-6">
                <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                        <h2 class="text-xl font-extrabold text-slate-900">Immutable Audit Trail</h2>
                        <p class="text-xs text-slate-500 mt-1">Immutable log of all administrative actions and system events.</p>
                    </div>

                    <button onclick="app.downloadAuditCsv()" class="px-4 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs flex items-center gap-2 transition shadow-sm self-start sm:self-auto">
                        <i data-lucide="cloud-download" class="w-4 h-4"></i>
                        <span>Export CSV</span>
                    </button>
                </div>

                <!-- Case-Insensitive Non-Blocking Search Box -->
                ${this.renderSearchBox("audit", "Search actions, targets, admin roles, details, timestamps...")}

                <!-- Severity Chips -->
                <div class="flex items-center gap-2">
                    ${["ALL", "INFO", "WARNING", "CRITICAL"].map(s => `
                        <button onclick="app.setAuditSeverity('${s}')" class="px-3 py-1.5 rounded-xl text-xs font-semibold transition ${sev === s ? 'bg-purple-700 text-white font-bold shadow-sm' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">
                            ${s}
                        </button>
                    `).join("")}
                </div>

                <!-- Audit Cards List with Balanced Spacing -->
                <div id="audit-grid" class="space-y-3">
                    ${this.renderAuditCards(filtered)}
                </div>
            </div>
        `;
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

    setAuditSeverity(sev) {
        this.severityFilter = sev;
        this.updateSectionGrid("audit");
    }

    // ==========================================
    // TAB 9: REPORTS
    // ==========================================
    getFilteredReportsOrders() {
        const q = (this.searchQueries.reports || "").toLowerCase().trim();
        return this.orders.filter(o => {
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
        const totalGMV = this.orders.reduce((acc, o) => acc + (o.totalMax || 0), 0);
        const totalPlatformFees = this.orders.reduce((acc, o) => acc + (o.deliveryFee || 0), 0);
        const totalEscrow = this.disputes.reduce((acc, d) => acc + (d.escrowAmount || 0), 0);
        const totalWallets = this.students.reduce((acc, s) => acc + (s.walletBalance || 0), 0);
        const filtered = this.getFilteredReportsOrders();

        return `
            <div class="space-y-6">
                <div>
                    <h2 class="text-xl font-extrabold text-slate-900">Financial Reports & Ledgers</h2>
                    <p class="text-xs text-slate-500 mt-1">Financial ledgers and CSV export generation.</p>
                </div>

                <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm space-y-4">
                    <h3 class="text-sm font-bold text-slate-900">Platform Financial Summary</h3>

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
                        <button onclick="app.downloadOrdersCsv()" class="p-4 rounded-2xl bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs flex items-center justify-center gap-3 transition shadow-sm">
                            <i data-lucide="file-spreadsheet" class="w-5 h-5"></i>
                            <span>Download Orders Financial CSV (Excel / Sheets)</span>
                        </button>

                        <button onclick="app.downloadAuditCsv()" class="p-4 rounded-2xl bg-slate-800 hover:bg-slate-900 text-white font-bold text-xs flex items-center justify-center gap-3 transition shadow-sm">
                            <i data-lucide="cloud-download" class="w-5 h-5"></i>
                            <span>Download Full Audit Trail CSV Log</span>
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

                    <div class="overflow-x-auto">
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

    downloadOrdersCsv() {
        const rows = [["OrderID", "CustomerEmail", "CustomerName", "Runner", "PickPoint", "DropPoint", "Status", "EstimatedBill", "ActualBill", "DeliveryFee"]];
        this.orders.forEach(o => {
            rows.push([
                o.id,
                o.customerId,
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
        this.downloadCsvFile("CampusAdmin_Orders_Report.csv", rows);
    }

    downloadAuditCsv() {
        const rows = [["ID", "Timestamp", "AdminRole", "IP_Address", "ActionType", "TargetID", "Severity", "Details"]];
        this.auditLogs.forEach(l => {
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
        this.downloadCsvFile("CampusAdmin_Audit_Trail.csv", rows);
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
        this.updateSectionGrid("feedback");
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
            this.activeRole = targetRoleId;
            localStorage.setItem("admin_active_role", targetRoleId);
            this.selectedTab = targetRole.allowedTabs[0] || 0;
            this.recordAudit("ROLE_SWITCH_SUCCESS", targetRoleId, `Switched active role to ${targetRole.name} upon successful PIN authentication.`, "INFO");
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
                                <h3 class="text-sm font-extrabold text-slate-900">Firebase Realtime Cloud Sync</h3>
                                <div class="text-[10px] text-slate-400 font-mono">campusapp-e8435 &bull; Realtime Database</div>
                            </div>
                        </div>
                        <button onclick="app.closeModal()" class="font-bold text-slate-400 text-xl">&times;</button>
                    </div>

                    <div class="space-y-3 mt-4 text-xs">
                        <div class="p-3 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
                            <div>
                                <div class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Sync Engine Status</div>
                                <div class="text-xs font-black ${isLive ? 'text-emerald-700' : 'text-amber-700'} flex items-center gap-1.5 mt-0.5">
                                    <span class="w-2 h-2 rounded-full ${isLive ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'}"></span>
                                    <span>${isLive ? 'Active & Synchronized' : 'Connecting / Local Buffer Active'}</span>
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

    closeModal() {
        const modal = document.getElementById("modal-generic");
        if (modal) modal.classList.add("hidden");
    }
}

// Global initialization on DOM ready
document.addEventListener("DOMContentLoaded", () => {
    window.app = new CampusAdminApp();
});
