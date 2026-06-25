browser.runtime.onMessage.addListener((message) => {
  if (message.action === "crawlJobs") {
    return scrapeJobs(message.count || 10);
  }
});

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// ===================== New UI (search-results page) =====================
// Uses stable attributes: componentkey, span[aria-hidden="true"], <p> tags

function extractJobNewUI(card) {
  // Job ID from componentkey attribute: "job-card-component-ref-4427825984"
  const componentKey = card.getAttribute("componentkey");
  const jobId = componentKey?.match(/\d+$/)?.[0];
  if (!jobId) return null;

  // Title: first span[aria-hidden="true"] inside the card
  const titleSpan = card.querySelector('span[aria-hidden="true"]');
  const title = titleSpan?.innerText?.trim();
  if (!title) return null;

  // Company & Location: <p> elements, skipping the one that contains the title span
  const paragraphs = card.querySelectorAll("p");
  const titleP = titleSpan?.closest("p");
  let company = null;
  let location = null;
  let found = 0;
  for (const p of paragraphs) {
    if (p === titleP) continue; // skip the title paragraph
    const text = p.innerText?.trim();
    if (!text) continue;
    if (found === 0) company = text;
    else if (found === 1) location = text;
    found++;
  }

  if (!company) return null;
  return { jobId, title, company, location };
}

// ===================== Old UI (top-applicant / collections page) =====================
// Uses data-occludable-job-id, a[href*="/jobs/view/"], text walker

function extractJobOldUI(card) {
  const jobId = card.getAttribute("data-occludable-job-id") || card.getAttribute("data-job-id");
  if (!jobId) return null;

  // Title: the job view link
  const titleLink = card.querySelector('a[href*="/jobs/view/"]');
  const title = titleLink?.innerText?.trim();
  if (!title) return null;

  // Company & Location: walk all visible text excluding the title link
  const textParts = [];
  const walker = document.createTreeWalker(
    card,
    NodeFilter.SHOW_TEXT,
    {
      acceptNode: (node) => {
        const text = node.textContent.trim();
        if (!text) return NodeFilter.FILTER_REJECT;
        if (titleLink && titleLink.contains(node)) return NodeFilter.FILTER_REJECT;
        return NodeFilter.FILTER_ACCEPT;
      }
    }
  );

  while (walker.nextNode()) {
    const text = walker.currentNode.textContent.trim();
    if (text && text.length > 1 && !textParts.includes(text)) {
      textParts.push(text);
    }
  }

  const company = textParts[0] || null;
  const location = textParts.length > 1 ? textParts[1] : null;

  if (!company) return null;
  return { jobId, title, company, location };
}

// ===================== Unified entry point =====================

function extractJob(card) {
  if (card.hasAttribute("componentkey")) {
    return extractJobNewUI(card);
  }
  return extractJobOldUI(card);
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
  // Scroll container (find once, reuse)
  const sentinel = document.querySelector("[data-results-list-top-scroll-sentinel]");
  const scrollContainer = sentinel?.parentElement
    || document.querySelector(".scaffold-layout__list")
    || document.querySelector(".jobs-search-results-list")
    || document.querySelector('[role="list"]')
    || document.body;

  const MAX_ATTEMPTS = 15;
  const SCROLL_DELAY_MS = 1200;
  const seenIds = new Set();
  const validJobs = [];
  let consecutiveEmptyScrolls = 0;

  // Scroll in a wave pattern: go down, then up, then down again
  // This helps with virtualization since recycling might skip IDs in one direction
  const scrollPositions = [0.3, 0.6, 0.85, 0.95, 0.7, 0.4, 0.1, 0.5, 0.9, 0.99, 0.3, 0.7, 0.95, 0.5, 0.99];

  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    const prevCardCount = seenIds.size;

    // --- First few attempts: just wait for lazy rendering (no scroll) ---
    if (attempt < 3) {
      await delay(SCROLL_DELAY_MS);
    } else {
      // --- Scroll to a specific position ---
      const pct = scrollPositions[Math.min(attempt - 3, scrollPositions.length - 1)];
      scrollContainer.scrollTop = scrollContainer.scrollHeight * pct;
      await delay(SCROLL_DELAY_MS);
    }

    // --- Query ALL job cards currently in the DOM ---
    // LinkedIn lazy-renders cards, so more appear over time without scrolling
    const cardIds = new Map(); // id → DOM element

    // Strategy 1: find by data-job-id on the inner div (most stable)
    document.querySelectorAll("div[data-job-id]").forEach(el => {
      const id = el.getAttribute("data-job-id");
      if (id) cardIds.set(id, el);
    });

    // Strategy 2: find by data-occludable-job-id on the li
    document.querySelectorAll("li[data-occludable-job-id]").forEach(el => {
      const id = el.getAttribute("data-occludable-job-id");
      if (id && !cardIds.has(id)) cardIds.set(id, el);
    });

    // Strategy 3: fallback — find by componentkey
    document.querySelectorAll("[componentkey^='job-card-component-ref-']").forEach(el => {
      const id = el.getAttribute("componentkey")?.match(/\d+$/)?.[0];
      if (id && !cardIds.has(id)) cardIds.set(id, el);
    });

    console.log(`scrapeJobs: attempt ${attempt}, ${cardIds.size} cards in DOM, ${seenIds.size} seen, ${validJobs.length} valid`);

    // --- Process all found cards ---
    for (const [id, element] of cardIds) {
      if (seenIds.has(id)) continue;
      seenIds.add(id);

      const job = extractJob(element);
      if (job) {
        validJobs.push(job);
        if (validJobs.length >= requestedCount) break;
      }
    }

    // --- Done? ---
    if (validJobs.length >= requestedCount) break;

    // --- Track consecutive scrolls with no new IDs ---
    if (seenIds.size === prevCardCount) {
      consecutiveEmptyScrolls++;
      if (consecutiveEmptyScrolls >= 3) break; // 3 empty = no more jobs
    } else {
      consecutiveEmptyScrolls = 0;
    }
  }

  return { jobs: validJobs.slice(0, requestedCount) };
}

browser.runtime.onMessage.addListener((message) => {
    console.log("LinkedIn content received:", message);

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
