document.addEventListener('DOMContentLoaded', function () {

    document.querySelectorAll('.bookmark-btn').forEach(btn => {
        btn.addEventListener('click', async function () {
            const jobId = this.dataset.jobId;
            const isSaved = this.dataset.saved === 'true';
            const newSaved = !isSaved;

            try {
                const response = await fetch(`/api/jobs/${jobId}`, {
                    method: 'PATCH',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ saved: newSaved })
                });

                if (!response.ok) throw new Error('Failed to update');

                this.dataset.saved = newSaved;
                this.classList.toggle('btn-warning', newSaved);
                this.classList.toggle('btn-outline-warning', !newSaved);

                const icon = this.querySelector('i');
                icon.classList.toggle('bi-bookmark', !newSaved);
                icon.classList.toggle('bi-bookmark-fill', newSaved);

                const textSpan = this.querySelector('span');
                if (textSpan) {
                    textSpan.textContent = newSaved ? 'Saved' : 'Save';
                }

                const row = this.closest('tr');
                if (row) {
                    row.classList.toggle('table-warning', newSaved);
                }

            } catch (error) {
                console.error('Bookmark error:', error);
                alert('Failed to toggle bookmark');
            }
        });
    });

    // ======== LinkedIn Extension Integration ========
    // Helper: inject a trigger element for the extension to find
    function injectTrigger(attrs) {
        var area = document.getElementById('cvsearch-trigger-area');
        if (!area) {
            area = document.createElement('div');
            area.id = 'cvsearch-trigger-area';
            area.className = 'd-none';
            document.body.appendChild(area);
        }
        var div = document.createElement('div');
        div.id = 'cvsearch-command';
        for (var key in attrs) {
            if (attrs.hasOwnProperty(key)) {
                div.setAttribute('data-' + key, attrs[key]);
            }
        }
        area.appendChild(div);
    }

    // Fetch description button on job detail page
    var fetchDescBtn = document.getElementById('fetch-desc-btn');
    if (fetchDescBtn) {
        fetchDescBtn.addEventListener('click', function () {
            var jobId = this.dataset.jobId;
            var url = this.dataset.url;

            this.disabled = true;
            this.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Fetching...';

            var statusEl = document.getElementById('desc-status');
            if (statusEl) {
                statusEl.className = 'small me-2 text-info';
                statusEl.textContent = 'Opening LinkedIn...';
                statusEl.classList.remove('d-none');
            }

            injectTrigger({
                action: 'description',
                'job-id': jobId,
                url: url
            });
        });
    }

    // Auto-trigger description fetch on page load if description is missing
    (function () {
        var btn = document.getElementById('fetch-desc-btn');
        if (btn) {
            var jobId = btn.dataset.jobId;
            var url = btn.dataset.url;
            var statusEl = document.getElementById('desc-status');
            if (statusEl) {
                statusEl.className = 'small me-2 text-info';
                statusEl.textContent = 'Auto-fetching description from LinkedIn...';
                statusEl.classList.remove('d-none');
            }
            injectTrigger({
                action: 'description',
                'job-id': jobId,
                url: url
            });
        }
    })();

    if (!window.__profileData) return;

    const pd = window.__profileData;

    let skills = [];
    let projects = [];
    let education = [];
    let languages = [];

    function parseJSON(raw, fallback) {
        if (!raw) return fallback;
        try { return JSON.parse(raw); } catch (e) { return fallback; }
    }

    skills = parseJSON(pd.skills, []);
    projects = parseJSON(pd.projects, []);
    education = parseJSON(pd.education, []);
    languages = parseJSON(pd.languages, []);

    const userId = pd.userId || '1';
    const apiUrl = pd.apiUrl || '/api/profile';

    if (pd.summary) document.getElementById('summary').value = pd.summary;
    if (pd.certs) document.getElementById('certifications').value = pd.certs;

    function renderTags(listId) {
        const list = document.getElementById(listId);
        const items = listId === 'skills-list' ? skills : languages;
        list.innerHTML = items.map((item, index) =>
            `<span class="tag" data-index="${index}">
                <span class="tag-text">${escapeHtml(item)}</span>
                <button type="button" class="tag-remove" aria-label="Remove">&times;</button>
            </span>`
        ).join('');
    }

    document.getElementById('skills-container').addEventListener('click', function (e) {
        const btn = e.target.closest('.tag-remove');
        if (!btn) return;
        const span = btn.closest('.tag');
        if (!span) return;
        const idx = parseInt(span.dataset.index);
        skills.splice(idx, 1);
        renderTags('skills-list');
    });

    document.getElementById('languages-container').addEventListener('click', function (e) {
        const btn = e.target.closest('.tag-remove');
        if (!btn) return;
        const span = btn.closest('.tag');
        if (!span) return;
        const idx = parseInt(span.dataset.index);
        languages.splice(idx, 1);
        renderTags('languages-list');
    });

    function renderProjects() {
        const container = document.getElementById('projects-container');
        if (projects.length === 0) {
            container.innerHTML = `<div class="text-muted small py-2">No projects added yet.</div>`;
            return;
        }
        container.innerHTML = projects.map((proj, index) =>
            `<div class="entry-card" data-index="${index}">
                <div class="entry-card-header">
                    <span class="fw-medium">Project ${index + 1}</span>
                    <button type="button" class="btn-remove-entry" data-index="${index}" aria-label="Remove project">&times;</button>
                </div>
                <div class="entry-card-body">
                    <div class="mb-2">
                        <label class="form-label-sm">Name</label>
                        <input type="text" class="form-control form-control-sm project-name" value="${escapeHtml(proj.name)}" placeholder="Project name" />
                    </div>
                    <div class="mb-2">
                        <label class="form-label-sm">Description</label>
                        <textarea class="form-control form-control-sm project-desc" rows="2" placeholder="Brief description">${escapeHtml(proj.description)}</textarea>
                    </div>
                    <div class="mb-2">
                        <label class="form-label-sm">Technologies</label>
                        <input type="text" class="form-control form-control-sm project-tech" value="${escapeHtml(proj.technologies)}" placeholder="Java, Spring Boot, PostgreSQL" />
                    </div>
                    <div>
                        <label class="form-label-sm">URL</label>
                        <input type="url" class="form-control form-control-sm project-url" value="${escapeHtml(proj.url)}" placeholder="https://github.com/..." />
                    </div>
                </div>
            </div>`
        ).join('');
        container.querySelectorAll('.btn-remove-entry').forEach(btn => {
            btn.addEventListener('click', function () {
                const idx = parseInt(this.dataset.index);
                projects.splice(idx, 1);
                renderProjects();
            });
        });
        container.querySelectorAll('.entry-card').forEach(card => {
            const idx = parseInt(card.dataset.index);
            card.querySelectorAll('input, textarea').forEach(input => {
                input.addEventListener('input', function () {
                    const p = projects[idx];
                    if (this.classList.contains('project-name')) p.name = this.value;
                    else if (this.classList.contains('project-desc')) p.description = this.value;
                    else if (this.classList.contains('project-tech')) p.technologies = this.value;
                    else if (this.classList.contains('project-url')) p.url = this.value;
                });
            });
        });
    }

    function renderEducation() {
        const container = document.getElementById('education-container');
        if (education.length === 0) {
            container.innerHTML = `<div class="text-muted small py-2">No education entries added yet.</div>`;
            return;
        }
        container.innerHTML = education.map((edu, index) =>
            `<div class="entry-card" data-index="${index}">
                <div class="entry-card-header">
                    <span class="fw-medium">Education ${index + 1}</span>
                    <button type="button" class="btn-remove-entry" data-index="${index}" aria-label="Remove entry">&times;</button>
                </div>
                <div class="entry-card-body">
                    <div class="mb-2">
                        <label class="form-label-sm">Degree</label>
                        <input type="text" class="form-control form-control-sm edu-degree" value="${escapeHtml(edu.degree)}" placeholder="BSc in Computer Science" />
                    </div>
                    <div class="mb-2">
                        <label class="form-label-sm">School</label>
                        <input type="text" class="form-control form-control-sm edu-school" value="${escapeHtml(edu.school)}" placeholder="University name" />
                    </div>
                    <div>
                        <label class="form-label-sm">Year</label>
                        <input type="text" class="form-control form-control-sm edu-year" value="${escapeHtml(edu.year)}" placeholder="2024" />
                    </div>
                </div>
            </div>`
        ).join('');
        container.querySelectorAll('.btn-remove-entry').forEach(btn => {
            btn.addEventListener('click', function () {
                const idx = parseInt(this.dataset.index);
                education.splice(idx, 1);
                renderEducation();
            });
        });
        container.querySelectorAll('.entry-card').forEach(card => {
            const idx = parseInt(card.dataset.index);
            card.querySelectorAll('input').forEach(input => {
                input.addEventListener('input', function () {
                    const e = education[idx];
                    if (this.classList.contains('edu-degree')) e.degree = this.value;
                    else if (this.classList.contains('edu-school')) e.school = this.value;
                    else if (this.classList.contains('edu-year')) e.year = this.value;
                });
            });
        });
    }

    function setupTagInput(inputId, btnId, listId, stateArray) {
        const input = document.getElementById(inputId);
        const btn = document.getElementById(btnId);

        function addTag() {
            const val = input.value.trim();
            if (val && !stateArray.includes(val)) {
                stateArray.push(val);
                input.value = '';
                renderTags(listId);
            }
            input.focus();
        }

        input.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                addTag();
            }
        });
        btn.addEventListener('click', addTag);
    }

    renderTags('skills-list');
    renderTags('languages-list');
    renderProjects();
    renderEducation();

    setupTagInput('skill-input', 'add-skill-btn', 'skills-list', skills);
    setupTagInput('language-input', 'add-language-btn', 'languages-list', languages);

    document.getElementById('add-project-btn').addEventListener('click', function () {
        projects.push({ name: '', description: '', technologies: '', url: '' });
        renderProjects();
    });

    document.getElementById('add-edu-btn').addEventListener('click', function () {
        education.push({ degree: '', school: '', year: '' });
        renderEducation();
    });

    document.getElementById('save-profile-btn').addEventListener('click', async function () {
        const statusEl = document.getElementById('save-status');
        statusEl.style.display = 'none';

        document.querySelectorAll('#projects-container .entry-card').forEach(card => {
            const idx = parseInt(card.dataset.index);
            if (projects[idx]) {
                const p = projects[idx];
                p.name = card.querySelector('.project-name')?.value || '';
                p.description = card.querySelector('.project-desc')?.value || '';
                p.technologies = card.querySelector('.project-tech')?.value || '';
                p.url = card.querySelector('.project-url')?.value || '';
            }
        });

        document.querySelectorAll('#education-container .entry-card').forEach(card => {
            const idx = parseInt(card.dataset.index);
            if (education[idx]) {
                const e = education[idx];
                e.degree = card.querySelector('.edu-degree')?.value || '';
                e.school = card.querySelector('.edu-school')?.value || '';
                e.year = card.querySelector('.edu-year')?.value || '';
            }
        });

        const summary = document.getElementById('summary').value.trim();

        const skillsJson = JSON.stringify(skills);
        const projectsJson = JSON.stringify(projects.map(p => ({
            name: p.name,
            description: p.description,
            technologies: p.technologies ? p.technologies.split(',').map(t => t.trim()).filter(Boolean) : [],
            url: p.url
        })));
        const educationJson = JSON.stringify(education.map(e => ({
            degree: e.degree,
            school: e.school,
            year: e.year
        })));
        const languagesJson = JSON.stringify(languages);
        const certificationsJson = document.getElementById('certifications').value.trim();

        const body = {
            summary: summary,
            skills: skillsJson,
            projects: projectsJson,
            education: educationJson,
            languages: languagesJson,
            certifications: certificationsJson
        };

        try {
            const response = await fetch(`${apiUrl}?userId=${userId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });

            if (response.ok) {
                showStatus('success', 'Profile saved successfully!');
                setTimeout(() => window.location.reload(), 1000);
            } else {
                const errText = await response.text();
                showStatus('danger', `Failed to save: ${response.status} ${errText.slice(0, 200)}`);
            }
        } catch (error) {
            showStatus('danger', 'Network error: ' + error.message);
        }
    });

    function showStatus(type, message) {
        const el = document.getElementById('save-status');
        el.className = `alert alert-${type} mt-3`;
        el.textContent = message;
        el.style.display = 'block';
    }

    function escapeHtml(str) {
        if (str == null) return '';
        if (typeof str !== 'string') str = String(str);
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#039;');
    }

});
