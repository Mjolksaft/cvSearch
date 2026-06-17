window.addEventListener("message", async (event) => {
    if (event.source !== window) return;

    console.log("Message received:", event.data);

    if (event.data?.type === "CRAWL_LINKEDIN") {
        const count = event.data.count || 10;

        const response = await browser.runtime.sendMessage({
            action: "crawlLinkedIn",
            count
        });

        window.postMessage({
            type: "CRAWL_LINKEDIN_DONE",
            result: response
        }, "*");
    }

    if (event.data?.type === "GET_LINKEDIN_DESC") {
        const jobId = event.data.jobId;
        const url = event.data.url;
        // Extract the LinkedIn job ID from the URL, e.g. /jobs/view/3909564436/
        const match = url && url.match(/\/jobs\/view\/(\d+)/);
        const externalId = match ? match[1] : null;

        const response = await browser.runtime.sendMessage({
            action: "getLinkedInDescription",
            jobId,
            externalId,
            url
        });

        window.postMessage({
            type: "GET_LINKEDIN_DESC_DONE",
            result: response
        }, "*");
    }
});