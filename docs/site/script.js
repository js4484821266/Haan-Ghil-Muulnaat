(function () {
  const FALLBACK_LANG = "en";
  const STORAGE_KEY = "haan_ghil_muulnaat_lang";

  function getByPath(obj, path) {
    return path.split(".").reduce((acc, key) => (acc && acc[key] !== undefined ? acc[key] : undefined), obj);
  }

  function setTextByKey(langPack, selector, attr) {
    document.querySelectorAll(selector).forEach((el) => {
      const key = el.getAttribute(attr);
      const value = getByPath(langPack, key);
      if (typeof value === "string") {
        el.textContent = value;
      }
    });
  }

  function renderList(id, items) {
    const root = document.getElementById(id);
    if (!root) return;
    root.innerHTML = "";
    if (!Array.isArray(items)) return;

    items.forEach((item) => {
      const li = document.createElement("li");
      li.textContent = item;
      root.appendChild(li);
    });
  }

  function renderResultsRows(rows) {
    const tbody = document.getElementById("resultsRows");
    if (!tbody) return;
    tbody.innerHTML = "";
    if (!Array.isArray(rows)) return;

    rows.forEach((row) => {
      const tr = document.createElement("tr");
      row.forEach((cell) => {
        const td = document.createElement("td");
        td.textContent = cell;
        tr.appendChild(td);
      });
      tbody.appendChild(tr);
    });
  }

  function applyLanguage(lang) {
    const pack = TRANSLATIONS[lang] || TRANSLATIONS[FALLBACK_LANG];
    const meta = pack.meta || { lang: FALLBACK_LANG, dir: "ltr" };

    document.documentElement.lang = meta.lang || FALLBACK_LANG;
    document.documentElement.dir = meta.dir || "ltr";

    setTextByKey(pack, "[data-i18n]", "data-i18n");

    document.querySelectorAll("[data-i18n-aria-label]").forEach((el) => {
      const key = el.getAttribute("data-i18n-aria-label");
      const value = getByPath(pack, key);
      if (typeof value === "string") {
        el.setAttribute("aria-label", value);
      }
    });

    renderList("whatBuiltList", getByPath(pack, "whatBuilt.items"));
    renderList("offlineList", getByPath(pack, "approach.offlineItems"));
    renderList("pipelineList", getByPath(pack, "approach.pipelineItems"));
    renderList("helpSteps", getByPath(pack, "help.steps"));
    renderResultsRows(getByPath(pack, "results.rows"));

    try {
      localStorage.setItem(STORAGE_KEY, lang);
    } catch (err) {
      // Ignore storage failures in restricted contexts.
    }
  }

  function getInitialLanguage() {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved && TRANSLATIONS[saved]) return saved;
    } catch (err) {
      // Ignore storage failures.
    }

    const browser = (navigator.language || "en").slice(0, 2).toLowerCase();
    if (TRANSLATIONS[browser]) return browser;
    return FALLBACK_LANG;
  }

  function initLanguageSwitcher() {
    const select = document.getElementById("languageSelect");
    if (!select) return;

    const initial = getInitialLanguage();
    select.value = TRANSLATIONS[initial] ? initial : FALLBACK_LANG;
    applyLanguage(select.value);

    select.addEventListener("change", (event) => {
      applyLanguage(event.target.value);
    });
  }

  document.addEventListener("DOMContentLoaded", initLanguageSwitcher);
})();
