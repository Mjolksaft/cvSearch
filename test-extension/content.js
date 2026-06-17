window.addEventListener("message", (event) => {
    console.log("Message received:", event.data);

    if (event.data?.type === "OPEN_GOOGLE") {
        chrome.runtime.sendMessage({
            action: "openGoogle"
        });
    }
});