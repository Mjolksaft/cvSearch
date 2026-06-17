browser.runtime.onMessage.addListener((message) => {
  if (message.action === "crawlJobs") {
    return scrapeJobs(message.count || 10);
  }
});

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Scrolls the LinkedIn job list container to the bottom.
 */
function scrollJobList() {
  const container = document.querySelector(".jobs-search-results-list")
    || document.querySelector(".scaffold-layout__list")
    || document.querySelector(".jobs-search-results")
    || document.body;
  container.scrollTop = container.scrollHeight;
}

/**
 * Extract job data from a card DOM element.
 * Returns null if the card has no real title or company (placeholder / blank).
 */
function extractJob(card) {
  const titleEl = card.querySelector(".job-card-list__title--link");
  const companyEl = card.querySelector(".artdeco-entity-lockup__subtitle");
  const locationEl = card.querySelector(".artdeco-entity-lockup__caption");

  const jobId = card.getAttribute("data-occludable-job-id");
  const title = titleEl?.innerText?.trim();
  const company = companyEl?.innerText?.trim();
  const location = locationEl?.innerText?.trim();

  if (!title || !company) return null;
  return { jobId, title, company, location: location || null };
}

/**
 * Scrape up to `requestedCount` valid jobs from the LinkedIn page.
 *
 * Strategy for speed:
 *   - 1st pass: grab whatever cards are already rendered.
 *   - If cards exist but none have data yet (still hydrating), wait briefly and re-check.
 *   - Then scroll to lazy-load more, checking whether new cards actually appeared.
 *   - Bail as soon as we have enough, or if scrolling adds nothing new.
 */
async function scrapeJobs(requestedCount) {
  const SCROLL_DELAY_MS = 800;
  const HYDRATE_WAIT_MS = 600;
  const MAX_ATTEMPTS = 5;
  const seenIds = new Set();
  const validJobs = [];

  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    const prevCardCount = seenIds.size;

    // --- Scroll (except first attempt) ---
    if (attempt > 0) {
      scrollJobList();
      await delay(SCROLL_DELAY_MS);
    }

    // --- Scan all currently visible job cards ---
    const cards = document.querySelectorAll("li[data-occludable-job-id]");

    for (const card of cards) {
      const id = card.getAttribute("data-occludable-job-id");
      if (!id || seenIds.has(id)) continue;
      seenIds.add(id);

      const job = extractJob(card);
      if (job) {
        validJobs.push(job);
        if (validJobs.length >= requestedCount) break;
      }
    }

    // --- Done? ---
    if (validJobs.length >= requestedCount) break;

    // --- Early bail: no new cards appeared since last scroll ---
    if (attempt > 0 && seenIds.size === prevCardCount) break;

    // --- Hydration fix: if cards exist but all were blank, wait and retry once ---
    if (attempt === 0 && cards.length > 0 && validJobs.length === 0) {
      await delay(HYDRATE_WAIT_MS);
      // Re-check the same cards without scrolling (they may have hydrated by now)
      for (const card of cards) {
        const id = card.getAttribute("data-occludable-job-id");
        if (!id || seenIds.has(id)) continue;
        seenIds.add(id);

        const job = extractJob(card);
        if (job) {
          validJobs.push(job);
          if (validJobs.length >= requestedCount) break;
        }
      }
      if (validJobs.length >= requestedCount) break;
    }
  }

  return { jobs: validJobs.slice(0, requestedCount) };
}
