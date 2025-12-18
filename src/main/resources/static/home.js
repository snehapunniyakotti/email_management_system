/*******************
* Mock JSON store
*******************/
const mailData = {
    folders: { INBOX: [], Starred: [], Snoozed: [], Sent: [], Drafts: [] },
    category: [{ label: 'Social', icon: '👥', key: 'INBOX/social', searchKey: 'category:social' }, { label: 'Updates', icon: 'ⓘ', key: 'INBOX/updates', searchKey: 'category:updates' }, { label: 'Forms', icon: '🗪', key: 'INBOX/forms', searchKey: 'category:forums' }, { label: 'Promotions', icon: '🏷', key: 'INBOX/promotions', searchKey: 'category:promotions' },],
    more: [
        { key: '[Gmail]/Important', label: 'Important', icon: '⚑', searchKey: 'is:important' },
        { key: 'chats', label: 'Chats', icon: '💬', searchKey: '' },
        { key: '[Gmail]/Schedule', label: 'Scheduled', icon: '⏰', searchKey: 'in:scheduled' },
        { key: '[Gmail]/All Mail', label: 'All Mail', icon: '✉️', searchKey: '' },
        { key: '[Gmail]/Spam', label: 'Spam', icon: '🚫', searchKey: 'in:spam' },
        { key: '[Gmail]/Trash', label: 'Trash', icon: '🗑️', searchKey: 'in:trash' },
        { key: 'newLabel', label: 'Create new label', icon: '➕', searchKey: '' }
    ],
    labels: [],
    messages: [
        { id: 1, folder: 'INBOX', category: 'INBOX/personal', from: 'Flattrade', subject: 'Important Advisory from Exchange: Beware of Illegal Trading Activities', snippet: 'Please be advised that certain activities...', body: 'Full body of the Flattrade advisory email.\\n\\nRegards, Flattrade', date: '3:02 PM', starred: false, read: false, labels: ['Sneha'] },
    ],
};

const folderSearchKeys = new Map([
    ['is:important', '[Gmail]/Important'],
    ['is:starred', '[Gmail]/Starred'],
    ['is:read', 'read'],
    ['is:archived ', '[Gmail]/Archive'],
    ['in:snoozed', '[Gmail]/Snoozed'],
    ['in:sent', '[Gmail]/Sent Mail'],
    ['in:scheduled', '[Gmail]/Schedule'],
    ['in:draft', '[Gmail]/Drafts'],
    ['in:spam', '[Gmail]/Spam'],
    ['in:trash', '[Gmail]/Trash'],
    ['category:social', 'INBOX/social'],
    ['category:updates', 'INBOX/updates'],
    ['category:forums', 'INBOX/forms'],
    ['category:promotions', 'INBOX/promotions'],
]);

/*******************
 * loadder
 *******************/

const loader = document.getElementById('loader-overlay');

// Function to show the loader
function showLoader() {
    loader.classList.remove('hidden');
}

// Function to hide the loader
function hideLoader() {
    loader.classList.add('hidden');
}

/*******************
 * App state
 *******************/
const state = {
    currentFolder: 'INBOX',
    currentCategory: 'INBOX/personal',
    page: 0,
    pageSize: 50,
    search: '',
    selected: new Set(),
    detailOpen: false,
    currentDetailId: null,
    totalEmails: 0,
    totalPages: 0,
    curMsgIdForDraftEdit: "",
    replyMailIdList: new Set(),
    baseSubMailMap: new Map(),
    messages: [],
    csrfHeaderName: "",
    csrfToken: "",
};

/*******************
 * initial function call
 *******************/
function init() {
    fetchMailWithPageAndSize('INBOX/personal', 0, state.pageSize);
    fetchLabels();
    csrfToken();
    setInterval(() => {
        // fetchLabels();
        fetchWithRetry();
    }, 1200000);// 2 mins once
}

init();

/*******************
 * fetch label with retry design pattern
 *******************/

/// retry pattern to call api with calcalculated interval if the api call fails
let retryCount = 0;
let maxRetries = 6;
let initialDelay = 2000; // 2 seconds

async function fetchWithRetry() {
    if (retryCount >= maxRetries) {
        console.log("fetchLabels Max retries reached. Stopping.");
        return;
    }

    try {
        await fetchLabels(); // <-- your API call
        console.log("fetchLabels API call successful");
        retryCount = 0; // reset if success 
    } catch (err) {
        console.error("fetchLabels API call failed:", err);

        // calculate next delay
        let delay = initialDelay * Math.pow(2, retryCount);
        console.log("fetchLabels printing the delay ::::::::: " + delay)
        retryCount++;

        console.log(`fetchLabels Next retry in ${delay / 1000}s`);

        // wait before retrying
        await new Promise((resolve) => setTimeout(resolve, delay));

        return fetchWithRetry(); // recursive retry
    }
}

fetchWithRetry();

/*******************
 * Sidebar rendering
 *******************/
const coreFolderDefs = [
    { key: 'INBOX', label: 'Inbox', icon: '✉️', searchKey: '' },
    { key: '[Gmail]/Starred', label: 'Starred', icon: '⭐', searchKey: 'is:starred' },
    { key: '[Gmail]/Snoozed', label: 'Snoozed', icon: '⏱️', searchKey: 'in:snoozed' },
    { key: '[Gmail]/Sent Mail', label: 'Sent', icon: '📤', searchKey: 'in:sent' },
    { key: '[Gmail]/Drafts', label: 'Drafts', icon: '📝', searchKey: 'in:draft' },
    { key: '[Gmail]/Archive', label: 'Archive', icon: '📩', searchKey: 'is:archived' }
];

/*******************
 * helper functions
 *******************/

function toggleMenu(dotElement) {
    const menu = dotElement.nextElementSibling; // menu is right after ⋮

    // Hide any other open menus
    document.querySelectorAll('.menu-list').forEach(m => {
        if (m !== menu) m.style.display = 'none';
    });

    // Toggle current menu
    menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
}

function getChildren(input, data) {
    const prefix = input + "/";
    const children = [];

    data.forEach(item => {
        if (item.startsWith(prefix)) {
            const rest = item.slice(prefix.length);  // part after prefix
            const parts = rest.split("/");

            // take only the first segment (direct child)
            children.push(parts.join("/"));
        }
    });

    return children;
}

function editLabel(fullPath) {
    fullPath = fullPath.replaceAll("-", "/");
    // let children = getChildren(fullPath, mailData.labels);
    let curfolder = fullPath.split("/");
    const name = prompt('Rename label:', curfolder[curfolder.length - 1]);
    curfolder[curfolder.length - 1] = name;
    let newPath = curfolder.join("/");

    manipulateLabels(fullPath, "EDIT", newPath)
}

function deleteLabel(fullPath) {
    fullPath = fullPath.replaceAll("-", "/");
    let children = getChildren(fullPath, mailData.labels)
    if (confirm("Are you sure you want to delete: " + fullPath + "? \n " + children.join("\n"))) {
        manipulateLabels(fullPath, "DELETE")
    }
}

function createLabelPrompt(parentLabel) {
    parentLabel = parentLabel.replaceAll("-", "/");
    const name = prompt('Create new label:');
    let label = (parentLabel.length != 0) ? parentLabel + "/" + name : name;
    if (name && !mailData.labels.includes(name)) {
        // call addLabel api
        manipulateLabels(label, "ADD");
    }
}

function handleLabelClick(fullPath) {
    // hiding category tabs
    let tab = document.getElementById('tabs');
    tab.style.display = "none";

    // 🔹 Clear "active" from all nav-items
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });

    // // 🔹 Set active only for the clicked one
    // div.classList.add('active');

    state.search = `label:${fullPath}`;
    document.getElementById('searchInput').value = state.search;
    state.page = 0;
    let searchFolder = fullPath.replaceAll('-', '/');
    state.currentFolder = searchFolder
    fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
}

////////updating the filter folder list
function updateSearchFilterFolderList(labelList) {
    const folderSelect = document.querySelector('select[name="folder"]');
    if (!folderSelect) {
        console.error("Folder select element not found.");
        return;
    }
    folderSelect.innerHTML = '';
    let newFolders = [
        { text: "All Mail", value: "[Gmail]/All Mail" },
        { text: "Inbox", value: "INBOX" },
        { text: "Starred", value: "[Gmail]/Starred" },
        { text: "Sent Mail", value: "[Gmail]/Sent Mail" },
        { text: "Drafts", value: "[Gmail]/Drafts" },
        { text: "Spam", value: "[Gmail]/Spam" },
        { text: "Trash", value: "[Gmail]/Trash" },
        { text: "Mail & Spam & Trash", value: "anywhere" },
        { text: "Read Mail", value: "read" },
        { text: "Unread Mail", value: "unread" },
        { text: "Social", value: "INBOX/social" },
        { text: "Updates", value: "INBOX/updates" },
        { text: "Forms", value: "INBOX/forms" },
        { text: "Promotions", value: "INBOX/promotions" }
    ];

    labelList.forEach(label => {
        newFolders.push({ text: label, value: label });
    });
    newFolders.forEach(folder => {
        const newOption = document.createElement('option');
        newOption.textContent = folder.text;
        newOption.value = folder.value;
        folderSelect.appendChild(newOption);
    });
}

function getFilteredFolder() {
    let labelList = ["Updates", "Social", "Forums", "Promotions"]
    mailData.labels.forEach(label => {
        labelList.push(label)
    });
    labelList.push("Spam");
    labelList.push("Trash");
    if (!state.currentFolder.startsWith("INBOX")) {
        labelList.push("INBOX");
    }
    console.log(" state.currentCategory :::::: " + state.currentCategory)
    console.log(" state.currentFolder ::::: " + state.currentFolder)

    let folderName = state.currentFolder.startsWith("INBOX") ? state.currentCategory : state.currentFolder;

    let folderParts = folderName.split("/");
    let folderBase = folderParts[folderParts.length - 1].toLowerCase();

    let uniqueLabels = [...new Set(labelList.map(l => l.trim()))];

    let filtered = uniqueLabels.filter(label => label.toLowerCase() !== folderBase.toLowerCase());

    console.log(" filtered  ****************** " + filtered);
    console.log(" labelList ::::: " + labelList);

    return filtered;
}


//////////// Filtering & list rendering

function filteredMessages() {
    let items = mailData.messages

    return items;
}

function currentPageItems() {
    const items = filteredMessages();
    const start = state.page * state.pageSize;
    // console.log(" start :::: " + start)
    return items.slice(start, start + state.pageSize);
}

function getCheckedCount() {
    // closing the menu listing labels (add labels menu)
    moreMenu.style.display = 'none';

    const checkedCheckboxes = document.querySelectorAll('input.subCheckBox:checked');
    // console.log("checkedCheckboxes.length !!!!!!!!!!!!!!!!!!  ", checkedCheckboxes.length)
    return checkedCheckboxes.length;

}

/// date formate conversion
function formatAndAddRelativeTime(isoDateString) {
    const date = new Date(isoDateString);
    const now = new Date();

    const formattedDate = date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: 'numeric',
        hour12: true,
    });
    return `${formattedDate}`;
}

function customDateAndTimeToShow(isoDateString) {
    const date = new Date(isoDateString);
    const now = new Date();

    const diffInMilliseconds = now - date;
    const diffInDays = Math.floor(diffInMilliseconds / (1000 * 60 * 60 * 24));

    let formattedDate;
    if (diffInDays === 0) {
        formattedDate = date.toLocaleString('en-US', {
            hour: 'numeric',
            minute: 'numeric',
            hour12: true,
        });

    } else {
        formattedDate = date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
        });
    }
    return `${formattedDate}`;
}


function checkStringMatch(str1, str2) {

    if (!str1 || !str2) {
        return false;
    }
    const normalizedStr1 = str1.toLowerCase().trim();
    const normalizedStr2 = str2.toLowerCase().trim();

    if (normalizedStr1.includes(normalizedStr2) || normalizedStr2.includes(normalizedStr1)) {
        return true;
    }

    return false;
}

// compact preview (right-hand column) when single-clicking a row
function openCompactPreview(id) {
    const m = mailData.messages.find(x => x.id === id); if (!m) return;
    const prev = document.getElementById('preview');
    prev.innerHTML = `<div style="padding:14px"><h3 style="margin:0 0 6px">${m.subject}</h3><div style="color:var(--muted)">${m.from} • ${formatAndAddRelativeTime(m.date)}</div><div style="margin-top:12px;color:var(--muted)}">${m.snippet}</div></div>`;
    // mark as read on single click? optional: keep read unchanged for demo
}

//// filtering and rendering ends here 

function openDetailView(mail) {

    // call fetchBody api here
    // fetchMailBody(mail.id) // change the logic of body getting

    console.log(" Printing the  details preView id ", mail)
    state.detailOpen = true;
    state.currentDetailId = mail.id;

    detailPanel.classList.add('open');
    detailPanel.setAttribute('aria-hidden', 'false');
    renderDetail();
}

function closeDetail() {
    state.detailOpen = false;
    state.currentDetailId = null;
    detailPanel.classList.remove('open');
    detailPanel.setAttribute('aria-hidden', 'true');
}

// utility: escape HTML use when no need to show as html
// function escapeHtml(s) { return String(s || '').replace(/[&<>"']/g, (m) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m])); }

function deleteMessage(ids) {
    ids.forEach(id => {
        console.log(state)
        console.log(" deleteMessage called !!!!!!!!!!!!!! ")
        const idx = mailData.messages.findIndex(m => m.id === id);

        if (idx !== -1) {
            console.log(" idx  :::  " + idx);

            mailData.messages.splice(idx, 1); // this will remove that email only from list
            recomputeFolderCounts();
            renderAll();
            closeDetail();
        }
    });

}

function markAsRead() {

    /////////// need to create a request list of ReadAndUnreadDTO from backend just 10 min

    console.log(" printing the state  ::::: " + state.currentCategory)

    // with check box selection
    const selectedIds = Array.from(state.selected);
    console.log(" state.selected :::::: " + state.selected)
    console.log(" pritint the selectedIds  ::: " + selectedIds)

    let readBoolVal = false;
    selectedIds.forEach(selectedId => {

        const m = mailData.messages.find(x => x.id === selectedId);
        console.log(" markAsRead() :::::: m.read *** " + m.read);
        if (!m.read)
            readBoolVal = true;
    });

    console.log(" readBoolVal  $$$$$$$$$  " + readBoolVal)

    const m = mailData.messages.find(x => x.id === state.currentDetailId);

    if (!m) return;

    updateread(selectedIds, readBoolVal, m.category);

}

function toggleRead(ids) {
    // inside mail message body

    ids.forEach(id => {
        const m = mailData.messages.find(x => x.id === id);
        if (!m) return;

        // Toggle the local state first
        m.read = false;
    });

    updateread(ids, false, state.currentCategory)
    closeDetail();
}

function toggleStar(id) {
    const m = mailData.messages.find(x => x.id === id);
    console.log("   m @@@@@@@@@@@@@@@@@@@@@  " + m + " m.category ", m.category)
    if (!m) return;

    console.log(" inside the toggle star checking m.starred ***************** " + m.starred + " !m.starred  toggle star  ******** " + !m.starred)
    m.starred = !m.starred;
    updateStarred(m.id, m.starred, m.category, m.messageId);
}

function detailPrev() {
    const items = filteredMessages(); const idx = items.findIndex(x => x.id === state.currentDetailId);
    if (idx > 0) {
        state.currentDetailId = items[idx - 1].id;
        // renderDetail(); 
        detailedViewOrDraftEditMsg();
    }
}
function detailNext() {
    const items = filteredMessages(); const idx = items.findIndex(x => x.id === state.currentDetailId);
    if (idx < items.length - 1) {
        state.currentDetailId = items[idx + 1].id;
        // renderDetail(); 
        detailedViewOrDraftEditMsg();
    }
}

function detailedViewOrDraftEditMsg() {

    const m = mailData.messages.find(x => x.id === state.currentDetailId);

    if (state.currentFolder === "[Gmail]/Drafts" || m.labels.includes("Draft")) {
        let to = document.querySelector('input[placeholder="To"]');
        let cc = document.querySelector('input[placeholder="Cc"]');
        let bcc = document.querySelector('input[placeholder="Bcc"]');
        let subject = document.querySelector('input[placeholder="Subject"]');
        let body = document.querySelector('textarea');
        const attachmentsDiv = document.getElementById('attachments');

        let files = Array.from(fileInput.files);
        attachmentsDiv.innerHTML = " "; // clear the old elements 
        console.log("m.fileList.length (((((((((((((((((((((()))))))))))))))))))))) ", m.fileList.length)
        if (m.fileList.length > 0) {
            handleAttachments(m.fileList);
        }

        to.value = m.to;
        cc.value = m.cc;
        bcc.value = m.bcc;
        subject.value = m.subject;
        body.value = m.body;

        state.curMsgIdForDraftEdit = m.messageId;

        composeDialog.style.display = 'flex';
        console.log(m)
        closeDetail();
    } else {
        state.curMsgIdForDraftEdit = "";
        renderDetail();
    }
}


// helper to delete message (used by delete button)
function deleteMessageById(id) {
    const idx = mailData.messages.findIndex(m => m.id === id);
    if (idx >= 0) { mailData.messages.splice(idx, 1); recomputeFolderCounts(); renderAll(); closeDetail(); }
}

function composeFormReset() {
    composeDialog.style.display = 'flex';
    scheduleDateTime.style.display = 'none';
    scheduleSendBtn.style.display = 'none';
    state.curMsgIdForDraftEdit = "";
    composeMailform.reset();
    sendBtn.disabled = false;
    document.getElementById('attachments').innerHTML = ""

}

function saveDraftUsingTimeOut(isChangesMade) {
    clearTimeout(saveDraftTimeout);
    saveDraftTimeout = setTimeout(() => {
        console.log(" printing inside the saveDraftUsingTimeOut and isChangesMade is ", isChangesMade)
        if (isChangesMade && !forwardMessageFlag && !replyMessageFlag) {
            // param isDraft, schedule ,saveDraftUsingTimeOutFlag
            sendMail(true, false, true);
        }
    }, 60000);
}


function handleAttachments(filesList) {

    dataTransferFileList = [];

    console.log(" dataTransferFileList &&&&&&&&&&&&&&&&&&&&&&&&&&&&&  ", dataTransferFileList)

    filesList.forEach(file => {
        dataTransferFileList.push(file);
    });

    dataTransferFileList.forEach(file => {
        const span = document.createElement('span');
        span.classList.add('attachment-item');
        span.textContent = file.ogname;

        const removeIcon = document.createElement('span');
        removeIcon.classList.add('remove-icon');
        removeIcon.textContent = '✖';

        removeIcon.addEventListener('click', (e) => {
            e.stopPropagation();

            dataTransferFileList.pop(file);

            span.remove();

            isChangesMade = true;
            console.log(" isChangesMade in handleAttachments :: " + isChangesMade)
            saveDraftUsingTimeOut(isChangesMade)
        });

        span.appendChild(removeIcon);
        attachmentsDiv.appendChild(span);
    });
}


function existingFileSizeInCompose() {
    let currentSize = 0;
    dataTransferFileList.forEach(file => {
        currentSize += file.size;
    });
    return currentSize;
}

// format datetime -> DD/MM/YYYY hh:mm AM/PM
function formatDateTime(value) {
    const d = new Date(value);
    const pad = n => n.toString().padStart(2, "0");

    let day = pad(d.getDate());
    let month = pad(d.getMonth() + 1);
    let year = d.getFullYear();

    let hours = d.getHours();
    let minutes = pad(d.getMinutes());
    let ampm = hours >= 12 ? "PM" : "AM";
    hours = hours % 12;
    hours = hours ? hours : 12; // 0 -> 12
    let hoursStr = pad(hours);

    return `${day}/${month}/${year} ${hoursStr}:${minutes} ${ampm}`;
}

function sendMail(draft = false, schedule = false, saveDraftUsingTimeOutFlag = false) {

    console.log(" forwardMessageFlag ::: " + forwardMessageFlag);
    console.log(" replyMessageFlag ::: " + replyMessageFlag);

    if (forwardMessageFlag || replyMessageFlag) {

        if (composeTo.value.trim() == "" && forwardMessageFlag) {
            alert("atleast add one recepient !!!!")
            return;
        }
        composeDialog.style.display = 'none';

        if (forwardMessageFlag)
            ForwardMessageApi();

        if (replyMessageFlag)
            ReplyMessageApi();

        return;
    }

    // checking draft message is have any change or not
    if (draft && !isChangesMade) {
        composeDialog.style.display = 'none';
        console.log(" draft && !isChangesMade :: " + draft && !isChangesMade)
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    const to = document.querySelector('input[placeholder="To"]').value.split(",");
    const cc = document.querySelector('input[placeholder="Cc"]').value.split(",");
    const bcc = document.querySelector('input[placeholder="Bcc"]').value.split(",");
    const subject = document.querySelector('input[placeholder="Subject"]').value;
    const body = document.querySelector('textarea').value;

    let continueFlag = true;


    if (to[0] === "" && draft == false) {
        alert("atleast add one recepient !!!!")
        return;
    }

    const formData = new FormData();
    formData.append("subject", subject);
    formData.append("msgBody", body);

    to.forEach(v => {
        if (!emailRegex.test(v) && draft == false) {
            alert("Please enter a valid email in To field.");
            continueFlag = false
            return;
        }
        formData.append("send_to", v.trim())
    });

    if (!continueFlag) {
        return;
    }

    cc.forEach(v => formData.append("cc", v.trim()));
    bcc.forEach(v => formData.append("bcc", v.trim()));

    console.log(" Array.from(fileInput.files).length  , ", Array.from(fileInput.files).length)

    Array.from(fileInput.files).forEach(file => {
        console.log("filessssssssss")
        formData.append("Files", file);
    });

    let idList = [];
    dataTransferFileList.forEach(file => {
        idList.push(file.id);
    });

    formData.append("oldFiles", idList);

    formData.append("draft", draft);

    formData.append("gmailMessageId", state.curMsgIdForDraftEdit);

    console.log(" to :: ", to);
    for (const [key, value] of formData.entries()) {
        console.log(`${key}: ${value}`);
    }

    console.log("to[0].length !== ''", to[0] === "", " to ", to)
    console.log("cc[0].length !== ''", cc[0] === "", " cc ", cc)
    console.log("bcc[0].length !== ''", bcc[0] === "", " bcc ", bcc)
    console.log("subject !== '' ", subject !== "")
    console.log("body !== '' ", body !== "")

    console.log(" to[0] !== '' && cc[0] !== '' && bcc[0] !== '' && subject !== '' && body !== '' ",
        (to[0] !== "" || cc[0] !== "" || bcc[0] !== "" || subject !== "" || body !== ""))


    if (to[0] !== "" || cc[0] !== "" || bcc[0] !== "" || subject !== "" || body !== "") {

        if (!saveDraftUsingTimeOutFlag)
            composeDialog.style.display = 'none';

        isChangesMade = false; // reset
        /// call api
        if (schedule) {
            const selected = scheduleDateTime.value;
            if (!selected) {
                alert("Please select date and time first.");
                return;
            }
            const formatted = formatDateTime(selected);

            console.log("formatted :::::::::::::::::: " + formatted);

            formData.append("scheduledTime", formatted);

            console.log(" formData :::::::::::: " + JSON.stringify(formData))
            scheduleMailAPI(formData);
        } else {


            console.log("formData :: ", formData)
            sendMailAPI(formData, saveDraftUsingTimeOutFlag);

        }
    } else {
        composeDialog.style.display = 'none';
    }
}

function toDatetimeLocal(d) {
    const pad = n => n.toString().padStart(2, "0");
    return d.getFullYear() + "-" +
        pad(d.getMonth() + 1) + "-" +
        pad(d.getDate()) + "T" +
        pad(d.getHours()) + ":" +
        pad(d.getMinutes());
}


function getBeforeAndAfterDates(dateString, interval, value) {

    const [year, month, day] = dateString.split('/').map(Number);
    const baseDate = new Date(year, month - 1, day);

    const formatDate = (date) => {
        const d = String(date.getDate()).padStart(2, '0');
        const m = String(date.getMonth() + 1).padStart(2, '0');
        const y = date.getFullYear();
        console.log(" d : " + d + " m : " + m + " y : " + y)
        return `${d}/${m}/${y}`;
    };

    let beforeDate, afterDate;

    switch (interval.toUpperCase()) {
        case 'DAY':

            const afterDay = new Date(baseDate);
            console.log("afterDate 1: " + formatDate(afterDay))
            console.log(" value : " + value)

            console.log(String(baseDate.getDate() - value))
            console.log(String(baseDate.getDate() + parseInt(value, 10)))

            afterDay.setDate(baseDate.getDate() + parseInt(value, 10) + 1); // +1 is added for 1 day extra
            console.log("afterDate 2: " + formatDate(afterDay))
            afterDate = formatDate(afterDay);

            const beforeDay = new Date(baseDate);
            beforeDay.setDate(baseDate.getDate() - value);
            beforeDate = formatDate(beforeDay);
            break;

        case 'WEEK':
            const beforeWeek = new Date(baseDate);
            beforeWeek.setDate(baseDate.getDate() - (value * 7));
            beforeDate = formatDate(beforeWeek);

            const afterWeek = new Date(baseDate);
            afterWeek.setDate(baseDate.getDate() + (parseInt(value, 10) * 7) + 1);// +1 is added for 1 day extra
            afterDate = formatDate(afterWeek);
            break;

        case 'MONTH':
            const beforeMonth = new Date(baseDate);
            beforeMonth.setMonth(baseDate.getMonth() - value);
            beforeDate = formatDate(beforeMonth);

            const afterMonth = new Date(baseDate);
            afterMonth.setMonth(baseDate.getMonth() + parseInt(value, 10));
            afterMonth.setDate(baseDate.getDate() + 1); // +1 is added for 1 day extra
            afterDate = formatDate(afterMonth);
            break;

        case 'YEAR':
            const beforeYear = new Date(baseDate);
            beforeYear.setFullYear(baseDate.getFullYear() - value);
            beforeDate = formatDate(beforeYear);

            const afterYear = new Date(baseDate);
            afterYear.setFullYear(baseDate.getFullYear() + parseInt(value, 10));
            afterYear.setDate(baseDate.getDate() + 1); // +1 is added for 1 day extra
            afterDate = formatDate(afterYear);
            break;

        default:
            return 'Invalid interval. Use "day", "week", or "month".';
    }

    return " after:" + beforeDate + " before:" + afterDate + " ";
}



/*******************
 * document click
 *******************/

// Close menu if clicking outside
document.addEventListener('click', function (e) {
    if (!e.target.classList.contains('menu-dots')) {
        document.querySelectorAll('.menu-list').forEach(m => m.style.display = 'none');
    }
});

/*******************
 * side pannel
 *******************/

document.getElementById('categoryToggle').onclick = () => {
    const mf = document.getElementById('categoryFolders');
    mf.style.display = mf.style.display === 'none' ? 'block' : 'none';
    document.getElementById('categoryLabel').textContent = mf.style.display === 'none' ? '▼ Category' : '▲ Category';
};

document.getElementById('moreToggle').onclick = () => {
    const mf = document.getElementById('moreFolders');
    mf.style.display = mf.style.display === 'none' ? 'block' : 'none';
    document.getElementById('moreLabel').textContent = mf.style.display === 'none' ? '▼ More' : '▲ Less';
};

document.getElementById('addLabelBtn').onclick = () => {
    createLabelPrompt('')
};

/*******************
 * Tabs & Search
 *******************/
const filterIcon = document.getElementById('filterIcon');
const filterDialog = document.getElementById('filterDialog');
const filterCloseBtn = document.getElementById('filterCloseBtn');
const filterForm = document.getElementById('filterForm');
const beforeDate = document.getElementById('beforeDate');
const clearDateBtn = document.getElementById('clearDate');
const todayDateBtn = document.getElementById('todayDate');
const searchInput = document.getElementById('searchInput');

document.getElementById('tabs').addEventListener('click', (e) => {
    const tab = e.target.closest('.tab'); if (!tab) return;
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    state.currentCategory = tab.dataset.category || tab.textContent.trim();
    state.page = 0;
    fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
    // renderList();
});

searchInput.addEventListener('input', (e) => {
    state.search = e.target.value.trim();
    state.page = 0;
});

searchIcon.onclick = (e) => {
    console.log(" state.search !!!!!!!!!!!!!!!!!!! ", state.search);
    if (state.search.startsWith("label:")) {

        // Remove "label:" prefix and convert to lowercase
        let result = state.search.replace('label:', '').toLowerCase();

        // For the second case, replace hyphens with forward slashes
        result = result.replace(/-/g, '/');
        console.log("search term starts with label : result ::: ", result);
        mailData.labels.forEach(label => {
            if (label.toLowerCase() === result) {
                searchInput.value = 'label:' + result; // now result have all in lowercase
                result = label; // update result label name to call api req param
                state.currentFolder = label // updating cur folder to matching label
            }
        });
        let tab = document.getElementById('tabs');
        if (state.currentFolder == "INBOX") {
            tab.style.display = "flex";
        } else {
            tab.style.display = "none";
        }
        fetchMailWithPageAndSize(result, state.page, state.pageSize);
    } else if (state.search.startsWith("is:") || state.search.startsWith("in:") || state.search.startsWith("category:")) {
        const searchString = state.search;

        const searchFolder = folderSearchKeys.get(searchString);
        console.log("searchFolder !!!!!!!!!!!!!!!!!!!!!!  ", searchFolder)

        if (searchFolder != undefined) {
            if (searchFolder === "read") {
                fetchRedEmails()
                return;
            }
            fetchMailWithPageAndSize(searchFolder, state.page, state.pageSize);
            let tab = document.getElementById('tabs');
            tab.style.display = "none";
        } else {
            searchEmailMessage(searchString)
        }

    }
    else {
        searchEmailMessage(state.search)
    }
}

// Restrict calendar to today or earlier
beforeDate.max = new Date().toISOString().split("T")[0];

// Open dialog
filterIcon.onclick = () => filterDialog.showModal();

// Close dialog
filterCloseBtn.onclick = () => filterDialog.close();

// Clear date field
clearDateBtn.onclick = () => {
    beforeDate.value = "";
};

// Set today’s date
todayDateBtn.onclick = () => {
    beforeDate.value = new Date().toISOString().split("T")[0];
};

// Handle search form submit
filterForm.onsubmit = (e) => {
    e.preventDefault();

    // Collect form data
    const formData = new FormData(filterForm);
    const searchParams = {};
    formData.forEach((value, key) => {
        if (value.trim() !== null && value.trim() !== "") {
            if (key === "dateWithin") {
                let dateStr = value.split(" ");
                console.log("dateStr ::::: " + dateStr[0], " fsdvd ", dateStr[1])
                searchParams["dateWithinValue"] = dateStr[0];
                searchParams["dateWithinPeriod"] = dateStr[1];
            } else if (key === "date") {
                // Convert yyyy-MM-dd → yyyy/MM/dd
                const formattedDate = value.replace(/-/g, "/");
                searchParams[key] = formattedDate;
            } else {
                searchParams[key] = value;
            }
        }
        console.log(" key " + key + " and value " + value)

    });
    console.log("searchInput.value final :::::::::::::::::   " + searchInput.value)

    console.log("Search API called with:", searchParams);

    filterForm.reset();

    filterApi(searchParams);

    filterDialog.close();
};


/*******************
 * Actions top tab
 *******************/

const elementsByClass = document.getElementsByClassName('action-btn-more');
const refreshBtn = document.getElementById('refreshBtn');

const detailMore = document.getElementById('detailMore1');
const moreMenu = document.getElementById('moreMenu');

const selectAllCheckBox = document.getElementById('selectAll');
const labelMenu = document.getElementById("labelMenu");



// Close menu when clicking outside
document.addEventListener('click', () => {
    moreMenu.style.display = 'none';
});

// Prevent submenu clicks from closing menu
moreMenu.addEventListener('click', (e) => {
    e.stopPropagation();
});


// Toggle menu on button click
detailMore.onclick = (e) => {
    const selectedIds = Array.from(state.selected);

    if (selectedIds.length > 0) {
        e.stopPropagation();
        moreMenu.style.display = moreMenu.style.display === 'block' ? 'none' : 'block';
        const rect = detailMore.getBoundingClientRect();
        moreMenu.style.top = rect.bottom + "px";
        moreMenu.style.left = rect.left + "px";
        let labelList = ["Updates", "Social", "Forums", "Promotions"]
        // labelList.push(mailData.labels)

        mailData.labels.forEach(label => {
            labelList.push(label)
        });

        console.log(" labelList ::::: " + labelList);
        renderFlatMenu(labelList, labelMenu)
    } else {
        alert("select atleast one email")
    }

};

document.getElementById('selectAll').addEventListener('change', (e) => {

    console.log(" clicking toggle select all  ::::  " + e);

    const idsOnPage = mailData.messages.map(m => m.id);
    if (e.target.checked) {
        idsOnPage.forEach(id => state.selected.add(id));
        Array.from(elementsByClass).forEach(element => {
            element.style.display = 'flex';
        });
        refreshBtn.style.display = 'none';
    }
    else {
        idsOnPage.forEach(id => state.selected.delete(id));
        Array.from(elementsByClass).forEach(element => {
            element.style.display = 'none';
        });
        refreshBtn.style.display = 'flex';
    }
    renderList();
});

document.getElementById('detailImportant1').onclick = () => {
    const selectedIds = Array.from(state.selected);
    console.log(" selectedIds :::: " + selectedIds);
    console.log(" state.curfolder  ::::  " + state.currentFolder + " curr cat " + state.currentCategory);

    let folder = state.currentFolder.startsWith("INBOX") ? state.currentCategory : state.currentFolder;

    console.log("folder name : " + folder);
    important(selectedIds, folder);
};

document.getElementById('detailArchive1').onclick = () => {
    const selectedIds = Array.from(state.selected);
    console.log(" selectedIds :::: " + selectedIds);

    console.log(" state.curfolder  ::::  " + state.currentFolder + " curr cat " + state.currentCategory);

    let folder = state.currentFolder.startsWith("INBOX") ? state.currentCategory : state.currentFolder;

    console.log("folder name : " + folder);

    archieve(selectedIds, folder);
};

document.getElementById('detailInfo1').onclick = () => {

    if (state.currentCategory == "[Gmail]/Spam") {
        if (confirm('Not spam ?')) {
            const selectedIds = Array.from(state.selected);

            console.log(" selectedIds :: ", selectedIds)
            if (selectedIds.length === 0) {
                alert("No emails selected.");
                return;
            }
            move(selectedIds, state.currentFolder, "INBOX");
            deleteMessage(selectedIds);

            state.selected.clear();
            renderList();
        }

    } else {
        if (confirm('Report spam ?')) {
            const selectedIds = Array.from(state.selected);

            console.log(" selectedIds :: ", selectedIds)
            if (selectedIds.length === 0) {
                alert("No emails selected.");
                return;
            }
            move(selectedIds, state.currentFolder, "[Gmail]/Spam");
            deleteMessage(selectedIds);

            state.selected.clear();
            renderList();
        }
    }

}
document.getElementById('detailDelete1').onclick = () => {
    const selectedIds = Array.from(state.selected);
    console.log(" pritint the selectedIds  ::: " + selectedIds)
    console.log(" state.currentDetailId :::  ", state.currentDetailId)

    console.log(" selectedIds :::: " + selectedIds);
    console.log(" state.curfolder  ::::  " + state.currentFolder + " curr cat " + state.currentCategory);

    let folder = state.currentFolder.startsWith("INBOX") ? state.currentCategory : state.currentFolder;
    console.log("folder name : " + folder);


    if (state.currentFolder !== "[Gmail]/Spam" && state.currentFolder !== "[Gmail]/Trash") {
        if (confirm('Delete this email message ?')) {
            moveToTrash(selectedIds, folder);
            // deleteMessage(state.currentDetailId); 
        }
    } else {
        if (confirm('Delete this email message permanently?')) {
            deleteForever(selectedIds, folder)
            console.log("!!!!!!!!!!!")
        }
    }
};
document.getElementById('detailMarkRead1').onclick = () => {
    // alert(" outside ");
    markAsRead();
    // renderDetail();
    // renderList();
};

document.getElementById('detailMove1').onclick = () => {
    renderMoveTofolderMenuList(getFilteredFolder(), document.getElementById('detailMove1'), true)
};

document.getElementById('refreshBtn').onclick = () => {
    if (state.currentFolder == "INBOX") {
        fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
    } else {
        fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
    }
    // clear the search input
    document.getElementById('searchInput').value = "";

    recomputeFolderCounts();
    renderAll();
};

document.getElementById('prevPage').onclick = () => {
    console.log("  state.page ************ :: ", state.page)

    if (state.page > 0) {

        state.page--;

        if (state.currentFolder == "INBOX") {
            fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
        } else {
            fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
        }
        //  renderList(); 
    }
};
document.getElementById('nextPage').onclick = () => {
    console.log("  state.page ************ :: ", state.page)
    const total = filteredMessages().length;
    if (state.page + 1 < state.totalPages) {
        state.page++;

        if (state.currentFolder == "INBOX") {
            fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
        } else {
            fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
        }
        // renderList(); 
    }
};



/*******************
 * DETAILED VIEW behavior
 *******************/
const detailPanel = document.getElementById('detailPanel');
const mailHead = document.getElementById('mailHead');
const mailBody = document.getElementById('mailBody');
const detailPager = document.getElementById('detailPager');
const mailContainer = document.getElementById('mailContainer');



document.getElementById('detailBack').onclick = () => closeDetail();
// actions (these are mock behaviors)

document.getElementById('detailImportant2').onclick = () => {
    state.selected.add(state.currentDetailId) // just add cur id in selected id


    const selectedIds = Array.from(state.selected);
    console.log(" selectedIds :::: " + selectedIds);
    console.log(" state.curfolder  ::::  " + state.currentFolder + " curr cat " + state.currentCategory);

    let folder = state.currentFolder.startsWith("INBOX") ? state.currentCategory : state.currentFolder;

    console.log("folder name : " + folder);
    important(selectedIds, folder);


};

document.getElementById('detailArchive2').onclick = () => {
    state.selected.add(state.currentDetailId) // just add cur id in selected id

    const selectedIds = Array.from(state.selected);
    console.log(" selectedIds :::: " + selectedIds);
    console.log(" state.curfolder  ::::  " + state.currentFolder + " curr cat " + state.currentCategory);

    let folder = state.currentFolder.startsWith("INBOX") ? state.currentCategory : state.currentFolder;

    console.log("folder name : " + folder);
    archieve(selectedIds, folder);
};

document.getElementById('detailInfo2').onclick = () => {


    if (state.currentCategory == "[Gmail]/Spam") {
        if (confirm('Not spam ?')) {
            const selectedIds = Array.from(state.selected);

            console.log(" selectedIds :: ", selectedIds)
            if (selectedIds.length === 0) {
                alert("No emails selected.");
                return;
            }
            move(selectedIds, state.currentFolder, "INBOX");
            deleteMessage(selectedIds);

            state.selected.clear();
            renderList();
        }

    } else {
        if (confirm('Report spam ?')) {
            const selectedIds = Array.from(state.selected);

            if (selectedIds.length === 0) {
                alert("No emails selected.");
                return;
            }
            move(selectedIds, state.currentFolder, "[Gmail]/Spam");
            deleteMessage(selectedIds);

            state.selected.clear();
            renderList();
        }

    }

};

document.getElementById('detailDelete2').onclick = () => {

    const selectedIds = Array.from(state.selected);
    console.log(" pritint the selectedIds  ::: " + selectedIds)
    console.log(" state.currentDetailId :::  ", state.currentDetailId)

    console.log(" selectedIds :::: " + selectedIds);

    console.log(" state.curfolder  ::::  " + state.currentFolder + " curr cat " + state.currentCategory);

    let folder = state.currentFolder.startsWith("INBOX") ? state.currentCategory : state.currentFolder;

    console.log("folder name : " + folder);


    if (state.currentFolder !== "[Gmail]/Spam" && state.currentFolder !== "[Gmail]/Trash") {
        if (confirm('Delete this email message ?')) {
            moveToTrash(selectedIds, folder);
        }
    } else {
        if (confirm('Delete this email message permanently?')) {
            deleteForever(selectedIds, folder)
        }
    }
};
document.getElementById('detailMarkRead2').onclick = () => {

    const selectedIds = Array.from(state.selected);
    console.log(" selectedIds !!!!!!!!!!!!!!!!!!!!!!!!!!!!:::::  " + selectedIds)
    toggleRead(selectedIds);

    // renderDetail();
    // renderList();
};
document.getElementById('detailMove2').onclick = () => {
    renderMoveTofolderMenuList(getFilteredFolder(), document.getElementById('detailMove2'), false)
};

document.getElementById('detailMore2').onclick = (e) => {

    console.log(" state.baseSubMailMap.get(state.currentDetailId) : " + state.baseSubMailMap.get(state.currentDetailId));

    console.log(" state.selected id  : " + Array.from(state.selected))

    e.stopPropagation();
    moreMenu.style.display = moreMenu.style.display === 'block' ? 'none' : 'block';
    const rect = document.getElementById('detailMore2').getBoundingClientRect();
    moreMenu.style.top = rect.bottom + "px";
    moreMenu.style.left = rect.left + "px";
    let labelList = ["Updates", "Social", "Forums", "Promotions"]

    mailData.labels.forEach(label => {
        labelList.push(label)
    });

    console.log(" labelList ::::: " + labelList);
    renderFlatMenu(labelList, labelMenu)


};

document.getElementById('detailPrev').onclick = () => detailPrev();
document.getElementById('detailNext').onclick = () => detailNext();



/*******************
 * compose email
 *******************/

const composeBtn = document.getElementById('composeBtn');
const composeDialog = document.getElementById('composeDialog');
const closeBtn = document.getElementById('closeBtn');
const fileInput = document.getElementById('fileInput');
const attachmentsDiv = document.getElementById('attachments');
const composeMailform = document.getElementById("emailForm");
const sendBtn = document.querySelector(".send-btn");

const scheduleBtn = document.getElementById("scheduleBtn");
const scheduleSendBtn = document.getElementById("scheduleSendBtn");
const scheduleDateTime = document.getElementById("scheduleDateTime");
let isChangesMade = false;

const composeTo = document.getElementById("composeTo");
const composeCC = document.getElementById("composeCC");
const composeBcc = document.getElementById("composeBcc");
const composeSubject = document.getElementById("composeSubject")
const composeMsg = document.getElementById("composeMsg");
let saveDraftTimeout;
let forwardMessageFlag = false;
let replyMessageFlag = false;

composeBtn.addEventListener('click', () => {
    composeFormReset();

    document.getElementById("composeToLable").style.display = 'flex';
    document.getElementById("composeCcLable").style.display = 'flex';
    document.getElementById("composeBccLable").style.display = 'flex';
    document.getElementById("composeSubjectLable").style.display = 'flex';
    document.getElementById("compose-header-msg").innerText = "New Message";
    composeMsg.innerText = ""
    scheduleBtn.style.display = 'flex'
    forwardMessageFlag = false;
    replyMessageFlag = false;

});

let oldToVal = composeTo.value.trim().split(",");
composeTo.addEventListener('input', () => {
    let currentToVal = composeTo.value.trim().split(",");
    const oldStr = JSON.stringify(oldToVal);
    const currentStr = JSON.stringify(currentToVal);
    isChangesMade = isChangesMade || (currentStr !== oldStr);
    console.log(" isChangesMade in input :: " + isChangesMade)
    saveDraftUsingTimeOut(isChangesMade)
    oldToVal = currentToVal;
});

let oldCCVal = composeCC.value.trim().split(",");
composeCC.addEventListener('input', () => {
    let currentCCVal = composeCC.value.trim().split(",");
    const oldStr = JSON.stringify(oldCCVal);
    const currentStr = JSON.stringify(currentCCVal);
    isChangesMade = isChangesMade || (currentStr !== oldStr);
    console.log(" isChangesMade in input :: " + isChangesMade)
    saveDraftUsingTimeOut(isChangesMade)
    oldCCVal = currentCCVal;
});

let oldBCCVal = composeBcc.value.split(",");
composeBcc.addEventListener('input', () => {
    let currentBCCVal = composeBcc.value.trim().split(",");
    const oldStr = JSON.stringify(oldBCCVal);
    const currentStr = JSON.stringify(currentBCCVal);
    isChangesMade = isChangesMade || (currentStr !== oldStr);
    console.log(" isChangesMade in input :: " + isChangesMade)
    saveDraftUsingTimeOut(isChangesMade)
    currentBCCVal = oldBCCVal;
});

let oldSubVal = composeSubject.value.split(",");
composeSubject.addEventListener('input', () => {
    let currentSubCVal = composeSubject.value.trim().split(",");
    const oldStr = JSON.stringify(oldSubVal);
    const currentStr = JSON.stringify(currentSubCVal);
    isChangesMade = isChangesMade || (currentStr !== oldStr);
    console.log(" isChangesMade in input :: " + isChangesMade)
    saveDraftUsingTimeOut(isChangesMade)
    oldSubVal = currentSubCVal;
});

let oldMsgVal = composeMsg.value.split(",");
composeMsg.addEventListener('input', () => {
    let currentMsgVal = composeMsg.value.trim().split(",");
    const oldStr = JSON.stringify(oldMsgVal);
    const currentStr = JSON.stringify(currentMsgVal);
    isChangesMade = isChangesMade || (currentStr !== oldStr);
    console.log(" isChangesMade in input :: " + isChangesMade)
    saveDraftUsingTimeOut(isChangesMade)
    oldMsgVal = currentMsgVal;
});


closeBtn.addEventListener('click', () => {

    if (!forwardMessageFlag && !replyMessageFlag) {
        // param isDraft, schedule ,saveDraftUsingTimeOutFlag
        sendMail(true, false, true);
        attachmentsDiv.innerHTML = "";
    } else {
        forwardMessageFlag = false; // reset to false while closing dialog
        replyMessageFlag = false;
    }
    // composeMailform.reset();
    composeDialog.style.display = 'none';
});

const MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024;

// Handle file attachments
fileInput.addEventListener('change', () => {

    existingFileSizeInCompose

    isChangesMade = true;
    console.log(" isChangesMade in handleAttachments :: " + isChangesMade)
    saveDraftUsingTimeOut(isChangesMade)

    // Use a DataTransfer object to manage the files
    let dataTransfer = new DataTransfer();
    let oversizeFilesFound = false;
    let totalsize = existingFileSizeInCompose();

    Array.from(fileInput.files).forEach(file => {
        totalsize += file.size;
        console.log(" totalsize %%%%%%%%%%%%%%%  ::: " + totalsize);
        if (totalsize > MAX_FILE_SIZE_BYTES) {
            alert(`File "${file.name}" is too large. The maximum size is 25MB.`);
            oversizeFilesFound = true;
        } else {
            dataTransfer.items.add(file);
        }
    });

    // If any oversize files were found, the original file input remains unchanged
    if (oversizeFilesFound) {
        fileInput.value = '';
        fileInput.files = dataTransfer.files;
        return;
    }
    Array.from(dataTransfer.files).forEach(file => {
        const span = document.createElement('span');
        span.classList.add('attachment-item');
        span.textContent = file.name;

        const removeIcon = document.createElement('span');
        removeIcon.classList.add('remove-icon');
        removeIcon.textContent = '✖';

        removeIcon.addEventListener('click', (e) => {
            e.stopPropagation();
            const index = Array.from(dataTransfer.files).indexOf(file);

            if (index > -1) {
                dataTransfer.items.remove(index);
            }

            fileInput.files = dataTransfer.files;
            span.remove();

            isChangesMade = true;
            console.log(" isChangesMade in handleAttachments :: " + isChangesMade)
            saveDraftUsingTimeOut(isChangesMade)
        });

        span.appendChild(removeIcon);
        attachmentsDiv.appendChild(span);
    });
});

let dataTransferFileList = [];


/*******************
 * schedule
 *******************/

// set min/max datetime (today -> +7 days)
const now = new Date();
const maxDate = new Date();
maxDate.setDate(now.getDate() + 7);

scheduleDateTime.min = toDatetimeLocal(now);
scheduleDateTime.max = toDatetimeLocal(maxDate);

// toggle datetime input when clicking schedule
scheduleBtn.addEventListener("click", () => {

    const to = document.querySelector('input[placeholder="To"]').value.split(",");
    if (to[0] === "") {
        console.log("printing inside the atClick schedule button !!!!!!!!!!!! ")
        alert("atleast add one recepient !!!!")
        return;
    }
    scheduleDateTime.style.display = "block";
    sendBtn.disabled = true;
});

// show "Schedule Send" button only after choosing datetime
scheduleDateTime.addEventListener("change", () => {
    if (scheduleDateTime.value) {
        scheduleSendBtn.style.display = "inline-block";
    }
});

scheduleSendBtn.addEventListener("click", () => {
    const selected = scheduleDateTime.value;
    if (!selected) {
        alert("Please select date and time first.");
        return;
    }

    const formatted = formatDateTime(selected);

    console.log("Scheduling email for:", formatted);
    alert("Email scheduled for " + formatted);

    // param isDraft, schedule 
    sendMail(false, true);

});

sendBtn.addEventListener("click", () => {
    sendMail();
});


/*******************
 * render helper
 *******************/

// helper: recompute folder lists from messages
function recomputeFolderCounts() {
    Object.keys(mailData.folders).forEach(k => mailData.folders[k] = []);
    for (const m of mailData.messages) {
        if (!mailData.folders[m.folder]) mailData.folders[m.folder] = [];
        mailData.folders[m.folder].push(m.id);
        if (m.starred) {
            if (!mailData.folders.Starred) mailData.folders.Starred = [];
            if (!mailData.folders.Starred.includes(m.id)) mailData.folders.Starred.push(m.id);
        }
    }
}

function renderCoreFolders() {
    const el = document.getElementById('coreFolders');
    el.innerHTML = '';
    // recomputeFolderCounts();
    for (const f of coreFolderDefs) {
        const count = (mailData.folders[f.key] || []).length;
        const div = document.createElement('div');
        console.log("state.currentCategory " + state.currentCategory)
        div.className = 'nav-item' + (state.currentFolder === f.key ? ' active' : '');

        div.innerHTML = `<span>${f.icon}</span><span>${f.label}</span><span style="margin-left:auto;color:var(--muted)">${count || ''}</span>`;
        div.onclick = () => {
            document.getElementById('searchInput').value = f.searchKey;
            state.search = f.searchKey;
            console.log("state.currentFolder " + state.currentFolder)

            let tab = document.getElementById('tabs');
            // console.log("  printing the  tab ::::::::::::: ", tab);

            state.currentFolder = f.key;
            state.page = 0;
            state.selected.clear();

            // 🔹 Clear "active" from all nav-items
            document.querySelectorAll('.nav-item').forEach(item => {
                item.classList.remove('active');
            });

            // 🔹 Set active only for the clicked one
            div.classList.add('active');

            console.log(" printing the state.currentFolder ::::: ", state.currentFolder);
            mailData.messages = [];
            if (state.currentFolder == "INBOX") {
                // at clicking inbox setting initial category as 'INBOX/personal'
                state.currentCategory = "INBOX/personal"
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
                tab.style.display = "flex";

                const allTabs = document.querySelectorAll('.tab');
                if (allTabs.length > 0) {
                    allTabs.forEach(tab => {
                        tab.classList.remove('active');
                    });
                    allTabs[0].classList.add('active');
                }
            } else {
                fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
                tab.style.display = "none";
            }
            // renderAll();
            closeDetail();
        };
        el.appendChild(div);
    }
}

function renderCategoryFolders() {
    const el = document.getElementById('categoryFolders');
    el.innerHTML = '';
    for (const cat of mailData.category) {

        const div = document.createElement('div');
        div.className = 'nav-item';
        div.innerHTML = `<span>${cat.icon}</span><span>${cat.label}</span>`;
        div.onclick = () => {
            document.getElementById('searchInput').value = cat.searchKey;
            state.search = cat.searchKey;
            let tab = document.getElementById('tabs');
            state.currentFolder = cat.key;
            state.page = 0;
            state.selected.clear();

            // 🔹 Clear "active" from all nav-items
            document.querySelectorAll('.nav-item').forEach(item => {
                item.classList.remove('active');
            });

            // 🔹 Set active only for the clicked one
            div.classList.add('active');
            mailData.messages = [];

            fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
            tab.style.display = "none";

            // renderAll();
        };
        el.appendChild(div);
    }
}

function renderMoreFolders() {
    const el = document.getElementById('moreFolders');
    el.innerHTML = '';
    for (const item of mailData.more) {
        const div = document.createElement('div');
        div.className = 'nav-item';
        div.innerHTML = `<span>${item.icon}</span><span>${item.label}</span>`;
        div.onclick = () => {
            document.getElementById('searchInput').value = item.searchKey;
            state.search = item.searchKey;
            let tab = document.getElementById('tabs');
            state.currentFolder = item.key;
            state.page = 0;
            state.selected.clear();

            // 🔹 Clear "active" from all nav-items
            document.querySelectorAll('.nav-item').forEach(item => {
                item.classList.remove('active');
            });

            // 🔹 Set active only for the clicked one
            div.classList.add('active');

            mailData.messages = [];
            if (state.currentFolder == "newLabel") {
                createLabelPrompt('');
                return;
            }
            fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
            tab.style.display = "none";
            closeDetail();
            // renderAll();
        };
        el.appendChild(div);
    }
}

function createTree(paths) {
    const tree = {};
    paths.forEach(path => {
        const parts = path.split('/');
        let currentLevel = tree;
        parts.forEach(part => {
            if (!currentLevel[part]) {
                currentLevel[part] = {};
            }
            currentLevel = currentLevel[part];
        });
    });
    return tree;
}

function renderLabels(obj, parentEl = null, parentPath = "") {
    const el = parentEl || document.getElementById('labelsList');
    if (!parentEl) el.innerHTML = '';

    Object.keys(obj).forEach(label => {
        const wrapper = document.createElement('div');
        wrapper.className = 'label-wrapper';

        const d = document.createElement('div');
        d.className = 'label-item';

        // full path of this label
        const fullPath = parentPath ? `${parentPath}-${label}` : label;
        const hasChildren = obj[label] && Object.keys(obj[label]).length > 0;

        d.innerHTML = `
    <div class="label-row" style="display:flex; align-items:center; justify-content:space-between; width:100%;">
        
        <div class="label-left" style="display:flex; align-items:center;">
            <span style="width:18px;display:inline-block;">
                ${hasChildren ? "▶" : ""} 
            </span>
            <span>🏷️</span>
            <span style="margin-left:5px; cursor:pointer;" onclick="handleLabelClick('${fullPath}')">${label}</span>
        </div>

        <span class="menu-wrapper" style="position:relative; display:inline-block;">
            <span class="menu-dots" style="cursor:pointer;" onclick="toggleMenu(this)">⋮</span>
            <div class="menu-list" style="display:none; position:absolute; right:0; bottom:100%; background:#fff; border:1px solid #ccc; padding:5px; z-index:1000;">
                <div onclick="createLabelPrompt('${fullPath}')">Add Label</div>
                <div onclick="editLabel('${fullPath}')">Edit</div>
                <div onclick="deleteLabel('${fullPath}')">Delete</div>
            </div>
        </span>

    </div>`;

        wrapper.appendChild(d);
        if (hasChildren) {
            const childContainer = document.createElement('div');
            childContainer.style.display = "none";
            childContainer.style.marginLeft = "20px";
            wrapper.appendChild(childContainer);

            // Recursively render children with updated path
            renderLabels(obj[label], childContainer, fullPath);

            // toggle expand/collapse on arrow click
            d.querySelector("span").onclick = (e) => {
                e.stopPropagation();
                const arrow = d.querySelector("span");
                if (childContainer.style.display === "none") {
                    childContainer.style.display = "block";
                    arrow.textContent = "▼";
                } else {
                    childContainer.style.display = "none";
                    arrow.textContent = "▶";
                }
            };
        }
        el.appendChild(wrapper);
    });
}

function renderFlatMenu(list, container) {
    container.innerHTML = "";
    let commonLabel = [];
    const selectedIds = Array.from(state.selected);
    const selectedMessages = mailData.messages.filter(m => selectedIds.includes(m.id));

    list.forEach(label => {
        const labelWrapper = document.createElement('label');
        labelWrapper.classList.add('submenu-item');

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.value = label;
        checkbox.checked = false;
        checkbox.class = 'labelListCheckBox'

        // ---- calculate label presence across selected messages ----
        let count = 0;
        selectedMessages.forEach(m => {
            if (m.labels.includes(label)) {
                count++;
            }
        });

        if (count === selectedMessages.length || (label === state.currentCategory)) {
            // label is on ALL messages
            checkbox.checked = true;
            checkbox.indeterminate = false;
            commonLabel.push(label);
        } else if (count > 0) {
            // label is on SOME but not all
            checkbox.checked = false;
            checkbox.indeterminate = true;
            commonLabel.push(label);
        } else {
            // label is on NONE
            checkbox.checked = false;
            checkbox.indeterminate = false;
        }

        checkbox.onclick = (e) => {

            // checkbox.checked = !checkbox.checked;
            console.log("check box clickedd :: " + checkbox.checked)
        }

        labelWrapper.appendChild(checkbox);
        labelWrapper.append(" " + label);

        container.appendChild(labelWrapper);
    });

    const applyBtn = document.createElement('button');
    applyBtn.textContent = "Apply";
    applyBtn.classList.add("apply-btn");

    applyBtn.onclick = () => {
        // const selectedIds = Array.from(state.selected);

        console.log(" selectedIds :::: " + selectedIds);

        const checkboxes = container.querySelectorAll('input[type="checkbox"]');
        let checkedLabels = [];
        let uncheckedLabels = [];
        let indeterminateLabels = [];

        checkboxes.forEach(cb => {
            if (cb.checked) {
                checkedLabels.push(cb.value);
            } else if (cb.indeterminate) {
                indeterminateLabels.push(cb.value);
            } else {
                uncheckedLabels.push(cb.value);
            }
        });

        console.log(" Checked labels:", checkedLabels);
        console.log(" Unchecked labels:", uncheckedLabels);
        if (indeterminateLabels.length > 0)
            console.log(" interminate labels:", indeterminateLabels);


        let requestList = [];

        selectedIds.forEach(id => {

            console.log(" selected id : " + id)
            const mdata = state.messages.find(m => m.id === id);

            console.log(" mdata inside the renderFlatMenu :: " + mdata)
            let labelList = [];

            if (mdata.labels != undefined)
                console.log("mdata.labels **************** " + mdata.labels)

            // mark existing labels as false if they are unchecked
            mdata.labels.forEach(label => {
                console.log("label ::::::::::::: ", label)
                console.log("  checkedLabels.includes(label)  !!!! ", uncheckedLabels.includes(label));
                if (uncheckedLabels.includes(label)) {
                    let labelObj = {
                        [label]: false
                    };
                    labelList.push(labelObj);
                }
            });

            // mark checked labels as true
            checkedLabels.forEach(label => {
                let labelObj = {
                    [label]: true
                };
                labelList.push(labelObj);
            });

            console.log("printing inside the renderFlatMenu ", mdata)

            let obj = {
                id: mdata.id,
                folderName: mdata.category, // work for both folder and category
                // //for now i give [Gmail]/All Mail (common)
                // folderName:"[Gmail]/All Mail",
                labels: labelList,
                msgId: mdata.messageId
            };
            requestList.push(obj);

            console.log(" print the data :::::::: " + JSON.stringify(mdata.labels))
            console.log(" printing the each obj  :::: obj ", obj)
        });
        console.log(" requestList ::::: " + JSON.stringify(requestList));
        console.log(" commonLabel %%%%%%%%%%%%% ", commonLabel)
        if (requestList.length > 0) {
            updateLabelsInEmail(requestList)
        }
    };


    container.appendChild(applyBtn);
}

function renderMoveTofolderMenuList(labelList, parentEl, isIdList) {

    const menu = document.createElement("ul");
    menu.id = "dynamicLabelMenu";
    menu.style.position = "absolute";
    menu.style.background = "#fff";
    menu.style.border = "1px solid #ccc";
    menu.style.padding = "6px 0";
    menu.style.listStyle = "none";
    menu.style.minWidth = "150px";
    menu.style.boxShadow = "0px 2px 6px rgba(0,0,0,0.2)";
    menu.style.zIndex = "9999";

    const rect = parentEl.getBoundingClientRect();
    menu.style.top = rect.bottom + window.scrollY + "px";
    menu.style.left = rect.left + window.scrollX + "px";


    labelList.forEach(label => {
        const li = document.createElement("li");
        li.textContent = label;
        li.style.cursor = "pointer";
        li.style.padding = "6px 12px";
        li.onmouseover = () => li.style.background = "#eee";
        li.onmouseout = () => li.style.background = "#fff";

        li.onclick = () => {
            // state.selected.add(state.currentDetailId) // just add cur id in selected id
            // if (state.baseSubMailMap.has(state.currentDetailId)) {
            //     state.baseSubMailMap.get(state.currentDetailId).forEach(id => {
            //         state.selected.add(id);
            //     });
            // }

            const selectedIds = Array.from(state.selected);
            // const selectedIds = isIdList ? Array.from(state.selected) : Array.of(state.currentDetailId);

            console.log(" selectedIds :::: " + selectedIds);
            moveTo(label, selectedIds);// api call 
            console.log(" printing the label ::: " + label)
            menu.remove(); // close menu
        };
        menu.appendChild(li);
    });
    document.body.appendChild(menu);
    setTimeout(() => {
        document.addEventListener("click", function closeMenu(e) {
            if (!menu.contains(e.target) && e.target !== parentEl) {
                menu.remove();
                document.removeEventListener("click", closeMenu);
            }
        });
    }, 0);
}


function renderList() {
    console.log(" printing the state in renderList :::::::::::  ", state);


    //  show refresh option and hide other options every time render list 
    refreshBtn.style.display = 'flex';
    Array.from(elementsByClass).forEach(element => {
        element.style.display = 'none';
    });

    const list = document.getElementById('list');
    list.innerHTML = '';
    // const items = currentPageItems(); 
    const items = mailData.messages;
    // const total = filteredMessages().length;
    const total = state.totalEmails;

    // console.log(" printing the total mail :::::   "+ total)
    const startIndex = total ? (state.page * state.pageSize) + 1 : 0;
    const endIndex = Math.min((state.page + 1) * state.pageSize, total);
    console.log("startIndex :::: ", startIndex)
    console.log("endIndex :::::  ", endIndex)
    document.getElementById('rangeText').textContent = `${startIndex}–${endIndex} of ${total}`;

    if (total == 0) {
        const divele = document.createElement('div');
        divele.innerText = "No Email Message"
        list.appendChild(divele);
        return;
    }

    const idsOnPage = items.map(m => m.id);
    // console.log("  @@@@  ",idsOnPage);
    selectAllCheckBox.checked = idsOnPage.length > 0 && idsOnPage.every(id => state.selected.has(id));



    for (const m of items) {

        console.log(" m ----------------------  :::: " + m)

        // skip the reply mail id rendering
        // if (state.replyMailIdList.has(m.id)) {
        //     continue;
        // }

        if (m.category === "[Gmail]/Starred" && m.starred) {
            console.log(m.id, "  $$$  ", m.starred);
        }

        const row = document.createElement('div'); row.className = 'row' + (m.read ? '' : ' unread'); row.dataset.id = m.id;
        // checkbox
        const cbWrap = document.createElement('div');
        cbWrap.className = 'checkbox';
        const cb = document.createElement('input');
        cb.type = 'checkbox';
        cb.checked = state.selected.has(m.id);
        cb.className = 'subCheckBox'


        cb.onclick = (ev) => {
            ev.stopPropagation();
            state.currentDetailId = m.id
            console.log("printing the checked message labels ", m.labels)
            console.log("printing the checked message folder name ", m.folder, " and category ", m.category)
            console.log("mailData.labels ::::: ", mailData.labels)
            if (mailData.labels.includes(m.category)) {
                m.labels.push(m.category)
            }

            let count = getCheckedCount();
            console.log(" getCheckedCount() :::  " + (count > 0));
            console.log(" count :: ", count)
            if (count > 0) {
                Array.from(elementsByClass).forEach(element => {
                    element.style.display = 'flex';
                });
                refreshBtn.style.display = 'none';
            } else {
                Array.from(elementsByClass).forEach(element => {
                    element.style.display = 'none';
                });
                refreshBtn.style.display = 'flex';
            }

            // setting select check box true or false by checked count and length of sub check box 
            selectAllCheckBox.checked = (count === idsOnPage.length);
            cb.checked ? state.selected.add(m.id) : state.selected.delete(m.id);
        };

        cbWrap.appendChild(cb);

        // star
        const starWrap = document.createElement('div'); const star = document.createElement('span'); star.className = 'star'; star.innerHTML = m.starred ? '★' : '☆';
        star.title = 'Star'; star.onclick = (ev) => {

            // call api 
            console.log("   m @@@@@@@@@@@@@@@@@@@@@  " + " m.category ", m.category)
            console.log(state.currentCategory, " state.currentCategory")

            // if (m.category.startsWith("INBOX")) {
            //     updateStarred(m.id, !m.starred, "INBOX");
            // }
            // else {
            updateStarred(m.id, !m.starred, m.category, m.messageId);
            // }
            console.log(" m ::  ", m)
            console.log(" star.innerHTML == m.starred  ::::: " + !m.starred, m.id, m.folder);
            ev.stopPropagation(); m.starred = !m.starred; recomputeFolderCounts(); renderCoreFolders(); star.innerHTML = m.starred ? '★' : '☆';
        };
        starWrap.appendChild(star);

        const from = document.createElement('div'); from.className = 'from'; from.textContent = m.from;
        const subj = document.createElement('div');
        // Use a ternary operator to include the snippet part only if m.snippet exists
        const snippetString = m.snippet ? `&nbsp;<span class="snippet">- ${m.snippet}</span>` : '';
        subj.innerHTML = `<span class="subject">${m.subject}</span>${snippetString}`;
        // const subj = document.createElement('div'); subj.innerHTML = `<span class="subject">${m.subject}</span>&nbsp;<span class="snippet">- ${m.snippet}</span>`;
        const date = document.createElement('div'); date.className = 'date'; date.textContent = customDateAndTimeToShow(m.date);


        row.appendChild(cbWrap); row.appendChild(starWrap); row.appendChild(from); row.appendChild(subj); row.appendChild(date);

        // single click selects (preview area update) and double-click opens detail panel
        // row.addEventListener('click', () => { openCompactPreview(m.id); });

        row.addEventListener('dblclick', () => {
            console.log("state.currentCategory :::::: " + state.currentFolder)
            console.log(" (state.currentCategory === '[Gmail]/Drafts' ", (state.currentFolder === "[Gmail]/Drafts"));

            if (state.currentFolder === "[Gmail]/Drafts" || m.labels.includes("Draft")) {
                let to = document.querySelector('input[placeholder="To"]');
                let cc = document.querySelector('input[placeholder="Cc"]');
                let bcc = document.querySelector('input[placeholder="Bcc"]');
                let subject = document.querySelector('input[placeholder="Subject"]');
                let body = document.querySelector('textarea');
                const attachmentsDiv = document.getElementById('attachments');

                let files = Array.from(fileInput.files);
                attachmentsDiv.innerHTML = " "; // clear the old elements 
                console.log("m.fileList.length (((((((((((((((((((((()))))))))))))))))))))) ", m.fileList.length)
                if (m.fileList.length > 0) {
                    handleAttachments(m.fileList);
                }

                to.value = m.to;
                cc.value = m.cc;
                bcc.value = m.bcc;
                subject.value = m.subject;
                body.value = m.body;

                state.curMsgIdForDraftEdit = m.messageId;

                composeDialog.style.display = 'flex';
                console.log(m)
            } else {
                state.curMsgIdForDraftEdit = "";
                openDetailView(m);
            }
        });

        if (m.category === "[Gmail]/Starred" && m.starred) {
            // console.log(m.id, "  $$$  ",m.starred);
            list.appendChild(row);
        } else if (m.category !== "[Gmail]/Starred") {
            list.appendChild(row);
        }

    }
}


function renderDetail() {
    const items = mailData.messages

    const idx = items.findIndex(x => x.id === state.currentDetailId);
    if (idx === -1) {
        // if not found in filtered set, pick the first available
        if (items.length === 0) { closeDetail(); return; } else state.currentDetailId = items[0].id;
    }
    const m = mailData.messages.find(x => x.id === state.currentDetailId);
    if (!m) return;

    let replyMails = [m]

    if (state.baseSubMailMap.has(m.id)) {
        // .sort((a,b) => b - a) ---> descending
        for (let id of state.baseSubMailMap.get(m.id).sort((a, b) => b - a)) {
            const mailDet = state.messages.find(x => x.id === id);
            replyMails.push(mailDet)
        }
    }

    console.log(" replyMails.length :::: " + replyMails.length)


    // mark as read when opening detail
    // renderList();
    if (!m.read) {
        console.log(" inside renderDetail updateread is called  bodyyyy !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ")
        m.read = true;
        let requestList = [];
        let obj = {
            id: m.id,
            isRead: m.read,
            folder: m.category,
            msgId: m.messageId
        };
        requestList.push(obj);
        console.log("!!!!!!!!!!!!!!!!!!!!!!! updatere 3   :: ", m.id)
        updateread([m.id], m.read, m.category)
    }

    // toolbar pager
    const total = items.length;
    const pos = items.findIndex(x => x.id === m.id) + 1;
    detailPager.textContent = `${pos} of ${total}`;

    console.log(" detailPager.textContent , ", detailPager.textContent)

    console.log(" m.labels  &&&& " + m.labels)

    if (state.baseSubMailMap.has(m.id))
        console.log(state.baseSubMailMap.get(m.id))

    state.selected = new Set(); // reset before adding

    state.selected.add(state.currentDetailId)// just add cur id in selected id
    if (state.baseSubMailMap.has(state.currentDetailId)) {
        state.baseSubMailMap.get(state.currentDetailId).forEach(id => {
            state.selected.add(id);
        });
    }

    console.log("state.selected ::: " + Array.from(state.selected))

    // render mail details
    renderMailDetails(replyMails, mailContainer)

}



function renderMailDetails(mails, container) {
    container.innerHTML = '';

    mails.forEach((mail, index, array) => {
        if (mail.body) {
            if (index != 0) {
                mail.subject = "";
            }
            // Create the mail-head element
            const detailBodyDiv = document.createElement('div');
            detailBodyDiv.classList.add('detail-body');

            const mailHeadDiv = document.createElement('div');
            mailHeadDiv.classList.add('mail-head');
            mailHeadDiv.innerHTML = `
            <div style="display:flex;align-items:center;justify-content:space-between">
            <div style="flex:1">
              <h1>${(mail.subject)}</h1>
              <div class="mail-meta">
                <div style="display:flex;align-items:center;gap:8px"><span style="width:42px;height:42px;border-radius:50%;background:#e8f0fe;display:inline-grid;place-items:center;color:var(--accent);font-weight:700">${(mail.from[0] || '').toUpperCase()}</span>
                  <div style="display:flex;flex-direction:column">
                    <strong>${(mail.from)}</strong>
                    <span style="color:var(--muted);font-size:13px">${mail.from.toLowerCase().includes('@') ? `${mail.from} • ` : ''}${formatAndAddRelativeTime(mail.date)}</span>
                  </div>
                </div>
              </div>
            </div>
            <div style="margin-left:12px">
              <button class="meta-actions action" title="Star" onclick="(function(ev){ ev.stopPropagation(); toggleStar(${mail.id}); renderDetail(); renderList(); })(event)">${mail.starred ? '★' : '☆'}</button>
            </div>
            </div>
            <div style="margin-top:8px">${(mail.labels || []).map(l => ` <span class="pill">${(l)}</span>`).join('')}</div>
            `;

            // Create the mail-body element
            const mailBodyDiv = document.createElement('div');
            mailBodyDiv.classList.add('mail-body');

            const rowspan = document.createElement('row');

            if (mail.fileList.length > 0) {
                mail.fileList.forEach(file => {
                    console.log("file ^^^^^^^^^^^^^^^^^^^^^^^^ " + JSON.stringify(file))
                    const span = document.createElement('span');
                    span.classList.add('attachment-item');
                    span.textContent = file.ogname + file.extention + " 📥";
                    span.onclick = () => {
                        console.log(" printing the file.id ****** " + file.id)
                        fileDownload(file.id);
                    }
                    rowspan.appendChild(span);
                });
            }

            // mailBodyDiv.innerHTML = `<div>${escapeHtml(mail.body)}</div>`;
            mailBodyDiv.innerHTML = `<div>${mail.body}</div>`;

            mailBodyDiv.appendChild(rowspan);

            detailBodyDiv.appendChild(mailHeadDiv);
            detailBodyDiv.appendChild(mailBodyDiv);

            if (index == array.length - 1) {
                const buttonContainerDiv = document.createElement('div');
                buttonContainerDiv.classList.add('mail-actions-buttons');

                // reply
                const replyButton = document.createElement('button');
                replyButton.textContent = '↩ Reply';
                replyButton.onclick = () => {

                    replyMessageFlag = true;
                    composeFormReset();

                    document.getElementById("composeToLable").style.display = 'none';
                    document.getElementById("composeCcLable").style.display = 'none';
                    document.getElementById("composeBccLable").style.display = 'none';
                    document.getElementById("composeSubjectLable").style.display = 'none';
                    document.getElementById("compose-header-msg").innerText = "Reply Message";

                    composeMsg.innerText = "---------- Reply message ---------"

                    scheduleBtn.style.display = 'none'

                    addFilesInView(mail.fileList);

                    let composeTo = mail.from;
                    if (mail.from.endsWith(">")) {
                        let str1 = mail.from;
                        let startIndex = str1.indexOf("<");
                        let endIndex = str1.indexOf(">");
                        composeTo = str1.substring(startIndex + 1, endIndex);
                        console.log(composeTo);
                    }

                    document.getElementById("composeTo").value = composeTo

                };

                // forward
                const forwardButton = document.createElement('button');
                forwardButton.textContent = '↪ Forward';
                forwardButton.onclick = () => {
                    forwardMessageFlag = true;
                    composeFormReset();

                    let ids = Array.from(state.selected);
                    console.log('Mail to forward. ', ids);

                    document.getElementById("composeToLable").style.display = 'flex';
                    document.getElementById("composeCcLable").style.display = 'flex';
                    document.getElementById("composeBccLable").style.display = 'flex';
                    document.getElementById("composeSubjectLable").style.display = 'none';
                    document.getElementById("compose-header-msg").innerText = "Forward Message";

                    addFilesInView(mail.fileList);

                    composeMsg.innerText = "---------- Forwarded message ---------"

                    scheduleBtn.style.display = 'none'


                    // + mail.body;

                    // composeMsg.appendChild = `<div>---------- Forwarded message --------- </br><div>${mail.body}</div></div>`;
                    // const composeMsg = document.getElementById("composeMsg");
                    // const forwardedMessage = `<div>${mail.body}</div>`;
                    // composeMsg.insertAdjacentHTML("beforeend", forwardedMessage);


                };

                // Append the buttons to their container
                buttonContainerDiv.appendChild(replyButton);
                buttonContainerDiv.appendChild(forwardButton);

                // Append the button container to the main container after the loop has finished
                detailBodyDiv.appendChild(buttonContainerDiv);
                const br = document.createElement('br');
                detailBodyDiv.appendChild(br);
            }

            container.appendChild(detailBodyDiv);
        }
    });
    // attach a replay and forword email buttom in end of the body

}


function addFilesInView(fileList) {
    const attachmentsDiv = document.getElementById('attachments');

    attachmentsDiv.innerHTML = " "; // clear the old elements 
    console.log("fileList.length (((((((((((((((((((((()))))))))))))))))))))) ", fileList.length)
    if (fileList.length > 0) {
        handleAttachments(fileList);
    }
}

/*******************
 * navigation helpers
 *******************/
function renderAll() { renderCoreFolders(); renderCategoryFolders(); renderMoreFolders(); renderLabels(createTree(mailData.labels)); renderList(); }



/*******************
 * API CALLS
 *******************/

// sync email
function syncEmail(pfolderName) {

    fetch("http://localhost:8080/emails/sync", {
        method: "POST",
        headers: {
            folder: pfolderName
        }
    })
        // .then(res => res.json())
        .then(data => {
            console.log(" 1  sync folder ")
            console.log(data);
        })
        .catch(err => {
            console.error(err);
        });

}

// action top tab api call

function move(pids, pfolderName, pnewFolderName) {
    showLoader();
    console.log(" parameters  ::::::::::  ", pids, " folder name ", pfolderName, " new folder name ", pnewFolderName);
    fetch("http://localhost:8080/emails/move", {
        method: "POST",
        headers: {
            ids: pids,
            folderName: pfolderName,
            newFolderName: pnewFolderName
        }
    })
        .then(res => res.json())
        .then(data => {
            console.log(data);
            if (data.status !== 200) {
                alert(res.msg);
            }

            if (state.currentFolder == "INBOX") {
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
            } else {
                fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
            }
            Array.from(elementsByClass).forEach(element => {
                element.style.display = 'none';
            });
            refreshBtn.style.display = 'flex';
            // renderAll();

            // renderDetail();
            // renderList();
        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });

}

function deleteForever(pids, pfolderName) {
    console.log(" 1 ");
    console.log(" pids ", pids);
    console.log(" folderName ", pfolderName)

    showLoader();

    fetch("http://localhost:8080/emails/deleteForever", {
        method: "POST",
        headers: {
            ids: pids,
            folderName: pfolderName
        }
    })
        .then(res => res.json())
        .then(data => {
            console.log(" data ", data);

            if (data.status !== 200) {
                alert(data.msg);
            }

            if (state.currentFolder == "INBOX") {
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
            } else {
                fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
            }
            // syncEmail("[Gmail]/Trash");
            deleteMessage(pids);

            // renderDetail();
            // renderList();
        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });

}


function moveToTrash(pids, pfolderName) {
    console.log(" 1 ");
    console.log(" pids ", pids);
    console.log(" folderName ", pfolderName)

    showLoader();
    let csrfHeaderName = state.csrfHeaderName;
    let csrfToken = state.csrfToken;

    console.log(" state.csrfToken :: "+state.csrfToken)

    fetch("http://localhost:8080/emails/delete", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-CSRF-TOKEN" :csrfToken,
            ids: pids,
            folderName: pfolderName,
        }
    })
        .then(res => res.json())
        .then(data => {
            if (data.status !== 200) {
                alert(data.msg);
            }
            console.log(" data ", data.data);
            console.log(" move to trash response ########################################")
            if (state.currentFolder == "INBOX") {
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
            } else {
                fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
            }
            // syncEmail("[Gmail]/Trash");
            deleteMessage(pids);
            // renderDetail();
            // renderList();
        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });

}

//  starred, read, label manipulation on email

function updateread(requestList, pIsRead, pfolderName) {

    showLoader();

    console.log(" requestList :: ", requestList)
    console.log(" pIsRead :: ", pIsRead)
    console.log(" pfolderName :: ", pfolderName)
    console.log(" JSON.stringify(requestList) ::: ", JSON.stringify(requestList))

    fetch("http://localhost:8080/emails/read", {
        method: "POST",
        headers: {
            // 'Content-Type': 'application/json',
            ids: requestList,
            isRead: pIsRead,
            folderName: pfolderName

        },
        // body: JSON.stringify(requestList),
    })
        .then(res => res.json())
        .then(data => {

            if (data.status !== 200) {
                alert(data.msg);
            }
            console.log("requestList :::: inside after response ::: " + JSON.stringify(requestList))

            console.log(data);
            if (data.msg == 'S') {
                console.log("state.currentFolder ::: " + state.currentFolder, "state.currentCategory ::: " + state.currentCategory)
                if (state.currentFolder == "INBOX") {
                    fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
                } else {
                    fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
                }

            }
            // renderDetail();
            // renderList();
        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });

}

function updateStarred(pid, pstarred, pfolderName, pmessageId) {
    console.log(" pid ", pid)
    console.log(" pstarred ", pstarred)
    console.log(" pfolderName ", pfolderName)
    console.log(" pmessageId ", pmessageId)

    showLoader();

    fetch("http://localhost:8080/emails/starred", {
        method: "POST",
        headers: {
            id: pid,
            starred: pstarred,
            folderName: pfolderName,
            msgId: pmessageId
        }
    })
        .then(res => res.json())
        .then(data => {

            if (data.status !== 200) {
                alert(data.msg);
            }
            console.log(data);
            console.log("state.currentFolder ::: " + state.currentFolder, "state.currentCategory ::: " + state.currentCategory)
            if (state.currentFolder == "INBOX") {
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
            } else {
                fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
            }

        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });

}

function sendMailAPI(pformData, saveDraftUsingTimeOutFlag) {
    const dataObject = Object.fromEntries(pformData.entries());
    console.log(dataObject);

    console.log(dataObject.gmailMessageId, " gmail message id !!!!!!!!!!!!!!")

    console.log("state.curMsgIdForDraftEdit ::::: ", state.curMsgIdForDraftEdit)
    console.log(" saveDraftUsingTimeOutFlag :: " + saveDraftUsingTimeOutFlag)
    console.log(" pformData :::: " + JSON.stringify(pformData))

    if (!saveDraftUsingTimeOutFlag)
        showLoader();

    fetch("http://localhost:8080/emails/sendMail", {
        method: "POST",
        body: pformData,
    })
        .then(res => res.json())
        .then(data => {
            if (data.status === 200 && pformData.draft == false) {
                alert("Mail sent successfully!");
            } else {
                if (data.msg != undefined && pformData.draft == false)
                    alert(data.msg);
            }
            // if (data.status === 200 && draft == true) {
            //     alert("Mail save in draft successfully!");
            // }
            console.log(" data !!!!!!!!!!!!", JSON.stringify(data));
            if (!saveDraftUsingTimeOutFlag) {
                attachmentsDiv.innerHTML = '';
                state.curMsgIdForDraftEdit = "";
                composeMailform.reset();
                dataTransferFileList = [];
            } else {
                syncFolder('[Gmail]/Drafts');
                console.log(" sync folder is call for gmail draft folder while saving draft ")
                if (data.msgId != undefined && data.msgId != null) {
                    state.curMsgIdForDraftEdit = data.msgId;
                }
            }
        })
        .catch(err => {
            // alert("Failed to send mail");
            console.error(err);
        }).finally(() => {
            if (!saveDraftUsingTimeOutFlag)
                hideLoader();
        });
}

async function scheduleMailAPI(pformdata) {

    console.log("request data from  scheduleMailAPI ::: " + pformdata)
    showLoader();

    fetch("http://localhost:8080/emails/schedule", {
        method: "POST",
        body: pformdata,
    })
        .then(res => res.json())
        .then(data => {
            if (data.status === 200) {
                if (pformdata.draft == false)
                    alert("Mail scheduled sent successfully!");
            } else {
                alert(data.msg);
            }
            // if (data.status === 200 && draft == true) {
            //     alert("Mail save in draft successfully!");
            // }
            console.log(" data !!!!!!!!!!!!", JSON.stringify(data));
            attachmentsDiv.innerHTML = '';
            state.curMsgIdForDraftEdit = "";
            composeMailform.reset();
        })
        .catch(err => {
            // alert("Failed to send mail");
            console.error(err);
            // composeMailform.reset();
        }).finally(() => {
            hideLoader();
        });
}

function important(pIds, pFolderName) {
    showLoader();

    console.log("pIds ::::::  " + pIds);
    console.log("folderName :::::: " + pFolderName)

    fetch("http://localhost:8080/emails/importantFlag", {
        method: "POST",
        headers: {
            'Content-Type': 'application/json',
            ids: pIds,
            folderName: pFolderName,
        },
    })
        .then(res => res.json())
        .then(res => {
            console.log(" importantFlag Email   ")
            console.log("data !!!!!!! ", res);
            if (res.status !== 200) {
                alert(res.msg);
            }

            if (state.currentFolder == "INBOX") {
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
            } else {
                fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
            }

        })
        .catch(err => {
            console.error(err);
            moreMenu.style.display = 'none';
        }).finally(() => {
            hideLoader();
        });
}

function archieve(pIds, pFolderName) {

    showLoader();

    console.log("pIds ::::::  " + pIds);
    console.log("folderName :::::: " + pFolderName)

    fetch("http://localhost:8080/emails/archive", {
        method: "POST",
        headers: {
            'Content-Type': 'application/json',
            ids: pIds,
            folderName: pFolderName,
        },
    })
        .then(res => res.json())
        .then(res => {
            console.log(" archive Email   ")
            console.log("data !!!!!!! ", res);
            if (res.status !== 200) {
                alert(res.msg);
            }
            // console.log(res.data);
            // renderList();
            console.log("state.currentFolder ::: " + state.currentFolder, "state.currentCategory ::: " + state.currentCategory)
            if (state.currentFolder == "INBOX") {
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
            } else {
                fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
            }

        })
        .catch(err => {
            console.error(err);
            moreMenu.style.display = 'none';
        }).finally(() => {
            hideLoader();
        });
}

function moveTo(newFolder, idList) {
    showLoader();
    console.log(" printing inside moveTo ::::::::::::: newFolder ", newFolder, " idList ", idList)

    let curfolder = state.currentFolder.startsWith("INBOX") ? state.currentCategory : state.currentFolder;

    fetch("http://localhost:8080/emails/move", {
        method: "POST",
        headers: {
            'Content-Type': 'application/json',
            ids: idList,
            folderName: curfolder,
            newFolderName: newFolder,
        },
    })
        .then(res => res.json())
        .then(res => {
            console.log("data !!!!!!! ", res);
            console.log(res.data);
            if (res.status !== 200) {
                alert(res.msg);
            }
            if (state.currentFolder == "INBOX") {
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize);
            } else {
                fetchMailWithPageAndSize(state.currentFolder, state.page, state.pageSize);
            }

        })
        .catch(err => {
            console.error(err);
            moreMenu.style.display = 'none';
        }).finally(() => {
            hideLoader();
        });
}

function updateLabelsInEmail(requestList) {

    showLoader()

    console.log(" printing the requestList ::::::::::::: ", requestList)

    fetch("http://localhost:8080/emails/updateLabelsInEmail", {
        method: "POST",
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestList),
    })
        .then(res => res.json())
        .then(res => {
            console.log(" updateLabelsInEmail   ")
            console.log("data !!!!!!! ", res);
            console.log(res.data);
            moreMenu.style.display = 'none';
            if (res.status !== 200) {
                alert(res.msg);
            }

        })
        .catch(err => {
            console.error(err);
            moreMenu.style.display = 'none';
        }).finally(() => {
            hideLoader();
        });
}

function searchEmailMessage(pSearchTerm) {
    showLoader();

    console.log(" pSearchTerm inside searchEmailMessage :: ", pSearchTerm)
    fetch("http://localhost:8080/emails/search", {
        method: "GET",
        headers: {
            searchTerm: pSearchTerm
        }
    })
        .then(res => res.json())
        .then(res => {
            console.log(" searchTerm  ")
            console.log("data !!!!!!! ", res);
            console.log(res.data);

            if (res.status !== 200) {
                alert(res.msg);
            }

            let mailList = res.data;

            mailData.messages = [];
            mailList.forEach(mail => {

                let parentFolder = mail.folder.split("/")
                // console.log(parentFolder[0], " @@@@ ", parentFolder[1])
                let mailMessage = {
                    id: mail.id, folder: parentFolder[0],
                    category: mail.folder, from: mail.fromAddr,
                    subject: mail.subject,
                    snippet: mail.snippet,
                    body: mail.bodyCached,
                    date: mail.sentAt,
                    starred: mail.starred,
                    read: mail.read, labels: mail.labels,
                    body: mail.body,
                    messageId: mail.messageId,
                    to: mail.toAddr,
                    cc: mail.cc,
                    bcc: mail.bcc,
                    fileList: mail.fileList,
                }

                mailData.messages.push(mailMessage);
            });
            let tab = document.getElementById('tabs');
            tab.style.display = "none";
            state.totalEmails = mailList.length;
            state.totalPages = 1;
            renderList();

            // clearing the search input.
            document.getElementById('searchInput').value = "";

        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });
}



function filterApi(filterFormData) {
    showLoader();

    console.log(" filterFormData ::::::::::::::::::: ", filterFormData)

    console.log(" JSON.stringify(filterFormData) :::::::::::::::::::: " + JSON.stringify(filterFormData))

    let searchString = "";
    let sizeRange = "";
    let sizeValue = "";
    let dateValue = "";
    let dateInterval = "";

    const formDataRequest = new FormData();
    Object.keys(filterFormData).forEach(key => {

        let value = filterFormData[key];
        formDataRequest.append(key, value);

        console.log("key : " + key + " value : " + value)
        switch (key) {
            case "fromAdd":
                searchString += "from:(" + value + ") ";
                break;
            case "toAdd":
                searchString += "to:(" + value + ") ";
                break;
            case "subject":
                searchString += key + ":" + value + " ";
                break;
            case "hasTheWord":
                searchString += value + " ";
                break;
            case "doesNotHave":
                searchString += "-" + value + " ";
                break;
            case "sizeRange":
                sizeRange = value.startsWith("GT") ? "larger" : "smaller";
                break;
            case "sizeValue":
                sizeValue = sizeRange + ":" + value;
                break;
            case "size":
                searchString += sizeValue.trim() != "" ? sizeValue + value : "";
                break;
            case "hasAttachment":
                searchString += "has:attachment ";
                break;
            case "dateWithinValue":
                dateValue = value;
                break;
            case "dateWithinPeriod":
                dateInterval = value;
                break;
            case "date":
                searchString += getBeforeAndAfterDates(value, dateInterval, dateValue);
                break;
            case "folder":
                searchString += value + " ";
                break;

            default:
            case key:
                // searchString += key + ":" + value + " ";
                break;
        }
    });


    console.log("Inspecting formDataRequest with Array.from():");
    console.log(Array.from(formDataRequest.entries()));

    fetch("http://localhost:8080/emails/filter", {
        method: "POST",
        body: formDataRequest,
    })
        .then(res => res.json())
        .then(res => {
            console.log(" Filter API  ")
            console.log("data !!!!!!! ", res);
            console.log(res.data);

            if (res.status !== 200) {
                alert(res.msg);
            }

            let mailList = res.data;

            mailData.messages = [];
            mailList.forEach(mail => {

                let parentFolder = mail.folder.split("/")
                // console.log(parentFolder[0], " @@@@ ", parentFolder[1])
                let mailMessage = {
                    id: mail.id, folder: parentFolder[0],
                    category: mail.folder, from: mail.fromAddr,
                    subject: mail.subject,
                    snippet: mail.snippet,
                    body: mail.bodyCached,
                    date: mail.sentAt,
                    starred: mail.starred,
                    read: mail.read, labels: mail.labels,
                    body: mail.body,
                    messageId: mail.messageId,
                    to: mail.toAddr,
                    cc: mail.cc,
                    bcc: mail.bcc,
                    fileList: mail.fileList,
                }
                mailData.messages.push(mailMessage);
            });
            let tab = document.getElementById('tabs');
            tab.style.display = "none";
            state.totalEmails = mailList.length;
            state.totalPages = 1;
            document.getElementById('searchInput').value = searchString;
            renderList();

        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });

}

async function fetchMailBody(ID) {

    await fetch('http://localhost:8080/emails/body', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            id: ID,
        },
    })
        .then(response => response.json())
        .then(res => {
            // console.log(" typeOf  ::::  ", typeof (JSON.stringify(res.data)));
            // console.log(" res.data.length :: ", res.data.length);
            console.log(" res.data :: ", res.data);

            if (ID == res.data.emailId) {
                // mailData.messages[ID].body = res.data.body;


                mailData.messages.forEach(msg => {
                    if (msg.id == ID) {
                        // console.log(" msg before !!!!!! "+msg.body);
                        // console.log("***********************")
                        msg.body = res.data.body;
                        // console.log(" msg after !!!!! "+msg.body);

                        renderDetail();

                        detailPanel.classList.add('open');
                        detailPanel.setAttribute('aria-hidden', 'false');
                    }
                });
            }

        })
        .catch(error => {
            console.error(' labels failed:', error);
        });
}

function manipulateLabels(labelName, manipulateAction, newLName = "") {

    console.log(" labelName :::::::::: " + labelName);
    console.log(" manipulateAction ::::::::::::  " + manipulateAction)
    console.log(" newLName ::::::::::::::::: " + newLName)
    showLoader();

    fetch('http://localhost:8080/emails/addLabel', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            label: labelName,
            action: manipulateAction,
            newLabelName: newLName,
        },
    })
        .then(response => response.json())
        .then(res => {
            // console.log(" typeOf  ::::  ", typeof (JSON.stringify(res.data)));
            // console.log(" res.data.length :: ", res.data.length);
            // console.log(" res.data :: ", res.data);
            if (res.status !== 200) {
                alert(res.msg);
            }

            console.log(" JSON.stringify(res.data) :::::: " + JSON.stringify(res.data))

            mailData.labels = res.data;
            renderLabels(createTree(mailData.labels));
            updateSearchFilterFolderList(res.data);
            if (res.data.length == 0) {
                alert(res.errMsg);
                fetchLabels();
            }

        })
        .catch(error => {
            console.error(' labels failed:', error);
        }).finally(() => {
            hideLoader();
        });
}

let labelsLength = 0;
async function fetchLabels() {
    fetch('http://localhost:8080/emails/labels', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then(response => response.json())
        .then(res => {
            // console.log("fetchLabels res ::::: " + JSON.stringify(res))
            if (res.status === 200 && labelsLength != res.data.length) {
                if (res.data !== undefined && res.data !== null && res.data.length > 0) {
                    // console.log(" res.data  :::::::::::::::: " + res.data)
                    mailData.labels = res.data;
                    updateSearchFilterFolderList(res.data)
                }

                console.log(' labels  : ', mailData.labels)
                renderLabels(createTree(mailData.labels));
            }
        })
        .catch(error => {
            console.error(' labels failed:', error);
        });
}


/// fetch api to get email from database 

function fetchMailWithPageAndSize(folder, page, size, callsyncfolder = true) {

    fetch('http://localhost:8080/emails/mailData', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'folder': folder,
            'page': page,
            'size': size
        },
        // body: JSON.stringify({ content: content }),
    })
        .then(response => response.json())
        .then(res => {

            if (res.status !== 200) {
                alert(res.msg);
            }

            // [Gmail]/Snoozed folder is not present in db and imap , using in js for folder refrence.
            if (callsyncfolder && folder !== '[Gmail]/Snoozed') {
                // console.log("Sync folder calleddd !!!!!!!!!!!!!!!!!!!!!! ")
                syncFolder(folder);
            }

            // console.log("printing the back end response data.content : " + JSON.stringify(res))
            //  mailData.messages.push(...data.data.content)  

            let mailList = res.data.content;

            mailData.messages = [];
            state.messages = [];
            state.baseSubMailMap = new Map();
            state.replyMailIdList = new Set();
            state.selected = new Set();// reset

            let emap = new Map();
            mailList.forEach(mail => {

                let parentFolder = mail.folder.split("/")
                // console.log(parentFolder[0], " @@@@ ", parentFolder[1])
                let mailMessage = {
                    id: mail.id,
                    folder: parentFolder[0],
                    category: mail.folder,
                    from: mail.fromAddr,
                    subject: mail.subject,
                    snippet: mail.snippet,
                    body: mail.bodyCached,
                    date: mail.sentAt,
                    starred: mail.starred,
                    read: mail.read, labels: mail.labels,
                    body: mail.body,
                    messageId: mail.messageId,
                    to: mail.toAddr,
                    cc: mail.cc,
                    bcc: mail.bcc,
                    fileList: mail.fileList,
                }

                state.messages.push(mailMessage);

                // console.log("!mail.subject.startsWith('Re')  " + mail.id)

                // && !mail.subject.startsWith("Fwd:") //|| mail.folder == '[Gmail]/Sent Mail'
                if (mail.subject == null || (!mail.subject.startsWith("Re")) || mail.folder == '[Gmail]/Starred') {
                    mailData.messages.push(mailMessage);
                }

                const val = {
                    to: mail.toAddr,
                    from: mail.fromAddr,
                    sub: mail.subject,
                    subMail: mail.subject != null ? mail.subject.startsWith("Re") : false
                };
                // console.log("val : " + JSON.stringify(val) + " id : " + mail.id)
                emap.set(mail.id, val);
            });

            console.log(" mailData.messages.length  :: " + mailData.messages.length)

            for (let m of state.messages) {
                const targetTo = m.to;
                const targetFrom = m.from;
                const targetSub = m.subject;

                emap.forEach((mailInfo, mailId) => {
                    if (checkStringMatch(mailInfo.to, targetTo) && checkStringMatch(mailInfo.from, targetFrom)
                        && checkStringMatch(mailInfo.sub, targetSub) && (m.id !== mailId) && mailInfo.subMail) {
                        state.replyMailIdList.add(mailId);

                        // mailData.messages.pop()
                        if (state.baseSubMailMap.has(m.id)) {
                            state.baseSubMailMap.get(m.id).push(mailId);
                        } else {
                            state.baseSubMailMap.set(m.id, [mailId]);
                        }
                    }
                });
            }

            let pageDetails = res.data.page;
            // state.totalEmails = pageDetails.totalElements // old logic 
            state.totalEmails = mailData.messages.length;
            state.totalPages = pageDetails.totalPages;
            state.currentCategory = folder;


            // console.log("  @@@@@@  pageDetails  ", pageDetails);

            if (folder == "INBOX/personal") {
                // recomputeFolderCounts();
                renderAll();
            } else {
                renderList();
            }

            if (getCheckedCount() > 0) {
                Array.from(elementsByClass).forEach(element => {
                    element.style.display = 'flex';
                });
                refreshBtn.style.display = 'none';
            } else {
                Array.from(elementsByClass).forEach(element => {
                    element.style.display = 'none';
                });
                refreshBtn.style.display = 'flex';
            }

            // console.log('emails/mailData:', res);
        })
        .catch(error => {
            console.error('emails/mailData failed:', error);
        });
}


async function syncFolder(pFolderName) {
    // console.log(" printing the pFolderName ::::::::::::: ", pFolderName)

    fetch("http://localhost:8080/emails/syncFolder", {
        method: "GET",
        headers: {
            // 'Content-Type': 'application/json',
            folderName: pFolderName
        },
    })
        .then(res => res.json())
        .then(res => {
            // console.log(" printing the syncFolder response :: ", res)
            if (res.data) {
                // console.log(" again fetch mail with page and size is calledd !!!!!!!!!!!!!!!!!!")
                fetchMailWithPageAndSize(pFolderName, state.page, state.pageSize, false)
            }
        })
        .catch(err => {
            console.error(err);
        })
}


function fetchRedEmails() {
    showLoader();

    fetch("http://localhost:8080/emails/fetchRedEmails", {
        method: "GET",
        headers: {
            'Content-Type': 'application/json',
        },
    })
        .then(res => res.json())
        .then(res => {
            console.log(" printing the fetchRedEmails response :: ", res)

            if (res.status !== 200) {
                alert(res.msg);
            }

            let mailList = res.data;

            mailData.messages = [];
            mailList.forEach(mail => {

                let parentFolder = mail.folder.split("/")
                // console.log(parentFolder[0], " @@@@ ", parentFolder[1])
                let mailMessage = {
                    id: mail.id, folder: parentFolder[0],
                    category: mail.folder, from: mail.fromAddr,
                    subject: mail.subject,
                    snippet: mail.snippet,
                    body: mail.bodyCached,
                    date: mail.sentAt,
                    starred: mail.starred,
                    read: mail.read, labels: mail.labels,
                    body: mail.body,
                    messageId: mail.messageId,
                    to: mail.toAddr,
                    cc: mail.cc,
                    bcc: mail.bcc,
                    fileList: mail.fileList,
                }
                mailData.messages.push(mailMessage);
            });

            let tab = document.getElementById('tabs');
            tab.style.display = "none";
            state.totalEmails = mailList.length;
            state.totalPages = 1;
            renderList();
        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });
}




function fileDownload(pid) {

    const baseUrl = "http://localhost:8080/file/download";

    const url = `${baseUrl}/${pid}`;

    fetch(url, {
        method: "GET",
    })
        .then(async res => {
            if (!res.ok) {
                throw new Error(`HTTP error! Status: ${res.status}`);
            }

            console.log(" res  " + res.headers + "JSON.stringify(res.headers)" + JSON.stringify(res.headers))
            console.log(" res.body " + res.body)
            // Extract the filename from the Content-Disposition header
            const contentDisposition = res.headers.get('Content-Disposition');
            console.log("contentDisposition &&&&&&&&&&&&&&& " + contentDisposition)
            let filename = 'downloaded-file';
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
                console.log("filenameMatch %%%%%%%%%%%% " + filenameMatch)
                if (filenameMatch && filenameMatch[1]) {
                    filename = filenameMatch[1];
                }
            }

            // Read the response as a Blob
            const blob = await res.blob();
            return ({ blob, filename });
        })
        .then(({ blob, filename }) => {
            console.log("File downloaded as a blob:", blob);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            a.download = filename;

            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            a.remove();
        })
        .catch(err => {
            console.error("File download failed:", err);
        });
    // .then(res => res.blob())
    // .then(res => {
    //     console.log(" printing the file download response :: ", res)
    // })
    // .catch(err => {
    //     console.error(err);
    // })

}

function ForwardMessageApi() {

    showLoader();
    console.log(" ForwardMessageApi is called............")

    const formData = new FormData();
    formData.append("ids", Array.from(state.selected));
    formData.append("to", composeTo.value.trim().split(","));
    formData.append("cc", (composeCC.value.trim() == "") ? [] : composeCC.value.trim().split(","));
    formData.append("bcc", (composeBcc.value.trim() == "") ? [] : composeBcc.value.trim().split(","));
    formData.append("content", composeMsg.value.trim());
    formData.append("folderName", state.currentCategory);

    console.log(" Array.from(fileInput.files).length  , ", Array.from(fileInput.files).length)

    Array.from(fileInput.files).forEach(file => {
        console.log("filessssssssss")
        formData.append("Files", file);
    });

    for (const [key, value] of formData.entries()) {
        console.log(`${key}: ${value}`);
    }

    fetch("http://localhost:8080/emails/forwardEmail", {
        method: "POST",
        body: formData,
    })
        .then(res => res.json())
        .then(res => {
            alert(res.msg);
            console.log(" printing the syncFolder response :: ", res)
            if (res.status === 200) {
                console.log(" again fetch mail with page and size is calledd !!!!!!!!!!!!!!!!!! " + res.data)
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize, false)
            }
        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });
}

function ReplyMessageApi() {

    showLoader();
    console.log(" ReplyMessageApi is called............")

    let id = state.currentDetailId;
    // if(state.selected.size > 1){
    //     state.selected.sort((a, b) => a - b)
    //     id = state.selected.
    // }

    const formData = new FormData();
    formData.append("id", id);
    formData.append("content", composeMsg.value.trim());
    formData.append("folderName", state.currentCategory);

    console.log(" Array.from(fileInput.files).length  , ", Array.from(fileInput.files).length)

    Array.from(fileInput.files).forEach(file => {
        console.log("filessssssssss")
        formData.append("Files", file);
    });

    for (const [key, value] of formData.entries()) {
        console.log(`${key}: ${value}`);
    }

    fetch("http://localhost:8080/emails/replyEmail", {
        method: "POST",
        body: formData,
    })
        .then(res => res.json())
        .then(res => {
            alert(res.msg);
            console.log(" printing the syncFolder response :: ", res)
            if (res.status === 200) {
                console.log(" again fetch mail with page and size is calledd !!!!!!!!!!!!!!!!!! " + res.data)
                fetchMailWithPageAndSize(state.currentCategory, state.page, state.pageSize, false)
            }
        })
        .catch(err => {
            console.error(err);
        }).finally(() => {
            hideLoader();
        });
}

function csrfToken() {
    fetch("http://localhost:8080/csrf/token", {
        method: "GET",
    })
        .then(res => res.json())
        .then(res => {
            console.log(" printing the csrfToken response :: ", res)
            if(res == null){
       
            }
            console.log("response.token :: "+res.token);
     
            state.csrfHeaderName = res.headerName;
            state.csrfToken = res.token;
            console.log("state.csrfHeaderName :: "+state.csrfHeaderName)
            console.log("state.csrfToken :: "+state.csrfToken);
 
        })
        .catch(err => {
            state.csrfHeaderName = "";
            state.csrfToken = "";
            console.error(err);
        })
}



