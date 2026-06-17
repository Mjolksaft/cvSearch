window.addEventListener("message", async (event) => {
    console.log("Message received:", event.data);

    if (event.data?.type === "CRAWL_LINKEDIN") {
        const count = event.data.count || 10;
        await browser.runtime.sendMessage({ action: "crawlLinkedIn", count });
        window.postMessage({ type: "CRAWL_LINKEDIN_DONE" }, "*");
    }
});
