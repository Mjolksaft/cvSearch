const BACKEND_URL = "http://localhost:8080";

browser.runtime.onMessage.addListener(async (message) => {
    console.log("Background received:", message);

    if (message.action === "crawlLinkedIn") {
        try {
            // Check if already on LinkedIn
            const [currentTab] = await browser.tabs.query({ active: true, currentWindow: true });

            if (currentTab && currentTab.url && currentTab.url.startsWith("https://www.linkedin.com/jobs/")) {
                await scrapeAndSave(currentTab.id);
                return;
            }

            // Open LinkedIn in background tab
            const linkedinTab = await browser.tabs.create({
                url: "https://www.linkedin.com/jobs/collections/top-applicant/",
                active: false
            });

            // Wait for it to load
            await new Promise((resolve) => {
                function listener(tabId, changeInfo) {
                    if (tabId === linkedinTab.id && changeInfo.status === "complete") {
                        browser.tabs.onUpdated.removeListener(listener);
                        setTimeout(resolve, 3000);
                    }
                }
                browser.tabs.onUpdated.addListener(listener);
            });

            await scrapeAndSave(linkedinTab.id);
            await browser.tabs.remove(linkedinTab.id);

            return { status: "ok" };
        } catch (err) {
            console.error("Crawl error:", err);
            return { status: "error", error: err.message };
        }
    }
});

async function scrapeAndSave(tabId) {
    const response = await browser.tabs.sendMessage(tabId, { action: "crawlJobs" });

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
