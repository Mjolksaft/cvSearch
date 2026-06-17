browser.runtime.onMessage.addListener((message) => {
  if (message.action === "crawlJobs") {
    const jobs = [...document.querySelectorAll("li[data-occludable-job-id]")]
      .slice(0, 3)
      .map((jobCard) => ({
        jobId: jobCard.getAttribute("data-occludable-job-id"),
        title: jobCard.querySelector(".job-card-list__title--link")?.innerText.trim(),
        company: jobCard.querySelector(".artdeco-entity-lockup__subtitle")?.innerText.trim(),
        location: jobCard.querySelector(".artdeco-entity-lockup__caption")?.innerText.trim()
      }));

    return Promise.resolve({ jobs });
  }
});
