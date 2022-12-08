let sentNotifications = {};

function start() {
    chrome.runtime.onInstalled.addListener(async () => {
        await chrome.action.setBadgeText({
            text: "OFF",
        });
    });

    chrome.commands.onCommand.addListener(async () => {
        let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

        if (tab.url.startsWith("https://www.drillster.com/user")) {
            const prevState = await chrome.action.getBadgeText({tabId: tab.id});
            const nextState = prevState === "ON" ? "OFF" : "ON";

            if (prevState === "DNE" || prevState === "RLD")
                return;

            await sendState(nextState);
        }
    });

    chrome.runtime.onMessage.addListener(async (request, sender, sendResponse) => {
        if (request.percentage !== undefined) {
            let notification = {
                type: "basic",
                iconUrl: "../images/icon-128.png",
                title: "DrillsterBot",
                message: "DrillsterBot is klaar met " + request.drill_title + "! Vragen: " + request.questions + ", Fouten: " + request.flaws,
                buttons: [{title: "Close"}]
            }

            if(!request.auto_close) {
                notification.buttons = [{title: "Close Tab"}, {title: "Browse Tab"}];
                await setBadgeText("DNE", sender.tab.id);
            }
            else {
                await chrome.tabs.remove(sender.tab.id);
            }

            chrome.notifications.create("drill_finished", notification, function (notificationId) {
                if(!request.auto_close)
                    sentNotifications[notificationId] = sender.tab.id;
            });
        } else if (request.retry !== undefined) {
            await sendState("ON", sender.tab.id);
        } else if (request.initialize !== undefined) {
            await sendState("OFF", sender.tab.id);
        }

        sendResponse();
        return true;
    });

    chrome.notifications.onButtonClicked.addListener(async (notificationId, buttonIndex) => {
        if(notificationId in sentNotifications) {
            let tab = await chrome.tabs.get(sentNotifications[notificationId]);

            chrome.notifications.clear(notificationId);
            delete sentNotifications[notificationId];

            if (tab === undefined)
                return;

            if (buttonIndex === 0)
                await chrome.tabs.remove(tab.id);
            else
                await chrome.tabs.update(tab.id, {"active": true});
        }
    });
}

async function sendState(state, preferredTabId) {
    let tabId = preferredTabId;

    if (preferredTabId === undefined) {
        let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});
        tabId = tab.id;
    }

    await chrome.tabs.sendMessage(tabId, {run_mode: state}, async () => {
        if (chrome.runtime.lastError) {
            await chrome.action.setBadgeText({
                tabId: tabId,
                text: "RLD",
            });
        }
    });

    await setBadgeText(state, tabId);
}

async function setBadgeText(text, tabId) {
    await chrome.action.setBadgeText({
        tabId: tabId,
        text: text,
    });
}

start();

/*chrome.runtime.onInstalled.addListener(toggleonoff("OFF"));
chrome.commands.onCommand.addListener(toggleonoff());

chrome.runtime.onMessage.addListener(async (request, sender, sendResponse) => {
    if (request.percentage !== undefined) {
        chrome.notifications.create("drill_finished", {
            type: "basic",
            iconUrl: "../images/icon-128.png",
            title: "DrillsterBot",
            message: "DrillsterBot is klaar met deze Drill! Vragen: " + request.questions + ", Fouten: " + request.flaws
        });

        await chrome.action.setBadgeText({
            tabId: request.tabId,
            text: "DNE",
        });

        return true;
    }

    if (request.retry !== undefined) {
        await toggleonoff("ON");
    }

    sendResponse();
    return true;
});

async function toggleonoff(state) {
    let [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});

    if (tab.url.startsWith("https://www.drillster.com/user")) {
        const prevState = await chrome.action.getBadgeText({tabId: tab.id});
        let nextState = state === undefined ? (prevState === "ON" ? "OFF" : "ON") : state;

        if (prevState === "DNE" || prevState === "RLD")
            return;

        chrome.tabs.sendMessage(tab.id, {run_mode: nextState, tabId: tab.id}, async () => {
            if (chrome.runtime.lastError) {
                await chrome.action.setBadgeText({
                    tabId: tab.id,
                    text: "RLD",
                });
            }
        });

        await chrome.action.setBadgeText({
            tabId: tab.id,
            text: nextState,
        });
    }
}*/