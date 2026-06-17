const BACKEND_URL = "http://localhost:8080";
const statusEl = document.getElementById("status");
const checkBtn = document.getElementById("checkBtn");

function setStatus(message, isError) {
  statusEl.textContent = message;
  statusEl.style.color = isError ? "#c00" : "#333";
}

checkBtn.addEventListener("click", async () => {
  checkBtn.disabled = true;
  setStatus("Opening LinkedIn...");

  try {
    const [currentTab] = await browser.tabs.query({ active: true, currentWindow: true });

    // If already on a LinkedIn jobs page, scrape directly
    if (currentTab && currentTab.url && currentTab.url.startsWith("https://www.linkedin.com/jobs/")) {
      await scrapeAndSave(currentTab.id);
      return;
    }

    // Otherwise open LinkedIn in a background tab
    const linkedinTab = await browser.tabs.create({
      url: "https://www.linkedin.com/jobs/collections/top-applicant/",
      active: false
    });

    setStatus("Waiting for LinkedIn to load...");

    // Wait for the tab to fully load
    await new Promise((resolve) => {
      function listener(tabId, changeInfo) {
        if (tabId === linkedinTab.id && changeInfo.status === "complete") {
          browser.tabs.onUpdated.removeListener(listener);
          // Give it a moment for the job cards to render
          setTimeout(resolve, 3000);
        }
      }
      browser.tabs.onUpdated.addListener(listener);
    });

    await scrapeAndSave(linkedinTab.id);

    // Close the background tab
    await browser.tabs.remove(linkedinTab.id);
  } catch (err) {
    setStatus("Error: " + err.message, true);
  } finally {
    checkBtn.disabled = false;
  }
});

async function scrapeAndSave(tabId) {
  setStatus("Scraping jobs...");

  const response = await browser.tabs.sendMessage(tabId, { action: "crawlJobs" });

  if (!response || !response.jobs || response.jobs.length === 0) {
    setStatus("No jobs found", true);
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

  setStatus(`Saving ${bulkItems.length} job(s)...`);

  const saveResponse = await fetch(`${BACKEND_URL}/api/jobs/bulk`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(bulkItems)
  });

  if (!saveResponse.ok) {
    const errorText = await saveResponse.text();
    throw new Error("Server returned " + saveResponse.status);
  }

  setStatus("Saved " + bulkItems.length + " job(s)!");
}
