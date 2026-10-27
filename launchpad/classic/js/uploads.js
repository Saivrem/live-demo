const uploadsApi = '/icedata/api/v2/uploads';

async function uploadError(response) {
    try {
        const body = await response.json();
        return body.message || body.detail || `Request failed (${response.status}).`;
    } catch {
        return response.status === 413
            ? 'Upload is too large. Each file must be at most 20 MB; total at most 100 MB.'
            : `Request failed (${response.status}).`;
    }
}

const uploadForm = document.getElementById('uploadForm');
if (uploadForm) {
    uploadForm.addEventListener('submit', async event => {
        event.preventDefault();
        const status = document.getElementById('uploadStatus');
        const submit = uploadForm.querySelector('button[type="submit"]');
        const files = Array.from(document.getElementById('customFile').files);
        if (!files.length || files.some(file => file.size === 0 || file.size > 20 * 1024 * 1024 || !/\.(csv|xls|xlsx|docx|doc)$/i.test(file.name))) {
            status.textContent = 'Choose nonempty csv, xls, xlsx, docx or doc files, at most 20 MB each.';
            return;
        }
        submit.disabled = true;
        status.textContent = 'Uploading…';
        try {
            const response = await fetch(uploadsApi, { method: 'POST', body: new FormData(uploadForm) });
            if (!response.ok) throw new Error(await uploadError(response));
            const uploaded = await response.json();
            status.replaceChildren(document.createTextNode(`Uploaded ${uploaded.length} file(s). `));
            const link = document.createElement('a');
            link.href = '/classic/uploads/';
            link.textContent = 'View uploads';
            status.append(link);
            uploadForm.reset();
        } catch (error) {
            status.textContent = error.message;
        } finally {
            submit.disabled = false;
        }
    });
}

async function loadUploads() {
    const list = document.getElementById('uploadsList');
    const status = document.getElementById('uploadsStatus');
    if (!list) return;
    status.textContent = 'Loading…';
    try {
        const response = await fetch(uploadsApi, { cache: 'no-store' });
        if (!response.ok) throw new Error(await uploadError(response));
        const uploads = await response.json();
        list.replaceChildren();
        for (const file of uploads) {
            const row = document.createElement('li');
            const link = document.createElement('a');
            link.href = file.url;
            link.textContent = file.name;
            row.append(link, document.createTextNode(` (${Math.ceil(file.size / 1024)} KB)`));
            list.append(row);
        }
        status.textContent = uploads.length ? '' : 'No files uploaded yet.';
    } catch (error) {
        status.textContent = error.message;
    }
}

const refreshUploads = document.getElementById('refreshUploads');
if (refreshUploads) refreshUploads.addEventListener('click', loadUploads);
loadUploads();
