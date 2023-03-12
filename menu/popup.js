//Popup menu script - communicating with background.js
function start() {
    chrome.runtime.sendMessage({get_storage: true}, (response) => {
        document.getElementById("saveWordlist").checked = response.storage_enabled;
        document.getElementById("answerTime").value = response.answer_time;
        document.getElementById("flawMarge").value = response.flaw_marge;
        document.getElementById("autoClose").checked = response.auto_close;

        document.getElementById("saveWordlist").addEventListener("click", () => setSaveWordlist());
        document.getElementById("autoClose").addEventListener("click", () => setAutoClose());
        document.getElementById("answerTime").addEventListener("input", () => setAnswerTime());
        document.getElementById("flawMarge").addEventListener("input", () => setFlawMarge());
    });
}

function setSaveWordlist() {
    const wordlist = document.getElementById("saveWordlist");
    chrome.runtime.sendMessage({set_storage_enabled: wordlist.checked}, () => {
    });
}

function setAutoClose() {
    const autoClose = document.getElementById("autoClose");
    chrome.runtime.sendMessage({set_auto_close: autoClose.checked}, () => {
    });
}

function setAnswerTime() {
    const answerTime = document.getElementById("answerTime");
    let value = answerTime.value;

    if (value > 999)
        value = 999;
    else if (value < 1)
        value = 1;

    answerTime.value = value;
    chrome.runtime.sendMessage({set_answer_time: value}, () => {
    });
}

function setFlawMarge() {
    const flawMarge = document.getElementById("flawMarge");
    let value = flawMarge.value;

    if (value > 99)
        value = 99;
    else if (value < 0)
        value = 25;

    flawMarge.value = value;
    chrome.runtime.sendMessage({set_flaw_marge: value}, () => {
    });
}

start();