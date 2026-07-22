// IntelliTrip — vanilla JS SPA (no frameworks, no build step)
(function () {
  "use strict";

  const API_BASE = "/api";
  const TOKEN_KEY = "intellitrip_user";

  /* ----------------------------- Storage / Auth ----------------------------- */
  function getStoredUser() {
    try {
      const raw = localStorage.getItem(TOKEN_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) {
      return null;
    }
  }
  function setStoredUser(user) {
    if (user) localStorage.setItem(TOKEN_KEY, JSON.stringify(user));
    else localStorage.removeItem(TOKEN_KEY);
  }

  const state = {
    user: getStoredUser(),
  };

  class UnauthorizedError extends Error {
    constructor() {
      super("Unauthorized");
      this.name = "UnauthorizedError";
    }
  }

  /* --------------------------------- API ------------------------------------ */
  async function request(path, options) {
    options = options || {};
    const user = getStoredUser();
    const headers = new Headers(options.headers);
    if (!headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    if (user) headers.set("X-User-Id", user.id);

    const res = await fetch(API_BASE + path, {
      credentials: "include",
      headers: headers,
      method: options.method || "GET",
      body: options.body,
    });

    if (res.status === 401) throw new UnauthorizedError();
    if (!res.ok) {
      let message = "Request failed with status " + res.status;
      try {
        const data = await res.json();
        if (data && data.error) message = data.error;
      } catch (e) {}
      throw new Error(message);
    }
    if (res.status === 204) return undefined;
    return res.json();
  }

  const api = {
    get: (path) => request(path),
    post: (path, body) => request(path, { method: "POST", body: body ? JSON.stringify(body) : undefined }),
    put: (path, body) => request(path, { method: "PUT", body: body ? JSON.stringify(body) : undefined }),
    patch: (path, body) => request(path, { method: "PATCH", body: body ? JSON.stringify(body) : undefined }),
    del: (path) => request(path, { method: "DELETE" }),
  };

  async function login(email, password) {
    const data = await api.post("/auth/login", { email, password });
    setStoredUser(data);
    state.user = data;
  }
  async function signup(name, email, password) {
    const data = await api.post("/auth/signup", { name, email, password });
    setStoredUser(data);
    state.user = data;
  }
  function logout() {
    setStoredUser(null);
    state.user = null;
  }

  /* ------------------------------- Utilities -------------------------------- */
  function esc(str) {
    if (str === null || str === undefined) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }
  function cap(s) {
    if (!s) return "";
    return s.charAt(0).toUpperCase() + s.slice(1);
  }
  function usd(n) {
    if (n == null) return "0";
    return "$" + Math.round(n).toLocaleString();
  }
  function money(n) {
    if (n == null) return "$0";
    return "$" + Math.round(n).toLocaleString();
  }

  /* ------------------------------- Router ----------------------------------- */
  const routes = [
    { path: /^\/$/, public: true, render: Landing },
    { path: /^\/login$/, public: true, render: Login },
    { path: /^\/signup$/, public: true, render: Signup },
    { path: /^\/dashboard$/, render: Dashboard },
    { path: /^\/generate$/, render: Generate },
    { path: /^\/trips$/, render: Trips },
    { path: /^\/trips\/([^/]+)$/, render: TripDetail },
    { path: /^\/saved$/, render: Saved },
    { path: /^\/admin$/, admin: true, render: Admin },
  ];

  function currentPath() {
    return location.hash.replace(/^#/, "") || "/";
  }

  function navigate(path) {
    location.hash = path;
  }

  function resolve() {
    const path = currentPath();
    const user = state.user;

    if (user && (path === "/" || path === "/login" || path === "/signup")) {
      return Dashboard();
    }

    for (const r of routes) {
      const m = path.match(r.path);
      if (!m) continue;

      if (!r.public && !user) {
        return mount(ProtectedShell(LoginFragment()));
      }
      if (r.admin && user && user.role !== "ADMIN") {
        return mount(ProtectedShell(NotFoundFragment()));
      }
      return r.render(m);
    }

    return mount(ProtectedShell(NotFoundFragment()));
  }

  function render() {
    resolve();
  }

  window.addEventListener("hashchange", render);
  window.addEventListener("DOMContentLoaded", render);
  if (document.readyState !== "loading") render();

  /* ------------------------------- Shells ----------------------------------- */
  function Navbar() {
    const user = state.user;
    const links = [
      { to: "/dashboard", label: "Dashboard" },
      { to: "/generate", label: "Plan Trip" },
      { to: "/trips", label: "My Trips" },
      { to: "/saved", label: "Saved" },
    ];
    const path = currentPath();

    const navLinks = user
      ? links
          .map((l) => {
            const active = path === l.to ? " active" : "";
            return `<a class="nav-link${active}" href="#${l.to}">${l.label}</a>`;
          })
          .join("")
      : "";

    const adminLink =
      user && user.role === "ADMIN"
        ? `<a class="nav-link${path === "/admin" ? " active" : ""}" href="#/admin">Admin</a>`
        : "";

    const authSection = user
      ? `<span class="text-sm text-muted-foreground" style="display:none">${esc(user.name)}</span>
         <button class="btn btn-outline btn-sm" id="logout-btn">Log out</button>`
      : `<a class="btn btn-ghost btn-sm" href="#/login">Sign in</a>
         <a class="btn btn-primary btn-sm" href="#/signup">Sign up</a>`;

    const el = document.createElement("header");
    el.className = "sticky top-0 z-50 border-b bg-background-80 backdrop-blur";
    el.innerHTML = `
      <div class="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
        <a href="#/" class="text-xl font-bold">Intelli<span class="text-primary-60">Trip</span></a>
        <nav class="hidden items-center gap-6 md:flex">
          ${navLinks}
          ${adminLink}
        </nav>
        <div class="flex items-center gap-3">
          ${authSection}
        </div>
      </div>`;

    if (user) {
      el.querySelector("#logout-btn").addEventListener("click", async () => {
        logout();
        navigate("/login");
      });
    }
    return el;
  }

  function ProtectedShell(content) {
    const wrap = document.createElement("div");
    wrap.className = "min-h-screen";
    const nav = Navbar();
    const main = document.createElement("main");
    main.className = "mx-auto max-w-6xl px-4 py-8";
    main.appendChild(content);
    wrap.appendChild(nav);
    wrap.appendChild(main);
    return wrap;
  }

  function PublicShell(content) {
    const wrap = document.createElement("div");
    wrap.className = "min-h-screen";
    wrap.appendChild(Navbar());
    const main = document.createElement("main");
    main.appendChild(content);
    wrap.appendChild(main);
    return wrap;
  }

  function NotFoundFragment() {
    const d = document.createElement("div");
    d.innerHTML = `<h1 class="text-2xl font-bold">Not found</h1><p class="text-muted-foreground mt-2">Page not found. <a class="text-primary" href="#/dashboard">Go home</a></p>`;
    return d;
  }

  function mount(node) {
    const app = document.getElementById("app");
    app.innerHTML = "";
    app.appendChild(node);
    window.scrollTo(0, 0);
  }

  /* ------------------------------- Pages ------------------------------------ */
  function Landing() {
    const features = [
      { title: "Instant Itineraries", body: "Get a full multi-day plan with activities, dining, and transport in one click." },
      { title: "Budget Aware", body: "Pick a budget tier and we balance accommodation, food, and activities for you." },
      { title: "Save & Track", body: "Keep all your trips organized and track upcoming days and spend at a glance." },
    ];
    const el = document.createElement("div");
    el.innerHTML = `
      <section class="relative overflow-hidden">
        <div class="mx-auto max-w-6xl px-4 py-24 text-center">
          <span class="badge mb-4">AI-Powered Travel Planning</span>
          <h1 class="mx-auto max-w-3xl text-4xl font-bold leading-tight sm:text-6xl">
            Plan smarter trips with <span class="text-primary-60">IntelliTrip</span>
          </h1>
          <p class="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground">
            Generate personalized day-by-day itineraries in seconds. Tailor every trip to
            your budget, interests, and travel style.
          </p>
          <div class="mt-8 flex items-center justify-center gap-4">
            <a class="btn btn-primary btn-lg" href="#/signup">Get started</a>
            <a class="btn btn-outline btn-lg" href="#/login">Sign in</a>
          </div>
        </div>
      </section>
      <section class="mx-auto grid max-w-6xl gap-6 px-4 pb-24 md:grid-cols-3">
        ${features
          .map(
            (f) => `
          <div class="card">
            <h3 class="text-lg font-semibold">${f.title}</h3>
            <p class="mt-2 text-sm text-muted-foreground">${f.body}</p>
          </div>`
          )
          .join("")}
      </section>`;
    return PublicShell(el);
  }

  function LoginFragment() {
    const el = document.createElement("div");
    el.innerHTML = `
      <div class="mx-auto flex max-w-md flex-col px-4 py-16">
        <h1 class="text-2xl font-bold">Welcome back</h1>
        <p class="mt-1 text-sm text-muted-foreground">Sign in to plan your next trip.</p>
        <div id="error-slot"></div>
        <form id="login-form" class="mt-6 space-y-4">
          <div>
            <label class="label" for="email">Email</label>
            <input id="email" type="email" required class="input" placeholder="you@example.com" />
          </div>
          <div>
            <label class="label" for="password">Password</label>
            <input id="password" type="password" required class="input" placeholder="••••••••" />
          </div>
          <button type="submit" class="btn btn-primary w-full" id="submit-btn">Sign in</button>
        </form>
        <p class="mt-6 text-center text-sm text-muted-foreground">
          Don't have an account? <a class="font-medium text-primary" href="#/signup">Sign up</a>
        </p>
        <p class="mt-4 text-center text-xs text-muted-foreground">
          Demo admin: admin@intellitrip.com / admin123
        </p>
      </div>`;

    el.querySelector("#login-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const email = el.querySelector("#email").value;
      const password = el.querySelector("#password").value;
      const slot = el.querySelector("#error-slot");
      const btn = el.querySelector("#submit-btn");
      slot.innerHTML = "";
      btn.disabled = true;
      btn.textContent = "Signing in…";
      try {
        await login(email, password);
        navigate("/dashboard");
      } catch (err) {
        slot.innerHTML = `<div class="mt-4 error-box">${esc(err.message || "Invalid email or password.")}</div>`;
      } finally {
        btn.disabled = false;
        btn.textContent = "Sign in";
      }
    });
    return el;
  }

  function Login() {
    return PublicShell(LoginFragment());
  }

  function Signup() {
    const el = document.createElement("div");
    el.innerHTML = `
      <div class="mx-auto flex max-w-md flex-col px-4 py-16">
        <h1 class="text-2xl font-bold">Create your account</h1>
        <p class="mt-1 text-sm text-muted-foreground">Start planning in seconds.</p>
        <div id="error-slot"></div>
        <form id="signup-form" class="mt-6 space-y-4">
          <div>
            <label class="label" for="name">Full name</label>
            <input id="name" required class="input" />
          </div>
          <div>
            <label class="label" for="email">Email</label>
            <input id="email" type="email" required class="input" />
          </div>
          <div>
            <label class="label" for="password">Password</label>
            <input id="password" type="password" required class="input" placeholder="At least 6 characters" />
          </div>
          <button type="submit" class="btn btn-primary w-full" id="submit-btn">Create account</button>
        </form>
        <p class="mt-6 text-center text-sm text-muted-foreground">
          Already have an account? <a class="font-medium text-primary" href="#/login">Sign in</a>
        </p>
      </div>`;

    el.querySelector("#signup-form").addEventListener("submit", async (e) => {
      e.preventDefault();
      const name = el.querySelector("#name").value;
      const email = el.querySelector("#email").value;
      const password = el.querySelector("#password").value;
      const slot = el.querySelector("#error-slot");
      const btn = el.querySelector("#submit-btn");
      slot.innerHTML = "";
      if (password.length < 6) {
        slot.innerHTML = `<div class="mt-4 error-box">Password must be at least 6 characters.</div>`;
        return;
      }
      btn.disabled = true;
      btn.textContent = "Creating…";
      try {
        await signup(name, email, password);
        navigate("/dashboard");
      } catch (err) {
        slot.innerHTML = `<div class="mt-4 error-box">${esc(err.message || "Could not create account.")}</div>`;
      } finally {
        btn.disabled = false;
        btn.textContent = "Create account";
      }
    });
    return PublicShell(el);
  }

  function Stat(label, value) {
    return `<div class="card">
      <p class="text-sm text-muted-foreground">${label}</p>
      <p class="mt-1 text-2xl font-bold">${value}</p>
    </div>`;
  }

  function Dashboard() {
    const el = document.createElement("div");
    el.innerHTML = `<p class="text-muted-foreground">Loading dashboard…</p>`;
    const shell = ProtectedShell(el);
    mount(shell);

    Promise.all([
      api.get("/trips").catch(() => []),
      api.get("/notifications").catch(() => []),
    ]).then(([trips, notifications]) => {
      trips = trips || [];
      notifications = notifications || [];
      const user = state.user;
      const upcoming = trips.filter((t) => t.status === "upcoming");
      const drafts = trips.filter((t) => t.status === "draft");
      const upcomingDays = upcoming.reduce((s, t) => s + (t.days || 0), 0);
      const budget = upcoming.reduce((s, t) => s + (t.budgetUsd || 0), 0);

      el.innerHTML = `
        <div class="mb-8 flex items-center justify-between">
          <div>
            <h1 class="text-3xl font-bold">Welcome, ${esc((user && user.name || "").split(" ")[0])}</h1>
            <p class="text-muted-foreground">Here's what's coming up on your trips.</p>
          </div>
          <a class="btn btn-primary" href="#/generate">Plan a trip</a>
        </div>
        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          ${Stat("Total trips", String(trips.length))}
          ${Stat("Upcoming days", String(upcomingDays))}
          ${Stat("Budget tracked", usd(budget))}
          ${Stat("Notifications", String(notifications.length))}
        </div>
        ${
          notifications.length
            ? `<div class="card mt-6">
          <h2 class="mb-3 text-lg font-semibold">Notifications</h2>
          <ul class="space-y-2">
            ${notifications
              .map((n) => `<li class="rounded-lg bg-muted px-4 py-3 text-sm">${esc(n.message)}</li>`)
              .join("")}
          </ul>
        </div>`
            : ""
        }
        <section class="mt-8">
          <h2 class="mb-4 text-xl font-semibold">Upcoming trips</h2>
          ${
            upcoming.length === 0
              ? `<div class="card text-muted-foreground">No upcoming trips yet. <a class="text-primary" href="#/generate">Plan one now</a>.</div>`
              : `<div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">${upcoming.map(TripCardHTML).join("")}</div>`
          }
        </section>
        ${
          drafts.length
            ? `<section class="mt-8">
          <h2 class="mb-4 text-xl font-semibold">Drafts</h2>
          <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">${drafts.map(TripCardHTML).join("")}</div>
        </section>`
            : ""
        }`;
    });

    return shell;
  }

  function TripCardHTML(trip) {
    const status = trip.status || "draft";
    const statusLabel = cap(status);
    const statusClass = status === "upcoming" ? "status-upcoming" : status === "draft" ? "status-draft" : "status-completed";
    return `<a href="#/trips/${esc(trip.id)}" class="card trip-card">
      <div class="flex items-start justify-between">
        <div>
          <h3 class="font-semibold">${esc(trip.destination)}</h3>
          <p class="text-sm text-muted-foreground">${trip.days} days · ${cap(trip.travelType || "Solo")}</p>
        </div>
        <span class="badge ${statusClass}">${statusLabel}</span>
      </div>
      <div class="mt-4 flex items-center justify-between text-sm text-muted-foreground">
        <span class="capitalize">${esc(trip.budget || "—")} budget</span>
        ${trip.budgetUsd != null ? `<span>${usd(trip.budgetUsd)}</span>` : ""}
      </div>
    </a>`;
  }

  function Generate() {
    const BUDGETS = ["budget friendly", "medium", "luxury"];
    const TRAVELERS = ["solo", "couple", "family", "friends"];
    const INTERESTS = ["culture", "food", "adventure", "nature", "nightlife", "relaxation", "shopping", "history"];

    let destination = "";
    let days = 5;
    let budget = "medium";
    let traveler = "solo";
    let interests = ["culture", "food"];
    let loading = false;
    let error = null;
    let result = null;
    let saved = false;

    const el = document.createElement("div");
    const shell = ProtectedShell(el);

    function budgetButtons() {
      return BUDGETS.map(
        (b) =>
          `<button type="button" class="btn btn-sm ${budget === b ? "btn-primary" : "btn-outline"}" data-budget="${b}">${cap(b)}</button>`
      ).join("");
    }
    function interestButtons() {
      return INTERESTS.map(
        (i) =>
          `<button type="button" class="btn btn-sm ${interests.includes(i) ? "btn-primary" : "btn-outline"}" data-interest="${i}">${cap(i)}</button>`
      ).join("");
    }
    function travelerOptions() {
      return TRAVELERS.map((t) => `<option value="${t}" ${t === traveler ? "selected" : ""}>${cap(t)}</option>`).join("");
    }

    function renderResult() {
      if (!result) return "";
      return `<div class="mt-10">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="text-2xl font-bold">${esc(result.title)}</h2>
          <button class="btn btn-primary" id="save-btn" ${loading || saved ? "disabled" : ""}>${saved ? "Saved" : "Save trip"}</button>
        </div>
        ${ItineraryHTML(result)}
      </div>`;
    }

    function renderForm() {
      return `<form id="gen-form" class="card max-w-2xl space-y-5">
        <div>
          <label class="label">Destination</label>
          <input class="input" id="destination" placeholder="e.g. Tokyo, Paris, Bali" value="${esc(destination)}" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="label">Number of days</label>
            <input type="number" min="1" max="30" class="input" id="days" value="${days}" />
          </div>
          <div>
            <label class="label">Travelers</label>
            <select class="input" id="traveler">${travelerOptions()}</select>
          </div>
        </div>
        <div>
          <label class="label">Budget</label>
          <div class="flex gap-2" id="budget-group">${budgetButtons()}</div>
        </div>
        <div>
          <label class="label">Interests</label>
          <div class="flex flex-wrap gap-2" id="interest-group">${interestButtons()}</div>
        </div>
        <div id="error-slot"></div>
        <button type="submit" class="btn btn-primary w-full" id="gen-btn" ${loading ? "disabled" : ""}>
          ${loading && !result ? "Generating…" : "Generate itinerary"}
        </button>
      </form>`;
    }

    function paint() {
      el.innerHTML = `
        <h1 class="mb-2 text-3xl font-bold">Plan a trip</h1>
        <p class="mb-8 text-muted-foreground">Tell us about your trip and we'll build the itinerary.</p>
        ${renderForm()}
        ${error && !loading ? `<div class="mt-4 error-box">${esc(error)}</div>` : ""}
        ${renderResult()}`;

      el.querySelector("#gen-form").addEventListener("submit", handleGenerate);
      el.querySelector("#budget-group").addEventListener("click", (e) => {
        const b = e.target.getAttribute("data-budget");
        if (b) {
          budget = b;
          el.querySelector("#budget-group").innerHTML = budgetButtons();
        }
      });
      el.querySelector("#interest-group").addEventListener("click", (e) => {
        const i = e.target.getAttribute("data-interest");
        if (i) {
          interests = interests.includes(i) ? interests.filter((x) => x !== i) : [...interests, i];
          el.querySelector("#interest-group").innerHTML = interestButtons();
        }
      });
      const saveBtn = el.querySelector("#save-btn");
      if (saveBtn) saveBtn.addEventListener("click", handleSave);
    }

    async function handleGenerate(e) {
      e.preventDefault();
      error = null;
      result = null;
      saved = false;
      destination = el.querySelector("#destination").value;
      days = Number(el.querySelector("#days").value) || 1;
      traveler = el.querySelector("#traveler").value;
      if (!destination.trim()) {
        error = "Please enter a destination.";
        paint();
        return;
      }
      loading = true;
      paint();
      try {
        const data = await api.post("/generate-trip", {
          city: destination.trim(),
          numberOfDays: days,
          budget,
          travelers: traveler,
          interests,
        });
        result = data;
      } catch (err) {
        error = err.message || "Failed to generate trip.";
      } finally {
        loading = false;
        paint();
      }
    }

    async function handleSave() {
      if (!result) return;
      loading = true;
      paint();
      try {
        await api.post("/trips", result);
        saved = true;
        navigate("/trips");
      } catch (err) {
        error = err.message || "Failed to save trip.";
        loading = false;
        paint();
      }
    }

    paint();
    return shell;
  }

  function ItineraryHTML(it) {
    const totalCost =
      it.costBreakdown != null
        ? Object.values(it.costBreakdown).reduce((a, b) => a + (b || 0), 0)
        : it.totalBudget != null
        ? it.totalBudget
        : null;

    const days = (it.days || [])
      .map(
        (day) => `
      <div class="card">
        <h3 class="mb-3 text-lg font-semibold">Day ${day.day}: ${esc(day.title)}</h3>
        ${day.theme ? `<p class="mb-3 text-sm text-muted-foreground">${esc(day.theme)}</p>` : ""}
        <ol class="space-y-3">
          ${(day.activities || [])
            .map(
              (a) => `
            <li class="flex gap-4">
              <span class="shrink-0 rounded-md bg-muted px-2 py-1 text-xs font-medium text-muted-foreground">${esc(a.time)}</span>
              <div>
                <p class="font-medium">${esc(a.activity)}</p>
                <p class="text-sm text-muted-foreground">${esc(a.description)}</p>
                ${a.estimatedCost != null ? `<p class="text-xs text-muted-foreground">~${money(a.estimatedCost)}</p>` : ""}
              </div>
            </li>`
            )
            .join("")}
        </ol>
      </div>`
      )
      .join("");

    const accom = it.accommodation && it.accommodation.length
      ? `<div class="card">
        <h3 class="mb-3 text-lg font-semibold">Where to stay</h3>
        <div class="grid gap-3 sm:grid-cols-2">
          ${it.accommodation
            .map(
              (h) => `
            <div class="rounded-lg border border-border p-3">
              <div class="flex items-center justify-between">
                <p class="font-medium">${esc(h.name)}</p>
                <span class="text-sm text-muted-foreground">${esc(h.rating)}</span>
              </div>
              <p class="text-sm text-muted-foreground">${esc(h.type)} · ${money(h.price)}/night</p>
              <p class="mt-1 text-xs text-muted-foreground">${esc(h.description)}</p>
            </div>`
            )
            .join("")}
        </div>
      </div>`
      : "";

    const cost = it.costBreakdown
      ? `<div class="card">
        <h3 class="mb-3 text-lg font-semibold">Estimated cost breakdown</h3>
        <div class="space-y-2 text-sm">
          ${row("Accommodation", it.costBreakdown.accommodation)}
          ${row("Food", it.costBreakdown.food)}
          ${row("Activities", it.costBreakdown.activities)}
          ${row("Transportation", it.costBreakdown.transportation)}
          ${row("Other", it.costBreakdown.other)}
          ${
            totalCost != null
              ? `<div class="mt-2 flex justify-between border-t pt-2 font-semibold"><span>Total</span><span>${money(totalCost)}</span></div>`
              : ""
          }
        </div>
      </div>`
      : "";

    const tips = it.tips && it.tips.length
      ? `<div class="card">
        <h3 class="mb-3 text-lg font-semibold">Tips</h3>
        <ul class="list-inside list-disc space-y-1 text-sm text-muted-foreground">
          ${it.tips.map((t) => `<li>${esc(t)}</li>`).join("")}
        </ul>
      </div>`
      : "";

    return `<div class="space-y-6">
      ${it.overview ? `<p class="text-muted-foreground">${esc(it.overview)}</p>` : ""}
      ${days}
      ${accom}
      ${cost}
      ${tips}
    </div>`;
  }

  function row(label, value) {
    return `<div class="flex justify-between">
      <span class="text-muted-foreground">${label}</span>
      <span>${money(value || 0)}</span>
    </div>`;
  }

  function Trips() {
    const el = document.createElement("div");
    el.innerHTML = `<h1 class="mb-6 text-3xl font-bold">My trips</h1><p class="text-muted-foreground">Loading…</p>`;
    const shell = ProtectedShell(el);
    mount(shell);

    api
      .get("/trips")
      .then((data) => {
        data = data || [];
        if (data.length === 0) {
          el.innerHTML = `<h1 class="mb-6 text-3xl font-bold">My trips</h1>
            <div class="card text-muted-foreground">You haven't created any trips yet.</div>`;
          return;
        }
        el.innerHTML = `<h1 class="mb-6 text-3xl font-bold">My trips</h1>
          <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">${data.map(TripCardHTML).join("")}</div>`;
      })
      .catch((err) => {
        el.innerHTML = `<h1 class="mb-6 text-3xl font-bold">My trips</h1>
          <div class="card border-border text-destructive">${esc(err.message)}</div>`;
      });

    return shell;
  }

  function TripDetail(match) {
    const id = match[1];
    const el = document.createElement("div");
    el.innerHTML = `<p class="text-muted-foreground">Loading…</p>`;
    const shell = ProtectedShell(el);
    mount(shell);

    let busy = false;

    function paint(data) {
      const STATUSES = ["UPCOMING", "DRAFT", "COMPLETED"];
      const statusOptions = STATUSES.map(
        (s) => `<option value="${s}" ${data.status === s.toLowerCase() ? "selected" : ""}>${cap(s.toLowerCase())}</option>`
      ).join("");

      el.innerHTML = `
        <div class="mb-6 flex items-start justify-between">
          <div>
            <h1 class="text-3xl font-bold">${esc(data.destination)}</h1>
            <p class="text-muted-foreground">${data.days} days · ${cap(data.travelType)} · ${esc(data.country || "—")}</p>
          </div>
          <div class="flex gap-2">
            <select class="input w-auto" id="status-select" ${busy ? "disabled" : ""}>${statusOptions}</select>
            <button class="btn btn-outline" id="delete-btn" ${busy ? "disabled" : ""}>Delete</button>
          </div>
        </div>
        <div class="grid gap-4 sm:grid-cols-3">
          <div class="card"><p class="text-sm text-muted-foreground">Budget</p><p class="mt-1 font-semibold capitalize">${esc((data.budget || "").toLowerCase())}</p></div>
          <div class="card"><p class="text-sm text-muted-foreground">Budget (USD)</p><p class="mt-1 font-semibold">${usd(data.budgetUsd)}</p></div>
          <div class="card"><p class="text-sm text-muted-foreground">Start date</p><p class="mt-1 font-semibold">${esc(data.startDate || "—")}</p></div>
        </div>
        ${
          data.interests && data.interests.length
            ? `<div class="mt-6 flex flex-wrap gap-2">${data.interests.map((i) => `<span class="badge">${esc(i)}</span>`).join("")}</div>`
            : ""
        }`;

      el.querySelector("#status-select").addEventListener("change", async (e) => {
        busy = true;
        await api.patch(`/trips/${id}/status`, { status: e.target.value });
        busy = false;
        reload();
      });
      el.querySelector("#delete-btn").addEventListener("click", async () => {
        if (!confirm("Delete this trip?")) return;
        busy = true;
        await api.del(`/trips/${id}`);
        navigate("/trips");
      });
    }

    function reload() {
      api.get(`/trips/${id}`).then(paint).catch((err) => {
        el.innerHTML = `<div class="card border-border text-destructive">${esc(err.message)}</div>`;
      });
    }

    api
      .get(`/trips/${id}`)
      .then(paint)
      .catch((err) => {
        el.innerHTML = `<div class="card border-border text-destructive">${esc(err.message)}</div>`;
      });

    return shell;
  }

  function Saved() {
    const demo = [
      { name: "Eiffel Tower", location: "Paris, France", rating: 4.7, category: "Landmark" },
      { name: "Fushimi Inari", location: "Kyoto, Japan", rating: 4.8, category: "Temple" },
      { name: "Sagrada Família", location: "Barcelona, Spain", rating: 4.9, category: "Architecture" },
    ];
    const el = document.createElement("div");
    el.innerHTML = `
      <h1 class="mb-6 text-3xl font-bold">Saved places</h1>
      <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        ${demo
          .map(
            (p) => `
          <div class="card">
            <div class="flex items-center justify-between">
              <h3 class="font-semibold">${esc(p.name)}</h3>
              <span class="badge">★ ${p.rating}</span>
            </div>
            <p class="text-sm text-muted-foreground">${esc(p.location)}</p>
            <span class="badge mt-3">${esc(p.category)}</span>
          </div>`
          )
          .join("")}
      </div>`;
    return ProtectedShell(el);
  }

  function Admin() {
    const el = document.createElement("div");
    el.innerHTML = `<p class="text-muted-foreground">Loading…</p>`;
    const shell = ProtectedShell(el);
    mount(shell);

    api
      .get("/admin/analytics")
      .then((data) => {
        if (!data) return;
        el.innerHTML = `
          <h1 class="mb-6 text-3xl font-bold">Admin dashboard</h1>
          <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            ${Stat("Total users", (data.totalUsers || 0).toLocaleString())}
            ${Stat("Total trips", (data.totalTrips || 0).toLocaleString())}
            ${Stat("Avg trip days", String(data.avgTripDays || 0))}
            ${Stat("Generated this week", String(data.generatedThisWeek || 0))}
          </div>
          <div class="mt-8 grid gap-6 lg:grid-cols-2">
            <div class="card">
              <h2 class="mb-4 text-lg font-semibold">Weekly signups</h2>
              ${barChart(data.weeklySignups || [])}
            </div>
            <div class="card">
              <h2 class="mb-4 text-lg font-semibold">Trips by type</h2>
              ${pieChart(data.tripsByType || [])}
            </div>
          </div>`;
      })
      .catch((err) => {
        el.innerHTML = `<div class="card border-border text-destructive">${esc(err.message)}</div>`;
      });

    return shell;
  }

  function barChart(data) {
    const max = Math.max(1, ...data.map((d) => d.users || 0));
    const bars = data
      .map(
        (d) => `
      <div class="bar-wrap">
        <span class="bar-value">${d.users}</span>
        <div class="bar" style="height:${(d.users / max) * 100}%"></div>
        <span class="bar-label">${esc(d.day)}</span>
      </div>`
      )
      .join("");
    return `<div class="chart">${bars}</div>`;
  }

  function pieChart(data) {
    const colors = ["#0f172a", "#64748b", "#0ea5e9", "#f59e0b"];
    const total = data.reduce((s, d) => s + (d.count || 0), 0) || 1;
    const slices = data
      .map((d, i) => {
        const pct = ((d.count || 0) / total) * 100;
        return `<div style="height:${pct}%;background:${colors[i % colors.length]};border-radius:4px 4px 0 0"></div>`;
      })
      .join("");
    const legend = data
      .map(
        (d, i) =>
          `<span class="item"><span class="dot" style="background:${colors[i % colors.length]}"></span>${esc(d.type)} (${d.count})</span>`
      )
      .join("");
    return `<div style="display:flex;height:200px;gap:2px;align-items:flex-end">${slices}</div><div class="legend">${legend}</div>`;
  }
})();
