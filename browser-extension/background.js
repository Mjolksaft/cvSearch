const BACKEND_URL = "http://localhost:8080";

/**
 * Single message listener — handles ALL actions.
 * IMPORTANT: Only ONE listener to prevent Promise race conditions
 * between multiple listeners.
 */
browser.runtime.onMessage.addListener(async (message) => {
    console.log("Background received:", message);

    if (message.action === "crawlLinkedIn") {
        return handleCrawlLinkedIn(message.count || 10);
    }

    if (message.action === "getLinkedInDescription") {
        return handleGetLinkedInDescription(message.jobId, message.externalId, message.url);
    }
});

// ===================== Crawl LinkedIn =====================

async function handleCrawlLinkedIn(jobCount) {
    try {
        const [currentTab] = await browser.tabs.query({ active: true, currentWindow: true });

        if (currentTab && currentTab.url && currentTab.url.startsWith("https://www.linkedin.com/jobs/")) {
            await scrapeAndSave(currentTab.id, jobCount);
            return { status: "ok" };
        }

        const linkedinTab = await browser.tabs.create({
            url: "https://www.linkedin.com/jobs/collections/top-applicant/",
            active: false
        });

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
        await browser.tabs.remove(linkedinTab.id);

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
    const response = await browser.tabs.sendMessage(tabId, { action: "crawlJobs", count });

    if (!response || !response.jobs || response.jobs.length === 0) {
        console.log("No jobs found");
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
