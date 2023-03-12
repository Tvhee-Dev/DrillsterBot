//Drillster repetoire overview script
let questions = 0;
let flaws = 0;

//Queue
let currentCourse = undefined;
let courseQueue = [];
let drillQueue = [];

function start() {
    try {
        if (window.self !== window.top)
            return;
    } catch (e) {
    }

    chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
        if (request.run_mode !== undefined) {
            if (request.run_mode === "ON") {
                //Check if the frame.js script is already injected, otherwise load the repertoire
                if (document.getElementsByTagName("iframe").length === 0) {
                    if (request.url.startsWith("https://www.drillster.com/user/repertoire")) {
                        loadRepertoire();
                    } else if (request.startsWith("https://www.drillster.com/user")) {
                        document.getElementsByClassName("fe-a fd-a fdn-a")[0].click();
                        loadRepertoire();
                    }
                }
            }
        } else if (request.drill_finished !== undefined) {
            questions += request.questions;
            flaws += request.flaws;

            finishDrill(request.drill_finished);
        } else if (request.storage !== undefined) {
            sendResponse({can_popup: false});
            return true;
        }

        sendResponse();
        return true;
    });
}

function loadRepertoire() {
    let loadPage = setInterval(() => {
        if (document.getElementsByClassName("button radius tiny secondary fe-a fd-a fdn-a")[0] === undefined)
            return;

        clearInterval(loadPage);

        let elements = document.getElementsByClassName("fe-div fd-div course item");
        let noneSelected = document.getElementsByClassName("button-flat fe-span fd-span")[0] === undefined;

        if (noneSelected && !confirm("Wil je echt alle Drills doen?")) {
            chrome.runtime.sendMessage({off_state: true}).then();
            return;
        }

        function scanDrills() {
            for (let index in elements) {
                let element = elements[index];

                if (element.className === "fe-div fd-div course item") {
                    let percent = parseInt(element.getElementsByClassName("fe-span fd-span")[0].innerText);
                    let checkbox = element.getElementsByTagName("input")[0];
                    let courseTitle = element.getElementsByClassName("fe-h2 fd-h2")[0].innerText;

                    if (percent !== 100) {
                        let showMoreButton = element.getElementsByClassName("button-flat secondary left only-item fe-a fd-a fdn-a")[0];

                        if (showMoreButton !== undefined) {
                            let courseElements = element.getElementsByClassName("item fe-div fd-div");

                            if(courseElements.length === 0 && (checkbox.checked || noneSelected)) {
                                drillQueue.push(courseTitle + "|ALL");
                                continue;
                            }

                            for (let courseIndex in courseElements) {
                                let courseElement = courseElements[courseIndex];

                                if (!(courseElement instanceof HTMLElement) || courseElement.className !== "item fe-div fd-div")
                                    continue;

                                let courseElementPercent = parseInt(courseElement.getElementsByClassName("label proficiency only-item fe-span fd-span")[0].innerText);
                                let courseElementCheckbox = courseElement.getElementsByTagName("input")[0];

                                if (courseElementPercent !== 100 && (courseElementCheckbox.checked || checkbox.checked || noneSelected))
                                    drillQueue.push(courseTitle + "|" + courseElement.getElementsByClassName("header truncate-gray-gradient fe-div fd-div")[0].innerText);
                            }
                        } else if (checkbox.checked || noneSelected) {
                            drillQueue.push(courseTitle);
                        }
                    }
                }
            }

            if (drillQueue.length === 0 && elements.length !== 0) {
                alert("Alle Drills zijn al op 100%!");
                chrome.runtime.sendMessage({off_state: true}).then();
                return;
            }

            startDrill(drillQueue[0]);
        }

        if(noneSelected)
            clickAllShowMoreButtons(() => scanDrills());
        else
            scanDrills();
    }, 1);
}

function startDrill(drillTitle) {
    let elements = document.getElementsByClassName("item fe-div fd-div");

    function sendStartSignal(element) {
        element.getElementsByClassName("button-flat fe-a fd-a fdn-a")[0].click();

        setTimeout(() => chrome.runtime.sendMessage({on_state: true}).then(), 1000);
    }

    for (let index in elements) {
        let element = elements[index];

        if (!(element instanceof HTMLElement))
            continue;

        let courseTitle = element.getElementsByClassName("fe-h2 fd-h2")[0];
        let elementTitle = element.getElementsByClassName("header truncate-gray-gradient fe-div fd-div")[0];

        if (courseTitle !== undefined && courseTitle.innerText === drillTitle.split("|")[0]) {
            let showMoreButton = element.getElementsByClassName("material-icons middle fe-i fd-i")[0];

            if(showMoreButton === undefined) {
                sendStartSignal(element);
                return;
            }

            clickShowMoreButton(showMoreButton, () => {
                if(drillTitle.split("|")[1] === "ALL") {
                    courseQueue = [];
                    currentCourse = courseTitle.innerText;
                    let courseElements = element.getElementsByClassName("item fe-div fd-div");

                    for (let courseIndex in courseElements) {
                        let courseElement = courseElements[courseIndex];

                        if (!(courseElement instanceof HTMLElement) || courseElement.className !== "item fe-div fd-div")
                            continue;

                        let courseElementPercent = parseInt(courseElement.getElementsByClassName("label proficiency only-item fe-span fd-span")[0].innerText);

                        if (courseElementPercent !== 100)
                            courseQueue.push(courseTitle.innerText + "|" + courseElement.getElementsByClassName("header truncate-gray-gradient fe-div fd-div")[0].innerText);
                    }

                    startDrill(courseQueue[0]);
                    return;
                }

                startDrill(drillTitle.split("|")[1]);
            });
        } else if (elementTitle !== undefined && elementTitle.innerText === drillTitle) {
            sendStartSignal(element);
        }
    }
}

function clickAllShowMoreButtons(runAfter) {
    let clickMoreLoad = setInterval(() => {
        let moreButton = document.getElementsByClassName("button tiny secondary gray radius fe-a fd-a fdn-a")[0];

        if (moreButton !== undefined) {
            moreButton.click();
            return;
        }

        clearInterval(clickMoreLoad);

        let showMoreButtons = document.getElementsByClassName("material-icons middle fe-i fd-i").length;
        let index = 0;

        function takeButton() {
            clickShowMoreButton(showMoreButtons[index], () => {
                index++;

                if(index >= showMoreButtons)
                    runAfter();
                else
                    takeButton();
            });
        }

        takeButton();
    }, 50);
}

function clickShowMoreButton(button, runAfter) {
    if (button.innerText === "expand_more")
        button.click();

    let courseElementLoad = setInterval(() => {
        if (document.getElementsByClassName("spinner").length === 0) {
            clearInterval(courseElementLoad);
            runAfter();
        }
    }, 50);
}

function finishDrill(drillTitle) {
    function startNextDrillQueue() {
        let index = drillQueue.indexOf(drillTitle) + 1;

        if (index === drillQueue.length)
            chrome.runtime.sendMessage({drills_finished: true, questions: questions, flaws: flaws}).then();
        else
            startDrill(drillQueue[index]);
    }

    if(currentCourse !== undefined) {
        let index = courseQueue.indexOf(currentCourse + "|" + drillTitle) + 1;

        if(index === courseQueue.length) {
            drillTitle = currentCourse + "|ALL";
            startNextDrillQueue();
        }
        else {
            startDrill(courseQueue[index]);
        }
    }
    else {
        startNextDrillQueue();
    }
}

start();