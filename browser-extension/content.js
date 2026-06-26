// ===================== Dual UI: Search Results + Collections =====================
// Auto-detects page type and uses the right extraction strategy.

browser.runtime.onMessage.addListener((message) => {
  if (message.action === "crawlJobs") {
    return scrapeJobs(message.count || 10);
  }
});

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// ===================== Search Results UI (new) =====================
// Used on /search-results/?keywords=...
// Cards found via div[data-job-id] or [componentkey]

function extractSearchJob(card) {
  // ID from componentkey attribute: "job-card-component-ref-4427825984"
  const componentKey = card.getAttribute("componentkey");
  const jobId = componentKey?.match(/\d+$/)?.[0];
  if (!jobId) return null;

  // Title: first span[aria-hidden="true"] inside the card
  const titleSpan = card.querySelector('span[aria-hidden="true"]');
  const title = titleSpan?.innerText?.trim();
  if (!title) {
    console.log("  [extractSearchJob] No title for", jobId);
    return null;
  }

  // Company & Location: <p> elements, skipping the one that contains the title span
  const paragraphs = card.querySelectorAll("p");
  const titleP = titleSpan?.closest("p");
  let company = null;
  let location = null;
  let found = 0;
  for (const p of paragraphs) {
    if (p === titleP) continue;
    const text = p.innerText?.trim();
    if (!text) continue;
    if (found === 0) company = text;
    else if (found === 1) location = text;
    found++;
  }

  return { jobId, title, company: company || null, location: location || null };
}

// ===================== Collections UI (old) =====================
// Used on /collections/top-applicant/ and similar pages
// Cards found via li[data-occludable-job-id]

function extractCollectionJob(card) {
  const jobId = card.getAttribute("data-occludable-job-id");
  if (!jobId) return null;

  // Title: the job view link
  const titleLink = card.querySelector('a[href*="/jobs/view/"]');
  const title = titleLink?.innerText?.trim();
  if (!title) {
    console.log("  [extractCollectionJob] No title for", jobId);
    return null;
  }

  // Company: Art Deco design system subtitle
  const subtitleEl = card.querySelector(".artdeco-entity-lockup__subtitle");
  const company = subtitleEl?.innerText?.trim() || null;

  // Location: metadata wrapper
  const metadataEl = card.querySelector(".job-card-container__metadata-wrapper");
  const location = metadataEl?.innerText?.trim() || null;

  return { jobId, title, company, location };
}

// ===================== Scraping engine =====================

async function scrapeJobs(requestedCount) {
  // --- Auto-detect page type ---
  const isSearchResults = window.location.href.includes("/search-results/");
  console.log(`[scrapeJobs] Page: ${isSearchResults ? "SEARCH" : "COLLECTIONS"}, requesting ${requestedCount} jobs`);

  // --- Configure selectors based on page type ---
  let cardSelector, idExtractor, extractFn;

  if (isSearchResults) {
    // Search results: cards have componentkey attribute
    cardSelector = "[componentkey^='job-card-component-ref-']";
    idExtractor = (el) => el.getAttribute("componentkey")?.match(/\d+$/)?.[0];
    extractFn = extractSearchJob;
  } else {
    // Collections: cards are li elements with data-occludable-job-id
    cardSelector = "li[data-occludable-job-id]";
    idExtractor = (el) => el.getAttribute("data-occludable-job-id");
    extractFn = extractCollectionJob;
  }

  // --- Find scroll container ---
  const sentinel = document.querySelector("[data-results-list-top-scroll-sentinel]");
  const scrollContainer = sentinel?.parentElement
    || document.querySelector(".scaffold-layout__list")
    || document.querySelector(".jobs-search-results-list")
    || document.querySelector('[role="list"]')
    || document.body;

  const MAX_ATTEMPTS = 15;
  const SCROLL_DELAY_MS = 1200;
  const allIds = new Set();
  let jobs = [];
  const scrollPositions = [0.3, 0.6, 0.85, 0.95, 0.7, 0.4, 0.1, 0.5, 0.9, 0.99, 0.3, 0.7, 0.95, 0.5, 0.99];
  let consecutiveEmptyScrolls = 0;

  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    const prevCount = allIds.size;

    // --- Wait or scroll ---
    if (attempt < 3) {
      await delay(SCROLL_DELAY_MS);
    } else {
      const pct = scrollPositions[Math.min(attempt - 3, scrollPositions.length - 1)];
      scrollContainer.scrollTop = scrollContainer.scrollHeight * pct;
      await delay(SCROLL_DELAY_MS);
    }

    // --- Collect cards and extract ---
    const cards = document.querySelectorAll(cardSelector);
    const currentIds = [...cards].map(el => idExtractor(el)).filter(Boolean);

    for (const card of cards) {
      const id = idExtractor(card);
      if (!id || allIds.has(id)) continue;

      const job = extractFn(card);
      if (job) {
        allIds.add(id);
        jobs.push(job);
      }
      // stub — retry next attempt
    }

    console.log(
      `[scrapeJobs] Attempt ${attempt}: ${cards.length} cards, ${jobs.length} extracted so far`
    );
    if (currentIds.length) {
      console.log("  IDs:", currentIds);
    }

    if (jobs.length >= requestedCount) break;

    if (allIds.size === prevCount) {
      consecutiveEmptyScrolls++;
      if (consecutiveEmptyScrolls >= 3) {
        console.log("[scrapeJobs] Stopping: 3 consecutive scrolls returned no new jobs");
        break;
      }
    } else {
      consecutiveEmptyScrolls = 0;
    }
  }

  jobs = jobs.slice(0, requestedCount);
  console.log(`[scrapeJobs] Done. Collected ${jobs.length} jobs:`, jobs);
  return { jobs };
}

// ===================== Description extraction (unrelated to crawl) =====================

browser.runtime.onMessage.addListener((message) => {
  if (message.action === "extractLinkedInDescription") {
    const description = extractLinkedInDescription();
    return Promise.resolve({
      jobId: message.jobId,
      description
    });
  }
});

function extractLinkedInDescription() {
  const textBox = document.querySelector('[data-testid="expandable-text-box"]');
  if (!textBox) {
    console.log("No description box found");
    return null;
  }
  return textBox.innerText
    .replace(/…\s*mer$/i, "")
    .replace(/\n\s*\n\s*\n+/g, "\n\n")
    .trim();
}
