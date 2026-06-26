const BACKEND_URL = "http://localhost:8080";

/**
 * Single message listener — handles ALL actions.
 * IMPORTANT: Only ONE listener to prevent Promise race conditions
 * between multiple listeners.
 */
browser.runtime.onMessage.addListener(async (message) => {
    console.log("Background received:", message);

    if (message.action === "crawlLinkedIn") {
        return handleCrawlLinkedIn(message.count || 10, message.keywords || "");
    }

    if (message.action === "getLinkedInDescription") {
        return handleGetLinkedInDescription(message.jobId, message.externalId, message.url);
    }
});

// ===================== Crawl LinkedIn =====================

async function handleCrawlLinkedIn(jobCount, keywords) {
    try {
        const [currentTab] = await browser.tabs.query({ active: true, currentWindow: true });

        // If already on a LinkedIn jobs page, scrape the active tab directly
        if (currentTab && currentTab.url && currentTab.url.startsWith("https://www.linkedin.com/jobs/")) {
            console.log("[handleCrawlLinkedIn] Scraping active LinkedIn tab");
            await scrapeAndSave(currentTab.id, jobCount);
            return { status: "ok" };
        }

        // Build URL: search results if keywords, otherwise default top-applicant page
        let linkedInUrl;
        if (keywords) {
            linkedInUrl = `https://www.linkedin.com/jobs/search-results/?keywords=${encodeURIComponent(keywords)}&origin=JOB_SEARCH_PAGE_JOB_FILTER&geoId=119330644&distance=99.41936`;
        } else {
            linkedInUrl = "https://www.linkedin.com/jobs/collections/top-applicant/";
        }

        // --- Create an off-screen popup window ---
        // Popup windows stay "active" enough for LinkedIn's virtualizer to render,
        // unlike background tabs which freeze requestAnimationFrame.
        console.log("[handleCrawlLinkedIn] Creating popup window...");
        const popupWin = await browser.windows.create({
            url: linkedInUrl,
            type: "popup",
            width: 800,
            height: 600,
            left: -30000, // way off-screen to the left
            top: -30000
        });
        const linkedinTab = popupWin.tabs[0];

        // Switch focus back to the original window so the popup stays behind it
        if (currentTab?.windowId) {
            await browser.windows.update(currentTab.windowId, { focused: true });
        }

        // Wait for page to finish loading
        await new Promise((resolve) => {
            function listener(tabId, changeInfo) {
                if (tabId === linkedinTab.id && changeInfo.status === "complete") {
                    browser.tabs.onUpdated.removeListener(listener);
                    setTimeout(resolve, 3000);
                }
            }
            browser.tabs.onUpdated.addListener(listener);
        });

        await scrapeAndSave(linkedinTab.id, jobCount);

        // Close the popup window
        await browser.windows.remove(popupWin.id);

        return { status: "ok" };
    } catch (err) {
        console.error("Crawl error:", err);
        return { status: "error", error: err.message };
    }
}

// ===================== Fetch Description from LinkedIn =====================

async function handleGetLinkedInDescription(jobId, externalId, url) {
    let linkedinTab = null;

    try {
        linkedinTab = await browser.tabs.create({ url, active: false });

        await waitForTabLoad(linkedinTab.id);
        await delay(3000);

        const response = await browser.tabs.sendMessage(linkedinTab.id, {
            action: "extractLinkedInDescription",
            jobId
        });

        if (!response?.description) {
            throw new Error("No description found on LinkedIn page");
        }

        await fetch(`${BACKEND_URL}/api/jobs/external/${externalId}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ description: response.description })
        });

        return {
            status: "ok",
            jobId,
            externalId,
            description: response.description
        };
    } catch (err) {
        console.error("Description error:", err);
        return { status: "error", error: err.message };
    } finally {
        if (linkedinTab) {
            try { await browser.tabs.remove(linkedinTab.id); } catch (e) { /* tab may already be closed */ }
        }
    }
}

function waitForTabLoad(tabId) {
    return new Promise((resolve) => {
        function listener(updatedTabId, changeInfo) {
            if (updatedTabId === tabId && changeInfo.status === "complete") {
                browser.tabs.onUpdated.removeListener(listener);
                resolve();
            }
        }

        browser.tabs.onUpdated.addListener(listener);
    });
}

function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}


async function scrapeAndSave(tabId, count) {
    console.log(`[scrapeAndSave] Sending crawlJobs action to tab ${tabId}, requesting ${count} jobs`);
    const response = await browser.tabs.sendMessage(tabId, { action: "crawlJobs", count });

    console.log("[scrapeAndSave] Response from content script:", response);

    if (!response || !response.jobs || response.jobs.length === 0) {
        console.log("[scrapeAndSave] No jobs found");
        return;
    }

    const bulkItems = response.jobs.map((job) => ({
        title: job.title || "Unknown title",
        companyName: job.company || "Unknown company",
        location: job.location || null,
        externalId: job.jobId ? parseInt(job.jobId, 10) : null,
        website: job.jobId ? `https://www.linkedin.com/jobs/view/${job.jobId}/` : null,
        description: null
    }));

    const saveResponse = await fetch(`${BACKEND_URL}/api/jobs/bulk`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bulkItems)
    });

    if (!saveResponse.ok) {
        throw new Error("Server returned " + saveResponse.status);
    }

    console.log(`Saved ${bulkItems.length} job(s)`);
}
