async function start() {
    let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

    chrome.tabs.sendMessage(tab.id, {storage: "Retrieve"}, function (response) {
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
                const wordlist = document.getElementById("saveWordlist");
                wordlist.checked = response.storage_enabled;
            }

            if (response.answer_time !== undefined) {
                const answerTime = document.getElementById("answerTime");
                answerTime.value = response.answer_time;
            }
        }
    );

    document.getElementById("saveWordlist").addEventListener("click", () => setSaveWordlist());
    document.getElementById("answerTime").addEventListener("input", () => setAnswerTime());
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

    if(value > 5000)
        value = 5000;
    else if(value < 100)
        value = 100;

    let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

    chrome.tabs.sendMessage(tab.id, {set_answer_time: value}, function (response) {
    });
}

start();