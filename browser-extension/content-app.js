window.addEventListener("message", async (event) => {
    console.log("Message received:", event.data);

    if (event.data?.type === "CRAWL_LINKEDIN") {
        await browser.runtime.sendMessage({ action: "crawlLinkedIn" });
        window.postMessage({ type: "CRAWL_LINKEDIN_DONE" }, "*");
    }
});
