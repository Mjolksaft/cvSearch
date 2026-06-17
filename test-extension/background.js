chrome.runtime.onMessage.addListener((message) => {
    console.log("Background received:", message);

    if (message.action === "openGoogle") {
        chrome.tabs.create({
            url: "https://google.com"
        });
    }
});