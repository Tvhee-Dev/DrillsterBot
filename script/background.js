let sentNotifications = {};

function start() {
    chrome.commands.onCommand.addListener(async (command, tab) => {
        if (!tab.url.startsWith("https://www.drillster.com/user"))
            return;

        if (command === "start_stop") {
            const prevState = await chrome.action.getBadgeText({tabId: tab.id});
            const nextState = prevState === "ON" ? "OFF" : "ON";

            if (prevState === "DNE" || prevState === "RLD")
                return;

            await sendState(nextState);
        } else if (command === "wordlist") {
            await chrome.tabs.sendMessage(tab.id, {show_wordlist: true}, async () => {
                if (chrome.runtime.lastError) {} //Preventing "Receiving end does not exist!"
            });
        }
    });

    chrome.runtime.onMessage.addListener(async (request, sender, sendResponse) => {
        if (request.percentage !== undefined) {
            let notification = {type: "basic", iconUrl: "../images/icon-128.png", title: "DrillsterBot",
                message: "DrillsterBot is klaar met " + request.drill_title + "! Vragen: " + request.questions + ", Fouten: " + request.flaws,
                buttons: [{title: "Close"}]
            }

            if (!request.auto_close) {
                notification.buttons = [{title: "Close Tab"}, {title: "Browse Tab"}];
                await setBadgeText("DNE", sender.tab.id);
            } else {
                await chrome.tabs.remove(sender.tab.id);
            }

            chrome.notifications.create("drill_finished", notification, (notificationId) => {
                if (!request.auto_close)
                    sentNotifications[notificationId] = sender.tab.id;
            });
        } else if (request.initialize !== undefined) {
            await sendState("OFF", sender.tab.id);
        }

        sendResponse();
        return true;
    });

    chrome.notifications.onButtonClicked.addListener(async (notificationId, buttonIndex) => {
        if (notificationId in sentNotifications) {
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