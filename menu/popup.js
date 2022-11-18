async function start() {
    let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

    chrome.tabs.sendMessage(tab.id, {storage: "GET"}, function (response) {
            if (chrome.runtime.lastError) {
                document.getElementById("instruction").remove();
                const element = document.getElementsByClassName("option");

                if (element.length > 0) {
                    while (element.length > 0) {
                        element[0].remove();
                    }
                }

                const paragraph = document.createElement("p");
                const text = document.createTextNode("Je kunt de instellingen alleen wijzigen als je in een Drill bent!");
                paragraph.appendChild(text);

                const target = document.querySelector("#credits");
                target.parentNode.insertBefore(paragraph, target);
                return;
            }

            if (response.storage_enabled !== undefined) {
                document.getElementById("saveWordlist").checked = response.storage_enabled;
            }

            if (response.answer_time !== undefined) {
                document.getElementById("answerTime").value = response.answer_time;
            }

            if (response.flaw_marge !== undefined) {
                document.getElementById("flawMarge").value = response.flaw_marge;
            }
        }
    );

    document.getElementById("saveWordlist").addEventListener("click", () => setSaveWordlist());
    document.getElementById("answerTime").addEventListener("input", () => setAnswerTime());
    document.getElementById("flawMarge").addEventListener("input", () => setFlawMarge());
}

async function setSaveWordlist() {
    const wordlist = document.getElementById("saveWordlist");
    let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

    chrome.tabs.sendMessage(tab.id, {set_storage_enabled: wordlist.checked}, function (response) {
    });
}

async function setAnswerTime() {
    const answerTime = document.getElementById("answerTime");
    let value = answerTime.value;

    if (value > 999)
        value = 999;
    else if (value < 100)
        value = 100;

    let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

    chrome.tabs.sendMessage(tab.id, {set_answer_time: value}, function (response) {
    });
}

async function setFlawMarge() {
    const flawMarge = document.getElementById("flawMarge");
    let value = flawMarge.value;

    if (value > 99)
        value = 25;
    else if (value < 0)
        value = 25;

    let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

    chrome.tabs.sendMessage(tab.id, {set_flaw_marge: value}, function (response) {
    });
}

start();